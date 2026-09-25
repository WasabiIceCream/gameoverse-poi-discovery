package net.gameoverse.poidiscovery.mixin;

import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.saveddata.maps.MapDecorationType;
import net.minecraft.world.level.saveddata.maps.MapItemSavedData;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

/**
 * Exposes the private {@code addDecoration} - the method that actually writes into
 * {@code MapItemSavedData}'s own renderable {@code decorations} map. Vanilla only calls it from
 * {@code tickCarriedBy}, which itself only runs automatically while a player directly holds a map
 * (hand/offhand/item frame); a map bundled inside a MapStitch Atlas is never "carried" in that
 * sense, so a target decoration written via {@code addTargetDecoration} alone sits on the item
 * forever without ever converting into something that actually renders. Calling this directly
 * (see {@link net.gameoverse.poidiscovery.MapMarking}) does that conversion ourselves, without the
 * unrelated player-position/item-frame bookkeeping the rest of {@code tickCarriedBy} does.
 * <p>
 * Also exposes {@code unlimitedTracking} (normally {@code final}, only ever set {@code true} at
 * creation time for a vanilla Explorer/Treasure Map) - {@code addDecoration}'s own internal
 * {@code calculateDecorationLocationAndType} silently drops (never even calls
 * {@code removeDecoration}, just returns without adding) any non-player decoration whose real
 * position falls outside that specific map's own ~127-block captured square, *unless* this flag
 * is set. A player-made map defaults it to {@code false}, so without flipping it a POI's marker
 * would only ever appear on a map tile that happens to already cover its exact position - the
 * opposite of what a "vague direction+distance hint" discovery feature is for.
 */
@Mixin(MapItemSavedData.class)
public interface MapItemSavedDataAccessor {
   @Invoker("addDecoration")
   void gameoverse$addDecoration(Holder<MapDecorationType> type, LevelAccessor level, String key, double x, double z, double rotation, Component name);

   @Accessor("unlimitedTracking")
   boolean gameoverse$isUnlimitedTracking();

   @Mutable
   @Accessor("unlimitedTracking")
   void gameoverse$setUnlimitedTracking(boolean value);
}
