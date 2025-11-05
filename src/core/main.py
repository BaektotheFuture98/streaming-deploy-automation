from fastapi import FastAPI, HTTPException
from src.entities.schema import Schema
from src.utils.file_util import make_file, update_service_in_bash,get_unique_project_name
from src.services.metadata_schema import set_fields, set_meta
from src.services.git_command import git_push_main
import subprocess, os

app = FastAPI()

@app.post("/register")
def register_schema(schema: Schema):
    try:
        base_path = "modules/EsToDB/Services"
        # 고유 project_name 생성
        counter = get_unique_project_name(base_path, schema.project_name)
        schema.project_name = f"{schema.project_name}-{counter-1}"
        schema.table = f"{schema.table}-{counter}"
        os.makedirs(f"{base_path}/{schema.project_name}", exist_ok=True)        # 스키마 파일 변환 구현
        schema_data = set_fields(schema)  # 데이터 스키마 설정파일 구현
        make_file(f"modules/EsToDB/Services/{schema.project_name}/schema.json", schema_data)

        # 메타데이터 설정파일 구현
        meta_data = set_meta(schema)
        make_file(f"modules/EsToDB/Services/{schema.project_name}/{schema.project_name}.json", meta_data)

        # deploy.py 파일 수정
        update_service_in_bash("modules/deploy.sh", schema.project_name)

        # git 배포
        git_push_main(f"add : {schema.project_name}")

        return {"message": "Schema and metadata files created successfully"}
    except Exception as e:
        raise HTTPException(status_code=500, detail=str(e))
    

# @app.get("/status")
# def get_status(): 
#     # jenkins: 서비스 상태 확인용 엔드포인트
#     return {"status": "Service is running"}

# @app.post("/restart") 
# def restart_pipeline(name : str) : 
#     # logstash : 파이프라인 재시작용
#     return  {"message": f"Pipeline {name} restart requested"}