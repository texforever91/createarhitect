# Create: Architect

Create: Architect is an experimental addon for the Minecraft mod
[Create](https://github.com/Creators-of-Create/Create). Its goal is to expand
Create's Schematicannon and schematic workflow into a more capable system for
planning and managing large construction projects.

Create currently focuses on a straightforward workflow: one schematic is
deployed, placed into one Schematicannon, and built as one construction job.
Create: Architect explores what that system could become if schematics remained
visible, could be edited from the cannon, understood the surrounding terrain,
and could eventually be organised into coordinated multi-schematic projects.

> **Early alpha:** This project is an experimental proof of concept. Features,
> controls, saved data, and compatibility may change. Back up important worlds
> before testing it.

## Project origin and AI disclosure

The idea and direction for Create: Architect came from **Tex**, who wanted this
kind of construction-planning addon but could not find an existing project that
provided it.

Tex is the project originator and tester, not the programmer. The repository's
code and documentation were produced with an OpenAI Codex coding agent under
Tex's direction, feedback, and testing. The project should therefore be
understood as an AI-assisted community experiment built to explore an idea—not
as code personally written by Tex.

Bug reports and contributions are welcome, but users should review the code and
use alpha builds with the same caution they would apply to any experimental mod.

## Current functionality

The current alpha provides the first persistent-schematic workflow:

- A deployed schematic placed inside a stopped Schematicannon remains visible
  even when the player is no longer holding the schematic item.
- The preview reuses Create's native schematic renderer and visual style.
- Create's schematic boundary remains visible around the planned structure.
- Pressing `G` enters Architect Mode for the selected Schematicannon.
- Create's normal schematic toolbar can move, rotate, and mirror the schematic
  stored inside the cannon.
- A button in the Schematicannon screen opens a detached free camera so large
  builds can be viewed without physically moving the player.
- Entering freecam from a stopped cannon also opens Create's schematic editing
  toolbar; the configured edit key can independently hide or restore them.
- Placement changes are validated by the server and written back to the
  schematic held by the stopped cannon.
- Leaving Architect Mode returns the camera to the player and removes the
  editing-only placement visualization.

## Architect Mode controls

First deploy a normal Create schematic and place it into a Schematicannon. Open
the Schematicannon screen and use its freecam button to detach the camera; this
also works while the cannon is running. Freecam enables the editing controls
immediately. Applying the first transform to an active cannon safely stops and
resets its printer plan before updating the schematic placement.

| Control | Action |
| --- | --- |
| `G` (configurable) | Enable or disable schematic editing |
| Mouse | Look around in freecam |
| `W`, `A`, `S`, `D` | Move the free camera |
| `Space` | Move the camera upward |
| `Shift` | Move the camera downward |
| `Ctrl` | Increase camera speed |
| `V` (configurable) | Leave freecam and return to the player |
| Create schematic toolbar controls | Move, rotate, or mirror the schematic |

The free camera is currently limited to 192 blocks from the selected cannon.
The player's real position does not move while freecam is active.

## Planned roadmap

The roadmap describes the intended direction, not functionality promised for a
particular release. Development is deliberately incremental so the persistent
preview foundation can be tested before larger systems are added.

### 1. Stable persistent previews

- Preview on/off controls
- Better selection when several Schematicannons are nearby
- Improved culling and performance for very large schematics
- More polished Architect Mode controls and camera behaviour
- Multiplayer testing and clearer permissions

### 2. Intelligent construction preview

Give planned blocks different visual states, such as:

- Already correct
- Missing and ready to place
- Waiting for materials
- Will replace an existing block
- Will remove an existing block
- Protected or ignored
- Completed

### 3. Construction rules

Allow a project or cannon to define how it may change the world. Possible modes
include:

- **Build Only:** place missing schematic blocks without clearing terrain
- **Exact:** make the affected area match the schematic
- **Clear Interior:** remove blocks that obstruct required interior space
- **Foundation:** modify only the terrain needed to support the structure
- **Preserve:** never modify configured blocks
- **Ignore:** exclude configured blocks from construction calculations

Planned filters may preserve ores, inventories, spawners, trees, water, or lava,
and could support rules such as replacing one block type with another.

### 4. Terrain-aware construction

Instead of blindly clearing an entire schematic box, the addon should calculate
which terrain genuinely interferes with a structure. Planned options include
footprint-only excavation, foundations, exact-volume clearing, configurable
padding, and a preview of every terrain change before building begins.

### 5. Project Blueprints

A future Project Blueprint—also referred to as a Schematic Portfolio—would
store several related schematic placements as one construction project. For
example, a mountain base might contain separate foundation, house, roof,
workshop, railway station, and landscaping schematics.

Each child schematic could retain its relative position, rotation, mirror,
construction rules, enabled state, and build order. Moving the project's master
anchor would transform the entire collection together.

### 6. Project controller and coordinated logistics

A later controller block, tentatively called the **Architect's Table** or
**Construction Coordinator**, could manage:

- Overall and per-schematic progress
- Build order and dependencies
- Missing-material totals
- Multiple coordinated Schematicannons
- Shared inventories and Create logistics
- Pausing, resuming, and inspecting project stages

The long-term goal is a workflow closer to:

> construction project → multiple schematics → persistent previews →
> construction rules → terraforming → coordinated Schematicannons → shared
> logistics

## Requirements

The current development target is:

- Minecraft 1.21.1
- NeoForge 21.1.252
- Create 6.0.10
- Java 21

Only NeoForge on Minecraft 1.21.1 is currently targeted. Multi-loader and
multi-version support are outside the present scope.

## Installation

1. Install the supported Minecraft 1.21.1 version of NeoForge.
2. Install Create 6.0.10 and all dependencies required by Create.
3. Place the Create: Architect `.jar` in the instance's `mods` directory.
4. Start Minecraft and confirm that Create: Architect appears in the mod list.

Do not assume that an alpha build is safe for irreplaceable worlds. Keep a world
backup and test with a copy first.

## Building from source

Use a Java 21 development environment, clone the repository, and run:

```bash
./gradlew build
```

The built mod will be written to `build/libs/`.

To launch the development client:

```bash
./gradlew runClient
```

Technical research and implementation notes are available in
[`CREATE_RESEARCH.md`](CREATE_RESEARCH.md).

## Known limitations

- This is an early proof of concept, not a finished construction-management
  system.
- The persistent preview currently works with deployed schematics stored in
  stopped Schematicannons.
- The intelligent-preview, construction-rule, terraforming, project-blueprint,
  and coordination features described above are not implemented yet.
- Large schematics and multiplayer environments need broader performance and
  compatibility testing.
- Compatibility is currently tied to the listed Minecraft, NeoForge, and Create
  versions.
- Other mods that substantially change Create's schematic client may conflict
  with this addon.

## Contributing and testing

Testing feedback is especially useful while the project is in alpha. When
reporting a problem, include the Minecraft, NeoForge, Create, and Create:
Architect versions; whether the issue occurred in single-player or multiplayer;
and clear reproduction steps. Logs, screenshots, and a small test schematic can
also help.

Contributors should keep the addon incremental and avoid modifying or forking
Create itself. Public APIs and events are preferred; invasive hooks should be
limited to places where Create does not currently expose the required addon
integration.

## License

Create: Architect is available under the [MIT License](LICENSE).

This repository does not grant rights to Minecraft, NeoForge, Create, or any
third-party assets. Their respective names, code, and assets remain subject to
their own licenses and terms.

## Disclaimer

Create: Architect is an unofficial addon and is not affiliated with, maintained
by, or endorsed by Mojang Studios, Microsoft, the NeoForge project, or the
Create development team.
