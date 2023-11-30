package net.william278.huskclaims.user;

import org.jetbrains.annotations.NotNull;

import java.time.OffsetDateTime;

/**
 * Record of a user's data
 *
 * @param user        The user
 * @param lastLogin   The last time the user logged in
 * @param claimBlocks The number of claim blocks the user has
 * @param preferences The user's preferences
 */
public record SavedUser(
        @NotNull User user,
        @NotNull OffsetDateTime lastLogin,
        long claimBlocks,
        @NotNull Preferences preferences
) {

    public long getDaysSinceLastLogin() {
        return lastLogin.until(OffsetDateTime.now(), java.time.temporal.ChronoUnit.DAYS);
    }

}
