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
import net.william278.huskclaims.config.Locales;
import net.william278.huskclaims.group.UserGroup;
import net.william278.huskclaims.user.CommandUser;
import net.william278.huskclaims.user.OnlineUser;
import net.william278.huskclaims.user.SavedUser;
import net.william278.huskclaims.user.User;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.stream.Collectors;

public class UserGroupsCommand extends Command implements TabCompletable {

    protected UserGroupsCommand(@NotNull HuskClaims plugin) {
        super(List.of("group", "usergroup"), "<create|delete|edit> [name] [args]", plugin);
    }

    @Override
    public void execute(@NotNull CommandUser executor, @NotNull String[] args) {
        final OnlineUser user = (OnlineUser) executor;
        final Optional<String> operation = parseStringArg(args, 0);
        final Optional<String> name = parseStringArg(args, 1);
        if (operation.isEmpty() || name.isEmpty()) {
            showGroupList(user);
            return;
        }

        switch (operation.get().toLowerCase(Locale.ENGLISH)) {
            case "create" -> createGroup(user, name.get());
            case "delete" -> deleteGroup(user, name.get());
            case "edit" -> {
                final Optional<String> action = parseStringArg(args, 2);
                final Optional<String> player = parseStringArg(args, 3);
                if (player.isEmpty() || action.isEmpty()) {
                    showGroupMemberList(user, name.get());
                    return;
                }
                editGroupPlayers(user, name.get(), action.get(), player.get());
            }
        }
    }

    private void showGroupList(@NotNull OnlineUser user) {
        final List<UserGroup> groups = plugin.getUserGroups(user.getUuid());
        plugin.getLocales().getRawLocale(
                        "group_list",
                        groups.isEmpty()
                                ? plugin.getLocales().getNone()
                                : groups.stream().map(group -> getGroupEntry(group, plugin))
                                .collect(Collectors.joining(plugin.getLocales().getListJoiner()))
                )
                .map(l -> plugin.getLocales().format(l))
                .ifPresent(user::sendMessage);
    }

    @NotNull
    private String getGroupEntry(@NotNull UserGroup group, @NotNull HuskClaims plugin) {
        return plugin.getLocales().getRawLocale("group_list_item",
                Locales.escapeText(group.name()),
                Integer.toString(group.members().size()),
                group.members().stream()
                        .map(user -> Locales.escapeText(user.getName()))
                        .collect(Collectors.joining("\n"))
        ).orElse(group.name());
    }

    private void editGroupPlayers(@NotNull OnlineUser user, @NotNull String name,
                                  @NotNull String action, @NotNull String player) {
        switch (action.toLowerCase(Locale.ENGLISH)) {
            case "add" -> addPlayerToGroup(user, name, player);
            case "remove" -> removePlayerFromGroup(user, name, player);
            default -> showGroupMemberList(user, name);
        }
    }

    private void createGroup(@NotNull OnlineUser user, @NotNull String name) {
        if (plugin.getUserGroup(user.getUuid(), name).isPresent()) {
            plugin.getLocales().getLocale("error_group_already_exists", name)
                    .ifPresent(user::sendMessage);
            return;
        }
        if (!getPlugin().isValidGroupName(name)) {
            plugin.getLocales().getLocale("error_invalid_group_name")
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

    private void showGroupMemberList(@NotNull OnlineUser user, @NotNull String name) {
        editGroupIfExists(
                user, name,
                (group) -> plugin.getLocales().getRawLocale(
                        "group_member_list",
                        Locales.escapeText(group.name()),
                        group.members().isEmpty()
                                ? plugin.getLocales().getNone()
                                : group.members().stream().map(User::getName).map(Locales::escapeText)
                                .collect(Collectors.joining(plugin.getLocales().getListJoiner()))
                ).map(l -> plugin.getLocales().format(l)).ifPresent(user::sendMessage)
        );
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

    @Nullable
    @Override
    public List<String> suggest(@NotNull CommandUser user, @NotNull String[] args) {
        return switch (args.length) {
            case 0, 1 -> List.of("create", "delete", "edit", "list");
            case 2 -> List.of("edit", "delete").contains(args[0].toLowerCase(Locale.ENGLISH))
                    ? user instanceof OnlineUser online ? getUserGroupNames(online.getUuid()) : null
                    : null;
            case 3 -> args[0].equals(args[0].toLowerCase(Locale.ENGLISH))
                    ? List.of("add", "remove", "list")
                    : null;
            case 4 -> args[0].equals("edit")
                    ? user instanceof OnlineUser online ? (args[2].equals("remove")
                    ? plugin.getUserGroup(online.getUuid(), args[1]).map(UserGroup::members).orElse(List.of())
                    : plugin.getUserList()).stream().map(User::getName).toList() : null
                    : null;
            default -> null;
        };
    }

    @NotNull
    private List<String> getUserGroupNames(@NotNull UUID uuid) {
        return plugin.getUserGroups(uuid).stream()
                .map(UserGroup::name)
                .collect(Collectors.toList());
    }

}
