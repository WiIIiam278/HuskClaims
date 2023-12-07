package net.william278.huskclaims.command;

import net.william278.huskclaims.user.CommandUser;
import net.william278.huskclaims.user.OnlineUser;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.UUID;

public interface TrustableTabCompletable extends UserListTabCompletable {

    @Nullable
    @Override
    default List<String> suggest(@NotNull CommandUser user, @NotNull String[] args) {
        final OnlineUser onlineUser = (OnlineUser) user;
        if (args.length > 1) {
            return null;
        }

        // Suggest group names
        if (getPlugin().getSettings().getUserGroups().isEnabled() && getGroupOwner(onlineUser) != null) {
            final String prefix = getPlugin().getSettings().getUserGroups().getGroupSpecifierPrefix();
            if (args[0].startsWith(prefix)) {
                return getPlugin().getUserGroups().stream()
                        .filter(group -> group.groupOwner().equals(getGroupOwner(onlineUser)))
                        .map(group -> prefix + group.name())
                        .toList();
            }
        }

        // Suggest usernames
        return UserListTabCompletable.super.suggest(user, args);
    }

    @Nullable
    UUID getGroupOwner(@NotNull OnlineUser user);


}
