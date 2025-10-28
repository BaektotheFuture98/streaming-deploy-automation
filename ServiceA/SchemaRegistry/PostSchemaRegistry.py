from schema_registry.client import SchemaRegistryClient,schema
import os, json, pathlib


# client = SchemaRegistryClient(url=os.getenv("SCHEMA_REGISTRY"))
client = SchemaRegistryClient(url="http://192.168.125.61:8081")

matches = list(pathlib.Path(__file__).resolve().parent.glob("schema.json"))
schema_file = json.loads(matches[0].read_text(encoding="utf-8"))
print("schema_file:", schema_file)
my_avro_schema_dict = schema_file

avro_schema = schema.AvroSchema(my_avro_schema_dict)

    # Register the schema under a subject
# subject_name = os.getenv("SERVICE_NAME") + "-value" #
subject_name = "BPE_DATA" + "-value" #
schema_id = client.register(subject_name, avro_schema)

print(f"Schema registered with ID: {schema_id}")