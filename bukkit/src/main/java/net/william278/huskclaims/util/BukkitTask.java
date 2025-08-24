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

package net.william278.huskclaims.util;

import net.william278.huskclaims.BukkitHuskClaims;
import net.william278.huskclaims.HuskClaims;
import net.william278.huskclaims.position.Position;
import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.jetbrains.annotations.NotNull;
import space.arim.morepaperlib.scheduling.GracefulScheduling;
import space.arim.morepaperlib.scheduling.ScheduledTask;

import java.time.Duration;

public interface BukkitTask extends Task {

    class Sync extends Task.Sync implements BukkitTask {

        private ScheduledTask task;

        protected Sync(@NotNull HuskClaims plugin, @NotNull Runnable runnable, @NotNull Duration initialDelay) {
            super(plugin, runnable, initialDelay);
        }

        @Override
        public void cancel() {
            if (task != null && !cancelled) {
                task.cancel();
            }
            super.cancel();
        }

        @Override
        public void run() {
            if (isPluginDisabled()) {
                runnable.run();
                return;
            }

            if (initialDelay.compareTo(Duration.ZERO) > 0) {
                this.task = getScheduler().globalRegionalScheduler().runDelayed(
                        runnable, initialDelay.toMillis() / 50
                );
            } else {
                if (getScheduler().isOnGlobalRegionThread()) {
                    runnable.run();
                    return;
                }
                this.task = getScheduler().globalRegionalScheduler().run(runnable);
            }
        }
    }

    class Async extends Task.Async implements BukkitTask {

        private ScheduledTask task;

        protected Async(@NotNull HuskClaims plugin, @NotNull Runnable runnable) {
            super(plugin, runnable);
        }

        @Override
        public void cancel() {
            if (task != null && !cancelled) {
                task.cancel();
            }
            super.cancel();
        }

        @Override
        public void run() {
            if (isPluginDisabled()) {
                runnable.run();
                return;
            }

            if (!cancelled) {
                this.task = getScheduler().asyncScheduler().run(runnable);
            }
        }
    }

    class Repeating extends Task.Repeating implements BukkitTask {

        private ScheduledTask task;

        protected Repeating(@NotNull HuskClaims plugin, @NotNull Runnable runnable,
                            Duration repeatingTicks, Duration delayTicks) {
            super(plugin, runnable, repeatingTicks, delayTicks);
        }

        @Override
        public void cancel() {
            if (task != null && !cancelled) {
                task.cancel();
            }
            super.cancel();
        }

        @Override
        public void run() {
            if (isPluginDisabled() || cancelled) {
                return;
            }
            this.task = getScheduler().asyncScheduler().runAtFixedRate(runnable, initialDelay, repeatPeriod);
        }
    }

    // Returns if the Bukkit HuskClaims plugin is disabled
    default boolean isPluginDisabled() {
        return !((BukkitHuskClaims) getPlugin()).isEnabled();
    }

    interface Supplier extends Task.Supplier {

        @NotNull
        @Override
        default Task.Sync getSyncTask(@NotNull Runnable runnable, @NotNull Duration initialDelay) {
            return new Sync(getPlugin(), runnable, initialDelay);
        }

        @NotNull
        @Override
        default Task.Async getAsyncTask(@NotNull Runnable runnable) {
            return new Async(getPlugin(), runnable);
        }

        @NotNull
        @Override
        default Task.Repeating getRepeatingTask(@NotNull Runnable runnable, @NotNull Duration repeatingTicks, @NotNull Duration delayTicks) {
            return new Repeating(getPlugin(), runnable, repeatingTicks, delayTicks);
        }

        @Override
        @NotNull
        default <T> Task.Sync runSync(T e, @NotNull Runnable runnable) {
            final BukkitHuskClaims bukkit = (BukkitHuskClaims) getPlugin();

            final BukkitTask.Sync task = new BukkitTask.Sync(bukkit, runnable, Duration.ZERO) {

                private ScheduledTask scheduled;

                @Override
                public void run() {
                    if (isPluginDisabled() || cancelled) {
                        return;
                    }

                    final GracefulScheduling scheduler = getScheduler();

                    if (e instanceof Entity entity) {
                        scheduled = scheduler.entitySpecificScheduler(entity).run(runnable, null);
                    } else if (e instanceof Location location) {
                        scheduled = scheduler.regionSpecificScheduler(location).run(runnable);
                    } else if (e instanceof Chunk chunk) {
                        scheduled = scheduler.regionSpecificScheduler(chunk.getWorld(), chunk.getX(), chunk.getZ()).run(runnable);
                    } else {
                        scheduled = scheduler.globalRegionalScheduler().run(runnable);
                    }
                }

                @Override
                public void cancel() {
                    if (!cancelled) {
                        if (scheduled != null) {
                            scheduled.cancel();
                        }
                        super.cancel();
                    }
                }
            };

            task.run();
            return task;
        }

        default void runSync(@NotNull Entity entity, @NotNull Runnable runnable) {
            runSync((Object) entity, runnable);
        }

        @NotNull
        default Task.Sync runSync(@NotNull Location location, @NotNull Runnable runnable) {
            return runSync((Object) location, runnable);
        }

        @Override
        @NotNull
        default Task.Sync runSync(Position position, @NotNull Runnable runnable) {
            return runSync(new Location(Bukkit.getWorld(position.getWorld().getName()),
                    position.getX(), position.getY(), position.getZ()), runnable);
        }

        @Override
        default void cancelTasks() {
            final BukkitHuskClaims plugin = (BukkitHuskClaims) getPlugin();
            getTaskQueue().forEach(Runnable::run);
            getTaskQueue().clear();
            plugin.getMorePaperLib().scheduling().cancelGlobalTasks();
        }

    }

    @NotNull
    default GracefulScheduling getScheduler() {
        return ((BukkitHuskClaims) getPlugin()).getMorePaperLib().scheduling();
    }

}
