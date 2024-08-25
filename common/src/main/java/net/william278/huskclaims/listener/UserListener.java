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

package net.william278.huskclaims.listener;

import net.william278.huskclaims.HuskClaims;
import net.william278.huskclaims.config.Settings;
import net.william278.huskclaims.user.OnlineUser;
import net.william278.huskclaims.user.Preferences;
import org.jetbrains.annotations.NotNull;

public interface UserListener {

    default void onUserJoin(@NotNull OnlineUser user) {
        getPlugin().runAsync(() -> {
            getPlugin().loadUserData(user);
            if (getPlugin().getUserPreferences(user.getUuid()).map(Preferences::isIgnoringClaims).orElse(false)) {
                getPlugin().getLocales().getLocale("ignoring_claims_reminder")
                        .ifPresent(user::sendMessage);
                return;
            }

            final Settings.ClaimSettings settings = getPlugin().getSettings().getClaims();
            if (settings.getBans().isEnabled() || settings.getBans().isPrivateClaims()) {
                checkClaimEnterOnLogin(user);
            }
        });
    }

    default void onUserQuit(@NotNull OnlineUser user) {
        getPlugin().invalidateUserCache(user.getUuid());
        if (!getPlugin().getSettings().getCrossServer().isEnabled()) {
            getPlugin().unlockDrops(user);
        }
    }

    default void onUserSwitchHeldItem(@NotNull OnlineUser user, @NotNull String mainHand, @NotNull String offHand) {
        final Settings.ClaimSettings claims = getPlugin().getSettings().getClaims();
        if (mainHand.equals(claims.getClaimTool()) || mainHand.equals(claims.getInspectionTool())
                || offHand.equals(claims.getClaimTool()) || offHand.equals(claims.getInspectionTool())) {
            return;
        }

        getPlugin().getHighlighter().stopHighlighting(user);
        if (getPlugin().clearClaimSelection(user)) {
            getPlugin().getLocales().getLocale("claim_selection_cancelled")
                    .ifPresent(user::sendMessage);
        }
    }

    // Check a user is able to enter a claim on join (that they are not banned / the claim has been made private)
    private void checkClaimEnterOnLogin(@NotNull OnlineUser u) {
        getPlugin().getClaimWorld(u.getWorld()).ifPresent(w -> w.getClaimAt(u.getPosition()).ifPresent(c -> {
            if (w.isBannedFromClaim(u, c, getPlugin()) || w.cannotNavigatePrivateClaim(u, c, getPlugin())) {
                getPlugin().teleportOutOfClaim(u);
            }
        }));
    }

    @NotNull
    HuskClaims getPlugin();

}
