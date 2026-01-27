from imports import *

def train_isolation_forest(X_train, y_train, benign_label, max_benign=100000):
    X_benign = X_train[y_train == benign_label]

    if len(X_benign) > max_benign:
        idx = np.random.choice(len(X_benign), max_benign, replace=False)
        X_benign = X_benign[idx]

    print(f"Isolation Forest treinado com {len(X_benign)} amostras BENIGN")

    iso = IsolationForest(
        n_estimators=100,
        contamination=0.05,
        random_state=42,
        n_jobs=-1
    )

    iso.fit(X_benign)
    return (iso)

def train_random_forest(X_train, y_train):
    print("Treinando Random Forest (sem SMOTE – class_weight balanceado)")

    rf = RandomForestClassifier(
        n_estimators=150,
        random_state=42,
        n_jobs=-1,
        class_weight='balanced'
    )

    rf.fit(X_train, y_train)
    return (rf)

def hybrid_detection(iso, rf, X_test, y_test, label_encoder):
    print("\nDetectando anomalias...")
    iso_pred = iso.predict(X_test)
    anomaly_mask = iso_pred == -1

    print(f"Anomalias detectadas: {anomaly_mask.sum()} / {len(X_test)}")

    if anomaly_mask.sum() == 0:
        print("Nenhuma anomalia detectada.")
        return

    X_anom = X_test[anomaly_mask]
    y_anom = y_test[anomaly_mask]

    print("\nClassificando anomalias conhecidas...")
    y_pred = rf.predict(X_anom)

    print("\nClassification Report:")
    print(classification_report(
        y_anom,
        y_pred,
        target_names=label_encoder.inverse_transform(
            np.unique(y_anom)
        ),
        zero_division=0
    ))

    print("\nConfusion Matrix:")
    print(confusion_matrix(y_anom, y_pred))