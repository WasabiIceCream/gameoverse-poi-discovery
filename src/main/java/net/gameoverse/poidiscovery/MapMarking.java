package net.gameoverse.poidiscovery;

import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.BundleContents;
import net.minecraft.world.level.saveddata.maps.MapItemSavedData;
import net.minecraft.world.item.MapItem;

/**
 * Marks a discovered POI's banner on any of a player's already-existing map data that happens to
 * cover it - checked against MapStitch's real Atlas item by registry ID only (soft reference, no
 * compile-time dependency on the mod itself; if it's ever uninstalled this silently does nothing).
 *
 * <p>Deliberately best-effort for now: a player with no Atlas, or whose Atlas has no map covering
 * this position yet, still gets the discovery recorded (see {@link PlayerDiscoveries}) and the
 * chat/actionbar notice, just not an immediate map marker. MapStitch's own Atlas auto-generates
 * map coverage as a player explores, so the marker typically appears soon after anyway once that
 * catches up - a real known limitation, not silently pretended away, see the README.
 */
public final class MapMarking {
   private static final Identifier ATLAS_ID = Identifier.fromNamespaceAndPath("mapstitch", "atlas");

   private MapMarking() {
   }

   /** Returns true if the banner was actually added to at least one of the player's maps. */
   public static boolean markOnAnyCoveringMap(ServerPlayer player, BlockPos bannerPos) {
      Optional<Item> atlasItem = BuiltInRegistries.ITEM.getOptional(ATLAS_ID);
      if (atlasItem.isEmpty()) {
         return false;
      }

      boolean markedAny = false;
      for (int slot = 0; slot < player.getInventory().getContainerSize(); slot++) {
         ItemStack stack = player.getInventory().getItem(slot);
         if (!stack.is(atlasItem.get())) {
            continue;
         }

         BundleContents contents = stack.getOrDefault(DataComponents.BUNDLE_CONTENTS, BundleContents.EMPTY);
         for (ItemStack mapStack : contents.itemCopyStream().toList()) {
            if (!mapStack.has(DataComponents.MAP_ID)) {
               continue;
            }

            MapItemSavedData mapData = MapItem.getSavedData(mapStack, player.level());
            if (mapData == null || !mapData.dimension.equals(player.level().dimension())) {
               continue;
            }

            if (mapData.toggleBanner(player.level(), bannerPos)) {
               markedAny = true;
            }
         }
      }

      return markedAny;
   }
}
