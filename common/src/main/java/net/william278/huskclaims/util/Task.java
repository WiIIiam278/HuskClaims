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

import net.william278.huskclaims.HuskClaims;
import org.jetbrains.annotations.NotNull;

import java.time.Duration;
import java.util.concurrent.CompletableFuture;

/**
 * Cross-platform task scheduling implementation
 *
 * @since 1.0
 */
public interface Task extends Runnable {

    abstract class Base implements Task {

        protected final HuskClaims plugin;
        protected final Runnable runnable;
        protected boolean cancelled = false;

        protected Base(@NotNull HuskClaims plugin, @NotNull Runnable runnable) {
            this.plugin = plugin;
            this.runnable = runnable;
        }

        public void cancel() {
            cancelled = true;
        }

        @NotNull
        @Override
        public HuskClaims getPlugin() {
            return plugin;
        }

    }

    abstract class Async extends Base {

        protected Async(@NotNull HuskClaims plugin, @NotNull Runnable runnable) {
            super(plugin, runnable);
        }

    }

    abstract class Sync extends Base {

        protected Duration initialDelay;

        protected Sync(@NotNull HuskClaims plugin, @NotNull Runnable runnable, @NotNull Duration initialDelay) {
            super(plugin, runnable);
            this.initialDelay = initialDelay;
        }

    }

    abstract class Repeating extends Base {

        protected final Duration repeatPeriod;
        protected final Duration initialDelay;

        protected Repeating(@NotNull HuskClaims plugin, @NotNull Runnable runnable,
                            @NotNull Duration repeatPeriod, @NotNull Duration initialDelay) {
            super(plugin, runnable);
            this.repeatPeriod = repeatPeriod;
            this.initialDelay = initialDelay;
        }

    }

    @SuppressWarnings("UnusedReturnValue")
    interface Supplier {

        @NotNull
        Task.Sync getSyncTask(@NotNull Runnable runnable, @NotNull Duration initialDelay);

        @NotNull
        Task.Async getAsyncTask(@NotNull Runnable runnable);

        @NotNull
        Task.Repeating getRepeatingTask(@NotNull Runnable runnable, @NotNull Duration repeatPeriod,
                                        @NotNull Duration initialDelay);

        @NotNull
        default Task.Sync runSyncDelayed(@NotNull Runnable runnable, Duration initialDelay) {
            final Task.Sync task = getSyncTask(runnable, initialDelay);
            task.run();
            return task;
        }

        @NotNull
        default Task.Sync runSync(@NotNull Runnable runnable) {
            return runSyncDelayed(runnable, Duration.ZERO);
        }

        @NotNull
        default Task.Async runAsync(@NotNull Runnable runnable) {
            final Task.Async task = getAsyncTask(runnable);
            task.run();
            return task;
        }

        default <T> CompletableFuture<T> supplyAsync(@NotNull java.util.function.Supplier<T> supplier) {
            final CompletableFuture<T> future = new CompletableFuture<>();
            runAsync(() -> {
                try {
                    future.complete(supplier.get());
                } catch (Throwable throwable) {
                    future.completeExceptionally(throwable);
                }
            });
            return future;
        }

        void cancelTasks();

        @NotNull
        HuskClaims getPlugin();

    }

    @NotNull
    HuskClaims getPlugin();

}
