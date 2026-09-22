package net.gameoverse.poidiscovery;

import com.mojang.serialization.Codec;
import java.util.HashSet;
import java.util.Set;
import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentTarget;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;

/**
 * Which POIs (by {@link PoiEntry#key()}) each player has personally discovered - independent per
 * player, unlike vanilla's own per-map banner toggling. {@code copyOnDeath()} so dying doesn't
 * un-discover anything.
 */
public final class PlayerDiscoveries {
   public static final AttachmentType<Set<String>> DISCOVERED = AttachmentRegistry.<Set<String>>builder()
      .persistent(Codec.STRING.listOf().xmap(HashSet::new, list -> list.stream().toList()))
      .copyOnDeath()
      .initializer(HashSet::new)
      .buildAndRegister(Identifier.fromNamespaceAndPath("gameoverse_poi_discovery", "discovered"));

   private PlayerDiscoveries() {
   }

   /** No-op - just forces this class (and its DISCOVERED AttachmentType registration) to load early. */
   public static void touch() {
   }

   public static boolean hasDiscovered(ServerPlayer player, String poiKey) {
      return ((AttachmentTarget) player).getAttachedOrCreate(DISCOVERED, HashSet::new).contains(poiKey);
   }

   public static void markDiscovered(ServerPlayer player, String poiKey) {
      ((AttachmentTarget) player).getAttachedOrCreate(DISCOVERED, HashSet::new).add(poiKey);
   }

   /** Debug/testing helper - clears every POI this player has discovered. */
   public static void forgetAll(ServerPlayer player) {
      ((AttachmentTarget) player).getAttachedOrCreate(DISCOVERED, HashSet::new).clear();
   }
}
