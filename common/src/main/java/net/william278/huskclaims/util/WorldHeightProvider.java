package net.william278.huskclaims.util;

import net.william278.huskclaims.position.BlockPosition;
import net.william278.huskclaims.position.World;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public interface WorldHeightProvider {

    /**
     * Get a list of the highest block Y at a list of positions
     *
     * @param positions The positions to get the highest block Y at
     * @param world     The world to get the highest block Y at
     * @return A list of the highest block Y at the positions
     * @since 1.0
     */
    @NotNull
    List<Integer> getHighestBlockYAt(@NotNull List<BlockPosition> positions, @NotNull World world);

}
