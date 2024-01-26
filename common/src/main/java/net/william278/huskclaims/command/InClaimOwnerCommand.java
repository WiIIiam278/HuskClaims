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

import lombok.AccessLevel;
import lombok.Setter;
import net.william278.huskclaims.HuskClaims;
import net.william278.huskclaims.claim.Claim;
import net.william278.huskclaims.claim.ClaimWorld;
import net.william278.huskclaims.claim.ClaimingMode;
import net.william278.huskclaims.trust.TrustLevel;
import net.william278.huskclaims.user.OnlineUser;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Optional;

public abstract class InClaimOwnerCommand extends InClaimCommand {

    @Setter(AccessLevel.PROTECTED)
    protected String noPrivilegeMessage = "no_claim_privilege";

    protected InClaimOwnerCommand(@NotNull List<String> aliases, @NotNull String usage, @NotNull HuskClaims plugin) {
        super(aliases, usage, plugin);
    }

    @Override
    public void execute(@NotNull OnlineUser executor, @NotNull ClaimWorld world,
                        @NotNull Claim claim, @NotNull String[] args) {
        final Optional<Claim> parent = claim.getParent(world);
        final boolean otherPermission = hasPermission(executor, "other");
        final boolean isAdminEditable = claim.isAdminClaim() && !ClaimingMode.ADMIN_CLAIMS.canUse(executor);

        // Handle child claims
        if (parent.isPresent()) {
            if (isAdminEditable || !otherPermission && !claim.getOwner().map(
                    uuid -> uuid.equals(executor.getUuid())).orElse(false)) {
                plugin.getLocales().getLocale(noPrivilegeMessage)
                        .ifPresent(executor::sendMessage);
                return;
            }
            executeChild(executor, world, claim, parent.get(), args);
            return;
        }

        // Handle parent claims
        if (isAdminEditable || !otherPermission && !claim.isPrivilegeAllowed(
                TrustLevel.Privilege.MANAGE_CHILD_CLAIMS, executor, world, plugin)) {
            plugin.getLocales().getLocale(noPrivilegeMessage)
                    .ifPresent(executor::sendMessage);
            return;
        }
        executeParent(executor, world, claim, args);
    }

    public abstract void executeChild(@NotNull OnlineUser executor, @NotNull ClaimWorld world,
                                      @NotNull Claim child, @NotNull Claim parent, @NotNull String[] args);

    public abstract void executeParent(@NotNull OnlineUser executor, @NotNull ClaimWorld world,
                                       @NotNull Claim claim, @NotNull String[] args);

}
