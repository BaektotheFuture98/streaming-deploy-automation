from src.entities.schema import Schema
import json 

def set_meta(schema: Schema) -> json: 
    meta = {}
    meta["MYSQL_HOST"] = schema.host + "/" + schema.database
    meta["MYSQL_USER"] = schema.user
    meta["MYSQL_PASSWORD"] = schema.password
    meta["SERVICE_NAME"] = schema.project_name
    meta["ELASTICSEARCH_INDEX"] = schema.elasticsearch_index
    meta["TABLE"] = schema.table
    meta["QUERY"] = schema.query

    return json.dumps(meta, ensure_ascii=False, indent=4)

def set_fields(schema: Schema) -> json: 
    data_schema = {}
    data_schema["type"] = "record"
    data_schema["name"] = schema.project_name

    fields = []
    for field_name in schema.fields : 
        
        if field_name == "in_date" : 
            field_type = "int"
        else : 
            field_type = "string"  # field type은 하드코딩으로 name에 맞춰서 지정 필요

        field_schema = {}
        field_schema["name"] = field_name
        field_schema["type"] = field_type
        fields.append(field_schema)

    data_schema["fields"] = fields

    return json.dumps(data_schema, ensure_ascii=False, indent=4)

