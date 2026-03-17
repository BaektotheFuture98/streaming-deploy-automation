#!/bin/bash

## 에러 발생 시 스크립트 종료
set -e

# 활성화된 서비스 목록 정의
services=("example_service")

for service in "${services[@]}"; do
    echo "$service"
done
