import data_preprocessor as data

file_path = 'RT_IOT2022.csv'
df = data.load_and_clean_data(file_path)
print(df.head())

