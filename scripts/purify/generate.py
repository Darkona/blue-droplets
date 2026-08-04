#!/usr/bin/env python3
"""Regenerates the water purification recipes for the 6 purity levels (0-5).

Outputs (all rewritten from scratch, stale files are deleted):
  src/main/resources/datapacks/purify_{campfire,smelting,smoking,cooking_pot}/data/blue_droplets/recipe/
  src/main/resources/data/blue_droplets/recipe/compat/create/  (splashing, heated mixing, cactus)

Run:  python3 scripts/purify/generate.py          (rewrite files)
      python3 scripts/purify/generate.py --check  (fail if the files on disk differ)

Scheme: campfire and cooking pot +1 per cooking, furnace and smoker +2.
Caps: 4 with Create loaded, 5 without it. Water without a stored purity counts as DEFAULT (3).
Recipe ids: <item>_from_<method>_to_<level>[_with_create|_without_create]; the suffix appears only
when the sources or the condition differ between the two caps. Cooking pot ids end in _manual_only.
"""
import json
import shutil
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
RES = ROOT / "src/main/resources"

MAX_PURITY = 5
DEFAULT_PURITY = 3
CAP_CREATE = 4
LEVELS = ["contaminated", "dirty", "murky", "acceptable", "clean", "pure"]
KEY = "blue_droplets:purity"

POTION = {"potion_contents": {"potion": "minecraft:water"}}
# name, item id, extra components, extra mod conditions
ITEMS = [
    ("water_bottle", "minecraft:potion", POTION, []),
    ("water_bucket", "minecraft:water_bucket", {}, []),
    ("terracotta_water_bowl", "blue_droplets:terracotta_water_bowl", {}, []),
    ("filled_waterskin", "cold_sweat:filled_waterskin", {}, ["cold_sweat"]),
]
# method: (recipe type, step, cooking time, ingredient key, pack, extra mod conditions)
METHODS = {
    "campfire_cooking": ("minecraft:campfire_cooking", 1, 300, "ingredient", "purify_campfire", []),
    "smelting": ("minecraft:smelting", 2, 200, "ingredient", "purify_smelting", []),
    "smoking": ("minecraft:smoking", 2, 100, "ingredient", "purify_smoking", []),
    "cooking_pot": ("farmersdelight:cooking", 1, 200, "ingredients", "purify_cooking_pot", ["farmersdelight"]),
}

C_ENABLED = {"type": "blue_droplets:purity_enabled"}


def mod_loaded(mod):
    return {"type": "neoforge:mod_loaded", "modid": mod}


def no_create():
    return {"type": "neoforge:not", "value": mod_loaded("create")}


def with_purity(extra, p):
    return {KEY: p, **extra}


def item_ing(item_id, extra, p):
    # filled_waterskin always carries other data, so a component ingredient cannot say "no purity"
    if p == DEFAULT_PURITY and item_id != "cold_sweat:filled_waterskin":
        # water without a stored purity counts as the default; strict matches an empty patch
        return [
            {"type": "neoforge:components", "items": item_id, "components": with_purity(extra, p)},
            {"type": "neoforge:components", "items": item_id, "components": dict(extra), "strict": True},
        ]
    return [{"type": "neoforge:components", "items": item_id, "components": with_purity(extra, p)}]


def groups(step, cap):
    """result level -> sorted list of source levels, for a given cap."""
    g = {}
    for s in range(0, cap):
        r = min(s + step, cap)
        g.setdefault(r, []).append(s)
    return g


def plans(step):
    """Yield (result, sources, create_state) with create_state in None/'with'/'without'."""
    g4, g5 = groups(step, CAP_CREATE), groups(step, MAX_PURITY)
    for r in range(1, MAX_PURITY + 1):
        a, b = g4.get(r), g5.get(r)
        if a and b and a == b:
            yield r, a, None
        else:
            if a:
                yield r, a, "with"
            if b:
                yield r, b, "without" if a else "only_without"


def create_cond(state):
    if state == "with":
        return [mod_loaded("create")]
    if state in ("without", "only_without"):
        return [no_create()]
    return []


def rid(name, method, r, state, pot):
    s = f"{name}_from_{method}_to_{LEVELS[r]}"
    if state in ("with", "without"):
        s += f"_{state}_create"
    return s + ("_manual_only" if pot else "")


