package net.william278.huskclaims.user;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import net.kyori.adventure.audience.Audience;
import org.jetbrains.annotations.NotNull;

@Getter
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class ConsoleUser implements CommandUser {

    private final Audience audience;

    @NotNull
    public static ConsoleUser wrap(@NotNull Audience audience) {
        return new ConsoleUser(audience);
    }

    @Override
    public boolean hasPermission(@NotNull String permission) {
        return true;
    }
}
