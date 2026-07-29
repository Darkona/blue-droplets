# Tooling de la compat con Farmer's Delight

Scripts en Python 3 estándar. Se corren desde cualquier carpeta; los CSV de `data/` son los que se editan, los scripts los vuelven a aplicar cuantas veces haga falta.

Esta es la versión de la rama 1.18.2 (Forge 40): lee las recetas de `data/<ns>/recipes/` de los jars de Forge 1.18.2 y escribe los formatos de 1.18.2, que son los de 1.20.1 (`conditions`, `forge:difference`, `forge:partial_nbt`, `forge:conditions` en `drinks.json`, carpetas `recipes/` y `tags/items/`).

## Flujo

- `python3 scripts/delight/extract_recipes.py` lee las recetas de los jars fijados en `mods.json` (se bajan de Modrinth, versiones Forge 1.18.2, a `~/.cache/blue-droplets-delight`, fuera del repo) y escribe `data/recipes.csv` con las que usan agua. Con `--source repo` lee los repos clonados en su lugar (la rama de 1.18.2 de cada mod).
- `python3 scripts/delight/classify.py` pone una categoría a cada ítem de `data/items_raw.csv` con las reglas ordenadas de `rules/categories.toml` (primera que coincide gana: tags, efectos, palabras del id) y escribe `data/categories.csv` (`item_id,category,reason`, con la regla que decidió). `data/categories_manual.csv` (`item_id,category`) gana siempre. `--sample N` muestra ejemplos por categoría y `--show CAT` lista una categoría entera.
- `python3 scripts/delight/prepare_items.py` toma `data/items_raw.csv` (lo genera el comando de desarrollo del mod `/blue_droplets dev dump_items *` con los mods de `-PwithDelight -PwithCompat`) y escribe `data/items.csv` con comida y bebidas, sus pasos de crafteo y los valores iniciales de `rules/items.json`.
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

- Cada fila activa produce la receta original con el agua cambiada por `forge:difference` (el ingrediente de agua del original menos un `forge:partial_nbt` por cada pureza por debajo del mínimo, sobre los ítems que `rules/water.json` da para ese ítem o tag). Con la pureza apagada el agua no guarda NBT y se acepta igual. Cada receta lleva `forge:mod_loaded` de su mod además de sus condiciones.
- Solo se sobrescriben ingredientes de ítem: los ingredientes de fluido de 1.20.1 no tienen resta, así que una receta con agua como fluido queda con `enabled=0`. Es el caso del keg de Brewin' and Chewin' 1.20.1, que compara el agua con un `FluidStack` sin NBT y la crea sin NBT al verter: la regla del agua limpia no se puede exigir con datos.
- Un tipo de receta fuera de `supported_types` no se escribe y se avisa. Antes de escribir, y en `--check` sobre lo que hay en disco, se verifica que cada ingrediente acepte agua sin componente y purezas mayores o iguales al mínimo, y rechace las menores.
- Si la receta cambió en el mod desde la última extracción, `apply.py` lo avisa y no la escribe hasta volver a correr `extract_recipes.py`.
- `drinks.json` solo se toca en las claves que están en `items.csv`; el resto queda igual. Las filas con thirst y quenched en 0 no llevan entrada.
- Los packs `purify_*` (incluida la olla de FD, ids terminados en `_manual_only`) y las recetas de Create los genera `python3 scripts/purify/generate.py`, no `apply.py`.


## Cobertura y fuentes de verdad

- `items_raw.csv` puede traer cualquier namespace (`minecraft`, `blue_droplets`, mods con compat, la tanda Delight). `apply.py` escribe todas las entradas de `drinks.json` desde `items.csv`: sin condición para `minecraft` y `blue_droplets`, con `forge:mod_loaded` para el resto.
- En `drinks.json` solo se tocan las claves que están en `items.csv`; los tags (`#forge:drinks/juice`), los mods no volcados y los campos que el CSV no maneja (por ejemplo `purity`) se conservan.
- Es la única fuente de valores de bebida y comida del repo (`hydrating_blocks.json` es de bloques y va aparte). Todo sale como datos de datapack, así que un modpack lo cambia con su propio `drinks.json`, los tags `salty` y `no_thirst` o recetas con el mismo id, sin tocar el jar.
- Filas congeladas: `manual_ids` en `rules/items.json` (la botella y el cubo de agua, el cuenco, la cantimplora) se toman de `drinks.json` y llevan `auto = manual`. Para congelar otra fila escribe `manual` en su columna `auto`. Una fila cuyos valores ya no coinciden con `auto` (la editaste) tampoco se pisa.
- `data/categories.csv` lo escribe `classify.py`. Con una categoría conocida (`categories` en `rules/items.json`) los valores salen de la categoría; si no, de las palabras. Una categoría nueva sin regla sale como aviso.

## Agregar un mod nuevo

