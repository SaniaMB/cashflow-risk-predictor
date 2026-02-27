"""
===========================================================
  Cashflow Risk Predictor — Model Training
  Role: ML Engineer (Python)
  Hackathon Project: AI-Based Personal Cashflow Risk Predictor
===========================================================

  FEATURES (computed by Backend / sent from DB):
    - savings_ratio
    - emi_ratio
    - expense_volatility
    - spending_trend_slope
    - income_irregularity_score
    - expense_to_income_ratio  (total_expense / total_income)

  OUTPUTS:
    - risk_probability   (0.0 – 1.0)
    - risk_category      (LOW / MEDIUM / HIGH)
    - confidence         (0.0 – 1.0)
    - top_factors        (list of top 3 feature names)
"""

import os
import json
import joblib
import numpy as np
import pandas as pd
from sklearn.ensemble import GradientBoostingClassifier
from sklearn.preprocessing import LabelEncoder
from sklearn.model_selection import train_test_split, cross_val_score
from sklearn.metrics import classification_report, accuracy_score

# ── Paths ──────────────────────────────────────────────────────────────
BASE_DIR   = os.path.dirname(os.path.abspath(__file__))
MODEL_PATH = os.path.join(BASE_DIR, "risk_model.pkl")
META_PATH  = os.path.join(BASE_DIR, "model_meta.json")

# ── Feature names (MUST match what API receives from backend) ──────────
FEATURE_COLS = [
    "savings_ratio",
    "emi_ratio",
    "expense_volatility",
    "spending_trend_slope",
    "income_irregularity_score",
    "expense_to_income_ratio",
]

LABEL_MAP = {"LOW": 0, "MEDIUM": 1, "HIGH": 2}
LABEL_INV = {v: k for k, v in LABEL_MAP.items()}


# ══════════════════════════════════════════════════════════════════════
#  STEP 1 – Synthetic Training Data
#  Generated to match the DB seed patterns + realistic variations.
#  In production: replace / augment with real rows from monthly_financial_summary.
# ══════════════════════════════════════════════════════════════════════
def generate_training_data(n_per_class: int = 500, seed: int = 42) -> pd.DataFrame:
    rng = np.random.default_rng(seed)

    def jitter(arr, scale):
        return arr + rng.normal(0, scale, len(arr))

    n = n_per_class
    records = []

    # ── LOW RISK ────────────────────────────────────────────────────────
    low = pd.DataFrame({
        "savings_ratio":            jitter(np.linspace(0.30, 0.40, n), 0.03).clip(0.20, 0.60),
        "emi_ratio":                jitter(np.full(n, 0.17), 0.03).clip(0.05, 0.30),
        "expense_volatility":       jitter(np.full(n, 0.04), 0.01).clip(0.01, 0.10),
        "spending_trend_slope":     jitter(np.full(n, 0.00), 0.01).clip(-0.05, 0.05),
        "income_irregularity_score":jitter(np.full(n, 0.01), 0.005).clip(0.0, 0.05),
        "expense_to_income_ratio":  jitter(np.full(n, 0.67), 0.04).clip(0.40, 0.80),
        "label": "LOW"
    })

    # ── MEDIUM RISK ─────────────────────────────────────────────────────
    med = pd.DataFrame({
        "savings_ratio":            jitter(np.linspace(0.08, 0.25, n), 0.04).clip(0.0, 0.35),
        "emi_ratio":                jitter(np.full(n, 0.33), 0.05).clip(0.15, 0.50),
        "expense_volatility":       jitter(np.full(n, 0.15), 0.04).clip(0.05, 0.30),
        "spending_trend_slope":     jitter(np.full(n, 0.04), 0.02).clip(0.0, 0.12),
        "income_irregularity_score":jitter(np.full(n, 0.02), 0.01).clip(0.0, 0.08),
        "expense_to_income_ratio":  jitter(np.full(n, 0.88), 0.06).clip(0.65, 1.05),
        "label": "MEDIUM"
    })

    # ── HIGH RISK ───────────────────────────────────────────────────────
    high = pd.DataFrame({
        "savings_ratio":            jitter(np.linspace(-0.13, 0.05, n), 0.04).clip(-0.30, 0.15),
        "emi_ratio":                jitter(np.full(n, 0.50), 0.06).clip(0.30, 0.80),
        "expense_volatility":       jitter(np.full(n, 0.35), 0.06).clip(0.15, 0.60),
        "spending_trend_slope":     jitter(np.full(n, 0.08), 0.02).clip(0.03, 0.20),
        "income_irregularity_score":jitter(np.full(n, 0.03), 0.01).clip(0.0, 0.10),
        "expense_to_income_ratio":  jitter(np.full(n, 1.10), 0.08).clip(0.90, 1.50),
        "label": "HIGH"
    })

    df = pd.concat([low, med, high], ignore_index=True).sample(frac=1, random_state=seed)
    return df


