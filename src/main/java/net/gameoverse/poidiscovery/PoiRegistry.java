package net.gameoverse.poidiscovery;

import com.mojang.serialization.Codec;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentTarget;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;

/**
 * The world's registered points of interest, keyed by {@link PoiEntry#key()}. Stored as a single
 * persistent attachment on the overworld (an arbitrary but stable single attachment point - the
 * registry itself is global, not per-dimension; each entry carries its own dimension). Same
 * Fabric Data Attachment pattern already used for {@code PlacedBlocks} in gameoverse-difficulty-hearts.
 */
public final class PoiRegistry {
   public static final AttachmentType<Map<String, PoiEntry>> POIS = AttachmentRegistry.createPersistent(
      Identifier.fromNamespaceAndPath("gameoverse_poi_discovery", "pois"), poiMapCodec()
   );

   private PoiRegistry() {
   }

   /** No-op - just forces this class (and its POIS AttachmentType registration) to load early. */
   public static void touch() {
   }

   private static Codec<Map<String, PoiEntry>> poiMapCodec() {
      return PoiEntry.CODEC.listOf().xmap(PoiRegistry::toMap, PoiRegistry::toList);
   }

   private static Map<String, PoiEntry> toMap(List<PoiEntry> entries) {
      Map<String, PoiEntry> map = new LinkedHashMap<>();
      for (PoiEntry entry : entries) {
         map.put(entry.key(), entry);
      }
      return map;
   }

   private static List<PoiEntry> toList(Map<String, PoiEntry> map) {
      return new ArrayList<>(map.values());
   }

   private static Map<String, PoiEntry> registry(ServerLevel level) {
      ServerLevel overworld = level.getServer().overworld();
      return ((AttachmentTarget) overworld).getAttachedOrCreate(POIS, LinkedHashMap::new);
   }

   public static void register(ServerLevel level, PoiEntry entry) {
      registry(level).put(entry.key(), entry);
   }

   public static boolean remove(ServerLevel level, String key) {
      return registry(level).remove(key) != null;
   }

   /**
    * Bulk-removes every POI registered for one dimension. Needed for a real scenario, not just
    * theoretical: deleting and regenerating a dimension's own save folder (done once already, for
    * the Nether biome-dilution fix) leaves every POI the structure scanner had already found there
    * stale - this registry is a separate persistent attachment on the overworld, entirely unrelated
    * to that dimension's own region files, so wiping the dimension never touches it. A stale entry
    * doesn't just linger harmlessly: it still passes discovery's proximity/line-of-sight checks
    * against a real, physical structure that may no longer exist anywhere near that position in the
    * regenerated terrain.
    */
   public static int removeAllInDimension(ServerLevel level, ResourceKey<Level> dimension) {
      Map<String, PoiEntry> map = registry(level);
      List<String> toRemove = map.values().stream().filter(e -> e.dimension().equals(dimension)).map(PoiEntry::key).toList();
      toRemove.forEach(map::remove);
      return toRemove.size();
   }

   public static List<PoiEntry> all(ServerLevel level) {
      return List.copyOf(registry(level).values());
   }
}
