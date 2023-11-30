package net.william278.huskclaims.claim;

import net.william278.cloplib.handler.Handler;
import net.william278.cloplib.operation.Operation;
import net.william278.cloplib.operation.OperationPosition;
import net.william278.cloplib.operation.OperationUser;
import net.william278.cloplib.operation.OperationWorld;
import net.william278.huskclaims.HuskClaims;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;

/**
 * Handler for {@link Operation}s in {@link Claim}s
 *
 * @since 1.0
 */
public interface ClaimHandler extends Handler {

    @Override
    default boolean cancelOperation(@NotNull Operation operation) {
        return getClaimWorld(operation.getOperationPosition().getWorld())
                .map(world -> !world.isOperationAllowed(operation, getPlugin()))
                .orElse(false);
    }

    @Override
    default boolean cancelMovement(@NotNull OperationUser user,
                                   @NotNull OperationPosition from, @NotNull OperationPosition to) {
        // We don't care, just yet. Future todo: enter claim event
        return false;
    }

    @Override
    default boolean cancelNature(@NotNull OperationWorld world,
                                 @NotNull OperationPosition position1, @NotNull OperationPosition position2) {
        // If this isn't in a claim world, we don't care
        if (getClaimWorld(world).isEmpty()) {
            return false;
        }

        // If the two claims are the same, allow it, otherwise, deny it
        final Optional<Claim> claim1 = getClaimAt(position1);
        final Optional<Claim> claim2 = getClaimAt(position2);
        if (claim1.isPresent() && claim2.isPresent()) {
            return !claim1.get().equals(claim2.get());
        }

        // Otherwise allow it so long as there's no claim at either position
        return !(claim1.isEmpty() && claim2.isEmpty());
    }

    Optional<ClaimWorld> getClaimWorld(@NotNull OperationWorld world);

    Optional<Claim> getClaimAt(@NotNull OperationPosition position);

    @NotNull
    HuskClaims getPlugin();

}
