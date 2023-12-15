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
import net.william278.huskclaims.highlighter.Highlightable;
import net.william278.huskclaims.position.BlockPosition;
import net.william278.huskclaims.position.Position;
import net.william278.huskclaims.user.OnlineUser;
import net.william278.huskclaims.user.Preferences;
import net.william278.huskclaims.user.User;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentMap;

/**
 * Interface for handling the selection and creation of claims with the claim tool
 *
 * @since 1.0
 */
public interface ClaimEditor {

    @NotNull
    ConcurrentMap<UUID, ClaimSelection> getClaimSelections();

    default Optional<ClaimSelection> getClaimSelection(@NotNull User user) {
        return Optional.ofNullable(getClaimSelections().get(user.getUuid()));
    }

    default boolean clearClaimSelection(@NotNull User user) {
        return getClaimSelections().remove(user.getUuid()) != null;
    }

    // Create or select a block for claim creation
    default void handleSelection(@NotNull OnlineUser user, @NotNull ClaimWorld world, @NotNull Position clicked) {
        final ClaimingMode mode = getClaimingMode(user);
        final Optional<ClaimSelection> optionalSelection = getClaimSelection(user);

        // If no selection has been made yet, make one at the clicked position
        if (optionalSelection.isEmpty()) {
            switch (mode) {
                case CLAIMS -> makeClaimSelection(user, world, clicked, false);
                case ADMIN_CLAIMS -> makeClaimSelection(user, world, clicked, true);
                case CHILD_CLAIMS -> makeChildClaimSelection(user, world, clicked);
            }
            return;
        }

        // If the selection involves the resizing of a claim, resize it
        final ClaimSelection selection = optionalSelection.get();
        if (selection.isResizeSelection()) {
            switch (mode) {
                case CLAIMS, ADMIN_CLAIMS -> userResizeClaim(user, world, clicked, selection);
                case CHILD_CLAIMS -> userResizeChildClaim(user, world, clicked, selection);
            }
            clearClaimSelection(user);
            return;
        }

        // Otherwise, create a new claim
        switch (mode) {
            case CLAIMS -> userCreateClaim(user, world, Region.from(selection.getSelectedPosition(), clicked));
            case ADMIN_CLAIMS -> userCreateAdminClaim(user, world, Region.from(selection.getSelectedPosition(), clicked));
            case CHILD_CLAIMS -> userCreateChildClaim(user, world, Region.from(selection.getSelectedPosition(), clicked));
        }
        clearClaimSelection(user);
    }

    // Make a claim selection and add it to the map if valid
    private void makeClaimSelection(@NotNull OnlineUser user, @NotNull ClaimWorld world,
                                    @NotNull Position clickedBlock, boolean isAdmin) {
        createClaimSelection(user, world, clickedBlock, isAdmin).ifPresent(selection -> {
            getClaimSelections().put(user.getUuid(), selection);
            getPlugin().getHighlighter().startHighlighting(user, user.getWorld(), selection);

            if (selection.isResizeSelection()) {
                getPlugin().getLocales().getLocale("claim_selection_resize")
                        .ifPresent(user::sendMessage);
                return;
            }

            getPlugin().getLocales().getLocale("claim_selection_create")
                    .ifPresent(user::sendMessage);
        });
    }

    default void userResizeClaim(@NotNull OnlineUser user, @NotNull ClaimWorld world,
                                 @NotNull Position clickedBlock, @NotNull ClaimSelection selection) {
        final Claim claim = selection.getClaimBeingResized();
        assert claim != null : "Claim selection is not a resize selection";

        // Get the resized claim
        final Region resized = claim.getRegion().getResized(
                selection.getResizedCornerIndex(), Region.Point.wrap(clickedBlock)
        );
        userResizeClaim(user, world, claim, resized);
    }

