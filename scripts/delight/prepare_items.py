#!/usr/bin/env python3
"""Combina data/items_raw.csv (volcado del comando de Opus) con el grafo de recetas y escribe data/items.csv.

Solo entran comida (nutrition no vacio) y bebidas (is_drink true). Columnas:
  craft_steps  pasos de la receta mas corta desde ingredientes base (0 = base o sin receta)
  thirst, quenched, salty, no_thirst  valores iniciales de rules/items.json (editables)
  category     sale de data/categories.csv (item_id,category,reason) si existe; con categoria conocida,
               los valores salen de "categories" en rules/items.json en vez de las palabras
  notes        libre; rule = como salio el valor; auto = el ultimo valor que dio la formula
Al volver a correr, una fila cuyos valores siguen siendo los de la formula se actualiza con la formula nueva;
una fila que Javier toco (o con auto = manual) no se cambia (salvo con --force). notes nunca se pisa; category solo la cambia categories.csv.
"""
import argparse
import csv
import os
import re
import sys
import zipfile

import common as c

INF = 10 ** 6


# ------------------------------------------------------------------ datos del juego (recetas y tags)

def _zip_iter(path):
    with zipfile.ZipFile(path) as z:
        for name in sorted(z.namelist()):
            if name.endswith(".json") and name.startswith("data/"):
                yield name, lambda n=name, zz=z: __import__("json").loads(zz.read(n).decode("utf-8"))


def _repo_iter(mod):
    repo = c.expand(mod["repo"])
    import json
    for root in mod["data_roots"]:
        base = os.path.join(repo, root)
        for dp, _, fns in os.walk(base):
            for fn in sorted(fns):
                if fn.endswith(".json"):
                    p = os.path.join(dp, fn)
                    rel = "data/" + os.path.relpath(p, base).replace(os.sep, "/")
                    yield rel, lambda p=p: json.load(open(p, encoding="utf-8"))


def collect(cfg, mods, rules, source):
    recipes, tags = {}, {}
    sources = []
    for m in mods:
        if source == "jar" and m.get("modrinth"):
            sources.append(_zip_iter(c.modrinth_jar(cfg, m)))
        else:
            sources.append(_repo_iter(m))
    for j in rules.get("recipe_extra_jars", []):
        p = os.path.join(c.REPO, j)
        if os.path.exists(p):
            sources.append(_zip_iter(p))
        else:
            print("aviso: no existe %s (las recetas de vanilla no cuentan)" % j, file=sys.stderr)
    for it in sources:
        for name, loader in it:
            m = re.match(r"^data/([^/]+)/recipe/(.+)\.json$", name)
            if m:
                recipes["%s:%s" % (m.group(1), m.group(2))] = loader()
                continue
            m = re.match(r"^data/([^/]+)/tags/item/(.+)\.json$", name)
            if m:
                tags.setdefault("%s:%s" % (m.group(1), m.group(2)), []).extend(loader().get("values", []))
    return recipes, tags


def tag_members(tag, tags, seen=None):
    seen = seen or set()
    if tag in seen:
        return set()
    seen.add(tag)
    out = set()
    for v in tags.get(tag, []):
        v = v.get("id") if isinstance(v, dict) else v
        if not v:
            continue
        if v.startswith("#"):
            out |= tag_members(v[1:], tags, seen)
        else:
            out.add(v)
    return out


# ------------------------------------------------------------------ grafo

def _leaf_options(n):
    """Opciones ('item id' | '#tag') de un nodo hoja de ingrediente, o None si no es hoja."""
    if not isinstance(n, dict):
        return None
    t = n.get("type")
    if t == "neoforge:difference":
        return _leaf_options(n.get("base"))
    if t == "neoforge:compound":
        opts = set()
        for ch in n.get("children", []):
            o = _leaf_options(ch)
            if o:
                opts |= o
        return opts
    if isinstance(n.get("item"), str):
        return {n["item"]}
    if isinstance(n.get("tag"), str):
        return {"#" + n["tag"].lstrip("#")}
    it = n.get("items")
    if isinstance(it, str):
        return {it}
    if isinstance(it, list):
        return set(it)
    return None


