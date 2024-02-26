package net.william278.huskclaims.moderation;

import com.google.common.collect.Lists;
import net.william278.huskclaims.HuskClaims;
import net.william278.huskclaims.config.Settings;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Optional;
import java.util.StringJoiner;
import java.util.regex.Pattern;

/**
 * Interface for handling sign edits / placements, for moderation.
 *
 * @since 1.0.3
 */
public interface SignListener {


    /**
     * Handles the editing of a sign
     *
     * @param edit the sign edit
     * @return the processed sign edit
     */
    @NotNull
    default SignWrite handleSignEdit(@NotNull SignWrite edit) {
        filterSignText(edit.getText()).ifPresent(text -> {
            edit.setText(text);
            edit.setFiltered(true);
        });
        getPlugin().notifyAllSignModerators(edit);
        return edit;
    }

    // Filter sign text against regex
    private Optional<List<String>> filterSignText(@NotNull List<String> text) {
        final Settings.ModerationSettings.SignSettings signs = getPlugin().getSettings().getModeration().getSigns();
        if (!signs.isFilterMessages()) {
            return Optional.empty();
        }

        boolean isFiltered = false;
        final List<String> filtered = Lists.newArrayList();
        for (String line : text) {
            final String afterFilter = getFiltered(line, signs.getFilteredWords(), signs.getReplacementCharacter());
            filtered.add(afterFilter);
            isFiltered = isFiltered || afterFilter.equals(line);
        }
        return isFiltered ? Optional.of(filtered) : Optional.empty();
    }

    @NotNull
    private String getFiltered(@NotNull String line, @NotNull List<String> words, char replaceWith) {
        return line.replaceAll(getFiltered(words), String.valueOf(replaceWith));
    }

    @NotNull
    static String getFiltered(@NotNull List<String> illegal) {
        final StringJoiner pattern = new StringJoiner("|");
        illegal.forEach(word -> pattern.add(
                String.format("(?<=(?=%s).{0,%d}).", Pattern.quote(word), word.length() - 1)
        ));
        return pattern.toString();
    }

    @NotNull
    HuskClaims getPlugin();

}
