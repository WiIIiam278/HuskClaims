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

package net.william278.huskclaims.user;

import net.william278.huskclaims.HuskClaims;
import org.apache.commons.text.WordUtils;
import org.jetbrains.annotations.Blocking;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.Collection;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.logging.Level;

public interface ClaimBlocksManager {

    // How many times per hour to run the claim block update task for all users
    int HOURLY_BLOCKS_UPDATES = 4;

    // Permission to grant hourly claim blocks
    String HOURLY_BLOCKS_PERMISSION = "huskclaims.hourly_blocks.";
    String MAX_CLAIM_BLOCKS_PERMISSION = "huskclaims.max_claim_blocks.";

    Optional<SavedUser> getCachedSavedUser(@NotNull UUID uuid);

    @Blocking
    Optional<SavedUser> getSavedUser(@NotNull UUID uuid);

    @Blocking
    void editSavedUser(@NotNull UUID uuid, @NotNull Consumer<SavedUser> consumer);

    private long getCachedClaimBlocks(@NotNull UUID uuid) {
        return getCachedSavedUser(uuid).map(SavedUser::getClaimBlocks)
                .orElseThrow(() -> new IllegalArgumentException("Couldn't get cached claim blocks for: " + uuid));
    }

    private long getCachedSpentClaimBlocks(@NotNull UUID uuid) {
        return getCachedSavedUser(uuid).map(SavedUser::getSpentClaimBlocks)
                .orElseThrow(() -> new IllegalArgumentException("Couldn't get cached spent claim blocks for: " + uuid));
    }

    default long getCachedClaimBlocks(@NotNull OnlineUser user) {
        return getCachedClaimBlocks(user.getUuid());
    }

    default long getCachedSpentClaimBlocks(@NotNull OnlineUser user) {return getCachedSpentClaimBlocks(user.getUuid());}

    @Blocking
    default long getClaimBlocks(@NotNull UUID uuid) {
        return getSavedUser(uuid).map(SavedUser::getClaimBlocks)
                .orElseThrow(() -> new IllegalArgumentException("Couldn't get cached/saved claim blocks for: " + uuid));
    }

    @Blocking
    default long getSpentClaimBlocks(@NotNull UUID uuid) {
        return getSavedUser(uuid).map(SavedUser::getSpentClaimBlocks)
                .orElseThrow(() -> new IllegalArgumentException("Couldn't get cached/saved spent claim blocks for: " + uuid));
    }

    @Blocking
    default void editClaimBlocks(@NotNull User user, @NotNull SavedUserProvider.ClaimBlockSource source,
                                 @NotNull Function<Long, Long> consumer, @Nullable Consumer<Long> callback) {
        // Determine max claim blocks
        long maxClaimBlocks = getPlugin().getSettings().getClaims().getMaximumClaimBlocks();
        if (user instanceof OnlineUser onlineUser) {
            maxClaimBlocks = onlineUser.getNumericalPermission(MAX_CLAIM_BLOCKS_PERMISSION).orElse(maxClaimBlocks);
        }
        if (maxClaimBlocks < 0) {
            maxClaimBlocks = Long.MAX_VALUE;
        }

        // Calculate block balance
        long originalBlocks = getPlugin().getClaimBlocks(user.getUuid());
        long spent = getPlugin().getSpentClaimBlocks(user.getUuid());
        long currentTotal = originalBlocks + spent;
        long newTotal = consumer.apply(currentTotal);
        long finalTotal = Math.min(newTotal, maxClaimBlocks);
        final long newBlocks = finalTotal - spent;
        if (newBlocks < 0) {
            throw new IllegalArgumentException("Claim blocks cannot be negative (%s)".formatted(newBlocks));
        }

        // Fire the event, update the blocks, trigger callback
        getPlugin().fireClaimBlocksChangeEvent(
                user, originalBlocks, newBlocks, source,
                (event) -> editSavedUser(user.getUuid(), (savedUser) -> {
                    if (source != ClaimBlockSource.HOURLY_BLOCKS) {
                        savedUser.getPreferences().log(source, newBlocks);
                    }
                    savedUser.setClaimBlocks(newBlocks);
                    if (callback != null) {
                        callback.accept(newBlocks);
                    }
                })
        );
    }

    @Blocking
    default void editClaimBlocks(@NotNull User user, @NotNull SavedUserProvider.ClaimBlockSource source,
                                 @NotNull Function<Long, Long> consumer) {
        editClaimBlocks(user, source, consumer, null);
    }

