# RsNPC

A powerful, configuration-driven **NPC plugin for [PowerNukkitX](https://github.com/PowerNukkitX/PowerNukkitX)**.

RsNPC lets you place fully customizable human/entity NPCs in your world with skins, armor, emotes, click commands, chat messages, walking routes, multipage dialog windows, and more, all managed through in‑game GUI menus or YAML config files.

---

## Table of contents

- [Features](#features)
- [Commands](#commands)
- [Permissions](#permissions)
- [In-game menus](#in-game-menus)
- [Folder layout](#folder-layout)
- [NPC configuration](#npc-configuration)
- [Skins](#skins)
- [Dialog pages](#dialog-pages)
- [Placeholders / variables](#placeholders--variables)
- [Credits](#credits)

---

## Features

- **Human & custom-entity NPCs**: render as a fake player or as any registered custom entity.
- **Skins**: classic `.png` skins, slim-arm skins, and 4D/animated skins with custom geometry (`skin.json`).
- **Emotes**: play one or more emote animations on a timed loop.
- **Click commands**: run commands when a player clicks the NPC, with player / OP / console permission levels.
- **Click messages**: send chat messages to the player on interaction.
- **Movement & pathfinding**: define walking routes; optional assisted pathfinding navigates around obstacles.
- **Rotation**: spin the NPC in place (great for showcasing skins).
- **Dialog windows**: multipage Bedrock NPC dialog screens with branching buttons.
- **Per-NPC tuning**: display name, name-tag visibility, held item & armor, entity scale, look-at-player, custom collision box, and more.
- **Subfolder organization**: group NPCs and skins into nested folders (see [Folder layout](#folder-layout)).
- **In-game GUI**: create and fully manage NPCs without ever touching a config file.
- **Bilingual**: ships with English (`eng`) and Simplified Chinese (`chs`) language files; the active language follows the server language.

---

## Commands

The base command is `/rsnpc`. Running it with no arguments opens the management GUI.

| Command                  | Description                                                                                                                   |
|--------------------------|-------------------------------------------------------------------------------------------------------------------------------|
| `/rsnpc`                 | Open the main GUI menu.                                                                                                       |
| `/rsnpc create <name>`   | Create an NPC at your current position, facing your current direction. Supports subfolders: `/rsnpc create shops/blacksmith`. |
| `/rsnpc delete <name>`   | Remove an NPC and delete its config file.                                                                                     |
| `/rsnpc addroute <name>` | Append your current position to the NPC's movement route.                                                                     |
| `/rsnpc reload`          | Reload all skins, dialog pages, and NPC configs from disk.                                                                    |

> **NPC names are paths.** An NPC's name is its file path relative to the `Npcs/` folder, without the `.yml` extension. So an NPC stored at `Npcs/shops/blacksmith.yml` is referenced as `shops/blacksmith` in commands. Top-level NPCs keep their plain file name.

---

## Permissions

| Permission             | Default | Grants                         |
|------------------------|---------|--------------------------------|
| `RsNPC.admin`          | op      | Use `/rsnpc` and open the GUI. |
| `RsNPC.admin.create`   | op      | Create NPCs.                   |
| `RsNPC.admin.delete`   | op      | Delete NPCs.                   |
| `RsNPC.admin.reload`   | op      | Reload the plugin.             |
| `RsNPC.admin.addroute` | op      | Add route points.              |

---

## In-game menus

Open the GUI with `/rsnpc`. Buttons are shown based on your permissions.

**Main menu**
- **Create NPC**: opens a form to name and spawn a new NPC where you stand.
- **Manage NPC**: lists every loaded NPC; pick one to edit it.
- **Reload configuration**: reloads the plugin.

**Manage NPC → (select an NPC)**
A summary screen shows the NPC's full configuration and offers:
- **Modify the basic configuration**: display name, name-tag always visible, held item & 4 armor slots, skin (dropdown of all loaded skins), entity network ID, scale, look-at-player, allow-projectile-trigger, enable dialog, and which dialog page to use.
- **Modify emotes**: toggle emotes, set emote IDs (separate multiple with `;`), and the play interval in seconds.
- **Modify the click command**: add new click commands (choosing Player / OP / Console execution) or delete existing ones.
- **Modify click message**: add or delete chat messages sent on click.
- **Delete NPC**: remove the NPC.

All changes made in the GUI are saved to the NPC's YAML file and applied immediately.

---

## Folder layout

After first launch, `plugins/RsNPC/` contains:

```
plugins/RsNPC/
├── Npcs/                 # one .yml file per NPC (subfolders allowed)
├── Skins/                # skin images / skin folders (subfolders allowed)
├── Dialog/               # dialog page configs (e.g. demo.yml)
└── Language/             # chs/ and eng/ language + description files
```

**Subfolders are fully supported** in both `Npcs/` and `Skins/`, to any depth, so you can organize hundreds of NPCs:

```
Npcs/
├── spawn/
│   ├── greeter.yml       → NPC "spawn/greeter"
│   └── guide.yml         → NPC "spawn/guide"
├── shops/
│   └── blacksmith.yml    → NPC "shops/blacksmith"
└── boss.yml              → NPC "boss"
```

Because each NPC's name is its relative path, two files named `greeter.yml` in different folders are **distinct NPCs** and both load without conflict. The same path-based naming applies to skins.

---

## NPC configuration

Each NPC is a YAML file in `Npcs/`. New NPCs are created from a template; here are the fields:

| Key                                               | Description                                                                                                                                         |
|---------------------------------------------------|-----------------------------------------------------------------------------------------------------------------------------------------------------|
| `name`                                            | The NPC's display name (the floating name tag).                                                                                                     |
| `nameTagAlwaysVisible`                            | `true` = always visible; `false` = only when the player looks at the NPC.                                                                           |
| `spawn_point`                                     | NPC coordinates: `x`, `y`, `z`, `yaw`, and `level` (world name).                                                                                    |
| `handheldItem`                                    | Held item ID, e.g. `minecraft:apple:0`. Empty = nothing held.                                                                                       |
| `headItem` / `chestItem` / `legItem` / `footItem` | Armor slot item IDs. Empty = no armor.                                                                                                              |
| `skin`                                            | Name (path) of the skin to use, see [Skins](#skins).                                                                                                |
| `networkId`                                       | Entity network ID to send to clients. Use `-1` to render as a fake player (recommended).                                                            |
| `scale`                                           | Entity size multiplier.                                                                                                                             |
| `lookPlayer`                                      | Whether the NPC turns to face the nearest player.                                                                                                   |
| `emoji.enable`                                    | Whether the NPC plays emotes.                                                                                                                       |
| `emoji.id`                                        | List of emote IDs to play.                                                                                                                          |
| `emoji.interval`                                  | Seconds between emote plays.                                                                                                                        |
| `allow_projectile_trigger`                        | Whether arrows/snowballs etc. can trigger the NPC.                                                                                                  |
| `click_command`                                   | List of commands run on click (see below).                                                                                                          |
| `send_message`                                    | List of chat messages sent on click.                                                                                                                |
| `basic_speed`                                     | Movement speed.                                                                                                                                     |
| `route`                                           | List of route points (add them with `/rsnpc addroute`).                                                                                             |
| `enable_assisted_pathfinding`                     | `true` = navigate around obstacles to each point; `false` = move directly (cheaper, for flat terrain).                                              |
| `rotate`                                          | Spin angle per tick; negative reverses. Enabling this disables movement. Handy for rotating a skin.                                                 |
| `dialog.enable`                                   | Whether clicking opens a dialog window.                                                                                                             |
| `dialog.page`                                     | The dialog config file to open.                                                                                                                     |
| `CustomEntity.*`                                  | Render as a registered custom entity (`enable`, `identifier`, `skinId`). Requires a matching resource pack; players must rejoin after registration. |
| `CustomCollisionSize.*`                           | Override the clickable collision box (`enable`, `width`, `length`, `height`), useful for unusual skin shapes.                                       |

### Click commands

Each entry in `click_command` is `command&permission`. The text before `&` is the command; the suffix after `&` sets the execution context:

- *(no suffix)*: run with **player** permissions
- `&op`: run with **OP** permissions
- `&con`: run with **console** permissions

```yaml
click_command:
  - "give @p apple 1&con"
  - "say Hello!&op"
  - "me waves"            # runs as the player
```

> Each NPC config also includes a `ConfigVersion` field used for automatic config upgrades. **Do not edit it manually!**
> A descriptive, commented copy of every field is generated alongside the configs (see `Language/<lang>/NpcConfigDescription.yml`).

---

## Skins

Place skins in the `Skins/` folder (subfolders allowed). A skin's name is its path relative to `Skins/` (without the extension/suffix), and that's the value you put in an NPC's `skin:` field.

- **Classic skin**: `admin.png` → skin `admin`
- **Slim-arm skin**: add the `_slim` suffix: `helper_slim.png` → skin `helper`
- **4D / animated skin**: a folder containing `skin.png` (or `skin_slim.png`) **and** `skin.json` (the geometry/model). The folder name is the skin name. Supported model format versions include `1.8.0`, `1.10.0`, `1.12.0`, and `1.16.0`.
- **Organization**: any folder that does *not* directly contain `skin.png`/`skin_slim.png` is treated as an organizational subfolder and is searched recursively.

```
Skins/
├── steve.png                 → skin "steve"
├── staff/
│   ├── admin.png             → skin "staff/admin"
│   └── helper_slim.png       → skin "staff/helper"   (slim)
└── bosses/
    └── dragon/               → skin "bosses/dragon"  (4D skin)
        ├── skin.png
        └── skin.json
```

If an NPC references a skin name that doesn't exist, it falls back to the default skin and logs a warning naming the NPC.

---

## Dialog pages

Dialog configs live in `Dialog/` (a `demo.yml` is generated as a reference) and are opened by NPCs with `dialog.enable: true` and `dialog.page` set to the file name.

A dialog file defines a set of **pages**, each with a title, content, and buttons. Buttons can:
- run commands (`cmd:`, using the same `&op` / `&con` permission suffixes as click commands),
- jump to another page (`go:` a page `key`),
- or close the dialog (`action: "close"`).

A page may also define a `close:` block to jump somewhere when the player closes it, enabling branching flows. See the generated `Dialog/demo.yml` for a complete working example.

---

## Placeholders / variables

These placeholders can be used in click commands and messages:

| Placeholder | Replaced with                  |
|-------------|--------------------------------|
| `@p`        | The interacting player's name. |
| `%npcName%` | The NPC's name.                |
| `\n`        | A line break.                  |

---

## Credits

RsNPC was originally created by the **MemoriesOfTime** team. This fork stands entirely on their work, and the founding authors deserve full credit:

- **若水 (SmallasWater)**: founding author
- **LT_Name**: founding author & maintainer of the original project

Original project: <https://github.com/MemoriesOfTime/RsNPC>

This fork continues their work focusing on PowerNukkitX 3.0.0 compatibility and organizational improvements. Thank you to the original authors and contributors for building such a flexible NPC system. 🙏
