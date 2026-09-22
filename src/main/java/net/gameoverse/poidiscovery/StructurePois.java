package net.gameoverse.poidiscovery;

import java.util.Map;

/**
 * Curated allow-list of "major landmark" structures worth auto-registering as a POI - real
 * vanilla structure registry keys (confirmed against data/minecraft/worldgen/structure/*.json)
 * mapped to a display name and a real vanilla {@code MapDecorationTypes} icon id. Deliberately
 * excludes minor/loot-only structures (buried treasure, igloos, mineshafts, ocean ruins, ruined
 * portals, shipwrecks, trail ruins) per the user's own "worth traveling to" scope.
 * <p>
 * Icons are deliberately restricted to decoration types confirmed (via
 * {@code MapDecorationType#explorationMapElement()}, decompiled from vanilla's own
 * {@code MapDecorationTypes} registration bytecode) to be {@code explorationMapElement = false} -
 * banners, markers, and the two target icons. The structure-flavored icons vanilla itself uses for
 * Explorer Maps (woodland_mansion, ocean_monument, trial_chambers, swamp_hut, jungle_temple, every
 * village variant) are all {@code explorationMapElement = true}; writing one onto a player's own
 * regular map makes MapStitch's {@code ModUtil#isExplorationMap} misclassify that whole map as a
 * treasure map, permanently excluding it from {@code AtlasItem#updateActiveMap}'s candidate scan -
 * the real cause of a marked map going permanently unselectable ("No map in atlas for this area")
 * the moment a player left and returned to its area. See MapMarking's own javadoc for the render
 * side of this investigation.
 */
public final class StructurePois {
   private static final Map<String, Entry> STRUCTURES = Map.ofEntries(
      Map.entry("minecraft:village_plains", new Entry("Plains Village", "minecraft:white_banner")),
      Map.entry("minecraft:village_desert", new Entry("Desert Village", "minecraft:orange_banner")),
      Map.entry("minecraft:village_savanna", new Entry("Savanna Village", "minecraft:yellow_banner")),
      Map.entry("minecraft:village_snowy", new Entry("Snowy Village", "minecraft:light_blue_banner")),
      Map.entry("minecraft:village_taiga", new Entry("Taiga Village", "minecraft:green_banner")),
      Map.entry("minecraft:pillager_outpost", new Entry("Pillager Outpost", "minecraft:gray_banner")),
      Map.entry("minecraft:monument", new Entry("Ocean Monument", "minecraft:blue_banner")),
      Map.entry("minecraft:mansion", new Entry("Woodland Mansion", "minecraft:brown_banner")),
      Map.entry("minecraft:stronghold", new Entry("Stronghold", "minecraft:target_point")),
      Map.entry("minecraft:ancient_city", new Entry("Ancient City", "minecraft:black_banner")),
      Map.entry("minecraft:trial_chambers", new Entry("Trial Chambers", "minecraft:magenta_banner")),
      Map.entry("minecraft:fortress", new Entry("Nether Fortress", "minecraft:red_banner")),
      Map.entry("minecraft:bastion_remnant", new Entry("Bastion Remnant", "minecraft:light_gray_banner")),
      Map.entry("minecraft:jungle_pyramid", new Entry("Jungle Temple", "minecraft:lime_banner")),
      Map.entry("minecraft:swamp_hut", new Entry("Witch Hut", "minecraft:cyan_banner")),
      Map.entry("minecraft:desert_pyramid", new Entry("Desert Pyramid", "minecraft:target_x")),
      Map.entry("minecraft:end_city", new Entry("End City", "minecraft:purple_banner"))
   );

   private StructurePois() {
   }

   public static Entry lookup(String structureKey) {
      return STRUCTURES.get(structureKey);
   }

   public record Entry(String name, String icon) {
   }
}
