#!/bin/bash

## 에러 발생 시 스크립트 종료
set -e

# 활성화된 서비스 목록 정의 (예: "BPE", "SERVICE2", "SERVICE3")
services=("BPE")

for service in "${services[@]}"; do
    echo "$service"
done
