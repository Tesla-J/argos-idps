from imports import *

essential_features = [
        'Flow Duration', 'Total Fwd Packets', 'Total Backward Packets',
        'Total Length of Fwd Packets', 'Total Length of Bwd Packets',
        'Fwd Packet Length Max', 'Fwd Packet Length Min', 'Fwd Packet Length Mean', 'Fwd Packet Length Std',
        'Bwd Packet Length Max', 'Bwd Packet Length Min', 'Bwd Packet Length Mean', 'Bwd Packet Length Std',
        'Flow Bytes/s', 'Flow Packets/s', 'Flow IAT Mean', 'Flow IAT Std', 'Flow IAT Max', 'Flow IAT Min',
        'Fwd IAT Total', 'Fwd IAT Mean', 'Fwd IAT Std', 'Fwd IAT Max', 'Fwd IAT Min',
        'Bwd IAT Total', 'Bwd IAT Mean', 'Bwd IAT Std', 'Bwd IAT Max', 'Bwd IAT Min',
        'Fwd PSH Flags', 'Bwd PSH Flags', 'Fwd URG Flags', 'Bwd URG Flags',
        'FIN Flag Count', 'SYN Flag Count', 'RST Flag Count', 'PSH Flag Count', 'ACK Flag Count', 'URG Flag Count',
        'Average Packet Size', 'Min Packet Length', 'Max Packet Length', 'Packet Length Mean', 'Packet Length Std'
    ]

def load_and_clean_data(filepath, subsample_frac=1.0):
    print(f"Loading file: {filepath}")
    df = pd.read_csv(filepath, low_memory=False)

    # Limpeza básica
    df.columns = df.columns.str.strip()
    df.replace([np.inf, -np.inf], np.nan, inplace=True)
    df.dropna(inplace=True)

    # Subamostragem
    if subsample_frac < 1.0:
        df = df.sample(frac=subsample_frac, random_state=42)
    df = df[essential_features + ['Label']]

    print(f"Dataset final com features essenciais: {df.shape[0]} linhas | {df.shape[1]} colunas")
    print("Classes:", df['Label'].unique())
    return (df)

def prepare_data(df, test_size=0.2):
    le = LabelEncoder()
    df['Label_enc'] = le.fit_transform(df['Label'])

    X = df.drop(['Label', 'Label_enc'], axis=1)
    y = df['Label_enc']

    X_train, X_test, y_train, y_test = train_test_split(
        X, y,
        test_size=test_size,
        random_state=42,
        stratify=y
    )

    scaler = StandardScaler()
    X_train_scaled = scaler.fit_transform(X_train)
    X_test_scaled = scaler.transform(X_test)

    return (X_train_scaled, X_test_scaled, y_train, y_test, scaler, le)