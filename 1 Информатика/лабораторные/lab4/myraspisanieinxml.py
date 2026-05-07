from myraspisanie import load_parsed_yaml


def xml_escape(s: str) -> str:
    return (
        s.replace("\"", "&quot;")
         .replace("'", "&apos;")
    )


def schedule_to_xml(data: dict) -> str:
    lines = []
    lines.append('<?xml version="1.0" encoding="UTF-8"?>')
    lines.append("<schedule>")

    lines.append(f"  <univer>{xml_escape(data.get('univer', ''))}</univer>")
    lines.append(f"  <gripa>{xml_escape(data.get('gripa', ''))}</gripa>")

    rasp = data.get("raspisanie", {})
    lines.append("  <raspisanie>")
    for day_name, groups in rasp.items():
        lines.append(f'    <day name="{xml_escape(day_name)}">')
        for group in groups:
            lines.append(
                f'      <group nedela="{xml_escape(group.get("nedela", ""))}">'
            )
            n=0
            for lesson in group.get("zanatia", []):
                n += 1
                lines.append(f'        <lesson{n}>')
                lines.append(
                    f"          <vremya>{xml_escape(lesson.get('vremya', ''))}</vremya>"
                )
                lines.append(
                    f"          <predmet>{xml_escape(lesson.get('predmet', ''))}</predmet>"
                )
                lines.append(
                    f"          <tip>{xml_escape(lesson.get('tip', ''))}</tip>"
                )
                lines.append(
                    f"          <prepodavatel>{xml_escape(lesson.get('prepodavatel', ''))}</prepodavatel>"
                )
                lines.append(
                    f"          <address>{xml_escape(lesson.get('address', ''))}</address>"
                )
                lines.append(
                    f"          <auditorial>{xml_escape(lesson.get('auditorial', ''))}</auditorial>"
                )
                lines.append(f'        </lesson{n}>')

            lines.append("      </group>")
        lines.append("    </day>")

    lines.append("  </raspisanie>")
    lines.append("</schedule>")

    return "\n".join(lines) + "\n"


parsed_data = load_parsed_yaml()

xml_text = schedule_to_xml(parsed_data)

print(xml_text)

with open("raspisanie.xml", "w", encoding="utf-8") as f:
    f.write(xml_text)