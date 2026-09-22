package net.gameoverse.poidiscovery;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Containers;
import net.minecraft.world.item.ItemStack;

/**
 * Universal rare Rumor drop, across basically every loot source, at a flat/fixed chance per
 * source category - same design and same reasoning as gameoverse-difficulty-hearts' own
 * UniversalHeartDrops, deliberately re-implemented independently here rather than shared, so
 * there's nothing to compose (safely or otherwise) with that mod or any other. See its javadoc
 * for the real incident (v1.3.0) that's why this uses separate non-wrapping event/injection
 * points per source instead of a shared LootTable#getRandomItemsRaw hook.
 * <p>
 * No block-placement gating (unlike Heart Crystals): Rumors are low-stakes navigational hints
 * toward a finite set of already-placed POIs, not a permanently exploitable resource - worst
 * case a player farms extra Rumors that just tell them "nothing left to find."
 */
public final class RumorDrops {
   private static final float KILL_CHANCE = 0.01F;
   private static final float MATURE_CROP_CHANCE = 0.004F;
   private static final float BLOCK_BREAK_CHANCE = 0.001F;
   private static final float CHEST_CHANCE = 0.02F;
   private static final float FISHING_CHANCE = 0.02F;

   private RumorDrops() {
   }

   public static void rollKill(ServerLevel level, double x, double y, double z) {
      roll(level, KILL_CHANCE, x, y, z);
   }

   public static void rollChest(ServerLevel level, BlockPos pos) {
      roll(level, CHEST_CHANCE, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5);
   }

   public static void rollFishing(ServerLevel level, double x, double y, double z) {
      roll(level, FISHING_CHANCE, x, y, z);
   }

   public static void rollBlockBreak(ServerLevel level, BlockPos pos, boolean matureCrop) {
      roll(level, matureCrop ? MATURE_CROP_CHANCE : BLOCK_BREAK_CHANCE, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5);
   }

   private static void roll(ServerLevel level, float chance, double x, double y, double z) {
      if (level.getRandom().nextFloat() >= chance) {
         return;
      }
      Containers.dropItemStack(level, x, y, z, new ItemStack(ModItems.RUMOR));
   }
}
