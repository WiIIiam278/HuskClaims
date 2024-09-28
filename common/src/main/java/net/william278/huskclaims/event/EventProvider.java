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

package net.william278.huskclaims.event;

import net.william278.huskclaims.claim.Claim;
import net.william278.huskclaims.claim.ClaimWorld;
import net.william278.huskclaims.claim.Region;
import net.william278.huskclaims.claim.ServerWorldClaim;
import net.william278.huskclaims.position.Position;
import net.william278.huskclaims.trust.TrustLevel;
import net.william278.huskclaims.trust.Trustable;
import net.william278.huskclaims.user.OnlineUser;
import net.william278.huskclaims.user.User;
import net.william278.huskclaims.user.UserManager;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.Map;

public interface EventProvider {

    @NotNull
    ClaimBlocksChangeEvent getClaimBlocksChangeEvent(@NotNull User user, long oldBlocks, long newBlocks,
                                                     @NotNull UserManager.ClaimBlockSource reason);

    @NotNull
    CreateChildClaimEvent getCreateChildClaimEvent(@NotNull OnlineUser claimer, @NotNull Claim parent,
                                                   @NotNull Region childRegion, @NotNull ClaimWorld claimWorld);

    @NotNull
    CreateClaimEvent getCreateClaimEvent(@NotNull OnlineUser claimer, @Nullable User claimOwner,
                                         @NotNull Region region, @NotNull ClaimWorld claimWorld);

    @NotNull
    DeleteAllClaimsEvent getDeleteAllClaimsEvent(@NotNull OnlineUser deleter, @Nullable User claimOwner,
                                                 @NotNull Collection<ServerWorldClaim> claims);

    @NotNull
    DeleteChildClaimEvent getDeleteChildClaimEvent(@NotNull OnlineUser deleter, @NotNull Claim parent,
                                                   @NotNull ClaimWorld claimWorld, @NotNull Claim child);

    @NotNull
    DeleteClaimEvent getDeleteClaimEvent(@NotNull OnlineUser deleter, @NotNull Claim claim,
                                         @NotNull ClaimWorld claimWorld);

    @NotNull
    EnterClaimEvent getEnterClaimEvent(@NotNull OnlineUser user, @NotNull Claim claim,
                                       @NotNull ClaimWorld claimWorld,
                                       @NotNull Position enteredFrom, @NotNull Position enteredTo);

    @NotNull
    ExitClaimEvent getExitClaimEvent(@NotNull OnlineUser user, @NotNull Claim claim,
                                     @NotNull ClaimWorld claimWorld,
                                     @NotNull Position exitedFrom, @NotNull Position exitedTo);

    @NotNull
    ResizeChildClaimEvent getResizeChildClaimEvent(@NotNull OnlineUser resizer, @NotNull Claim parent,
                                                   @NotNull Claim child, @NotNull Region newRegion,
                                                   @NotNull ClaimWorld claimWorld);

    @NotNull
    ResizeClaimEvent getResizeClaimEvent(@NotNull OnlineUser resizer, @NotNull Claim claim, @NotNull Region newRegion,
                                         @NotNull ClaimWorld claimWorld);

    @NotNull
    TrustEvent getTrustEvent(@NotNull OnlineUser user, @NotNull TrustLevel level, @NotNull Trustable trusted,
                             @NotNull Claim claim, @NotNull ClaimWorld claimWorld);

    @NotNull
    UnTrustEvent getUnTrustEvent(@NotNull OnlineUser user, @NotNull TrustLevel level, @NotNull Trustable untrusted,
                                 @NotNull Claim claim, @NotNull ClaimWorld claimWorld);

    @NotNull
    TransferClaimEvent getTransferClaimEvent(@NotNull OnlineUser user, @NotNull Claim claim,
                                             @NotNull ClaimWorld claimWorld, @NotNull User newOwner);

    @NotNull
    ClaimBanEvent getClaimBanEvent(@NotNull OnlineUser user, @NotNull Claim claim, @NotNull ClaimWorld claimWorld,
                                   @NotNull User bannedUser);

    @NotNull
    ClaimUnBanEvent getClaimUnBanEvent(@NotNull OnlineUser user, @NotNull Claim claim, @NotNull ClaimWorld claimWorld,
                                       @NotNull User bannedUser);

    @NotNull
    ClaimMakePrivateEvent getClaimMakePrivateEvent(@NotNull OnlineUser user, @NotNull Claim claim,
                                                   @NotNull ClaimWorld claimWorld);

    @NotNull
    ClaimMakePublicEvent getClaimMakePublicEvent(@NotNull OnlineUser user, @NotNull Claim claim,
                                                 @NotNull ClaimWorld claimWorld);

    @NotNull
    ClaimWorldPruneEvent getClaimWorldPruneEvent(@NotNull ClaimWorld claimWorld, @NotNull Map<User, Long> userMap);

}
