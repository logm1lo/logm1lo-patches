#!/usr/bin/env python3
import json, re, sys
from collections import defaultdict

def patches_table(data):
    patches = data.get("patches", [])
    grouped = defaultdict(list)
    for p in patches:
        for cp in p.get("compatiblePackages", []):
            grouped[cp.get("packageName", "unknown")].append((cp, p))

    lines = []
    lines.append('<details open><summary><b>Available Patches</b></summary>\n')
    for pkg, entries in sorted(grouped.items()):
        app_name = entries[0][0].get("appName", pkg)
        lines.append(f'### {app_name} (`{pkg}`)\n')
        lines.append('| Patch | Description | Versions |')
        lines.append('|-------|-------------|----------|')
        for compat, patch in entries:
            name = patch.get("name", "?")
            desc = patch.get("description", "")
            targets = ", ".join(t.get("version", "any") for t in compat.get("targets", []))
            experimental = " (exp)" if any(t.get("isExperimental") for t in compat.get("targets", [])) else ""
            lines.append(f'| {name} | {desc} | {targets}{experimental} |')
        lines.append("")
    lines.append("</details>")
    return "\n".join(lines)

def main():
    with open("patches-list.json") as f:
        data = json.load(f)
    table = patches_table(data)
    readme = "README.md"
    with open(readme) as f:
        content = f.read()
    pattern = r"<!-- PATCHES_START.*?-->.*?<!-- PATCHES_END -->"
    replacement = f"<!-- PATCHES_START -->\n{table}\n<!-- PATCHES_END -->"
    new_content = re.sub(pattern, replacement, content, flags=re.DOTALL)
    with open(readme, "w") as f:
        f.write(new_content)
    print(f"Updated {readme} with patches table")

if __name__ == "__main__":
    main()
