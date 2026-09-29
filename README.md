# Create: Architect

Create: Architect is an experimental NeoForge addon for Create's construction
workflow. Development is deliberately milestone-based; the current codebase is
Phase 0 plus a Milestone 1 persistent-schematic-preview proof of concept.

## Development target

- Minecraft 1.21.1
- NeoForge 21.1.252
- Java 21 toolchain
- Create 6.0.10
- Mod ID `createarchitect`

Build with `./gradlew build` and launch the development client with
`./gradlew runClient`.

With a deployed schematic in a stopped Schematicannon, its hologram remains
visible through Create's native schematic handler. Look at the cannon and press
`G` to enter or leave Architect Mode; while editing, Create's normal schematic
tool-menu controls move, rotate, and mirror the cannon's stored schematic.

See [CREATE_RESEARCH.md](CREATE_RESEARCH.md) for the upstream architecture audit,
implementation rationale, verification status, and known limitations.
