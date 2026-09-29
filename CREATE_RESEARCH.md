# Create: Architect — Phase 0 research

Research date: 2026-09-29. Create source inspected at commit
`fc9535d82a29419164a1e9dc9c678bdcddeab30d` on branch `mc1.21.1/dev`.

## Verified development baseline

| Component | Selected version | Basis |
|---|---:|---|
| Minecraft | 1.21.1 | Requested target and Create's supported NeoForge line |
| NeoForge | 21.1.252 | Current official 1.21.1 ModDevGradle MDK value |
| Java | 21 toolchain | Minecraft 1.21.1 target; development launch selected JDK 21.0.12 |
| Create | 6.0.10 (`6.0.10-280`) | Current Create addon documentation dependency coordinate |
| Ponder | 1.0.82 | Create addon documentation |
| Flywheel | 1.0.6 | Create addon documentation |
| Registrate | MC1.21-1.3.0+67 | Create addon documentation |
| Mappings | Parchment 2024.11.17 for 1.21.1 | Current official MDK |
| Gradle | wrapper 9.2.1 | Current official MDK |

The folder was empty and was not a Git repository. It was initialized from the
official NeoForge 1.21.1 ModDevGradle MDK; no previous mod sources were replaced.

## Build and launch status

- `./gradlew clean build`: successful.
- `./gradlew runClient`: successful through mod discovery, mixin application,
  resource reload, Create/Ponder registration, Flywheel shader loading, sound
  startup, and main-menu rendering.
- Loaded mods included Minecraft, NeoForge, Flywheel, Ponder, Create 6.0.10, and
  Create: Architect 0.1.0-alpha.1.
- The machine logs a non-fatal narrator error because system library
  `libflite.so` is unavailable. This is unrelated to the addon.
- A hands-on world test with a deployed schematic and Schematicannon has not yet
  been completed. Therefore the persistent ghost is a compiled, launch-tested
  proof of concept, not yet a gameplay-verified milestone.

## Create schematic data flow

### Item and structure data

`SchematicItem` stores schematic identity and placement as typed data components:

- `SCHEMATIC_OWNER` and `SCHEMATIC_FILE` identify the server/client NBT file.
- `SCHEMATIC_DEPLOYED` gates placement/preview use.
- `SCHEMATIC_ANCHOR` is the world-space master position.
- `SCHEMATIC_ROTATION`, `SCHEMATIC_MIRROR`, and `SCHEMATIC_BOUNDS` describe the
  transformation and dimensions.

`SchematicItem.loadSchematic()` loads an ordinary Minecraft `StructureTemplate`
from Create's schematic storage. Create has not invented a separate schematic
format. `SchematicItem.getSettings()` converts rotation and mirror components to
`StructurePlaceSettings`.

`SchematicSyncPacket` is client-to-server and updates the held schematic's
deployed flag, anchor, rotation, and mirror. Schematic uploads are handled in
chunks by `SchematicUploadPacket`, `ClientSchematicLoader`, and
`ServerSchematicLoader`.

### Client preview

`SchematicHandler` is a singleton owned by `CreateClient`. Each client tick it
looks only for a schematic in the player's main hand. When that condition stops,
the handler becomes inactive; this is why the normal preview disappears.

For a held schematic it loads the `StructureTemplate` into one or more virtual
`SchematicLevel` instances and creates cached `SchematicRenderer` buffers.
Mirrored variants are prebuilt. The handler applies a `SchematicTransformation`
to the pose stack and renders at `RenderLevelStageEvent.Stage.AFTER_PARTICLES`.

`SchematicInstances` is a reusable public cache keyed by a hash of the schematic
item's data-component patch. It creates a transformed `SchematicLevel` directly
at the deployed anchor and expires unused entries after five minutes.
`SchematicRenderer` is also public and caches a `SuperByteBuffer` per chunk render
layer. These two classes are sufficient for Architect's persistent renderer;
there is no need to copy Create's parser or tessellator.

### Schematicannon state and inventory

`SchematicannonBlockEntity` owns a five-slot `SchematicannonInventory`; deployed
schematics enter slot 0. Its persistent server save includes the entire inventory.
The cannon validates that the schematic is deployed and within its maximum anchor
distance before `SchematicPrinter.loadSchematic()` builds a virtual block reader.

