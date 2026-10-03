package net.gameoverse.poidiscovery;

import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.Item;

public final class ModItems {
   public static final ResourceKey<Item> RUMOR_KEY = ResourceKey.create(Registries.ITEM, Identifier.fromNamespaceAndPath("gameoverse_poi_discovery", "rumor"));
   // Reuses vanilla paper's own model (see assets/gameoverse_poi_discovery/items/rumor.json)
   // rather than shipping a custom texture - a reflavored, renamed paper item is enough for a
   // flavor/lore item like this.
   public static final Item RUMOR = new RumorItem(new Item.Properties().stacksTo(64).setId(RUMOR_KEY));

   private ModItems() {
   }

   public static void register() {
      Registry.register(BuiltInRegistries.ITEM, RUMOR_KEY, RUMOR);
   }
}
