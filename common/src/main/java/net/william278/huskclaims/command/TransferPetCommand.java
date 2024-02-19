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
import net.william278.huskclaims.user.OnlineUser;
import net.william278.huskclaims.user.SavedUser;
import net.william278.huskclaims.user.User;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Map;
import java.util.Optional;

public class TransferPetCommand extends OnlineUserCommand implements UserListTabCompletable {

    protected TransferPetCommand(@NotNull HuskClaims plugin) {
        super(
                List.of("transferpet", "givepet"),
                "<username>",
                plugin
        );
        addAdditionalPermissions(Map.of("other", true));
    }

    @Override
    public void execute(@NotNull OnlineUser executor, @NotNull String[] args) {
        final Optional<String> username = parseStringArg(args, 0);
        if (username.isEmpty()) {
            plugin.getLocales().getLocale("error_invalid_syntax", getUsage())
                    .ifPresent(executor::sendMessage);
            return;
        }

        transferPet(executor, username.get());
    }

    // Transfer a pet to a user by name
    private void transferPet(@NotNull OnlineUser executor, @NotNull String username) {
        final Optional<User> user = plugin.getDatabase().getUser(username).map(SavedUser::getUser);
        if (user.isEmpty()) {
            plugin.getLocales().getLocale("error_invalid_user", username)
                    .ifPresent(executor::sendMessage);
            return;
        }

        plugin.userTransferPet(executor, user.get(), hasPermission(executor, "other"));
    }

}
