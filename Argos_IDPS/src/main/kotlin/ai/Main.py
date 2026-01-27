from build import *
from hybrid import *

file_path = 'MachineLearningCSV/'

# 1. Load
df = load_and_clean_data(file_path, subsample_frac=1.0)

# 2. Prepare
X_train, X_test, y_train, y_test, scaler, le = prepare_data(df)

FEATURE_COLUMNS = essential_features
joblib.dump(FEATURE_COLUMNS, 'features.pkl')

# 3. Isolation Forest
benign_label = le.transform(['BENIGN'])[0]
iso_model = train_isolation_forest(X_train, y_train, benign_label)

# 4. Random Forest
rf_model = train_random_forest(X_train, y_train)

# 5. Híbrido
hybrid_detection(iso_model, rf_model, X_test, y_test, le)

# 6. Save models
joblib.dump(iso_model, 'iso_forest.pkl')
joblib.dump(rf_model, 'rf_classifier.pkl')
joblib.dump(scaler, 'scaler.pkl')
joblib.dump(le, 'label_encoder.pkl')

print("\n✅ ARGOS pronto e funcional")