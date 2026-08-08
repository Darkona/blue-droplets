#!/usr/bin/env python3
"""Genera las recetas estrictas por pureza para brewinandchewin:pouring/water_bucket y potion.

Salida idempotente: misma salida si se corre dos veces. Con --check, compara y sale con 1 si difiere.
"""
import argparse
import json
import os
import sys

import common as c

PACK_CLEAN = os.path.join(c.PACKS, "clean_water_cooking")


def mcmeta(desc):
    return c.dump_json({"pack": {"description": desc, "min_format": 107, "max_format": 107}})


def mod_loaded(mod):
    return {"type": "neoforge:mod_loaded", "modid": mod}


def water_ingredient_strict(purity):
    """Ingrediente estricto para cubo de agua con pureza específica."""
    return {
        c.TYPE_KEY: "neoforge:components",
        "items": "#c:buckets/water",
        "components": {"blue_droplets:purity": purity},
        "strict": True
    }


def potion_ingredient_strict(purity):
    """Ingrediente estricto para poción de agua con pureza específica."""
    return {
        c.TYPE_KEY: "neoforge:components",
        "items": "minecraft:potion",
        "components": {"minecraft:potion_contents": {"potion": "minecraft:water"}, "blue_droplets:purity": purity},
        "strict": True
    }


def fluid_with_purity(purity):
    """Fluido de agua con componente de pureza."""
    return {
        "amount": 1000,
        "id": "minecraft:water",
        "components": {"blue_droplets:purity": purity}
    }


def potion_fluid_with_purity(purity):
    """Fluido de agua (250 mB) para poción, con pureza."""
    return {
        "amount": 250,
        "id": "minecraft:water",
        "components": {"blue_droplets:purity": purity}
    }


def water_bucket_output_with_purity(purity):
    """Cubo de agua como output con componente de pureza."""
    return {
        "count": 1,
        "id": "minecraft:water_bucket",
        "components": {"blue_droplets:purity": purity}
    }


def potion_output_with_purity(purity):
    """Poción de agua como output con ambos componentes: potion_contents y purity."""
    return {
        "components": {
            "minecraft:potion_contents": {"potion": "minecraft:water"},
            "blue_droplets:purity": purity
        },
        "count": 1,
        "id": "minecraft:potion"
    }


def build_keg_pouring(check, problems, info):
    """Genera las recetas estrictas por pureza."""
    files = {}

    # Base para ambas recetas: condición de mod_loaded
    conditions = [mod_loaded("brewinandchewin")]

    # Generar recetas para water_bucket (0-5 + sin componente que cuenta como 3)
    for purity in range(6):
        rid = f"brewinandchewin:pouring/water_bucket_purity_{purity}"
        recipe = {
            "neoforge:conditions": conditions,
            "type": "brewinandchewin:keg_pouring",
            "fluid": fluid_with_purity(purity),
            "output": water_bucket_output_with_purity(purity),
            "strict": True,
            "unit": "millibuckets"
        }
        ns, path = rid.split(":", 1)
        files[f"data/{ns}/recipe/{path}.json"] = c.dump_json(recipe)

    # La receta original, estricta: solo el cubo sin pureza (se lee como la pureza por defecto)
    rid = "brewinandchewin:pouring/water_bucket"
    recipe = {
        "neoforge:conditions": conditions,
        "type": "brewinandchewin:keg_pouring",
        "fluid": {
            "amount": 1000,
            "id": "minecraft:water"
        },
        "output": {"count": 1, "id": "minecraft:water_bucket"},
        "strict": True,
        "unit": "millibuckets"
    }
    ns, path = rid.split(":", 1)
    files[f"data/{ns}/recipe/{path}.json"] = c.dump_json(recipe)

    # Generar recetas para potion (0-5 + sin componente que cuenta como 3)
    for purity in range(6):
        rid = f"brewinandchewin:pouring/potion_purity_{purity}"
        recipe = {
            "neoforge:conditions": conditions,
            "type": "brewinandchewin:keg_pouring",
            "container": {
                "count": 1,
                "id": "minecraft:glass_bottle"
            },
            "fluid": potion_fluid_with_purity(purity),
            "output": potion_output_with_purity(purity),
            "strict": True,
            "unit": "millibuckets"
        }
        ns, path = rid.split(":", 1)
        files[f"data/{ns}/recipe/{path}.json"] = c.dump_json(recipe)

    # La receta original, estricta: solo la botella sin pureza
    rid = "brewinandchewin:pouring/potion"
    recipe = {
        "neoforge:conditions": conditions,
        "type": "brewinandchewin:keg_pouring",
        "container": {
            "count": 1,
            "id": "minecraft:glass_bottle"
        },
        "fluid": {
            "amount": 250,
            "id": "minecraft:water"
        },
        "output": {"components": {"minecraft:potion_contents": {"potion": "minecraft:water"}}, "count": 1, "id": "minecraft:potion"},
        "strict": True,
        "unit": "millibuckets"
    }
    ns, path = rid.split(":", 1)
    files[f"data/{ns}/recipe/{path}.json"] = c.dump_json(recipe)


    return files


def walk_files(root):
    out = {}
    for dp, _, fns in os.walk(root):
        for fn in fns:
            p = os.path.join(dp, fn)
            out[os.path.relpath(p, root).replace(os.sep, "/")] = p
    return out


def sync_dir(root, files, check, diffs):
    disk = walk_files(root) if os.path.isdir(root) else {}
    for rel, text in files.items():
        p = os.path.join(root, rel)
        if rel in disk:
            with open(p, encoding="utf-8") as f:
                if f.read() == text:
                    continue
            diffs.append("distinto: %s" % os.path.relpath(p, c.REPO))
        else:
            diffs.append("falta:    %s" % os.path.relpath(p, c.REPO))
        if not check:
            os.makedirs(os.path.dirname(p), exist_ok=True)
            with open(p, "w", encoding="utf-8", newline="") as f:
                f.write(text)

    # Solo limpiar recetas generadas de este script (los que coinciden con el patrón)
    for rel, p in disk.items():
        if rel not in files and "/recipe/pouring/" in rel and ("water_bucket" in rel or "potion" in rel):
            if any(x in rel for x in ["purity_", "no_purity"]):
                diffs.append("sobra:    %s" % os.path.relpath(p, c.REPO))
                if not check:
                    os.remove(p)

    if not check and os.path.isdir(root):
        for dp, dn, fn in os.walk(root, topdown=False):
            if not dn and not fn:
                os.rmdir(dp)


def main():
    """Las recetas del keg las escribe apply.py junto con el resto de clean_water_cooking; esto solo lo invoca."""
    import apply
    return apply.main()


if __name__ == "__main__":
    sys.exit(main())
