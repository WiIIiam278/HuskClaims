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

import lombok.Builder;
import lombok.Getter;
import net.william278.huskclaims.HuskClaims;
import net.william278.huskclaims.position.BlockPosition;
import net.william278.huskclaims.position.Position;
import net.william278.huskclaims.user.OnlineUser;
import net.william278.huskclaims.user.Preferences;
import net.william278.huskclaims.user.User;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;
import java.util.concurrent.ConcurrentMap;

/**
 * Interface for handling the selection and creation of claims with the claim tool
 *
 * @since 1.0
 */
public interface ClaimSelector {

    @NotNull
    ConcurrentMap<UUID, ClaimSelection> getClaimSelections();

    // Create or select a block for claim creation
    default void handleSelection(@NotNull OnlineUser user, @NotNull ClaimWorld world, @NotNull Position clicked) {
        final ClaimManager.ClaimingMode mode = getClaimingMode(user);
        final ClaimSelection selection = getClaimSelections().get(user.getUuid());

        // If no selection has been made yet, make one at the clicked position
        if (selection == null) {
            switch (mode) {
                case CLAIMS -> makeClaimSelection(user, world, clicked);
                case CHILD_CLAIMS -> makeChildClaimSelection(user, world, clicked);
            }
            return;
        }

        // If the selection involves the resizing of a claim, resize it
        if (selection.isResizeSelection()) {
            switch (mode) {
                case CLAIMS -> resizeClaim(user, world, clicked, selection);
                case CHILD_CLAIMS -> resizeChildClaim(user, world, clicked, selection);
            }
            return;
        }

        // Otherwise, create a new claim
        switch (mode) {
            case CLAIMS -> createClaim(user, world, Region.from(selection.getSelectedPosition(), clicked));
            case CHILD_CLAIMS -> createChildClaim(user, world, Region.from(selection.getSelectedPosition(), clicked));
        }
    }

    private void makeClaimSelection(@NotNull OnlineUser user, @NotNull ClaimWorld world,
                                    @NotNull Position clickedBlock) {
    }

    private void resizeClaim(@NotNull OnlineUser user, @NotNull ClaimWorld world,
                             @NotNull Position clickedBlock, @NotNull ClaimSelection selection) {

    }

    private void createClaim(@NotNull OnlineUser user, @NotNull ClaimWorld world, @NotNull Region region) {

    }


    private ClaimSelection createClaimSelection(@NotNull OnlineUser user, @NotNull ClaimWorld world,
                                                @NotNull BlockPosition clicked) {
        throw new UnsupportedOperationException("Not yet implemented");

    }

    private void makeChildClaimSelection(@NotNull OnlineUser user, @NotNull ClaimWorld world,
                                         @NotNull Position clickedBlock) {

    }

    private void resizeChildClaim(@NotNull OnlineUser user, @NotNull ClaimWorld world,
                                  @NotNull Position clickedBlock, @NotNull ClaimSelection selection) {

    }

    private void createChildClaim(@NotNull OnlineUser user, @NotNull ClaimWorld world, @NotNull Region region) {

    }

    private ClaimSelection createChildClaimSelection(@NotNull OnlineUser user, @NotNull ClaimWorld world,
                                                     @NotNull Claim parent, @NotNull BlockPosition clicked) {
        throw new UnsupportedOperationException("Not yet implemented");
    }

    @NotNull
    private ClaimManager.ClaimingMode getClaimingMode(@NotNull User user) {
        return getPlugin().getUserPreferences(user.getUuid())
                .map(Preferences::getClaimingMode)
                .orElse(ClaimManager.ClaimingMode.CLAIMS);
    }

    @Builder
    @Getter
    class ClaimSelection {

        @Nullable
        Claim claimBeingResized;
        @NotNull
        BlockPosition selectedPosition;

        public boolean isResizeSelection() {
            return claimBeingResized != null;
        }

        public boolean isChildResizeSelection(@NotNull ClaimWorld world) {
            return claimBeingResized != null && claimBeingResized.isChildClaim(world);
        }

        public int getResizedCornerIndex() {
            if (claimBeingResized == null) {
                return -1;
            }
            return claimBeingResized.getRegion().getCorners().indexOf(Region.Corner.wrap(selectedPosition));
        }

    }

    @NotNull
    HuskClaims getPlugin();

}
