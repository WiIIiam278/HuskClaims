package net.william278.huskclaims.util;

import net.william278.huskclaims.HuskClaims;
import net.william278.huskclaims.config.Settings;
import org.jetbrains.annotations.NotNull;

public interface TextValidator {

    int MAX_GROUP_NAME_LENGTH = 32;

    // Check a group name contains only valid characters
    default boolean isValidGroupName(@NotNull String name) {
        return (name.matches(getSettings().getUserGroups().getGroupNameRegex())
                || !getSettings().getUserGroups().isRestrictGroupNames())
                && !name.contains("\u0000")
                && isValidNameLength(name)
                && !containsWhitespace(name)
                && !name.contains(getSettings().getUserGroups().getGroupSpecifierPrefix());
    }

    // Check a group name is of a valid length
    private boolean isValidNameLength(@NotNull String name) {
        return name.length() <= MAX_GROUP_NAME_LENGTH && !name.isEmpty();
    }

    // Check if a string contains whitespace
    private boolean containsWhitespace(@NotNull String string) {
        return string.matches(".*\\s.*");
    }

    @NotNull
    HuskClaims getPlugin();

    @NotNull
    Settings getSettings();

}
