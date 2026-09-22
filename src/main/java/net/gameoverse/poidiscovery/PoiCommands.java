package net.gameoverse.poidiscovery;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import java.util.List;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.permissions.PermissionCheck;
import net.minecraft.server.permissions.Permissions;
import net.minecraft.world.level.saveddata.maps.MapBanner;

/** {@code /poi register|remove|list} - admin-only, matches this project's LuckPerms-driven permission model. */
public final class PoiCommands {
   private static final PermissionCheck PERMISSION_CHECK = new PermissionCheck.Require(Permissions.COMMANDS_GAMEMASTER);

   private PoiCommands() {
   }

   public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
      dispatcher.register(
         Commands.literal("poi")
            .requires(Commands.hasPermission(PERMISSION_CHECK))
            .then(Commands.literal("register").executes(PoiCommands::registerAtFeet))
            .then(Commands.literal("remove").executes(PoiCommands::removeAtFeet))
            .then(Commands.literal("list").executes(PoiCommands::list))
      );
   }

   private static int registerAtFeet(com.mojang.brigadier.context.CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
      ServerPlayer player = context.getSource().getPlayerOrException();
      ServerLevel level = context.getSource().getLevel();
      BlockPos pos = player.blockPosition();

      MapBanner banner = MapBanner.fromWorld(level, pos);
      if (banner == null) {
         context.getSource().sendFailure(Component.literal("Stand inside/on the banner you want to register - no banner found at " + pos.toShortString()));
         return 0;
      }

      PoiEntry entry = new PoiEntry(pos, level.dimension());
      PoiRegistry.register(level, entry);

      Component name = banner.name().orElse(Component.literal("(unnamed banner)"));
      context.getSource().sendSuccess(() -> Component.literal("Registered POI: ").append(name).append(" at " + pos.toShortString()), true);
      return 1;
   }

   private static int removeAtFeet(com.mojang.brigadier.context.CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
      ServerPlayer player = context.getSource().getPlayerOrException();
      ServerLevel level = context.getSource().getLevel();
      BlockPos pos = player.blockPosition();

      PoiEntry entry = new PoiEntry(pos, level.dimension());
      boolean removed = PoiRegistry.remove(level, entry.key());
      context.getSource()
         .sendSuccess(() -> Component.literal(removed ? "Removed POI at " + pos.toShortString() : "No POI registered at " + pos.toShortString()), true);
      return removed ? 1 : 0;
   }

   private static int list(com.mojang.brigadier.context.CommandContext<CommandSourceStack> context) {
      ServerLevel level = context.getSource().getLevel();
      List<PoiEntry> all = PoiRegistry.all(level);
      if (all.isEmpty()) {
         context.getSource().sendSuccess(() -> Component.literal("No POIs registered."), false);
         return 0;
      }

      context.getSource().sendSuccess(() -> Component.literal(all.size() + " registered POI(s):"), false);
      for (PoiEntry entry : all) {
         MapBanner banner = MapBanner.fromWorld(level, entry.pos());
         Component name = banner != null ? banner.name().orElse(Component.literal("(unnamed)")) : Component.literal("(banner missing!)");
         context.getSource()
            .sendSuccess(() -> Component.literal(" - ").append(name).append(" @ " + entry.pos().toShortString() + " in " + entry.dimension().identifier()), false);
      }
      return all.size();
   }
}
