#!/usr/bin/env python3
"""Lee las recetas de los mods de mods.json y escribe data/recipes.csv con las que usan agua.

Las columnas heat, min_purity, enabled y notes de las filas que ya existen se conservan
(son las que Javier edita); solo las filas nuevas salen de las reglas de rules/heat.json.
  --source jar|repo   de donde salen las recetas (por defecto jar de Modrinth, en cache)
  --reapply-rules     recalcula heat y min_purity de TODAS las filas con las reglas
  --mod ID            solo ese mod (repetible)
"""
import argparse
import os
import sys

import common as c


def main():
    ap = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    ap.add_argument("--source", choices=["jar", "repo"], default="jar")
    ap.add_argument("--reapply-rules", action="store_true")
    ap.add_argument("--mod", action="append")
    args = ap.parse_args()

    cfg, mods = c.load_mods()
    wr = c.load_json(os.path.join(c.RULES, "water.json"))
    hr = c.load_json(os.path.join(c.RULES, "heat.json"))
    path = os.path.join(c.DATA, "recipes.csv")
    old = {(r["mod"], r["recipe_id"]): r for r in c.read_csv(path)}
    only = set(args.mod or [])
    rows = []
    stats = {}

    for mod in mods:
        if only and mod["id"] not in only:
            # conserva las filas de los mods que no se procesan
            rows.extend(r for (m, _), r in old.items() if m == mod["id"])
            continue
        recipes = c.load_recipes(cfg, mod, args.source)
        n_water = 0
        for rid in sorted(recipes):
            recipe = recipes[rid]
            rtype = recipe.get("type", "")
            if rtype in wr["ignore_types"]:
                continue
            hits = c.find_water(recipe, wr)
            manual = wr["manual_rows"].get(rid)
            if not hits and not manual:
                continue
            n_water += 1
            res_id, res_count = c.result_of(recipe)
            heat = c.heat_for(rid, rtype, res_id, hr)
            row = {
                "mod": mod["id"], "recipe_id": rid, "recipe_type": rtype,
                "water_form": "+".join(sorted({h.form for h in hits})) if hits else manual["water_form"],
                "water_amount": sum(int(h.amount) for h in hits) if hits else manual["water_amount"],
                "result_id": res_id, "result_count": res_count,
                "heat": heat, "min_purity": hr["purity_by_heat"][heat],
                "enabled": 1 if rtype in wr["supported_types"] and hits else 0,
                "notes": "",
                "recipe_sha": c.sha(recipe),
            }
            if manual:
                row["notes"] = manual["note"]
            elif rtype not in wr["supported_types"]:
                row["notes"] = "tipo de receta sin verificar: no se sobrescribe"
            prev = old.get((mod["id"], rid))
            if prev and not args.reapply_rules:
                for k in ("heat", "min_purity", "enabled", "notes"):
                    if prev.get(k, "") != "":
                        row[k] = prev[k]
            rows.append(row)
        stats[mod["id"]] = (len(recipes), n_water)
        print("%-16s %5d recetas, %3d con agua" % (mod["id"], len(recipes), n_water))

    rows.sort(key=lambda r: (r["mod"], r["recipe_id"]))
    gone = set(old) - {(r["mod"], r["recipe_id"]) for r in rows}
    for m, rid in sorted(gone):
        print("  ya no existe (se quita del CSV): %s" % rid)
    changed = c.write_csv(path, rows, c.RECIPE_FIELDS)
    print("%s %s (%d filas)" % ("escrito" if changed else "sin cambios:", os.path.relpath(path, c.REPO), len(rows)))


if __name__ == "__main__":
    sys.exit(main())
