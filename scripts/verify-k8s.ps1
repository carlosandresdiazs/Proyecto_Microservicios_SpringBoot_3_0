param(
    [switch]$BuildImages,
    [switch]$SkipHealthChecks
)
# Docker y kubectl escriben progreso en stderr sin que sea necesariamente un error.
$ErrorActionPreference = 'Continue'
if (Get-Variable PSNativeCommandUseErrorActionPreference -ErrorAction SilentlyContinue) { $PSNativeCommandUseErrorActionPreference = $false }
Set-Location (Resolve-Path (Join-Path $PSScriptRoot '..'))
$log = Join-Path (Get-Location) 'reporte_validacion_kubernetes.txt'
$failures = New-Object System.Collections.Generic.List[string]
"Verificacion Proyecto 03 Kubernetes - $(Get-Date -Format s)" | Set-Content $log
function Report([string]$message) { Write-Host $message; Add-Content -Path $log -Value $message }
function Run([string]$label, [scriptblock]$action) {
    Report "`n=== $label ==="
    try {
        & $action 2>&1 | ForEach-Object { $line = "$_"; Write-Host $line; Add-Content $log $line }
        if ($LASTEXITCODE -ne 0) { throw "Exit code: $LASTEXITCODE (consulta el log anterior)" }
        Report "OK: $label"
    } catch {
        Report "ERROR: ${label}: $_"
        $failures.Add($label)
    }
}
Run 'Docker engine' { docker info --format '{{.ServerVersion}}' }
Run 'Kubernetes context' { kubectl config current-context }
Run 'Kubernetes nodes' { kubectl get nodes -o wide }
$names = @('api-gateway', 'auth-service', 'user-service', 'product-service', 'payment-service')
if ($BuildImages) {
    foreach ($name in $names) {
        Run "Docker build $name" { docker build --progress=plain -f "$name/Dockerfile" -t "proyecto03/${name}:3.1" . }
    }
}
foreach ($name in $names) {
    Run "Docker image $name" { docker image inspect "proyecto03/${name}:3.1" --format '{{.Id}}' }
}
Run 'Kubernetes resources' { kubectl -n proyecto-03 get deployments,pods,svc,pvc -o wide }
foreach ($name in ($names + @('auth-db','users-db','products-db','payments-db','keycloak'))) {
    Run "Rollout $name" { kubectl -n proyecto-03 rollout status "deployment/$name" --timeout=120s }
}
if (-not $SkipHealthChecks) {
    $targets = @(
        @{Name='api-gateway'; Remote=8080; Local=18080; Path='/actuator/health'},
        @{Name='auth-service'; Remote=8081; Local=18081; Path='/actuator/health'},
        @{Name='user-service'; Remote=8082; Local=18082; Path='/actuator/health'},
        @{Name='product-service'; Remote=8083; Local=18083; Path='/actuator/health'},
        @{Name='payment-service'; Remote=8084; Local=18084; Path='/actuator/health'},
        @{Name='keycloak'; Remote=8080; Local=18085; Path='/realms/proyecto03/.well-known/openid-configuration'}
    )
    $kubectlExe = (Get-Command kubectl -ErrorAction Stop).Source
    foreach ($item in $targets) {
        $name = $item.Name
        Report "`n=== HTTP health $name ==="
        $proc = $null
        try {
            $arguments = @('-n','proyecto-03','port-forward',"svc/$name", "$( $item.Local ):$( $item.Remote )")
            $proc = Start-Process -FilePath $kubectlExe -ArgumentList $arguments -PassThru -WindowStyle Hidden
            $response = $null
            for ($i=0; $i -lt 18; $i++) {
                Start-Sleep -Seconds 1
                try {
                    $response = Invoke-WebRequest -UseBasicParsing -TimeoutSec 4 -Uri "http://127.0.0.1:$($item.Local)$($item.Path)"
                    break
                } catch { }
            }
            if ($null -eq $response -or $response.StatusCode -ne 200) { throw 'No HTTP 200 from forwarded service' }
            Report "OK: $name HTTP $($response.StatusCode)"
            if ($name -eq 'keycloak' -and $response.Content -notmatch 'issuer') { throw 'OIDC discovery missing issuer' }
        } catch {
            Report "ERROR: $name endpoint: $_"
            $failures.Add("health $name")
        } finally {
            if ($proc) { Stop-Process -Id $proc.Id -Force -ErrorAction SilentlyContinue }
        }
    }
}
Report "`n=== Final ==="
if ($failures.Count -gt 0) {
    Report "FALLAS ($($failures.Count)): $($failures -join ', ')"
    Report 'Para investigar: kubectl -n proyecto-03 describe pod <nombre-pod>; kubectl -n proyecto-03 logs deployment/<servicio> --tail=100'
    exit 1
}
Report 'PASS: compilacion de imagenes (si -BuildImages) y comprobaciones de despliegue/HTTP solicitadas completadas.'
Report 'NOTA: probar autenticacion JWT, OAuth2, CRUD y webhooks por separado; este script no certifica esas funcionalidades.'
exit 0
