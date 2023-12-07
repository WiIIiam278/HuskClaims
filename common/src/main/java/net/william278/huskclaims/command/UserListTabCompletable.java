package net.william278.huskclaims.command;

import net.william278.huskclaims.HuskClaims;
import net.william278.huskclaims.user.CommandUser;
import net.william278.huskclaims.user.User;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public interface UserListTabCompletable extends TabCompletable {

    @Override
    @Nullable
    default List<String> suggest(@NotNull CommandUser user, @NotNull String[] args) {
        return args.length < 2 ? getPlugin().getUserList().stream().map(User::getUsername).toList() : null;
    }

    @NotNull
    HuskClaims getPlugin();

}
