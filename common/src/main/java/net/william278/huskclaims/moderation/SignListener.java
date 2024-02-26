package net.william278.huskclaims.moderation;

import net.william278.huskclaims.HuskClaims;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Optional;

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
        return Optional.empty();
    }

    @NotNull
    HuskClaims getPlugin();

}
