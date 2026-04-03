# Streaming Deploy Automation
**메타데이터 기반 데이터 파이프라인 자동 배포 시스템**

## 개요
이 프로젝트는 **메타데이터를 기반으로 Logstash → Kafka → MySQL 등 데이터 파이프라인을 자동으로 구성하고 배포**하기 위한 자동화 플랫폼입니다.  
개발자가 파이프라인 구성 정보를 직접 코드로 조작하는 대신, **서비스별 환경 변수 및 스키마(메타데이터)** 를 정의하면, 시스템이 해당 정보를 읽고 **파이프라인을 자동으로 생성/배포/실행**합니다.

즉, *파이프라인을 “코드로 정의하는 것이 아니라 메타데이터로 정의”하는 방식*입니다.

---

## 목적
| 기존 방식 | 본 프로젝트 방식 |
|----------|----------------|
| Logstash, Kafka Connector, 스키마 설정을 수작업으로 관리 | **메타데이터를 입력하면 설정이 자동 생성** |
| 신규 서비스마다 설정/스크립트 복사-수정-테스트 반복 | **서비스 단위 메타데이터만 추가하면 자동 확장** |
| 사람이 관리하는 운영 → 변경 시 리스크 큼 | **자동화된 설정 생성 → 재현 가능성 + 안정성 증가** |

> ✅ **핵심 목표: 메타데이터만 업데이트하면, 파이프라인이 자동으로 생성되고 실행되는 환경 구축**

## 빠르게 이해하기
이 저장소는 크게 두 흐름으로 구성됩니다.

1. API가 서비스 메타데이터를 생성합니다.
   - [`src/core/main.py`](src/core/main.py) 의 `/register` 엔드포인트가 입력 payload를 받아
   - [`modules/EsToDB/Services/<service>/schema.json`](modules/EsToDB/Services/example_service/schema.json) 과
   - [`modules/EsToDB/Services/<service>/<service>.json`](modules/EsToDB/Services/example_service/example_service.json) 을 생성합니다.
2. Jenkins가 생성된 메타데이터를 배포합니다.
   - [`modules/deploy.sh`](modules/deploy.sh) 에 적힌 서비스 목록을 읽고
   - [`jenkins.groovy`](jenkins.groovy) 가 Schema Registry 등록, Logstash 설정 생성, Kafka Connector 배포를 순서대로 수행합니다.

## 주요 디렉터리
| 경로 | 역할 |
|------|------|
| `src/core` | FastAPI 엔드포인트와 서비스 등록 진입점 |
| `src/services` | 메타데이터 및 스키마 JSON 생성 로직 |
| `src/utils` | 파일 생성, 서비스명 갱신 등 보조 유틸리티 |
| `modules/EsToDB/SchemaRegistry` | Avro 스키마 등록 스크립트 |
| `modules/EsToDB/Logstash` | Logstash 템플릿 |
| `modules/EsToDB/Connector` | Kafka Connect JDBC Sink 템플릿 |
| `modules/EsToDB/Services` | 서비스별 스키마/메타데이터 예시 |

## 예시 파일
- 서비스 메타데이터 예시: [`modules/EsToDB/Services/example_service/example_service.json`](modules/EsToDB/Services/example_service/example_service.json)
- 서비스 스키마 예시: [`modules/EsToDB/Services/example_service/schema.json`](modules/EsToDB/Services/example_service/schema.json)
- API 요청 payload 예시: [`examples/register-schema-request.json`](examples/register-schema-request.json)
- Jenkins 환경 변수 예시: [`jenkins.env.example`](jenkins.env.example)

예시 파일에 들어 있는 `mysql.example.internal`, `connect.example.internal`, `schema-registry.example.internal` 같은 주소는 실제 접속 주소가 아니라 저장소 공개용 placeholder 입니다. 그대로는 접속되지 않으며, 운영 환경에서는 실제 내부 주소나 Jenkins Credentials 값으로 바꿔서 사용해야 합니다.

## 서비스 등록 예시
먼저 의존성을 설치하고 FastAPI 서버를 실행한 뒤, 아래 payload를 `/register` 로 보내면 새 서비스용 메타데이터를 생성합니다.

