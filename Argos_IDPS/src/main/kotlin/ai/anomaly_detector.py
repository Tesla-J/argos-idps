import joblib
from sklearn.ensemble import IsolationForest

def train_anomaly_detector(X_normal_scaled, contamination=0.01, random_state=42):
    if X_normal_scaled is None or len(X_normal_scaled) == 0:
        print("Aviso: Nenhum dado BENIGN para treino. Usando contamination alta.")
        contamination = 0.1 
    
    print("Treinando Isolation Forest...")
    model = IsolationForest(
        contamination=contamination,
        random_state=random_state,
        n_estimators=100,
        max_samples='auto'
    )
    
    model.fit(X_normal_scaled)
    print("Isolation Forest treinado com sucesso!")
    
    return model

def detect_anomalies(model, X_data_scaled):
    predictions = model.predict(X_data_scaled)
    anomalies_count = (predictions == -1).sum()
    print(f"Detectadas {anomalies_count} anomalias em {len(predictions)} amostras.")
    return predictions

def save_model(model, filename='iso_forest_model.pkl'):
    joblib.dump(model, filename)
    print(f"Modelo salvo em {filename}")

def load_model(filename='iso_forest_model.pkl'):
    model = joblib.load(filename)
    print(f"Modelo carregado de {filename}")
    return model