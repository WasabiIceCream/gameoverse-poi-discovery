package net.gameoverse.poidiscovery;

import eu.pb4.trinkets.api.TrinketSlotAccess;
import eu.pb4.trinkets.api.TrinketsApi;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.List;

/**
 * Isolated from {@link MapMarking} so its class-loading (and therefore its direct reference to
 * Trinkets' own API classes) only happens after {@code FabricLoader.isModLoaded("trinkets")} has
 * already confirmed Trinkets is present - touching this class with Trinkets absent would throw
 * NoClassDefFoundError otherwise. Same isolation pattern MapStitch itself uses for its own
 * Trinkets compat (see mapstitch-fabric's {@code AccessoryUtil}/{@code TrinketsCompat}).
 * <p>
 * {@link TrinketSlotAccess#get()} returns the real, live backing {@link ItemStack} for the slot
 * (not a defensive copy) - mutating it in place, the same way {@link MapMarking} already mutates
 * an Atlas found in vanilla inventory, is safe and actually persists.
 */
final class TrinketAtlasLookup {
   private TrinketAtlasLookup() {
   }

   static List<ItemStack> getEquippedAtlases(LivingEntity entity, Item atlasItem) {
      return TrinketsApi.getAttachment(entity)
         .equipped(stack -> stack.is(atlasItem), false)
         .stream()
         .map(TrinketSlotAccess::get)
         .toList();
   }
}
