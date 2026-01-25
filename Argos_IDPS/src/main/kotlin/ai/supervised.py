# attack_classifier.py
import joblib
from sklearn.ensemble import RandomForestClassifier
from sklearn.metrics import classification_report, confusion_matrix

def train_classifier(X_train_scaled, y_train, random_state=42):
    """
    Train Random Forest to classify known attack types.
    """
    print("Training Random Forest...")
    model = RandomForestClassifier(
        n_estimators=100,       
        random_state=random_state,
        n_jobs=-1,               
        class_weight='balanced' 
    )
    model.fit(X_train_scaled, y_train)
    print("Random Forest trained successfully!")
    return model

def classify_attacks(model, X_anomalous_scaled, y_anomalous, label_encoder):
    """
    Classify samples flagged as anomalies (-1).
    Displays classification report and confusion matrix.
    """
    if len(X_anomalous_scaled) == 0:
        print("No anomalies to classify.")
        return None
    
    y_pred = model.predict(X_anomalous_scaled)
    
    print("\nClassification Report on Anomalies:")
    print(classification_report(y_anomalous, y_pred, target_names=label_encoder.classes_))
    
    print("\nConfusion Matrix:")
    print(confusion_matrix(y_anomalous, y_pred))
    
    return y_pred

def save_classifier(model, filename='rf_classifier_model.pkl'):
    joblib.dump(model, filename)
    print(f"Classifier saved to {filename}")