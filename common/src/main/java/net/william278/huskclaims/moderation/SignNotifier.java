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

import net.william278.huskclaims.HuskClaims;
import net.william278.huskclaims.config.Locales;
import net.william278.huskclaims.config.Settings;
import net.william278.huskclaims.network.Message;
import net.william278.huskclaims.network.Payload;
import net.william278.huskclaims.position.Position;
import net.william278.huskclaims.position.ServerWorld;
import net.william278.huskclaims.user.OnlineUser;
import org.jetbrains.annotations.NotNull;

public interface SignNotifier {

    String MODERATOR_POSITION = "huskclaims.moderate_signs";

    default void notifyLocalSignModerators(@NotNull SignWrite write) {
        if (!shouldNotify(write)) {
            return;
        }
        getPlugin().getOnlineUsers()
                .stream().filter(mod -> mod.hasPermission(MODERATOR_POSITION))
                .forEach(mod -> notifySignModerator(mod, write));
    }

    default void notifyAllSignModerators(@NotNull SignWrite write) {
        if (!shouldNotify(write)) {
            return;
        }
        notifyLocalSignModerators(write);
        getPlugin().getBroker().ifPresent(broker -> Message.builder()
                .type(Message.MessageType.SIGN_WRITE)
                .target(Message.TARGET_ALL, Message.TargetType.PLAYER)
                .payload(Payload.signEdit(write))
                .build().send(broker, (OnlineUser) write.getEditor()));
    }

    private void notifySignModerator(@NotNull OnlineUser moderator, @NotNull SignWrite write) {
        if (!shouldNotify(write)) {
            return;
        }
        final Locales locales = getPlugin().getLocales();
        final Position position = write.getPosition();

        locales.getRawLocale("sign_write_notify",
                        Locales.escapeText(write.getEditor().getName()),
                        Locales.escapeText(write.getType().getLocale(locales)),
                        getPlugin().getLocales().getPositionText(position, (int) position.getY(),
                                new ServerWorld(write.getServer(), position.getWorld()), moderator, getPlugin()),
                        write.isFiltered() ? locales.getRawLocale("sign_write_filtered").orElse("") : "",
                        Locales.escapeText(String.join(" / ", write.getText())))
                .map(t -> getPlugin().getLocales().format(t))
                .ifPresent(moderator::sendMessage);
    }

    private boolean shouldNotify(@NotNull SignWrite write) {
        final Settings.ModerationSettings.SignSettings signs = getPlugin().getSettings().getModeration().getSigns();
        return signs.isNotifyModerators() && (!signs.isOnlyNotifyIfFiltered() || write.isFiltered());
    }

    @NotNull
    HuskClaims getPlugin();
}
