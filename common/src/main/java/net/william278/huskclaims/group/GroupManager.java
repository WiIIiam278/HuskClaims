package net.william278.huskclaims.group;

import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Interface for managing {@link UserGroup}s &mdash; groups of multiple users for easier management of claims
 */
public interface GroupManager {

    /**
     * Get a list of user groups
     *
     * @return the list of user groups
     * @since 1.0
     */
    @NotNull
    List<UserGroup> getUserGroups();

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
     * Load the user groups from the database
     *
     * @since 1.0
     */
    default void loadUserGroups() {
        //todo load from DB
    }

}
