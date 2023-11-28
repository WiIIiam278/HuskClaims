package net.william278.huskclaims.claim;

import com.google.common.collect.Maps;
import com.google.common.collect.Queues;
import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;
import net.william278.cloplib.operation.Operation;
import net.william278.cloplib.operation.OperationType;
import net.william278.huskclaims.HuskClaims;
import net.william278.huskclaims.user.User;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ConcurrentMap;

public class Claim {

    // The claim region
    @Expose
    private SquareRegion region;

    // Map of TrustLevels to a list of UUID players with that TrustLevel
    @Expose
    private ConcurrentMap<UUID, String> trustees;

    // List of child claims
    @Expose
    private ConcurrentLinkedQueue<Claim> children;

    // List of OperationTypes allowed on this claim to everyone
    @Expose
    @SerializedName("universal_flags")
    private List<OperationType> universalFlags;

    // If this is a child claim, whether to inherit member trust levels from the parent.
    // If set to false, this is a restricted child claim.
    @Expose
    @SerializedName("inherit_parent")
    private boolean inheritParent;

    private Claim(@NotNull SquareRegion region, @NotNull ConcurrentMap<UUID, String> trustees,
                  @NotNull ConcurrentLinkedQueue<Claim> children, @NotNull List<OperationType> universalFlags,
                  boolean inheritParent) {
        this.region = region;
        this.trustees = trustees;
        this.children = children;
        this.universalFlags = universalFlags;
        this.inheritParent = inheritParent;
    }

    private Claim(@NotNull SquareRegion region, @NotNull HuskClaims plugin) {
        this(
                region,
                Maps.newConcurrentMap(),
                Queues.newConcurrentLinkedQueue(),
                new ArrayList<>(),
                true
        );
    }

    @SuppressWarnings("unused")
    private Claim() {
    }

    @NotNull
    public SquareRegion getRegion() {
        return region;
    }

    @NotNull
    public ConcurrentMap<UUID, String> getTrustees() {
        return trustees;
    }

    @NotNull
    public Optional<TrustLevel> getEffectiveTrustLevel(@NotNull User user, @NotNull ClaimWorld world,
                                                       @NotNull HuskClaims plugin) {
        return Optional.ofNullable(trustees.get(user.getUuid()))
                .flatMap(plugin::getTrustLevel)
                .or(() -> inheritParent
                        ? getParent(world).flatMap(parent -> parent.getEffectiveTrustLevel(user, world, plugin))
                        : Optional.empty());
    }

    public boolean isPrivilegeAllowed(@NotNull TrustLevel.Privilege privilege, @NotNull User user,
                                      @NotNull ClaimWorld world, @NotNull HuskClaims plugin) {
        return getEffectiveTrustLevel(user, world, plugin)
                .map(level -> level.getPrivileges().contains(privilege))
                .orElse(false);
    }

    public boolean isOperationAllowed(@NotNull Operation operation, @NotNull ClaimWorld world,
                                      @NotNull HuskClaims plugin) {
        // If the operation is explicitly allowed, return it
        return universalFlags.contains(operation.getType()) || operation.getUser()

                // Or, if there's a user involved in this operation, check their rights
                .map(user -> trustees.get(user.getUuid())).flatMap(plugin::getTrustLevel)
                .map(level -> level.getFlags().contains(operation.getType()))

                // If the user doesn't have a trust level here, try getting it from the parent
                .orElseGet(() -> inheritParent && getParent(world)
                        .map(parent -> parent.isOperationAllowed(operation, world, plugin))
                        .orElse(false));

    }

    public Optional<Claim> getParent(@NotNull ClaimWorld world) {
        return world.getClaims().stream()
                .filter(claim -> claim.getChildren().contains(this))
                .findFirst();
    }

    public boolean isChildClaim(@NotNull ClaimWorld world) {
        return getParent(world).isPresent();
    }

    @NotNull
    public ConcurrentLinkedQueue<Claim> getChildren() {
        return children;
    }

    @NotNull
    public Claim createAndAddChild(@NotNull SquareRegion subRegion, @NotNull ClaimWorld world, @NotNull HuskClaims plugin)
            throws IllegalArgumentException {
        if (isChildClaim(world)) {
            throw new IllegalArgumentException("A child claim cannot be within another child claim");
        }
        if (!region.contains(subRegion.getNearCorner()) || !region.contains(subRegion.getFarCorner())) {
            throw new IllegalArgumentException("Child claim must be contained within parent claim");
        }
        final Claim child = new Claim(region, plugin);
        children.add(child);
        return child;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof Claim claim) {
            return claim.region.equals(region)
                    && claim.children.equals(children);
        }
        return false;
    }
}
