#!/usr/bin/env python3
"""Genera, de forma idempotente, los datos de Droplets of Thirst a partir de los CSV.

  datapacks/clean_water_cooking   una receta sobrescrita por fila activa de data/recipes.csv
  (los packs purify_* y las recetas de Create los genera scripts/purify/generate.py)
  data/droplets_of_thirst/data_maps/item/drinks.json   entradas de data/items.csv de TODOS los mods, vanilla y
                    droplets_of_thirst incluidos (sin condicion los dos ultimos, con mod_loaded el resto). Las claves
                    que no estan en el CSV (tags, mods no volcados) se dejan como estan
  data/droplets_of_thirst/tags/item/salty.json y no_thirst.json   desde data/items.csv

  --check          no escribe: compara y sale con 1 si algo difiere
  --source jar|repo  de donde salen las recetas originales (por defecto jar en cache)
"""
import argparse
import json
import os
import sys

import common as c
import generate_keg_pouring as keg

PACK_CLEAN = os.path.join(c.PACKS, "clean_water_cooking")
MAIN_DATA = os.path.join(c.REPO, "src", "main", "resources", "data", "droplets_of_thirst")
DRINKS = os.path.join(MAIN_DATA, "data_maps", "item", "drinks.json")
TAG_SALTY = os.path.join(MAIN_DATA, "tags", "item", "salty.json")
TAG_NO_THIRST = os.path.join(MAIN_DATA, "tags", "item", "no_thirst.json")
# Datos de mods sin version para esta version de Minecraft: fuera del jar, en src/disabled (no es un source set)
DISABLED_DATA = os.path.join(c.REPO, "src", "disabled", "resources", "data", "droplets_of_thirst")
DISABLED = {path: os.path.join(DISABLED_DATA, os.path.relpath(path, MAIN_DATA)) for path in (DRINKS, TAG_SALTY, TAG_NO_THIRST)}


def active_namespaces():
    """Namespaces cuyos datos van al jar: mods.json active_namespaces (los mods con version para esta Minecraft)."""
    cfg, _ = c.load_mods()
    return set(cfg.get("active_namespaces", ["minecraft", "droplets_of_thirst"]))


def is_active(key, active):
    return key.startswith("#") or key.split(":", 1)[0] in active


def mcmeta(desc):
    # Minecraft 26.x data packs: format range instead of pack_format (data pack format 107 for 26.2)
    return c.dump_json({"pack": {"description": desc, "min_format": 107, "max_format": 107}})


def mod_loaded(mod):
    return {"type": "neoforge:mod_loaded", "modid": mod}


# ------------------------------------------------------------------ clean_water_cooking

