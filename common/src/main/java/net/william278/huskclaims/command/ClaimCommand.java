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
import net.william278.huskclaims.config.Settings;
import net.william278.huskclaims.user.OnlineUser;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;
import java.util.StringJoiner;

public class ClaimCommand extends OnlineUserCommand {

    private final ClaimingMode mode;

    protected ClaimCommand(@NotNull ClaimingMode mode, @NotNull HuskClaims plugin) {
        super(
                mode.getCommandAliases(),
                "[radius]",
                plugin
        );
        this.mode = mode;
        this.setOperatorCommand(mode.isAdminRequired());
    }

    @Override
    @NotNull
    public String getPermission(@NotNull String... child) {
        final StringJoiner joiner = new StringJoiner(".").add(mode.getUsePermission());
        for (String node : child) {
            joiner.add(node);
        }
        return joiner.toString();
    }

    @Override
    public void execute(@NotNull OnlineUser executor, @NotNull String[] args) {
        final Optional<Integer> claimRadius = parseIntArg(args, 0).map(s -> Math.max(1, s));
        if (claimRadius.isPresent()) {
            this.createClaim(executor, plugin.getClaimWorld(executor.getWorld()).orElse(null), claimRadius.get());
            return;
        }

        plugin.editUserPreferences(executor, (preferences -> {
            final ClaimingMode currentMode = preferences.getClaimingMode();
            final ClaimingMode newMode;
            if (currentMode != mode) {
                newMode = mode;
            } else if (mode == ClaimingMode.CLAIMS) {
                plugin.getLocales().getLocale("error_invalid_syntax", String.format("/%s <radius>", getName()))
                        .ifPresent(executor::sendMessage);
                return;
            } else {
                newMode = ClaimingMode.CLAIMS;
            }
            preferences.setClaimingMode(newMode);
            plugin.getLocales().getLocale("switched_claiming_mode", newMode.getDisplayName(plugin.getLocales()))
                    .ifPresent(executor::sendMessage);
        }));
    }

    private void createClaim(@NotNull OnlineUser user, @Nullable ClaimWorld world, int radius) {
        final Settings.ClaimSettings claims = plugin.getSettings().getClaims();
        if (claims.isRequireToolForCommands() && !user.isHolding(claims.getClaimToolData())) {
            plugin.getLocales().getLocale("claim_tool_required")
                    .ifPresent(user::sendMessage);
            return;
        }

        if (world == null) {
            plugin.getLocales().getLocale("world_not_claimable")
                    .ifPresent(user::sendMessage);
            return;
        }
        switch (mode) {
            case CLAIMS -> plugin.userCreateClaim(user, world, Region.around(user.getPosition(), radius));
            case ADMIN_CLAIMS -> plugin.userCreateAdminClaim(user, world, Region.around(user.getPosition(), radius));
            case CHILD_CLAIMS -> plugin.userCreateChildClaim(user, world, Region.around(user.getPosition(), radius));
        }
    }

}
