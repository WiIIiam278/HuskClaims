package net.william278.huskclaims.listener;

import net.william278.cloplib.operation.OperationPosition;
import net.william278.cloplib.operation.OperationUser;
import net.william278.huskclaims.HuskClaims;
import org.jetbrains.annotations.NotNull;

/**
 * Handler for when the claim creation and resize tool is used
 */
public interface ClaimsToolHandler {

    default void onClaimToolUse(@NotNull OperationUser user, @NotNull OperationPosition position) {

    }

    @NotNull
    HuskClaims getPlugin();

}
