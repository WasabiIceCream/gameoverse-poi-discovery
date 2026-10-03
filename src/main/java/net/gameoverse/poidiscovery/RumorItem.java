package net.gameoverse.poidiscovery;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/**
 * A single-use hint toward the player's nearest undiscovered POI - deliberately vague (8-point
 * compass direction + a distance bucket, never exact coordinates), matching the "here's roughly
 * where to look" role this was designed for (see docs/current-state.md). Consumed on a successful
 * hint; kept if there's genuinely nothing left to point at, so a player never wastes one on a
 * dead-end.
 */
public class RumorItem extends Item {
   public RumorItem(Properties properties) {
      super(properties);
   }

   @Override
   public InteractionResult use(Level level, Player player, net.minecraft.world.InteractionHand hand) {
      if (level.isClientSide() || !(player instanceof ServerPlayer serverPlayer) || !(level instanceof ServerLevel serverLevel)) {
         return InteractionResult.SUCCESS;
      }

      List<PoiEntry> undiscovered = PoiRegistry.all(serverLevel)
         .stream()
         .filter(poi -> poi.dimension().equals(serverLevel.dimension()))
         .filter(poi -> !PlayerDiscoveries.hasDiscovered(serverPlayer, poi.key()))
         .toList();

      Optional<PoiEntry> nearest = undiscovered.stream()
         .min(Comparator.comparingDouble(poi -> serverPlayer.position().distanceToSqr(poi.pos().getX() + 0.5, poi.pos().getY() + 0.5, poi.pos().getZ() + 0.5)));

      if (nearest.isEmpty()) {
         serverPlayer.sendSystemMessage(Component.translatable("gameoverse_poi_discovery.rumor.nothing"));
         return InteractionResult.SUCCESS;
      }

      PoiEntry poi = nearest.get();
      double dx = poi.pos().getX() + 0.5 - serverPlayer.getX();
      double dz = poi.pos().getZ() + 0.5 - serverPlayer.getZ();
      double distance = Math.sqrt(dx * dx + dz * dz);

      serverPlayer.sendSystemMessage(
         Component.translatable("gameoverse_poi_discovery.rumor.hint", Component.translatable(direction(dx, dz)),
            Component.translatable(relative(dx, dz, serverPlayer.getYRot())), Component.translatable(distanceBucket(distance)),
            Component.translatable(height(poi.pos().getY() + 0.5 - serverPlayer.getY())))
      );
      serverLevel.playSound(null, serverPlayer.blockPosition(), SoundEvents.BOOK_PAGE_TURN, SoundSource.PLAYERS, 0.6F, 1.2F);

      ItemStack held = player.getItemInHand(hand);
      held.shrink(1);
      return InteractionResult.SUCCESS;
   }

   /** 8-point compass direction, from the player toward the POI. Minecraft's -Z is north, +X is east. */
   private static String direction(double dx, double dz) {
      double angle = Math.toDegrees(Math.atan2(dx, -dz));
      if (angle < 0) {
         angle += 360;
      }
      String[] directions = {
         "gameoverse_poi_discovery.direction.n",
         "gameoverse_poi_discovery.direction.ne",
         "gameoverse_poi_discovery.direction.e",
         "gameoverse_poi_discovery.direction.se",
         "gameoverse_poi_discovery.direction.s",
         "gameoverse_poi_discovery.direction.sw",
         "gameoverse_poi_discovery.direction.w",
         "gameoverse_poi_discovery.direction.nw"
      };
      int index = (int) Math.round(angle / 45.0) % 8;
      return directions[index];
   }

   /**
    * The same bearing relative to where the player is looking: compasses only work in the Overworld, so in the Nether,
    * the End and post-End dimensions "to the northeast" alone gave no way to tell which way to go.
    * Yaw 0 faces south (+Z); the player's right is (-fz, fx).
    */
   private static String relative(double dx, double dz, float yaw) {
      double fx = -Math.sin(Math.toRadians(yaw));
      double fz = Math.cos(Math.toRadians(yaw));
      double angle = Math.toDegrees(Math.atan2(-dx * fz + dz * fx, dx * fx + dz * fz));
      if (angle < 0) {
         angle += 360;
      }
      String[] relative = {"ahead", "ahead_right", "right", "behind_right", "behind", "behind_left", "left", "ahead_left"};
      return "gameoverse_poi_discovery.relative." + relative[(int) Math.round(angle / 45.0) % 8];
   }

   /** Height of the landmark's centre against the player's: post-End dimensions put dungeons underground and towers
    *  near the build limit, so a bearing alone left players searching the wrong layer. */
   private static String height(double dy) {
      if (Math.abs(dy) < 16) {
         return "gameoverse_poi_discovery.height.level";
      } else if (dy > 0) {
         return dy > 64 ? "gameoverse_poi_discovery.height.far_above" : "gameoverse_poi_discovery.height.above";
      } else {
         return dy < -64 ? "gameoverse_poi_discovery.height.far_below" : "gameoverse_poi_discovery.height.below";
      }
   }

   private static String distanceBucket(double distance) {
      if (distance < 150) {
         return "gameoverse_poi_discovery.distance.close";
      } else if (distance < 600) {
         return "gameoverse_poi_discovery.distance.moderate";
      } else {
         return "gameoverse_poi_discovery.distance.far";
      }
   }
}
