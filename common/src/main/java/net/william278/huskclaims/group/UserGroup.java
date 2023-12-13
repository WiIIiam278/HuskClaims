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

package net.william278.huskclaims.group;

import net.william278.huskclaims.HuskClaims;
import net.william278.huskclaims.claim.Trustable;
import net.william278.huskclaims.user.User;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.UUID;

/**
 * A group of users that can be added to a claim. Useful for bulk management of claim permissions.
 *
 * @param groupOwner The UUID of the group owner
 * @param name       The name of the group
 * @param members    The members of the group
 * @since 1.0
 */
public record UserGroup(
        @NotNull UUID groupOwner,
        @NotNull String name,
        @NotNull List<User> members
) implements Trustable {

    public boolean isMember(@NotNull UUID uuid) {
        return members.stream().anyMatch(user -> user.getUuid().equals(uuid));
    }

    public boolean removeMember(@NotNull UUID uuid) {
        return members.removeIf(user -> user.getUuid().equals(uuid));
    }

    @NotNull
    @Override
    public String getTrustIdentifier(@NotNull HuskClaims plugin) {
        return String.format(
                "%s%s",
                plugin.getSettings().getUserGroups().getGroupSpecifierPrefix(),
                name.replaceAll(" ", "_")
        );
    }
}
