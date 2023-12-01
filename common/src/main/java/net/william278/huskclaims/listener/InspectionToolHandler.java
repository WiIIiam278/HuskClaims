package net.william278.huskclaims.listener;

import net.william278.cloplib.operation.OperationPosition;
import net.william278.cloplib.operation.OperationUser;
import net.william278.huskclaims.HuskClaims;
import net.william278.huskclaims.claim.Claim;
import net.william278.huskclaims.claim.ClaimWorld;
import net.william278.huskclaims.position.Position;
import net.william278.huskclaims.user.OnlineUser;
import net.william278.huskclaims.user.User;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;

/**
 * Handler for when the claim creation and resize tool is used
 */
public interface InspectionToolHandler {

    // When the inspection tool (default: stick) is used
    default void onInspectionToolUse(@NotNull OperationUser operationUser, @NotNull OperationPosition position) {
        final OnlineUser user = (OnlineUser) operationUser;

        // Check that the world is claimable
        final Optional<ClaimWorld> optionalWorld = getPlugin().getClaimWorld(position.getWorld());
        if (optionalWorld.isEmpty()) {
            getPlugin().getLocales().getLocale("world_not_claimable")
                    .ifPresent(user::sendMessage);
            return;
        }
        final ClaimWorld claimWorld = optionalWorld.get();

        // Check if there is a claim at the position
        //todo - future: highlight ALL nearby claims if the user is sneaking.
        final Optional<Claim> optionalClaim = claimWorld.getClaimAt((Position) position);
        if (optionalClaim.isEmpty()) {
            getPlugin().getLocales().getLocale("land_not_claimed")
                    .ifPresent(user::sendMessage);
            return;
        }
        highlightClaim(user, optionalClaim.get(), claimWorld);
    }

    // Highlight the claim for the user and send a message
    private void highlightClaim(@NotNull OnlineUser user, @NotNull Claim claim, @NotNull ClaimWorld world) {
        getPlugin().getClaimHighlighter().highlightClaim(user, world, claim);
        getPlugin().getLocales().getLocale("land_claimed_by", getClaimUsername(claim, world))
                .ifPresent(user::sendMessage);
    }

    // Get the claim owner's username
    @NotNull
    private String getClaimUsername(@NotNull Claim claim, @NotNull ClaimWorld world) {
        return claim.getOwner()
                // Get the owner username from the cache. Or, if it's an admin claim, get the admin username
                .flatMap(owner -> world.getUser(owner).map(User::getUsername)
                        .or(() -> getPlugin().getLocales().getRawLocale("administrator_username")))
                // Otherwise, if the name could not be found, return "N/A"
                .orElse(getPlugin().getLocales().getNotApplicable());
    }

    @NotNull
    HuskClaims getPlugin();

}
