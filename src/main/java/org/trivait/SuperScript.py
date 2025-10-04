import os

def collect_java_files(start_dir, output_file):
    with open(output_file, "w", encoding="utf-8") as out:
        for root, dirs, files in os.walk(start_dir):
            for file in files:
                if file.endswith(".java"):
                    file_path = os.path.join(root, file)
                    out.write(f"\n\n===== {file_path} =====\n\n")
                    try:
                        with open(file_path, "r", encoding="utf-8") as f:
                            out.write(f.read())
                    except Exception as e:
                        out.write(f"\n[Ошибка чтения файла: {e}]\n")

if __name__ == "__main__":
    current_dir = os.path.dirname(os.path.abspath(__file__))
    output_file = os.path.join(current_dir, "all_classes.txt")
    collect_java_files(current_dir, output_file)
    print(f"Содержимое всех .java файлов сохранено в {output_file}")
