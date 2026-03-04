import logging
import structlog
from fastapi import FastAPI, HTTPException
from pydantic import BaseModel
from sentence_transformers import SentenceTransformer
from typing import List
import torch

# Logging setup
structlog.configure(
    processors=[
        structlog.processors.TimeStamper(fmt="iso"),
        structlog.stdlib.add_log_level,
        structlog.processors.JSONRenderer()
    ],
    wrapper_class=structlog.stdlib.BoundLogger,
    logger_factory=structlog.stdlib.LoggerFactory(),
)
logger = structlog.get_logger()

app = FastAPI(title="Embedding Service")

# Load model at startup (multilingual-e5-large — strong russian+english support, 1024 dim)
MODEL_NAME = "intfloat/multilingual-e5-large"
device = "cuda" if torch.cuda.is_available() else "cpu"
logger.info("loading embedding model", model=MODEL_NAME, device=device)

try:
    model = SentenceTransformer(MODEL_NAME, device=device)
    logger.info("embedding model loaded successfully")
except Exception as e:
    logger.error("failed to load embedding model", exc_info=True)
    raise RuntimeError("Cannot start without embedding model") from e


class EmbedRequest(BaseModel):
    texts: List[str]


class EmbedResponse(BaseModel):
    embeddings: List[List[float]]


@app.post("/embed", response_model=EmbedResponse)
async def embed_texts(req: EmbedRequest):
    if not req.texts:
        raise HTTPException(400, "texts list cannot be empty")

    try:
        # truncate very long texts (safety)
        cleaned_texts = [t[:8000] for t in req.texts if t and t.strip()]

        if not cleaned_texts:
            return EmbedResponse(embeddings=[])

        embeddings = model.encode(
            cleaned_texts,
            normalize_embeddings=True,
            batch_size=32,
            show_progress_bar=False,
            convert_to_numpy=True,
        ).tolist()

        logger.info("generated embeddings", count=len(embeddings), sample_shape=len(embeddings[0]) if embeddings else 0)
        return EmbedResponse(embeddings=embeddings)

    except Exception as e:
        logger.error("embedding failed", exc_info=True)
        raise HTTPException(500, "Embedding error") from e


@app.get("/health")
async def health():
    return {"status": "ok", "model": MODEL_NAME, "dimension": model.get_sentence_embedding_dimension()}


if __name__ == "__main__":
    import uvicorn
    uvicorn.run(app, host="0.0.0.0", port=8001, log_level="info")