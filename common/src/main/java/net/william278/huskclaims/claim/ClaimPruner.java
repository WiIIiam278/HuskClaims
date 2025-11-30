/*
 * This file is part of HuskClaims, licensed under the Apache License 2.0.
 *
 *  Copyright (c) William278 <will27528@gmail.com>
 *  Copyright (c) contributors
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package net.william278.huskclaims.claim;

import com.google.common.collect.Maps;
import net.william278.huskclaims.HuskClaims;
import net.william278.huskclaims.database.Database;
import net.william278.huskclaims.event.ClaimWorldPruneEvent;
import net.william278.huskclaims.user.ClaimBlocksManager.ClaimBlockSource;
import net.william278.huskclaims.user.SavedUser;
import net.william278.huskclaims.user.User;
import org.jetbrains.annotations.Blocking;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Unmodifiable;

import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.logging.Level;
import java.util.stream.Collectors;

import net.william278.huskclaims.config.Settings;

import static net.william278.huskclaims.config.Settings.ClaimSettings;

/**
 * Interface for pruning claims created by inactive users
 *
 * @since 1.1
 */
public interface ClaimPruner {

    /**
     * Prunes claims on <b>this server</b> as per the plugin config.
     * <p>
     * This deletes all claims created by users marked inactive
     * (those who have not logged on recently as configured in the plugin config).
     * Claim blocks will be refunded accordingly.
     *
     * @since 1.1
     */
    @Blocking
    default void pruneClaims() {
        if (!getSettings().isEnabled() || getSettings().getInactiveDays() <= 0) {
            return;
        }

        // Determine who to prune
        final Set<ClaimWorld> toPrune = getWorldsToPrune();
        final Set<User> inactiveUsers = getInactiveUsers();
        final LocalTime startTime = LocalTime.now();
        getPlugin().log(Level.INFO, String.format("Pruning %s claim worlds with claims by %s inactive users...",
                toPrune.size(), inactiveUsers.size()));

        // Carry out pruning
        refundPrunedBlocks(pruneAndCalculateRefunds(toPrune, inactiveUsers));
        getPlugin().log(Level.INFO, String.format("Successfully pruned claims in %s seconds",
                ChronoUnit.MILLIS.between(startTime, LocalTime.now()) / 1000d));
    }

    // Prunes claim worlds
    @NotNull
    @Blocking
    private Map<User, Long> pruneAndCalculateRefunds(@NotNull Set<ClaimWorld> worlds, @NotNull Set<User> users) {
        // Prune each claim world
        final Map<User, Long> toRefund = Maps.newHashMap();
        for (ClaimWorld world : worlds) {
            if (world.getClaims().isEmpty()) {
                continue;
            }

            // Determine users to prune and claim block volume
            final Map<User, Long> blocks = Maps.newHashMap();
            for (User user : users) {
                final long surfaceArea = world.getSurfaceClaimedBy(user);
                if (surfaceArea > 0) {
                    blocks.put(user, surfaceArea);
                }
            }

            // Fire event, carry out pruning
            final Optional<ClaimWorldPruneEvent> event = getPlugin().fireIsCancelledClaimWorldPruneEvent(world, blocks);
            if (event.isEmpty()) {
                continue;
            }
            event.get().getUserBlocksMap().forEach((u, b) -> {
                toRefund.compute(u, (k, v) -> v == null ? b : v + b);
                world.removeClaimsBy(u);
            });
            getDatabase().updateClaimWorld(world);
        }
        return toRefund;
    }

    // Refunds claim blocks based on a user map of blocks to refund
    @Blocking
    private void refundPrunedBlocks(@NotNull Map<User, Long> blocksToRefund) {
        blocksToRefund.forEach((user, blocks) -> {
            getPlugin().editClaimBlocks(
                    user,
                    ClaimBlockSource.CLAIMS_DELETED_PRUNED,
                    (b -> b + blocks)
            );
            getPlugin().editSpentClaimBlocks(
                    user,
                    ClaimBlockSource.CLAIMS_DELETED_PRUNED,
                    (b -> b - blocks)
            );
        });
    }

    @NotNull
    @Unmodifiable
    default Set<ClaimWorld> getWorldsToPrune() {
        return getPlugin().getClaimWorlds().entrySet().stream()
                .filter((entry) -> !getSettings().getExcludedWorlds().contains(entry.getKey()))
                .map(Map.Entry::getValue).collect(Collectors.toSet());
    }

