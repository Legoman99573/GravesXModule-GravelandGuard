package dev.cwhead.GravesX.modules.gravelandguard.listener;

import com.ranull.graves.manager.CacheManager;
import com.ranull.graves.type.Grave;
import dev.cwhead.GravesX.module.ModuleContext;
import dev.cwhead.GravesX.modules.gravelandguard.i18n.GravelandGuardI18n;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockExplodeEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityExplodeEvent;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

public class GravelandGuardListener implements Listener {
    private static final String BYPASS_PERMISSION = "graves.gravelandguard.bypass";

    private final ModuleContext ctx;
    private final CacheManager cacheManager;
    private final GravelandGuardI18n i18n;

    public GravelandGuardListener(ModuleContext ctx, CacheManager cacheManager, GravelandGuardI18n i18n) {
        this.ctx = ctx;
        this.cacheManager = cacheManager;
        this.i18n = i18n;
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        if (!ctx.getConfig().getBoolean("protection.prevent-block-break", true)) return;

        Player player = event.getPlayer();
        if (ctx.getPlugin().getPermissionManager().hasGrantedPermission(BYPASS_PERMISSION, player.getPlayer())) return;

        Location loc = event.getBlock().getLocation();
        boolean allowOwner = ctx.getConfig().getBoolean("protection.allow-grave-owner-place-break", false);

        if (allowOwner) {
            if (isLocationProtectedForPlayer(loc, player)) {
                event.setCancelled(true);
                i18n.sendDenyModify(player);
            }
        } else {
            // No owner exception: block edits only if within *any* grave radius
            if (isWithinAnyGraveRadius(loc)) {
                event.setCancelled(true);
                i18n.sendDenyModify(player);
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onBlockPlace(BlockPlaceEvent event) {
        if (!ctx.getConfig().getBoolean("protection.prevent-block-place", true)) return;

        Player player = event.getPlayer();
        if (ctx.getPlugin().getPermissionManager().hasGrantedPermission(BYPASS_PERMISSION, player.getPlayer())) return;

        Location loc = event.getBlock().getLocation();
        boolean allowOwner = ctx.getConfig().getBoolean("protection.allow-grave-owner-place-break", false);

        if (allowOwner) {
            if (isLocationProtectedForPlayer(loc, player)) {
                event.setCancelled(true);
                i18n.sendDenyModify(player);
            }
        } else {
            if (isWithinAnyGraveRadius(loc)) {
                event.setCancelled(true);
                i18n.sendDenyModify(player);
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onEntityExplode(EntityExplodeEvent event) {
        if (!ctx.getConfig().getBoolean("protection.prevent-explosions", true)) return;

        double radius = getRadius();
        Collection<Grave> graves = getCachedGravesSnapshot();
        if (graves.isEmpty() || event.blockList().isEmpty()) return;

        List<Grave> relevantGraves = getRelevantGravesForExplosion(
                graves,
                radius,
                event.getLocation(),
                event.blockList()
        );
        if (relevantGraves.isEmpty()) return;

        event.blockList().removeIf(block ->
                isWithinAnyGraveRadius(block.getLocation(), radius, relevantGraves)
        );
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onBlockExplode(BlockExplodeEvent event) {
        if (!ctx.getConfig().getBoolean("protection.prevent-explosions", true)) return;

        double radius = getRadius();
        Collection<Grave> graves = getCachedGravesSnapshot();
        if (graves.isEmpty() || event.blockList().isEmpty()) return;

        List<Grave> relevantGraves = getRelevantGravesForExplosion(
                graves,
                radius,
                event.getBlock().getLocation(),
                event.blockList()
        );
        if (relevantGraves.isEmpty()) return;

        event.blockList().removeIf(block ->
                isWithinAnyGraveRadius(block.getLocation(), radius, relevantGraves)
        );
    }

    private List<Grave> getRelevantGravesForExplosion(Collection<Grave> graves,
                                                      double radius,
                                                      Location explosionLocation,
                                                      List<Block> blocks) {
        List<Grave> relevant = new ArrayList<>();
        if (graves.isEmpty() || blocks.isEmpty()) return relevant;

        double minX = explosionLocation.getX();
        double minY = explosionLocation.getY();
        double minZ = explosionLocation.getZ();
        double maxX = minX;
        double maxY = minY;
        double maxZ = minZ;

        for (Block block : blocks) {
            Location l = block.getLocation();
            double x = l.getX();
            double y = l.getY();
            double z = l.getZ();

            if (x < minX) minX = x;
            if (y < minY) minY = y;
            if (z < minZ) minZ = z;
            if (x > maxX) maxX = x;
            if (y > maxY) maxY = y;
            if (z > maxZ) maxZ = z;
        }

        for (Grave grave : graves) {
            if (grave == null) continue;
            Location g = grave.getLocationDeath();
            if (g == null || g.getWorld() == null) continue;
            if (!g.getWorld().equals(explosionLocation.getWorld())) continue;

            double gx = g.getX();
            double gy = g.getY();
            double gz = g.getZ();

            // Grave's cubic protection area
            double graveMinX = gx - radius;
            double graveMaxX = gx + radius;
            double graveMinY = gy - radius;
            double graveMaxY = gy + radius;
            double graveMinZ = gz - radius;
            double graveMaxZ = gz + radius;

            // AABB intersection: if the two boxes overlap at all, this grave is relevant
            boolean intersectX = graveMinX <= maxX && graveMaxX >= minX;
            boolean intersectY = graveMinY <= maxY && graveMaxY >= minY;
            boolean intersectZ = graveMinZ <= maxZ && graveMaxZ >= minZ;

            if (intersectX && intersectY && intersectZ) {
                relevant.add(grave);
            }
        }

        return relevant;
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onEntityDamageByEntity(EntityDamageByEntityEvent event) {
        if (ctx.getConfig().getBoolean("protection.allow-pvp", false)) return;

        if (!(event.getEntity() instanceof Player victim)) return;
        if (!(event.getDamager() instanceof Player damager)) return;

        if (ctx.getPlugin().getPermissionManager().hasGrantedPermission(BYPASS_PERMISSION, victim)
                || ctx.getPlugin().getPermissionManager().hasGrantedPermission(BYPASS_PERMISSION, damager)) return;

        if (isWithinAnyGraveRadius(victim.getLocation()) || isWithinAnyGraveRadius(damager.getLocation())) {
            event.setCancelled(true);
            i18n.sendDenyPvp(damager);
        }
    }

    private boolean isWithinAnyGraveRadius(Location location) {
        return isWithinAnyGraveRadius(location, getRadius(), getCachedGravesSnapshot());
    }

    /**
     * Cubic radius check:
     * inside if |dx| <= radius && |dy| <= radius && |dz| <= radius
     * for any grave in the provided collection.
     *
     * Safe to call on Folia region threads as it only reads cached data
     * and does not touch world state outside the event's region.
     */
    private boolean isWithinAnyGraveRadius(Location location, double radius, Collection<Grave> graves) {
        if (location == null || location.getWorld() == null) return false;
        if (graves == null || graves.isEmpty()) return false;

        for (Grave grave : graves) {
            if (grave == null) continue;

            Location graveLoc = grave.getLocationDeath();
            if (graveLoc == null || graveLoc.getWorld() == null) continue;
            if (!graveLoc.getWorld().equals(location.getWorld())) continue;

            double dx = Math.abs(location.getX() - graveLoc.getX());
            double dy = Math.abs(location.getY() - graveLoc.getY());
            double dz = Math.abs(location.getZ() - graveLoc.getZ());

            if (dx <= radius && dy <= radius && dz <= radius) {
                return true;
            }
        }

        return false;
    }

    /**
     * Returns a snapshot of the cached graves so we can safely iterate
     * on Folia's region threads without concurrent modification issues.
     */
    private Collection<Grave> getCachedGravesSnapshot() {
        Collection<Grave> values = cacheManager.getGraveMap().values();
        return values.isEmpty() ? List.of() : new ArrayList<>(values);
    }

    private double getRadius() {
        return ctx.getConfig().getDouble("protection.radius", 5.0D);
    }

    /**
     * Folia-safe owner check using cached graves only.
     *
     * Protection logic (for this specific player):
     * - Compute all graves covering this location.
     * - hasPlayerGrave = any of those belong to this player.
     * - hasOtherOwnedGrave = any of those belong to someone else (or owner null).
     *
     * The location is considered *protected from this player* iff:
     *   - there is at least one other player's grave at this location AND
     *   - there is NO grave at this location owned by this player.
     *
     * This means:
     *   - Only their own grave: not protected (they can build/break).
     *   - Overlapping graves including theirs: not protected for them.
     *   - Only other players' graves: protected.
     */
    private boolean isLocationProtectedForPlayer(Location location, Player player) {
        if (location == null || location.getWorld() == null || player == null) return false;

        Collection<Grave> graves = getCachedGravesSnapshot();
        if (graves.isEmpty()) {
            return false;
        }

        UUID playerId = player.getUniqueId();
        boolean hasPlayerGrave = false;
        boolean hasOtherOwnedGrave = false;

        double radius = getRadius();
        double radiusSq = radius * radius;

        for (Grave grave : graves) {
            if (grave == null) continue;

            Location graveLoc = grave.getLocationDeath();
            if (graveLoc == null || graveLoc.getWorld() == null) continue;
            if (!graveLoc.getWorld().equals(location.getWorld())) continue;

            if (graveLoc.distanceSquared(location) > radiusSq) {
                continue;
            }

            UUID owner = grave.getOwnerUUID();

            if (owner == null) {
                hasOtherOwnedGrave = true;
            } else if (owner.equals(playerId)) {
                hasPlayerGrave = true;
            } else {
                hasOtherOwnedGrave = true;
            }
        }

        if (hasPlayerGrave) {
            return false;
        }

        return hasOtherOwnedGrave;
    }
}