package net.william278.huskclaims.claim;

import com.google.gson.annotations.Expose;
import net.william278.cloplib.operation.OperationPosition;
import net.william278.huskclaims.position.CoordinatePoint;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class ClaimCorner implements CoordinatePoint {

    @Expose
    int x;
    @Expose
    int z;

    @SuppressWarnings("unused")
    private ClaimCorner() {
    }

    private ClaimCorner(int x, int z) {
        this.x = x;
        this.z = z;
    }

    @NotNull
    public static ClaimCorner at(int x, int z) {
        return new ClaimCorner(x, z);
    }

    @NotNull
    public static ClaimCorner wrap(@NotNull OperationPosition position) {
        return at((int) position.getX(), (int) position.getZ());
    }

    @Override
    public int getBlockX() {
        return x;
    }

    @Override
    public int getBlockZ() {
        return z;
    }
}
