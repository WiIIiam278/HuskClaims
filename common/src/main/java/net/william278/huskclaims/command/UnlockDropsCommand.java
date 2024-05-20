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
import net.william278.huskclaims.user.CommandUser;
import net.william278.huskclaims.user.OnlineUser;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Map;
import java.util.Optional;

public class UnlockDropsCommand extends Command implements UserListTabCompletable {

    protected UnlockDropsCommand(@NotNull HuskClaims plugin) {
        super(List.of("unlockdrops"), "[username]", plugin);
        addAdditionalPermissions(Map.of("other", true));
    }

    @Override
    public void execute(@NotNull CommandUser executor, @NotNull String[] args) {
        final Optional<OnlineUser> user = resolveOnlineUser(executor, args);
        if (user.isEmpty() || (!user.get().equals(executor) && !hasPermission(executor, "other"))) {
            plugin.getLocales().getLocale("error_invalid_syntax", getUsage())
                    .ifPresent(executor::sendMessage);
            return;
        }

        plugin.runSync(() -> {
            final long unlocked = plugin.unlockDrops(user.get());
            if (unlocked > 0) {
                getPlugin().getLocales().getLocale("death_drops_unlocked", user.get().getName(),
                        Long.toString(unlocked)).ifPresent(executor::sendMessage);
                return;
            }
            getPlugin().getLocales().getLocale("error_no_death_drops", user.get().getName())
                    .ifPresent(executor::sendMessage);
        });
    }


}
