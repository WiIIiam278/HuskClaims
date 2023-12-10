package net.william278.huskclaims.command;

import net.william278.huskclaims.HuskClaims;
import net.william278.huskclaims.claim.Claim;
import net.william278.huskclaims.claim.ClaimWorld;
import net.william278.huskclaims.claim.TrustLevel;
import net.william278.huskclaims.config.Locales;
import net.william278.huskclaims.user.OnlineUser;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.StringJoiner;
import java.util.UUID;
import java.util.stream.Collectors;

public class TrustListCommand extends InClaimCommand {

    private static final int DESCRIPTION_WRAP = 40;

    protected TrustListCommand(@NotNull HuskClaims plugin) {
        super(List.of("trustlist"), plugin);
    }

    @Override
    public void execute(@NotNull OnlineUser executor, @NotNull ClaimWorld world,
                        @NotNull Claim claim, @NotNull String[] args) {
        if (!claim.isPrivilegeAllowed(TrustLevel.Privilege.MANAGE_TRUSTEES, executor, world, plugin)) {
            plugin.getLocales().getLocale("no_managing_permission")
                    .ifPresent(executor::sendMessage);
            return;
        }

        // Send the trust list menu
        this.sendTrustListMenu(executor, claim, world);
    }

    private void sendTrustListMenu(@NotNull OnlineUser executor, @NotNull Claim claim, @NotNull ClaimWorld world) {
        plugin.getLocales().getLocale("trust_list_claim_header",
                claim.getOwnerName(world, plugin)).ifPresent(executor::sendMessage);
        plugin.getTrustLevels().forEach(level -> sendTrustListRow(executor, level, claim, world));
    }

    private void sendTrustListRow(@NotNull OnlineUser executor, @NotNull TrustLevel level,
                                  @NotNull Claim claim, @NotNull ClaimWorld world) {
        if (claim.getTrustedUsers().isEmpty() && claim.getTrustedGroups().isEmpty()) {
            return;
        }

        // Add trusted users and groups
        final StringJoiner joiner = new StringJoiner(
                plugin.getLocales().getRawLocale("trust_list_separator").orElse(", ")
        );
        claim.getTrustedUsers().forEach(
                (uuid, string) -> joiner.add(getMemberEntry(uuid, world))
        );
        claim.getOwner().ifPresent(owner -> claim.getTrustedGroups().forEach(
                (name, uuid) -> joiner.add(getGroupEntry(name, owner, plugin))
        ));

        // Return the row
        plugin.getLocales().getLocale("trust_list_row",
                Locales.escapeText(level.getDisplayName()),
                plugin.getLocales().wrapText(Locales.escapeText(level.getDescription()), DESCRIPTION_WRAP),
                joiner.toString()
        ).ifPresent(executor::sendMessage);
    }

    @NotNull
    private String getMemberEntry(@NotNull UUID uuid, @NotNull ClaimWorld world) {
        return world.getUser(uuid)
                .flatMap(user -> plugin.getLocales().getRawLocale("trust_list_user",
                        Locales.escapeText(user.getName())))
                .orElse(uuid.toString());
    }

    @NotNull
    private String getGroupEntry(@NotNull String name, @Nullable UUID owner, @NotNull HuskClaims plugin) {
        if (owner == null) {
            return name;
        }
        return plugin.getUserGroup(owner, name)
                .flatMap(group -> plugin.getLocales().getRawLocale("trust_list_group",
                        Locales.escapeText(group.getTrustIdentifier(plugin)),
                        Integer.toString(group.members().size()),
                        group.members().stream()
                                .map(user -> Locales.escapeText(user.getName()))
                                .collect(Collectors.joining("\n"))
                ))
                .orElse(name);
    }

}
