package net.william278.huskclaims.util;

import net.minecraft.entity.Entity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.TeleportTarget;
import org.jetbrains.annotations.NotNull;

public record Location(@NotNull ServerWorld world, @NotNull Vec3d pos, float yaw, float pitch) {

    public Location(@NotNull Entity entity) {
        this((ServerWorld) entity.getWorld(), entity.getPos(), entity.getYaw(), entity.getPitch());
    }

    public Location(@NotNull ServerWorld world, @NotNull BlockPos pos) {
        this(world, pos.toCenterPos(), 0.0f, 0.0f);
    }

    public Location(@NotNull ServerWorld world, double x, double y, double z, float yaw, float pitch) {
        this(world, new Vec3d(x, y, z), yaw, pitch);
    }

    @NotNull
    public BlockPos blockPos() {
        return BlockPos.ofFloored(pos);
    }

    public TeleportTarget teleportTarget() {
        return new TeleportTarget(world, pos, Vec3d.ZERO, yaw, pitch, TeleportTarget.NO_OP);
    }

}
