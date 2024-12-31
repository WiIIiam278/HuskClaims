package net.william278.huskclaims.user;

import net.minecraft.server.network.ServerPlayerEntity;
import net.william278.huskclaims.FabricHuskClaims;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

public interface FabricUserProvider extends UserProvider {

    @Override
    @NotNull
    default OnlineUser getOnlineUser(@NotNull UUID uuid) {
        return getOnlineUser(getPlugin().getMinecraftServer().getPlayerManager().getPlayer(uuid));
    }

    @NotNull
    default FabricUser getOnlineUser(@Nullable ServerPlayerEntity player) {
        if (player == null) {
            throw new IllegalArgumentException("Player is not online");
        }
        FabricUser user = (FabricUser) getOnlineUserMap().get(player.getUuid());
        if (user == null || user.getFabricPlayer().isRemoved() || user.getFabricPlayer().isDisconnected()) {
            user = FabricUser.adapt(player, getPlugin());
            getOnlineUserMap().put(player.getUuid(), user);
            return user;
        }
        return user;
    }

    @NotNull
    FabricHuskClaims getPlugin();

}
