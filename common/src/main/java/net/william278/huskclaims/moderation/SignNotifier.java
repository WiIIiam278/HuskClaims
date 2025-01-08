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
import net.william278.huskclaims.command.SignSpyCommand;
import net.william278.huskclaims.config.Locales;
import net.william278.huskclaims.config.Settings;
import net.william278.huskclaims.network.Message;
import net.william278.huskclaims.network.Payload;
import net.william278.huskclaims.position.Position;
import net.william278.huskclaims.position.ServerWorld;
import net.william278.huskclaims.user.OnlineUser;
import net.william278.huskclaims.user.Preferences;
import org.jetbrains.annotations.NotNull;

/**
 * Interface for notifying moderators of sign edits
 *
 * @since 1.1
 */
public interface SignNotifier {

    // Notify online sign spying moderators of a sign edit (excluding the writer)
    default void notifyLocalSignModerators(@NotNull SignWrite write) {
        if (shouldIgnoreSignWrite(write)) {
            return;
        }
        getPlugin().getOnlineUsers().stream()
                .filter(u -> !u.equals(write.getEditor())).filter(this::isSignSpying)
                .forEach(mod -> notifySignModerator(mod, write));
    }

    default void notifyAllSignModerators(@NotNull SignWrite write) {
        if (shouldIgnoreSignWrite(write)) {
            return;
        }
        notifyLocalSignModerators(write);
        getPlugin().getBroker().ifPresent(broker -> Message.builder()
                .type(Message.MessageType.SIGN_WRITE)
                .payload(Payload.signEdit(write))
                .target(Message.TARGET_ALL, Message.TargetType.PLAYER).build()
                .send(broker, (OnlineUser) write.getEditor()));
    }

    private void notifySignModerator(@NotNull OnlineUser moderator, @NotNull SignWrite write) {
        if (shouldIgnoreSignWrite(write)) {
            return;
        }
        final Locales locales = getPlugin().getLocales();
        final Position position = write.getPosition();

        locales.getRawLocale("sign_write_notify",
                        write.getType().getLocale(locales, write.getEditor().getName()),
                        getPlugin().getLocales().getPositionText(position, (int) position.getY(),
                                new ServerWorld(write.getServer(), position.getWorld()), moderator, getPlugin()),
                        write.isFiltered() ? locales.getRawLocale("sign_write_filtered").orElse("") : "",
                        String.join(
                                locales.getRawLocale("sign_notify_line_separator").orElse("\n"),
                                write.getText().stream().map(Locales::escapeText).toArray(String[]::new)
                        ))
                .map(t -> getPlugin().getLocales().format(t))
                .ifPresent(moderator::sendMessage);
    }

    private boolean shouldIgnoreSignWrite(@NotNull SignWrite write) {
        final Settings.ModerationSettings.SignSettings signs = getPlugin().getSettings().getModeration().getSigns();
        if (!signs.isNotifyModerators() || (signs.isOnlyNotifyIfFiltered() && !write.isFiltered())) {
            return true;
        }
        return write.getText().stream().allMatch(String::isBlank);
    }

    private boolean isSignSpying(@NotNull OnlineUser user) {
        boolean toggled = getPlugin().getCachedUserPreferences(user.getUuid())
                .map(Preferences::isSignNotifications).orElse(false);
        if (toggled && !getPlugin().canUseCommand(SignSpyCommand.class, user)) {
            getPlugin().runAsync(() -> {
                getPlugin().editUserPreferences(user, (preferences) -> preferences.setSignNotifications(false));
                getPlugin().getLocales().getLocale("sign_notify_off")
                        .ifPresent(user::sendMessage);
            });
            return false;
        }
        return toggled;
    }

    @NotNull
    HuskClaims getPlugin();
}
