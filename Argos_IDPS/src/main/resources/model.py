#!/usr/bin/env python3

import joblib
import numpy as np
import os
import socket
import json
import warnings
from concurrent.futures import ThreadPoolExecutor

warnings.filterwarnings('ignore')

# =====================================================
# 1. CARREGAR MODELOS E METADATA
# =====================================================

path = '/tmp/argos/'
iso_model       = joblib.load(f"{path}iso_forest.pkl")
rf_model        = joblib.load(f"{path}rf_classifier.pkl")
scaler          = joblib.load(f"{path}scaler.pkl")
FEATURE_COLUMNS = joblib.load(f"{path}features.pkl")
label_encoder   = joblib.load(f"{path}label_encoder.pkl")
IF_THRESHOLD    = joblib.load(f"{path}if_threshold.pkl")

# =====================================================
# 2. FUNÇÃO DE INFERÊNCIA
# =====================================================

def argos_predict(flow_values):
    if len(flow_values) != len(FEATURE_COLUMNS):
        raise ValueError(
            f"Esperado {len(FEATURE_COLUMNS)} valores, recebido {len(flow_values)}"
        )

    X        = np.array([flow_values], dtype=float)
    X_scaled = scaler.transform(X)

    # score_samples() devolve pontuação contínua.
    # Score abaixo do threshold calibrado = anomalia.
    # predict() NÃO é usado porque usa contamination fixo do modelo
    # e ignora a calibração feita durante o treino.
    score = iso_model.score_samples(X_scaled)[0]

    if score >= IF_THRESHOLD:
        return {"status": "NORMAL", "attack_type": "Unknown"}

    rf_pred     = rf_model.predict(X_scaled)[0]
    attack_name = label_encoder.inverse_transform([rf_pred])[0]
    return {"status": "ANOMALIA", "attack_type": attack_name}

# =====================================================
# 3. SERVIDOR TCP
# =====================================================

HOST        = 'localhost'
PORT        = 3469
BUFFER_SIZE = 1024

def handle_analysis(conn):
    with conn:
        try:
            data = conn.recv(BUFFER_SIZE)
            if not data:
                return
            result = json.dumps(
                argos_predict(
                    list(map(float, data.decode('utf-8').split('|')))
                )
            ).encode()
            conn.sendall(result)
        except Exception:
            pass

def init():
    workers  = os.cpu_count() * 4
    executor = ThreadPoolExecutor(max_workers=workers)
    with socket.socket(socket.AF_INET, socket.SOCK_STREAM) as sock:
        sock.setsockopt(socket.SOL_SOCKET, socket.SO_REUSEADDR, 1)
        sock.bind((HOST, PORT))
        sock.listen(256)
        while True:
            conn, _ = sock.accept()
            executor.submit(handle_analysis, conn)

if __name__ == '__main__':
    init()