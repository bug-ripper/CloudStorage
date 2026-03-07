# ---- IngestIndexerService Dockerfile ----
FROM python:3.11-slim

WORKDIR /app

COPY ./app ./app
COPY requirements.txt .

RUN pip install --no-cache-dir -r requirements.txt

ENV TRANSFORMERS_CACHE=/models/transformers_cache
ENV TORCH_HOME=/models/torch_cache
ENV PYTORCH_PRETRAINED_BERT_CACHE=/models/torch_cache

VOLUME ["/models"]

EXPOSE 8000

CMD ["python", "-m", "app.main"]