def slots_of(node, skip_top, top=True):
    out = []
    if isinstance(node, list):
        opts, rest = set(), []
        for e in node:
            o = _leaf_options(e)
            if o:
                opts |= o
            else:
                rest.append(e)
        if opts:
            out.append(opts)
        for e in rest:
            out += slots_of(e, skip_top, False)
    elif isinstance(node, dict):
        o = _leaf_options(node)
        if o:
            out.append(o)
        else:
            for k, v in node.items():
                if top and k in skip_top:
                    continue
                out += slots_of(v, skip_top, False)
    return out


def outputs_of(recipe):
    outs = set()
    for key in ("result", "results", "outItem", "output", "outputs"):
        r = recipe.get(key)
        for e in (r if isinstance(r, list) else [r]):
            if isinstance(e, dict):
                i = e.get("id") or e.get("item")
                if isinstance(i, str) and ":" in i:
                    outs.add(i)
            elif isinstance(e, str) and ":" in e:
                outs.add(e)
    return outs


def craft_depths(recipes, tags, wr, ignore_words):
    skip_top = set(wr["output_keys"])
    defs = {}  # item -> [slots]
    for rid, rec in recipes.items():
        toks = c._tokens(rid.split(":")[1])
        if toks & set(ignore_words):
            continue
        outs = outputs_of(rec)
        if not outs:
            continue
        sl = slots_of(rec, skip_top)
        if not sl:
            continue
        for o in outs:
            defs.setdefault(o, []).append(sl)
    members = {}
    d = {}

    def depth_of(opt):
        if opt.startswith("#"):
            if opt not in members:
                members[opt] = tag_members(opt[1:], tags)
            ms = members[opt]
            return min((d.get(m, 0) if m not in defs else d.get(m, INF) for m in ms), default=0)
        return d.get(opt, 0) if opt not in defs else d.get(opt, INF)

    def run():
        changed = True
        while changed:
            changed = False
            for item, rlist in defs.items():
                if item in fixed:
                    continue
                best = d.get(item, INF)
                for sl in rlist:
                    worst = 0
                    for opts in sl:
                        worst = max(worst, min(depth_of(o) for o in opts))
                    if worst < INF and 1 + worst < best:
                        best = 1 + worst
                if best < d.get(item, INF):
                    d[item] = best
                    changed = True

    fixed = set()
    run()
    unresolved = [i for i in defs if d.get(i, INF) >= INF]
    for i in unresolved:  # ciclos puros (bloque <-> lingote): cuentan como base
        d[i] = 0
        fixed.add(i)
    if unresolved:
        run()
    return {i: v for i, v in d.items()}


# ------------------------------------------------------------------ formula

def _match(cls, toks, tags, iid_path):
    if any(w in toks for w in cls.get("words", [])):
        return True
    if any(t in tags for t in cls.get("tags", [])):
        return True
    return iid_path in cls.get("ids", [])


def parse_effects(s):
    out = []
    for e in filter(None, s.split(";")):
        p = e.split(",")
        try:
            out.append((p[0], float(p[3]) if len(p) > 3 else 1.0))
        except ValueError:
            out.append((p[0], 1.0))
    return out