    @NotNull
    default Set<User> getInactiveUsers() {
        final long days = Math.max(1, getSettings().getInactiveDays());
        return getPlugin().getDatabase().getInactiveUsers(days).stream()
                .map(SavedUser::getUser)
                .filter(user -> !(getSettings().getExcludedUsers().contains(user.getUuid().toString())
                        || getSettings().getExcludedUsers().contains(user.getName())))
                .collect(Collectors.toSet());
    }

    /**
     * Prunes claims with overdue property tax
     * <p>
     * This deletes all claims where the owner has not paid property tax for the configured number of days.
     * Claim blocks will be refunded accordingly.
     *
     * @since 1.5
     */
    @Blocking
    default void pruneOverdueTaxClaims() {
        final Settings.ClaimSettings.PropertyTaxSettings taxSettings =
                getPlugin().getSettings().getClaims().getPropertyTax();
        if (!taxSettings.isEnabled()) {
            return;
        }

        // Determine which worlds to check
        final Set<ClaimWorld> toPrune = getWorldsToPruneForTax();
        if (toPrune.isEmpty()) {
            return;
        }

        final LocalTime startTime = LocalTime.now();
        getPlugin().log(Level.INFO, "Checking for overdue property tax claims...");

        // Find claims with overdue tax
        final Map<User, Long> toRefund = Maps.newHashMap();
        for (ClaimWorld world : toPrune) {
            if (world.getClaims().isEmpty()) {
                continue;
            }

            final Map<User, Long> blocks = Maps.newHashMap();
            for (Claim claim : world.getClaims()) {
                if (claim.isAdminClaim() || claim.getOwner().isEmpty()) {
                    continue;
                }

                final UUID ownerUuid = claim.getOwner().get();
                final Optional<SavedUser> savedUser = getPlugin().getDatabase().getUser(ownerUuid);
                if (savedUser.isEmpty()) {
                    continue;
                }

                final User owner = savedUser.get().getUser();
                final double taxBalance = savedUser.get().getTaxBalance();

                // Check if claim is overdue
                if (getPlugin().isClaimOverdue(claim, world, taxBalance)) {
                    final long surfaceArea = claim.getRegion().getSurfaceArea();
                    blocks.compute(owner, (k, v) -> v == null ? surfaceArea : v + surfaceArea);
                    // Remove the claim (tax balance adjustment not needed - claim removal stops tax accrual)
                    world.removeClaim(claim);
                }
            }

            if (!blocks.isEmpty()) {
                getDatabase().updateClaimWorld(world);
                blocks.forEach((u, b) -> toRefund.compute(u, (k, v) -> v == null ? b : v + b));
            }
        }

        // Refund claim blocks
        refundPrunedBlocks(toRefund);
        if (!toRefund.isEmpty()) {
            getPlugin().log(Level.INFO, String.format("Pruned %s overdue tax claim(s) in %s seconds",
                    toRefund.size(), ChronoUnit.MILLIS.between(startTime, LocalTime.now()) / 1000d));
        }
    }

    @NotNull
    @Unmodifiable
    default Set<ClaimWorld> getWorldsToPruneForTax() {
        final Settings.ClaimSettings.PropertyTaxSettings taxSettings =
                getPlugin().getSettings().getClaims().getPropertyTax();
        return getPlugin().getClaimWorlds().values().stream()
                .filter((world) -> {
                    try {
                        final String worldName = world.getName(getPlugin());
                        return !taxSettings.getExcludedWorlds().contains(worldName);
                    } catch (IllegalStateException e) {
                        return false;
                    }
                })
                .collect(Collectors.toSet());
    }

    @NotNull
    private ClaimSettings.InactivityPruningSettings getSettings() {
        return getPlugin().getSettings().getClaims().getInactivityPruning();
    }

    /**
     * Check if a claim is overdue for tax payment
     *
     * @param claim the claim to check
     * @param world the claim world
     * @param userTaxBalance the user's tax balance
     * @return true if the claim is overdue
     */
    default boolean isClaimOverdue(@NotNull Claim claim, @NotNull ClaimWorld world, double userTaxBalance) {
        return getPlugin().isClaimOverdue(claim, world, userTaxBalance);
    }

    @NotNull
    HuskClaims getPlugin();

    @NotNull
    Database getDatabase();

}
