import pandas as pd
import numpy as np
import glob
import os
from sklearn.preprocessing import LabelEncoder, StandardScaler
from sklearn.model_selection import train_test_split
from imblearn.over_sampling import SMOTE


def load_and_clean_data(folder_path):
    if not os.path.isdir(folder_path):
        raise FileNotFoundError(f"The folder {folder_path} does not exist or is not valid.")

    files = glob.glob(os.path.join(folder_path, "*.csv"))
    if not files:
        raise FileNotFoundError("No CSV files found in the folder.")

    print(f"Found {len(files)} CSV files:")
    for f in files:
        print(" -", f)

    df_list = []
    for file in files:
        tmp = pd.read_csv(file, low_memory=False)
        df_list.append(tmp)

    df = pd.concat(df_list, ignore_index=True)
    df.columns = df.columns.str.strip()

    # Limpeza
    df.replace([np.inf, -np.inf], np.nan, inplace=True)
    df.dropna(inplace=True)

    print(f"Combined dataset: {len(df)} rows, {len(df.columns)} columns after cleaning.")

    return df


def prepare_data_for_models(df, test_size=0.2, random_state=42):
    # Encode Label
    label_encoder = LabelEncoder()
    df['Attack_encoded'] = label_encoder.fit_transform(df['Label'])
    print("Classes found in Label:", label_encoder.classes_)

    X = df.drop(['Label', 'Attack_encoded'], axis=1)
    y = df['Attack_encoded']

    # BENIGN
    normal_mask = df['Label'] == 'BENIGN'
    X_normal = X[normal_mask]

    print(f"'BENIGN' samples found: {len(X_normal)} rows.")

    # SMOTE
    smote = SMOTE(random_state=random_state)
    X_balanced, y_balanced = smote.fit_resample(X, y)

    print("After SMOTE class distribution:")
    print(pd.Series(y_balanced).value_counts())

    # Train / test split
    X_train, X_test, y_train, y_test = train_test_split(
        X_balanced,
        y_balanced,
        test_size=test_size,
        random_state=random_state,
        stratify=y_balanced
    )

    # Scaling
    scaler = StandardScaler()
    X_train_scaled = scaler.fit_transform(X_train)
    X_test_scaled = scaler.transform(X_test)
    X_normal_scaled = scaler.transform(X_normal)

    return {
        'X_normal_scaled': X_normal_scaled,
        'X_train_scaled': X_train_scaled,
        'X_test_scaled': X_test_scaled,
        'y_train': y_train,
        'y_test': y_test,
        'scaler': scaler,
        'label_encoder': label_encoder
    }
