package net.gameoverse.poidiscovery.mixin;

import net.gameoverse.poidiscovery.RumorDrops;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.FishingHook;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Universal Rumor fishing-catch roll. Same target/pattern as gameoverse-difficulty-hearts' own
 * FishingHookMixin - fires right after FishingHook#retrieve's real loot table roll completes, via
 * a shift=AFTER @Inject on that exact call, never touching LootTable#getRandomItemsRaw directly
 * (see RumorDrops' javadoc for why).
 */
@Mixin(FishingHook.class)
public abstract class FishingHookMixin {
   @Inject(
      method = "retrieve",
      at = @At(
         value = "INVOKE",
         target = "Lnet/minecraft/world/level/storage/loot/LootTable;getRandomItems(Lnet/minecraft/world/level/storage/loot/LootParams;)Lit/unimi/dsi/fastutil/objects/ObjectArrayList;",
         shift = At.Shift.AFTER
      )
   )
   private void gameoverse$rollRumorFishingDrop(ItemStack rod, CallbackInfoReturnable<Integer> cir) {
      FishingHook self = (FishingHook) (Object) this;
      Player owner = self.getPlayerOwner();
      if (owner instanceof ServerPlayer && self.level() instanceof ServerLevel level) {
         RumorDrops.rollFishing(level, self.getX(), self.getY(), self.getZ());
      }
   }
}
