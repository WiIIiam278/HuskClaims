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
import net.william278.huskclaims.claim.Claim;
import net.william278.huskclaims.claim.ClaimWorld;
import net.william278.huskclaims.claim.TrustLevel;
import net.william278.huskclaims.config.Settings;
import net.william278.huskclaims.user.OnlineUser;
import org.jetbrains.annotations.NotNull;

public class OperationGroupCommand extends InClaimCommand {

    private final Settings.OperationGroup group;

    protected OperationGroupCommand(@NotNull Settings.OperationGroup group, @NotNull HuskClaims plugin) {
        super(group.getToggleCommandAliases(), plugin);
        this.group = group;
    }

    @Override
    public void execute(@NotNull OnlineUser executor, @NotNull ClaimWorld world,
                        @NotNull Claim claim, @NotNull String[] args) {
        if (!claim.isPrivilegeAllowed(TrustLevel.Privilege.MANAGE_OPERATION_GROUPS, executor, world, plugin)) {
            plugin.getLocales().getLocale("no_managing_permission")
                    .ifPresent(executor::sendMessage);
            return;
        }

        if (isOperationGroupSet(claim)) {
            group.getAllowedOperations().forEach(claim.getDefaultFlags()::remove);
            plugin.getLocales().getLocale("enabled_operation_group", group.getName())
                    .ifPresent(executor::sendMessage);
        } else {
            claim.getDefaultFlags().addAll(group.getAllowedOperations());
            plugin.getLocales().getLocale("disabled_operation_group", group.getName())
                    .ifPresent(executor::sendMessage);
        }
        plugin.getDatabase().updateClaimWorld(world);
    }

    private boolean isOperationGroupSet(@NotNull Claim claim) {
        return claim.getDefaultFlags().containsAll(group.getAllowedOperations());
    }

    @NotNull
    @Override
    public String getDescription() {
        return group.getDescription();
    }

}