    default void userResizeClaim(@NotNull OnlineUser user, @NotNull ClaimWorld world,
                                 @NotNull Claim claim, @NotNull Region resized) {
        final List<Claim> overlapsWith = world.getParentClaimsWithin(resized, claim.getRegion());
        if (!overlapsWith.isEmpty()) {
            getPlugin().getLocales().getLocale("land_selection_overlaps")
                    .ifPresent(user::sendMessage);
            getPlugin().getHighlighter().startHighlighting(user, user.getWorld(), overlapsWith, true);
            return;
        }

        // Check all existing child claims still fit within the resized claim
        if (!claim.getChildren().stream().map(Claim::getRegion).allMatch(resized::fullyEncloses)) {
            getPlugin().getLocales().getLocale("selection_resize_not_enclosing_children")
                    .ifPresent(user::sendMessage);
            return;
        }

        // Check claim blocks (non-admin claims)
        if (claim.getOwner().isPresent()) {
            final long additionalBlocksNeeded = resized.getSurfaceArea() - claim.getRegion().getSurfaceArea();
            if (additionalBlocksNeeded > 0 && getPlugin().getClaimBlocks(user.getUuid()) < additionalBlocksNeeded) {
                getPlugin().getLocales().getLocale("error_not_enough_claim_blocks",
                        Long.toString(additionalBlocksNeeded)).ifPresent(user::sendMessage);
                return;
            }

            // Gives blocks back or takes them away based on the difference in size
            getPlugin().editClaimBlocks(user, (blocks) -> blocks - additionalBlocksNeeded);
        }

        claim.setRegion(resized);
        getPlugin().getDatabase().updateClaimWorld(world);
        getPlugin().getHighlighter().startHighlighting(user, user.getWorld(), claim);
        getPlugin().getLocales().getLocale("claim_resized")
                .ifPresent(user::sendMessage);
    }

    default void userCreateClaim(@NotNull OnlineUser user, @NotNull ClaimWorld world, @NotNull Region region) {
        // Validate that the region is not already occupied
        if (doesClaimOverlap(user, world, region)) {
            return;
        }

        // Validate they have enough claim blocks
        final long surfaceArea = region.getSurfaceArea();
        final long userBlocks = getPlugin().getClaimBlocks(user.getUuid());
        if (userBlocks < surfaceArea) {
            getPlugin().getLocales().getLocale("error_not_enough_claim_blocks",
                            Long.toString(surfaceArea - userBlocks))
                    .ifPresent(user::sendMessage);
            return;
        }

        if (region.getShortestEdge() < getPlugin().getSettings().getClaims().getMinimumClaimSize()) {
            getPlugin().getLocales().getLocale("error_claim_too_small",
                            Integer.toString(getPlugin().getSettings().getClaims().getMinimumClaimSize()))
                    .ifPresent(user::sendMessage);
            return;
        }

        // Create the claim
        world.cacheUser(user);
        final Claim claim = getPlugin().createClaimAt(user.getWorld(), region, user);
        getPlugin().getHighlighter().startHighlighting(user, user.getWorld(), claim);
        getPlugin().getLocales().getLocale("claim_created", Long.toString(surfaceArea))
                .ifPresent(user::sendMessage);
    }

    default void userCreateAdminClaim(@NotNull OnlineUser user, @NotNull ClaimWorld world, @NotNull Region region) {
        // Validate that the region is not already occupied
        if (doesClaimOverlap(user, world, region)) {
            return;
        }

        // Create the claim
        world.cacheUser(user);
        final Claim claim = getPlugin().createAdminClaimAt(user.getWorld(), region);

        // Grant the claim creator the highest trust level
        claim.setTrustLevel(user, world, getPlugin().getHighestTrustLevel());

        // Highlight the claim
        getPlugin().getHighlighter().startHighlighting(user, user.getWorld(), claim);
        getPlugin().getLocales().getLocale("claim_created_admin")
                .ifPresent(user::sendMessage);
    }

    private boolean doesClaimOverlap(@NotNull OnlineUser user, @NotNull ClaimWorld world, @NotNull Region region) {
        final List<Claim> overlapsWith = world.getParentClaimsWithin(region);
        if (!overlapsWith.isEmpty()) {
            getPlugin().getLocales().getLocale("land_selection_overlaps")
                    .ifPresent(user::sendMessage);
            getPlugin().getHighlighter().startHighlighting(user, user.getWorld(), overlapsWith, true);
            return true;
        }
        return false;
    }