Important sync constraint: `SchematicannonBlockEntity.write/read` intentionally
serialize the inventory only when `clientPacket == false`. Normal block-entity
update tags carry progress, options, printer coordinates, flying blocks, and
status, but not the schematic item. `SchematicannonMenu` supplies a full inventory
snapshot only while its GUI is being opened. A persistent renderer therefore
cannot identify the file from Create's normal client block-entity state after the
player walks away.

The inventory's `onContentsChanged()` calls only `setChanged()`, not
`notifyUpdate()`, so changing slot 0 does not itself guarantee a client update.

### Printing and replacement behavior

`SchematicPrinter` loads the transformed structure into a `SchematicLevel`,
tracks bounds/current position, compares planned and world states, and exposes a
placement predicate. `SchematicannonBlockEntity.shouldPlace()` implements the
existing four replacement modes and special cases. The cannon's server tick is
authoritative for inventory consumption, requirements, fuel, progress, block
replacement, launched-block animation, and completion.

This is the appropriate seam for later construction rules: extend or wrap the
placement predicate and requirement calculation on the server. Rendering should
consume a derived immutable preview state, not decide gameplay changes.

## Milestone 1 proof of concept

The original standalone-renderer prototype failed its first in-world test. It
was replaced in alpha.2 with a bridge into Create's complete `SchematicHandler`.
The nearest loaded cannon supplies a virtual active schematic when no schematic
is held; passive input is locked, while `G` aimed at a stopped cannon enables
Create's native placement tools. Transform updates use an Architect serverbound
packet validated against player distance, cannon state, schematic type, and the
cannon's maximum anchor distance.

The underlying sync path remains deliberately narrow:

1. A mixin appends only slot 0's `ItemStack` to the Schematicannon's existing
   client update tag and restores it on the client.
2. A second mixin calls `notifyUpdate()` when server slot 0 changes.
3. The client scans already-loaded chunks periodically for Schematicannons with a
   deployed schematic.
4. A `SchematicHandler` mixin supplies the selected cannon stack only when Create
   finds no schematic in hand, preserving the original held-item workflow.
5. Passive previews suppress editing input and overlays; Architect Mode redirects
   Create's normal delayed transformation sync to the selected cannon.

The server remains authoritative and no Create source is modified. The only
invasive portion is the small sync bridge forced by Create's deliberate inventory
omission. The renderer and structure pipeline use public Create classes.

## Risks and open questions

- Create schematic files are player-owned files. In multiplayer, another client
  may receive the schematic `ItemStack` but not possess the referenced local NBT
  file. A production design needs an authorized, size-limited server-to-client
  schematic data service or a server-assigned preview asset ID.
- The POC always enables associated previews; a user-facing `Preview: ON/OFF`
  control and persisted per-cannon flag remain to be added.
- Native handler reuse should preserve Create's visual compatibility, but
  Fabulous graphics, shaders, Sodium, and block entity models still need hands-on
  compatibility testing.
- Bounds/anchor correctness must be tested for all rotations and mirrors.
- Large schematics need profiling. Current renderer tessellation is cached and
  draw calls are culled, but initial buffer construction can still stall the
  render thread. A production path should budget/asynchronously prepare sections
  and split very large previews spatially.
- Scanning loaded chunks every 20 ticks is acceptable for a POC, not the desired
  final index. Client chunk load/unload tracking or an explicit preview registry
  should replace it.
- Completed cannons may move their schematic to the output slot; clarify whether
  the ghost should disappear on completion or persist as a project record.

## Recommended next step

Run the manual matrix below before expanding the feature:

1. Create a small asymmetric schematic, deploy it, insert it into a cannon, switch
   held items, close/reopen the world, and confirm the ghost remains aligned.
2. Repeat every rotation and both mirror axes.
3. Remove/replace the schematic and break/unload the cannon; confirm cache cleanup.
4. Test two cannons, then a moderately large schematic while profiling frame time.
5. Test a dedicated server with two clients to expose the player-owned-file gap.
6. After those pass, add a persisted per-cannon preview toggle and replace periodic
   chunk scanning with an explicit registry/network payload.

Do not begin intelligent state coloring until this matrix is stable.
