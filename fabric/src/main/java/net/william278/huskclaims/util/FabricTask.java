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

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import net.william278.huskclaims.FabricHuskClaims;
import net.william278.huskclaims.HuskClaims;
import org.jetbrains.annotations.NotNull;

import java.time.Duration;
import java.util.concurrent.*;

public interface FabricTask extends Task {

    ScheduledExecutorService ASYNC_EXEC = Executors.newScheduledThreadPool(4,
            new ThreadFactoryBuilder()
                    .setDaemon(true)
                    .setNameFormat("HuskClaims-ThreadPool")
                    .build()
    );

    class Sync extends Task.Sync implements FabricTask {

        protected Sync(@NotNull HuskClaims plugin, @NotNull Runnable runnable, @NotNull Duration initialDelay) {
            super(plugin, runnable, initialDelay);
        }

        @Override
        public void cancel() {
            super.cancel();
        }

        @Override
        public void run() {
            if (!cancelled) {
                ASYNC_EXEC.schedule(() -> ((FabricHuskClaims) getPlugin()).getMinecraftServer().executeSync(runnable),
                        initialDelay.toMillis(),
                        TimeUnit.MILLISECONDS);
            }
        }
    }

    class Async extends Task.Async implements FabricTask {
        private CompletableFuture<Void> task;

        protected Async(@NotNull HuskClaims plugin, @NotNull Runnable runnable) {
            super(plugin, runnable);
        }

        @Override
        public void cancel() {
            if (task != null && !cancelled) {
                task.cancel(true);
            }
            super.cancel();
        }

        @Override
        public void run() {
            if (!cancelled) {
                this.task = CompletableFuture.runAsync(runnable, ASYNC_EXEC);
            }
        }
    }

    class Repeating extends Task.Repeating implements FabricTask {

        private ScheduledFuture<?> task;

        protected Repeating(@NotNull HuskClaims plugin, @NotNull Runnable runnable,
                            Duration repeatingTicks, Duration delayTicks) {
            super(plugin, runnable, repeatingTicks, delayTicks);
        }

        @Override
        public void cancel() {
            if (task != null && !cancelled) {
                task.cancel(true);
            }
            super.cancel();
        }

        @Override
        public void run() {
            if (!cancelled) {
                this.task = ASYNC_EXEC.scheduleAtFixedRate(
                        runnable,
                        initialDelay.toMillis(),
                        repeatPeriod.toMillis(),
                        TimeUnit.MILLISECONDS
                );
            }
        }
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
        default void cancelTasks() {
            ASYNC_EXEC.shutdownNow();
        }

    }

}