    @NotNull
    private Optional<ClaimSelection> createClaimSelection(@NotNull OnlineUser user, @NotNull ClaimWorld world,
                                                          @NotNull BlockPosition clicked, boolean isAdmin) {
        final Optional<Claim> clickedClaim = world.getParentClaimAt(clicked);

        // If there's no claim, create a selection at the clicked position
        final ClaimSelection.ClaimSelectionBuilder builder = ClaimSelection.builder().selectedPosition(clicked);
        if (clickedClaim.isEmpty()) {
            if (!(isAdmin ? ClaimingMode.ADMIN_CLAIMS : ClaimingMode.CLAIMS).canUse(user)) {
                getPlugin().getLocales().getLocale("error_no_claiming_permission")
                        .ifPresent(user::sendMessage);
                return Optional.empty();
            }

            return Optional.ofNullable(builder.build());
        }

        // Get the claim and check if they have permission to resize it
        final Claim claim = clickedClaim.get();
        if (claim.getRegion().getClickedCorner(Region.Point.wrap(clicked)) != -1) {
            final ClaimSelection selection = builder.claimBeingResized(claim).build();

            if (!selection.canResize(user, world, getPlugin())) {
                getPlugin().getLocales().getLocale("no_resizing_permission")
                        .ifPresent(user::sendMessage);
                return Optional.empty();
            }

            return Optional.ofNullable(builder.claimBeingResized(claim).build());
        }

        // Otherwise, the user clicked some other part of the claim
        getPlugin().getLocales().getLocale("land_already_claimed", claim.getOwnerName(world, getPlugin()))
                .ifPresent(user::sendMessage);
        return Optional.empty();
    }

    private void makeChildClaimSelection(@NotNull OnlineUser user, @NotNull ClaimWorld world,
                                         @NotNull Position clickedBlock) {
        throw new UnsupportedOperationException("Not yet implemented");
    }

    default void userResizeChildClaim(@NotNull OnlineUser user, @NotNull ClaimWorld world,
                                      @NotNull Position clickedBlock, @NotNull ClaimSelection selection) {
        throw new UnsupportedOperationException("Not yet implemented");
    }

    default void userCreateChildClaim(@NotNull OnlineUser user, @NotNull ClaimWorld world, @NotNull Region region) {
        throw new UnsupportedOperationException("Not yet implemented");
    }

    @NotNull
    private Optional<ClaimSelection> createChildClaimSelection(@NotNull OnlineUser user, @NotNull ClaimWorld world,
                                                               @NotNull Claim parent, @NotNull BlockPosition clicked) {
        throw new UnsupportedOperationException("Not yet implemented");
    }

    @NotNull
    private ClaimingMode getClaimingMode(@NotNull User user) {
        return getPlugin().getUserPreferences(user.getUuid())
                .map(Preferences::getClaimingMode)
                .orElse(ClaimingMode.CLAIMS);
    }

    /**
     * Class representing a claim selection for creation or resize
     *
     * @since 1.0
     */
    @Builder
    @Getter
    class ClaimSelection implements Highlightable {

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
            return claimBeingResized.getRegion().getCorners().indexOf(Region.Point.wrap(selectedPosition));
        }

        public boolean canResize(@NotNull OnlineUser user, @NotNull ClaimWorld world, @NotNull HuskClaims plugin) {
            if (claimBeingResized == null) {
                return false;
            }

            // Check child claim resize privileges
            if (claimBeingResized.isChildClaim(world)) {
                return claimBeingResized.isPrivilegeAllowed(
                        TrustLevel.Privilege.MANAGE_CHILD_CLAIMS, user, world, plugin
                ) && ClaimingMode.CHILD_CLAIMS.canUse(user);
            }

            // Check claim resize privileges
            return claimBeingResized.getOwner()
                    .map(uuid -> uuid.equals(user.getUuid()) && ClaimingMode.CLAIMS.canUse(user))
                    .orElseGet(() -> ClaimingMode.ADMIN_CLAIMS.canUse(user));
        }

        @Override
        @NotNull
        public Map<Region.Point, HighlightType> getHighlightPoints(@NotNull ClaimWorld world, boolean showOverlap) {
            return isResizeSelection() && claimBeingResized != null
                    ? claimBeingResized.getHighlightPoints(world, showOverlap)
                    : Map.of(Region.Point.wrap(selectedPosition), HighlightType.SELECTION);
        }
    }

    @NotNull
    HuskClaims getPlugin();

}
