package dev.cwhead.GravesX.modules.gravelandguard.command;

import dev.cwhead.GravesX.module.ModuleContext;
import dev.cwhead.GravesX.module.command.GravesXModuleCommand;
import dev.cwhead.GravesX.modules.gravelandguard.i18n.GravelandGuardI18n;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public final class GravelandGuardCommand implements GravesXModuleCommand {

    private final ModuleContext ctx;
    private final GravelandGuardI18n gravelandGuard;

    public GravelandGuardCommand(ModuleContext ctx) {
        this.ctx = ctx;
        this.gravelandGuard = new GravelandGuardI18n(ctx);
    }

    /**
     * Executes the given command, returning its success.
     * <br>
     * If false is returned, then the "usage" plugin.yml entry for this command
     * (if defined) will be sent to the player.
     *
     * @param sender  Source of the command
     * @param command Command which was executed
     * @param label   Alias of the command which was used
     * @param args    Passed command arguments
     * @return true if a valid command, otherwise false
     */
    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String @NotNull [] args) {
        if (sender instanceof Player player) {
            if (ctx.getPlugin().getPermissionManager().hasGrantedPermission("graves.gravelandguard.reload", player.getPlayer())) {
                sender.sendMessage("GravelandGuard:" + ChatColor.RED + " You do not have permission to run this command.");
                return false;
            }
        }
        ctx.reloadConfig();
        gravelandGuard.reload();

        sender.sendMessage("GravelandGuard: config & locale reloaded.");
        return true;
    }
}