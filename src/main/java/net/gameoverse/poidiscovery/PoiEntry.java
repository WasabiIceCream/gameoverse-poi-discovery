package net.gameoverse.poidiscovery;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;

/**
 * A registered point of interest: just a position + dimension. Deliberately doesn't duplicate the
 * banner's name/color - those already live on the real banner block entity in the world (read via
 * vanilla's own {@code MapBanner.fromWorld()} at discovery time), so there's nothing to keep in
 * sync if someone re-names or re-dyes the banner later.
 */
public record PoiEntry(BlockPos pos, ResourceKey<Level> dimension) {
   public static final Codec<PoiEntry> CODEC = RecordCodecBuilder.create(
      instance -> instance.group(
            BlockPos.CODEC.fieldOf("pos").forGetter(PoiEntry::pos), Level.RESOURCE_KEY_CODEC.fieldOf("dimension").forGetter(PoiEntry::dimension)
         )
         .apply(instance, PoiEntry::new)
   );

   /** Stable string key for this POI, used both as its registry key and in each player's discovered set. */
   public String key() {
      return dimension.identifier() + "@" + pos.getX() + "," + pos.getY() + "," + pos.getZ();
   }
}
