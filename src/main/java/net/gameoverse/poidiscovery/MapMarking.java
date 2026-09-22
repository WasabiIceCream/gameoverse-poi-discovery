package net.gameoverse.poidiscovery;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import net.gameoverse.poidiscovery.mixin.MapItemSavedDataAccessor;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
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
 * Marks a discovered POI on every map inside a player's MapStitch Atlas - checked against the
 * real Atlas item only by registry ID (soft reference, no compile-time dependency on the mod
 * itself; if it's ever uninstalled this silently does nothing).
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

   /** Returns true if the POI was actually marked on at least one of the player's maps. */
   public static boolean markOnAllMaps(ServerPlayer player, PoiEntry poi) {
      Optional<Item> atlasItem = BuiltInRegistries.ITEM.getOptional(ATLAS_ID);
      Optional<Holder.Reference<MapDecorationType>> iconType = BuiltInRegistries.MAP_DECORATION_TYPE.get(Identifier.parse(poi.icon()));
      if (atlasItem.isEmpty() || iconType.isEmpty()) {
         return false;
      }

      ServerLevel level = player.level();
      boolean markedAny = false;
      for (int slot = 0; slot < player.getInventory().getContainerSize(); slot++) {
         ItemStack atlasStack = player.getInventory().getItem(slot);
         if (!atlasStack.is(atlasItem.get())) {
            continue;
         }

         BundleContents contents = atlasStack.getOrDefault(DataComponents.BUNDLE_CONTENTS, BundleContents.EMPTY);
         if (contents.isEmpty()) {
            continue;
         }

         List<ItemStackTemplate> rebuilt = new ArrayList<>();
         boolean changed = false;
         for (ItemStack mapStack : contents.itemCopyStream().toList()) {
            if (mapStack.has(DataComponents.MAP_ID)) {
               markedAny = true;
               if (!alreadyMarked(mapStack, poi, iconType.get())) {
                  MapItemSavedData.addTargetDecoration(mapStack, poi.pos(), poi.key(), iconType.get());
                  convertToVisibleDecoration(level, mapStack, poi, iconType.get());
                  changed = true;
               }
            }
            rebuilt.add(ItemStackTemplate.fromNonEmptyStack(mapStack));
         }

         if (changed) {
            atlasStack.set(DataComponents.BUNDLE_CONTENTS, new BundleContents(rebuilt));
         }
      }

      return markedAny;
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

      ((MapItemSavedDataAccessor) mapData).gameoverse$addDecoration(
         iconType, level, poi.key(), poi.pos().getX(), poi.pos().getZ(), 180.0, null
      );
   }
}
