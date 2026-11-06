package edu.unibague.payments.config;
import org.springframework.context.annotation.*;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.security.oauth2.core.*;
import org.springframework.security.web.SecurityFilterChain;
import java.util.*;
@Configuration public class SecurityConfig {
 @Bean SecurityFilterChain filterChain(HttpSecurity http)throws Exception{
  return http.csrf(c->c.disable()).sessionManagement(s->s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
   .authorizeHttpRequests(a->a
    .requestMatchers("/actuator/health/**").permitAll()
    .requestMatchers(HttpMethod.POST,"/api/payments/webhooks/sandbox").permitAll()
    .requestMatchers(HttpMethod.POST,"/api/payments").hasAuthority("SCOPE_payments:write")
    .requestMatchers(HttpMethod.GET,"/api/payments/**").hasAuthority("SCOPE_payments:read")
    .anyRequest().denyAll())
   .oauth2ResourceServer(o->o.jwt(j->{})).build();
 }
 @Bean JwtDecoder jwtDecoder(@org.springframework.beans.factory.annotation.Value("${spring.security.oauth2.resourceserver.jwt.jwk-set-uri}") String jwks,
 @org.springframework.beans.factory.annotation.Value("${spring.security.oauth2.resourceserver.jwt.issuer-uri}") String issuer,
 @org.springframework.beans.factory.annotation.Value("${app.oauth.audience:payments-api}") String audience){
  NimbusJwtDecoder decoder=NimbusJwtDecoder.withJwkSetUri(jwks).build();
  OAuth2TokenValidator<Jwt> standard=JwtValidators.createDefaultWithIssuer(issuer);
  OAuth2TokenValidator<Jwt> aud=jwt->jwt.getAudience().contains(audience)?OAuth2TokenValidatorResult.success():
   OAuth2TokenValidatorResult.failure(new OAuth2Error("invalid_token","Audiencia no permitida",null));
  decoder.setJwtValidator(new DelegatingOAuth2TokenValidator<>(standard,aud));return decoder;
 }
}