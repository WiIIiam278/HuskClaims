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
import com.google.common.collect.Sets;
import net.william278.huskclaims.HuskClaims;
import net.william278.huskclaims.database.Database;
import net.william278.huskclaims.user.ClaimBlocksManager.ClaimBlockSource;
import net.william278.huskclaims.user.SavedUser;
import net.william278.huskclaims.user.User;
import org.jetbrains.annotations.Blocking;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Unmodifiable;

import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.stream.Collectors;

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
        if (!getSettings().isEnabled()) {
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
        final Map<User, Long> blocksToRefund = Maps.newHashMap();
        worlds.stream()
                .filter(w -> users.stream().anyMatch(u -> {
                    final long blocks = w.getSurfaceClaimedBy(u);
                    if (blocks > 0 && w.removeClaimsBy(u)) {
                        blocksToRefund.compute(u, (k, v) -> v == null ? blocks : v + blocks);
                        return true;
                    }
                    return false;
                }))
                .forEach(w -> getDatabase().updateClaimWorld(w));
        return blocksToRefund;
    }

    // Refunds claim blocks based on a user map of blocks to refund
    @Blocking
    private void refundPrunedBlocks(@NotNull Map<User, Long> blocksToRefund) {
        blocksToRefund.forEach((user, blocks) -> getPlugin().editClaimBlocks(
                user,
                ClaimBlockSource.CLAIMS_DELETED_PRUNED,
                (b -> b + blocks)
        ));
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
        return getPlugin().getDatabase().getInactiveUsers(getSettings().getInactiveDays()).stream()
                .map(SavedUser::getUser)
                .filter(user -> !(getSettings().getExcludedUsers().contains(user.getUuid().toString())
                        || getSettings().getExcludedUsers().contains(user.getName())))
                .collect(Collectors.toSet());
    }

    @NotNull
    private ClaimSettings.InactivityPruningSettings getSettings() {
        return getPlugin().getSettings().getClaims().getInactivityPruning();
    }

    @NotNull
    HuskClaims getPlugin();

    @NotNull
    Database getDatabase();

}
