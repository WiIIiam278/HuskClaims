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

import net.william278.cloplib.handler.Handler;
import net.william278.cloplib.operation.Operation;
import net.william278.cloplib.operation.OperationPosition;
import net.william278.cloplib.operation.OperationUser;
import net.william278.cloplib.operation.OperationWorld;
import net.william278.huskclaims.HuskClaims;
import net.william278.huskclaims.command.IgnoreClaimsCommand;
import net.william278.huskclaims.position.Position;
import net.william278.huskclaims.position.World;
import net.william278.huskclaims.user.OnlineUser;
import net.william278.huskclaims.user.Preferences;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;
import java.util.UUID;
import net.william278.cloplib.operation.OperationType;

/**
 * Handler for {@link Operation}s in {@link Claim}s
 *
 * @since 1.0
 */
public interface ClaimHandler extends Handler {

    @Override
    default boolean cancelOperation(@NotNull Operation operation) {
        if (isOperationIgnored(operation)) {
            return false;
        }
        return getClaimWorld((World) operation.getOperationPosition().getWorld())
                .map(world -> !world.isOperationAllowed(operation, getPlugin()))
                .orElse(false);
    }

    @Override
    default boolean cancelMovement(@NotNull OperationUser user,
                                   @NotNull OperationPosition from, @NotNull OperationPosition to) {
        final OnlineUser online = (OnlineUser) user;
        final Position fromPos = (Position) from;
        final Position toPos = (Position) to;

        // Determine the claim world
        final Optional<ClaimWorld> optionalClaimWorld = getClaimWorld(toPos.getWorld());
        if (optionalClaimWorld.isEmpty()) {
            return false;
        }
        final ClaimWorld world = optionalClaimWorld.get();

        // Determine from and to claims
        final Optional<Claim> fromClaim = world.getClaimAt((Position) from);
        final Optional<Claim> toClaim = world.getClaimAt((Position) to);
        if (fromClaim.equals(toClaim)) {
            return false;
        }

        // Handle claim -> claim movement
        if (fromClaim.isPresent() && toClaim.isPresent()) {
            final Claim fc = fromClaim.get();
            final Claim tc = toClaim.get();
            if (getPlugin().fireIsCancelledExitClaimEvent(online, fc, world, fromPos, toPos)
                    || world.isBannedFromClaim(online, tc, getPlugin()) // Check if the user is banned before firing enter event
                    || world.cannotNavigatePrivateClaim(online, tc, getPlugin())
                    || getPlugin().fireIsCancelledEnterClaimEvent(online, tc, world, fromPos, toPos)) {
                return true;
            }

            // Send an entry message
            if (getPlugin().getSettings().getClaims().isSendEntryMessage()
                    && !tc.isChildClaim() && !fc.isChildClaim()) {
                getPlugin().getLocales().getLocale("claim_entered", tc.getOwnerName(world, getPlugin()))
                        .ifPresent(online::sendMessage);
            }
            return false;
        } else if (toClaim.isPresent()) {
            // Handle wilderness -> claim movement
            final Claim tc = toClaim.get();
            if (world.isBannedFromClaim(online, tc, getPlugin())
                    || world.cannotNavigatePrivateClaim(online, tc, getPlugin())
                    || getPlugin().fireIsCancelledEnterClaimEvent(online, tc, world, fromPos, toPos)) {
                return true;
            }

            // Send an entry message
            if (getPlugin().getSettings().getClaims().isSendEntryMessage() && !tc.isChildClaim()) {
                getPlugin().getLocales().getLocale("claim_entered", tc.getOwnerName(world, getPlugin()))
                        .ifPresent(online::sendMessage);
            }
        } else if (fromClaim.isPresent()) {
            // Handle claim -> wilderness movement
            final Claim fc = fromClaim.get();
            if (getPlugin().fireIsCancelledExitClaimEvent(online, fc, world, fromPos, toPos)) {
                return true;
            }

            // Send an exit message
            if (getPlugin().getSettings().getClaims().isSendExitMessage() && !fc.isChildClaim()) {
                getPlugin().getLocales().getLocale("claim_exited", fc.getOwnerName(world, getPlugin()))
                        .ifPresent(online::sendMessage);
            }
        }
        return false;
    }

