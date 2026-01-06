package dev.cwhead.GravesX.modules.gravelandguard;

import com.ranull.graves.Graves;
import com.ranull.graves.manager.CacheManager;
import dev.cwhead.GravesX.module.GravesXModule;
import dev.cwhead.GravesX.module.ModuleContext;
import dev.cwhead.GravesX.modules.gravelandguard.i18n.GravelandGuardI18n;
import dev.cwhead.GravesX.modules.gravelandguard.listener.GravelandGuardFoliaListener;
import dev.cwhead.GravesX.modules.gravelandguard.listener.GravelandGuardListener;
import org.bukkit.Bukkit;

/**
 * GravelandGuard module:
 * Protects an area around all cached Graves in GravesX.
 */
public final class GravelandGuard extends GravesXModule {

    @Override
    public void onModuleLoad(ModuleContext ctx) {
        ctx.saveDefaultConfig();
        ctx.getLogger().info("[GravelandGuard] Module loaded.");
    }

    @Override
    public void onModuleEnable(ModuleContext ctx) {
        if (!ctx.getConfig().getBoolean("enabled", true)) {
            ctx.getLogger().info("[GravelandGuard] Disabled via config.");
            ctx.getGravesXModules().disableModule(this.toString());
            return;
        }

        Graves plugin = ctx.getPlugin();
        CacheManager cacheManager = plugin.getCacheManager();
        GravelandGuardI18n i18n = new GravelandGuardI18n(ctx);
        if (ctx.getPlugin().getVersionManager().isFolia()) {
            ctx.registerListener(new GravelandGuardFoliaListener(ctx, cacheManager, i18n));
        } else {
            ctx.registerListener(new GravelandGuardListener(ctx, cacheManager, i18n));
        }
        ctx.getLogger().info("[GravelandGuard] Enabled on " + Bukkit.getServer().getName());
    }

    @Override
    public void onModuleDisable(ModuleContext ctx) {
        ctx.getLogger().info("[GravelandGuard] Disabled.");
    }
}