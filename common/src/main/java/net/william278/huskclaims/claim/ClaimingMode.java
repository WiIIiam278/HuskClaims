package net.william278.huskclaims.claim;

import lombok.AllArgsConstructor;

import java.util.List;

/**
 * Types of claim selection modes
 *
 * @since 1.0
 */
@AllArgsConstructor
public enum ClaimingMode {
    CLAIMS(List.of()),
    CHILD_CLAIMS(List.of("childclaims", "subdivideclaims")),
    ADMIN_CLAIMS(List.of("adminclaim"));

    private List<String> switchCommandAliases;
}
