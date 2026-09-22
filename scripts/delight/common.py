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
    marker = os.path.join(d, "1.20.1-%s-%s.jar" % (mod["id"], re.sub(r"[^A-Za-z0-9._-]", "_", version)))
    if os.path.exists(marker):
        return marker
    url = "https://api.modrinth.com/v2/project/%s/version?%s" % (
        slug, urllib.parse.urlencode({"loaders": '["forge"]', "game_versions": '["1.20.1"]'}))
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


RECIPE_RE = re.compile(r"^data/([^/]+)/recipes/(.+)\.json$")


def load_recipes(cfg, mod, source):
    """Devuelve {recipe_id: json} de todas las recetas que trae el mod (cualquier namespace)."""
    out = {}
    if source == "jar" and mod.get("modrinth"):
        with zipfile.ZipFile(modrinth_jar(cfg, mod)) as z:
            for name in sorted(z.namelist()):
                m = RECIPE_RE.match(name)
                if m:
                    try:
                        out["%s:%s" % (m.group(1), m.group(2))] = json.loads(z.read(name).decode("utf-8"))
                    except ValueError:
                        print("  aviso: %s de %s no es JSON valido (vacio o roto), se omite" % (name, mod["id"]))
    else:
        repo = expand(mod["repo"])
        for root in mod["data_roots"]:
            base = os.path.join(repo, root)
            for ns in sorted(os.listdir(base)) if os.path.isdir(base) else []:
                rdir = os.path.join(base, ns, "recipes")
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
    """Pocion de un ingrediente 1.20.1 con NBT: {"item": ..., "nbt": {"Potion": ...}} (tambien como texto SNBT)."""
    nbt = node.get("nbt")
    if isinstance(nbt, dict):
        return nbt.get("Potion")
    if isinstance(nbt, str):
        m = re.search(r'Potion:\s*"?([a-z0-9_:./-]+)"?', nbt)
        return m.group(1) if m else None
    return None


ITEM_TYPES = (None, "forge:nbt", "forge:partial_nbt")


def _classify(node, path, parent, wr):
    """Si el dict `node` es un ingrediente de agua devuelve un Hit, si no None (formatos de Forge 1.20.1)."""
    t = node.get("type")
    # items
    if t in ITEM_TYPES and isinstance(node.get("item"), str):
        item = node["item"]
        if item in wr["potion_items"] and _potion_of(node) in wr["water_potions"]:
            return Hit(path, "item", "bottle", wr["default_amounts"]["bottle"], node, item)
        if t is None and item in wr["item_ids"]:
            form = wr["item_ids"][item]
            return Hit(path, "item", form, wr["default_amounts"][form], node, item)
    if t is None and isinstance(node.get("tag"), str):
        info = wr["tags"].get(_strip(node["tag"]))
        if info and info["kind"] == "item":
            return Hit(path, "item", info["form"], wr["default_amounts"][info["form"]], node, "#" + _strip(node["tag"]))
    # fluidos: {"fluid", "amount"} (Create y otros) o {"fluidTag"}
    if t is None and node.get("fluid") in wr["fluid_ids"] and "item" not in node:
        return Hit(path, "fluid_flat", "fluid", node.get("amount", node.get("count", 1000)), node, node["fluid"])
    if t is None and isinstance(node.get("fluidTag"), str) and _strip(node["fluidTag"]) in wr["tags"]:
        return Hit(path, "fluid_flat", "tag", node.get("amount", 1000), node, "#" + _strip(node["fluidTag"]))
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

def purity_levels_below(min_purity):
    """Purezas a restar: todas las menores que el minimo (el agua sin pureza cuenta como DEFAULT_PURITY)."""
    return list(range(0, min_purity))


def subtract_items(hit, wr):
    """Items que llevan la pureza para un nodo de agua: el propio item, o los que lista water.json para un tag."""
    if hit.ref.startswith("#"):
        return list(wr["tags"][hit.ref[1:]]["items"])
    return [hit.ref]


