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

package net.william278.huskclaims.event;

import lombok.Getter;
import lombok.Setter;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

/**
 * Bukkit event fired when a player receives hourly claim blocks.
 * <p>
 * This event is designed for integration with booster plugins (e.g. AxBoosters)
 * that modify values via reflection using getter/setter methods.
 * <p>
 * AxBoosters config example:
 * <pre>
 * huskclaims:
 *   icon: GRASS_BLOCK
 *   event: net.william278.huskclaims.event.BukkitHourlyClaimBlocksEvent
 *   player: getPlayer
 *   getter: getAmount
 *   setter: setAmount
 * </pre>
 */
@Getter
public class BukkitHourlyClaimBlocksEvent extends Event {

    private static final HandlerList HANDLER_LIST = new HandlerList();

    private final Player player;

    @Setter
    private long amount;

    public BukkitHourlyClaimBlocksEvent(@NotNull Player player, long amount) {
        super(false);
        this.player = player;
        this.amount = amount;
    }

    @NotNull
    @Override
    public HandlerList getHandlers() {
        return HANDLER_LIST;
    }

    public static HandlerList getHandlerList() {
        return HANDLER_LIST;
    }

}
