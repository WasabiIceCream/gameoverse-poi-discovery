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
import net.minecraft.server.level.ServerLevel;

/**
 * The world's registered points of interest, keyed by {@link PoiEntry#key()}. Stored as a
 * persistent attachment on each individual dimension's own {@link ServerLevel} - the same Fabric
 * Data Attachment pattern already used for {@code PlacedBlocks} in
 * gameoverse-difficulty-hearts, but genuinely per-dimension here rather than a single global map
 * parked on the overworld (that was this class's original design; changed 2026-09-25 after a
 * real incident: deleting and regenerating the Nether's own save folder for the biome-dilution
 * fix left every Nether POI the scanner had found stale, since a global registry attached to the
 * overworld is entirely untouched by wiping any other dimension's region files). A Fabric
 * attachment on a {@code ServerLevel} persists inside that level's own data folder (confirmed on
 * disk: {@code dimensions/minecraft/overworld/data/fabric/attachments.dat} - other mods already
 * installed here, like cardinal-components and biolith, keep their own per-dimension data the
 * same way under each dimension's own {@code data/} folder), so attaching per-dimension means
 * deleting a dimension's save folder now deletes its own POIs right along with it - no manual
 * cleanup needed, the class of bug is gone rather than patched after the fact. {@code /poi purge}
 * (see {@link PoiCommands}) stays as a manual fallback for anything not caused by a full wipe.
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

   /** {@code level} is the dimension a POI actually belongs to - not a stand-in for "the server",
    *  every dimension keeps its own independent map. */
   private static Map<String, PoiEntry> registry(ServerLevel level) {
      return ((AttachmentTarget) level).getAttachedOrCreate(POIS, LinkedHashMap::new);
   }

   public static void register(ServerLevel level, PoiEntry entry) {
      registry(level).put(entry.key(), entry);
   }

   public static boolean remove(ServerLevel level, String key) {
      return registry(level).remove(key) != null;
   }

   /**
    * Bulk-clears every POI registered for one dimension - a manual fallback for anything that
    * needs cleaning up without a full save-folder wipe (a bounding box correction, a bad manual
    * `/poi register`, etc.). A full dimension reset no longer needs this at all: see the class doc.
    */
   public static int removeAll(ServerLevel level) {
      Map<String, PoiEntry> map = registry(level);
      int count = map.size();
      map.clear();
      return count;
   }

   public static List<PoiEntry> all(ServerLevel level) {
      return List.copyOf(registry(level).values());
   }
}
