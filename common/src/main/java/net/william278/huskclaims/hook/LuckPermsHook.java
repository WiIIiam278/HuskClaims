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

package net.william278.huskclaims.hook;

import com.google.common.collect.Sets;
import net.luckperms.api.LuckPerms;
import net.luckperms.api.LuckPermsProvider;
import net.luckperms.api.model.group.Group;
import net.luckperms.api.model.group.GroupManager;
import net.luckperms.api.model.user.UserManager;
import net.luckperms.api.query.QueryOptions;
import net.william278.huskclaims.HuskClaims;
import net.william278.huskclaims.config.Locales;
import net.william278.huskclaims.trust.TrustTag;
import net.william278.huskclaims.user.User;
import org.jetbrains.annotations.NotNull;

import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.logging.Level;

public class LuckPermsHook extends Hook {

    private LuckPerms luckPerms;
    private final Set<String> groupTags;

    protected LuckPermsHook(@NotNull HuskClaims plugin) {
        super("LuckPerms", plugin);
        groupTags = Sets.newHashSet();
    }

    @Override
    public void load() throws IllegalStateException {
        this.luckPerms = LuckPermsProvider.get();

        // Load LuckPerms group tags
        if (plugin.getSettings().getTrustTags().isEnabled()) {
            plugin.log(Level.INFO, "Registering LuckPerms group trust tags...");
            loadGroupTags();
        }
    }

    private void loadGroupTags() {
        final GroupManager groups = luckPerms.getGroupManager();
        groups.loadAllGroups().thenRun(() -> groups.getLoadedGroups().forEach(group -> {
            try {
                final GroupTrustTag tag = new GroupTrustTag(group, luckPerms.getUserManager(), plugin);
                groupTags.add(tag.getName());
                plugin.registerTrustTag(tag);
            } catch (Throwable e) {
                plugin.log(Level.WARNING, "Failed to create LuckPerms trust tag for " + group.getName(), e);
            }
        }));
    }

    @Override
    public void unload() {
        groupTags.forEach(plugin::unregisterTrustTag);
    }

    public static class GroupTrustTag extends TrustTag {

        public static final String USE_PERMISSION = "huskclaims.trust.luckperms";
        private final UserManager users;
        private final Group group;

        protected GroupTrustTag(@NotNull Group group, @NotNull UserManager users, @NotNull HuskClaims plugin) {
            super(
                    String.format(
                            "%s%s",
                            plugin.getSettings().getHooks().getLuckPerms().getTrustTagPrefix(),
                            group.getName().toLowerCase(Locale.ENGLISH).replaceAll(" ", "_")
                    ),
                    plugin.getLocales().getRawLocale("group_tag_description", Locales.
                            escapeText(group.getDisplayName() == null ? group.getName() : group.getDisplayName())
                    ).orElse(plugin.getLocales().getNotApplicable()),
                    plugin.getSettings().getHooks().getLuckPerms().isTrustTagUsePermission() ? USE_PERMISSION : null,
                    false
            );
            this.group = group;
            this.users = users;
        }

        @Override
        public boolean includes(@NotNull User trustable) {
            return Optional.ofNullable(users.getUser(trustable.getUuid()))
                    .map(user -> user.getInheritedGroups(QueryOptions.defaultContextualOptions()))
                    .map(groups -> groups.contains(group))
                    .orElse(false);
        }

    }

}
