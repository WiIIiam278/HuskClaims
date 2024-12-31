package net.william278.huskclaims.util;

import net.william278.huskclaims.position.Position;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;

public interface FabricSafeTeleportProvider extends SafeTeleportProvider {

    @Override
    @NotNull
    default CompletableFuture<Optional<Position>> findSafePosition(@NotNull Position position) {
        return CompletableFuture.completedFuture(Optional.empty()); // todo
    }

}
