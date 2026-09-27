"""Codigo compartido de los scripts de scripts/delight (solo libreria estandar)."""
import csv
import hashlib
import io
import json
import os
import re
import sys
import urllib.parse
import urllib.request
import zipfile

HERE = os.path.dirname(os.path.abspath(__file__))
REPO = os.path.abspath(os.path.join(HERE, "..", ".."))
DATA = os.path.join(HERE, "data")
RULES = os.path.join(HERE, "rules")
PACKS = os.path.join(REPO, "src", "main", "resources", "datapacks")
MOD_ID = "droplets_of_thirst"
PURITY = "droplets_of_thirst:purity"
# Minecraft 26.1: NeoForge custom ingredients (item and fluid) name their type with this key, not "type"
TYPE_KEY = "neoforge:ingredient_type"
MAX_PURITY = 5      # niveles 0..5
DEFAULT_PURITY = 3  # el agua sin componente cuenta como 3

RECIPE_FIELDS = ["mod", "recipe_id", "recipe_type", "water_form", "water_amount", "result_id",
                 "result_count", "heat", "min_purity", "enabled", "notes", "recipe_sha"]
# thirst..notes los edita Javier; craft_steps, kind, rule, auto_* se recalculan siempre.
ITEM_FIELDS = ["item_id", "mod", "kind", "craft_steps", "thirst", "quenched", "salty", "no_thirst", "category",
               "notes", "rule", "auto"]
ITEM_EDIT = ["thirst", "quenched", "salty", "no_thirst", "category", "notes"]


def load_json(path):
    with open(path, encoding="utf-8") as f:
        return json.load(f)


def dump_json(obj):
    """Salida determinista: 2 espacios, orden de claves original, salto final."""
    return json.dumps(obj, indent=2, ensure_ascii=False) + "\n"


def expand(p):
    return os.path.expanduser(p)


def load_mods():
    cfg = load_json(os.path.join(HERE, "mods.json"))
    return cfg, cfg["mods"]


def sha(obj):
    return hashlib.sha1(json.dumps(obj, sort_keys=True, separators=(",", ":")).encode()).hexdigest()[:12]


# ---------------------------------------------------------------- CSV

def read_csv(path):
    if not os.path.exists(path):
        return []
    with open(path, encoding="utf-8", newline="") as f:
        return list(csv.DictReader(f))


def csv_text(rows, fields):
    buf = io.StringIO()
    w = csv.DictWriter(buf, fieldnames=fields, lineterminator="\n", extrasaction="ignore")
    w.writeheader()
    for r in rows:
        w.writerow({k: r.get(k, "") for k in fields})
    return buf.getvalue()


def write_csv(path, rows, fields):
    os.makedirs(os.path.dirname(path), exist_ok=True)
    text = csv_text(rows, fields)
    if os.path.exists(path):
        with open(path, encoding="utf-8", newline="") as f:
            if f.read() == text:
                return False
    with open(path, "w", encoding="utf-8", newline="") as f:
        f.write(text)
    return True


def yes(v):
    return str(v).strip().lower() in ("1", "true", "yes", "si", "sí", "y", "s")


# ---------------------------------------------------------------- fuentes de recetas

def modrinth_jar(cfg, mod):
    """Baja (si falta) el jar fijado del mod al cache y devuelve su ruta."""
    cache = expand(cfg["cache_dir"])
    d = os.path.join(cache, "jars")
    os.makedirs(d, exist_ok=True)
    slug, version = mod["modrinth"], mod["version"]
    marker = os.path.join(d, "%s-%s.jar" % (mod["id"], re.sub(r"[^A-Za-z0-9._-]", "_", version)))
    if os.path.exists(marker):
        return marker
    url = "https://api.modrinth.com/v2/project/%s/version?%s" % (
        slug, urllib.parse.urlencode({"loaders": '["neoforge"]', "game_versions": '["1.21.1"]'}))
    req = urllib.request.Request(url, headers={"User-Agent": "droplets-of-thirst-delight-tooling"})
    versions = json.load(urllib.request.urlopen(req, timeout=60))
    hit = next((v for v in versions if v["version_number"] == version), None)
    if hit is None:
        raise SystemExit("%s: la version %s no esta en Modrinth (hay: %s)" % (
            mod["id"], version, ", ".join(v["version_number"] for v in versions[:6])))
    f = next((x for x in hit["files"] if x.get("primary")), hit["files"][0])
    print("  bajando %s ..." % f["filename"], file=sys.stderr)
    req = urllib.request.Request(f["url"], headers={"User-Agent": "droplets-of-thirst-delight-tooling"})
    data = urllib.request.urlopen(req, timeout=120).read()
    want = f.get("hashes", {}).get("sha1")
    if want and hashlib.sha1(data).hexdigest() != want:
        raise SystemExit("%s: sha1 distinto al bajar %s" % (mod["id"], f["filename"]))
    with open(marker + ".part", "wb") as out:
        out.write(data)
    os.replace(marker + ".part", marker)
    return marker


