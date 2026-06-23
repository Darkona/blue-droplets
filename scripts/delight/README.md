# Tooling de la compat con Farmer's Delight

Scripts en Python 3 estándar. Se corren desde cualquier carpeta; los CSV de `data/` son los que se editan, los scripts los vuelven a aplicar cuantas veces haga falta.

## Flujo

- `python3 scripts/delight/extract_recipes.py` lee las recetas de los jars fijados en `mods.json` (se bajan de Modrinth a `~/.cache/blue-droplets-delight`, fuera del repo) y escribe `data/recipes.csv` con las que usan agua. Con `--source repo` lee los repos clonados en su lugar.
- `python3 scripts/delight/prepare_items.py` toma `data/items_raw.csv` (lo genera el comando de desarrollo del mod) y escribe `data/items.csv` con comida y bebidas, sus pasos de crafteo y los valores iniciales de `rules/items.json`.
- `python3 scripts/delight/apply.py` genera los datapacks `clean_water_cooking` y `purify_cooking_pot`, las entradas de `drinks.json` y los tags `salty` y `no_thirst`. Con `--check` solo compara y sale con 1 si algo difiere.
- `python3 scripts/delight/report.py` escribe `data/REPORT.md`, `data/REPORT_recipes.csv` y `data/REPORT_items.csv` para revisar.

## Qué se edita

- `data/recipes.csv`: `heat`, `min_purity` (1 o 2, es un mínimo: 1 acepta 1, 2 y 3; 2 acepta 2 y 3), `enabled` (1 o 0) y `notes`. Al volver a correr `extract_recipes.py` se conservan; `--reapply-rules` los recalcula con las reglas.
- `data/items.csv`: `thirst`, `quenched`, `salty` (si/no), `no_thirst` (si/no), `category` y `notes`. `prepare_items.py` actualiza con la fórmula nueva las filas que no se tocaron y respeta las editadas; `--force` las pisa. `category` y `notes` nunca se pisan.
- `rules/heat.json`: calor por tipo de receta y por palabras del resultado, y la pureza mínima de cada calor.
- `rules/water.json`: cómo se reconoce el agua, tipos de receta soportados y filas manuales.
- `rules/items.json`: la fórmula de sed, con la regla de Hunger y Poison.
- `mods.json`: mods y versiones fijadas.

## Reglas de apply.py

- Cada fila activa produce la receta original con el agua cambiada por `neoforge:difference` (agua del original menos las purezas por debajo del mínimo). Con la pureza apagada el agua no guarda componente y se acepta igual. Cada receta lleva `neoforge:mod_loaded` de su mod además de sus condiciones.
- Un tipo de receta fuera de `supported_types` no se escribe y se avisa. Antes de escribir, y en `--check` sobre lo que hay en disco, se verifica que cada ingrediente acepte agua sin componente y purezas mayores o iguales al mínimo, y rechace las menores.
- Si la receta cambió en el mod desde la última extracción, `apply.py` lo avisa y no la escribe hasta volver a correr `extract_recipes.py`.
- `drinks.json` solo se toca en las claves que están en `items.csv`; el resto queda igual. Las filas con thirst y quenched en 0 no llevan entrada.
- `purify_cooking_pot` se deriva de `purify_campfire`: mismas etapas, en la olla de FD, con ids terminados en `_manual_only`.

## Pendiente en Java

`BlueDroplets.addPacks` tiene que registrar `clean_water_cooking` y `purify_cooking_pot` (`PackSource.BUILT_IN`).
