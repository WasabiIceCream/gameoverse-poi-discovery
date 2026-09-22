package net.gameoverse.poidiscovery.mixin;

import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.saveddata.maps.MapDecorationType;
import net.minecraft.world.level.saveddata.maps.MapItemSavedData;
import org.spongepowered.asm.mixin.Mixin;
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
 */
@Mixin(MapItemSavedData.class)
public interface MapItemSavedDataAccessor {
   @Invoker("addDecoration")
   void gameoverse$addDecoration(Holder<MapDecorationType> type, LevelAccessor level, String key, double x, double z, double rotation, Component name);
}
