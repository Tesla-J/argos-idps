import pandas as pd 
import numpy as np 
from sklearn.preprocessing import LabelEncoder, StandardScaler
from sklearn.model_selection import train_test_split
from imblearn.over_sampling import SMOTE


def load_and_clean_data(file_path):
    df = pd.read_csv(file_path)
    df.drop('Unnamed: 0', axis=1, inplace=True)
    # Encode categorical features
    categorical_cols = ['proto', 'service']
    for col in categorical_cols:
        le = LabelEncoder()
        df[col] = le.fit_transform(df[col])
    return df

def prepare_data_for_models(df, test_size=0.2, random_state=42):
    """
    Prepara dados para: não supervisionado (Isolation Forest) e supervisionado (classificação)
    """
    # Encode attack labels
    label_encoder = LabelEncoder()
    df['Attack_encoded'] = label_encoder.fit_transform(df['Attack_type'])
    print("Classes encontradas em Attack_type:", label_encoder.classes_)

    # Extract features and labels
    X = df.drop(['Attack_type', 'Attack_encoded'], axis=1)
    y = df['Attack_encoded']

    # Separate normal data for unsupervised model
    normal_mask = df['Attack_type'] == 'Normal'
    X_normal = X[normal_mask]

    # Balance classes using SMOTE
    smote = SMOTE(random_state=random_state)
    X_balanced, y_balanced = smote.fit_resample(X, y)

    # Split into train/test with stratification
    X_train, X_test, y_train, y_test = train_test_split(
        X_balanced, y_balanced, test_size=test_size, random_state=random_state, stratify=y_balanced
    )

    # Normalize data (fit on train, apply to test)
    scaler = StandardScaler()
    X_train_scaled = scaler.fit_transform(X_train)
    X_test_scaled = scaler.transform(X_test)
    X_normal_scaled = scaler.transform(X_normal) if not X_normal.empty else None

    return {
        'X_normal_scaled': X_normal_scaled,
        'X_train_scaled': X_train_scaled,
        'X_test_scaled': X_test_scaled,
        'y_train': y_train,
        'y_test': y_test,
        'scaler': scaler,
        'label_encoder': label_encoder
    }