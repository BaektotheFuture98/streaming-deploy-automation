# import pymysql, os
# from mysql.connector import pooling
# from dotenv import load_dotenv


# # .env 파일 로드
# load_dotenv()

# def mysql_conn_status() -> pymysql.Connection:
#     connection = pymysql.connect(
#         host = os.getenv("MYSQL_STATUS_HOST"),
#         port = os.getenv("MYSQL_STATUS_PORT"),
#         user = os.getenv("MYSQL_USER"),
#         passsword = os.getenv("MYSQL_PASSWORD")
#     )

#     return connection

# def mysql_queries(service : str) : 
#     if service == "logstash" : 
#         pass
#     elif service == ""
