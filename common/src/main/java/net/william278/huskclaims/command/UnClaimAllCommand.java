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
import net.william278.huskclaims.claim.ClaimingMode;
import net.william278.huskclaims.claim.ServerWorldClaim;
import net.william278.huskclaims.user.ClaimBlocksManager;
import net.william278.huskclaims.user.OnlineUser;
import net.william278.huskclaims.user.Preferences;
import net.william278.huskclaims.user.User;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Map;
import java.util.Optional;

public class UnClaimAllCommand extends OnlineUserCommand implements UserListTabCompletable, GlobalClaimsProvider {

    protected UnClaimAllCommand(@NotNull HuskClaims plugin) {
        super(
                List.of("unclaimall", "abandonallclaims"),
                "[player] [confirm]",
                plugin
        );
        addAdditionalPermissions(Map.of(
                "other", true
        ));
    }

    @Override
    public void execute(@NotNull OnlineUser executor, @NotNull String[] args) {
        final ClaimingMode mode = plugin.getUserPreferences(executor.getUuid())
                .map(Preferences::getClaimingMode).orElse(ClaimingMode.CLAIMS);
        final boolean confirm = parseConfirmArg(args);

        // Handle contextually based on current claiming mode
        switch (mode) {
            case ADMIN_CLAIMS -> userDeleteAllAdminClaims(executor, confirm);
            case CLAIMS -> {
                final Optional<User> optionalUser = resolveUser(executor, args);
                if (optionalUser.isEmpty()) {
                    plugin.getLocales().getLocale("error_invalid_syntax", getUsage())
                            .ifPresent(executor::sendMessage);
                    return;
                }
                final User user = optionalUser.get();

                if (!hasPermission(executor, "other") && !executor.equals(user)) {
                    plugin.getLocales().getLocale("error_no_permission")
                            .ifPresent(executor::sendMessage);
                    return;
                }
                userDeleteAllUserClaims(executor, user, confirm);
            }
            case CHILD_CLAIMS -> plugin.getLocales().getLocale("error_delete_all_children")
                    .ifPresent(executor::sendMessage);
        }
    }

    private void userDeleteAllAdminClaims(@NotNull OnlineUser executor, boolean confirm) {
        // Require admin claiming permission
        if (!ClaimingMode.ADMIN_CLAIMS.canUse(executor)) {
            plugin.getLocales().getLocale("error_no_permission")
                    .ifPresent(executor::sendMessage);
            return;
        }

        // Check there are admin claims
        final List<ServerWorldClaim> claims = getAdminClaims();
        if (claims.isEmpty()) {
            plugin.getLocales().getLocale("error_no_admin_claims_made")
                    .ifPresent(executor::sendMessage);
            return;
        }

        // Require confirmation
        if (!confirm) {
            plugin.getLocales().getLocale("delete_all_admin_claims_confirm",
                            Integer.toString(claims.size()), String.format("%s confirm", getName()))
                    .ifPresent(executor::sendMessage);
            return;
        }

        // Remove the admin claims
        plugin.fireDeleteAllClaimsEvent(executor, null, claims, (event) -> {
            plugin.deleteAllAdminClaims(executor);
            plugin.getLocales().getLocale("delete_all_admin_claims", Integer.toString(claims.size()))
                    .ifPresent(executor::sendMessage);
            plugin.getHighlighter(executor).stopHighlighting(executor);
        });
    }

    private void userDeleteAllUserClaims(@NotNull OnlineUser executor, @NotNull User user, boolean confirm) {
        // Check the user has claims
        final List<ServerWorldClaim> claims = getUserClaims(user);
        if (claims.isEmpty()) {
            plugin.getLocales().getLocale("error_no_claims_made", user.getName())
                    .ifPresent(executor::sendMessage);
            return;
        }

        // Require confirmation
        if (!confirm) {
            plugin.getLocales().getLocale("delete_all_claims_confirm", user.getName(),
                            Integer.toString(claims.size()), String.format("%s %s confirm", getName(), user.getName()))
                    .ifPresent(executor::sendMessage);
            return;
        }

        // Remove the claims and return the blocks
        getPlugin().fireDeleteAllClaimsEvent(executor, user, claims, (event) -> {
            long reclaimedBlocks = claims.stream().mapToLong(ServerWorldClaim::getSurfaceArea).sum();
            plugin.deleteAllClaims(executor, user);
            plugin.editClaimBlocks(user, ClaimBlocksManager.ClaimBlockSource.CLAIM_DELETED, (blocks) -> blocks + reclaimedBlocks);
            plugin.getLocales().getLocale("delete_all_claims", user.getName(), Integer.toString(claims.size()),
                    Long.toString(reclaimedBlocks)).ifPresent(executor::sendMessage);
            plugin.getHighlighter(executor).stopHighlighting(executor);
            plugin.invalidateClaimListCache(user.getUuid());
        });
    }

}
