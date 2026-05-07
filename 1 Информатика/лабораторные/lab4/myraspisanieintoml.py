import time
from myraspisanie import load_parsed_yaml
def toml_escape(s: str) -> str:
    s = s.replace("\\", "\\\\").replace("\"", "\\\"")
    return "\"" + s + "\""


def schedule_to_toml(data: dict) -> str:
    lines = []
    lines.append(f"univer = {toml_escape(data.get('univer', ''))}")
    lines.append(f"gripa = {toml_escape(data.get('gripa', ''))}")
    lines.append("")
    rasp = data.get("raspisanie", {})

    for day_name, groups in rasp.items():
        for group in groups:
            lines.append(f"[[raspisanie.{day_name}]]")
            lines.append(f"nedela = {toml_escape(group.get('nedela', ''))}")
            lines.append("")

            for lesson in group.get("zanatia", []):
                lines.append(f"[[raspisanie.{day_name}.zanatia]]")
                lines.append(f"vremya = {toml_escape(lesson.get('vremya', ''))}")
                lines.append(f"predmet = {toml_escape(lesson.get('predmet', ''))}")
                lines.append(f"tip = {toml_escape(lesson.get('tip', ''))}")
                lines.append(f"prepodavatel = {toml_escape(lesson.get('prepodavatel', ''))}")
                lines.append(f"address = {toml_escape(lesson.get('address', ''))}")
                lines.append(f"auditorial = {toml_escape(lesson.get('auditorial', ''))}")
                lines.append("")

    return "\n".join(lines).rstrip() + "\n"

parsed_data = load_parsed_yaml()
print(parsed_data)
toml_text = schedule_to_toml(parsed_data)

print(toml_text)

with open("raspisanie.toml", "w", encoding="utf-8") as f:
    f.write(toml_text)
