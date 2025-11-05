import json, re, os

def make_file(path: str, content: json) -> None: 
    with open(path, "w", encoding="utf-8") as f: 
        f.write(content)
        # json.dump(content, f, ensure_ascii=False, indent=4)

def update_service_in_bash(script_path: str, service_name: str) -> None:
    """
    Bash 스크립트 내의 services=(...) 부분을 단일 서비스로 교체합니다.

    Args:
        script_path (str): 수정할 bash 파일 경로
        service_name (str): 새롭게 넣을 서비스 이름
    """
    try:
        # 파일 내용 읽기
        with open(script_path, "r", encoding="utf-8") as f:
            content = f.read()

        # 새로운 services 라인 생성
        new_line = f'services=("{service_name}")'

        # 정규식으로 기존 services=(...) 라인 교체
        updated_content = re.sub(r'services=\([^)]+\)', new_line, content)

        # 수정된 내용 덮어쓰기
        with open(script_path, "w", encoding="utf-8") as f:
            f.write(updated_content)

        print(f"✅ {script_path} 내 services 값이 '{service_name}' 으로 변경되었습니다.")

    except FileNotFoundError:
        print(f"❌ 파일을 찾을 수 없습니다: {script_path}")
    except Exception as e:
        print(f"⚠️ 업데이트 중 오류 발생: {e}")

def get_unique_project_name(base_path, project_name, pad_width=3):
    """
    이미 존재하는 project_name이면 -001, -002 ... 붙여서 고유하게 반환
    pad_width: 숫자 패딩 길이 (기본 3자리)
    """
    existing_names = set(os.listdir(base_path))
    unique_name = project_name
    print(f"project_name : {project_name}")
    counter = 1

    while unique_name in existing_names:
        unique_name = f"{project_name}_{counter}"
        print(f"unique_name : {unique_name} counter : {counter}")
        counter += 1
        

    return counter