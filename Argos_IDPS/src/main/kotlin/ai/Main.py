import build_data as bld
import supervised as sup
import unsupervised as unsup

folder_path = 'MachineLearningCSV'

df = bld.load_and_clean_data(folder_path)
prepared = bld.prepare_data_for_models(df)

print("\nPreparation summary:")
print("Classes:", prepared['label_encoder'].classes_)
print("X_normal (BENIGN) size:", len(prepared['X_normal_scaled']) if prepared['X_normal_scaled'] is not None else "No BENIGN")

if prepared['X_normal_scaled'] is not None and len(prepared['X_normal_scaled']) > 0:
    print("\n=== Training Isolation Forest (anomaly detection) ===")
    iso_model = unsup.train_anomaly_detector(prepared['X_normal_scaled'], contamination=0.01)
    unsup.save_model(iso_model, 'iso_forest_model.pkl')
    
    test_predictions = unsup.detect_anomalies(iso_model, prepared['X_test_scaled'])
    print("Test predictions example (first 10):", test_predictions[:10])
    
    anomalies_count = (test_predictions == -1).sum()
    total_test = len(test_predictions)
    print(f"Detection summary: {anomalies_count} anomalies in {total_test} samples ({anomalies_count/total_test*100:.1f}%)")
else:
    print("\nError: No BENIGN data for training Isolation Forest.")
    exit(1)

print("\n=== Training Random Forest (attack classification) ===")
clf_model = sup.train_classifier(prepared['X_train_scaled'], prepared['y_train'])
sup.save_classifier(clf_model, 'rf_classifier_model.pkl')

print("\n=== Applying hybrid: classification on anomalies ===")
test_anomalies_mask = test_predictions == -1
if test_anomalies_mask.any():
    X_test_anomalous = prepared['X_test_scaled'][test_anomalies_mask]
    y_test_anomalous = prepared['y_test'][test_anomalies_mask]

    sup.classify_attacks(clf_model, X_test_anomalous, y_test_anomalous, prepared['label_encoder'])
else:
    print("No anomalies detected in test set – all considered normal.")