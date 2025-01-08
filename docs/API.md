The HuskClaims API provides methods for interfacing with and editing claims and users, alongside a selection of API events for listening to when players perform certain actions.

## Compatibility
[![Maven](https://repo.william278.net/api/badge/latest/releases/net/william278/huskclaims/huskclaims-common?color=00fb9a&name=Maven&prefix=v)](https://repo.william278.net/#/releases/net/william278/huskclaims/)

The HuskClaims API shares version numbering with the plugin itself for consistency and convenience. Please note minor and patch plugin releases may make API additions and deprecations, but will not introduce breaking changes without notice.

| API Version | HuskClaims Versions  | Supported |
|:-----------:|:--------------------:|:---------:|
|    v1.x     | _v1.0&mdash;Current_ |     ✅     |


### Platforms
The HuskClaims API is available for the following platforms:

* `bukkit` - Bukkit, Spigot, Paper, etc. Provides Bukkit API event listeners and adapters to `org.bukkit` objects.
* `fabric` - Fabric, Quilt, etc. Provides Fabric API event callbacks and adapters to `net.minecraft` objects.
* `common` - Common API for all platforms.

## Table of contents
1. Adding the API to your project
2. Adding HuskClaims as a dependency
3. Next steps

## API Introduction
### 1.1 Setup with Maven
<details>
<summary>Maven setup information</summary>

Add the repository to your `pom.xml` as per below. You can alternatively specify `/snapshots` for the repository containing the latest development builds (not recommended).
```xml
<repositories>
    <repository>
        <id>william278.net</id>
        <url>https://repo.william278.net/releases</url>
    </repository>
</repositories>
```
Add the dependency to your `pom.xml` as per below. Replace `PLATFORM` with your target platform (see above) and `VERSION` with the latest version of HuskClaims (without the v): ![Latest version](https://img.shields.io/github/v/tag/WiIIiam278/HuskClaims?color=%23282828&label=%20&style=flat-square). Note for Fabric you must append the target Minecraft version to the version number (e.g. `1.5+1.21.1`).
```xml
<dependency>
    <groupId>net.william278.huskclaims</groupId>
    <artifactId>huskclaims-PLATFORM</artifactId>
    <version>VERSION</version>
    <scope>provided</scope>
</dependency>
```
</details>

### 1.2 Setup with Gradle
<details>
<summary>Gradle setup information</summary>

Add the dependency as per below to your `build.gradle`. You can alternatively specify `/snapshots` for the repository containing the latest development builds (not recommended).
```groovy
allprojects {
	repositories {
		maven { url 'https://repo.william278.net/releases' }
	}
}
```
Add the dependency as per below. Replace `PLATFORM` with your target platform (see above) and `VERSION` with the latest version of HuskClaims (without the v): ![Latest version](https://img.shields.io/github/v/tag/WiIIiam278/HuskClaims?color=%23282828&label=%20&style=flat-square). Note for Fabric you must append the target Minecraft version to the version number (e.g. `1.5+1.21.1`).

```groovy
dependencies {
    compileOnly 'net.william278.huskclaims:huskclaims-PLATFORM:VERSION'
}
```
</details>

### 2. Adding HuskClaims as a Bukkit plugin dependency
Add HuskClaims to your `softdepend` (if you want to optionally use HuskClaims) or `depend` (if your plugin relies on HuskClaims) section in `plugin.yml` of your project.

```yaml
name: MyPlugin
version: 1.0
main: net.william278.myplugin.MyPlugin
author: William278
description: 'A plugin that hooks with the HuskClaims API!'
softdepend: # Or, use 'depend' here
  - HuskClaims
```

## 3. Creating a class to interface with the API
- Unless your plugin completely relies on HuskClaims, you shouldn't put HuskClaims API calls into your main class, otherwise if HuskClaims is not installed you'll encounter `ClassNotFoundException`s

```java
public class HuskClaimsAPIHook {

    public HuskClaimsAPIHook() {
        // Ready to do stuff with the API
    }

}
```
## 4. Checking if HuskClaims is present and creating the hook
- Check to make sure the HuskClaims plugin is present before instantiating the API hook class

```java
public class MyPlugin extends JavaPlugin {

    public HuskClaimsAPIHook huskClaimsAPIHook;

    @Override
    public void onEnable() {
        if (Bukkit.getPluginManager().getPlugin("HuskClaims") != null) {
            this.huskClaimsAPIHook = new HuskClaimsAPIHook();
        }
    }
}
```

## 5. Getting an instance of the API
- You can now get the API instance by calling `HuskClaimsAPI#getInstance()`

```java
import net.william278.huskclaims.api.BukkitHuskClaimsAPI;

public class HuskClaimsAPIHook {

    private final BukkitHuskClaimsAPI huskClaimsAPI;

    public HuskClaimsAPIHook() {
        this.huskClaimsAPI = BukkitHuskClaimsAPI.getInstance(); // Or, HuskClaimsAPI.getInstance() for the common API
    }

}
```

## 6. CompletableFuture and Optional basics
- HuskClaims's API methods often deal with `CompletableFuture`s and `Optional`s.
- A `CompletableFuture` is an asynchronous callback mechanism. The method will be processed asynchronously and the data returned when it has been retrieved. Then, use `CompletableFuture#thenAccept(data -> {})` to do what you want to do with the `data` you requested after it has asynchronously been retrieved, to prevent lag.
- An `Optional` is a null-safe representation of data, or no data. You can check if the Optional is empty via `Optional#isEmpty()` (which will be returned by the API if no data could be found for the call you made). If the optional does contain data, you can get it via `Optional#get().

> **Warning:** You should never call `#join()` on futures returned from the HuskClaims API as futures are processed on server asynchronous tasks, which could lead to thread deadlock and crash your server if you attempt to lock the main thread to process them.

### 7. Next steps
Now that you've got everything ready, you can start doing stuff with the HuskClaims API!
- [[Claims API]] &mdash; Get, create, resize, & delete [[claims]], child claims, and admin claims, and manage [[claim blocks]]
- [[Trust API]] &mdash; Trust users, groups and trust tags in claims, and provide custom [trust tags](trust#trust-tags)
- [[Highlighter API]] &mdash; Provide a custom claim highlighter
- [[Operations API]] &mdash; Register operation types and create Operations using them to have HuskClaims protect against custom actions 
- [[API Events]] &mdash; Listen to events when players perform certain actions
