package net.william278.huskclaims.claim;

import net.william278.huskclaims.position.World;
import org.jetbrains.annotations.NotNull;

import java.util.Map;
import java.util.Optional;

/**
 * Interface for managing {@link ClaimWorld}s
 */
public interface ClaimManager {

    /**
     * Get a trust level by name
     *
     * @return the trust level, if found
     * @since 1.0
     */
    @NotNull
    Map<World, ClaimWorld> getClaimWorlds();

    /**
     * Get a claim world by world
     *
     * @param world The world to get the claim world for
     * @return the claim world, if found
     * @since 1.0
     */
    default Optional<ClaimWorld> getClaimWorld(@NotNull World world) {
        return Optional.ofNullable(getClaimWorlds().get(world));
    }

    /**
     * Load the claim worlds from the database
     *
     * @since 1.0
     */
    default void loadClaimWorlds() {
        //todo load from DB
    }

}
