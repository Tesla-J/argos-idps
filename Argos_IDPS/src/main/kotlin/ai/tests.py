from imports import *

# =====================================================
# 1. CARREGAR MODELOS E METADATA
# =====================================================

print("[ARGOS] A carregar modelos...")

iso_model = joblib.load("/content/iso_forest.pkl")
rf_model = joblib.load("/content/rf_classifier.pkl")
scaler = joblib.load("/content/scaler.pkl")
FEATURE_COLUMNS = joblib.load("/content/features.pkl")
label_encoder = joblib.load("/content/label_encoder.pkl")

print("[ARGOS] Modelos carregados com sucesso")

# =====================================================
# 2. FUNÇÃO DE INFERÊNCIA (CHAMAR A IA)
# =====================================================

def argos_predict(flow_values):
    """
    Recebe:
        flow_values -> lista com valores das features essenciais (SEM Label)
    Retorna:
        dict com decisão da IA
    """
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

    # Classificar ataque conhecido
    rf_pred = rf_model.predict(X_scaled)[0]
    attack_name = label_encoder.inverse_transform([rf_pred])[0]

    return {
        "status": "ANOMALIA",
        "attack_type": attack_name
    }

# =====================================================
# 3. TESTE MANUAL (LINHAS REAIS DO DATASET)
# =====================================================

if __name__ == "__main__":
    print("\n[ARGOS] Teste manual iniciado\n")

    dataset_path = "/content/drive/MyDrive/Datasets/Wednesday-workingHours.pcap_ISCX.csv"
    df = pd.read_csv(dataset_path, low_memory=False)
    df.columns = df.columns.str.strip()

    if "Label" not in df.columns:
        raise ValueError("Coluna 'Label' não encontrada no dataset")

    labels_unicos = df['Label'].unique()
    samples = []

    for label in labels_unicos:
        row = df[df['Label'] == label].iloc[0] 
        sample_features = row[FEATURE_COLUMNS].tolist()
        samples.append((label, sample_features))

    for label, sample_features in samples:
        result = argos_predict(sample_features)
        print(f"--- Teste para Label: {label} ---")
        print("Resultado da IA:")
        print(result)
        print()
