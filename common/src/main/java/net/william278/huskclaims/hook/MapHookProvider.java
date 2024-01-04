package net.william278.huskclaims.hook;

import net.william278.huskclaims.claim.Claim;
import net.william278.huskclaims.claim.ClaimWorld;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.Unmodifiable;

import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

public interface MapHookProvider {

    @NotNull
    @Unmodifiable
    default Set<MapHook> getMapHooks() {
        return getHooks().stream()
                .filter(hook -> hook instanceof MapHook)
                .map(hook -> (MapHook) hook)
                .collect(Collectors.toSet());
    }

    default void addMappedClaim(@NotNull Claim claim, @NotNull ClaimWorld claimWorld) {
        getMapHooks().forEach(hook -> hook.markClaim(claim, claimWorld));
    }

    default void removeMappedClaim(@NotNull Claim claim, @NotNull ClaimWorld claimWorld) {
        getMapHooks().forEach(hook -> hook.unMarkClaim(claim, claimWorld));
    }

    default void removeAllMappedClaims(@Nullable UUID owner) {
        getMapHooks().forEach(hook -> hook.unMarkClaimsBy(owner));
    }

    default void removeAllMappedAdminClaims() {
        getMapHooks().forEach(MapHook::unMarkAdminClaims);
    }

    default void clearAllMapMarkers() {
        getMapHooks().forEach(MapHook::unMarkAllClaims);
    }

    @NotNull
    Set<Hook> getHooks();

}
