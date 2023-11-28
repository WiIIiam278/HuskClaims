package net.william278.huskclaims.claim;

import com.google.common.collect.Maps;
import com.google.common.collect.Queues;
import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;
import net.william278.cloplib.operation.Operation;
import net.william278.cloplib.operation.OperationType;
import net.william278.huskclaims.HuskClaims;
import net.william278.huskclaims.position.CoordinatePoint;
import net.william278.huskclaims.user.User;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ConcurrentMap;

public class ClaimWorld {

    @Expose
    private ConcurrentLinkedQueue<Claim> claims;
    @Expose
    @SerializedName("user_cache")
    private ConcurrentMap<UUID, String> userCache;
    @Expose
    @SerializedName("wilderness_flags")
    private List<OperationType> wildernessFlags;

    private ClaimWorld(@NotNull HuskClaims plugin) {
        this.claims = Queues.newConcurrentLinkedQueue();
        this.userCache = Maps.newConcurrentMap();
        this.wildernessFlags = new ArrayList<>();
    }

    @SuppressWarnings("unused")
    private ClaimWorld() {
    }

    public Optional<User> getUser(@NotNull UUID uuid) {
        return Optional.ofNullable(userCache.get(uuid)).map(name -> User.of(name, uuid));
    }

    @NotNull
    public ConcurrentLinkedQueue<Claim> getClaims() {
        return claims;
    }

    public Optional<Claim> getParentClaimAt(@NotNull CoordinatePoint position) {
        return getClaims().stream().filter(claim -> claim.getRegion().contains(position)).findFirst();
    }

    public Optional<Claim> getClaimAt(@NotNull CoordinatePoint position) {
        return getParentClaimAt(position).map(parent -> parent.getChildren().stream()
                .filter(c -> c.getRegion().contains(position)).findFirst()
                .orElse(parent));
    }

    @NotNull
    public List<Claim> getParentClaimsWithin(@NotNull SquareRegion region) {
        return getClaims().stream().filter(claim -> claim.getRegion().overlaps(region)).toList();
    }

    public boolean isRegionClaimed(@NotNull SquareRegion region) {
        return !getParentClaimsWithin(region).isEmpty();
    }

    public boolean isOperationAllowed(@NotNull Operation operation, @NotNull HuskClaims plugin) {
        return getClaimAt(ClaimCorner.wrap(operation.getOperationPosition()))
                .map(claim -> claim.isOperationAllowed(operation, this, plugin))
                .orElse(wildernessFlags.contains(operation.getType()));
    }

    @NotNull
    public ConcurrentMap<UUID, String> getUserCache() {
        return userCache;
    }

    @NotNull
    public List<OperationType> getWildernessFlags() {
        return wildernessFlags;
    }

}
