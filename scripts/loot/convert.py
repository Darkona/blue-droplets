#!/usr/bin/env python3
"""Pasa tablas de botin y modificadores globales de botin al formato de Minecraft 26.3, en el sitio e idempotente.

Minecraft 26.3 cambio las claves de botin, y las viejas se ignoran sin error (las funciones y condiciones no se
aplican). En cada objeto, de forma recursiva:

  "functions": [..]              -> "modifier": [..] (una sola funcion queda como objeto)
  "conditions": [{..}]           -> "condition": {..} (varias: {"type": "minecraft:all_of", "terms": [..]})
  "function": "x" / "condition": "x"  -> "type": "x" (la clave que elige el tipo de funcion o condicion)

"neoforge:conditions" (condiciones de carga del fichero entero) no cambia.

  python3 scripts/loot/convert.py FICHERO_O_CARPETA...   (sin argumentos: los datos de Droplets of Thirst)
  --check   no escribe: sale con 1 si algun fichero cambiaria
"""
import argparse
import json
import os
import sys

REPO = os.path.dirname(os.path.dirname(os.path.dirname(os.path.abspath(__file__))))
DEFAULT = [os.path.join(REPO, "src", "main", "resources", "data", "droplets_of_thirst", d) for d in ("loot_table", "loot_modifiers")]


def convert(node):
    if isinstance(node, list):
        return [convert(n) for n in node]
    if not isinstance(node, dict):
        return node
    out = {}
    for key, value in node.items():
        if key == "functions" and isinstance(value, list):
            value = convert(value)
            out["modifier"] = value[0] if len(value) == 1 else value
        elif key == "conditions" and isinstance(value, list):
            value = convert(value)
            out["condition"] = value[0] if len(value) == 1 else {"type": "minecraft:all_of", "terms": value}
        elif key in ("function", "condition") and isinstance(value, str):
            out["type"] = value
        else:
            out[key] = convert(value) if key != "neoforge:conditions" else value
    return out


def files(paths):
    for path in paths:
        if os.path.isdir(path):
            for root, _, names in os.walk(path):
                for name in sorted(names):
                    if name.endswith(".json"):
                        yield os.path.join(root, name)
        else:
            yield path


def main():
    ap = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    ap.add_argument("paths", nargs="*")
    ap.add_argument("--check", action="store_true")
    args = ap.parse_args()
    changed = 0
    for path in files(args.paths or DEFAULT):
        with open(path, encoding="utf-8") as f:
            data = json.load(f)
        new = convert(data)
        if new == data:
            continue
        changed += 1
        print(("cambiaria: " if args.check else "escrito: ") + os.path.relpath(path, REPO))
        if not args.check:
            with open(path, "w", encoding="utf-8") as f:
                f.write(json.dumps(new, indent=2, ensure_ascii=False) + "\n")
    if not changed:
        print("sin cambios")
    return 1 if args.check and changed else 0


if __name__ == "__main__":
    sys.exit(main())
