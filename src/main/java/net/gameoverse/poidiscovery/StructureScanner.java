package net.gameoverse.poidiscovery;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureStart;

/**
 * Periodically scans loaded chunks around online players for major landmark structures (see
 * {@link StructurePois}) and auto-registers a POI for each new one found - both for structures
 * generating fresh as the world expands, and for ones that already existed in an already-explored
 * world (since this just checks whatever chunks happen to already be loaded, it needs no special
 * case for "world already had this structure before the mod was installed").
 * <p>
 * Deliberately only looks at already-loaded chunks ({@code getChunkNow}, never forces a chunk to
 * load) - this is a courtesy scan riding along with normal exploration, not a full world scan.
 */
public final class StructureScanner {
   /** How far out (in chunks) to scan around each player. */
   private static final int SCAN_RADIUS_CHUNKS = 12;

   /** Scanning is more expensive than the discovery tick, so it runs less often. */
   private static final int SCAN_INTERVAL_TICKS = 100;

   private int tickCounter;

   public void tick(net.minecraft.server.MinecraftServer server) {
      if (++tickCounter < SCAN_INTERVAL_TICKS) {
         return;
      }
      tickCounter = 0;

      for (ServerPlayer player : server.getPlayerList().getPlayers()) {
         ServerLevel level = player.level();
         Registry<Structure> structures = level.registryAccess().lookupOrThrow(Registries.STRUCTURE);
         ChunkPos center = new ChunkPos(player.blockPosition().getX() >> 4, player.blockPosition().getZ() >> 4);

         for (int dx = -SCAN_RADIUS_CHUNKS; dx <= SCAN_RADIUS_CHUNKS; dx++) {
            for (int dz = -SCAN_RADIUS_CHUNKS; dz <= SCAN_RADIUS_CHUNKS; dz++) {
               ChunkAccess chunk = level.getChunkSource().getChunkNow(center.x() + dx, center.z() + dz);
               if (chunk == null) {
                  continue;
               }

               for (var entry : chunk.getAllStarts().entrySet()) {
                  StructureStart start = entry.getValue();
                  if (start == null || !start.isValid()) {
                     continue;
                  }

                  Identifier structureId = structures.getKey(entry.getKey());
                  if (structureId == null) {
                     continue;
                  }

                  StructurePois.Entry poi = StructurePois.lookup(structureId.toString());
                  if (poi == null) {
                     continue;
                  }

                  BlockPos center3d = start.getBoundingBox().getCenter();
                  PoiRegistry.register(level, new PoiEntry(center3d, level.dimension(), poi.name(), poi.icon()));
               }
            }
         }
      }
   }
}
