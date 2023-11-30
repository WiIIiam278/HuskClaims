package net.william278.huskclaims.group;

import net.william278.huskclaims.user.User;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.UUID;

public record UserGroup(
        @NotNull UUID groupOwner,
        @NotNull String name,
        @NotNull List<User> members
) {
}
