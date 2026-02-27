"""
===========================================================
  Cashflow Risk Predictor — FastAPI Server
  Role: ML Engineer (Python)
===========================================================

  BASE URL : http://localhost:8000
  Docs     : http://localhost:8000/docs   (Swagger)
  Redoc    : http://localhost:8000/redoc

  ENDPOINTS
  ─────────────────────────────────────────────────────────
  GET  /health              → liveness check
  POST /predict             → single user prediction
  POST /predict/batch       → batch predictions
  GET  /model/info          → model metadata
"""

import sys
import os

# Add project root to path so model package is importable
sys.path.insert(0, os.path.join(os.path.dirname(__file__), ".."))

from fastapi import FastAPI, HTTPException
from fastapi.middleware.cors import CORSMiddleware
from pydantic import BaseModel, Field, validator
from typing import List, Optional
import json
import time
import logging

from model.train import predict, load_artifacts, FEATURE_COLS, MODEL_PATH, META_PATH

# ── Logging ────────────────────────────────────────────────────────────
logging.basicConfig(level=logging.INFO, format="%(levelname)s │ %(message)s")
log = logging.getLogger("cashflow_ml")

# ── App init ────────────────────────────────────────────────────────────
app = FastAPI(
    title="💸 Cashflow Risk Predictor — ML API",
    description=(
        "AI-powered personal cashflow risk prediction for young professionals. "
        "Receives pre-computed financial features from the Java backend and returns "
        "risk scores with explainability."
    ),
    version="1.0.0",
    contact={"name": "ML Team", "email": "ml@cashflow.dev"},
)

# ── CORS — allow Java backend + React frontend ──────────────────────────
app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],   # tighten in production
    allow_methods=["*"],
    allow_headers=["*"],
)


# ══════════════════════════════════════════════════════════════════════
#  SCHEMAS
# ══════════════════════════════════════════════════════════════════════

class FinancialFeatures(BaseModel):
    """
    Features computed by the Java backend from monthly_financial_summary.
    All fields map directly to DB columns.
    """
    savings_ratio: float = Field(
        ..., description="(total_income - total_expense) / total_income. Can be negative.",
        example=0.25
    )
    emi_ratio: float = Field(
        ..., description="EMI payments / total_income",
        ge=0.0, example=0.33
    )
    expense_volatility: float = Field(
        ..., description="Std deviation of monthly expenses (normalised)",
        ge=0.0, example=0.12
    )
    spending_trend_slope: float = Field(
        ..., description="Linear regression slope of monthly expenses",
        example=0.04
    )
    income_irregularity_score: float = Field(
        ..., description="Coefficient of variation of income",
        ge=0.0, example=0.02
    )
    expense_to_income_ratio: float = Field(
        ..., description="total_expense / total_income",
        ge=0.0, example=0.88
    )

    @validator("expense_to_income_ratio", "emi_ratio", "expense_volatility",
               "income_irregularity_score")
    def non_negative(cls, v):
        if v < 0:
            raise ValueError("Value must be ≥ 0")
        return v


class PredictRequest(BaseModel):
    user_id: int          = Field(..., description="DB user ID", example=2)
    month:   str          = Field(..., description="ISO month string YYYY-MM-DD", example="2026-02-01")
    features: FinancialFeatures


class PredictResponse(BaseModel):
    user_id:           int
    month:             str
    risk_probability:  float  = Field(..., description="Probability of HIGH risk (0–1)")
    risk_category:     str    = Field(..., description="LOW | MEDIUM | HIGH")
    confidence:        float  = Field(..., description="Model confidence in predicted class")
    top_factors:       List[str] = Field(..., description="Top 3 contributing risk factors")
    class_probabilities: dict = Field(..., description="Per-class probability breakdown")
    latency_ms:        float


class BatchPredictRequest(BaseModel):
    requests: List[PredictRequest]


class BatchPredictResponse(BaseModel):
    count:   int
    results: List[PredictResponse]


# ══════════════════════════════════════════════════════════════════════
#  STARTUP — verify model exists
# ══════════════════════════════════════════════════════════════════════

@app.on_event("startup")
async def startup_event():
    if not os.path.exists(MODEL_PATH):
        log.warning("Model not found at startup. Run model/train.py first.")
    else:
        _, meta = load_artifacts()
        log.info(f"Model loaded │ type={meta['model_type']} │ test_acc={meta['test_accuracy']}")


# ══════════════════════════════════════════════════════════════════════
#  ROUTES
# ══════════════════════════════════════════════════════════════════════

@app.get("/health", tags=["System"])
def health():
    """Liveness check. Java backend should ping this before calling /predict."""
    model_ok = os.path.exists(MODEL_PATH)
    return {
        "status":    "healthy" if model_ok else "degraded",
        "model_ready": model_ok,
        "service":   "cashflow-risk-ml",
        "version":   "1.0.0",
    }


@app.get("/model/info", tags=["System"])
def model_info():
    """Returns model metadata — feature importances, accuracy, etc."""
    if not os.path.exists(META_PATH):
        raise HTTPException(status_code=503, detail="Model not trained yet. Run train.py.")
    with open(META_PATH) as f:
        return json.load(f)


@app.post("/predict", response_model=PredictResponse, tags=["Prediction"])
def predict_risk(req: PredictRequest):
    """
    **Main prediction endpoint.**

    Called by the Java backend after it computes financial features
    from `monthly_financial_summary`.

    Returns:
    - `risk_probability` — probability of HIGH risk (0–1)
    - `risk_category`    — LOW / MEDIUM / HIGH
    - `confidence`       — model's confidence in its prediction
    - `top_factors`      — 3 human-readable risk drivers
    """
    if not os.path.exists(MODEL_PATH):
        raise HTTPException(status_code=503, detail="Model not ready. Run train.py first.")

    t0 = time.perf_counter()
    try:
        result = predict(req.features.dict())
    except Exception as e:
        log.error(f"Prediction error: {e}")
        raise HTTPException(status_code=500, detail=f"Prediction failed: {str(e)}")

    latency_ms = round((time.perf_counter() - t0) * 1000, 2)
    log.info(
        f"user={req.user_id} month={req.month} → "
        f"{result['risk_category']} p={result['risk_probability']} "
        f"conf={result['confidence']} [{latency_ms}ms]"
    )

    return PredictResponse(
        user_id=req.user_id,
        month=req.month,
        latency_ms=latency_ms,
        **result,
    )


@app.post("/predict/batch", response_model=BatchPredictResponse, tags=["Prediction"])
def predict_batch(req: BatchPredictRequest):
    """
    Batch prediction for multiple users at once.
    Useful for dashboard population or scheduled risk re-scoring.
    """
    if not os.path.exists(MODEL_PATH):
        raise HTTPException(status_code=503, detail="Model not ready.")

    results = []
    for r in req.requests:
        t0 = time.perf_counter()
        result = predict(r.features.dict())
        latency_ms = round((time.perf_counter() - t0) * 1000, 2)
        results.append(PredictResponse(
            user_id=r.user_id,
            month=r.month,
            latency_ms=latency_ms,
            **result,
        ))

    return BatchPredictResponse(count=len(results), results=results)


# ══════════════════════════════════════════════════════════════════════
#  RUN
# ══════════════════════════════════════════════════════════════════════
if __name__ == "__main__":
    import uvicorn
    uvicorn.run("main:app", host="0.0.0.0", port=8000, reload=True)