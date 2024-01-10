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

package net.william278.huskclaims.trust;

import lombok.AllArgsConstructor;
import lombok.Getter;
import net.william278.huskclaims.HuskClaims;
import net.william278.huskclaims.user.CommandUser;
import net.william278.huskclaims.user.User;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

@Getter
@AllArgsConstructor
public abstract class TrustTag implements TrustableCollection {

    /**
     * The name of this tag
     */
    protected String name;

    /**
     * The description of this tag
     */
    protected String description;

    /**
     * The permission required to use this tag
     */
    @Nullable
    protected String permissionToUse;

    /**
     * Whether the permission is a default permission
     */
    protected boolean permissionDefault;

    public TrustTag(@NotNull String name, @NotNull String description) {
        this.name = name;
        this.description = description;
    }
    @NotNull
    public static DeletedTrustTag getDeletedTag(@NotNull String name) {
        return new DeletedTrustTag(name);
    }

    @NotNull
    @Override
    public String getTrustIdentifier(@NotNull HuskClaims plugin) {
        return String.format(
                "%s%s",
                plugin.getSettings().getTrustTags().getTagSpecifierPrefix(),
                name.replaceAll(" ", "_")
        );
    }

    @Override
    public abstract boolean includes(@NotNull User trustable);

    public boolean canUse(@NotNull CommandUser user) {
        return permissionToUse == null || user.hasPermission(permissionToUse, permissionDefault);
    }

    public static class DeletedTrustTag extends TrustTag {

        private DeletedTrustTag(@NotNull String name) {
            super(name, "(deleted)");
        }

        @Override
        public boolean includes(@NotNull User trustable) {
            return false;
        }
    }

}
