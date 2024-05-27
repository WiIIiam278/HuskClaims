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

import net.william278.huskclaims.config.Settings;
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

        // Suggest group names
        final Settings.UserGroupSettings groups = getPlugin().getSettings().getUserGroups();
        final UUID groupOwner = getGroupOwner(onlineUser);
        if (groupOwner != null && groups.isEnabled() &&
            (args.length > 0 && args[args.length - 1].startsWith(groups.getGroupSpecifierPrefix()))) {
            return getPlugin().getUserGroups(groupOwner).stream()
                    .map(group -> group.getTrustIdentifier(getPlugin()))
                    .toList();
        }

        // Suggest tags
        final Settings.TrustTagSettings tags = getPlugin().getSettings().getTrustTags();
        if (tags.isEnabled() && (args.length > 0 && args[args.length - 1].startsWith(tags.getTagSpecifierPrefix()))) {
            return getPlugin().getTrustTags().stream()
                    .map(tag -> tag.getTrustIdentifier(getPlugin()))
                    .toList();
        }

        // Suggest usernames
        return UserListTabCompletable.super.suggest(user, args);
    }

    @Nullable
    UUID getGroupOwner(@NotNull OnlineUser user);


}