RECIPE_RE = re.compile(r"^data/([^/]+)/recipe/(.+)\.json$")


def load_recipes(cfg, mod, source):
    """Devuelve {recipe_id: json} de todas las recetas que trae el mod (cualquier namespace)."""
    out = {}
    if source == "jar" and mod.get("modrinth"):
        with zipfile.ZipFile(modrinth_jar(cfg, mod)) as z:
            for name in sorted(z.namelist()):
                m = RECIPE_RE.match(name)
                if m:
                    out["%s:%s" % (m.group(1), m.group(2))] = json.loads(z.read(name).decode("utf-8"))
    else:
        repo = expand(mod["repo"])
        for root in mod["data_roots"]:
            base = os.path.join(repo, root)
            for ns in sorted(os.listdir(base)) if os.path.isdir(base) else []:
                rdir = os.path.join(base, ns, "recipe")
                for dp, _, files in os.walk(rdir):
                    for fn in sorted(files):
                        if fn.endswith(".json"):
                            rel = os.path.relpath(os.path.join(dp, fn), rdir)[:-5].replace(os.sep, "/")
                            out["%s:%s" % (ns, rel)] = load_json(os.path.join(dp, fn))
    return out


# ---------------------------------------------------------------- deteccion del agua

class Hit:
    __slots__ = ("path", "kind", "form", "amount", "node", "ref")

    def __init__(self, path, kind, form, amount, node, ref):
        self.path, self.kind, self.form, self.amount, self.node, self.ref = path, kind, form, amount, node, ref


def _strip(tag):
    return tag[1:] if tag.startswith("#") else tag


def _potion_of(node):
    comps = node.get("components")
    if not isinstance(comps, dict):
        return None
    pc = comps.get("minecraft:potion_contents", comps.get("potion_contents"))
    return pc.get("potion") if isinstance(pc, dict) else None


def _classify(node, path, parent, wr):
    """Si el dict `node` es un ingrediente de agua devuelve un Hit, si no None."""
    dpath = ".".join(str(p) for p in path if isinstance(p, str))
    nested = any(dpath.endswith(n) for n in wr["nested_fluid_paths"])
    if nested and isinstance(node.get("tag"), str) and _strip(node["tag"]) in wr["tags"]:
        t = wr["tags"][_strip(node["tag"])]
        amt = parent.get("amount", wr["default_amounts"].get("fluid", 1000)) if isinstance(parent, dict) else 1000
        return Hit(path, "fluid_nested", t["form"], amt, node, "#" + _strip(node["tag"]))
    if nested and node.get("fluid") in wr["fluid_ids"]:
        amt = parent.get("amount", 1000) if isinstance(parent, dict) else 1000
        return Hit(path, "fluid_nested", "fluid", amt, node, node["fluid"])
    t = ingredient_type(node)
    # items
    if t is None or t == "neoforge:components":
        if t is None and node.get("item") in wr["item_ids"]:
            form = wr["item_ids"][node["item"]]
            return Hit(path, "item", form, wr["default_amounts"][form], node, node["item"])
        if t is None and isinstance(node.get("tag"), str):
            info = wr["tags"].get(_strip(node["tag"]))
            if info and info["kind"] == "item":
                return Hit(path, "item", info["form"], wr["default_amounts"][info["form"]], node, "#" + _strip(node["tag"]))
            if info and info["kind"] == "fluid":
                return Hit(path, "fluid_flat", info["form"], node.get("amount", 1000), node, "#" + _strip(node["tag"]))
        if t == "neoforge:components" and isinstance(node.get("items"), str):
            if node["items"] in wr["potion_items"] and _potion_of(node) in wr["water_potions"]:
                return Hit(path, "item", "bottle", wr["default_amounts"]["bottle"], node, node["items"])
            if node["items"] in wr["item_ids"]:
                form = wr["item_ids"][node["items"]]
                return Hit(path, "item", form, wr["default_amounts"][form], node, node["items"])
    # fluidos planos (SizedFluidIngredient.FLAT_CODEC, fluid_stack de Create)
    if t in (None, "fluid_stack") and node.get("fluid") in wr["fluid_ids"]:
        return Hit(path, "fluid_flat", "fluid", node.get("amount", 1000), node, node["fluid"])
    if t == "fluid_tag" or "fluid_tag" in node:
        tag = _strip(str(node.get("fluid_tag", "")))
        if tag in wr["tags"]:
            return Hit(path, "fluid_flat", "tag", node.get("amount", 1000), node, "#" + tag)
    return None


