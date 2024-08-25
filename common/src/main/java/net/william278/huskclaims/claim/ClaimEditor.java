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
import net.william278.huskclaims.trust.TrustLevel;
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
                case CLAIMS -> userMakeClaimSelection(user, world, clicked, false);
                case ADMIN_CLAIMS -> userMakeClaimSelection(user, world, clicked, true);
                case CHILD_CLAIMS -> userMakeChildClaimSelection(user, world, clicked);
            }
            return;
        }

        // If the selection involves the resizing of a claim, resize it
        final ClaimSelection select = optionalSelection.get();
        if (select.isResizeSelection()) {
            switch (mode) {
                case CLAIMS, ADMIN_CLAIMS -> userResizeClaim(user, world, clicked, select);
                case CHILD_CLAIMS -> userResizeChildClaim(user, world, clicked, select);
            }
            clearClaimSelection(user);
            return;
        }

        // Otherwise, create a new claim
        switch (mode) {
            case CLAIMS -> userCreateClaim(user, world, Region.from(select.getSelectedPosition(), clicked));
            case ADMIN_CLAIMS -> userCreateAdminClaim(user, world, Region.from(select.getSelectedPosition(), clicked));
            case CHILD_CLAIMS -> userCreateChildClaim(user, world, Region.from(select.getSelectedPosition(), clicked));
        }
        clearClaimSelection(user);
    }

    // Make a claim selection and add it to the map if valid
    private void userMakeClaimSelection(@NotNull OnlineUser user, @NotNull ClaimWorld world,
                                        @NotNull Position clickedBlock, boolean isAdmin) {
        createClaimSelection(user, world, clickedBlock, isAdmin).ifPresent(select -> {
            getClaimSelections().put(user.getUuid(), select);
            getPlugin().getHighlighter().startHighlighting(user, user.getWorld(), select);

            if (select.isResizeSelection()) {
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

        // Get the corner being resized
        final int corner;
        if (claim == null || (corner = selection.getResizedCornerIndex()) == -1) {
            clearClaimSelection(user);
            getPlugin().getLocales().getLocale("claim_selection_cancelled")
                    .ifPresent(user::sendMessage);
            return;
        }

        // Get the resized claim
        final Region resized = claim.getRegion().getResized(corner, Region.Point.wrap(clickedBlock));
        userResizeClaim(user, world, claim, resized);
    }

    default void userResizeClaim(@NotNull OnlineUser user, @NotNull ClaimWorld world,
                                 @NotNull Claim claim, @NotNull Region resized) {
        // Check that the claim is still present in the claim world
        if (!world.contains(claim)) {
            getPlugin().getLocales().getLocale("error_claim_deleted")
                    .ifPresent(user::sendMessage);
            return;
        }

        // Check the area doesn't overlap with another claim, excluding the original claim
        final List<Claim> overlapsWith = world.getParentClaimsOverlapping(resized, claim.getRegion());
        if (!overlapsWith.isEmpty()) {
            getPlugin().getLocales().getLocale("land_selection_overlaps", Integer.toString(overlapsWith.size()))
                    .ifPresent(user::sendMessage);
            getPlugin().getHighlighter().startHighlighting(user, user.getWorld(), overlapsWith, true);
            return;
        }

        // Check the new area isn't restricted by another plugin
        if (getPlugin().getRestrictedRegionHooks().stream().anyMatch(h -> h.isRestricted(resized, user.getWorld()))) {
            getPlugin().getLocales().getLocale("land_selection_restricted")
                    .ifPresent(user::sendMessage);
            return;
        }

        // Check all existing child claims still fit within the resized claim
        if (!claim.getChildren().stream().map(Claim::getRegion).allMatch(resized::fullyEncloses)) {
            getPlugin().getLocales().getLocale("selection_resize_not_enclosing_children")
                    .ifPresent(user::sendMessage);
            return;
        }

        // Checks for non-admin claims
        long neededBlocks = resized.getSurfaceArea() - claim.getRegion().getSurfaceArea();
        if (claim.getOwner().isPresent()) {
            // Check minimum claim size
            if (resized.getShortestEdge() < getPlugin().getSettings().getClaims().getMinimumClaimSize()) {
                getPlugin().getLocales().getLocale("error_claim_too_small",
                                Integer.toString(getPlugin().getSettings().getClaims().getMinimumClaimSize()))
                        .ifPresent(user::sendMessage);
                return;
            }

            // Check claim blocks
            if (getPlugin().getClaimBlocks(user) < neededBlocks) {
                getPlugin().getLocales().getLocale("error_not_enough_claim_blocks",
                        Long.toString(neededBlocks)).ifPresent(user::sendMessage);
                return;
            }
        }

        // Resize the claim
        getPlugin().fireResizeClaimEvent(user, claim, resized, world, (event) -> {
            getPlugin().resizeClaim(world, claim, resized);
            getPlugin().getHighlighter().startHighlighting(user, user.getWorld(), claim);

            // Send the correct message
            if (claim.getOwner().isEmpty()) {
                getPlugin().getLocales().getLocale("admin_claim_resized")
                        .ifPresent(user::sendMessage);
                return;
            }
            getPlugin().getLocales().getLocale(
                            (neededBlocks > 0 ? "claim_resized_spent_blocks" : "claim_resized_reclaimed_blocks"),
                            Long.toString(Math.abs(neededBlocks)))
                    .ifPresent(user::sendMessage);
        });
    }

    default void userCreateClaim(@NotNull OnlineUser user, @NotNull ClaimWorld world, @NotNull Region region) {
        // Validate that the region is not already occupied
        if (doesClaimOverlap(user, world, region)) {
            return;
        }

        // Validate they have enough claim blocks
        final long surfaceArea = region.getSurfaceArea();
        final long userBlocks = getPlugin().getClaimBlocks(user);
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
        getPlugin().fireCreateClaimEvent(user, user, region, world, (event) -> {
            world.cacheUser(user);
            final Claim claim = getPlugin().createClaimAt(world, region, user);
            getPlugin().getHighlighter().startHighlighting(user, user.getWorld(), claim);

            // Indicate that the claim has been created, suggest a trust level command
            getPlugin().getLocales().getLocale("claim_created", Long.toString(surfaceArea))
                    .ifPresent(user::sendMessage);
            getPlugin().getBuildTrustLevel().flatMap(level -> level
                    .getCommandAliases().stream().findFirst()
                    .flatMap(alias -> getPlugin().getLocales().getLocale("claim_created_trust_prompt",
                            alias, level.getDisplayName(), level.getColor(), level.getDescription()
                    ))).ifPresent(user::sendMessage);
        });
    }

    default void userTransferClaim(@NotNull OnlineUser user, @NotNull Claim claim,
                                   @NotNull ClaimWorld claimWorld, @NotNull User newOwner) {
        if (claim.getOwner().isPresent() && claim.getOwner().get().equals(newOwner.getUuid())) {
            getPlugin().getLocales().getLocale("error_transfer_same_owner", newOwner.getName())
                    .ifPresent(user::sendMessage);
            return;
        }

        // Transfer claim
        getPlugin().fireTransferClaimEvent(user, claim, claimWorld, newOwner, (event) -> {
            // Cache user, send message, invalidate source user/admin claim list cache
            claimWorld.cacheUser(newOwner);
            getPlugin().getLocales().getLocale("claim_transferred", claim.getOwnerName(claimWorld, getPlugin()),
                    newOwner.getName()).ifPresent(user::sendMessage);
            getPlugin().invalidateClaimListCache(claim.getOwner().orElse(null));

            // Set the claim, highlight it, invalidate the new owner's claim list cache
            getPlugin().removeMappedClaim(claim, claimWorld);
            claim.unBanUser(newOwner);
            claim.setOwner(newOwner.getUuid());
            getPlugin().getDatabase().updateClaimWorld(claimWorld);
            getPlugin().addMappedClaim(claim, claimWorld);
            getPlugin().getHighlighter().startHighlighting(user, user.getWorld(), claim);
            getPlugin().invalidateClaimListCache(newOwner.getUuid());
        });
    }

    default void userCreateAdminClaim(@NotNull OnlineUser user, @NotNull ClaimWorld world, @NotNull Region region) {
        // Validate that the region is not already occupied
        if (doesClaimOverlap(user, world, region)) {
            return;
        }

        // Create the claim
        getPlugin().fireCreateClaimEvent(user, null, region, world, (event) -> {
            world.cacheUser(user);
            final Claim claim = getPlugin().createAdminClaimAt(world, region);

            // Grant the claim creator the highest trust level
            claim.setTrustLevel(user, getPlugin().getHighestTrustLevel());

            // Highlight the claim
            getPlugin().getHighlighter().startHighlighting(user, user.getWorld(), claim);
            getPlugin().getLocales().getLocale("created_admin_claim")
                    .ifPresent(user::sendMessage);
        });
    }

    // Handle deletion of a claim
    default void userDeleteClaim(@NotNull OnlineUser executor, @NotNull ClaimWorld world,
                                 @NotNull Claim claim) {
        getPlugin().fireDeleteClaimEvent(executor, claim, world, (event) -> {
            getPlugin().deleteClaim(world, claim);
            getPlugin().getHighlighter().stopHighlighting(executor);

            // Send the correct deletion message
            if (claim.getOwner().isPresent()) {
                getPlugin().getLocales().getLocale("claim_deleted",
                                Integer.toString(claim.getRegion().getSurfaceArea()))
                        .ifPresent(executor::sendMessage);
                return;
            }
            getPlugin().getLocales().getLocale("admin_claim_deleted")
                    .ifPresent(executor::sendMessage);
        });
    }

    private boolean doesClaimOverlap(@NotNull OnlineUser user, @NotNull ClaimWorld world, @NotNull Region region) {
        // Check the area doesn't overlap with another claim
        final List<Claim> overlapsWith = world.getParentClaimsOverlapping(region);
        if (!overlapsWith.isEmpty()) {
            getPlugin().getLocales().getLocale("land_selection_overlaps", Integer.toString(overlapsWith.size()))
                    .ifPresent(user::sendMessage);
            getPlugin().getHighlighter().startHighlighting(user, user.getWorld(), overlapsWith, true);
            return true;
        }

        // Check the area isn't restricted by another plugin
        if (getPlugin().getRestrictedRegionHooks().stream().anyMatch(h -> h.isRestricted(region, user.getWorld()))) {
            getPlugin().getLocales().getLocale("land_selection_restricted")
                    .ifPresent(user::sendMessage);
            return true;
        }
        return false;
    }

    @NotNull
    private Optional<ClaimSelection> createClaimSelection(@NotNull OnlineUser user, @NotNull ClaimWorld world,
                                                          @NotNull BlockPosition clicked, boolean isAdmin) {
        // Ensure the clicked position is within the world limits
        if (Region.Point.isOutOfRange(clicked, 0, 0)) {
            getPlugin().getLocales().getLocale("region_outside_world_limits")
                    .ifPresent(user::sendMessage);
            return Optional.empty();
        }
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
            if (selection.cannotResize(user, getPlugin())) {
                getPlugin().getLocales().getLocale("no_resizing_permission")
                        .ifPresent(user::sendMessage);
                return Optional.empty();
            }

            return Optional.ofNullable(builder.build());
        }

        // Otherwise, the user clicked some other part of the claim
        getPlugin().getLocales().getLocale("land_already_claimed", claim.getOwnerName(world, getPlugin()))
                .ifPresent(user::sendMessage);
        getPlugin().getHighlighter().startHighlighting(user, user.getWorld(), claim, true);
        return Optional.empty();
    }

    private void userMakeChildClaimSelection(@NotNull OnlineUser user, @NotNull ClaimWorld world,
                                             @NotNull Position clickedBlock) {
        createChildClaimSelection(user, world, clickedBlock).ifPresent(selection -> {
            getClaimSelections().put(user.getUuid(), selection);
            getPlugin().getHighlighter().startHighlighting(user, user.getWorld(), selection);

            if (selection.isResizeSelection()) {
                getPlugin().getLocales().getLocale("claim_selection_resize_child")
                        .ifPresent(user::sendMessage);
                return;
            }

            getPlugin().getLocales().getLocale("claim_selection_create_child")
                    .ifPresent(user::sendMessage);
        });
    }

    default void userResizeChildClaim(@NotNull OnlineUser user, @NotNull ClaimWorld world,
                                      @NotNull Position clickedBlock, @NotNull ClaimSelection selection) {
        final Claim claim = selection.getClaimBeingResized();

        // Get the resized claim
        final int corner;
        if (claim == null || (corner = selection.getResizedCornerIndex()) == -1) {
            clearClaimSelection(user);
            getPlugin().getLocales().getLocale("claim_selection_cancelled")
                    .ifPresent(user::sendMessage);
            return;
        }
        final Region resized = claim.getRegion().getResized(corner, Region.Point.wrap(clickedBlock));
        userResizeChildClaim(user, world, claim, resized);
    }

    default void userResizeChildClaim(@NotNull OnlineUser user, @NotNull ClaimWorld world,
                                      @NotNull Claim child, @NotNull Region resized) {
        // Check both the parent and child claim being resized still exist
        final Optional<Claim> optionalParent = child.getParent();
        if (optionalParent.isEmpty() || !world.contains(optionalParent.get())) {
            getPlugin().getLocales().getLocale("error_parent_claim_deleted")
                    .ifPresent(user::sendMessage);
            return;
        }
        final Claim parent = optionalParent.get();
        if (!parent.containsChild(child)) {
            getPlugin().getLocales().getLocale("error_claim_deleted")
                    .ifPresent(user::sendMessage);
            return;
        }

        // Ensure the resized region is valid
        if (!parent.getRegion().fullyEncloses(resized) || parent.getRegion().equals(resized)) {
            getPlugin().getHighlighter().startHighlighting(user, user.getWorld(), parent, true);
            getPlugin().getLocales().getLocale("selection_child_not_enclosing_parent")
                    .ifPresent(user::sendMessage);
            return;
        }
        final List<Claim> overlapsWith = parent.getChildClaimsWithin(resized, child.getRegion());
        if (!overlapsWith.isEmpty()) {
            getPlugin().getLocales().getLocale("land_selection_overlaps_child",
                    Integer.toString(overlapsWith.size())).ifPresent(user::sendMessage);
            getPlugin().getHighlighter().startHighlighting(user, user.getWorld(), overlapsWith, true);
            return;
        }

        // Resize the child claim
        getPlugin().fireResizeChildClaimEvent(user, parent, child, resized, world, (event) -> {
            getPlugin().resizeChildClaim(world, child, resized);
            getPlugin().getHighlighter().startHighlighting(user, user.getWorld(), child);
            getPlugin().getLocales().getLocale("child_claim_resized")
                    .ifPresent(user::sendMessage);
        });
    }

    default void userCreateChildClaim(@NotNull OnlineUser user, @NotNull ClaimWorld world, @NotNull Region region) {
        final Optional<Claim> optionalParent = world.getParentClaimAt(region.getNearCorner());
        if (optionalParent.isEmpty()) {
            getPlugin().getLocales().getLocale("selection_child_no_parent")
                    .ifPresent(user::sendMessage);
            return;
        }

        final Claim parent = optionalParent.get();
        if (!parent.getRegion().fullyEncloses(region) || parent.getRegion().equals(region)) {
            getPlugin().getHighlighter().startHighlighting(user, user.getWorld(), parent, true);
            getPlugin().getLocales().getLocale("selection_child_not_enclosing_parent")
                    .ifPresent(user::sendMessage);
            return;
        }

        final List<Claim> overlapsWith = parent.getChildClaimsWithin(region);
        if (!overlapsWith.isEmpty()) {
            getPlugin().getLocales().getLocale("land_selection_overlaps_child",
                    Integer.toString(overlapsWith.size())).ifPresent(user::sendMessage);
            getPlugin().getHighlighter().startHighlighting(user, user.getWorld(), overlapsWith, true);
            return;
        }

        // Create the child claim
        getPlugin().fireCreateChildClaimEvent(user, parent, region, world, (event) -> {
            final Claim child = getPlugin().createChildClaimAt(world, region);
            getPlugin().getHighlighter().startHighlighting(user, user.getWorld(), List.of(parent, child));
            getPlugin().getLocales().getLocale("created_child_claim")
                    .ifPresent(user::sendMessage);
        });
    }

    // Handle deletion of a child claim
    default void userDeleteChildClaim(@NotNull OnlineUser executor, @NotNull ClaimWorld world,
                                      @NotNull Claim claim, @NotNull Claim parent) {
        if (!claim.isPrivilegeAllowed(TrustLevel.Privilege.MANAGE_CHILD_CLAIMS, executor, getPlugin())) {
            getPlugin().getLocales().getLocale("no_child_deletion_permission")
                    .ifPresent(executor::sendMessage);
            return;
        }

        getPlugin().fireDeleteChildClaimEvent(executor, parent, claim, world, (event) -> {
            getPlugin().deleteChildClaim(world, parent, claim);
            getPlugin().getHighlighter().startHighlighting(executor, executor.getWorld(), parent);
            getPlugin().getLocales().getLocale("child_claim_deleted")
                    .ifPresent(executor::sendMessage);
        });
    }

    @NotNull
    private Optional<ClaimSelection> createChildClaimSelection(@NotNull OnlineUser user, @NotNull ClaimWorld world,
                                                               @NotNull BlockPosition clicked) {
        final Optional<Claim> clickedClaim = world.getClaimAt(clicked);
        if (clickedClaim.isEmpty()) {
            getPlugin().getLocales().getLocale("selection_child_no_parent")
                    .ifPresent(user::sendMessage);
            return Optional.empty();
        }

        final Claim claim = clickedClaim.get();
        final ClaimSelection.ClaimSelectionBuilder builder = ClaimSelection.builder().selectedPosition(clicked);
        if (!claim.isChildClaim()) {
            if (!claim.isPrivilegeAllowed(TrustLevel.Privilege.MANAGE_CHILD_CLAIMS, user, getPlugin())) {
                getPlugin().getLocales().getLocale("no_claiming_child_permission")
                        .ifPresent(user::sendMessage);
                return Optional.empty();
            }

            return Optional.of(builder.build());
        }

        if (claim.getRegion().getClickedCorner(Region.Point.wrap(clicked)) != -1) {
            final ClaimSelection selection = builder.claimBeingResized(claim).build();
            if (selection.cannotResize(user, getPlugin())) {
                getPlugin().getLocales().getLocale("no_resizing_child_permission")
                        .ifPresent(user::sendMessage);
                return Optional.empty();
            }

            return Optional.ofNullable(builder.build());
        }

        getPlugin().getLocales().getLocale("land_already_child_claim")
                .ifPresent(user::sendMessage);
        getPlugin().getHighlighter().startHighlighting(user, user.getWorld(), claim, true);
        return Optional.empty();
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

        public int getResizedCornerIndex() {
            if (claimBeingResized == null) {
                return -1;
            }
            return claimBeingResized.getRegion().getCorners().indexOf(Region.Point.wrap(selectedPosition));
        }

        /**
         * @deprecated Use the inverse of {@link #cannotResize(OnlineUser, HuskClaims)} instead
         */
        @Deprecated(since = "1.3.2")
        public boolean canResize(@NotNull OnlineUser user, @SuppressWarnings("unused") @NotNull ClaimWorld world,
                                 @NotNull HuskClaims plugin) {
            return !cannotResize(user, plugin);
        }

        public boolean cannotResize(@NotNull OnlineUser user, @NotNull HuskClaims plugin) {
            if (claimBeingResized == null) {
                return true;
            }

            // Check child claim resize privileges
            if (claimBeingResized.isChildClaim()) {
                return !(claimBeingResized.isPrivilegeAllowed(TrustLevel.Privilege.MANAGE_CHILD_CLAIMS, user, plugin)
                         && ClaimingMode.CHILD_CLAIMS.canUse(user));
            }

            // Check claim resize privileges
            return claimBeingResized.getOwner()
                    .map(uuid -> !(uuid.equals(user.getUuid()) && ClaimingMode.CLAIMS.canUse(user)))
                    .orElse(!ClaimingMode.ADMIN_CLAIMS.canUse(user));
        }

        @Override
        @NotNull
        public Map<Region.Point, Type> getHighlightPoints(@NotNull ClaimWorld world, boolean showOverlap,
                                                          @NotNull BlockPosition viewer, long range) {
            return isResizeSelection() && claimBeingResized != null
                    ? claimBeingResized.getHighlightPoints(world, showOverlap, viewer, range)
                    : Map.of(Region.Point.wrap(selectedPosition), Type.SELECTION);
        }
    }

    @NotNull
    HuskClaims getPlugin();

}
