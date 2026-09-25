package net.gameoverse.poidiscovery;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import java.util.List;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.DimensionArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.permissions.PermissionCheck;
import net.minecraft.server.permissions.Permissions;
import net.minecraft.world.level.Level;

/**
 * {@code /poi register|remove|list|forget} - admin-only, matches this project's LuckPerms-driven
 * permission model. Major landmark structures register themselves automatically (see
 * {@link StructureScanner}); this is only for manually adding a one-off POI that isn't a
 * recognized structure.
 */
public final class PoiCommands {
   private static final PermissionCheck PERMISSION_CHECK = new PermissionCheck.Require(Permissions.COMMANDS_GAMEMASTER);
   private static final String DEFAULT_ICON = "minecraft:target_point";

   private PoiCommands() {
   }

   public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
      dispatcher.register(
         Commands.literal("poi")
            .requires(Commands.hasPermission(PERMISSION_CHECK))
            .then(Commands.literal("register").then(Commands.argument("name", StringArgumentType.greedyString()).executes(PoiCommands::registerAtFeet)))
            .then(Commands.literal("remove").executes(PoiCommands::removeAtFeet))
            .then(Commands.literal("list").executes(PoiCommands::list))
            .then(Commands.literal("forget").executes(PoiCommands::forgetAll))
            .then(Commands.literal("purge").then(Commands.argument("dimension", DimensionArgument.dimension()).executes(PoiCommands::purgeDimension)))
      );
   }

   private static int registerAtFeet(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
      ServerPlayer player = context.getSource().getPlayerOrException();
      ServerLevel level = context.getSource().getLevel();
      BlockPos pos = player.blockPosition();
      String name = StringArgumentType.getString(context, "name");

      PoiEntry entry = new PoiEntry(pos, pos, pos, level.dimension(), name, DEFAULT_ICON);
      PoiRegistry.register(level, entry);

      context.getSource().sendSuccess(() -> Component.literal("Registered POI: " + name + " at " + pos.toShortString()), true);
      return 1;
   }

   private static int removeAtFeet(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
      ServerPlayer player = context.getSource().getPlayerOrException();
      ServerLevel level = context.getSource().getLevel();
      BlockPos pos = player.blockPosition();

      PoiEntry entry = new PoiEntry(pos, pos, pos, level.dimension(), "", "");
      boolean removed = PoiRegistry.remove(level, entry.key());
      context.getSource()
         .sendSuccess(() -> Component.literal(removed ? "Removed POI at " + pos.toShortString() : "No POI registered at " + pos.toShortString()), true);
      return removed ? 1 : 0;
   }

   private static int list(CommandContext<CommandSourceStack> context) {
      ServerLevel level = context.getSource().getLevel();
      List<PoiEntry> all = PoiRegistry.all(level);
      if (all.isEmpty()) {
         context.getSource().sendSuccess(() -> Component.literal("No POIs registered."), false);
         return 0;
      }

      context.getSource().sendSuccess(() -> Component.literal(all.size() + " registered POI(s):"), false);
      for (PoiEntry entry : all) {
         context.getSource()
            .sendSuccess(() -> Component.literal(" - " + entry.name() + " @ " + entry.pos().toShortString() + " in " + entry.dimension().identifier()), false);
      }
      return all.size();
   }

   /**
    * Bulk-removes every registered POI for one dimension - needed after deleting and regenerating
    * that dimension's own save folder (done once already, for the Nether biome-dilution fix): the
    * registry is a separate persistent attachment on the overworld, untouched by wiping any other
    * dimension's region files, so every POI the scanner had found there is left stale, pointing at
    * terrain that may no longer hold anything real. Also strips the marker from - and un-discovers
    * it for - every online player who'd already found one of the purged POIs, so nothing orphaned
    * is left sitting on a map forever with no registry entry left to ever clean it up again.
    */
   private static int purgeDimension(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
      ServerLevel level = context.getSource().getLevel();
      ServerLevel target = DimensionArgument.getDimension(context, "dimension");
      ResourceKey<Level> dimension = target.dimension();

      List<PoiEntry> purged = PoiRegistry.all(level).stream().filter(e -> e.dimension().equals(dimension)).toList();
      int removed = PoiRegistry.removeAllInDimension(level, dimension);

      for (ServerPlayer player : level.getServer().getPlayerList().getPlayers()) {
         var discovered = PlayerDiscoveries.discovered(player);
         for (PoiEntry entry : purged) {
            if (discovered.contains(entry.key())) {
               MapMarking.removeFromAllMaps(player, entry);
               PlayerDiscoveries.forget(player, entry.key());
            }
         }
      }

      context.getSource().sendSuccess(() -> Component.literal("Purged " + removed + " POI(s) registered for " + dimension.identifier()), true);
      return removed;
   }

   /** Debug/testing only - clears every POI the calling player has discovered. */
   private static int forgetAll(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
      ServerPlayer player = context.getSource().getPlayerOrException();
      PlayerDiscoveries.forgetAll(player);
      context.getSource().sendSuccess(() -> Component.literal("Forgot all discovered POIs."), true);
      return 1;
   }
}
