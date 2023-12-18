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

import org.jetbrains.annotations.NotNull;

import java.time.OffsetDateTime;

/**
 * Record of a user's data
 *
 * @param user        The user
 * @param lastLogin   The last time the user logged in
 * @param claimBlocks The number of claim blocks the user has
 * @param hoursPlayed The number of hours this user has played
 * @param preferences The user's preferences
 */
public record SavedUser(
        @NotNull User user,
        @NotNull OffsetDateTime lastLogin,
        long claimBlocks,
        int hoursPlayed,
        @NotNull Preferences preferences
) {

    public long getDaysSinceLastLogin() {
        return lastLogin.until(OffsetDateTime.now(), java.time.temporal.ChronoUnit.DAYS);
    }

}
