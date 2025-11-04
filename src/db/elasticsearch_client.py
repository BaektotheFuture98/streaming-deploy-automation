import os,logging, time
from dotenv import load_dotenv
from elasticsearch import Elasticsearch as es

# .env 파일 로드
load_dotenv()

class ResilientElasticsearch:
    def __init__(self, retry_interval=5, max_retries=None):
        self.retry_interval = retry_interval
        self.max_retries = max_retries
        self.client = self.connect()

    def connect(self):
        INTERNAL_NETWORK = os.getenv("ELASTICSEARCH_HOSTS")
        ES_USER = os.getenv("ELASTICSEARCH_USER")
        ES_PASSWORD = os.getenv("ELASTICSEARCH_PASSWORD")

        hosts = [
            f"http://{host.strip()}:9200" for host in INTERNAL_NETWORK.split(",") if host.strip()
        ]
        
        if not hosts:
            raise ValueError("No valid Elasticsearch hosts available")

        attempt = 0
        while True:
            try:
                client = es(hosts, basic_auth=(ES_USER, ES_PASSWORD))
                print(client)
                if client.ping():
                    logging.info(f"Connected to Elasticsearch after {attempt} retries.")
                    return client
                else:
                    logging.warning("Elasticsearch server not responding. Retrying...")

            except Exception as e:
                logging.error(f"Elasticsearch connection failed: {e}")

            attempt += 1
            if self.max_retries and attempt >= self.max_retries:
                logging.error("Max retries reached. Could not connect to Elasticsearch.")
                return None
            
            time.sleep(self.retry_interval)

    def get_client(self):
        """ 클라이언트 상태를 확인하고, 끊겼으면 재연결 """
        try:
            if not self.client or not self.client.ping():
                logging.warning("Elasticsearch connection lost. Reconnecting...")
                self.client = self.connect()
        except Exception as e:
            logging.error(f"Error while checking Elasticsearch connection: {e}")
            self.client = self.connect()
        
        return self.client
    