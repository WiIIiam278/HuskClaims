package net.william278.huskclaims.listener;

import net.william278.cloplib.listener.OperationListener;
import net.william278.huskclaims.HuskClaims;
import org.jetbrains.annotations.NotNull;

public interface ClaimsListener extends OperationListener, ClaimsToolHandler, InspectionToolHandler {

    default void register() {
        setInspectorCallback(getPlugin().getSettings().getClaims().getInspectionTool(), this::onInspectionToolUse);
        setInspectorCallback(getPlugin().getSettings().getClaims().getClaimTool(), this::onClaimToolUse);
    }

    @Override
    default int getInspectionDistance() {
        return getPlugin().getSettings().getClaims().getInspectionDistance();
    }

    @NotNull
    HuskClaims getPlugin();

}
