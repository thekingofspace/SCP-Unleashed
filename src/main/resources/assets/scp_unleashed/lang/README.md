# SCP Unleashed translations

This repository holds all the text for the SCP Unleashed Minecraft mod (NeoForge 1.21.1). English lives in `en_us/`. Every other language gets its own folder, named with its Minecraft language code.

## Adding or improving a language

1. Copy the `en_us` folder and rename the copy to your language code, for example `de_de`, `fr_fr`, `es_es`, `pt_br`, `ru_ru`, `ja_jp` or `zh_cn`. Use the same code Minecraft uses for that language.
2. Translate the values on the right. Never change the keys on the left.
3. You don't have to translate everything. Any key you leave out shows the English text, so you can delete files or keys you haven't translated yet.
4. Open a pull request.

## How the files are laid out

- `en_us/001.json`, `en_us/079.json`, ...: one file per anomaly, holding its items, blocks, messages and dossier.
- `en_us/strangers.json`: everything the strangers say.
- `en_us/blocks.json`, `items.json`, `gui.json`, `jei.json`, `messages.json` and the other general files: shared text.
- `en_us.json`: intentionally empty. Leave it alone and don't create one for your language.

Only the keys matter. Inside your language folder you can split, merge, rename or nest files in subfolders however you like.

Files can be flat or nested. Nested objects join their names with dots, and list entries are numbered from 0:

```json
{ "gui.scp_unleashed": { "close": "Close", "pages": ["One", "Two"] } }
```

is the same as

```json
{ "gui.scp_unleashed.close": "Close", "gui.scp_unleashed.pages.0": "One", "gui.scp_unleashed.pages.1": "Two" }
```

## Formatting codes

| Code | Meaning |
|---|---|
| `%s`, `%1$s`, `%2$s` | Filled in by the game (a name, a number, ...). Keep every one. You can reorder the numbered ones to fit your grammar. |
| `%%` | A literal percent sign. |
| `%rtext%r` | Redacted text in a dossier, drawn as a black bar. Put the markers around the matching words in your language. |
| `[[text]]` | An older way of writing redacted text. Treat it like `%r`. |
| `**text**` | Bold text in a dossier. |

## Dossiers

Each anomaly's dossier sits under `dossier.scp_unleashed.<id>`:

```json
"dossier.scp_unleashed.079": {
  "designation": "SCP-079",
  "title": "Old AI",
  "caption": "...",
  "item": "SCP-079",
  "class": "Euclid",
  "page": [
    { "title": "Special Containment Procedures:", "text": ["First paragraph.", "Second paragraph."] },
    { "title": "Addendum 079-1: Interview Log", "text": "A single paragraph." }
  ]
}
```

- `text` can be one string or a list of paragraphs. The game flows the text onto as many sheets as it needs.
- Your language may have more or fewer pages than English. But once you translate any page of a dossier, your page list replaces the English one completely, so list every page you want shown.
- `designation`, `title`, `caption`, `item` and `class` fall back to English one by one.

## Stranger dialogue

Each stranger has a list of lines, and each line is one page of dialogue. You can use more or fewer lines than English.

## Testing in game

You don't need to build the mod to test a translation:

1. Make a resource pack folder containing your translation at `assets/scp_unleashed/lang/<your code>/`.
2. Add a `pack.mcmeta` next to `assets`: `{ "pack": { "pack_format": 34, "description": "SCP Unleashed translation" } }`
3. Put the folder in `.minecraft/resourcepacks`, enable the pack, and pick your language. Press F3+T to reload after each edit.

## Keeping up with English

When the English text changes, `git log -p -- en_us/` shows exactly which lines were added or reworded, so you can see what needs updating.
