package net.william278.huskclaims.position;

import com.google.gson.annotations.Expose;
import net.william278.cloplib.operation.OperationChunk;
import net.william278.cloplib.operation.OperationPosition;
import net.william278.cloplib.operation.OperationWorld;
import org.jetbrains.annotations.NotNull;

public class Position implements CoordinatePoint, OperationPosition {

    @Expose
    private double x;
    @Expose
    private double y;
    @Expose
    private double z;
    @Expose
    private float yaw;
    @Expose
    private float pitch;
    @Expose
    private World world;

    private Position(double x, double y, double z, float yaw, float pitch, @NotNull World world) {
        this.x = x;
        this.y = y;
        this.z = z;
        this.yaw = yaw;
        this.pitch = pitch;
        this.world = world;
    }

    @SuppressWarnings("unused")
    private Position() {
    }

    @NotNull
    public static Position at(double x, double y, double z, @NotNull World world) {
        return new Position(x, y, z, 0f, 0f, world);
    }

    @NotNull
    public static Position at(double x, double y, double z, float yaw, float pitch, @NotNull World world) {
        return new Position(x, y, z, yaw, pitch, world);
    }

    @Override
    public double getX() {
        return x;
    }

    @Override
    public double getY() {
        return y;
    }

    @Override
    public double getZ() {
        return z;
    }

    @Override
    public float getYaw() {
        return yaw;
    }

    @Override
    public float getPitch() {
        return pitch;
    }

    @NotNull
    @Override
    public OperationWorld getWorld() {
        return world;
    }

    @NotNull
    @Override
    public OperationChunk getChunk() {
        return new OperationChunk() {
            @Override
            public int getX() {
                return ((int) x) << 16;
            }

            @Override
            public int getZ() {
                return ((int) z) << 16;
            }
        };
    }

    @Override
    public int getBlockX() {
        return (int) getX();
    }

    @Override
    public int getBlockZ() {
        return (int) getZ();
    }
}
