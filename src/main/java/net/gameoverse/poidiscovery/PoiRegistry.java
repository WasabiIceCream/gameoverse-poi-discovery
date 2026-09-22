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

   public static List<PoiEntry> all(ServerLevel level) {
      return List.copyOf(registry(level).values());
   }
}
