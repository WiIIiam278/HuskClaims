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

package net.william278.huskclaims.hook;

import net.william278.huskclaims.HuskClaims;
import net.william278.huskclaims.position.Position;
import net.william278.huskclaims.trust.TrustLevel;
import net.william278.huskclaims.user.OnlineUser;
import org.jetbrains.annotations.NotNull;

import java.util.logging.Level;

public abstract class HuskHomesHook extends Hook {

    private TrustLevel requiredLevel;

    protected HuskHomesHook(@NotNull HuskClaims plugin) {
        super("HuskHomes", plugin);
    }

    @Override
    public void load() {
        requiredLevel = plugin.getTrustLevel(plugin.getSettings().getHooks().getHuskHomes().getSetHomeTrustLevel())
                .orElseGet(() -> {
                    plugin.log(Level.WARNING, "Invalid home trust level specified for HuskHomes hook.");
                    return plugin.getLowestTrustLevel();
                });
    }

    protected boolean cancelHomeAt(@NotNull OnlineUser user, @NotNull Position position) {
        return plugin.getClaimAt(position)
                .flatMap(claim -> claim.getTrustLevel(user, plugin).map(level -> level.compareTo(requiredLevel) < 0))
                .orElse(false);
    }

    public abstract void teleport(@NotNull OnlineUser user, @NotNull Position position, @NotNull String server);

}
