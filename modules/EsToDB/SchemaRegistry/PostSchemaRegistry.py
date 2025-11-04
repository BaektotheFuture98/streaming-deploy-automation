from schema_registry.client import SchemaRegistryClient,schema
import os, json, pathlib

def schema_registry_client(schema_url : str = None) -> SchemaRegistryClient : 
    if schema_url is None :
        raise EnvironmentError("SCHEMA_REGISTRY environment variable is empty")  
    return SchemaRegistryClient(url=schema_url)

def get_schema_json_path() -> pathlib.Path: 
    schema_json = os.getenv("SCHEMA_FILE")
    # schema_json = list(pathlib.Path(__file__).resolve().parent.glob("schema.json"))
    if not schema_json : 
        raise FileNotFoundError("schema.json file not exist")
    return schema_json

def read_schema_json(schema_json_path : str): 
    return pathlib.Path(schema_json_path).read_text(encoding="utf-8")

def post_schema_to_registry(subject_name : str, avro_schema : str, client : SchemaRegistryClient) -> int: 
    schema_id = client.register(subject_name+"-topic-value", avro_schema) 
    return schema_id

def main() : 
    schema_registry_path = os.getenv("SCHEMA_REGISTRY")
    client = schema_registry_client(schema_registry_path)

    schema_file_path = get_schema_json_path()
    schema_file_content = read_schema_json(schema_file_path)   
    
    subject_name = os.getenv("SERVICE_NAME")
    schema_id = post_schema_to_registry(subject_name, schema_file_content, client)
    print(schema_id) # Jenkins 에서 사용하기 위해 출력

    return schema_id

if __name__ == "__main__": 
    main()