def find_water(recipe, wr):
    """Recorre la receta (sin las claves de salida) y devuelve la lista de Hit."""
    hits = []
    skip = set(wr["output_keys"])

    def walk(node, path, parent):
        if isinstance(node, dict):
            h = _classify(node, path, parent, wr)
            if h is not None:
                hits.append(h)
                return
            for k, v in node.items():
                if k in skip and not path:
                    continue
                walk(v, path + (k,), node)
        elif isinstance(node, list):
            for i, v in enumerate(node):
                walk(v, path + (i,), parent)

    walk(recipe, (), None)
    return hits


def result_of(recipe):
    """(id, count) del primer resultado de la receta."""
    for key in ("result", "results", "outItem", "output"):
        r = recipe.get(key)
        if isinstance(r, list) and r:
            r = r[0]
        if isinstance(r, dict):
            rid = r.get("id") or r.get("item") or ""
            cnt = r.get("count", r.get("amount", 1))
            return rid, cnt
        if isinstance(r, str):
            return r, 1
    return "", ""


# ---------------------------------------------------------------- calor

def _tokens(s):
    return set(t for t in re.split(r"[^a-z0-9]+", s.lower()) if t)


def heat_for(recipe_id, rtype, result_id, hr):
    if recipe_id in hr.get("overrides", {}):
        return hr["overrides"][recipe_id]
    if rtype in hr.get("keyword_types", []):
        toks = _tokens(result_id.split(":")[-1]) | _tokens(recipe_id.split(":")[-1])
        for rule in hr["keywords"]:
            if any(w in toks for w in rule["words"]):
                return rule["heat"]
    return hr["by_type"].get(rtype, hr["default"])


# ---------------------------------------------------------------- ingredientes de agua limpia

def ingredient_type(node):
    """Type of a custom ingredient: TYPE_KEY in 26.1, "type" in the recipes of earlier versions."""
    return node.get(TYPE_KEY, node.get("type")) if isinstance(node, dict) else None


def purity_levels_below(min_purity):
    """Purezas a restar: todas las menores que el minimo (el agua sin componente cuenta como DEFAULT_PURITY)."""
    return list(range(0, min_purity))


def _comp_item(items, purity, extra):
    comps = {PURITY: purity}
    comps.update(extra or {})
    return {TYPE_KEY: "neoforge:components", "items": items, "components": comps}


def _comp_fluid(fluids, purity):
    return {TYPE_KEY: "neoforge:components", "fluids": fluids, "components": {PURITY: purity}}


def _compound(children):
    return {TYPE_KEY: "neoforge:compound", "children": children}


def clean_replacement(hit, min_purity):
    """JSON que sustituye al nodo de agua: acepta el agua del nodo original con pureza >= min_purity."""
    levels = purity_levels_below(min_purity)
    node = hit.node
    if hit.kind == "item":
        extra = None
        if ingredient_type(node) == "neoforge:components":
            extra = {k: v for k, v in node["components"].items() if k != PURITY}
        subs = [_comp_item(hit.ref, p, extra) for p in levels]
        return {TYPE_KEY: "neoforge:difference", "base": node, "subtracted": _compound(subs)}
    fluids = [hit.ref] if not hit.ref.startswith("#") else hit.ref
    if hit.ref.startswith("#"):
        base = {"tag": hit.ref[1:]}
    else:
        base = {"fluid": hit.ref}
    subs = [_comp_fluid(fluids, p) for p in levels]
    out = {TYPE_KEY: "neoforge:difference", "base": base, "subtracted": _compound(subs)}
    if hit.kind == "fluid_flat":
        out["amount"] = hit.amount
    return out


