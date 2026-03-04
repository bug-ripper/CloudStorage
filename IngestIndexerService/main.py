import asyncio
import json
import logging
import structlog
from aiokafka import AIOKafkaConsumer, AIOKafkaProducer
from elasticsearch import AsyncElasticsearch
import httpx
import fitz  # PyMuPDF
from docx import Document
from PIL import Image
import pytesseract
import io
from langchain_text_splitters import RecursiveCharacterTextSplitter
from typing import List, Dict, Any
import uuid
from datetime import datetime

structlog.configure(
    processors=[
        structlog.contextvars.merge_contextvars,          # если используете contextvars
        structlog.processors.add_log_level,
        structlog.processors.StackInfoRenderer(),
        structlog.processors.format_exc_info,             # ← это важно для traceback
        structlog.processors.TimeStamper(fmt="iso"),
        structlog.dev.ConsoleRenderer(colors=True),       # ← красивый цветной вывод в консоль
    ],
    wrapper_class=structlog.make_filtering_bound_logger(logging.DEBUG),  # ← пропускаем DEBUG и выше
    logger_factory=structlog.stdlib.LoggerFactory(),
    cache_logger_on_first_use=True,
)

# Дополнительно: заставляем стандартный logging тоже выводить всё
logging.basicConfig(
    level=logging.DEBUG,
    format="%(message)s",  # structlog сам форматирует
)

logger = structlog.get_logger()

# Config
KAFKA_BOOTSTRAP = "localhost:9092"
UPLOADED_TOPIC = "file.uploaded"
INDEXED_TOPIC = "file.indexed"
STORAGE_URL = "http://localhost:8082"
ES_URL = "http://localhost:9200"
ES_INDEX = "documents"
EMBEDDING_URL = "http://localhost:8000/embed"

ALLOWED_MIME = {
    "text/plain",
    "application/pdf",
    "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
    "text/markdown",
    "image/jpeg",
    "image/png",
}

es = AsyncElasticsearch(ES_URL)
producer: AIOKafkaProducer = None
splitter = RecursiveCharacterTextSplitter(
    chunk_size=500,
    chunk_overlap=100,
    length_function=len,
    separators=["\n\n", "\n", " ", ""],
)


async def init_producer():
    global producer
    producer = AIOKafkaProducer(bootstrap_servers=KAFKA_BOOTSTRAP)
    await producer.start()
    logger.info("kafka producer started")


async def produce_indexed_event(file_id: str, status: str = "indexed", error: str = None):
    event = {
        "fileId": file_id,
        "status": status,
        "indexedAt": datetime.utcnow().isoformat(),
        "error": error,
    }
    await producer.send_and_wait(INDEXED_TOPIC, json.dumps(event).encode("utf-8"))
    logger.info("published FILE_INDEXED", file_id=file_id, status=status)


async def extract_text_from_pdf(content: bytes) -> str:
    doc = fitz.open(stream=content, filetype="pdf")
    text = ""
    for page in doc:
        text += page.get_text("text") + "\n"
    doc.close()
    return text.strip()


async def extract_text_from_docx(content: bytes) -> str:
    doc = Document(io.BytesIO(content))
    return "\n".join(p.text for p in doc.paragraphs if p.text.strip())


async def extract_text_from_image(content: bytes) -> str:
    img = Image.open(io.BytesIO(content))
    text = pytesseract.image_to_string(img, lang="rus+eng")
    return text.strip()


async def download_file(file_id: str, user_id: str) -> bytes:
    """
    Скачивает файл из StorageService.

    Args:
        file_id: ID файла
        user_id: ID пользователя (требуется в заголовке X-User-Id)

    Returns:
        bytes: содержимое файла
    """
    url = f"{STORAGE_URL}/api/storage/download/{file_id}"

    headers = {
        "X-User-Id": user_id,
        # Можно добавить другие заголовки при необходимости, например:
        # "Accept": "application/octet-stream",
    }

    logger.debug("скачивание файла", file_id=file_id, user_id=user_id, url=url)

    try:
        async with httpx.AsyncClient() as client:
            resp = await client.get(
                url,
                headers=headers,
                timeout=60.0
            )
            resp.raise_for_status()

            content = resp.content
            logger.info(
                "файл успешно скачан",
                file_id=file_id,
                size=len(content),
                content_type=resp.headers.get("content-type")
            )
            return content

    except httpx.HTTPStatusError as e:
        logger.error(
            "ошибка HTTP при скачивании файла",
            file_id=file_id,
            status_code=e.response.status_code,
            response_text=e.response.text[:200] if e.response else None,
            exc_info=True
        )
        raise
    except httpx.RequestError as e:
        logger.error(
            "ошибка соединения при скачивании файла",
            file_id=file_id,
            url=url,
            exc_info=True
        )
        raise
    except Exception as e:
        logger.error(
            "неизвестная ошибка при скачивании файла",
            file_id=file_id,
            exc_info=True
        )
        raise


