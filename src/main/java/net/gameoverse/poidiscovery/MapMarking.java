package net.gameoverse.poidiscovery;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import net.fabricmc.loader.api.FabricLoader;
import net.gameoverse.poidiscovery.mixin.MapItemSavedDataAccessor;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.item.component.BundleContents;
import net.minecraft.world.item.component.MapDecorations;
import net.minecraft.world.level.saveddata.maps.MapId;
import net.minecraft.world.level.saveddata.maps.MapItemSavedData;
import net.minecraft.world.level.saveddata.maps.MapDecorationType;

/**
 * Marks a discovered POI on the single map tile (across every Atlas the player carries or wears)
 * whose own real center is closest to the POI's position - checked against the real Atlas item
 * only by registry ID (soft reference, no compile-time dependency on the mod itself; if it's ever
 * uninstalled this silently does nothing).
 * <p>
 * Deliberately only the nearest tile, not every tile the player owns: marking every map produced a
 * real, confirmed bug once {@code unlimitedTracking} (see below) was added - on the merged
 * world-map mosaic, where many tiles render on screen simultaneously (unlike a single held map),
 * the same decoration showed up edge-clamped and duplicated on every tile that didn't actually
 * cover the POI's real position, all at once. The nearest tile is the one most likely to actually
 * cover the position (rendering it at its true location, no clamping at all) and, when nothing
 * covers it yet, the single most useful tile to show a directional edge arrow from.
 * <p>
 * Uses {@code MapItemSavedData#addTargetDecoration(ItemStack, BlockPos, String, Holder)} - the
 * same real, public vanilla API used for Explorer Map treasure markers - rather than the
 * banner/toggleBanner mechanism the first version of this mod used. Deliberately switched: a
 * target decoration lives entirely on the map ITEM's own data (no real block backing it, unlike a
 * banner), which is required now that POIs have no physical presence in the world at all. It also
 * doesn't need the map to already cover the position - the decoration shows once the player's map
 * view includes that area, whenever that happens.
 * <p>
 * {@code BundleContents} only exposes item copies via its normal read API, so marking a contained
 * map requires rebuilding the whole bundle: unpack each entry to a real {@link ItemStack}, mutate
 * it, repack via {@link ItemStackTemplate#fromNonEmptyStack}, and write the new
 * {@link BundleContents} back onto the Atlas stack in the player's inventory.
 * <p>
 * Also checks Trinkets accessory slots for the Atlas, not just vanilla inventory - guarded by
 * {@code FabricLoader.isModLoaded("trinkets")} and isolated to {@link TrinketAtlasLookup} so
 * Trinkets' own classes are never touched if it isn't installed. This server's MapStitch config
 * lists "accessories" as a valid Atlas location, so it's routinely worn there rather than carried.
 * <p>
 * A target decoration alone only lives on the map ITEM - vanilla only ever converts it into an
 * actually-visible decoration (on the map's own {@code MapItemSavedData}, which is what every
 * renderer, including MapStitch's minimap, actually reads) via {@code tickCarriedBy}, and that
 * only runs automatically while a player directly holds a map. A map bundled inside a MapStitch
 * Atlas is never "carried" in that sense, so without help the marker would sit in the item's data
 * forever and never actually render. {@link MapItemSavedDataAccessor} exposes vanilla's own
 * private {@code addDecoration} so that conversion can be done here explicitly instead.
 * <p>
 * {@link PoiDiscovery} calls this every check interval for every already-discovered POI, not just
 * once at discovery - so this has to actually skip maps that already carry the right decoration,
 * not just re-mark unconditionally. Rewriting the whole Atlas's {@code BundleContents} every
 * second (even with byte-identical content) was found to break MapStitch's own client-side
 * tracking of which inner map is currently open - the constant identity churn made the active map
 * disappear from view after navigating away and back. Checking the item's existing
 * {@code MAP_DECORATIONS} entry first avoids touching the bundle at all once nothing has changed.
 */
public final class MapMarking {
   private static final Identifier ATLAS_ID = Identifier.fromNamespaceAndPath("mapstitch", "atlas");

   private MapMarking() {
   }

   private static final boolean TRINKETS_LOADED = FabricLoader.getInstance().isModLoaded("trinkets");

