import data_preprocessor as data

files_path = 'GeneratedLabelledFlows/TrafficLabelling /'
df = data.load_and_clean_data(files_path)

prepared = data.prepare_data_for_models(df)

print("Classes:", prepared['label_encoder'].classes_)
print("Tamanho X_normal:", len(prepared['X_normal_scaled']))