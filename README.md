# gameoverse-poi-discovery

Named, colored banners as discoverable points of interest. Any player who
gets close enough discovers it permanently — independently, not just
whoever's first — without needing to be holding a map or atlas at the
time. Once discovered, it's marked on any MapStitch Atlas the player owns
that already covers the area (present or future). Wholly original work,
MIT licensed.

## Design context

Part of a broader "help players find things in a large, deliberately
random world without increasing content density" effort — see
`docs/current-state.md` for the full design conversation. The
complementary half (Rumor items, giving a vague hint *before* a player
has found something) reuses `gameoverse-difficulty-hearts`'s Heart
Crystal drop pattern and isn't built yet.

## How it works

- **Registration**: `/poi register` while standing at/on a banner
  (admin-only, LuckPerms `COMMANDS_GAMEMASTER`-equivalent). Reads the
  banner's real position; deliberately doesn't duplicate its name/color
  into the registry — those stay live on the actual banner block entity
  in the world (read via vanilla's own `MapBanner.fromWorld()` at
  discovery time), so renaming or re-dyeing a banner later needs no
  re-registration. `/poi remove` and `/poi list` for management.
- **Discovery**: every 20 ticks, every online player is checked against
  every registered POI in their current dimension. Within 24 blocks and
  not already discovered → marked discovered (a per-player persistent
  set, survives death), a chat message with the banner's real name, a
  level-up sound, and a best-effort attempt to mark it on any of the
  player's Atlas maps that already cover that position (via vanilla's
  own `MapItemSavedData#toggleBanner()` — the same method a real
  right-click would call, just invoked server-side on the player's
  behalf instead of requiring the click).
- **MapStitch integration is soft** — checked only by registry ID
  (`mapstitch:atlas`), no compile-time dependency. If MapStitch is ever
  removed, discovery still works (message + persistent flag), it just
  never finds an atlas to mark.

## Known limitation

A player with no Atlas yet, or whose Atlas has no map covering that
position yet, still gets the discovery recorded and the chat message —
just not an immediate marker. MapStitch's own Atlas auto-generates map
coverage as a player explores, so the marker typically appears soon
after anyway, but this isn't a guaranteed retroactive system yet (no
"check pending un-marked discoveries whenever new map coverage appears"
loop). Acceptable for now; revisit if it turns out to matter in practice.

## Two real bugs found and fixed while building this

1. **Chat messages showed the raw translation key** instead of "Discovered:
   X". Root cause: `fabric.mod.json` declared `"environment": "server"`.
   That's correct for the mod's actual logic (no client code at all), but
   it also meant the client-side lang file (`assets/.../lang/en_us.json`)
   never got indexed into the connecting client's resource manager, so
   `Component.translatable(...)` had nothing to resolve against and fell
   back to displaying the raw key. Fixed by setting `"environment": "*"`
   — harmless since there's still no client entrypoint, but it makes sure
   the mod's assets actually reach the client.
2. **The whole POI/discovery registry silently vanished on every server
   restart**, even after an explicit `/save-all flush` before stopping —
   ruling out a save-timing issue. The boot log had the real answer:
   `Found unknown attachment type gameoverse_poi_discovery:pois`. Fabric's
   Data Attachment API registers an `AttachmentType` via a plain Java
   static field, which only actually runs (class-loads) the first time
   something touches that class — which in the original code was only
   ever a command handler or the tick loop, both of which can easily run
   *after* the overworld has already tried to deserialize its saved
   attachment data at boot. Fixed by forcing both `PoiRegistry` and
   `PlayerDiscoveries` to class-load unconditionally and immediately in
   `onInitialize()` (a trivial `touch()` static method call on each),
   guaranteeing their `AttachmentType`s are registered before any world
   data loads. Confirmed fixed by registering a POI, force-saving,
   restarting, and checking it survived — twice, once to reproduce the
   bug and once to confirm the fix.

Both confirmed fixed in-game on this project's real local dedicated
server, including a full end-to-end pass after both fixes: register a
named banner → automatic proximity discovery → correctly-named chat
message → marker appears on the player's Atlas → survives a server
restart.

## Status

Working, validated in-game, local-only so far (not yet pushed to
production). Rumor items (the complementary "hint before discovery"
mechanism) not yet built.
