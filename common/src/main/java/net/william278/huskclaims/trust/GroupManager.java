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

package net.william278.huskclaims.trust;

import net.william278.huskclaims.HuskClaims;
import net.william278.huskclaims.database.Database;
import net.william278.huskclaims.network.Message;
import net.william278.huskclaims.network.Payload;
import net.william278.huskclaims.user.OnlineUser;
import net.william278.huskclaims.user.User;
import org.jetbrains.annotations.Blocking;
import org.jetbrains.annotations.NotNull;

import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.function.Consumer;
import java.util.logging.Level;

/**
 * Interface for managing {@link UserGroup}s &mdash; groups of multiple trustable users for easier management of claims
 *
 * @since 1.0
 */
public interface GroupManager {

    /**
     * Get a list of user groups
     *
     * @since 1.0
     */
    @NotNull
    Set<UserGroup> getUserGroups();

    /**
     * Set the list of user groups
     *
     * @param userGroups The list of user groups
     */
    void setUserGroups(@NotNull Set<UserGroup> userGroups);

    default void setUserGroups(@NotNull UUID owner, @NotNull Collection<UserGroup> userGroups) {
        getUserGroups().removeIf(userGroup -> userGroup.groupOwner().equals(owner));
        getUserGroups().addAll(userGroups);
    }

    /**
     * Get a list of user groups owned by a player
     *
     * @param owner The owner of the groups
     * @return the list of user groups
     * @since 1.0
     */
    @NotNull
    default List<UserGroup> getUserGroups(@NotNull UUID owner) {
        return getUserGroups().stream().filter(userGroup -> userGroup.groupOwner().equals(owner)).toList();
    }

    /**
     * Get a user group by name
     *
     * @param owner The owner of the group
     * @param name  The name of the group
     * @return the user group, if found
     * @since 1.0
     */
    default Optional<UserGroup> getUserGroup(@NotNull UUID owner, @NotNull String name) {
        return getUserGroups(owner).stream().filter(userGroup -> userGroup.name().equalsIgnoreCase(name)).findFirst();
    }

    /**
     * Create a user group
     *
     * @param owner   The user creating the group
     * @param name    The name of the group
     * @param members The members of the group
     * @since 1.0
     */
    @Blocking
    default void createUserGroup(@NotNull OnlineUser owner, @NotNull String name, @NotNull List<User> members) throws IllegalArgumentException {
        if (!getPlugin().isValidGroupName(name) || getUserGroup(owner.getUuid(), name).isPresent()) {
            throw new IllegalArgumentException("Invalid or already taken group name");
        }
        final UserGroup group = new UserGroup(owner.getUuid(), name, members);
        getUserGroups().add(group);
        getDatabase().addUserGroup(group);
        publishGroupChange(owner);
    }

    /**
     * Edit a user group
     *
     * @param owner      UUID of the group owner
     * @param groupName  Name of the group
     * @param editor     Consumer to edit the group
     * @param notPresent Runnable to run if the group is not present
     * @since 1.0
     */
    @Blocking
    default void editUserGroup(@NotNull OnlineUser owner, @NotNull String groupName,
                               @NotNull Consumer<UserGroup> editor, @NotNull Runnable notPresent) {
        getUserGroup(owner.getUuid(), groupName).ifPresentOrElse(group -> {
            editor.accept(group);
            getUserGroups().removeIf(g -> g.groupOwner().equals(owner.getUuid()) && g.name().equalsIgnoreCase(groupName));
            getUserGroups().add(group);
            getDatabase().updateUserGroup(owner.getUuid(), groupName, group);
            publishGroupChange(owner);
        }, notPresent);
    }

    /**
     * Delete a user group
     *
     * @param owner     UUID of the group owner
     * @param groupName Name of the group
     * @return {@code true} if the group was deleted, {@code false} otherwise
     * @since 1.0
     */
    @Blocking
    default boolean deleteUserGroup(@NotNull OnlineUser owner, @NotNull String groupName) {
        return getUserGroup(owner.getUuid(), groupName).map(group -> {
            getDatabase().deleteUserGroup(group);
            getUserGroups().remove(group);
            publishGroupChange(owner);
            return true;
        }).orElse(false);
    }

    private void publishGroupChange(@NotNull OnlineUser user) {
        getPlugin().getBroker().ifPresent(broker -> Message.builder()
                .type(Message.MessageType.INVALIDATE_USER_GROUPS)
                .payload(Payload.uuid(user.getUuid()))
                .target(Message.TARGET_ALL, Message.TargetType.SERVER)
                .build().send(broker, user));
    }

    /**
     * Load the user groups from the database
     *
     * @since 1.0
     */
    @Blocking
    default void loadUserGroups() {
        getPlugin().log(Level.INFO, "Loading user groups from the database...");
        LocalTime startTime = LocalTime.now();

        // Load all users groups from the database
        final Set<UserGroup> groups = getDatabase().getAllUserGroups();
        this.setUserGroups(groups);

        final long uniqueGroups = groups.stream().map(UserGroup::groupOwner).toList().stream().distinct().count();
        getPlugin().log(Level.INFO, String.format("Loaded %s user group(s) by %s user(s) in %s seconds",
                groups.size(), uniqueGroups, ChronoUnit.MILLIS.between(startTime, LocalTime.now()) / 1000d));
    }

    @NotNull
    Database getDatabase();

    @NotNull
    HuskClaims getPlugin();

}