async def get_embedding(texts: List[str]) -> List[List[float]]:
    if not texts:
        return []
    async with httpx.AsyncClient() as client:
        resp = await client.post(EMBEDDING_URL, json={"texts": texts}, timeout=120.0)
        resp.raise_for_status()
        return resp.json()["embeddings"]


async def index_chunks(file_id: str, chunks: List[str], original_filename: str, user_id: str):
    embeddings = await get_embedding(chunks)

    if len(embeddings) != len(chunks):
        raise ValueError("embedding count mismatch")

    actions = []
    for i, (chunk, emb) in enumerate(zip(chunks, embeddings)):
        doc = {
            "file_id": file_id,
            "chunk_id": str(uuid.uuid4()),
            "chunk_index": i,
            "text": chunk,
            "embedding": emb,
            "original_filename": original_filename,
            "user_id": user_id,
            "timestamp": datetime.utcnow().isoformat(),
        }
        actions.append({"index": {"_index": ES_INDEX, "_id": f"{file_id}_{i}"}})
        actions.append(doc)

    if actions:
        await es.bulk(body=actions)
        logger.info("indexed chunks", file_id=file_id, count=len(chunks))


async def process_event(event: Dict[str, Any]):
    file_id = event.get("fileId")
    user_id = event.get("userId")
    content_type = event.get("contentType")
    object_key = event.get("objectKey")  # or storagePath
    original_filename = event.get("originalFilename", "unknown")

    if not file_id or content_type not in ALLOWED_MIME:
        logger.info("skipping file - not indexable", file_id=file_id, content_type=content_type)
        return

    logger.info("processing file for indexing", file_id=file_id, content_type=content_type)

    try:
        content = await download_file(file_id, user_id)

        if content_type == "application/pdf":
            text = await extract_text_from_pdf(content)
        elif content_type == "application/vnd.openxmlformats-officedocument.wordprocessingml.document":
            text = await extract_text_from_docx(content)
        elif content_type in ("text/plain", "text/markdown"):
            text = content.decode("utf-8", errors="replace").strip()
        elif content_type in ("image/jpeg", "image/png"):
            text = await extract_text_from_image(content)
        else:
            logger.warning("unsupported mime after check", mime=content_type)
            return

        if not text.strip():
            logger.info("empty text extracted - skipping", file_id=file_id)
            await produce_indexed_event(file_id, "empty")
            return

        chunks = splitter.split_text(text)
        logger.info("split into chunks", file_id=file_id, count=len(chunks))

        await index_chunks(file_id, chunks, original_filename, user_id)

        await produce_indexed_event(file_id)

    except Exception as e:
        logger.error("indexing failed", file_id=file_id, exc_info=True)
        await produce_indexed_event(file_id, "error", str(e))


async def consume():
    consumer = AIOKafkaConsumer(
        UPLOADED_TOPIC,
        bootstrap_servers=KAFKA_BOOTSTRAP,
        group_id="ingest-indexer-group",
        value_deserializer=lambda m: json.loads(m.decode("utf-8")),
    )
    await consumer.start()
    logger.info("kafka consumer started", topic=UPLOADED_TOPIC)

    try:
        async for msg in consumer:
            event = msg.value
            # logger.debug("received FileUploadedEvent", event=event)
            await process_event(event)
    finally:
        await consumer.stop()


async def create_index_if_not_exists():
    if not await es.indices.exists(index=ES_INDEX):
        mapping = {
            "mappings": {
                "properties": {
                    "file_id": {"type": "keyword"},
                    "chunk_id": {"type": "keyword"},
                    "chunk_index": {"type": "integer"},
                    "text": {
                        "type": "text",
                        "analyzer": "standard"  # BM25
                    },
                    "embedding": {
                        "type": "dense_vector",
                        "dims": 1024,           # multilingual-e5-large
                        "index": True,
                        "similarity": "cosine"
                    },
                    "original_filename": {"type": "keyword"},
                    "user_id": {"type": "keyword"},
                    "timestamp": {"type": "date"},
                }
            }
        }
        await es.indices.create(index=ES_INDEX, body=mapping)
        logger.info("created elasticsearch index", index=ES_INDEX)


async def main():
    await init_producer()
    await create_index_if_not_exists()

    try:
        await consume()
    finally:
        await producer.stop()
        await es.close()


if __name__ == "__main__":
    asyncio.run(main())