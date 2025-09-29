#!/bin/bash

## 에러 발생 시 스크립트 종료
set -e

# 서비스 목록 정의
services=("ServiceD")

for service in "${services[@]}"; do
    echo "$service"
done