def clean_replacement(hit, min_purity, wr):
    """JSON que sustituye al nodo de agua: acepta el agua del nodo original sin pureza o con pureza >= min_purity.
    forge:difference del nodo original menos un forge:partial_nbt por pureza baja; solo items (los ingredientes de
    fluido de 1.20.1 no tienen resta)."""
    if hit.kind != "item":
        return None
    items = subtract_items(hit, wr)
    subs = [{"type": "forge:partial_nbt", "items": items, "nbt": {PURITY: p}} for p in purity_levels_below(min_purity)]
    return {"type": "forge:difference", "base": hit.node, "subtracted": subs}


def apply_hits(recipe, hits, min_purity, wr):
    """Copia de la receta con cada nodo de agua sustituido. Devuelve (receta, error|None)."""
    import copy
    new = copy.deepcopy(recipe)
    for h in hits:
        if h.kind == "fluid_flat":
            extra = set(h.node) - {"fluid", "amount", "type", "tag", "fluid_tag"}
            if extra:
                return None, "el nodo de fluido lleva claves que no se conservan: %s" % sorted(extra)
        repl = clean_replacement(h, min_purity, wr)
        if repl is None:
            return None, "agua como fluido: los ingredientes de fluido de 1.20.1 no aceptan la resta de purezas"
        cur = new
        for p in h.path[:-1]:
            cur = cur[p]
        if h.path:
            cur[h.path[-1]] = repl
        else:
            return None, "el agua es la receta entera"
    return new, None


# ---------------------------------------------------------------- verificacion (modelo de aceptacion)

def accepts(ing, stack):
    """Evalua un ingrediente generado contra un ejemplar: stack = {'id', 'tags': set, 'nbt': dict}."""
    if isinstance(ing, list):
        return any(accepts(i, stack) for i in ing)
    t = ing.get("type")
    if t == "forge:difference":
        return accepts(ing["base"], stack) and not accepts(ing["subtracted"], stack)
    if t == "forge:compound":
        return any(accepts(c, stack) for c in ing["children"])
    if t in ("forge:partial_nbt", "forge:nbt"):
        items = ing.get("items") or [ing["item"]]
        if stack["id"] not in items:
            return False
        want = ing.get("nbt", {})
        if isinstance(want, str):
            want = {"Potion": _potion_of(ing)} if _potion_of(ing) else {}
        if t == "forge:nbt":
            return stack["nbt"] == want
        return all(stack["nbt"].get(k) == v for k, v in want.items())
    if "item" in ing:
        return ing["item"] == stack["id"]
    if "tag" in ing:
        return _strip(ing["tag"]) in stack["tags"]
    raise ValueError("ingrediente que el verificador no conoce: %r" % ing)


def find_differences(node, out=None):
    """Todos los nodos forge:difference de una receta generada."""
    out = [] if out is None else out
    if isinstance(node, dict):
        if node.get("type") == "forge:difference":
            out.append(node)
        for v in node.values():
            find_differences(v, out)
    elif isinstance(node, list):
        for v in node:
            find_differences(v, out)
    return out


def _samples_for(diff, wr):
    """Ejemplar de agua que el ingrediente debe distinguir: id, tags y el NBT que no es pureza."""
    base = diff["base"]
    extra = {}
    if "tag" in base:
        tag = _strip(base["tag"])
        ident, tags = wr["tags"][tag]["items"][0], {tag}
    else:
        ident, tags = base["item"], set()
        pot = _potion_of(base)
        if pot:
            extra = {"Potion": pot}
    return ident, tags, extra


def verify_diff(diff, expected_min, wr):
    """Comprueba que el difference acepta agua sin pureza y pureza >= minimo, y rechaza el resto.
    Devuelve una lista de errores (vacia si esta bien)."""
    errs = []
    ident, tags, extra = _samples_for(diff, wr)
    for purity in [None] + list(range(0, MAX_PURITY + 1)):
        nbt = dict(extra)
        if purity is not None:
            nbt[PURITY] = purity
        stack = {"id": ident, "tags": set(tags), "nbt": nbt}
        got = accepts(diff, stack)
        want = True if purity is None else purity >= expected_min
        if got != want:
            errs.append("pureza %s: acepta=%s, deberia=%s (minimo %d)" % (purity, got, want, expected_min))
    return errs
