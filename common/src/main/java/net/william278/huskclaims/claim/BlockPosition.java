package net.william278.huskclaims.claim;

import com.google.gson.annotations.Expose;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class BlockPosition {

    @Expose
    int x;
    @Expose
    int z;

    private BlockPosition() {
    }

    private BlockPosition(int x, int z) {
        this.x = x;
        this.z = z;
    }

    @NotNull
    public static BlockPosition at(int x, int z) {
        return new BlockPosition(x, z);
    }

    public static List<BlockPosition> getSortedCorners(BlockPosition pos1, BlockPosition pos2) {
        return List.of(
                BlockPosition.at(Math.min(pos1.x, pos2.x), Math.min(pos1.z, pos2.z)),
                BlockPosition.at(Math.max(pos1.x, pos2.x), Math.min(pos1.z, pos2.z)),
                BlockPosition.at(Math.min(pos1.x, pos2.x), Math.max(pos1.z, pos2.z)),
                BlockPosition.at(Math.max(pos1.x, pos2.x), Math.max(pos1.z, pos2.z))
        );
    }

    public int getSurfaceArea(@NotNull BlockPosition other) {
        return Math.abs(x - other.x) * Math.abs(z - other.z);
    }

    public int getX() {
        return x;
    }

    public int getZ() {
        return z;
    }
}
