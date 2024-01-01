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

import com.google.common.collect.Lists;
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
import java.util.function.BiConsumer;
import java.util.stream.Collectors;

public class UserGroupsCommand extends OnlineUserCommand implements TabCompletable {

    protected UserGroupsCommand(@NotNull HuskClaims plugin) {
        super(
                List.of("group", "usergroup"),
                "[name] [<add|remove> <user(s)>|delete] ",
                plugin
        );
    }

    @Override
    public void execute(@NotNull OnlineUser executor, @NotNull String[] args) {
        final Optional<String> groupName = parseStringArg(args, 0);
        if (groupName.isEmpty()) {
            showGroupList(executor);
            return;
        }

        final String operation = parseStringArg(args, 1).orElse("list");
        switch (operation.toLowerCase(Locale.ENGLISH)) {
            case "add" -> addGroupUsers(executor, groupName.get(), parseDistinctNameList(args, 2));
            case "remove" -> removeGroupUsers(executor, groupName.get(), parseDistinctNameList(args, 2));
            case "list" -> listGroupUsers(executor, groupName.get());
            case "delete" -> deleteGroup(executor, groupName.get());
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

    private void deleteGroup(@NotNull OnlineUser user, @NotNull String name) {
        if (!plugin.deleteUserGroup(user, name)) {
            plugin.getLocales().getLocale("error_invalid_group", name)
                    .ifPresent(user::sendMessage);
            return;
        }
        plugin.getLocales().getLocale("group_deleted", name)
                .ifPresent(user::sendMessage);
    }

    private void listGroupUsers(@NotNull OnlineUser user, @NotNull String name) {
        plugin.getUserGroup(user.getUuid(), name).ifPresentOrElse(
                (group) -> plugin.getLocales().getRawLocale(
                        "group_member_list",
                        Locales.escapeText(group.name()),
                        group.members().isEmpty()
                                ? plugin.getLocales().getNone()
                                : group.members().stream().map(User::getName).map(Locales::escapeText)
                                .collect(Collectors.joining(plugin.getLocales().getListJoiner()))
                ).map(l -> plugin.getLocales().format(l)).ifPresent(user::sendMessage),
                () -> plugin.getLocales().getLocale("error_invalid_group", name)
                        .ifPresent(user::sendMessage)
        );
    }

    private void addGroupUsers(@NotNull OnlineUser executor, @NotNull String name, @NotNull List<String> users) {
        if (!getPlugin().isValidGroupName(name)) {
            plugin.getLocales().getLocale("error_invalid_group_name", name)
                    .ifPresent(executor::sendMessage);
            return;
        }

        editUserGroup(
                executor, name, users,
                (group, user) -> {
                    if (group.isMember(user.getUuid())) {
                        plugin.getLocales().getLocale("error_already_group_member", user.getName())
                                .ifPresent(executor::sendMessage);
                        return;
                    }
                    group.members().add(user);
                    plugin.getLocales().getLocale("group_added_player", user.getName(), group.name())
                            .ifPresent(executor::sendMessage);
                }, () -> {
                    final List<User> names = resolveUsers(executor, users);
                    if (names.isEmpty()) {
                        return;
                    }
                    plugin.createUserGroup(executor, name, Lists.newArrayList(names));
                    plugin.getLocales().getLocale("group_created", name,
                            users.stream().collect(
                                    Collectors.joining(plugin.getLocales().getListJoiner())
                            )).ifPresent(executor::sendMessage);
                }
        );
    }

    private void removeGroupUsers(@NotNull OnlineUser executor, @NotNull String name, @NotNull List<String> users) {
        editUserGroup(
                executor, name, users,
                (group, user) -> {
                    if (!group.isMember(user.getUuid())) {
                        plugin.getLocales().getLocale("error_not_group_member", user.getName())
                                .ifPresent(executor::sendMessage);
                        return;
                    }
                    group.members().remove(user);
                    plugin.getLocales().getLocale("group_removed_player", user.getName(), group.name())
                            .ifPresent(executor::sendMessage);
                }, () -> plugin.getLocales().getLocale("error_invalid_group", name)
                        .ifPresent(executor::sendMessage)
        );
    }

    private void editUserGroup(@NotNull OnlineUser executor, @NotNull String groupName, @NotNull List<String> users,
                               @NotNull BiConsumer<UserGroup, User> editor, @NotNull Runnable noGroup) {
        plugin.editUserGroup(
                executor,
                groupName,
                (group) -> resolveUsers(executor, users).forEach(user -> editor.accept(group, user)),
                noGroup
        );
    }

    @NotNull
    private List<User> resolveUsers(@NotNull OnlineUser user, @NotNull List<String> users) {
        return users.stream().map(username -> plugin.getDatabase().getUser(username))
                .filter(optional -> {
                    if (optional.isEmpty()) {
                        plugin.getLocales().getLocale("error_invalid_user")
                                .ifPresent(user::sendMessage);
                        return false;
                    }
                    return true;
                })
                .map(Optional::get).map(SavedUser::getUser)
                .toList();
    }

    @Nullable
    @Override
    public List<String> suggest(@NotNull CommandUser user, @NotNull String[] args) {
        return switch (args.length) {
            case 0, 1 -> user instanceof OnlineUser online ? getUserGroupNames(online.getUuid()) : null;
            case 2 -> List.of("add", "remove", "delete");
            default -> !args[1].equals("delete") && user instanceof OnlineUser online ? (args[0].equals("remove")
                    ? plugin.getUserGroup(online.getUuid(), args[1]).map(UserGroup::members).orElse(List.of())
                    : plugin.getUserList()).stream().map(User::getName).toList() : null;
        };
    }

    @NotNull
    private List<String> getUserGroupNames(@NotNull UUID uuid) {
        return plugin.getUserGroups(uuid).stream()
                .map(UserGroup::name)
                .collect(Collectors.toList());
    }

}
