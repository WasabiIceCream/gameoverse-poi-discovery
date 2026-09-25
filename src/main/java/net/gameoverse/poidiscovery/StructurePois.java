package net.gameoverse.poidiscovery;

import java.util.Map;

/**
 * Curated allow-list of "major landmark" structures worth auto-registering as a POI - real
 * structure registry keys (confirmed against every installed mod's own
 * data/&lt;namespace&gt;/worldgen/structure/*.json, not guessed) mapped to a display name and a
 * real vanilla {@code MapDecorationTypes} icon id. Originally vanilla-only; expanded 2026-09-23
 * to cover every installed mod's own major landmarks after a real gap was found (Archaion's own
 * Ancient Keep boss structure wasn't even in this list) - still deliberately excludes small
 * decorative/scenery pieces (individual houses, trees, wells, statues - the kind of thing that's
 * one jigsaw piece among many, not a destination on its own). Structures that share the exact
 * same design purpose (e.g. every per-biome Village or Pillager Outpost variant across vanilla and
 * Towns and Towers) intentionally reuse the same display name/icon rather than inventing a unique
 * name for each - the point is recognizing the landmark type, not every reskin.
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
      // --- Vanilla ---
      Map.entry("minecraft:village_plains", new Entry("Plains Village", "minecraft:banner_white")),
      Map.entry("minecraft:village_desert", new Entry("Desert Village", "minecraft:banner_orange")),
      Map.entry("minecraft:village_savanna", new Entry("Savanna Village", "minecraft:banner_yellow")),
      Map.entry("minecraft:village_snowy", new Entry("Snowy Village", "minecraft:banner_light_blue")),
      Map.entry("minecraft:village_taiga", new Entry("Taiga Village", "minecraft:banner_green")),
      Map.entry("minecraft:pillager_outpost", new Entry("Pillager Outpost", "minecraft:banner_gray")),
      Map.entry("minecraft:monument", new Entry("Ocean Monument", "minecraft:banner_blue")),
      Map.entry("minecraft:mansion", new Entry("Woodland Mansion", "minecraft:banner_brown")),
      Map.entry("minecraft:stronghold", new Entry("Stronghold", "minecraft:target_point")),
      Map.entry("minecraft:ancient_city", new Entry("Ancient City", "minecraft:banner_black")),
      Map.entry("minecraft:trial_chambers", new Entry("Trial Chambers", "minecraft:banner_magenta")),
      Map.entry("minecraft:fortress", new Entry("Nether Fortress", "minecraft:banner_red")),
      Map.entry("minecraft:bastion_remnant", new Entry("Bastion Remnant", "minecraft:banner_light_gray")),
      Map.entry("minecraft:jungle_pyramid", new Entry("Jungle Temple", "minecraft:banner_lime")),
      Map.entry("minecraft:swamp_hut", new Entry("Witch Hut", "minecraft:banner_cyan")),
      Map.entry("minecraft:desert_pyramid", new Entry("Desert Pyramid", "minecraft:target_x")),
      Map.entry("minecraft:end_city", new Entry("End City", "minecraft:banner_purple")),
      // Added 2026-09-23 at the user's request - mineshafts/dungeons/ruined portals now included.
      Map.entry("minecraft:mineshaft", new Entry("Mineshaft", "minecraft:target_x")),
      Map.entry("minecraft:mineshaft_mesa", new Entry("Mineshaft", "minecraft:target_x")),
      Map.entry("minecraft:ruined_portal", new Entry("Ruined Portal", "minecraft:target_x")),

      // --- Archaion (this project's own port) - a real gap, wasn't here at all ---
      Map.entry("archaion:ancient_keep", new Entry("Ancient Keep", "minecraft:banner_black")),

      // --- AdoraBuild Structures ---
      Map.entry("adorabuild_structures:ancient_palace_1", new Entry("Ancient Palace", "minecraft:banner_orange")),
      Map.entry("adorabuild_structures:ancient_palace_2", new Entry("Ancient Palace", "minecraft:banner_orange")),
      Map.entry("adorabuild_structures:ancient_palace_3", new Entry("Ancient Palace", "minecraft:banner_orange")),
      Map.entry("adorabuild_structures:basalt_chambers_large_1", new Entry("Basalt Chambers", "minecraft:banner_red")),
      Map.entry("adorabuild_structures:blackstone_bastion_medium_1", new Entry("Bastion Remnant", "minecraft:banner_light_gray")),
      Map.entry("adorabuild_structures:blackstone_bastion_medium_2", new Entry("Bastion Remnant", "minecraft:banner_light_gray")),
      Map.entry("adorabuild_structures:blackstone_bastion_medium_3", new Entry("Bastion Remnant", "minecraft:banner_light_gray")),
      Map.entry("adorabuild_structures:blackstone_bastion_small_1", new Entry("Bastion Remnant", "minecraft:banner_light_gray")),
      Map.entry("adorabuild_structures:blackstone_temple_small_1", new Entry("Blackstone Temple", "minecraft:banner_red")),
      Map.entry("adorabuild_structures:end_temple_large_1", new Entry("End Temple", "minecraft:banner_purple")),
      Map.entry("adorabuild_structures:library_large_1", new Entry("Library", "minecraft:target_point")),
      Map.entry("adorabuild_structures:nether_fortress_large_1", new Entry("Nether Fortress", "minecraft:banner_red")),
      Map.entry("adorabuild_structures:nether_fortress_large_2", new Entry("Nether Fortress", "minecraft:banner_red")),
      Map.entry("adorabuild_structures:nether_fortress_medium_1", new Entry("Nether Fortress", "minecraft:banner_red")),
      Map.entry("adorabuild_structures:nether_temple_medium_1", new Entry("Nether Temple", "minecraft:banner_red")),
      Map.entry("adorabuild_structures:ocean_temple_medium_1", new Entry("Ocean Temple", "minecraft:banner_blue")),
      Map.entry("adorabuild_structures:ocean_temple_medium_2", new Entry("Ocean Temple", "minecraft:banner_blue")),
      Map.entry("adorabuild_structures:ocean_temple_small_1", new Entry("Ocean Temple", "minecraft:banner_blue")),
      Map.entry("adorabuild_structures:ocean_temple_small_2", new Entry("Ocean Temple", "minecraft:banner_blue")),
      Map.entry("adorabuild_structures:pale_fortress_large_1", new Entry("Pale Fortress", "minecraft:banner_white")),
      Map.entry("adorabuild_structures:pale_fortress_medium_1", new Entry("Pale Fortress", "minecraft:banner_white")),
      Map.entry("adorabuild_structures:pale_garden_medium_1", new Entry("Pale Garden", "minecraft:banner_white")),
      Map.entry("adorabuild_structures:prison_large_1", new Entry("Prison", "minecraft:banner_gray")),
      Map.entry("adorabuild_structures:red_sand_temple_medium_1", new Entry("Desert Temple", "minecraft:banner_orange")),
      Map.entry("adorabuild_structures:sand_pyramid_1", new Entry("Desert Pyramid", "minecraft:target_x")),
      Map.entry("adorabuild_structures:sand_underground_castle_1", new Entry("Buried Castle", "minecraft:banner_orange")),

      // --- Ancient Remnants ---
      Map.entry("ancient_remnants:cherry_monument", new Entry("Ancient Monument", "minecraft:banner_pink")),
      Map.entry("ancient_remnants:coniferous_monument", new Entry("Ancient Monument", "minecraft:banner_green")),
      Map.entry("ancient_remnants:hunter_monolith", new Entry("Ancient Monolith", "minecraft:target_point")),
      Map.entry("ancient_remnants:oakwood_monument", new Entry("Ancient Monument", "minecraft:banner_brown")),
      Map.entry("ancient_remnants:sentinel_monolith", new Entry("Ancient Monolith", "minecraft:target_point")),
      Map.entry("ancient_remnants:warrior_monolith", new Entry("Ancient Monolith", "minecraft:target_point")),

      // --- Better Archeology ---
      Map.entry("betterarcheology:catacombs", new Entry("Catacombs", "minecraft:banner_black")),
      Map.entry("betterarcheology:desert_obelisk", new Entry("Desert Obelisk", "minecraft:banner_orange")),
      Map.entry("betterarcheology:light_temple", new Entry("Light Temple", "minecraft:banner_white")),
      Map.entry("betterarcheology:mesa_ruins", new Entry("Mesa Ruins", "minecraft:banner_orange")),
      Map.entry("betterarcheology:stonehenge_grassy", new Entry("Stonehenge", "minecraft:target_point")),
      Map.entry("betterarcheology:temple_jungle", new Entry("Jungle Temple", "minecraft:banner_lime")),
      Map.entry("betterarcheology:tumulus_grassy", new Entry("Burial Mound", "minecraft:banner_green")),

      // --- Dungeons+, Legacies and Legends dungeons, YUNG's Better Dungeons, vanilla mineshaft
      // variants (MMR) - added 2026-09-23 at the user's request. ---
      Map.entry("dungeons_plus:cold_dungeon", new Entry("Dungeon", "minecraft:target_x")),
      Map.entry("dungeons_plus:deepwater_dungeon", new Entry("Dungeon", "minecraft:target_x")),
      Map.entry("dungeons_plus:dungeon_ruins", new Entry("Dungeon", "minecraft:target_x")),
      Map.entry("dungeons_plus:dusty_tomb", new Entry("Dungeon", "minecraft:target_x")),
      Map.entry("dungeons_plus:frozen_dungeon", new Entry("Dungeon", "minecraft:target_x")),
      Map.entry("dungeons_plus:infested_dungeon", new Entry("Dungeon", "minecraft:target_x")),
      Map.entry("dungeons_plus:lush_dungeon", new Entry("Dungeon", "minecraft:target_x")),
      Map.entry("dungeons_plus:mouldy_dungeon", new Entry("Dungeon", "minecraft:target_x")),
      Map.entry("dungeons_plus:muddy_dungeon", new Entry("Dungeon", "minecraft:target_x")),
      Map.entry("dungeons_plus:scorched_tomb", new Entry("Dungeon", "minecraft:target_x")),
      Map.entry("dungeons_plus:webbed_dungeon", new Entry("Dungeon", "minecraft:target_x")),
      Map.entry("legacies_and_legends:arid_dungeon", new Entry("Dungeon", "minecraft:target_x")),
      Map.entry("legacies_and_legends:deep_dungeon", new Entry("Dungeon", "minecraft:target_x")),
      Map.entry("legacies_and_legends:frozen_dungeon", new Entry("Dungeon", "minecraft:target_x")),
      Map.entry("legacies_and_legends:simple_dungeon", new Entry("Dungeon", "minecraft:target_x")),
      Map.entry("legacies_and_legends:verdant_dungeon", new Entry("Dungeon", "minecraft:target_x")),
      Map.entry("legacies_and_legends:infernal_dungeon", new Entry("Dungeon", "minecraft:target_x")),
      Map.entry("betterdungeons:skeleton_dungeon", new Entry("Dungeon", "minecraft:target_x")),
      Map.entry("betterdungeons:small_dungeon", new Entry("Dungeon", "minecraft:target_x")),
      Map.entry("betterdungeons:small_nether_dungeon", new Entry("Dungeon", "minecraft:target_x")),
      Map.entry("betterdungeons:spider_dungeon", new Entry("Dungeon", "minecraft:target_x")),
      Map.entry("betterdungeons:zombie_dungeon", new Entry("Dungeon", "minecraft:target_x")),
      Map.entry("mmr:mesa_mineshaft", new Entry("Mineshaft", "minecraft:target_x")),
      Map.entry("mmr:jungle_mineshaft", new Entry("Mineshaft", "minecraft:target_x")),
      Map.entry("mmr:mineshaft", new Entry("Mineshaft", "minecraft:target_x")),
      Map.entry("mmr:nether_mineshaft", new Entry("Mineshaft", "minecraft:target_x")),
      Map.entry("mmr:desert_mineshaft", new Entry("Mineshaft", "minecraft:target_x")),
      Map.entry("mmr:snowy_mineshaft", new Entry("Mineshaft", "minecraft:target_x")),

      // --- Enderscape ---
      Map.entry("enderscape:end_haven", new Entry("End Haven", "minecraft:banner_purple")),

      // --- Eternal Nether ---
      Map.entry("eternalnether:catacomb", new Entry("Catacomb", "minecraft:banner_red")),
      Map.entry("eternalnether:citadel", new Entry("Citadel", "minecraft:banner_red")),
      Map.entry("eternalnether:piglin_manor", new Entry("Piglin Manor", "minecraft:banner_red")),

      // --- Explorations ---
      Map.entry("explorations:floating_island", new Entry("Floating Island", "minecraft:target_point")),
      Map.entry("explorations:jungle_temple", new Entry("Jungle Temple", "minecraft:banner_lime")),
      Map.entry("explorations:slime_cave", new Entry("Slime Cave", "minecraft:banner_lime")),
      Map.entry("explorations:underground_temple", new Entry("Underground Temple", "minecraft:target_point")),

      // --- Fish of Thieves ---
      Map.entry("fishofthieves:seapost", new Entry("Seapost", "minecraft:banner_blue")),

      // --- Formations (Nether/Overworld) ---
      Map.entry("formationsnether:blackstone_castle", new Entry("Blackstone Castle", "minecraft:banner_red")),
      Map.entry("formationsnether:blackstone_remnant", new Entry("Blackstone Remnant", "minecraft:banner_red")),
      Map.entry("formationsnether:checkerboard_temple", new Entry("Checkerboard Temple", "minecraft:banner_red")),
      Map.entry("formationsoverworld:ice_castle", new Entry("Ice Castle", "minecraft:banner_light_blue")),
      Map.entry("formationsoverworld:large_temple", new Entry("Temple", "minecraft:target_point")),
      Map.entry("formationsoverworld:mesoamerican_temple", new Entry("Temple", "minecraft:target_point")),
      Map.entry("formationsoverworld:witch_tower", new Entry("Witch Tower", "minecraft:banner_cyan")),

      // --- Friends & Foes ---
      Map.entry("friendsandfoes:citadel", new Entry("Citadel", "minecraft:banner_light_blue")),
      Map.entry("friendsandfoes:illusioner_training_grounds", new Entry("Illusioner Training Grounds", "minecraft:banner_purple")),

      // --- Illager Invasion ---
      Map.entry("illagerinvasion:illager_fort", new Entry("Illager Fort", "minecraft:banner_gray")),
      Map.entry("illagerinvasion:illusioner_tower", new Entry("Illusioner Tower", "minecraft:banner_purple")),
      Map.entry("illagerinvasion:labyrinth", new Entry("Labyrinth", "minecraft:banner_gray")),

      // --- Incendium ---
      Map.entry("incendium:infernal_altar", new Entry("Infernal Altar", "minecraft:banner_red")),
      Map.entry("incendium:nether_reactor", new Entry("Nether Reactor", "minecraft:banner_red")),
      Map.entry("incendium:piglin_village", new Entry("Piglin Village", "minecraft:banner_red")),
      Map.entry("incendium:sanctum", new Entry("Sanctum", "minecraft:banner_red")),
      Map.entry("incendium:forbidden_castle", new Entry("Forbidden Castle", "minecraft:banner_red")),
      Map.entry("incendium:ruined_lab", new Entry("Ruined Lab", "minecraft:banner_red")),
      Map.entry("incendium:abandoned_tower", new Entry("Abandoned Tower", "minecraft:banner_red")),

      // --- Legacies and Legends (non-dungeon landmarks) ---
      Map.entry("legacies_and_legends:obelisk", new Entry("Obelisk", "minecraft:target_point")),
      Map.entry("legacies_and_legends:shattered_obelisk", new Entry("Shattered Obelisk", "minecraft:target_point")),
      Map.entry("legacies_and_legends:ruined_library", new Entry("Ruined Library", "minecraft:target_point")),
      Map.entry("legacies_and_legends:spire", new Entry("Spire", "minecraft:target_point")),
      Map.entry("legacies_and_legends:sculk_ruins", new Entry("Sculk Ruins", "minecraft:banner_black")),

      // --- NotEnoughTrials ---
      Map.entry("notenoughtrials:vertical_trial_chambers", new Entry("Trial Chambers", "minecraft:banner_magenta")),

      // --- Ribbits ---
      Map.entry("ribbits:ribbit_village", new Entry("Ribbit Village", "minecraft:banner_green")),

      // --- Towns and Towers - reuses the same names/icons as vanilla's own villages/outposts,
      // just per-biome coverage vanilla doesn't have. ---
      Map.entry("towns_and_towers:mimic_desert", new Entry("Mimic", "minecraft:banner_orange")),
      Map.entry("towns_and_towers:pillager_outpost_badlands", new Entry("Pillager Outpost", "minecraft:banner_gray")),
      Map.entry("towns_and_towers:pillager_outpost_beach", new Entry("Pillager Outpost", "minecraft:banner_gray")),
      Map.entry("towns_and_towers:pillager_outpost_birch_forest", new Entry("Pillager Outpost", "minecraft:banner_gray")),
      Map.entry("towns_and_towers:pillager_outpost_desert", new Entry("Pillager Outpost", "minecraft:banner_gray")),
      Map.entry("towns_and_towers:pillager_outpost_flower_forest", new Entry("Pillager Outpost", "minecraft:banner_gray")),
      Map.entry("towns_and_towers:pillager_outpost_forest", new Entry("Pillager Outpost", "minecraft:banner_gray")),
      Map.entry("towns_and_towers:pillager_outpost_grove", new Entry("Pillager Outpost", "minecraft:banner_gray")),
      Map.entry("towns_and_towers:pillager_outpost_jungle", new Entry("Pillager Outpost", "minecraft:banner_gray")),
      Map.entry("towns_and_towers:pillager_outpost_meadow", new Entry("Pillager Outpost", "minecraft:banner_gray")),
      Map.entry("towns_and_towers:pillager_outpost_mushroom_fields", new Entry("Pillager Outpost", "minecraft:banner_gray")),
      Map.entry("towns_and_towers:pillager_outpost_ocean", new Entry("Pillager Outpost", "minecraft:banner_gray")),
      Map.entry("towns_and_towers:pillager_outpost_old_growth_taiga", new Entry("Pillager Outpost", "minecraft:banner_gray")),
      Map.entry("towns_and_towers:pillager_outpost_savanna", new Entry("Pillager Outpost", "minecraft:banner_gray")),
      Map.entry("towns_and_towers:pillager_outpost_savanna_plateau", new Entry("Pillager Outpost", "minecraft:banner_gray")),
      Map.entry("towns_and_towers:pillager_outpost_snowy_beach", new Entry("Pillager Outpost", "minecraft:banner_gray")),
      Map.entry("towns_and_towers:pillager_outpost_snowy_plains", new Entry("Pillager Outpost", "minecraft:banner_gray")),
      Map.entry("towns_and_towers:pillager_outpost_snowy_slopes", new Entry("Pillager Outpost", "minecraft:banner_gray")),
      Map.entry("towns_and_towers:pillager_outpost_snowy_taiga", new Entry("Pillager Outpost", "minecraft:banner_gray")),
      Map.entry("towns_and_towers:pillager_outpost_sparse_jungle", new Entry("Pillager Outpost", "minecraft:banner_gray")),
      Map.entry("towns_and_towers:pillager_outpost_sunflower_plains", new Entry("Pillager Outpost", "minecraft:banner_gray")),
      Map.entry("towns_and_towers:pillager_outpost_swamp", new Entry("Pillager Outpost", "minecraft:banner_gray")),
      Map.entry("towns_and_towers:pillager_outpost_taiga", new Entry("Pillager Outpost", "minecraft:banner_gray")),
      Map.entry("towns_and_towers:pillager_outpost_wooded_badlands", new Entry("Pillager Outpost", "minecraft:banner_gray")),
      Map.entry("towns_and_towers:village_badlands", new Entry("Village", "minecraft:banner_orange")),
      Map.entry("towns_and_towers:village_beach", new Entry("Village", "minecraft:banner_orange")),
      Map.entry("towns_and_towers:village_birch_forest", new Entry("Village", "minecraft:banner_orange")),
      Map.entry("towns_and_towers:village_flower_forest", new Entry("Village", "minecraft:banner_orange")),
      Map.entry("towns_and_towers:village_forest", new Entry("Village", "minecraft:banner_orange")),
      Map.entry("towns_and_towers:village_grove", new Entry("Village", "minecraft:banner_orange")),
      Map.entry("towns_and_towers:village_jungle", new Entry("Village", "minecraft:banner_orange")),
      Map.entry("towns_and_towers:village_meadow", new Entry("Village", "minecraft:banner_orange")),
      Map.entry("towns_and_towers:village_mushroom_fields", new Entry("Village", "minecraft:banner_orange")),
      Map.entry("towns_and_towers:village_ocean", new Entry("Village", "minecraft:banner_orange")),
      Map.entry("towns_and_towers:village_old_growth_taiga", new Entry("Village", "minecraft:banner_orange")),
      Map.entry("towns_and_towers:village_savanna_plateau", new Entry("Village", "minecraft:banner_orange")),
      Map.entry("towns_and_towers:village_snowy_slopes", new Entry("Village", "minecraft:banner_orange")),
      Map.entry("towns_and_towers:village_snowy_taiga", new Entry("Village", "minecraft:banner_orange")),
      Map.entry("towns_and_towers:village_sparse_jungle", new Entry("Village", "minecraft:banner_orange")),
      Map.entry("towns_and_towers:village_sunflower_plains", new Entry("Village", "minecraft:banner_orange")),
      Map.entry("towns_and_towers:village_swamp", new Entry("Village", "minecraft:banner_orange")),
      Map.entry("towns_and_towers:village_wooded_badlands", new Entry("Village", "minecraft:banner_orange")),

      // --- The Darkness Will Find You ---
      Map.entry("the_darkness_will_find_you:ancient_temple", new Entry("Ancient Temple", "minecraft:banner_black")),
      Map.entry("the_darkness_will_find_you:ancient_village", new Entry("Ancient Village", "minecraft:banner_black"))
   );

   private StructurePois() {
   }

   public static Entry lookup(String structureKey) {
      return STRUCTURES.get(structureKey);
   }

   public record Entry(String name, String icon) {
   }
}
