#!/usr/bin/env python3
"""Clasifica cada item de data/items_raw.csv en una categoria con reglas ordenadas (rules/categories.toml).

La primera regla que coincide gana. data/categories_manual.csv (item_id,category) gana siempre.
Escribe data/categories.csv (item_id,category,reason) con una fila por item de items_raw.csv; reason es el
nombre de la regla (o "manual"). Lo que no coincide con ninguna regla es `unclassified` y se cuenta.
  --sample N   imprime N ejemplos por categoria (por defecto 0)
  --show CAT   imprime todos los items de esa categoria con la regla que los puso ahi
"""
import argparse
import fnmatch
import os
import re
import sys

import common as c

CATEGORIES = ["water", "isotonic", "drink_simple", "drink_crafted", "alcohol", "soup", "stew", "meal",
              "fresh_produce", "dry", "baked_sweet", "sweet", "meat_cooked", "meat_raw", "salty", "seafood",
              "rotten_or_poison", "ingredient", "non_food", "unclassified"]


def parse_toml_subset(text):
    """Lo que usa categories.toml ([[rule]], cadenas, booleanos, listas de cadenas, comentarios de linea entera),
    para Python < 3.11 que no trae tomllib."""
    doc, cur = {}, None
    for n, line in enumerate(text.splitlines(), 1):
        line = line.strip()
        if not line or line.startswith("#"):
            continue
        if line.startswith("[["):
            cur = {}
            doc.setdefault(line.strip("[] "), []).append(cur)
            continue
        m = re.match(r'^([A-Za-z_]+)\s*=\s*(.+)$', line)
        if not m or cur is None:
            raise ValueError("categories.toml linea %d no entendida: %s" % (n, line))
        v = m.group(2).strip()
        if v in ("true", "false"):
            cur[m.group(1)] = v == "true"
        elif v.startswith("["):
            cur[m.group(1)] = re.findall(r'"((?:[^"\\]|\\.)*)"', v)
        elif v.startswith('"') and v.endswith('"'):
            cur[m.group(1)] = v[1:-1]
        else:
            raise ValueError("categories.toml linea %d: valor no soportado: %s" % (n, v))
    return doc


def load_rules():
    path = os.path.join(c.RULES, "categories.toml")
    try:
        import tomllib
        with open(path, "rb") as f:
            doc = tomllib.load(f)
    except ImportError:
        with open(path, encoding="utf-8") as f:
            doc = parse_toml_subset(f.read())
    rules = doc["rule"]
    for r in rules:
        if r["category"] not in CATEGORIES:
            sys.exit("regla %s: categoria desconocida %s" % (r.get("name"), r["category"]))
        r.setdefault("name", r["category"])
    return rules


class Item:
    __slots__ = ("id", "mod", "tokens", "tags", "effects", "is_drink", "has_food")

    def __init__(self, row):
        self.id = row["id"]
        self.mod = row["mod"]
        self.tokens = c._tokens(self.id.split(":", 1)[1])
        self.tags = set(filter(None, row["tags"].split(";")))
        self.effects = {e.split(",")[0] for e in filter(None, row["effects"].split(";"))}
        self.is_drink = row["is_drink"] == "true"
        self.has_food = row["nutrition"] != ""


def _any_glob(patterns, values):
    return any(fnmatch.fnmatchcase(v, p) for p in patterns for v in values)


def matches(rule, it):
    """Todas las condiciones presentes deben cumplirse; dentro de una lista basta una."""
    if "is_drink" in rule and rule["is_drink"] != it.is_drink:
        return False
    if "has_food" in rule and rule["has_food"] != it.has_food:
        return False
    if "mods" in rule and it.mod not in rule["mods"]:
        return False
    if "ids" in rule and it.id not in rule["ids"]:
        return False
    if "tags" in rule and not _any_glob(rule["tags"], it.tags):
        return False
    if "not_tags" in rule and _any_glob(rule["not_tags"], it.tags):
        return False
    if "effects" in rule and not _any_glob(rule["effects"], it.effects):
        return False
    if "not_effects" in rule and _any_glob(rule["not_effects"], it.effects):
        return False
    if "tokens" in rule and not (set(rule["tokens"]) & it.tokens):
        return False
    if "not_tokens" in rule and (set(rule["not_tokens"]) & it.tokens):
        return False
    if "all_tokens" in rule and not set(rule["all_tokens"]) <= it.tokens:
        return False
    if "also_tokens" in rule and not (set(rule["also_tokens"]) & it.tokens):
        return False
    if "only_tokens" in rule and not it.tokens <= set(rule["only_tokens"]):
        return False
    return True


def classify(it, rules):
    for r in rules:
        if matches(r, it):
            return r["category"], r["name"]
    return "unclassified", "no_rule"


def main():
    ap = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    ap.add_argument("--raw", default=os.path.join(c.DATA, "items_raw.csv"))
    ap.add_argument("--sample", type=int, default=0)
    ap.add_argument("--show")
    args = ap.parse_args()

    rules = load_rules()
    manual = {r["item_id"]: r["category"].strip() for r in c.read_csv(os.path.join(c.DATA, "categories_manual.csv"))
              if r.get("item_id")}
    for i, cat in manual.items():
        if cat not in CATEGORIES:
            sys.exit("categories_manual.csv: %s tiene la categoria desconocida '%s'" % (i, cat))
    rows = c.read_csv(args.raw)
    out, by_cat = [], {}
    for row in rows:
        it = Item(row)
        cat, why = (manual[it.id], "manual") if it.id in manual else classify(it, rules)
        out.append({"item_id": it.id, "category": cat, "reason": why})
        by_cat.setdefault(cat, []).append((it.id, why, row))
    changed = c.write_csv(os.path.join(c.DATA, "categories.csv"), out, ["item_id", "category", "reason"])
    cand = lambda row: row["nutrition"] != "" or row["is_drink"] == "true"
    print("%s data/categories.csv: %d items (%d comida o bebida)" % (
        "escrito" if changed else "sin cambios:", len(out), sum(1 for r in rows if cand(r))))
    for cat in CATEGORIES:
        items = by_cat.get(cat, [])
        n_food = sum(1 for _, _, r in items if cand(r))
        print("  %-17s %5d  (comida/bebida: %d)" % (cat, len(items), n_food))
        if args.sample and cat != "non_food":
            step = max(1, len(items) // args.sample)
            for i, why, _ in items[::step][:args.sample]:
                print("      %-50s %s" % (i, why))
    if args.show:
        for i, why, r in by_cat.get(args.show, []):
            print("%-52s %-24s %s" % (i, why, r["tags"][:70]))
    un = sum(1 for _, _, r in by_cat.get("unclassified", []) if cand(r))
    print("sin clasificar (comida o bebida): %d" % un)


if __name__ == "__main__":
    sys.exit(main())
