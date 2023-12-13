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

package net.william278.huskclaims.command;

import net.william278.huskclaims.HuskClaims;
import net.william278.huskclaims.claim.ClaimWorld;
import net.william278.huskclaims.claim.ClaimingMode;
import net.william278.huskclaims.claim.Region;
import net.william278.huskclaims.user.OnlineUser;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;

public class ClaimCommand extends OnlineUserCommand {

    private final ClaimingMode mode;

    protected ClaimCommand(@NotNull ClaimingMode mode, @NotNull HuskClaims plugin) {
        super(mode.getCommandAliases(), "[size]", plugin);
        this.mode = mode;
    }


    @Override
    public void execute(@NotNull OnlineUser executor, @NotNull String[] args) {
        final Optional<Integer> claimRadius = parseIntArg(args, 0).map(s -> Math.min(1, s / 2));
        if (claimRadius.isPresent()) {
            createClaim(executor, plugin.getClaimWorld(executor.getWorld()).orElse(null), claimRadius.get());
            return;
        }

        plugin.editUserPreferences(executor, (preferences -> {
            final ClaimingMode currentMode = preferences.getClaimingMode();
            if (currentMode != mode) {
                preferences.setClaimingMode(mode);
            } else if (mode != ClaimingMode.CLAIMS) {
                plugin.getLocales().getLocale("error_invalid_syntax", String.format("/%s <size>", getName()))
                        .ifPresent(executor::sendMessage);
                return;
            } else {
                preferences.setClaimingMode(ClaimingMode.CLAIMS);
            }
            plugin.getLocales().getLocale("switched_claiming_mode", mode.getDisplayName(plugin.getLocales()))
                    .ifPresent(executor::sendMessage);
        }));
    }

    private void createClaim(@NotNull OnlineUser user, @Nullable ClaimWorld world, int radius) {
        if (world == null) {
            plugin.getLocales().getLocale("world_not_claimable")
                    .ifPresent(user::sendMessage);
            return;
        }
        switch (mode) {
            case CLAIMS -> plugin.createClaim(user, world, Region.around(user.getPosition(), radius));
            case ADMIN_CLAIMS -> plugin.createAdminClaim(user, world, Region.around(user.getPosition(), radius));
            case CHILD_CLAIMS -> plugin.createChildClaim(user, world, Region.around(user.getPosition(), radius));
        }
    }

}