def build_clean(source, problems, info):
    cfg, mods = c.load_mods()
    by_id = {m["id"]: m for m in mods}
    wr = c.load_json(os.path.join(c.RULES, "water.json"))
    rows = [r for r in c.read_csv(os.path.join(c.DATA, "recipes.csv")) if c.yes(r["enabled"])]
    files = {}
    cache = {}
    for r in rows:
        rid, mod = r["recipe_id"], r["mod"]
        tag = "%s %s" % (mod, rid)
        if mod not in by_id:
            problems.append("%s: el mod no esta en mods.json" % tag)
            continue
        if r["recipe_type"] not in wr["supported_types"]:
            info.append("%s: tipo %s no acepta el ingrediente, no se escribe" % (tag, r["recipe_type"]))
            continue
        try:
            mp = int(r["min_purity"])
        except ValueError:
            problems.append("%s: min_purity '%s' no es un numero" % (tag, r["min_purity"]))
            continue
        if mp == 0:
            info.append("%s: min_purity 0 = acepta todo, no se escribe" % tag)
            continue
        if not 1 <= mp <= c.DEFAULT_PURITY:
            problems.append("%s: min_purity %d no soportado (1 a %d; el agua sin componente cuenta como %d)" % (tag, mp, c.DEFAULT_PURITY, c.DEFAULT_PURITY))
            continue
        if mod not in cache:
            cache[mod] = c.load_recipes(cfg, by_id[mod], source)
        orig = cache[mod].get(rid)
        if orig is None:
            problems.append("%s: la receta ya no esta en el mod" % tag)
            continue
        if r.get("recipe_sha") and r["recipe_sha"] != c.sha(orig):
            problems.append("%s: la receta cambio en el mod desde extract_recipes.py, vuelve a correrlo" % tag)
            continue
        hits = c.find_water(orig, wr)
        if not hits:
            problems.append("%s: no se encontro agua en la receta" % tag)
            continue
        new, err = c.apply_hits(orig, hits, mp)
        if err:
            info.append("%s: %s, no se escribe" % (tag, err))
            continue
        errs = []
        for d in c.find_differences(new):
            errs += c.verify_diff(d, mp, wr)
        if errs:
            problems.append("%s: verificacion fallida: %s" % (tag, "; ".join(errs)))
            continue
        conds = [x for x in orig.get("neoforge:conditions", [])]
        if mod_loaded(mod) not in conds:
            conds.append(mod_loaded(mod))
        out = {"neoforge:conditions": conds}
        for k, v in new.items():
            if k in ("neoforge:conditions", "fabric:load_conditions"):
                continue
            out[k] = v
        ns, path = rid.split(":", 1)
        files["data/%s/recipe/%s.json" % (ns, path)] = c.dump_json(out)
    # Keg de Brewin' and Chewin': recetas de vertido estrictas por pureza (generate_keg_pouring.py)
    if "brewinandchewin" in by_id:
        files.update(keg.build_keg_pouring(False, problems, info))
    # Archivos fijos escritos a mano (por ejemplo, arreglos de recetas rotas de otros mods): static/clean_water_cooking
    static = os.path.join(os.path.dirname(os.path.abspath(__file__)), "static", "clean_water_cooking")
    for dp, _, fns in os.walk(static):
        for fn in fns:
            src = os.path.join(dp, fn)
            with open(src, encoding="utf-8") as f:
                files[os.path.relpath(src, static).replace(os.sep, "/")] = f.read()
    if not files:
        # sin recetas (ningun mod de mods.json en esta version) no hay pack: sync_dir lo borra
        return files
    files["pack.mcmeta"] = mcmeta("Droplets of Thirst: recipes of Farmer's Delight addons need clean water")
    return files


# ------------------------------------------------------------------ items.csv -> data map y tags

def load_items():
    rows = c.read_csv(os.path.join(c.DATA, "items.csv"))
    return rows


def _int(v, default=0):
    try:
        return int(float(v))
    except (TypeError, ValueError):
        return default


def build_drinks(rows, problems, active):
    """Texto nuevo de drinks.json, activo y desactivado: las claves presentes en items.csv se reescriben, el resto queda
    igual. Las claves de namespaces fuera de active van al drinks.json de src/disabled."""
    cur = dict(c.load_json(DRINKS)["values"])
    if os.path.exists(DISABLED[DRINKS]):
        cur.update(c.load_json(DISABLED[DRINKS])["values"])
    managed = {r["item_id"] for r in rows}
    values = {k: v for k, v in cur.items() if k not in managed}
    gen = []
    for r in sorted(rows, key=lambda r: (r["mod"], r["item_id"])):
        th, qu = _int(r["thirst"]), _int(r["quenched"])
        if r["item_id"] in ("", None):
            continue
        if not -20 <= th <= 20 or qu < -20:
            problems.append("%s: thirst/quenched fuera de rango (%s/%s)" % (r["item_id"], th, qu))
            continue
        if th == 0 and qu == 0:
            continue
        entry = {}
        if r["mod"] not in ("minecraft", "droplets_of_thirst"):
            entry["neoforge:conditions"] = [mod_loaded(r["mod"])]
        entry["thirst"], entry["quenched"] = th, qu
        for k, v in cur.get(r["item_id"], {}).items():  # campos que el CSV no maneja (por ejemplo purity)
            if k not in ("thirst", "quenched", "neoforge:conditions"):
                entry[k] = v
        gen.append((r["item_id"], entry))
    for k, v in gen:
        values[k] = v

    def text(entries):
        lines = ['    %s: %s' % (json.dumps(k), json.dumps(v, ensure_ascii=False)) for k, v in entries]
        return '{\n  "values": {\n' + ",\n".join(lines) + '\n  }\n}\n'
    return (text([(k, v) for k, v in values.items() if is_active(k, active)]),
            text([(k, v) for k, v in values.items() if not is_active(k, active)]))


