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

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import net.william278.huskclaims.HuskClaims;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Range;

import java.time.OffsetDateTime;

/**
 * Represents a user with save data
 *
 * @since 1.0
 **/
@Getter
@AllArgsConstructor
public class SavedUser {

    public static final long MAX_CLAIM_BLOCKS = 999999999999999999L;

    private User user;
    private Preferences preferences;
    @Setter
    private OffsetDateTime lastLogin;
    @Setter
    @Range(from = 0, to = MAX_CLAIM_BLOCKS)
    private long claimBlocks;
    @Setter
    @Deprecated(since = "1.4.5", forRemoval = true)
    private int hoursPlayed;

    public SavedUser(@NotNull User user, @NotNull Preferences preferences,
                     @NotNull OffsetDateTime lastLogin, long claimBlocks) {
        this(user, preferences, lastLogin, claimBlocks, 0);
    }

    public long getDaysSinceLastLogin() {
        return lastLogin.until(OffsetDateTime.now(), java.time.temporal.ChronoUnit.DAYS);
    }

    /**
     * Create a new {@link SavedUser}
     *
     * @param user   The user to create the record for
     * @param plugin The plugin instance
     * @return The new {@link SavedUser}
     * @since 1.0.3
     */
    @NotNull
    public static SavedUser createNew(@NotNull User user, @NotNull HuskClaims plugin) {
        return new SavedUser(
                user,
                Preferences.DEFAULTS,
                OffsetDateTime.now(),
                plugin.getSettings().getClaims().getStartingClaimBlocks()
        );
    }

    /**
     * Create a new {@link SavedUser} with imported data
     *
     * @param user        The user to create the record for
     * @param lastLogin   The last login time of the user
     * @param claimBlocks The number of claim blocks to set
     * @return The {@link SavedUser} from the imported data
     * @since 1.0.3
     */
    @NotNull
    public static SavedUser createImported(@NotNull User user, @NotNull OffsetDateTime lastLogin, int claimBlocks) {
        return new SavedUser(
                user,
                Preferences.IMPORTED,
                lastLogin,
                claimBlocks
        );
    }

}
