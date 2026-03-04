# Cloud Storage с интеллектуальным поиском (Backend)

Микросервисная система облачного хранилища файлов с поддержкой интеллектуального поиска по содержимому (BM25 + векторный поиск через Elasticsearch).

## Архитектура

- **Микросервисы**:
    - **AuthService** — аутентификация, регистрация, выдача JWT
    - **ApiGateway** — единая точка входа, проксирование запросов
    - **StorageService** — загрузка, скачивание, удаление файлов (MinIO)
    - **MetadataService** — хранение метаданных файлов (PostgreSQL)
    - **IngestIndexerService** — индексация содержимого файлов
    - **EmbeddingService** — генерация эмбеддингов (sentence-transformers)
    - **SearchService** — гибридный поиск по содержимому (BM25 + dense_vector)

- **Инфраструктура**:
    - Kafka — брокер сообщений
    - MinIO — объектное хранилище
    - PostgreSQL — метаданные
    - Elasticsearch — поисковой движок
    - Tesseract OCR — распознавание текста на изображениях

## Основные эндпоинты (через ApiGateway — http://localhost:8080)

### AuthService
- `POST /auth/register` — регистрация  
  Body: `{ "email": "...", "password": "..." }`  
  Response: `{ "accessToken": "...", "refreshToken": "..." }`

- `POST /auth/login` — вход  
  Body: `{ "email": "...", "password": "..." }`  
  Response: `{ "accessToken": "...", "refreshToken": "..." }`

- `POST /auth/refresh?refreshToken=...` — обновление access-токена

### StorageService
- `POST /api/storage/upload` — загрузка файла  
  Multipart: `file=@path/to/file`  
  Headers: Authorization: Bearer ...

- `GET /api/storage/download/{fileId}` — скачивание файла  
  Headers: Authorization: Bearer ...

- `DELETE /api/storage/{fileId}` — удаление файла  
  Headers: Authorization: Bearer ...

### MetadataService
- `GET /api/metadata/files` — список файлов пользователя  
  Headers: Authorization: Bearer ... (X-User-Id добавляет Gateway)

- `GET /api/metadata/files/{fileId}` — метаданные одного файла

### SearchService
- `POST /api/search` — поиск по содержимому  
  Body: `{ "query": "текст", "page": 1, "size": 10 }`  
  Headers: Authorization: Bearer ...  
  Response: `{ "results": [...], "totalHits": N, "page": 1, "size": 10 }`

## Как запустить

### Требования
- Docker + Docker Compose
- Java 17+ (для Spring-сервисов)
- Python 3.10+ (для Python-сервисов)
- Maven (сборка java + Spring boot проектов)

# Локальный запуск (инфраструктура через Docker + сервисы вручную)

## Что запускается через Docker Compose

Через `docker-compose` поднимается **только инфраструктура**:
- Kafka + Zookeeper
- MinIO (объектное хранилище)
- PostgreSQL (метаданные)
- Elasticsearch (индекс для поиска)

Сами микросервисы запускаются **вручную** (Java/Python).

## Шаг 1. Запуск инфраструктуры

```bash
# В директории infra
docker-compose up -d
```
## Шаг 2. Запуск микросервисов вручную
Каждый в отдельном терминале:

### EmbeddingService (Python)
```bash
cd EmbeddingService
uvicorn main:app --host 0.0.0.0 --port 8001
```
### IngestIndexerService (Python)
```bash
cd IngestIndexerService
python main.py
```
### AuthService (Java)
```bash
cd AuthService
mvn spring-boot:run
```
### ApiGateway (Java)
```bash
cd ApiGateway
mvn spring-boot:run
```
### StorageService (Java)
```bash
cd StorageService
mvn spring-boot:run
```
### MetadataService (Java)
```bash
cd MetadataService
mvn spring-boot:run
```
### SearchService (Java)
```bash
cd SearchService
mvn spring-boot:run
```