```bash
pip install -e .
```

```bash
uvicorn src.core.main:app --reload
```

```bash
curl -X POST http://127.0.0.1:8000/register \
  -H 'Content-Type: application/json' \
  -d @examples/register-schema-request.json
```

---

## 주요 동작 방식
1. 신규 서비스용 스키마 및 파라미터를 메타데이터로 정의
2. Git push → Jenkins Pipeline 자동 실행
3. 자동 수행 단계:
   - 스키마 레지스트리 등록
   - Kafka 커넥터 설정 생성 & 배포
   - Logstash config 템플릿 환경 변수 치환 후 생성
   - Logstash 자동 재시작 및 동작 검증

---

## 운영 관점 장단점

### ✅ 장점
- **운영 일관성 확보**  
  템플릿 기반 자동 생성으로, 환경/사람에 따른 설정 편차가 사라짐.
- **서비스 확장 비용 매우 적음**  
  새로운 파이프라인 추가 시 *메타데이터 한 건 등록만으로 적용.*
- **휴먼에러 감소**  
  수작업 없이 자동화되어 실수로 인한 장애 가능성 감소.
- **재현성과 추적성 확보**  
  메타데이터와 템플릿은 Git으로 관리 → 변경 이력이 명확함.

### ⚠️ 단점 (운영 시 고려사항)
- **초기 템플릿 구조 설계 비용이 큼**  
  템플릿/환경 변수 체계가 안정되기 전까지는 조정 비용이 들 수 있음.
- **특수 케이스 반영 시 유연성 필요**  
  자동화가 일반 패턴에 맞춰져 있어, 예외적인 파이프라인 요구사항은 추가 템플릿 제작이 필요. -> jenkins 파이프라인 수정 필요
- **메타데이터의 정확도가 시스템 안정성을 좌우**  
  메타데이터 입력이 잘못되면 잘못된 파이프라인이 자동으로 배포됨 → 검증 체계 필요.
## 사용 기술

| 구분 | 기술/도구 | 역할 |
|------|-----------|------|
| **CI/CD** | Jenkins Pipeline | Git push 트리거 후 파이프라인 자동 배포 및 실행 |
| **데이터 스트리밍** | Kafka / Kafka Connect | 데이터 스트림 전달 및 Sink Connector 구성 |
| **메시지 스키마 관리** | Confluent Schema Registry | 데이터 포맷 제어 및 호환성 관리 |
| **데이터 수집/변환** | Logstash | Elasticsearch → kafka 데이터 파이프라인 구성 |
| **데이터 조회/추출** | Elasticsearch | 사용자 요청 조건 기반 데이터 조회 |
| **데이터 저장** | MySQL | 조회된 데이터 적재 |
| **템플릿 엔진** | envsubst / Bash Script | Logstash & Connector 설정 파일 환경변수 기반 템플릿화 |
| **배포 자동화 스크립트** | Python | 스키마 등록 |
| **버전 관리** | GitHub | 메타데이터/파이프라인 템플릿 변경 이력 관리 |
> 🚧 현재 이 프로젝트는 **Airflow 기반 파이프라인으로 전환 중**입니다.

## 보안 메모
- 저장소에는 실제 운영 계정, 비밀번호, 내부 IP를 넣지 않고 예시 값만 유지합니다.
- Jenkins 실행 시 필요한 값은 [`jenkins.env.example`](jenkins.env.example) 를 기준으로 외부 환경변수나 Jenkins Credentials에 주입하는 것을 권장합니다.
- `modules/EsToDB/Services` 아래 JSON은 구조 예시로 보고, 실제 운영값은 커밋 전에 반드시 placeholder 또는 비밀 저장소 참조 값으로 치환하세요.
- `127.0.0.1:8000/register` 예시는 로컬에서 FastAPI 서버를 띄웠을 때만 접속됩니다. 예시 JSON 안의 외부 주소들은 API 접속 주소가 아니라 메타데이터용 샘플 값입니다.

## 👨‍💻 Maintainers
- SEONMIN
