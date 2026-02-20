from pydantic import BaseSettings


class Settings(BaseSettings):
    # Kafka
    KAFKA_BOOTSTRAP_SERVERS: str = "localhost:9092"
    KAFKA_GROUP_ID: str = "ingest-indexer-group"
    KAFKA_TOPIC: str = "file.uploaded"

    # Storage
    STORAGE_SERVICE_URL: str = "http://storage-service:8001"

    # Elasticsearch
    ELASTICSEARCH_HOST: str = "http://localhost:9200"
    INDEX_NAME: str = "documents"

    # Embedding
    EMBEDDING_MODEL_NAME: str = "sentence-transformers/all-MiniLM-L6-v2"
    EMBEDDING_DIM: int = 384

    # Chunking
    CHUNK_SIZE: int = 800
    CHUNK_OVERLAP: int = 100

    class Config:
        env_file = ".env"


settings = Settings()
