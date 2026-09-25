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
 * RandomizableContainerMixin - plain @Inject on the interface default method, no
 * consumer-wrapping, nothing to compose with any other mod (including that one, even though it
 * injects the same target method - independent injections compose safely under Fabric Mixin).
 * <p>
 * Injects at {@code HEAD}, not {@code TAIL} - a real bug, found live 2026-09-25 (Rumors kept
 * dropping from a player's own long-emptied storage chests at home). Vanilla's own
 * {@code unpackLootTable} runs unconditionally on *every* container open, and only its own
 * internal {@code getLootTable() != null} check gates whether real loot actually generates (a
 * plain storage chest always has a null loot table, so it silently takes the no-op branch every
 * time). A {@code TAIL} injection fires after the whole method body regardless of which branch
 * ran, so the roll fired on every single chest open, not just genuine loot chests. Checking
 * {@code getLootTable()} at {@code HEAD} instead reads the real pre-open state, before vanilla's
 * own body clears it via {@code setLootTable(null)} partway through.
 */
@Mixin(RandomizableContainer.class)
public interface RandomizableContainerMixin {
   @Inject(method = "unpackLootTable", at = @At("HEAD"))
   private void gameoverse$rollRumorDrop(Player player, CallbackInfo ci) {
      RandomizableContainer self = (RandomizableContainer) this;
      if (self.getLootTable() == null) {
         return;
      }

      Level level = self.getLevel();
      if (player instanceof ServerPlayer && level instanceof ServerLevel serverLevel) {
         RumorDrops.rollChest(serverLevel, self.getBlockPos());
      }
   }
}
