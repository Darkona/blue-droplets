#!/usr/bin/env python3
"""Regenerates the brewing recipes of the Potion of Quenchness, Minecraft 26.3 format (minecraft:brewing).

Output (rewritten from scratch, stale files are deleted):
  src/main/resources/data/blue_droplets/recipe/brewing/

Run:  python3 scripts/brewing/generate.py          (rewrite files)
      python3 scripts/brewing/generate.py --check  (fail if the files on disk differ)

Minecraft 26.3 has no brewing registration in code: each mix is a recipe for one container, and each container
change (gunpowder, dragon's breath) is a recipe for one potion, like vanilla's data/minecraft/recipe/brewing. Names
follow vanilla: <input container>_<input potion>_<reagent>. Every recipe loads only with gameplay.toml
effects.quenchnessPotion (load condition blue_droplets:quenchness_potion).
"""
import json
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
OUT = ROOT / "src/main/resources/data/blue_droplets/recipe/brewing"

CONDITIONS = [{"type": "blue_droplets:quenchness_potion"}]
CONTAINERS = ["potion", "splash_potion", "lingering_potion"]
# input potion, reagent, output potion
MIXES = [
    ("minecraft:awkward", "minecraft:prismarine_crystals", "blue_droplets:quenchness"),
    ("blue_droplets:quenchness", "minecraft:redstone", "blue_droplets:long_quenchness"),
    ("blue_droplets:quenchness", "minecraft:glowstone_dust", "blue_droplets:strong_quenchness"),
]
POTIONS = ["blue_droplets:quenchness", "blue_droplets:long_quenchness", "blue_droplets:strong_quenchness"]
# input container, reagent, output container
CONVERSIONS = [
    ("potion", "minecraft:gunpowder", "splash_potion"),
    ("splash_potion", "minecraft:dragon_breath", "lingering_potion"),
]


def path(id_):
    return id_.split(":", 1)[1]


def recipe(container, potion, reagent, out_container, out_potion):
    return {
        "neoforge:conditions": CONDITIONS,
        "type": "minecraft:brewing",
        "input": {"item": "minecraft:" + container, "potion_contents": {"potions": potion}},
        "reagent": {"item": reagent},
        "output": {"id": "minecraft:" + out_container, "components": {"minecraft:potion_contents": {"potion": out_potion}}},
    }


def build():
    files = {}
    for container in CONTAINERS:
        for potion, reagent, out in MIXES:
            files["%s_%s_%s.json" % (container, path(potion), path(reagent))] = recipe(container, potion, reagent, container, out)
    for potion in POTIONS:
        for container, reagent, out_container in CONVERSIONS:
            files["%s_%s_%s.json" % (container, path(potion), path(reagent))] = recipe(container, potion, reagent, out_container, potion)
    return {name: json.dumps(data, indent=2) + "\n" for name, data in files.items()}


def main():
    check = "--check" in sys.argv[1:]
    files = build()
    existing = {p.name for p in OUT.glob("*.json")} if OUT.is_dir() else set()
    diffs = [n for n in sorted(existing - files.keys())]
    diffs += [n for n, text in sorted(files.items()) if not (OUT / n).is_file() or (OUT / n).read_text(encoding="utf-8") != text]
    if check:
        for n in diffs:
            print("differs: " + n)
        return 1 if diffs else 0
    OUT.mkdir(parents=True, exist_ok=True)
    for n in existing - files.keys():
        (OUT / n).unlink()
    for n, text in files.items():
        (OUT / n).write_text(text, encoding="utf-8")
    print("%d brewing recipes (%d changed)" % (len(files), len(diffs)))
    return 0


if __name__ == "__main__":
    sys.exit(main())
