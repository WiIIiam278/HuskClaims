package net.william278.huskclaims.util;

import net.kyori.adventure.audience.Audiences;
import net.kyori.adventure.platform.AudienceProvider;
import net.william278.huskclaims.user.ConsoleUser;
import org.jetbrains.annotations.NotNull;

/**
 * Interface for providing the {@link ConsoleUser} and {@link Audiences} instances
 *
 * @since 1.0
 */
public interface AudiencesProvider {

    /**
     * Get the {@link Audiences} instance
     *
     * @return the {@link Audiences} instance
     * @since 1.0
     */
    @NotNull
    AudienceProvider getAudiences();

    /**
     * Get the {@link ConsoleUser} instance
     *
     * @return the {@link ConsoleUser} instance
     * @since 1.0
     */
    @NotNull
    default ConsoleUser getConsole() {
        return ConsoleUser.wrap(getAudiences().console());
    }

}
