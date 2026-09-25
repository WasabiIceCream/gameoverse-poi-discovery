package net.gameoverse.poidiscovery;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/**
 * Major landmark structures (see {@link StructurePois}) as discoverable points of interest,
 * auto-registered as players explore (see {@link StructureScanner}) - completely invisible and
 * non-interactable, since a POI has no physical presence in the world at all (name/icon live
 * entirely in {@link PoiEntry}, marked on a player's map via {@link MapMarking}, not a real
 * banner block). The moment any player gets close enough, it's discovered permanently for them -
 * independently for every player, not just whoever's first, and without needing to be holding a
 * map at the time. Already-discovered POIs are re-marked every check interval, not just once at
 * the moment of discovery, so a map added to the Atlas later still picks up everything already
 * found. See README.md for the full design rationale.
 */
public class PoiDiscovery implements ModInitializer {
   /** How close a player needs to get to trigger discovery. */
   private static final double DISCOVERY_RADIUS = 200.0;
   private static final double DISCOVERY_RADIUS_SQ = DISCOVERY_RADIUS * DISCOVERY_RADIUS;

   /** Proximity is checked this often, not every tick - discovery isn't latency-sensitive. */
   private static final int CHECK_INTERVAL_TICKS = 20;

   private int tickCounter;
   private final StructureScanner structureScanner = new StructureScanner();

   @Override
   public void onInitialize() {
      // Force PoiRegistry/PlayerDiscoveries to class-load now, registering their AttachmentTypes
      // immediately. Both are otherwise only touched lazily (first command/tick), which can run
      // after the overworld has already tried to load its saved attachment data - Fabric's
      // attachment system silently discards data for a not-yet-registered type ("Found unknown
      // attachment type"), so registration has to happen unconditionally during mod init, not on
      // first real use.
      PoiRegistry.touch();
      PlayerDiscoveries.touch();

      ModItems.register();

      CommandRegistrationCallback.EVENT.register((dispatcher, buildContext, selection) -> PoiCommands.register(dispatcher));
      ServerTickEvents.END_SERVER_TICK.register(this::onServerTick);

      ServerLivingEntityEvents.AFTER_DEATH.register((entity, damageSource) -> {
         if (damageSource.getEntity() instanceof ServerPlayer && entity.level() instanceof ServerLevel level) {
            RumorDrops.rollKill(level, entity.getX(), entity.getY(), entity.getZ());
         }
      });

      PlayerBlockBreakEvents.AFTER.register((world, player, pos, state, blockEntity) -> {
         if (world instanceof ServerLevel level) {
            boolean matureCrop = state.getBlock() instanceof CropBlock crop && crop.isMaxAge(state);
            if (matureCrop || !(state.getBlock() instanceof CropBlock)) {
               RumorDrops.rollBlockBreak(level, pos, matureCrop);
            }
         }
      });
   }

   private void onServerTick(MinecraftServer server) {
      structureScanner.tick(server);

      if (++tickCounter < CHECK_INTERVAL_TICKS) {
         return;
      }
      tickCounter = 0;

      var allPois = PoiRegistry.all(server.overworld());
      for (ServerPlayer player : server.getPlayerList().getPlayers()) {
         var discovered = PlayerDiscoveries.discovered(player);

         for (PoiEntry poi : allPois) {
            if (!player.level().dimension().equals(poi.dimension())) {
               continue;
            }

            if (discovered.contains(poi.key())) {
               // Already discovered - keep re-marking it on the player's current maps rather than
               // only ever marking once at the moment of discovery. A map added to the Atlas
               // *after* discovering something would otherwise never pick up that marker at all.
               MapMarking.markOnAllMaps(player, poi);
               continue;
            }

            Vec3 nearest = poi.nearestPointTo(player.position());
            if (player.position().distanceToSqr(nearest) <= DISCOVERY_RADIUS_SQ && canSee(player, nearest)) {
               discover(player, poi);
            }
         }
      }
   }

   /**
    * Whether {@code player} has an unobstructed line to {@code target} (the closest point on a
    * POI's real bounding box, from {@link PoiEntry#nearestPointTo} - not one arbitrary fixed
    * coordinate deep inside a large structure). Being within the discovery radius while buried
    * under solid terrain - a deep tunnel below a surface ruin, the far side of a mountain from a
    * Nether fortress - shouldn't count as having found it. A single raycast to the *nearest* point
    * on the structure's box is a deliberate compromise over sampling many points across its whole
    * surface: it's the single most likely point to actually be exposed, so it correctly allows
    * discovery the moment any real part of the structure comes into view (walking up to its wall,
    * standing on its roof) without the cost of a multi-ray visibility scan every check interval.
    */
   private static boolean canSee(ServerPlayer player, Vec3 target) {
      ClipContext ctx = new ClipContext(player.getEyePosition(), target, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player);
      return player.level().clip(ctx).getType() == HitResult.Type.MISS;
   }

   private void discover(ServerPlayer player, PoiEntry poi) {
      PlayerDiscoveries.markDiscovered(player, poi.key());
      boolean marked = MapMarking.markOnAllMaps(player, poi);

      player.sendSystemMessage(
         Component.translatable(
            marked ? "gameoverse_poi_discovery.discovered" : "gameoverse_poi_discovery.discovered_no_map", Component.literal(poi.name())
         )
      );
      player.level().playSound(null, player.blockPosition(), SoundEvents.PLAYER_LEVELUP, SoundSource.PLAYERS, 0.6F, 1.4F);
   }
}
