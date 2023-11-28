package net.william278.huskclaims.claim;

import com.google.common.collect.Maps;
import com.google.common.collect.Queues;
import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;
import net.william278.cloplib.operation.OperationType;
import net.william278.huskclaims.HuskClaims;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ConcurrentMap;

public class Claim {

    // Claim corners - closest to the world boundary, and furthest from the world boundary
    @Expose
    @SerializedName("near_corner")
    private BlockPosition nearCorner;

    @Expose
    @SerializedName("far_corner")
    private BlockPosition farCorner;

    // Map of TrustLevels to a list of UUID players with that TrustLevel
    @Expose
    private ConcurrentMap<UUID, String> trustees;

    // List of child claims
    @Expose
    private ConcurrentLinkedQueue<Claim> children;

    // List of OperationTypes allowed on this claim to everyone
    @Expose
    @SerializedName("base_operation_flags")
    private ConcurrentLinkedQueue<OperationType> baseClaimFlags;

    // If this is a subdivision, whether to inherit member trust levels from the parent
    @Expose
    @SerializedName("inherit_from_parent")
    private boolean inheritFromParent;

    private Claim(@NotNull BlockPosition corner1, @NotNull BlockPosition corner2,
                  @NotNull ConcurrentMap<UUID, String> trustees, @NotNull ConcurrentLinkedQueue<Claim> children,
                  @NotNull ConcurrentLinkedQueue<OperationType> baseClaimFlags, boolean inheritFromParent) {
        final List<BlockPosition> corners = BlockPosition.getSortedCorners(corner1, corner2);
        this.nearCorner = corners.get(0);
        this.farCorner = corners.get(3);
        this.trustees = trustees;
        this.children = children;
        this.baseClaimFlags = baseClaimFlags;
        this.inheritFromParent = inheritFromParent;
    }

    private Claim(@NotNull BlockPosition corner1, @NotNull BlockPosition corner2, @NotNull HuskClaims plugin) {
        this(
                corner1, corner2,
                Maps.newConcurrentMap(),
                Queues.newConcurrentLinkedQueue(),
                Queues.newConcurrentLinkedQueue(),
                false
        );
    }

    @SuppressWarnings("unused")
    private Claim() {
    }

    @NotNull
    public BlockPosition getNearCorner() {
        return nearCorner;
    }

    @NotNull
    public BlockPosition getFarCorner() {
        return farCorner;
    }

    @NotNull
    public ConcurrentMap<UUID, String> getTrustees() {
        return trustees;
    }

    @NotNull
    public Optional<TrustLevel> getUserTrustLevel(@NotNull UUID uuid, @NotNull ClaimWorld world,
                                                  @NotNull HuskClaims plugin) {
        return Optional.ofNullable(trustees.get(uuid))
                .flatMap(plugin::getTrustLevel)
                .or(() -> getParent(world).flatMap(parent -> parent.getUserTrustLevel(uuid, world, plugin)));
    }

    public Optional<Claim> getParent(@NotNull ClaimWorld world) {
        return world.getClaims().stream()
                .filter(claim -> claim.getChildren().contains(this))
                .findFirst();
    }

    @NotNull
    public ConcurrentLinkedQueue<Claim> getChildren() {
        return children;
    }

    public int getSurfaceArea() {
        return nearCorner.getSurfaceArea(farCorner);
    }

    public Claim createAndAddChild(@NotNull BlockPosition corner1, @NotNull BlockPosition corner2,
                                   @NotNull HuskClaims plugin) throws IllegalArgumentException {
        if (!contains(corner1) || !contains(corner2)) {
            throw new IllegalArgumentException("Child claim must be contained within parent claim");
        }
        final Claim child = new Claim(corner1, corner2, plugin);
        children.add(child);
        return child;
    }

    public boolean contains(@NotNull BlockPosition position) {
        return position.getX() >= nearCorner.getX() && position.getX() <= farCorner.getX()
                && position.getZ() >= nearCorner.getZ() && position.getZ() <= farCorner.getZ();
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof Claim claim) {
            return claim.nearCorner.equals(nearCorner) && claim.farCorner.equals(farCorner);
        }
        return false;
    }
}
