package net.gameoverse.poidiscovery;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.saveddata.maps.MapBanner;

/**
 * Named, colored banners as discoverable points of interest. A banner's own name/color (real
 * vanilla {@code MapBanner} data, read live from the world) becomes a labeled map marker the
 * moment any player gets close enough - independently for every player, not just whoever's
 * first, and without needing to be holding a map at the time. See README.md for the full design
 * rationale and known limitations.
 */
public class PoiDiscovery implements ModInitializer {
   /** How close a player needs to get to trigger discovery. */
   private static final double DISCOVERY_RADIUS = 24.0;
   private static final double DISCOVERY_RADIUS_SQ = DISCOVERY_RADIUS * DISCOVERY_RADIUS;

   /** Proximity is checked this often, not every tick - discovery isn't latency-sensitive. */
   private static final int CHECK_INTERVAL_TICKS = 20;

   private int tickCounter;

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

      CommandRegistrationCallback.EVENT.register((dispatcher, buildContext, selection) -> PoiCommands.register(dispatcher));
      ServerTickEvents.END_SERVER_TICK.register(this::onServerTick);
   }

   private void onServerTick(MinecraftServer server) {
      if (++tickCounter < CHECK_INTERVAL_TICKS) {
         return;
      }
      tickCounter = 0;

      for (PoiEntry poi : PoiRegistry.all(server.overworld())) {
         for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            if (!player.level().dimension().equals(poi.dimension())) {
               continue;
            }
            if (PlayerDiscoveries.hasDiscovered(player, poi.key())) {
               continue;
            }
            if (player.position().distanceToSqr(poi.pos().getX() + 0.5, poi.pos().getY() + 0.5, poi.pos().getZ() + 0.5) > DISCOVERY_RADIUS_SQ) {
               continue;
            }

            discover(player, poi);
         }
      }
   }

   private void discover(ServerPlayer player, PoiEntry poi) {
      PlayerDiscoveries.markDiscovered(player, poi.key());
      boolean marked = MapMarking.markOnAnyCoveringMap(player, poi.pos());

      MapBanner banner = MapBanner.fromWorld(player.level(), poi.pos());
      Component name = banner != null && banner.name().isPresent() ? banner.name().get() : Component.translatable("gameoverse_poi_discovery.unnamed");

      player.sendSystemMessage(
         Component.translatable(marked ? "gameoverse_poi_discovery.discovered" : "gameoverse_poi_discovery.discovered_no_map", name)
      );
      player.level().playSound(null, player.blockPosition(), SoundEvents.PLAYER_LEVELUP, SoundSource.PLAYERS, 0.6F, 1.4F);
   }
}
