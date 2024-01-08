This will walk you through installing HuskClaims on either your Spigot server, or proxied network of Spigot servers.

## Requirements
* A Spigot-based Minecraft server (1.16.5 or higher, Java 16+)
* (For proxy network support) A proxy server (Velocity, BungeeCord) and MySQL (v8.0+) database
* (For optional redis support) A Redis database (v5.0+)

## Single-server Setup Instructions
These instructions are for simply installing HuskClaims on one Spigot/Paper server.

### 1. Install the jar
- Place the plugin jar file in the `/plugins/` directory of your Spigot server.
### 2. Restart the server and configure
- Start, then stop your server to let HuskClaims generate the config file.
- You can now edit the config files to your liking.
### 3. Turn on your server
- Start your server again and enjoy HuskClaims!

## Multi-server Setup Instructions
These instructions are for installing HuskClaims on multiple Spigot servers and having them network together. A MySQL database (v8.0+) is required.

### 1. Install the jar
- Place the plugin jar file in the `/plugins/` directory of each Spigot server.
- You don't need to install HuskClaims as a proxy plugin.
### 2. Restart the server and configure
- Start, then stop every server to let HuskClaims generate the config file.
- Advanced users: If you'd prefer, you can just create one config.yml file and create symbolic links in each `/plugins/HuskClaims/` folder to it to make updating it easier.
### 3. Configure servers to use cross-server mode
- Navigate to the HuskClaims general config file on each server (`~/plugins/HuskClaims/config.yml`)
- Under `database`, set `type` to `MYSQL`
- Under `mysql`/`credentials`, enter the credentials of your MySQL database server.
- Scroll down and look for the `cross_server` section. Set `enabled` to `true`.
- You can additionally configure a Redis server to use for network messaging, if you prefer (set the `broker_type` to `REDIS` if you do this).
- Save your config files. Make sure you've updated the files on every server.
### 4. Restart servers and set server.yml values
- Restart each server again. A `server.yml` file should generate inside (`~/plugins/HuskClaims/`)
- Set the `name` of the server in this file to the ID of this server as defined in the config of your proxy (e.g. if this is the "hub" server you access with `/server hub`, put "hub" here)
### 5. Restart your servers one last time
- Provided your MySQL database credentials were correct, your network should now be setup to use HuskClaims!
- You can delete the `HuskClaimsData.db` SQLite flat file that was generated, if you would like.

## Next steps
* [[Config]]
* [[Commands]]
* [[Permissions]]