    @Override
    default boolean cancelNature(@NotNull OperationWorld world,
                                 @NotNull OperationPosition position1, @NotNull OperationPosition position2) {
        // If this isn't in a claim world, we don't care
        final Optional<ClaimWorld> optionalClaimWorld = getClaimWorld((World) world);
        if (optionalClaimWorld.isEmpty()) {
            return false;
        }

        // If the two claims are the same or share the same owner, allow it, otherwise, deny it
        final ClaimWorld claimWorld = optionalClaimWorld.get();
        final Optional<Claim> claim1 = claimWorld.getClaimAt((Position) position1);
        final Optional<Claim> claim2 = claimWorld.getClaimAt((Position) position2);
        if (claim1.isPresent() && claim2.isPresent()) {
            final Claim c1 = claim1.get();
            final Claim c2 = claim2.get();
            if (c1.equals(c2)) {
                return false;
            }
            final Optional<UUID> owner1 = c1.getOwner();
            final Optional<UUID> owner2 = c2.getOwner();
            if (owner1.isEmpty() && owner2.isEmpty()) {
                return false;
            }
            if (owner1.isPresent() && owner2.isPresent()) {
                return !owner1.get().equals(owner2.get());
            }
            return true;
        }

        // Otherwise allow it so long as there's no claim at either position
        return !(claim1.isEmpty() && claim2.isEmpty());
    }

    // Checks if the outcome of an operation is being ignored by its involved user
    private boolean isOperationIgnored(@NotNull Operation operation) {
        return operation.getUser().map(user -> {
            final OnlineUser onlineUser = (OnlineUser) user;
            if (!isIgnoringClaims(onlineUser)) {
                return false;
            }
            
            // Check if user has permission to bypass this specific operation type
            return hasIgnoreClaimsPermission(onlineUser, operation.getType());
        }).orElse(false);
    }

    // Checks if a user has permission to bypass a specific operation type when ignoring claims
    private boolean hasIgnoreClaimsPermission(@NotNull OnlineUser user, @NotNull OperationType operationType) {
        // Check for wildcard permission first
        if (getPlugin().canUseCommand(IgnoreClaimsCommand.class, user, "operations", "*")) {
            return true;
        }
        
        // Check for specific operation type permission
        return getPlugin().canUseCommand(IgnoreClaimsCommand.class, user, "operations", operationType.asMinimalString());
    }

    // Checks if a user has permission to bypass claim bans when ignoring claims
    default boolean hasIgnoreClaimsBanPermission(@NotNull OnlineUser user) {
        return getPlugin().canUseCommand(IgnoreClaimsCommand.class, user, "bans");
    }

    // Checks if a user has permission to bypass private claims when ignoring claims
    default boolean hasIgnoreClaimsPrivatePermission(@NotNull OnlineUser user) {
        return getPlugin().canUseCommand(IgnoreClaimsCommand.class, user, "private");
    }

    // Checks if a user is ignoring claims, ensuring they also have permission to ignore claims
    default boolean isIgnoringClaims(@NotNull OnlineUser u) {
        boolean toggled = getPlugin().getCachedUserPreferences(u.getUuid()).map(Preferences::isIgnoringClaims).orElse(false);
        if (toggled && !getPlugin().canUseCommand(IgnoreClaimsCommand.class, u)) {
            getPlugin().runAsync(() -> {
                getPlugin().editUserPreferences(u, (preferences) -> preferences.setIgnoringClaims(false));
                getPlugin().getLocales().getLocale("respecting_claims")
                        .ifPresent(u::sendMessage);
            });
            return false;
        }
        return toggled;
    }

    Optional<ClaimWorld> getClaimWorld(@NotNull World world);

    Optional<Claim> getClaimAt(@NotNull Position position);

    @NotNull
    HuskClaims getPlugin();

}