def apply_hits(recipe, hits, min_purity):
    """Copia de la receta con cada nodo de agua sustituido. Devuelve (receta, error|None)."""
    import copy
    new = copy.deepcopy(recipe)
    for h in hits:
        if h.kind == "fluid_flat":
            extra = set(h.node) - {"fluid", "amount", "type", "tag", "fluid_tag"}
            if extra:
                return None, "el nodo de fluido lleva claves que no se conservan: %s" % sorted(extra)
        repl = clean_replacement(h, min_purity)
        cur = new
        for p in h.path[:-1]:
            cur = cur[p]
        if h.path:
            cur[h.path[-1]] = repl
        else:
            return None, "el agua es la receta entera"
    return new, None


# ---------------------------------------------------------------- verificacion (modelo de aceptacion)

def _holder_match(spec, ident, tags):
    """`items`/`fluids` de un ingrediente: id, '#tag' o lista de ids."""
    if isinstance(spec, list):
        return any(_holder_match(s, ident, tags) for s in spec)
    if spec.startswith("#"):
        return spec[1:] in tags
    return spec == ident


def accepts(ing, stack):
    """Evalua un ingrediente generado contra un ejemplar: stack = {'id', 'tags': set, 'components': dict}."""
    if isinstance(ing, list):
        return any(accepts(i, stack) for i in ing)
    t = ingredient_type(ing)
    if t == "neoforge:difference":
        return accepts(ing["base"], stack) and not accepts(ing["subtracted"], stack)
    if t == "neoforge:compound":
        return any(accepts(c, stack) for c in ing["children"])
    if t == "neoforge:components":
        spec = ing.get("items", ing.get("fluids"))
        if not _holder_match(spec, stack["id"], stack["tags"]):
            return False
        return all(stack["components"].get(k) == v for k, v in ing["components"].items())
    if "item" in ing or "fluid" in ing:
        return (ing.get("item") or ing.get("fluid")) == stack["id"]
    if "tag" in ing:
        return _strip(ing["tag"]) in stack["tags"]
    raise ValueError("ingrediente que el verificador no conoce: %r" % ing)


def find_differences(node, out=None):
    """Todos los nodos neoforge:difference de una receta generada."""
    out = [] if out is None else out
    if isinstance(node, dict):
        if ingredient_type(node) == "neoforge:difference":
            out.append(node)
        for v in node.values():
            find_differences(v, out)
    elif isinstance(node, list):
        for v in node:
            find_differences(v, out)
    return out


def _samples_for(diff, wr):
    """Ejemplares de agua que el ingrediente debe distinguir: sin pureza y purezas 0..MAX_PURITY."""
    base = diff["base"]
    subs = diff["subtracted"]["children"]
    first = subs[0] if subs else None
    if ingredient_type(base) == "neoforge:components":
        ident = base["items"] if isinstance(base["items"], str) else base["items"][0]
        tags = set()
        extra = {k: v for k, v in base["components"].items() if k != PURITY}
    elif "item" in base:
        ident, tags, extra = base["item"], set(), {}
    elif "fluid" in base:
        ident, tags, extra = base["fluid"], set(), {}
    else:
        tag = _strip(base["tag"])
        ident, tags, extra = "x:water_stand_in", {tag}, {}
    if first is not None:
        spec = first.get("items", first.get("fluids"))
        if isinstance(spec, str) and spec.startswith("#"):
            tags = tags | {spec[1:]}
        elif isinstance(spec, str):
            ident = spec
        elif isinstance(spec, list) and spec:
            ident = spec[0]
    return ident, tags, extra


def verify_diff(diff, expected_min, wr):
    """Comprueba que el difference acepta agua sin pureza y pureza >= minimo, y rechaza el resto.
    Devuelve una lista de errores (vacia si esta bien)."""
    errs = []
    ident, tags, extra = _samples_for(diff, wr)
    for purity in [None] + list(range(0, MAX_PURITY + 1)):
        comps = dict(extra)
        if purity is not None:
            comps[PURITY] = purity
        stack = {"id": ident, "tags": set(tags), "components": comps}
        got = accepts(diff, stack)
        want = True if purity is None else purity >= expected_min
        if got != want:
            errs.append("pureza %s: acepta=%s, deberia=%s (minimo %d)" % (purity, got, want, expected_min))
    return errs
