# Tooling de la compat con Farmer's Delight

Scripts en Python 3 estándar. Se corren desde cualquier carpeta; los CSV de `data/` son los que se editan, los scripts los vuelven a aplicar cuantas veces haga falta.

## Flujo

- `python3 scripts/delight/extract_recipes.py` lee las recetas de los jars fijados en `mods.json` (se bajan de Modrinth a `~/.cache/blue-droplets-delight`, fuera del repo) y escribe `data/recipes.csv` con las que usan agua. Con `--source repo` lee los repos clonados en su lugar.
- `python3 scripts/delight/classify.py` pone una categoría a cada ítem de `data/items_raw.csv` con las reglas ordenadas de `rules/categories.toml` (primera que coincide gana: tags, efectos, palabras del id) y escribe `data/categories.csv` (`item_id,category,reason`, con la regla que decidió). `data/categories_manual.csv` (`item_id,category`) gana siempre. `--sample N` muestra ejemplos por categoría y `--show CAT` lista una categoría entera.
- `python3 scripts/delight/prepare_items.py` toma `data/items_raw.csv` (lo genera el comando de desarrollo del mod) y escribe `data/items.csv` con comida y bebidas, sus pasos de crafteo y los valores iniciales de `rules/items.json`.
- `python3 scripts/delight/apply.py` genera el datapack `clean_water_cooking`, las entradas de `drinks.json` y los tags `salty` y `no_thirst`. Con `--check` solo compara y sale con 1 si algo difiere.
- `python3 scripts/delight/report.py` escribe `data/REPORT.md`, `data/REPORT_recipes.csv` y `data/REPORT_items.csv` para revisar.

## Qué se edita

- `data/recipes.csv`: `heat`, `min_purity` (1 a 3, es un mínimo: 2 acepta del 2 al 5; 3 acepta del 3 al 5; el agua sin componente cuenta como 3), `enabled` (1 o 0) y `notes`. Al volver a correr `extract_recipes.py` se conservan; `--reapply-rules` los recalcula con las reglas.
- `data/items.csv`: `thirst`, `quenched`, `salty` (si/no), `no_thirst` (si/no), `category` y `notes`. `prepare_items.py` actualiza con la fórmula nueva las filas que no se tocaron y respeta las editadas; `--force` las pisa. `category` y `notes` nunca se pisan.
- `rules/heat.json`: calor por tipo de receta y por palabras del resultado, y la pureza mínima de cada calor.
- `rules/water.json`: cómo se reconoce el agua, tipos de receta soportados y filas manuales.
- `rules/items.json`: la fórmula de sed (valores por categoría, palabras, bonus por pasos, regla de Hunger y Poison).
- `rules/categories.toml`: las reglas de `classify.py`; `data/categories_manual.csv` para correcciones sueltas.
- `mods.json`: mods y versiones fijadas.

## Reglas de apply.py

- Cada fila activa produce la receta original con el agua cambiada por `neoforge:difference` (agua del original menos las purezas por debajo del mínimo). Con la pureza apagada el agua no guarda componente y se acepta igual. Cada receta lleva `neoforge:mod_loaded` de su mod además de sus condiciones.
- Un tipo de receta fuera de `supported_types` no se escribe y se avisa. Antes de escribir, y en `--check` sobre lo que hay en disco, se verifica que cada ingrediente acepte agua sin componente y purezas mayores o iguales al mínimo, y rechace las menores.
- Si la receta cambió en el mod desde la última extracción, `apply.py` lo avisa y no la escribe hasta volver a correr `extract_recipes.py`.
- `drinks.json` solo se toca en las claves que están en `items.csv`; el resto queda igual. Las filas con thirst y quenched en 0 no llevan entrada.
- Los packs `purify_*` (incluida la olla de FD, ids terminados en `_manual_only`) y las recetas de Create los genera `python3 scripts/purify/generate.py`, no `apply.py`.


## Cobertura y fuentes de verdad

- `items_raw.csv` puede traer cualquier namespace (`minecraft`, `blue_droplets`, mods con compat, la tanda Delight). `apply.py` escribe todas las entradas de `drinks.json` desde `items.csv`: sin condición para `minecraft` y `blue_droplets`, con `neoforge:mod_loaded` para el resto.
- En `drinks.json` solo se tocan las claves que están en `items.csv`; los tags (`#c:drinks/juice`), los mods no volcados y los campos que el CSV no maneja (por ejemplo `purity`) se conservan.
- Es la única fuente de valores de bebida y comida del repo (`hydrating_blocks.json` es de bloques y va aparte). Todo sale como datos de datapack, así que un modpack lo cambia con su propio `drinks.json`, los tags `salty` y `no_thirst` o recetas con el mismo id, sin tocar el jar.
- Filas congeladas: `manual_ids` en `rules/items.json` (la botella y el cubo de agua, el cuenco, la cantimplora) se toman de `drinks.json` y llevan `auto = manual`. Para congelar otra fila escribe `manual` en su columna `auto`. Una fila cuyos valores ya no coinciden con `auto` (la editaste) tampoco se pisa.
- `data/categories.csv` lo escribe `classify.py`. Con una categoría conocida (`categories` en `rules/items.json`) los valores salen de la categoría; si no, de las palabras. Una categoría nueva sin regla sale como aviso.

## Agregar un mod nuevo

- Volcar sus ítems con `/blue_droplets dev dump_items <namespace>` a `data/items_raw.csv` (el volcado lleva todos los namespaces que quieras en un solo archivo).
- Opcional, para `craft_steps` y las recetas con agua: agregarlo a `mods.json` (id, `modrinth` y `version`, o `repo` y `data_roots`). Sin eso `craft_steps` queda en 0.
- Correr `extract_recipes.py`, `prepare_items.py`, `apply.py` y `report.py`. No hay que tocar código.
- Los packs `clean_water_cooking` y `purify_cooking_pot` los registra `BlueDroplets.addPacks`; `apply.py` avisa si falta alguno.