   /** Returns true if the POI was actually marked on at least one of the player's maps. */
   public static boolean markOnAllMaps(ServerPlayer player, PoiEntry poi) {
      Optional<Item> atlasItem = BuiltInRegistries.ITEM.getOptional(ATLAS_ID);
      Optional<Holder.Reference<MapDecorationType>> iconType = BuiltInRegistries.MAP_DECORATION_TYPE.get(Identifier.parse(poi.icon()));
      if (atlasItem.isEmpty() || iconType.isEmpty()) {
         return false;
      }

      ServerLevel level = player.level();
      List<ItemStack> atlasStacks = collectAtlasStacks(player, atlasItem.get());

      record AtlasMaps(ItemStack atlas, List<ItemStack> maps) {
      }
      List<AtlasMaps> candidates = new ArrayList<>();
      for (ItemStack atlasStack : atlasStacks) {
         BundleContents contents = atlasStack.getOrDefault(DataComponents.BUNDLE_CONTENTS, BundleContents.EMPTY);
         if (!contents.isEmpty()) {
            candidates.add(new AtlasMaps(atlasStack, contents.itemCopyStream().toList()));
         }
      }

      AtlasMaps nearest = null;
      int nearestIndex = -1;
      double bestDistSq = Double.MAX_VALUE;
      boolean anyMaps = false;

      for (AtlasMaps candidate : candidates) {
         List<ItemStack> maps = candidate.maps();
         for (int i = 0; i < maps.size(); i++) {
            ItemStack mapStack = maps.get(i);
            if (!mapStack.has(DataComponents.MAP_ID)) {
               continue;
            }
            anyMaps = true;

            MapId mapId = mapStack.get(DataComponents.MAP_ID);
            MapItemSavedData data = level.getMapData(mapId);
            if (data == null) {
               continue;
            }

            double dx = data.centerX - poi.pos().getX();
            double dz = data.centerZ - poi.pos().getZ();
            double distSq = dx * dx + dz * dz;
            if (distSq < bestDistSq) {
               bestDistSq = distSq;
               nearest = candidate;
               nearestIndex = i;
            }
         }
      }

      if (nearest == null) {
         return anyMaps;
      }

      // Every OTHER map still needs an explicit removal pass, not just "stop adding" - a map's
      // target-decoration entry (on the item) and its actually-rendered decoration (on the map's
      // own MapItemSavedData) are both real, additive state that never clears itself just because
      // this method stops calling addDecoration for it. Left alone, a POI that used to be nearest
      // to a different tile (this Atlas grew, or the structure scanner corrected a bounding box)
      // would leave a permanent stale duplicate on that old tile forever.
      for (AtlasMaps candidate : candidates) {
         List<ItemStackTemplate> rebuilt = new ArrayList<>();
         boolean changed = false;
         boolean isWinner = candidate == nearest;
         List<ItemStack> maps = candidate.maps();
         for (int i = 0; i < maps.size(); i++) {
            ItemStack mapStack = maps.get(i);
            if (isWinner && i == nearestIndex) {
               // The item's own target-decoration entry survives a server restart (it's a real
               // persisted data component), but the live MapItemSavedData's renderable decoration
               // is an in-memory-only field that doesn't - so convertToVisibleDecoration has to run
               // every time regardless of whether the item side already matches, or a POI already
               // marked before a restart would silently stop rendering until something about it
               // changed. addDecoration is itself idempotent, so this is safe to call
               // unconditionally; only the (comparatively expensive, and disruptive to MapStitch's
               // active-view tracking - see this class's own doc comment) bundle rewrite below
               // stays gated on alreadyMarked.
               convertToVisibleDecoration(level, mapStack, poi, iconType.get());
               if (!alreadyMarked(mapStack, poi, iconType.get())) {
                  MapItemSavedData.addTargetDecoration(mapStack, poi.pos(), poi.key(), iconType.get());
                  changed = true;
               }
            } else if (mapStack.has(DataComponents.MAP_ID) && removeMarker(level, mapStack, poi)) {
               changed = true;
            }
            rebuilt.add(ItemStackTemplate.fromNonEmptyStack(mapStack));
         }

         if (changed) {
            candidate.atlas().set(DataComponents.BUNDLE_CONTENTS, new BundleContents(rebuilt));
         }
      }

      return true;
   }

