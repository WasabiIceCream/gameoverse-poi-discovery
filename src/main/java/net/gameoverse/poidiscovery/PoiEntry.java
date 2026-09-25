package net.gameoverse.poidiscovery;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * A registered point of interest: position, dimension, a display name, and a real vanilla map
 * decoration icon (its {@code MapDecorationTypes} registry id, e.g. "minecraft:woodland_mansion").
 * Fully data-driven - no real block/entity anywhere in the world backs this, unlike the original
 * banner-based design (see docs/current-state.md for why that was dropped: the user wanted POIs
 * completely invisible and non-interactable, which a real placed banner can never be).
 * <p>
 * {@code boundsMin}/{@code boundsMax} are the real structure's own bounding box (from
 * {@code StructureStart#getBoundingBox()}), not just a derived pair around {@code pos} - a large
 * structure's actual footprint can be well over 100 blocks across, so discovery/visibility have to
 * measure against the nearest point on this box (see {@link #nearestPointTo}), not distance to one
 * fixed coordinate: a player standing right on top of a sprawling structure could otherwise still
 * read as far from its single recorded point. {@code pos} itself is kept only as this POI's stable
 * identity/display/map-marker anchor - unrelated to the discovery geometry. Old saved entries from
 * before this field existed decode with a zero-size box at {@code pos} (see the codec below),
 * degrading gracefully to the previous point-based behavior until the structure scanner naturally
 * re-registers them with a real box on the next pass.
 */
public record PoiEntry(BlockPos pos, BlockPos boundsMin, BlockPos boundsMax, ResourceKey<Level> dimension, String name, String icon) {
   public static final Codec<PoiEntry> CODEC = RecordCodecBuilder.create(
      instance -> instance.group(
            BlockPos.CODEC.fieldOf("pos").forGetter(PoiEntry::pos),
            BlockPos.CODEC.optionalFieldOf("bounds_min").forGetter(e -> java.util.Optional.of(e.boundsMin())),
            BlockPos.CODEC.optionalFieldOf("bounds_max").forGetter(e -> java.util.Optional.of(e.boundsMax())),
            Level.RESOURCE_KEY_CODEC.fieldOf("dimension").forGetter(PoiEntry::dimension),
            Codec.STRING.fieldOf("name").forGetter(PoiEntry::name),
            Codec.STRING.fieldOf("icon").forGetter(PoiEntry::icon)
         )
         .apply(instance, (pos, min, max, dimension, name, icon) ->
            new PoiEntry(pos, min.orElse(pos), max.orElse(pos), dimension, name, icon)
         )
   );

   /** Stable string key for this POI, used both as its registry key and in each player's discovered set. */
   public String key() {
      return dimension.identifier() + "@" + pos.getX() + "," + pos.getY() + "," + pos.getZ();
   }

   /** The closest point on this POI's real bounding box to {@code from} - the actual geometry to
    *  measure discovery distance and line-of-sight against, not {@link #pos} itself. */
   public Vec3 nearestPointTo(Vec3 from) {
      double x = Mth.clamp(from.x, boundsMin.getX(), boundsMax.getX() + 1.0);
      double y = Mth.clamp(from.y, boundsMin.getY(), boundsMax.getY() + 1.0);
      double z = Mth.clamp(from.z, boundsMin.getZ(), boundsMax.getZ() + 1.0);
      return new Vec3(x, y, z);
   }
}
