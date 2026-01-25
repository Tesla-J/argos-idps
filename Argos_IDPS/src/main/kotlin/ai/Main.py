import build_data as bld
from anomaly_detector import train_anomaly_detector, save_model, detect_anomalies

files_path = 'MachineLearningCSV'

df = bld.load_and_clean_data(files_path)
prepared = bld.prepare_data_for_models(df)

print("Classes:", prepared['label_encoder'].classes_)
print("Tamanho X_normal:", len(prepared['X_normal_scaled']) if prepared['X_normal_scaled'] is not None else "Nenhum BENIGN")

if prepared['X_normal_scaled'] is not None and len(prepared['X_normal_scaled']) > 0:
    iso_model = train_anomaly_detector(prepared['X_normal_scaled'], contamination=0.01)
    save_model(iso_model, 'iso_forest_model.pkl')
    
    test_predictions = detect_anomalies(iso_model, prepared['X_test_scaled'])
    print("Exemplo de predições no teste (primeiros 10):", test_predictions[:10])
else:
    print("Não foi possível treinar o Isolation Forest: sem dados BENIGN.")