- Volcar sus ítems con `/blue_droplets dev dump_items <namespace>` a `data/items_raw.csv` (el volcado lleva todos los namespaces que quieras en un solo archivo).
- Opcional, para `craft_steps` y las recetas con agua: agregarlo a `mods.json` (id, `modrinth` y `version`, o `repo` y `data_roots`). Sin eso `craft_steps` queda en 0.
- Correr `extract_recipes.py`, `prepare_items.py`, `apply.py` y `report.py`. No hay que tocar código.
- Los packs `clean_water_cooking` y `purify_cooking_pot` los registra `BlueDroplets.addPacks`; `apply.py` avisa si falta alguno.

## Archivos fijos

Lo que haya en `static/clean_water_cooking/` se copia tal cual al pack `clean_water_cooking` cada vez que corre `apply.py` (por ejemplo, arreglos de recetas rotas de otros mods; en 1.20.1 no hace falta ninguno y la carpeta no existe). Todo lo demás del pack que no genere el script se borra.

## Rama 1.18.2

- Mods: Farmer's Delight 1.2.3, Brewin' and Chewin' 1.0.1, Ocean's Delight 1.0.0, Ender's Delight 1.2.1, Miner's Delight 1.1.1 (`miners_delight`), Corn Delight 1.0.6, Crabber's Delight 1.1.2 y End's Delight 1.2.1 (mod id `ends_delight`), en sus versiones Forge 1.18.2 de `mods.json`. Fruits Delight, Cultural Delights, Rustic Delight, My Nether's Delight (y Cook's Collection) no tienen versión Forge 1.18.2 en Modrinth, además de los que ya faltaban en 1.19.2.
- `data/items_raw.csv` sale de un volcado con `-PwithCompat -PwithDelight` en 1.18.2. Los ítems que existen en 1.19.2 conservan sus valores; la botella de leche de Farmer's Delight, cuyo valor bajaba con los pasos de crafteo de 1.18.2, conserva el de 1.19.2 como fila editada.
- `drinks.json` no tiene las claves de ítems que no existen en 1.18.2 (las de los mods que faltan y algunas de Brewin' and Chewin', Crabber's Delight y End's Delight). `extra_mods` queda vacío: las tazas de Miner's Delight que registraba My Nether's Delight no existen.
- Las recetas de fermentar de Brewin' and Chewin' quedan con `enabled=0`, como en 1.19.2 y 1.20.1.

## Rama 1.19.2

- Mods: Farmer's Delight 1.2.4, Brewin' and Chewin' 1.19-2.0, Ocean's Delight 1.0.2, Ender's Delight 1.2.2, Miner's Delight 1.1.1 (`miners_delight`), Fruits Delight 0.5.9, Cultural Delights 0.16.0, Corn Delight 1.0.3, Rustic Delight 1.3.0, Crabber's Delight 1.1.4, End's Delight 2.1 (mod id `ends_delight`) y My Nether's Delight 1.7.6, en sus versiones Forge 1.19.2 de `mods.json`. Farm & Charm, HerbalBrews, Brewery, Extra Delight y Expanded Delight no tienen versión Forge 1.19.2.
- `data/items_raw.csv` sale de un volcado con `-PwithCompat -PwithDelight` en 1.19.2. Los ítems que existen en 1.20.1 conservan sus valores y categorías; los siete cuyos valores cambiaban con los pasos de crafteo de 1.19.2 llevan `auto = manual`.
- `drinks.json` no tiene las claves de ítems que no existen en 1.19.2 ni las de Farm & Charm, HerbalBrews y Brewery.
- Las recetas de fermentar de Brewin' and Chewin', las de añejar de Cultural Delights y las mezclas de Create de Fruits Delight son tipos sin verificar o con agua como fluido: quedan con `enabled=0`, como en 1.20.1.

## Rama 1.20.1

- Mods: Farmer's Delight, Brewin' and Chewin', Ocean's Delight, Ender's Delight, Farm & Charm, HerbalBrews y Miner's Delight (mod id `miners_delight` en 1.20.1), en sus versiones Forge 1.20.1 de `mods.json`. Extra Delight no tiene versión para 1.20.1. La segunda tanda (Fruits 1.1.3, Cultural 0.16.7, Corn 1.2.11, Rustic 1.7.0, Crabber's 1.2.3, End's 2.6.1 y My Nether's 1.8; la 1.10.x pide Forge 47.4) entra solo como datos. Expanded Delight no tiene versión Forge para 1.20.1. Cultural Delights trae archivos vacíos que pisan recetas de vanilla y los scripts los saltan con un aviso. Las recetas de mezcla de Create con fluido (Fruits) y el `brewing/coffee` de Rustic quedan con `enabled=0`. Brewery (Farm & Charm compat) no trae recetas con agua: su agua es de código.
- `classify.py` cuenta cada tag `forge:*` también como `c:*`, así las reglas de `categories.toml` sirven igual.
- Los ítems que existen en las dos versiones conservan las categorías y los valores de 1.21.1 (`data/categories_manual.csv` y las filas editadas de `data/items.csv`), porque las recetas de 1.20.1 dan otros pasos de crafteo.
