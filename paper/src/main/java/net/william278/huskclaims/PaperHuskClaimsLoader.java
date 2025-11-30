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

package net.william278.huskclaims;

import io.papermc.paper.plugin.loader.PluginClasspathBuilder;
import io.papermc.paper.plugin.loader.PluginLoader;
import io.papermc.paper.plugin.loader.library.impl.MavenLibraryResolver;
import org.eclipse.aether.artifact.DefaultArtifact;
import org.eclipse.aether.graph.Dependency;
import org.eclipse.aether.repository.RemoteRepository;
import org.jetbrains.annotations.NotNull;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.regex.Pattern;
import java.util.stream.Stream;

@SuppressWarnings("UnstableApiUsage")
public class PaperHuskClaimsLoader implements PluginLoader {

    @Override
    public void classloader(@NotNull PluginClasspathBuilder classpathBuilder) {
        final MavenLibraryResolver resolver = new MavenLibraryResolver();

        resolveLibraries(classpathBuilder).stream()
                .map(DefaultArtifact::new)
                .forEach(artifact -> resolver.addDependency(new Dependency(artifact, null)));
        resolver.addRepository(new RemoteRepository.Builder("maven", "default", getMavenUrl()).build());

        classpathBuilder.addLibrary(resolver);
    }

    @NotNull
    private static String getMavenUrl() {
        return Stream.of(
                System.getenv("PAPER_DEFAULT_CENTRAL_REPOSITORY"),
                System.getProperty("org.bukkit.plugin.java.LibraryLoader.centralURL"),
                "https://maven-central.storage-download.googleapis.com/maven2"
        ).filter(Objects::nonNull).findFirst().orElseThrow(IllegalStateException::new);
    }

    @NotNull
    private static List<String> resolveLibraries(@NotNull PluginClasspathBuilder classpathBuilder) {
        try (InputStream input = getLibraryListFile()) {
            if (input == null) {
                classpathBuilder.getContext().getLogger().warn("Failed to read paper-libraries.yml, using defaults");
                return getDefaultLibraries();
            }
            return parseYamlLibraries(input);
        } catch (Throwable e) {
            classpathBuilder.getContext().getLogger().error("Failed to resolve libraries", e);
            return getDefaultLibraries();
        }
    }

    @NotNull
    private static InputStream getLibraryListFile() {
        return PaperHuskClaimsLoader.class.getClassLoader().getResourceAsStream("paper-libraries.yml");
    }

    @NotNull
    private static List<String> parseYamlLibraries(@NotNull InputStream input) throws Exception {
        final List<String> libraries = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(input, StandardCharsets.UTF_8))) {
            String line;
            boolean inLibrariesSection = false;
            final Pattern libraryPattern = Pattern.compile("^\\s*-\\s*(.+)$");
            final Pattern listStartPattern = Pattern.compile("^\\s*libraries:\\s*$");

            while ((line = reader.readLine()) != null) {
                // Skip comments
                if (line.trim().startsWith("#")) {
                    continue;
                }

                // Check if we're entering the libraries section
                if (listStartPattern.matcher(line).matches()) {
                    inLibrariesSection = true;
                    continue;
                }

                // If we're in the libraries section, parse library entries
                if (inLibrariesSection) {
                    final java.util.regex.Matcher matcher = libraryPattern.matcher(line);
                    if (matcher.matches()) {
                        String library = matcher.group(1).trim();
                        // Remove quotes if present
                        if ((library.startsWith("'") && library.endsWith("'")) ||
                            (library.startsWith("\"") && library.endsWith("\""))) {
                            library = library.substring(1, library.length() - 1);
                        }
                        // Replace ${variable} placeholders (basic implementation)
                        library = library.replace("${jedis_version}", "6.0.0")
                                .replace("${mysql_driver_version}", "9.3.0")
                                .replace("${mariadb_driver_version}", "3.5.3")
                                .replace("${sqlite_driver_version}", "3.49.1.0");
                        libraries.add(library);
                    } else if (!line.trim().isEmpty() && !line.startsWith(" ")) {
                        // If we hit a non-indented line, we've left the libraries section
                        break;
                    }
                }
            }
        }
        return libraries.isEmpty() ? getDefaultLibraries() : libraries;
    }

    @NotNull
    private static List<String> getDefaultLibraries() {
        return List.of(
                "de.exlll:configlib-yaml:4.6.1",
                "redis.clients:jedis:6.0.0",
                "com.mysql:mysql-connector-j:9.3.0",
                "org.mariadb.jdbc:mariadb-java-client:3.5.3",
                "org.xerial:sqlite-jdbc:3.49.1.0"
        );
    }

}