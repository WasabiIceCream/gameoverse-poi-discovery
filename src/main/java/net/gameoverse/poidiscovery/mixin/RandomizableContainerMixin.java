package net.gameoverse.poidiscovery.mixin;

import net.gameoverse.poidiscovery.RumorDrops;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.RandomizableContainer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Universal Rumor chest-loot roll. Same target/pattern as gameoverse-difficulty-hearts' own
 * RandomizableContainerMixin - plain @Inject(TAIL) on the interface default method, no
 * consumer-wrapping, nothing to compose with any other mod (including that one, even though it
 * injects the same target method - independent TAIL injections compose safely under Fabric
 * Mixin).
 */
@Mixin(RandomizableContainer.class)
public interface RandomizableContainerMixin {
   @Inject(method = "unpackLootTable", at = @At("TAIL"))
   private void gameoverse$rollRumorDrop(Player player, CallbackInfo ci) {
      RandomizableContainer self = (RandomizableContainer) this;
      Level level = self.getLevel();
      if (player instanceof ServerPlayer && level instanceof ServerLevel serverLevel) {
         RumorDrops.rollChest(serverLevel, self.getBlockPos());
      }
   }
}
