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

package net.william278.huskclaims.moderation;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import net.william278.huskclaims.HuskClaims;
import net.william278.huskclaims.config.Settings;
import net.william278.huskclaims.user.OnlineUser;
import net.william278.huskclaims.user.User;
import org.jetbrains.annotations.NotNull;

import java.util.*;

public interface DropsHandler {

    @NotNull
    Map<UUID, List<DroppedItem>> getMarkedDrops();

    @NotNull
    Map<UUID, Set<GroundStack>> getTrackedItems();

    default void markItemsForLocking(@NotNull OnlineUser user,
                                     @NotNull Collection<? extends DroppedItem> items) {
        if (!getSettings().isLockItems()) {
            return;
        }
        getMarkedDrops().put(user.getUuid(), Lists.newArrayList(items));
        getPlugin().getLocales().getLocale("death_drops_locked")
                .ifPresent(user::sendMessage);
    }

    default void checkDroppedItem(@NotNull GroundStack item) {
        if (!getSettings().isLockItems()) {
            return;
        }
        removeIfMarkedDropper(item.getStack()).ifPresent(owner -> {
            lockDrop(owner, item);
            getMarkedDrops().get(owner).remove(item.getStack());
        });
    }

    default void lockDrop(@NotNull UUID owner, @NotNull GroundStack item) {
        lockDrops(owner, Lists.newArrayList(item));
    }

    default void lockDrops(@NotNull UUID owner, @NotNull Collection<? extends GroundStack> items) {
        items.forEach(item -> item.lock(owner, getSettings().isPreventDestruction()));
        if (getTrackedItems().containsKey(owner)) {
            getTrackedItems().get(owner).addAll(items);
            return;
        }
        getTrackedItems().put(owner, Sets.newHashSet(items));
    }

    //TODO: Future - permit globally unlocking via a network message?
    default long unlockDrops(@NotNull User toUnlock) {
        if (!getSettings().isLockItems()) {
            return 0;
        }
        final Set<GroundStack> stacks = getTrackedItems().getOrDefault(
                toUnlock.getUuid(), Collections.emptySet()
        );
        stacks.forEach(GroundStack::unlock);
        getTrackedItems().remove(toUnlock.getUuid());
        return stacks.size();
    }

    @NotNull
    private Settings.ModerationSettings.DropSettings getSettings() {
        return getPlugin().getSettings().getModeration().getDrops();
    }

    default Optional<UUID> removeIfMarkedDropper(@NotNull DroppedItem item) {
        return getMarkedDrops().entrySet().stream()
                .filter(entry -> entry.getValue().contains(item))
                .map(Map.Entry::getKey)
                .findFirst();
    }

    @NotNull
    HuskClaims getPlugin();

    interface DroppedItem {
        // How far to count death dropped stacks as equal, from their spawn origin (in blocks)
        double DEATH_DROPS_EQUAL_RANGE = 5.0d;
    }

    interface GroundStack {
        @NotNull
        DroppedItem getStack();

        void lock(@NotNull UUID user, boolean preventDestruction);

        void unlock();
    }

}