def build_cooking(name, item_id, extra, mods, method, r, srcs, state):
    rtype, step, time, key, pack, mmods = METHODS[method]
    conds = [C_ENABLED] + [mod_loaded(m) for m in mods] + create_cond(state) + [mod_loaded(m) for m in mmods]
    alts = [a for s in srcs for a in item_ing(item_id, extra, s)]
    d = {"neoforge:conditions": conds, "type": rtype}
    if key == "ingredients":
        d[key] = [alts[0]] if len(alts) == 1 else [{"type": "neoforge:compound", "children": alts}]
    else:
        d[key] = alts
    d["result"] = {"id": item_id, "count": 1, "components": with_purity(extra, r)}
    if method == "cooking_pot":
        d.update({"cookingtime": time, "experience": 0.35, "recipe_book_tab": "drinks"})
    else:
        d.update({"experience": 0.35, "cookingtime": time})
    return d


def cooking_files():
    out = {}
    for method, (_, step, _, _, pack, _) in METHODS.items():
        base = RES / "datapacks" / pack / "data/blue_droplets/recipe"
        for name, item_id, extra, mods in ITEMS:
            for r, srcs, state in plans(step):
                d = build_cooking(name, item_id, extra, mods, method, r, srcs, state)
                out[base / (rid(name, method, r, state, method == "cooking_pot") + ".json")] = d
    return out


# ---- Create (+1 per pass, cap 4) ----
def fluid_ing(p):
    if p == DEFAULT_PURITY:
        return {"type": "neoforge:compound", "children": [
            {"type": "neoforge:components", "fluids": "minecraft:water", "components": {KEY: p}, "amount": 250},
            {"type": "neoforge:components", "fluids": "minecraft:water", "components": {}, "strict": True, "amount": 250},
        ]}
    return {"type": "neoforge:components", "fluids": "minecraft:water", "components": {KEY: p}, "amount": 250}


def create_files():
    base = RES / "data/blue_droplets/recipe/compat/create"
    cc = [mod_loaded("create"), C_ENABLED]
    out = {}
    for r in range(1, CAP_CREATE + 1):
        s = r - 1
        # splashing: bottle and bowl
        for name, item_id, extra in (("water_bottle", "minecraft:potion", POTION),
                                     ("terracotta_water_bowl", "blue_droplets:terracotta_water_bowl", {})):
            ing = item_ing(item_id, extra, s)
            out[base / f"{name}_from_splashing_to_{LEVELS[r]}.json"] = {
                "neoforge:conditions": cc, "type": "create:splashing",
                "ingredients": [ing[0] if len(ing) == 1 else {"type": "neoforge:compound", "children": ing}],
                "results": [{"id": item_id, "components": with_purity(extra, r)}],
            }
        out[base / f"water_from_heated_mixing_to_{LEVELS[r]}.json"] = {
            "neoforge:conditions": cc, "type": "create:mixing", "heat_requirement": "heated",
            "ingredients": [fluid_ing(s)],
            "results": [{"id": "minecraft:water", "amount": 250, "components": {KEY: r}}],
        }
    out[base / "cactus.json"] = {
        "neoforge:conditions": cc, "type": "create:compacting",
        "ingredients": [{"item": "minecraft:cactus"}],
        "results": [{"id": "minecraft:water", "amount": 250, "components": {KEY: 3}}, {"id": "minecraft:green_dye"}],
    }
    return out


# files of the Create folder that this script owns (everything else there is hand written)
CREATE_OWNED = ("water_bottle_from_splashing_", "terracotta_water_bowl_from_splashing_",
                "water_from_heated_mixing_", "water_bottle_from_splashing", "terracotta_water_bowl_from_splashing")


def main():
    check = "--check" in sys.argv
    files = {**cooking_files(), **create_files()}
    # stale files: whole recipe dir of each purify pack, and the owned Create names
    stale = []
    for method, meta in METHODS.items():
        d = RES / "datapacks" / meta[4] / "data/blue_droplets/recipe"
        stale += [p for p in d.glob("*.json") if p not in files]
    cd = RES / "data/blue_droplets/recipe/compat/create"
    stale += [p for p in cd.glob("*.json") if p not in files and p.name.startswith(CREATE_OWNED)]
    bad = [p for p, d in files.items() if not p.exists() or p.read_text() != json.dumps(d, indent=2) + "\n"]
    if check:
        for p in bad + stale:
            print("out of date:", p.relative_to(ROOT))
        sys.exit(1 if bad or stale else 0)
    for p in stale:
        p.unlink()
    for p, d in files.items():
        p.parent.mkdir(parents=True, exist_ok=True)
        p.write_text(json.dumps(d, indent=2) + "\n")
    print(f"wrote {len(files)} recipes, removed {len(stale)} stale")


if __name__ == "__main__":
    main()