    @Blocking
    default void editSpentClaimBlocks(@NotNull User user, @NotNull SavedUserProvider.ClaimBlockSource ignoredSource,
                                 @NotNull Function<Long, Long> consumer, @Nullable Consumer<Long> callback) {
        getPlugin().editSavedUser(user.getUuid(), (savedUser) -> {
            savedUser.setSpentClaimBlocks(consumer.apply(savedUser.getSpentClaimBlocks()));
            if (callback != null) {
                callback.accept(savedUser.getSpentClaimBlocks());
            }
        });
    }

    @Blocking
    default void editSpentClaimBlocks(@NotNull User user, @NotNull SavedUserProvider.ClaimBlockSource source,
                                 @NotNull Function<Long, Long> consumer) {
        editSpentClaimBlocks(user, source, consumer, null);
    }

    @Blocking
    default void grantHourlyClaimBlocks(@NotNull OnlineUser user) {
        // Validate that the user still has accessible saved data before proceeding
        // This prevents race conditions when users disconnect during scheduler execution
        if (getCachedSavedUser(user.getUuid()).isEmpty()) {
            // User not in cache - skip database lookup during scheduler execution to avoid blocking
            // This prevents performance issues and indicates user likely disconnected
            return;
        }

        final long hourlyBlocks = user.getNumericalPermission(HOURLY_BLOCKS_PERMISSION)
                .orElse(getPlugin().getSettings().getClaims().getHourlyClaimBlocks()) / HOURLY_BLOCKS_UPDATES;
        if (hourlyBlocks <= 0) {
            return;
        }

        try {
            // Double-check user is still online right before block editing to minimize TOCTOU window
            if (!getPlugin().getOnlineUserMap().containsKey(user.getUuid())) {
                return;
            }
            editClaimBlocks(user, ClaimBlockSource.HOURLY_BLOCKS, (blocks -> blocks + hourlyBlocks));
        } catch (IllegalArgumentException e) {
            // Log warning if user data becomes unavailable during execution
            // This can happen if user disconnects after validation but before block editing
            getPlugin().log(Level.WARNING, "Failed to grant hourly claim blocks to user " + 
                user.getUuid() + " (user data unavailable): " + e.getMessage());
        }
    }

    default void loadClaimBlockScheduler() {
        getPlugin().getRepeatingTask(
                () -> {
                    // Create a stable snapshot of online users to prevent concurrent modification issues
                    final Collection<OnlineUser> onlineUsers = getPlugin().getOnlineUsers();
                    if (onlineUsers.isEmpty()) {
                        return; // No users online, skip this cycle
                    }

                    // Process each user with individual error handling to prevent one failure from affecting others
                    int processedUsers = 0;
                    int skippedUsers = 0;
                    
                    for (OnlineUser user : onlineUsers) {
                        try {
                            // Double-check user is still in the online users map (not just in the collection snapshot)
                            if (getPlugin().getOnlineUserMap().containsKey(user.getUuid())) {
                                grantHourlyClaimBlocks(user);
                                processedUsers++;
                            } else {
                                skippedUsers++;
                            }
                        } catch (Exception e) {
                            getPlugin().log(Level.SEVERE, "Unexpected error granting hourly claim blocks to user " +
                                user.getUuid() + ": " + e.getMessage(), e);
                            skippedUsers++;
                        }
                    }

                    if (skippedUsers > 0) {
                        getPlugin().log(Level.INFO, String.format(
                            "Hourly claim blocks cycle completed: %d users processed, %d users skipped", 
                            processedUsers, skippedUsers));
                    }
                },
                Duration.ofMinutes(60 / HOURLY_BLOCKS_UPDATES),
                getFirstClaimBlocksUpdateDelay()
        ).run();
    }

    @NotNull
    private Duration getFirstClaimBlocksUpdateDelay() {
        return Duration.between(
                OffsetDateTime.now(),
                OffsetDateTime.now()
                        .plusMinutes(60 / HOURLY_BLOCKS_UPDATES -
                                OffsetDateTime.now().getMinute() % (60 / HOURLY_BLOCKS_UPDATES))
                        .withSecond(0)
                        .withNano(0)
        );
    }

    @NotNull
    HuskClaims getPlugin();

    enum ClaimBlockSource {
        ADMIN_ADJUSTMENT,
        HOURLY_BLOCKS,
        PURCHASE,
        CLAIM_RESIZED,
        CLAIM_CREATED,
        CLAIM_TRANSFER_AWAY,
        CLAIM_DELETED,
        CLAIMS_DELETED_PRUNED,
        USER_GIFTED,
        API;

        @NotNull
        public String getFormattedName() {
            return WordUtils.capitalizeFully(name().replace("_", " "));
        }
    }

}
