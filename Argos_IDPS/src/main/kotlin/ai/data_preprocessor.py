import pandas as pd 
import numpy as np 
from sklearn.preprocessing import LabelEncoder, StandardScaler
from sklearn.model_selection import train_test_split
from imblearn.over_sampling import SMOTE


def load_and_clean_data(file_path):
    df = pd.read_csv(file_path)  # Por quê? Carrega o CSV em DataFrame. file_path = 'teu_csv.csv'.
    df.drop('Unnamed: 0', axis=1, inplace=True)  # Por quê? Remove coluna inútil (índice gerado).
    categorical_cols = ['proto', 'service']  # Por quê? Essas são object (strings) no teu info – precisam encode.
    for col in categorical_cols:
        le = LabelEncoder()
        df[col] = le.fit_transform(df[col])  # Por quê? Converte strings para ints (ex.: 'tcp' = 0, 'udp' = 1).
    return df

def prepare_data_for_models(df, test_size=0.2, random_state=42):
    """
    Prepara os dados para os dois modelos:
    - Para não supervisionado: só dados 'Normal'
    - Para supervisionado: todos os dados, balanceados e divididos
    Retorna: X_normal (para Isolation Forest), X_train_sup, X_test_sup, y_train_sup, y_test_sup
    """
    # Passo 1: Encode o label 'Attack_type' (string -> número)
    label_encoder = LabelEncoder()
    df['Attack_encoded'] = label_encoder.fit_transform(df['Attack_type'])  # Por quê? ML precisa de números, não strings. Depois usamos inverse_transform para voltar ao nome (ex.: 0 -> 'Normal', 1 -> 'MQTT_Publish').
    print("Classes encontradas em Attack_type:", label_encoder.classes_)  # Mostra as 12 classes para confirmares (Normal + 11 ataques típicos do RT-IoT2022).

    # Passo 2: Separar features (X) e label (y) para supervisionado
    X = df.drop(['Attack_type', 'Attack_encoded'], axis=1)  # Por quê? Remove as colunas de label (não queremos que o modelo "veja" o label durante treino).
    y = df['Attack_encoded']  # Label numérico para supervisionado.

    # Passo 3: Filtrar só 'Normal' para não supervisionado
    normal_mask = df['Attack_type'] == 'Normal'  # Ou o nome exato da classe benign (confirma com o print acima).
    X_normal = X[normal_mask]  # Por quê? Isolation Forest aprende só o "normal". Se não tiver 'Normal' no teu subsample, avisa – usamos contamination=0.1 para estimar.

    # Passo 4: Balancear classes para supervisionado (se desbalanceado)
    smote = SMOTE(random_state=random_state)
    X_balanced, y_balanced = smote.fit_resample(X, y)  # Por quê? Cria amostras sintéticas para classes minoritárias (ex.: ataques raros). Evita que o modelo ignore classes pequenas e só preveja 'Normal' ou 'MQTT_Publish'.

    # Passo 5: Dividir em treino/teste para supervisionado
    X_train, X_test, y_train, y_test = train_test_split(
        X_balanced, y_balanced, test_size=test_size, random_state=random_state, stratify=y_balanced
    )  # stratify=y_balanced garante que as classes fiquem balanceadas em treino e teste.

    # Passo 6: Normalizar (fit só no treino, transform em teste)
    scaler = StandardScaler()
    X_train_scaled = scaler.fit_transform(X_train)
    X_test_scaled = scaler.transform(X_test)
    X_normal_scaled = scaler.transform(X_normal) if not X_normal.empty else None  # Para não supervisionado

    return {
        'X_normal_scaled': X_normal_scaled,  # Para Isolation Forest
        'X_train_scaled': X_train_scaled,
        'X_test_scaled': X_test_scaled,
        'y_train': y_train,
        'y_test': y_test,
        'scaler': scaler,
        'label_encoder': label_encoder
    }