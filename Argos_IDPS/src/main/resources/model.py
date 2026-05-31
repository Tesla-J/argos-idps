#!/usr/bin/env python3

import joblib
import pandas as pd
import os
import sys
import socket
import selectors
import types

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

def get_flow_params(data):
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

HOST = 'localhost'
PORT = 3469
BUFFER_SIZE = 1024
sel = selectors.DefaultSelector()

def accept_wrapper(sock):
    conn, addr = sock.accept()
    conn.setblocking(False)
    data = types.SimpleNamespace(addr=addr, inb=b'', outb=b'')
    events = selectors.EVENT_READ | selectors.EVENT_WRITE
    sel.register(conn, events, data=data)

def handle_connection(key, mask):
    sock = key.fileobj
    data = key.data
    if mask & selectors.EVENT_READ:
        received_data = sock.recv(BUFFER_SIZE)
        if received_data:
            received_data = str(received_data)
            try:
                data.outb += argos_predict(list(map(float, received_data.split('|'))))
            except:
                pass
        else:
            sel.unresgister(sock)
            sock.close()
    # todo test with elif
    if mask & selectors.EVENT_WRITE:
        if data.outb:
            sent_count = sock.send(data.outb)
            data.outb = data.outb[sent_count:]

with socket.socket(socket.AF_INET, socket.SOCK_STREAM) as sock:
    sock.bind((HOST, PORT))
    sock.listen()
    sock.setblocking(False)
    sel.register(sock, selectors.EVENT_READ, data=None)
    try:
        while not False:
            events = sel.select(timeout=None)
            for key, mask in events:
                if key.data is None:
                    accept_wrapper(key.fileobj)
                else:
                    handle_connection(key, mask)
    except KeyboardInterrupt:
        print('Exiting...')
    finally:
        sel.close()