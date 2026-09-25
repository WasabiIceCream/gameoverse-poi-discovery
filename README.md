# gameoverse-poi-discovery

Major landmark structures as discoverable points of interest — completely
invisible and non-interactable, with no physical presence in the world at
all. Any player who gets close enough discovers it permanently —
independently, not just whoever's first — without needing to be holding a
map or atlas at the time. Once discovered, it's marked directly onto every
map inside any MapStitch Atlas the player owns, and stays marked as new
maps are added later too. Wholly original work, MIT licensed.

## Design context

Part of a broader "help players find things in a large, deliberately
random world without increasing content density" effort — see
`docs/current-state.md` for the full design conversation.

## v2: from physical banners to fully invisible POIs

The first version of this mod used real, physical banner blocks as POI
markers (an admin ran `/poi register` while standing on one), and marked
maps via vanilla's own `MapItemSavedData#toggleBanner()` — which reads a
banner block's name/color live from the world. That design was rejected
after real use: it required a physical block, had a short discovery
radius, and a banner is inherently visible/interactable, none of which
fit what was actually wanted (see the three requirements below). Rebuilt
from the ground up:

1. **Auto-placed, not admin-placed.** `StructureScanner` periodically
   scans already-loaded chunks around each online player
   (`ChunkAccess#getAllStarts()`) for a curated allow-list of major
   landmark structures (`StructurePois`) and auto-registers a POI the
   moment one is found — whether it's generating fresh as the world
   expands, or already existed in an already-explored world (no special
   case needed for "the mod was installed after this structure already
   generated"). `/poi register` still exists for the rare one-off POI
   that isn't a recognized structure.
2. **More range.** Discovery radius raised from 24 to 200 blocks.
3. **Truly non-physical.** POIs have no block backing them at all — see
   below.

## How it works

- **`PoiEntry`**: `pos` + `dimension` + `name` + `icon` (a
  `MapDecorationTypes` registry id). No physical block reference of any
  kind.
- **Discovery**: every 20 ticks, every online player is checked against
  every registered POI in their current dimension. Within 200 blocks and
  not already discovered → marked discovered (a per-player persistent
  set, survives death), a chat message, a level-up sound, and an attempt
  to mark it on the player's Atlas. Already-discovered POIs are
  re-checked every interval too, not just once at the moment of
  discovery — a map added to the Atlas afterward still picks up
  everything already found.
- **Marking a map**: uses vanilla's real, public
  `MapItemSavedData#addTargetDecoration(ItemStack, BlockPos, String,
  Holder<MapDecorationType>)` — the same API vanilla itself uses for
  Explorer Map treasure markers — which writes directly into the map
  ITEM's own data component. No real block needed anywhere, and it
  doesn't require the map to already cover the position first.
- **MapStitch integration is soft** — checked only by registry ID
  (`mapstitch:atlas`), no compile-time dependency. If MapStitch is ever
  removed, discovery still works (message + persistent flag), it just
  never finds an atlas to mark.
- **Rumor item**: a reflavored, single-use paper item that, on
  right-click, gives the player a vague hint (8-point compass direction
  + a 3-tier distance bucket) toward their own nearest *undiscovered*
  POI. Kept, not consumed, if there's nothing left to point at. Drops
  universally at a flat, small chance from kills, mature crop harvests,
  other block breaks, chest openings, and fishing — same independent,
  non-wrapping per-source hook pattern as `gameoverse-difficulty-hearts`'
  own Heart Crystal drops.
- `/poi forget` — debug/testing only, resets the calling player's
  discovered set.

## Three real bugs found and fixed getting v2 actually working in-game

Getting a decoration to actually *render* (not just sit correctly in the
map item's data, which is easy to mistake for "working" — see below) took
three separate, real bugs, each found only by testing in-game rather than
by inspecting stored data:

1. **A target decoration alone never renders.** `addTargetDecoration`
   only writes into the map ITEM's own `MAP_DECORATIONS` component.
   Vanilla only ever converts that into an actually-visible decoration
   (on the map's own `MapItemSavedData`, which is what every renderer,
   including MapStitch's minimap, actually reads) via
   `MapItemSavedData#tickCarriedBy()` — and that only runs automatically
   while a player *directly holds* a map (hand/offhand/item frame). A map
   bundled inside a MapStitch Atlas is never "carried" in that sense, so
   without help the marker just sits in the item's data forever and never
   renders. Confirmed by decompiling `tickCarriedBy`'s bytecode: its tail
   end does exactly the conversion needed (calls the private
   `addDecoration` for any target-decoration key not yet in the map's own
   live decoration set), independent of all the carried-by/player-marker
   bookkeeping earlier in the method. Fixed with a
   `MapItemSavedDataAccessor` mixin (`@Invoker` on the private
   `addDecoration`) so `MapMarking` can do that one conversion step
   directly, without the unwanted player-position-arrow side effects of
   calling the whole of `tickCarriedBy`.
   
   **The lesson that mattered most this round**: verifying the marker's
   data was present via `/data get entity ... Inventory` was *not*
   verifying it worked — it took a live screenshot from the player
   actually looking at their map to catch that nothing was rendering.
   Don't report a map-marking fix as working from data alone again.

2. **Marking a map every tick broke MapStitch's own map-selection
   tracking.** The re-mark-every-interval design (see above) meant
   `MapMarking` was rebuilding and overwriting the Atlas's entire
   `BundleContents` data component every second, for every
   already-discovered POI, even when nothing had actually changed. That
   constant identity churn was found (through the player leaving the map
   area and coming back to find it wouldn't load) to reset MapStitch's
   own client-side tracking of which inner map was currently open. Fixed
   by checking the map's existing `MAP_DECORATIONS` entry first and
   skipping the whole rebuild-and-write when the POI is already correctly
   marked — the bundle is now only touched when something has actually
   changed.

3. **Several curated icons made MapStitch permanently reject the map.**
   `StructurePois`' first icon choices (`woodland_mansion`,
   `ocean_monument`, `trial_chambers`, `swamp_hut`, `jungle_temple`,
   every village variant) are, in real vanilla data (confirmed by
   decompiling `MapDecorationTypes`' registration bytecode and reading
   `MapDecorationType#explorationMapElement()`), the *exact same*
   decoration types vanilla itself only ever puts on real Explorer/
   Treasure Maps — `explorationMapElement = true`. MapStitch's own
   `ModUtil#isExplorationMap()` uses exactly that flag as a heuristic:
   any map carrying one of those decorations gets classified as a
   treasure map and permanently excluded from
   `AtlasItem#updateActiveMap()`'s scan for "which map covers where I am
   now" — meaning once marked, that map could never be selected as the
   active map again, surfacing to the player as "No map in atlas for this
   area" the moment they left its radius and came back. Fixed by
   switching every `StructurePois` icon to decoration types confirmed
   `explorationMapElement = false` (banners, the two marker colors,
   `target_x`/`target_point`) instead. A stale already-marked map
   self-heals the next time the player is near the structure again (the
   scanner re-detects it and overwrites the stored icon; the next re-mark
   pass then overwrites both the item's target decoration and the map's
   live decoration with the corrected type).

Each confirmed fixed by an actual live playtest end to end: discover a
structure → marker renders on the Atlas → leave the area and return →
marker still there and the map still loads normally.

## Two real bugs carried over from v1 (still relevant)

1. **Chat messages showed the raw translation key** instead of
   "Discovered: X" — `fabric.mod.json` declaring `"environment": "server"`
   kept the client-side lang file from ever reaching the connecting
   client's resource manager. Fixed by setting `"environment": "*"`.
2. **The whole POI/discovery registry silently vanished on every server
   restart** — Fabric's Data Attachment API registers an `AttachmentType`
   via a plain Java static field, which only class-loads the first time
   something touches that class; the original code only touched it
   lazily (a command handler or the tick loop), which can run *after*
   the overworld already tried to deserialize its saved attachment data
   at boot. Fixed by forcing `PoiRegistry`/`PlayerDiscoveries` to
   class-load unconditionally in `onInitialize()`.

## Two more real bugs found 2026-09-24, chasing "markers still aren't showing up"

3. **A `MapDecorationType` ID-format bug blocked 72% of markers outright**
   — `StructurePois.java`'s vanilla banner icon IDs were written as
   `"<color>_banner"`, but the real registry key format (confirmed by
   decompiling `MapDecorationTypes.java`) is `"banner_<color>"`. Fixed by
   bulk-correcting all 120 affected entries across 16 colors.
2. **`MapMarking` only ever scanned vanilla inventory, never Trinkets
   accessory slots** — this server's MapStitch config lists
   `"accessories"` as a valid Atlas location (Trinkets Updated is
   installed), and the Atlas is routinely worn there rather than
   carried. `markOnAllMaps`'s inventory-slot loop could never find an
   Atlas worn as a trinket. Fixed by adding a Trinkets-aware lookup
   (`TrinketAtlasLookup`, isolated behind
   `FabricLoader.isModLoaded("trinkets")` so nothing breaks if Trinkets
   is ever removed) alongside the existing vanilla-inventory scan.
3. **The marker only ever appeared once a map tile already happened to
   cover the POI's exact position** — confirmed live: a player standing
   *on* a Nether Fortress still saw no marker, because none of their
   map tiles' own ~127-block captured squares included that spot yet.
   Root cause: vanilla's `MapItemSavedData.addDecoration` silently drops
   any non-player decoration outside the map's own bounds *unless*
   `unlimitedTracking` is set - the flag a real Explorer/Treasure Map
   gets at creation specifically so its arrow stays visible from any
   distance. A normal player-made map defaults it to `false`, so our
   target decorations (written via the same vanilla API an Explorer Map
   uses) were being clipped exactly like a physical banner would be,
   the opposite of the "vague direction+distance hint" this feature is
   for. Fixed by exposing the (normally `final`) `unlimitedTracking`
   field via a `@Mutable @Accessor` mixin and flipping it to `true` the
   first time any of a player's maps gets a POI decoration written to
   it - matches vanilla's own mechanism for the exact same UX Explorer
   Maps already have, rather than inventing a new one.

Confirmed live end to end after all three fixes: an already-discovered
POI marker appeared clamped to the edge of a map tile that didn't cover
its real position, pointing toward it - not just "eventually shows up
once you've mapped that exact spot."

## Status

Working, fully validated in-game end to end: automatic structure
detection, 200-block proximity discovery, a rendered marker (now
genuinely always-visible, not just on a map tile that happens to cover
the exact spot) on the player's Atlas whether carried or worn as a
Trinkets accessory, surviving leaving/returning and a server restart,
plus the Rumor item's hint system. Local-only so far, not yet pushed to
production.
