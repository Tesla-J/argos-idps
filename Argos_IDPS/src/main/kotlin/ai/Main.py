import data_preprocessor as data

file_path = 'RT_IOT2022.csv'
df = data.load_and_clean_data(file_path)
print(df.head())
prepared = data.prepare_data_for_models(df)
print("Classes:", prepared['label_encoder'].classes_)
print("Tamanho X_normal:", len(prepared['X_normal_scaled']) if prepared['X_normal_scaled'] is not None else "Nenhum Normal")
