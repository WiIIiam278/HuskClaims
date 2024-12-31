package net.william278.huskclaims.user;

import com.google.common.collect.Lists;
import me.lucko.fabric.api.permissions.v0.Permissions;
import net.minecraft.entity.Entity;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import net.william278.cloplib.listener.InspectorCallbackProvider;
import net.william278.huskclaims.FabricHuskClaims;
import net.william278.huskclaims.HuskClaims;
import net.william278.huskclaims.hook.HuskHomesHook;
import net.william278.huskclaims.position.Position;
import net.william278.huskclaims.util.Location;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;

public class FabricUser extends OnlineUser {

    private final ServerPlayerEntity fabricPlayer;

    private FabricUser(@NotNull ServerPlayerEntity fabricPlayer, @NotNull HuskClaims plugin) {
        super(fabricPlayer.getGameProfile().getName(), fabricPlayer.getUuid(), plugin);
        this.fabricPlayer = fabricPlayer;
    }

    @NotNull
    @ApiStatus.Internal
    public static FabricUser adapt(@NotNull ServerPlayerEntity player, @NotNull HuskClaims plugin) {
        return new FabricUser(player, plugin);
    }

    @NotNull
    @Override
    public Position getPosition() {
        return FabricHuskClaims.Adapter.adapt(new Location(fabricPlayer));
    }

    @Override
    public void sendPluginMessage(@NotNull String channel, byte[] message) {
//        fabricPlayer.networkHandler.sendPacket(new CustomPayloadS2CPacket(new FabricPluginMessage(message)));
    }

    @Override
    public boolean hasPermission(@NotNull String node) {
        boolean op = Boolean.TRUE.equals(((FabricHuskClaims) plugin).getPermissions().getOrDefault(node, true));
        return Permissions.check(fabricPlayer, node, op);
    }

    @Override
    public boolean hasPermission(@NotNull String node, boolean isDefault) {
        boolean op = Boolean.TRUE.equals(((FabricHuskClaims) plugin).getPermissions().getOrDefault(node, true));
        return Permissions.check(fabricPlayer, node, isDefault || (op || !fabricPlayer.hasPermissionLevel(3)));
    }

    @Override
    public boolean isHolding(@NotNull InspectorCallbackProvider.InspectionTool tool) {
        final Predicate<ItemStack> pred = (i) -> i.getRegistryEntry().getIdAsString().equals(tool.material());
        return pred.test(fabricPlayer.getMainHandStack()); // todo custom model data, etc
    }

    @Override
    @NotNull
    public Optional<Long> getNumericalPermission(@NotNull String prefix) {
        final List<Long> permissions = Lists.newArrayList();
        for (long l = 0; l < 100; l++) {
            if (hasPermission(prefix + l)) {
                permissions.add(l);
            }
        }
        return permissions.stream().sorted().findFirst();
    }

    @Override
    public boolean isSneaking() {
        return fabricPlayer.isSneaking();
    }

    @Override
    public void teleport(@NotNull Position position, boolean instant) {
        if (instant) {
            teleportInstant(position);
            return;
        }
        plugin.getHook(HuskHomesHook.class).ifPresentOrElse(
                homes -> homes.teleport(this, position, plugin.getServerName()),
                () -> teleportInstant(position)
        );
    }

    private void teleportInstant(@NotNull Position position) {
        fabricPlayer.stopRiding();
        fabricPlayer.getPassengerList().forEach(Entity::stopRiding);
        fabricPlayer.teleportTo(FabricHuskClaims.Adapter.adapt(
                position, ((FabricHuskClaims) plugin).getMinecraftServer()
        ).teleportTarget());
    }

    @NotNull
    public ServerPlayerEntity getFabricPlayer() {
        return fabricPlayer;
    }

}
