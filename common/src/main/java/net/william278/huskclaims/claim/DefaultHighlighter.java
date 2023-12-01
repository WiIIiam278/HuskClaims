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

package net.william278.huskclaims.claim;

import net.william278.huskclaims.HuskClaims;
import net.william278.huskclaims.config.Settings;
import net.william278.huskclaims.user.OnlineUser;
import org.jetbrains.annotations.NotNull;

public class DefaultHighlighter implements ClaimHighlighter {

    private final Settings.ClaimSettings.HighlighterSettings settings;

    public DefaultHighlighter(@NotNull HuskClaims plugin) {
        this.settings = plugin.getSettings().getClaims().getHighlighting();
    }

    @Override
    public void highlightClaim(@NotNull OnlineUser user, @NotNull ClaimWorld world, @NotNull Claim claim) {
        //todo task tracking and such
    }

}
