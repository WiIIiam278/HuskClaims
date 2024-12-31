package net.william278.huskclaims.pet;

import net.william278.huskclaims.user.OnlineUser;
import net.william278.huskclaims.user.User;
import org.jetbrains.annotations.NotNull;

public interface FabricPetHandler extends PetHandler {

    @Override
    default void userTransferPet(@NotNull OnlineUser user, @NotNull User newOwner, boolean forceTransfer) {

    }

}
