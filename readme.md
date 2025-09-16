# Data Pipeline Config Repository (개발중)

이 레포지토리는 **Schema Registry, Kafka Connector, Logstash pipeline** 설정 파일을 관리하고,  
**Jenkins CI/CD 파이프라인**을 통해 자동 배포를 수행하기 위해 사용됩니다.
---

## 🚀 Deployment Flow (with Jenkins)

1. **개발자가 수정**  
   - Schema Registry JSON  
   - Kafka Connector JSON  
   - Logstash pipeline.conf  

2. **Git Push**  
   - 브랜치 전략 (예: `dev`, `main`)  

3. **Jenkins Pipeline 자동 실행**  
   - GitHub Webhook → Jenkins Job Trigger  
   - 파라미터 선택  
     - `SERVICE_NAME`  
     - `SCHEMA_VERSION`  
     - `CONNECTOR_VERSION`  
     - `LOGSTASH_VERSION`  

4. **Stage별 배포**  
   - Stage 1: Schema Registry 등록 및 검증  
   - Stage 2: Kafka Connector 배포 및 상태 확인  
   - Stage 3: Logstash pipeline 배포 및 실행  

5. **운영 모니터링**  
   - Jenkins Stage 로그 확인  
   - Prometheus / Grafana 대시보드  

---

## 👨‍💻 Maintainers
- SEONMIN
