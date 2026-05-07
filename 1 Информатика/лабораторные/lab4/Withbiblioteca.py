import yaml
import tomli_w
import time

def yaml_to_dict(path: str) -> dict:
    with open(path, "r", encoding="utf-8") as f:
        data = yaml.safe_load(f)
    return data

def dict_to_toml(data: dict, path: str) -> None:
    with open(path, "wb") as f:
        tomli_w.dump(data, f)

def main():
    parsed_data = yaml_to_dict("raspisanie.yaml")
    dict_to_toml(parsed_data, "raspisanie_lib.toml")
    N = 100
    times = []

    for _ in range(5):
        yaml_to_dict("raspisanie.yaml")

    for _ in range(N):
        start = time.perf_counter()
        yaml_to_dict("raspisanie.yaml")
        end = time.perf_counter()
        times.append(end - start)

    print(f"Парсинг YAML:")
    print(f"Среднее: {sum(times) / N:.6f} сек")
    print(f"Минимум: {min(times):.6f} сек")
    print(f"Максимум: {max(times):.6f} сек")

if __name__ == "__main__":
    main()
