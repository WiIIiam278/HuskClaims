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

package net.william278.huskclaims.pet;

import net.william278.huskclaims.BukkitHuskClaims;
import net.william278.huskclaims.user.BukkitUser;
import net.william278.huskclaims.user.OnlineUser;
import net.william278.huskclaims.user.User;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.AnimalTamer;
import org.bukkit.entity.Player;
import org.bukkit.entity.Tameable;
import org.jetbrains.annotations.NotNull;

import java.util.Comparator;
import java.util.Optional;

public interface BukkitPetHandler extends PetHandler {

    // Distance to search for entities
    int TRANSFER_RANGE = 6;
    // Dot product to check if the player is looking at the entity
    float TRANSFER_ENTITY_DOT = 0.975f;

    @Override
    default void userTransferPet(@NotNull OnlineUser user, @NotNull User newOwner, boolean forceTransfer) {
        getPlugin().runSync(() -> {
            // Get the entity the player is looking at to transfer
            final Player player = ((BukkitUser) user).getBukkitPlayer();
            final Optional<Tameable> lookingAt = getLookingAtTamed(player);
            final Optional<User> owner = lookingAt.flatMap(this::getPetOwner);
            if (lookingAt.isEmpty() || owner.isEmpty()) {
                getPlugin().getLocales().getLocale("error_look_at_pet_transfer")
                        .ifPresent(user::sendMessage);
                return;
            }

            // Transfer the pet if the user is allowed to
            if (forceTransfer || !cancelPetOperation(user, owner.get())) {
                this.transferPet(lookingAt.get(), user, newOwner);
            }
        });
    }

    private void transferPet(@NotNull Tameable tamed, @NotNull OnlineUser owner, @NotNull User newOwner) {
        final OfflinePlayer offline = ((BukkitHuskClaims) getPlugin()).getServer().getOfflinePlayer(newOwner.getUuid());
        tamed.setOwner(offline);
        getPlugin().getLocales().getLocale("pet_transferred", tamed.getName(), newOwner.getName())
                .ifPresent(owner::sendMessage);
    }

    // Get the User owner of a pet from a Tameable entity
    default Optional<User> getPetOwner(@NotNull Tameable entity) {
        final AnimalTamer owner = entity.getOwner();
        if (!entity.isTamed() || owner == null) {
            return Optional.empty();
        }

        return Optional.of(User.of(owner.getUniqueId(), owner.getName() == null
                ? getPlugin().getLocales().getNotApplicable() : owner.getName()));
    }

    private Optional<Tameable> getLookingAtTamed(@NotNull Player player) {
        return player.getNearbyEntities(TRANSFER_RANGE, TRANSFER_RANGE, TRANSFER_RANGE).stream()
                .filter(entity -> entity instanceof Tameable)
                .map(entity -> (Tameable) entity)
                .filter(entity -> entity.getLocation().toVector()
                        .subtract(player.getEyeLocation().toVector())
                        .normalize().dot(player.getEyeLocation().getDirection()) > TRANSFER_ENTITY_DOT)
                .min(Comparator.comparing(entity -> entity.getLocation().distance(player.getLocation())));
    }

}
