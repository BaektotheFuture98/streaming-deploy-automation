from pydantic import BaseModel

class Status(BaseModel) : 
    project_name : str
    service : str
    health : str