def compute(row, steps, R, category=""):
    iid = row["id"]
    path = iid.split(":", 1)[1]
    toks = c._tokens(path)
    tags = set(filter(None, row["tags"].split(";")))
    drink = row["is_drink"] == "true"
    why = []
    if _match(R["no_thirst"], toks, tags, path):
        return 0, 0, False, True, "no_thirst"
    cat = R["categories"].get(category) if category else None
    if cat is not None and "salty_grade" in cat:
        salty = next((l for l in ("strong", "medium", "light") if _match(R["salty"][l], toks, tags, path)), cat["salty_grade"])
        cat = None
    else:
        salty = None
        for lvl in ("strong", "medium", "light"):
            if cat is None and _match(R["salty"][lvl], toks, tags, path):
                salty = lvl
                break
    exempt = False
    if cat is not None and cat["thirst"] < 0 and not cat.get("use_keywords"):
        t, q = cat["thirst"], cat["quenched"]
        why.append("cat:" + category)
    elif salty:
        t, q = R["salty"][salty]["thirst"], R["salty"][salty]["quenched"]
        why.append(("cat:%s " % category if category else "") + "salty:" + salty)
    else:
        classes, default = (R["drink_classes"], R["drink_default"]) if drink else (R["food_classes"], R["food_default"])
        cls = None
        if cat is not None and cat.get("use_keywords"):
            allowed = cat.get("keyword_classes")
            cls = next((k for k in classes if (allowed is None or k["name"] in allowed) and _match(k, toks, tags, path)), None)
            if cls is not None:
                why.append("cat:%s>%s" % (category, cls["name"]))
        if cls is None and cat is not None:
            cls = dict(cat, name=category)
            why.append("cat:" + category)
        elif cls is None:
            cls = next((k for k in classes if _match(k, toks, tags, path)), default)
            why.append(("drink:" if drink else "food:") + cls["name"])
        t, q = cls["thirst"], cls["quenched"]
        exempt = cls.get("exempt_cap", False)
        cx = R["complexity"]
        bonus = 0
        if drink and t > 0 and steps >= cx["from_steps"]:
            bonus = min(cx["max_bonus"], (steps - cx["from_steps"]) // cx["per_steps"] + 1)
        if bonus:
            t += bonus
            q += bonus
            why.append("steps%d:+%d" % (steps, bonus))
        cap = R["drink_max_thirst_complex"] if (bonus or exempt) else R["drink_max_thirst"]
        if not drink:
            cap = R["food_max_thirst"]
        if t > cap:
            t = cap
            why.append("cap%d" % cap)
        q = min(q, t + 1) if drink else min(q, t)
    pen = 0
    eff = R["effects"]
    for eid, prob in parse_effects(row["effects"]):
        if eid in eff["poison"]["ids"] and prob >= eff["poison"]["min_probability"]:
            pen = max(pen, eff["poison"]["penalty"])
            why.append("efecto:" + eid.split(":")[1])
        elif eid in eff["hunger"]["ids"]:
            rotten = any(w in toks for w in eff["hunger"]["rotten_words"])
            p = eff["hunger"]["rotten_penalty"] if rotten else eff["hunger"]["raw_penalty"]
            pen = max(pen, p)
            why.append("efecto:hunger" + (":podrido" if rotten else ":crudo"))
    if pen:
        t -= pen
        q -= pen
    t = max(t, R["min_thirst"])
    q = max(q, R["min_thirst"])
    if t < 0:
        q = min(q, t)
    return t, q, t < 0, False, " ".join(why)


def sn(b):
    return "si" if b else "no"


def main():
    ap = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    ap.add_argument("--source", choices=["jar", "repo"], default="jar")
    ap.add_argument("--force", action="store_true", help="pisa thirst/quenched/salty/no_thirst de todas las filas")
    ap.add_argument("--raw", default=os.path.join(c.DATA, "items_raw.csv"))
    args = ap.parse_args()

    if not os.path.exists(args.raw):
        sys.exit("falta %s (lo genera el comando /blue_droplets dev dump_items)" % args.raw)
    R = c.load_json(os.path.join(c.RULES, "items.json"))
    wr = c.load_json(os.path.join(c.RULES, "water.json"))
    cfg, mods = c.load_mods()
    recipes, tags = collect(cfg, mods, R, args.source)
    depths = craft_depths(recipes, tags, wr, R.get("graph_ignore_words", ["unpack", "uncraft"]))
    print("grafo: %d recetas, %d items con receta" % (len(recipes), len(depths)))

    raw = [r for r in c.read_csv(args.raw)
           if r["mod"] not in R["exclude_mods"] and (r["nutrition"] != "" or r["is_drink"] == "true")]
    old = {r["item_id"]: r for r in c.read_csv(os.path.join(c.DATA, "items.csv"))}
    cats = {r["item_id"]: r["category"].strip() for r in c.read_csv(os.path.join(c.DATA, "categories.csv"))}
    unknown = set()
    import json as _json
    try:
        cur_map = _json.load(open(os.path.join(c.REPO, "src", "main", "resources", "data", "blue_droplets",
                                               "data_maps", "item", "drinks.json"), encoding="utf-8"))["values"]
    except (OSError, ValueError, KeyError):
        cur_map = {}
    no_recipes = sorted({r["mod"] for r in raw} - {m["id"] for m in mods} - {"minecraft"})
    if no_recipes:
        print("aviso: sin recetas en mods.json (craft_steps = 0): " + ", ".join(no_recipes))
    rows = []
    kept = 0
    for r in raw:
        steps = depths.get(r["id"], 0)
        cat = cats.get(r["id"], "")
        if cat and cat not in R["categories"]:
            unknown.add(cat)
        t, q, salty, nothirst, rule = compute(r, steps, R, cat)
        auto = "%d/%d/%s/%s" % (t, q, sn(salty), sn(nothirst))
        row = {"item_id": r["id"], "mod": r["mod"], "kind": "drink" if r["is_drink"] == "true" else "food",
               "craft_steps": steps, "thirst": t, "quenched": q, "salty": sn(salty), "no_thirst": sn(nothirst),
               "category": cat, "notes": "", "rule": rule, "auto": auto}
        prev = old.get(r["id"])
        if r["id"] in R["manual_ids"]:
            cur = cur_map.get(r["id"], {})
            if prev is not None:
                cur = {"thirst": prev["thirst"], "quenched": prev["quenched"]}
            elif not cur and r["current_thirst"] != "":
                cur = {"thirst": r["current_thirst"], "quenched": r["current_quenched"]}
            if cur:
                row.update({"thirst": cur["thirst"], "quenched": cur["quenched"], "salty": sn(int(cur["thirst"]) < 0),
                            "no_thirst": "no"})
            row.update({"auto": "manual", "rule": "manual (fijo por el codigo o por Javier)"})
            if prev is not None:
                row["notes"] = prev["notes"]
            rows.append(row)
            kept += 1
            continue
        if prev is None:
            if r["current_thirst"] != "":
                row["notes"] = "antes: %s/%s" % (r["current_thirst"], r["current_quenched"])
        else:
            row["category"], row["notes"] = cat or prev["category"], prev["notes"]
            now = "%s/%s/%s/%s" % (prev["thirst"], prev["quenched"], prev["salty"], prev["no_thirst"])
            if not args.force and (prev["auto"] == "manual" or now != prev["auto"]):
                for k in ("thirst", "quenched", "salty", "no_thirst"):
                    row[k] = prev[k]
                kept += 1
        rows.append(row)
    for u in sorted(unknown):
        print("aviso: categoria '%s' no esta en rules/items.json (categories); se usan las palabras" % u)
    rows.sort(key=lambda r: (r["mod"], r["item_id"]))
    gone = set(old) - {r["item_id"] for r in rows}
    for g in sorted(gone):
        print("  ya no esta en items_raw (se quita): " + g)
    changed = c.write_csv(os.path.join(c.DATA, "items.csv"), rows, c.ITEM_FIELDS)
    print("%s data/items.csv: %d items (%d con valores editados a mano, conservados)" % (
        "escrito" if changed else "sin cambios:", len(rows), kept))


if __name__ == "__main__":
    sys.exit(main())
