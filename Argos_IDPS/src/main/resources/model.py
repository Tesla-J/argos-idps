#!/usr/bin/env python3

import joblib
import pandas as pd
import os
import sys

# =====================================================
# 1. CARREGAR MODELOS E METADATA
# =====================================================

#print("[ARGOS] A carregar modelos...")

path = '/tmp/argos/'
iso_model = joblib.load(f"{path}iso_forest.pkl")
rf_model = joblib.load(f"{path}rf_classifier.pkl")
scaler = joblib.load(f"{path}scaler.pkl")
FEATURE_COLUMNS = joblib.load(f"{path}features.pkl")
label_encoder = joblib.load(f"{path}label_encoder.pkl")

#print("[ARGOS] Modelos carregados com sucesso")

# =====================================================
# 2. FUNÇÃO DE INFERÊNCIA (CHAMAR A IA)
# =====================================================

def argos_predict(flow_values):
    if len(flow_values) != len(FEATURE_COLUMNS):
        raise ValueError(
            f"Esperado {len(FEATURE_COLUMNS)} valores, recebido {len(flow_values)}"
        )
    X = pd.DataFrame([flow_values], columns=FEATURE_COLUMNS)
    X_scaled = scaler.transform(X)
    iso_pred = iso_model.predict(X_scaled)[0]

    if iso_pred == 1:
        return {
            "status": "NORMAL",
            "attack_type": None
        }

    rf_pred = rf_model.predict(X_scaled)[0]
    attack_name = label_encoder.inverse_transform([rf_pred])[0]

    return {
        "status": "ANOMALIA",
        "attack_type": attack_name
    }

# =====================================================
# 3. Analise das anomalias
# =====================================================

def get_flow_params():
    if len(sys.argv) != 14:
        raise Exception("Invalid number of arguments: 13 were expected")
    flow = []
    for p in sys.argv[1:]:
        flow.append(float(p)) # Yeah, the exception raise threat is intentional
    return flow

def run_analysis():
    print(argos_predict(get_flow_params()))

# =======================================================
#                       LINK START!
# =======================================================

run_analysis()
#print("ANOMALIA")