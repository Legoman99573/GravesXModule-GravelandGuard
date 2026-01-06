package dev.cwhead.GravesX.modules.gravelandguard.i18n;

import com.ranull.graves.Graves;
import com.ranull.graves.integration.MiniMessage;
import dev.cwhead.GravesX.module.ModuleContext;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;

import java.io.File;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

public final class GravelandGuardI18n {

    private static final String KEY_DENY_MODIFY = "gravelandguard.protection.deny-modify";
    private static final String KEY_DENY_PVP = "gravelandguard.protection.deny-pvp";

    private static final String FALLBACK_DENY_MODIFY =
            "&cYou cannot modify blocks near a grave.";
    private static final String FALLBACK_DENY_PVP =
            "&cYou cannot fight so close to a grave.";

    private final Graves plugin;
    private final Logger logger;

    private final ModuleContext ctx;

    private final Map<String, FileConfiguration> localeCache = new ConcurrentHashMap<>();
    private String fallbackLocale;

    public GravelandGuardI18n(ModuleContext ctx) {
        this.ctx = ctx;
        this.plugin = ctx.getPlugin();
        this.logger = ctx.getLogger();

        loadFallbackLocale();
    }

    public void reload() {
        localeCache.clear();
        loadFallbackLocale();
        logger.info("[GravelandGuard] Reloaded locale system.");
    }

    private void loadFallbackLocale() {
        this.fallbackLocale = ctx.getConfig().getString("locale-fallback", "en_us").toLowerCase(Locale.ROOT);
    }

    public void sendDenyModify(Entity entity) {
        if (entity instanceof Player player) {
            if (plugin.getIntegrationManager().hasMiniMessage()) {
                String mainMessage = MiniMessage.convertLegacyToMiniMessage(resolveForEntity(entity, KEY_DENY_MODIFY, FALLBACK_DENY_MODIFY));
                player.sendMessage(MiniMessage.parseString(mainMessage));
            } else {
                player.sendMessage(resolveForEntity(entity, KEY_DENY_MODIFY, FALLBACK_DENY_MODIFY));
            }
        }
    }

    public void sendDenyPvp(Entity entity) {
        if (entity instanceof Player player) {
            if (plugin.getIntegrationManager().hasMiniMessage()) {
                String mainMessage = MiniMessage.convertLegacyToMiniMessage(resolveForEntity(entity, KEY_DENY_PVP, FALLBACK_DENY_PVP));
                player.sendMessage(MiniMessage.parseString(mainMessage));
            } else {
                player.sendMessage(resolveForEntity(entity, KEY_DENY_PVP, FALLBACK_DENY_PVP));
            }
        }
    }

    private String resolveForEntity(Entity entity, String key, String fallback) {
        String localeCode = getLocaleForEntity(entity);

        // Player locale → fallback locale → hardcoded
        String msg = resolveString(localeCode, key);
        if (msg != null) return msg;

        if (!localeCode.equalsIgnoreCase(fallbackLocale)) {
            msg = resolveString(fallbackLocale, key);
            if (msg != null) return msg;
        }

        return fallback;
    }

    private String getLocaleForEntity(Entity entity) {
        if (entity instanceof Player player) {
            try {
                String loc = player.getLocale();
                return loc.toLowerCase(Locale.ROOT);
            } catch (Throwable ignored) {}
        }
        return fallbackLocale;
    }

    private String resolveString(String localeCode, String key) {
        FileConfiguration cfg = getLocaleFile(localeCode);
        if (cfg == null) return null;
        String str = cfg.getString(key);
        return (str == null || str.isEmpty()) ? null : str;
    }

    private FileConfiguration getLocaleFile(String localeCode) {
        if (localeCache.containsKey(localeCode)) {
            return localeCache.get(localeCode);
        }

        File file = new File(ctx.getDataFolder(), "locales/" + localeCode + ".yml");
        if (!file.exists()) {
            localeCache.put(localeCode, null);
            return null;
        }

        FileConfiguration cfg = YamlConfiguration.loadConfiguration(file);
        localeCache.put(localeCode, cfg);
        return cfg;
    }
}