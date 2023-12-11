/*
 * This file is part of HuskClaims, licensed under the Apache License 2.0.
 *
 *  Copyright (c) William278 <will27528@gmail.com>
 *  Copyright (c) contributors
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package net.william278.huskclaims.command;

import net.william278.huskclaims.HuskClaims;
import net.william278.huskclaims.group.UserGroup;
import net.william278.huskclaims.user.CommandUser;
import net.william278.huskclaims.user.OnlineUser;
import net.william278.huskclaims.user.SavedUser;
import net.william278.huskclaims.user.User;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.function.Consumer;

public class UserGroupsCommand extends Command {

    protected UserGroupsCommand(@NotNull HuskClaims plugin) {
        super(List.of("group", "usergroup"), "<create|delete|player> [name] [args]", plugin);
    }

    @Override
    public void execute(@NotNull CommandUser executor, @NotNull String[] args) {
        final Optional<String> operation = parseStringArg(args, 0);
        final Optional<String> name = parseStringArg(args, 1);
        if (operation.isEmpty() || name.isEmpty()) {
            plugin.getLocales().getLocale("error_invalid_syntax", getUsage())
                    .ifPresent(executor::sendMessage);
            return;
        }

        final OnlineUser user = (OnlineUser) executor;
        switch (operation.get().toLowerCase(Locale.ENGLISH)) {
            case "create" -> createGroup(user, name.get());
            case "delete" -> deleteGroup(user, name.get());
            case "player" -> {
                final Optional<String> action = parseStringArg(args, 2);
                final Optional<String> player = parseStringArg(args, 3);
                if (player.isEmpty() || action.isEmpty()) {
                    plugin.getLocales().getLocale("error_invalid_syntax",
                                    "/group player [group] <add|remove> [username]")
                            .ifPresent(user::sendMessage);
                    return;
                }
                editGroupPlayers(user, name.get(), action.get(), player.get());
            }
        }
    }

    private void editGroupPlayers(@NotNull OnlineUser user, @NotNull String name,
                                  @NotNull String action, @NotNull String player) {
        switch (action.toLowerCase(Locale.ENGLISH)) {
            case "add" -> addPlayerToGroup(user, name, player);
            case "remove" -> removePlayerFromGroup(user, name, player);
            default -> plugin.getLocales().getLocale("error_invalid_syntax",
                            "/group player [group] <add|remove> [username]")
                    .ifPresent(user::sendMessage);
        }
    }

    private void createGroup(@NotNull OnlineUser user, @NotNull String name) {
        if (plugin.getUserGroup(user.getUuid(), name).isPresent()) {
            plugin.getLocales().getLocale("error_group_already_exists", name)
                    .ifPresent(user::sendMessage);
            return;
        }
        plugin.createUserGroup(user, name);
        plugin.getLocales().getLocale("group_created", name)
                .ifPresent(user::sendMessage);
    }

    private void deleteGroup(@NotNull OnlineUser user, @NotNull String name) {
        if (!plugin.deleteUserGroup(user, name)) {
            plugin.getLocales().getLocale("error_invalid_group")
                    .ifPresent(user::sendMessage);
            return;
        }
        plugin.getLocales().getLocale("group_deleted", name)
                .ifPresent(user::sendMessage);
    }

    private void addPlayerToGroup(@NotNull OnlineUser user, @NotNull String name, @NotNull String player) {
        getUserIfExists(user, player, (toAdd) -> editGroupIfExists(user, name, (group) -> {
            if (group.isMember(toAdd.getUuid())) {
                plugin.getLocales().getLocale("error_already_group_member", toAdd.getName())
                        .ifPresent(user::sendMessage);
                return;
            }
            group.members().add(toAdd);
            plugin.getLocales().getLocale("group_added_player", toAdd.getName(), group.name())
                    .ifPresent(user::sendMessage);
        }));
    }

    private void removePlayerFromGroup(@NotNull OnlineUser user, @NotNull String name, @NotNull String player) {
        getUserIfExists(user, player, (toRemove) -> editGroupIfExists(user, name, (group) -> {
            if (!group.isMember(toRemove.getUuid())) {
                plugin.getLocales().getLocale("error_not_group_member", toRemove.getName())
                        .ifPresent(user::sendMessage);
                return;
            }
            group.members().remove(toRemove);
            plugin.getLocales().getLocale("group_removed_player", toRemove.getName(), group.name())
                    .ifPresent(user::sendMessage);
        }));
    }

    private void getUserIfExists(@NotNull OnlineUser user, @NotNull String username,
                                 @NotNull Consumer<User> consumer) {
        plugin.getDatabase().getUser(username).map(SavedUser::user).ifPresentOrElse(
                consumer, () -> plugin.getLocales().getLocale("error_invalid_player")
                        .ifPresent(user::sendMessage)
        );
    }

    private void editGroupIfExists(@NotNull OnlineUser user, @NotNull String name,
                                   @NotNull Consumer<UserGroup> consumer) {
        plugin.editUserGroup(
                user, name, consumer,
                () -> plugin.getLocales().getLocale("error_invalid_group")
                        .ifPresent(user::sendMessage)
        );
    }

}
