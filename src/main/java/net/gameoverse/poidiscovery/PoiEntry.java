package net.gameoverse.poidiscovery;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;

/**
 * A registered point of interest: position, dimension, a display name, and a real vanilla map
 * decoration icon (its {@code MapDecorationTypes} registry id, e.g. "minecraft:woodland_mansion").
 * Fully data-driven - no real block/entity anywhere in the world backs this, unlike the original
 * banner-based design (see docs/current-state.md for why that was dropped: the user wanted POIs
 * completely invisible and non-interactable, which a real placed banner can never be).
 */
public record PoiEntry(BlockPos pos, ResourceKey<Level> dimension, String name, String icon) {
   public static final Codec<PoiEntry> CODEC = RecordCodecBuilder.create(
      instance -> instance.group(
            BlockPos.CODEC.fieldOf("pos").forGetter(PoiEntry::pos),
            Level.RESOURCE_KEY_CODEC.fieldOf("dimension").forGetter(PoiEntry::dimension),
            Codec.STRING.fieldOf("name").forGetter(PoiEntry::name),
            Codec.STRING.fieldOf("icon").forGetter(PoiEntry::icon)
         )
         .apply(instance, PoiEntry::new)
   );

   /** Stable string key for this POI, used both as its registry key and in each player's discovered set. */
   public String key() {
      return dimension.identifier() + "@" + pos.getX() + "," + pos.getY() + "," + pos.getZ();
   }
}
