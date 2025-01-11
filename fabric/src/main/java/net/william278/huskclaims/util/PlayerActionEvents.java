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

package net.william278.huskclaims.util;

import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.EventFactory;
import net.minecraft.block.entity.SignBlockEntity;
import net.minecraft.entity.ItemEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.server.filter.FilteredMessage;
import net.minecraft.server.network.ServerPlayerEntity;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.List;

public final class PlayerActionEvents {

    @NotNull
    public static final Event<AfterSwapHands> AFTER_HELD_ITEM_CHANGE = EventFactory.createArrayBacked(
            AfterSwapHands.class,
            (callbacks) -> (player, mainHand, offHand) -> {
                for (AfterSwapHands listener : callbacks) {
                    listener.swapHands(player, mainHand, offHand);
                }
            }
    );

    @NotNull
    public static final Event<BeforeChangeTextOnSign> BEFORE_CHANGE_TEXT_ON_SIGN = EventFactory.createArrayBacked(
        BeforeChangeTextOnSign.class,
        (callbacks) -> (sign, player, front, messages) -> {
            for (BeforeChangeTextOnSign listener : callbacks) {
                messages = listener.changeTextOnSign(sign, player, front, messages);
            }
            return messages;
        }
    );

    @NotNull
    public static final Event<AfterDeathDropItems> AFTER_DEATH_DROP_ITEMS = EventFactory.createArrayBacked(
        AfterDeathDropItems.class,
        (callbacks) -> (player, items) -> {
            for (AfterDeathDropItems listener : callbacks) {
                listener.dropItems(player, items);
            }
        }
    );

    @FunctionalInterface
    public interface AfterSwapHands {

        void swapHands(ServerPlayerEntity player, ItemStack mainHandItem, ItemStack offHandItem);

    }

    @FunctionalInterface
    public interface BeforeChangeTextOnSign {

        List<FilteredMessage> changeTextOnSign(SignBlockEntity sign, ServerPlayerEntity player, boolean front, List<FilteredMessage> messages);

    }

    @FunctionalInterface
    public interface AfterDeathDropItems {

        void dropItems(ServerPlayerEntity player, Collection<ItemEntity> items);

    }

}