def build_tag(rows, col, active):
    ids = sorted({r["item_id"] for r in rows if c.yes(r.get(col, ""))})

    def text(chosen):
        return c.dump_json({"values": [{"id": i, "required": False} for i in chosen]})
    return text([i for i in ids if is_active(i, active)]), text([i for i in ids if not is_active(i, active)])


# ------------------------------------------------------------------ escritura / comparacion

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
    for rel, p in disk.items():
        if rel not in files:
            diffs.append("sobra:    %s" % os.path.relpath(p, c.REPO))
            if not check:
                os.remove(p)
    if not check and os.path.isdir(root):
        for dp, dn, fn in os.walk(root, topdown=False):
            if not dn and not fn:
                os.rmdir(dp)


def sync_file(path, text, check, diffs):
    cur = None
    if os.path.exists(path):
        with open(path, encoding="utf-8") as f:
            cur = f.read()
    if cur == text:
        return
    diffs.append("%s: %s" % ("distinto" if cur is not None else "falta   ", os.path.relpath(path, c.REPO)))
    if not check:
        os.makedirs(os.path.dirname(path), exist_ok=True)
        with open(path, "w", encoding="utf-8", newline="") as f:
            f.write(text)


def verify_disk(root, rows, wr, problems):
    """--check: vuelve a comprobar los ficheros que hay en disco (por si alguien los toco a mano)."""
    want = {(r["recipe_id"]): int(r["min_purity"]) for r in rows
            if c.yes(r["enabled"]) and r["min_purity"] in ("1", "2", "3")}
    for rel, p in walk_files(root).items():
        if not rel.startswith("data/") or "/recipe/" not in rel:
            continue
        ns, path = rel[5:].split("/recipe/", 1)
        rid = "%s:%s" % (ns, path[:-5])
        mp = want.get(rid)
        if mp is None:
            continue
        for d in c.find_differences(c.load_json(p)):
            for e in c.verify_diff(d, mp, wr):
                problems.append("%s: %s" % (rid, e))


def main():
    ap = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    ap.add_argument("--check", action="store_true")
    ap.add_argument("--source", choices=["jar", "repo"], default="jar")
    args = ap.parse_args()

    problems, info, diffs = [], [], []
    clean = build_clean(args.source, problems, info)
    sync_dir(PACK_CLEAN, clean, args.check, diffs)
    print("clean_water_cooking: %d recetas" % max(len(clean) - 1, 0))

    if args.check:
        wr = c.load_json(os.path.join(c.RULES, "water.json"))
        verify_disk(PACK_CLEAN, c.read_csv(os.path.join(c.DATA, "recipes.csv")), wr, problems)

    items = load_items()
    if items:
        active = active_namespaces()
        for path, (on, off) in ((DRINKS, build_drinks(items, problems, active)), (TAG_SALTY, build_tag(items, "salty", active)),
                                (TAG_NO_THIRST, build_tag(items, "no_thirst", active))):
            sync_file(path, on, args.check, diffs)
            sync_file(DISABLED[path], off, args.check, diffs)
        print("items.csv: %d items" % len(items))
    else:
        print("items.csv: no existe todavia, no se tocan drinks.json ni los tags")

    for i in info:
        print("aviso: " + i)
    for p in problems:
        print("ERROR: " + p)
    for d in diffs:
        print(("diferencia: " if args.check else "escrito: ") + d)
    if not diffs:
        print("sin cambios")
    java = os.path.join(c.REPO, "src", "main", "java", "com", "darkona", "dropletsofthirst", "DropletsOfThirst.java")
    src = open(java, encoding="utf-8").read() if os.path.exists(java) else ""
    missing = [p for p in ("clean_water_cooking",) if clean and '"%s"' % p not in src]
    if missing:
        print("\nFalta en Java (no lo toco): registrar en DropletsOfThirst.addPacks (PackSource.BUILT_IN): " + ", ".join(missing))
    if problems or (args.check and diffs):
        return 1
    return 0


if __name__ == "__main__":
    sys.exit(main())
