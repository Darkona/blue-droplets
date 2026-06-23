#!/usr/bin/env python3
"""Escribe data/REPORT.md, data/REPORT_recipes.csv y data/REPORT_items.csv desde recipes.csv e items.csv."""
import os
import sys

import common as c

RFIELDS = ["mod", "recipe_id", "recipe_type", "water_form", "water_amount", "result_id", "heat", "min_purity", "status", "notes"]
IFIELDS = ["mod", "item_id", "kind", "craft_steps", "thirst", "quenched", "salty", "no_thirst", "category", "rule", "notes"]


def cell(v):
    return str(v).replace("|", "\\|").replace("\n", " ")


def table(rows, fields):
    out = ["| " + " | ".join(fields) + " |", "|" + "|".join("---" for _ in fields) + "|"]
    for r in rows:
        out.append("| " + " | ".join(cell(r.get(f, "")) for f in fields) + " |")
    return out


def main():
    wr = c.load_json(os.path.join(c.RULES, "water.json"))
    recipes = c.read_csv(os.path.join(c.DATA, "recipes.csv"))
    items = c.read_csv(os.path.join(c.DATA, "items.csv"))
    for r in recipes:
        if not c.yes(r["enabled"]):
            r["status"] = "no se sobrescribe"
        elif r["recipe_type"] not in wr["supported_types"]:
            r["status"] = "tipo no soportado"
        else:
            r["status"] = "sobrescrita"
    lines = ["# Informe de la compat con Farmer's Delight", "",
             "Generado por `scripts/delight/report.py` desde `recipes.csv` e `items.csv`. No se edita a mano.", ""]
    n_ok = sum(1 for r in recipes if r["status"] == "sobrescrita")
    lines += ["## Recetas de agua", "",
              "%d recetas con agua, %d se sobrescriben." % (len(recipes), n_ok), ""]
    by_mod = {}
    for r in recipes:
        by_mod.setdefault(r["mod"], []).append(r)
    lines += ["| mod | con agua | sobrescritas |", "|---|---|---|"]
    for m, rs in sorted(by_mod.items()):
        lines.append("| %s | %d | %d |" % (m, len(rs), sum(1 for r in rs if r["status"] == "sobrescrita")))
    lines.append("")
    lines += table(recipes, RFIELDS) + [""]
    lines += ["## Items", "", "%d items (comida y bebida)." % len(items), ""]
    lines += table(sorted(items, key=lambda r: (r["mod"], r["item_id"])), IFIELDS) + [""]
    text = "\n".join(lines)
    changed = False
    for name, body in (("REPORT.md", text),):
        p = os.path.join(c.DATA, name)
        old = open(p, encoding="utf-8").read() if os.path.exists(p) else None
        if old != body:
            open(p, "w", encoding="utf-8", newline="").write(body)
            changed = True
    changed |= c.write_csv(os.path.join(c.DATA, "REPORT_recipes.csv"), recipes, RFIELDS)
    changed |= c.write_csv(os.path.join(c.DATA, "REPORT_items.csv"), items, IFIELDS)
    print("informe %s: %d recetas, %d items" % ("escrito" if changed else "sin cambios", len(recipes), len(items)))


if __name__ == "__main__":
    sys.exit(main())
