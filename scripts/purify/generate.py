#!/usr/bin/env python3
"""Regenerates the water purification recipes for the 6 purity levels (0-5), Minecraft 26.1 formats.

Outputs (all rewritten from scratch, stale files are deleted):
  src/main/resources/datapacks/purify_{campfire,smelting,smoking}/data/blue_droplets/recipe/

Run:  python3 scripts/purify/generate.py          (rewrite files)
      python3 scripts/purify/generate.py --check  (fail if the files on disk differ)

Scheme (notes/NIVELES-PUREZA.md): campfire +1 per cooking, furnace and smoker +2, up to 5. Water without a stored
purity counts as DEFAULT (3). Minecraft 26.1 has no Create, so there is no cap of 4 and no Create recipes on this
branch. Recipe ids: <item>_from_<method>_to_<level>. Ingredients use NeoForge's custom ingredient format
({"neoforge:ingredient_type": ...}); several sources of one result are a neoforge:compound ingredient.
"""
import json
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
RES = ROOT / "src/main/resources"

MAX_PURITY = 5
DEFAULT_PURITY = 3
LEVELS = ["contaminated", "dirty", "murky", "acceptable", "clean", "pure"]
KEY = "blue_droplets:purity"

POTION = {"minecraft:potion_contents": {"potion": "minecraft:water"}}
# name, item id, extra components
ITEMS = [
    ("water_bottle", "minecraft:potion", POTION),
    ("water_bucket", "minecraft:water_bucket", {}),
    ("terracotta_water_bowl", "blue_droplets:terracotta_water_bowl", {}),
]
# method: (recipe type, step, cooking time, pack)
METHODS = {
    "campfire_cooking": ("minecraft:campfire_cooking", 1, 300, "purify_campfire"),
    "smelting": ("minecraft:smelting", 2, 200, "purify_smelting"),
    "smoking": ("minecraft:smoking", 2, 100, "purify_smoking"),
}

C_ENABLED = {"type": "blue_droplets:purity_enabled"}


def with_purity(extra, p):
    return {KEY: p, **extra}


def components(item_id, comps, strict=False):
    d = {"neoforge:ingredient_type": "neoforge:components", "items": item_id, "components": comps}
    if strict:
        d["strict"] = True
    return d


def item_ing(item_id, extra, p):
    if p == DEFAULT_PURITY:
        # water without a stored purity counts as the default; strict matches an empty patch
        return [components(item_id, with_purity(extra, p)), components(item_id, dict(extra), True)]
    return [components(item_id, with_purity(extra, p))]


def groups(step):
    """result level -> sorted list of source levels."""
    g = {}
    for s in range(0, MAX_PURITY):
        g.setdefault(min(s + step, MAX_PURITY), []).append(s)
    return g


def rid(name, method, r):
    return f"{name}_from_{method}_to_{LEVELS[r]}"


def build_cooking(item_id, extra, method, r, srcs):
    rtype, step, time, pack = METHODS[method]
    alts = [a for s in srcs for a in item_ing(item_id, extra, s)]
    return {
        "neoforge:conditions": [C_ENABLED],
        "type": rtype,
        "ingredient": alts[0] if len(alts) == 1 else {"neoforge:ingredient_type": "neoforge:compound", "children": alts},
        "result": {"id": item_id, "components": with_purity(extra, r)},
        "experience": 0.35,
        "cookingtime": time,
    }


def cooking_files():
    out = {}
    for method, (_, step, _, pack) in METHODS.items():
        base = RES / "datapacks" / pack / "data/blue_droplets/recipe"
        for name, item_id, extra in ITEMS:
            for r, srcs in sorted(groups(step).items()):
                out[base / (rid(name, method, r) + ".json")] = build_cooking(item_id, extra, method, r, srcs)
    return out


def main():
    check = "--check" in sys.argv
    files = cooking_files()
    # stale files: whole recipe dir of each purify pack
    stale = []
    for method, meta in METHODS.items():
        d = RES / "datapacks" / meta[3] / "data/blue_droplets/recipe"
        stale += [p for p in d.glob("*.json") if p not in files]
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
