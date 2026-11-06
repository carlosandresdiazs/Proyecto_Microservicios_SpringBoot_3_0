#!/usr/bin/env bash
set -euo pipefail
kubectl -n proyecto-03 get deployments,pods,svc,pvc
kubectl -n proyecto-03 get events --sort-by=.lastTimestamp | tail -25