   /**
    * Strips this POI's marker from every map the player carries or wears, wherever it currently
    * sits - used when a registry entry itself is being deleted (a stale/phantom POI, purged after
    * e.g. a dimension reset), not just superseded by a closer tile. Without this, deleting the
    * {@link PoiEntry} from {@link PoiRegistry} alone would leave an orphaned marker on whichever
    * map already had it forever: {@link PoiDiscovery}'s own re-mark loop only ever iterates
    * currently-registered POIs, so a removed one is never visited again to clean up after itself.
    */
   public static void removeFromAllMaps(ServerPlayer player, PoiEntry poi) {
      Optional<Item> atlasItem = BuiltInRegistries.ITEM.getOptional(ATLAS_ID);
      if (atlasItem.isEmpty()) {
         return;
      }

      ServerLevel level = player.level();
      for (ItemStack atlasStack : collectAtlasStacks(player, atlasItem.get())) {
         BundleContents contents = atlasStack.getOrDefault(DataComponents.BUNDLE_CONTENTS, BundleContents.EMPTY);
         if (contents.isEmpty()) {
            continue;
         }

         List<ItemStackTemplate> rebuilt = new ArrayList<>();
         boolean changed = false;
         for (ItemStack mapStack : contents.itemCopyStream().toList()) {
            if (mapStack.has(DataComponents.MAP_ID) && removeMarker(level, mapStack, poi)) {
               changed = true;
            }
            rebuilt.add(ItemStackTemplate.fromNonEmptyStack(mapStack));
         }

         if (changed) {
            atlasStack.set(DataComponents.BUNDLE_CONTENTS, new BundleContents(rebuilt));
         }
      }
   }

   private static List<ItemStack> collectAtlasStacks(ServerPlayer player, Item atlasItem) {
      List<ItemStack> atlasStacks = new ArrayList<>();
      for (int slot = 0; slot < player.getInventory().getContainerSize(); slot++) {
         ItemStack stack = player.getInventory().getItem(slot);
         if (stack.is(atlasItem)) {
            atlasStacks.add(stack);
         }
      }
      // The Atlas is just as commonly worn as a Trinkets accessory as it is carried in
      // vanilla inventory on this server - the loop above alone can never find one there.
      if (TRINKETS_LOADED) {
         atlasStacks.addAll(TrinketAtlasLookup.getEquippedAtlases(player, atlasItem));
      }
      return atlasStacks;
   }

   /** Removes this POI's marker (item-level target decoration and live rendered decoration alike)
    *  from one map, if present. Returns true if there was anything to remove. */
   private static boolean removeMarker(ServerLevel level, ItemStack mapStack, PoiEntry poi) {
      MapDecorations decorations = mapStack.getOrDefault(DataComponents.MAP_DECORATIONS, MapDecorations.EMPTY);
      if (!decorations.decorations().containsKey(poi.key())) {
         return false;
      }

      var without = new java.util.LinkedHashMap<>(decorations.decorations());
      without.remove(poi.key());
      mapStack.set(DataComponents.MAP_DECORATIONS, new MapDecorations(without));

      MapId mapId = mapStack.get(DataComponents.MAP_ID);
      if (mapId != null) {
         MapItemSavedData mapData = level.getMapData(mapId);
         if (mapData != null) {
            ((MapItemSavedDataAccessor) mapData).gameoverse$removeDecoration(poi.key());
         }
      }
      return true;
   }

   /** True if this map's item data already has exactly this POI's target decoration entry. */
   private static boolean alreadyMarked(ItemStack mapStack, PoiEntry poi, Holder<MapDecorationType> iconType) {
      MapDecorations decorations = mapStack.getOrDefault(DataComponents.MAP_DECORATIONS, MapDecorations.EMPTY);
      MapDecorations.Entry existing = decorations.decorations().get(poi.key());
      if (existing == null) {
         return false;
      }

      return existing.type().equals(iconType) && existing.x() == poi.pos().getX() && existing.z() == poi.pos().getZ();
   }

   /**
    * Does what {@code tickCarriedBy} would have done for this one target decoration, had the map
    * actually been directly held - writes it into the map's own renderable decoration set. Safe to
    * call repeatedly: {@code addDecoration} is itself idempotent (overwrites the same keyed entry).
    */
   private static void convertToVisibleDecoration(ServerLevel level, ItemStack mapStack, PoiEntry poi, Holder<MapDecorationType> iconType) {
      MapId mapId = mapStack.get(DataComponents.MAP_ID);
      if (mapId == null) {
         return;
      }

      MapItemSavedData mapData = level.getMapData(mapId);
      if (mapData == null) {
         return;
      }

      MapItemSavedDataAccessor accessor = (MapItemSavedDataAccessor) mapData;
      if (!accessor.gameoverse$isUnlimitedTracking()) {
         accessor.gameoverse$setUnlimitedTracking(true);
      }

      accessor.gameoverse$addDecoration(
         iconType, level, poi.key(), poi.pos().getX(), poi.pos().getZ(), 180.0, Component.literal(poi.name())
      );
   }
}