# ══════════════════════════════════════════════════════════════════════
#  STEP 2 – Train
# ══════════════════════════════════════════════════════════════════════
def train():
    print("━" * 55)
    print("  Cashflow Risk Predictor — Training Pipeline")
    print("━" * 55)

    df = generate_training_data(n_per_class=600)
    print(f"[DATA]  Rows: {len(df)} | Class dist:\n{df['label'].value_counts().to_string()}\n")

    X = df[FEATURE_COLS].values
    y = df["label"].map(LABEL_MAP).values

    X_train, X_test, y_train, y_test = train_test_split(
        X, y, test_size=0.20, random_state=42, stratify=y
    )

    model = GradientBoostingClassifier(
        n_estimators=200,
        learning_rate=0.08,
        max_depth=4,
        min_samples_leaf=5,
        subsample=0.85,
        random_state=42,
    )
    model.fit(X_train, y_train)

    # ── Evaluation ─────────────────────────────────────────────────────
    y_pred = model.predict(X_test)
    acc    = accuracy_score(y_test, y_pred)
    cv     = cross_val_score(model, X, y, cv=5, scoring="accuracy")

    print("[EVAL]  Test Accuracy : {:.4f}".format(acc))
    print("[EVAL]  CV Accuracy   : {:.4f} ± {:.4f}".format(cv.mean(), cv.std()))
    print("\n[REPORT]\n", classification_report(y_test, y_pred, target_names=["LOW", "MEDIUM", "HIGH"]))

    # ── Feature importances ─────────────────────────────────────────────
    fi = dict(zip(FEATURE_COLS, model.feature_importances_.tolist()))
    print("[FEATURES]")
    for k, v in sorted(fi.items(), key=lambda x: -x[1]):
        print(f"   {k:<35} {v:.4f}")

    # ── Save artifacts ──────────────────────────────────────────────────
    joblib.dump(model, MODEL_PATH)

    meta = {
        "feature_cols":        FEATURE_COLS,
        "label_map":           LABEL_MAP,
        "label_inv":           LABEL_INV,
        "feature_importances": fi,
        "test_accuracy":       round(acc, 4),
        "cv_mean":             round(float(cv.mean()), 4),
        "cv_std":              round(float(cv.std()), 4),
        "model_type":          "GradientBoostingClassifier",
    }
    with open(META_PATH, "w") as f:
        json.dump(meta, f, indent=2)

    print(f"\n[SAVED] Model → {MODEL_PATH}")
    print(f"[SAVED] Meta  → {META_PATH}")
    print("━" * 55)


# ══════════════════════════════════════════════════════════════════════
#  STEP 3 – Predict helper (used by FastAPI)
# ══════════════════════════════════════════════════════════════════════
def load_artifacts():
    model = joblib.load(MODEL_PATH)
    with open(META_PATH) as f:
        meta = json.load(f)
    return model, meta


def predict(features: dict) -> dict:
    """
    features: dict with keys matching FEATURE_COLS
    Returns:  risk_probability, risk_category, confidence, top_factors
    """
    model, meta = load_artifacts()
    label_inv   = {int(k): v for k, v in meta["label_inv"].items()}
    fi          = meta["feature_importances"]

    row = np.array([[features[c] for c in FEATURE_COLS]])

    proba      = model.predict_proba(row)[0]          # [p_LOW, p_MED, p_HIGH]
    pred_class = int(np.argmax(proba))
    confidence = float(proba[pred_class])
    risk_prob  = float(proba[2])                       # probability of HIGH
    risk_cat   = label_inv[pred_class]

    # top factors = feature importances, ranked descending
    top_factors = [k for k, _ in sorted(fi.items(), key=lambda x: -x[1])][:3]

    # Human-readable factor labels
    label_map_hr = {
        "savings_ratio":             "Low / Negative savings ratio",
        "emi_ratio":                 "High EMI burden",
        "expense_volatility":        "High expense volatility",
        "spending_trend_slope":      "Rapidly rising spending trend",
        "income_irregularity_score": "Irregular income pattern",
        "expense_to_income_ratio":   "High expense-to-income ratio",
    }
    top_factors_hr = [label_map_hr.get(f, f) for f in top_factors]

    return {
        "risk_probability": round(risk_prob, 4),
        "risk_category":    risk_cat,
        "confidence":       round(confidence, 4),
        "top_factors":      top_factors_hr,
        # raw proba breakdown (bonus)
        "class_probabilities": {
            "LOW":    round(float(proba[0]), 4),
            "MEDIUM": round(float(proba[1]), 4),
            "HIGH":   round(float(proba[2]), 4),
        },
    }


if __name__ == "__main__":
    train()