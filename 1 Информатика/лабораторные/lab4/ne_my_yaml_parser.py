def parse_value(v: str):
    v = v.strip()
    if not v:
        return None
    if len(v) >= 2 and v[0] == v[-1] == '"':
        return v[1:-1]
    if v.isdigit():
        return int(v)
    return v

def _clean_lines(text: str):
    res = []
    for raw in text.splitlines():
        line = raw.rstrip()
        if not line.strip():
            continue
        stripped = line.lstrip()
        if stripped.startswith("#"):
            continue
        indent = len(line) - len(stripped)
        res.append((indent, stripped))
    return res

def parse_mapping(lines, i, indent):
    m = {}
    n = len(lines)
    while i < n:
        curr_indent, text = lines[i]
        if curr_indent < indent or ":" not in text:
            break

        key, val = (p.strip() for p in text.split(":", 1))

        if val:
            m[key] = parse_value(val)
            i += 1
        else:
            j = i + 1
            if j >= n or lines[j][0] <= curr_indent:
                m[key] = {}
                i = j
                continue

            next_indent, next_text = lines[j]
            if next_text.startswith("- "):
                lst, i = parse_list(lines, j, next_indent)
                m[key] = lst
            else:
                sub, i = parse_mapping(lines, j, next_indent)
                m[key] = sub

    return m, i

def parse_list(lines, i, indent):
    lst = []
    n = len(lines)
    while i < n:
        curr_indent, text = lines[i]
        if curr_indent < indent or not text.startswith("-"):
            break

        rest = text[1:].strip()

        if ":" in rest:
            key, val = (p.strip() for p in rest.split(":", 1))
            item = {key: parse_value(val) if val else None}
            j = i + 1
            if j < n and lines[j][0] > curr_indent:
                sub, i = parse_mapping(lines, j, lines[j][0])
                item.update(sub)
            else:
                i = j
            lst.append(item)
        else:
            lst.append(parse_value(rest))
            i += 1

    return lst, i

def yaml_parser(content: str):
    lines = _clean_lines(content)
    if not lines:
        return {}
    result, _ = parse_mapping(lines, 0, lines[0][0])
    return result
