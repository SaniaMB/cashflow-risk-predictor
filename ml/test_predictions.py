"""
Smoke test — verifies the model reproduces the seed_data.sql expected outputs.
Run: python3 test_predictions.py
"""
import sys, os
sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
from model.train import predict

print("=" * 60)
print("  Cashflow Risk ML — Prediction Smoke Test")
print("=" * 60)

TEST_CASES = [
    {
        "name": "Aman Stable (Expected: LOW ~0.18)",
        "features": {
            "savings_ratio":             0.3300,
            "emi_ratio":                 0.1667,
            "expense_volatility":        0.04,
            "spending_trend_slope":      0.01,
            "income_irregularity_score": 0.01,
            "expense_to_income_ratio":   0.6700,
        },
        "expected_category": "LOW",
    },
    {
        "name": "Riya Moderate (Expected: MEDIUM ~0.55)",
        "features": {
            "savings_ratio":             0.0833,
            "emi_ratio":                 0.3333,
            "expense_volatility":        0.20,
            "spending_trend_slope":      0.07,
            "income_irregularity_score": 0.02,
            "expense_to_income_ratio":   0.9167,
        },
        "expected_category": "MEDIUM",
    },
    {
        "name": "Karan HighRisk (Expected: HIGH ~0.87)",
        "features": {
            "savings_ratio":            -0.1333,
            "emi_ratio":                 0.5000,
            "expense_volatility":        0.45,
            "spending_trend_slope":      0.10,
            "income_irregularity_score": 0.03,
            "expense_to_income_ratio":   1.1333,
        },
        "expected_category": "HIGH",
    },
]

all_passed = True
for tc in TEST_CASES:
    result = predict(tc["features"])
    passed = result["risk_category"] == tc["expected_category"]
    status = "✅ PASS" if passed else "❌ FAIL"
    if not passed:
        all_passed = False

    print(f"\n{status}  {tc['name']}")
    print(f"   Category   : {result['risk_category']}  (expected {tc['expected_category']})")
    print(f"   Probability: {result['risk_probability']}")
    print(f"   Confidence : {result['confidence']}")
    print(f"   Top Factors: {result['top_factors']}")
    print(f"   Class Proba: {result['class_probabilities']}")

print("\n" + "=" * 60)
print("  Result:", "ALL TESTS PASSED 🎉" if all_passed else "SOME TESTS FAILED ⚠️")
print("=" * 60)