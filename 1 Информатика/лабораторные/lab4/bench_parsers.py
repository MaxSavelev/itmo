import time
import statistics as st
import yaml

from my_yaml_parser import yaml_parser as my_parser


def bench(name: str, func, content: str, n=100, warmup=5):
    for _ in range(warmup):
        func(content)

    times = []
    for _ in range(n):
        start = time.perf_counter()
        func(content)
        end = time.perf_counter()
        times.append(end - start)

    print(f"\n{name}")
    print(f"  Среднее:  {sum(times) / n:.6f} сек")
    print(f"  Медиана:  {st.median(times):.6f} сек")
    print(f"  Мин:      {min(times):.6f} сек")
    print(f"  Макс:     {max(times):.6f} сек")


def pyyaml_parser(content: str):
    return yaml.safe_load(content)


def main():
    path = "raspisanie.yaml"
    with open(path, "r", encoding="utf-8") as f:
        content = f.read()

    bench("Мой парсер", my_parser, content, n=100, warmup=5)
    bench("PyYAML safe_load", pyyaml_parser, content, n=100, warmup=5)


if __name__ == "__main__":
    main()
