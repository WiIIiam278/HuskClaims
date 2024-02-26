package net.william278.huskclaims.moderation;

import net.william278.huskclaims.HuskClaims;
import net.william278.huskclaims.config.Locales;
import net.william278.huskclaims.network.Message;
import net.william278.huskclaims.network.Payload;
import net.william278.huskclaims.position.Position;
import net.william278.huskclaims.position.ServerWorld;
import net.william278.huskclaims.user.OnlineUser;
import org.jetbrains.annotations.NotNull;

public interface SignNotifier {

    String MODERATOR_POSITION = "huskclaims.moderate_signs";

    default void notifyLocalSignModerators(@NotNull SignWrite write) {
        getPlugin().getOnlineUsers()
                .stream().filter(mod -> mod.hasPermission(MODERATOR_POSITION))
                .forEach(mod -> notifySignModerators(mod, write));
    }

    default void notifyAllSignModerators(@NotNull SignWrite write) {
        notifyLocalSignModerators(write);
        getPlugin().getBroker().ifPresent(broker -> Message.builder()
                .type(Message.MessageType.SIGN_WRITE)
                .target(Message.TARGET_ALL, Message.TargetType.PLAYER)
                .payload(Payload.signEdit(write))
                .build().send(broker, (OnlineUser) write.getEditor()));
    }

    private void notifySignModerators(@NotNull OnlineUser moderator, @NotNull SignWrite write) {
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

    @NotNull
    HuskClaims getPlugin();
}
