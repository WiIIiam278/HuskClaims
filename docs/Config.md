This page contains the configuration structure for HuskClaims.

## Configuration structure
📁 `plugins/HuskClaims/`
- 📄 `config.yml`: General plugin configuration
- 📄 `server.yml`: Server ID configuration
- 📄 `trust_levels.yml`: Trust levels configuration (see [Trust levels](trust#trust-levels))
- 📄 `messages-xx-xx.yml`: Plugin locales, formatted in MineDown (see [[Translations]])

## Example files
<details>
<summary>config.yml</summary>

```yaml
# ┏━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━┓
# ┃      HuskClaims - Config     ┃
# ┃    Developed by William278   ┃
# ┣━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━┛
# ┣╸ Information: https://william278.net/project/huskclaims/
# ┣╸ Config Help: https://william278.net/docs/huskclaims/config/
# ┗╸ Documentation: https://william278.net/docs/huskclaims/

# Locale of the default language file to use. Docs: https://william278.net/docs/huskclaims/translations
language: en-gb
# Whether to automatically check for plugin updates on startup
check_for_updates: true
# Database settings
database:
  # Type of database to use (SQLITE, MYSQL or MARIADB)
  type: SQLITE
  # Specify credentials here if you are using MYSQL or MARIADB
  credentials:
    host: localhost
    port: 3306
    database: huskclaims
    username: root
    password: pa55w0rd
    parameters: ?autoReconnect=true&useSSL=false&useUnicode=true&characterEncoding=UTF-8&allowPublicKeyRetrieval=true
  # MYSQL / MARIADB database Hikari connection pool properties
  # Don't modify this unless you know what you're doing!
  pool_options:
    size: 12
    idle: 12
    lifetime: 1800000
    keep_alive: 30000
    timeout: 20000
  # Names of tables to use on your database. Don't modify this unless you know what you're doing!
  table_names:
    META_DATA: huskclaims_metadata
    USER_DATA: huskclaims_users
    USER_GROUP_DATA: huskclaims_user_groups
    CLAIM_DATA: huskclaims_claim_worlds
# Cross-server settings
cross_server:
  # Whether to enable cross-server mode
  enabled: false
  # The cluster ID, for if you're networking multiple separate groups of HuskClaims-enabled servers.
  # Do not change unless you know what you're doing
  cluster_id: main
  # Type of network message broker to ues for data synchronization (PLUGIN_MESSAGE or REDIS)
  broker_type: PLUGIN_MESSAGE
  # Settings for if you're using REDIS as your message broker
  redis:
    host: localhost
    port: 6379
    # Password for your Redis server. Leave blank if you're not using a password.
    password: ''
    use_ssl: false
    # Settings for if you're using Redis Sentinels.
    # If you're not sure what this is, please ignore this section.
    sentinel:
      master_name: ''
      # List of host:port pairs
      nodes: []
      password: ''
# Claim flags & world settings
claims:
  # Default flags for regular claims
  default_flags:
  - PLAYER_DAMAGE_MONSTER
  - EXPLOSION_DAMAGE_ENTITY
  - PLAYER_DAMAGE_PLAYER
  - MONSTER_SPAWN
  - PASSIVE_MOB_SPAWN
  # Default flags for admin claims
  admin_flags:
  - PLAYER_DAMAGE_MONSTER
  - EXPLOSION_DAMAGE_ENTITY
  - PLAYER_DAMAGE_PLAYER
  - MONSTER_SPAWN
  - PASSIVE_MOB_SPAWN
  # List of enabled claim types. Must include at least the regular "CLAIMS" mode
  enabled_claiming_modes:
  - CLAIMS
  - CHILD_CLAIMS
  - ADMIN_CLAIMS
  # Default flags for the wilderness (outside claims)
  wilderness_rules:
  - BLOCK_PLACE
  - BLOCK_BREAK
  - BLOCK_INTERACT
  - REDSTONE_INTERACT
  - FARM_BLOCK_BREAK
  - FARM_BLOCK_PLACE
  - PLAYER_DAMAGE_PLAYER
  - PLAYER_DAMAGE_MONSTER
  - PLAYER_DAMAGE_ENTITY
  - PLAYER_DAMAGE_PERSISTENT_ENTITY
  - MONSTER_SPAWN
  - PASSIVE_MOB_SPAWN
  - MONSTER_DAMAGE_TERRAIN
  - EXPLOSION_DAMAGE_TERRAIN
  - EXPLOSION_DAMAGE_ENTITY
  - FIRE_BURN
  - FIRE_SPREAD
  - FILL_BUCKET
  - EMPTY_BUCKET
  - PLACE_HANGING_ENTITY
  - BREAK_HANGING_ENTITY
  - ENTITY_INTERACT
  - FARM_BLOCK_INTERACT
  - USE_SPAWN_EGG
  - ENDER_PEARL_TELEPORT
  - CONTAINER_OPEN
  # List of worlds where users cannot claim
  unclaimable_worlds: []
  # The number of claim blocks a user gets when they first join the server
  starting_claim_blocks: 100
  # The number of claim blocks a user gets hourly.
  # Override with the "huskclaims.hourly_blocks.(amount)" permission
  hourly_claim_blocks: 100
  # Claim inspection tool (right click with this to inspect claims)
  inspection_tool: minecraft:stick
  # Claim creation & resize tool (right click with this to create/resize claims)
  claim_tool: minecraft:golden_shovel
  # Require players to hold the claim tool to use claim commands (e.g. /claim <radius>, /extendclaim)
  require_tool_for_commands: true
  # Minimum size of claims. This does not affect child or admin claims.
  minimum_claim_size: 5
  # Max range of inspector tools
  inspection_distance: 64
  # Whether to allow inspecting nearby claims by sneaking when using the inspection tool
  allow_nearby_claim_inspection: true
  # Whether to require confirmation when deleting claims that have children
  confirm_deleting_parent_claims: true
  # Whether to enable the /trapped command. Install HuskHomes to require a warmup before teleporting.
  trapped_command: true
  # Settings for automatically removing claims made by now-inactive users
  inactivity_pruning:
    # Whether to delete all claims made by users marked as inactive. (Warning: Dangerous!)
    enabled: false
    # The number of days a user must not log on for to be marked as inactive (Minimum: 1)
    inactive_days: 60
    # List of worlds to exclude from being pruned.
    excluded_worlds: []
    # List of users (by either UUID or username) to exclude from inactive claim pruning
    excluded_users: []
  # Settings for banning users from claims
  bans:
    # Whether to let users ban others from their claims (prevent them from entering) using /claimban
    # Also requires the MANAGE_BANS privilege (by default, restricted to those with 'manage' trust)
    enabled: false
# Groups of operations that can be toggled on/off in claims
operation_groups:
- name: Claim Explosions
  description: Toggle whether explosions can damage terrain in claims
  toggle_command_aliases:
  - claimexplosions
  allowed_operations:
  - EXPLOSION_DAMAGE_TERRAIN
  - MONSTER_DAMAGE_TERRAIN
# Settings for user groups, letting users quickly manage trust for groups of multiple players at once
user_groups:
  # Whether to enable user groups
  enabled: true
  # The prefix to use when specifying a group in a trust command (e.g. /trust @groupname)
  group_specifier_prefix: '@'
  # Whether to restrict group names with a regex filter
  restrict_group_names: true
  # Regex for group names
  group_name_regex: '[a-zA-Z0-9-_]*'
  # Max members per group
  max_members_per_group: 10
  # Max groups per player
  max_groups_per_player: 3
# Settings for trust tags, special representations of things you can trust in a claim
trust_tags:
  # Whether to enable trust tags
  enabled: true
  # The prefix to use when specifying a trust tag in a trust command (e.g. /trust #tagname)
  tag_specifier_prefix: '#'
  # The name of the default public access tag (to let anyone access certain claim levels)
  public_access_name: public
# Settings for the claim inspection/creation highlighter
highlighter:
  # Whether to use block display entities for glowing (requires Paper 1.19.4+)
  block_displays: true
  # If using block displays, whether highlights should use a glow effect (requires Paper 1.19.4+)
  glow_effect: true
  # The blocks to use when highlighting claims
  block_types:
    CORNER: minecraft:glowstone
    EDGE: minecraft:gold_block
    CHILD_CORNER: minecraft:iron_block
    CHILD_EDGE: minecraft:white_wool
    ADMIN_CORNER: minecraft:glowstone
    ADMIN_EDGE: minecraft:pumpkin
    OVERLAP_CORNER: minecraft:red_nether_bricks
    OVERLAP_EDGE: minecraft:netherrack
    SELECTION: minecraft:diamond_block
  # The color of the glow effect used for blocks when highlighting claims
  glow_colors:
    CORNER: YELLOW
    EDGE: YELLOW
    CHILD_CORNER: SILVER
    CHILD_EDGE: SILVER
    ADMIN_CORNER: ORANGE
    ADMIN_EDGE: ORANGE
    OVERLAP_CORNER: RED
    OVERLAP_EDGE: RED
    SELECTION: AQUA
# Settings for protecting tamed animals (pets). Docs: https://william278.net/docs/huskclaims/pets
pets:
  # Whether to enable protecting tamed animals to only be harmed/used by their owner
  enabled: true
# Settings for moderation tools
moderation:
  signs:
    # Whether to notify users with /signspy on when signs are placed.edited. Requires Minecraft 1.19.4+Requires Minecraft 1.19.4+
    notify_moderators: true
    # Whether to filter messages
    filter_messages: false
    # Whether sign notifications should be limited to just filtered signs
    only_notify_if_filtered: false
    # Single character to replace filtered message content with
    replacement_character: '#'
    # List of words to filter out of signs
    filtered_words: []
  drops:
    # Whether to lock ground items dropped by players when they die from being picked up by others
    lock_items: false
    # Whether to also prevent death drops from being destroyed by lava, fire, cacti, etc.
    prevent_destruction: true
# Settings for integration hooks with other plugins
hooks:
  luck_perms:
    # Whether to hook into LuckPerms for permission group trust tags
    enabled: true
    # Require users to have the "huskclaims.trust.luckperms" permission to use LuckPerms trust tags
    trust_tag_use_permission: true
    # The prefix to use when specifying a LuckPerms group trust tag (e.g. /trust #role/groupname)
    trust_tag_prefix: role/
  plan:
    # Whether to hook into Plan to display claim analytics
    enabled: true
  husk_homes:
    # Whether to hook into HuskHomes for claim teleportation and home restriction
    enabled: true
    # Whether to restrict setting a home in claims to a trust level
    restrict_set_home: true
    # The trust level required to set a home in a claim (the ID of a level in 'trust_levels.yml')
    set_home_trust_level: access
  husk_towns:
    # Whether to hook into HuskTowns to disable claiming over town claims
    enabled: true
  economy:
    # Whether to hook into an economy plugin to allow buying claim blocks
    enabled: true
    # The cost of buying 1 claim block
    cost_per_block: 1.0
  placeholders:
    # Whether to hook into PlaceholderAPI to provide a HuskClaims placeholder expansion
    enabled: true
  map:
    # Whether to hook into Dynmap, BlueMap, or Pl3xMap to show claims on the map
    enabled: true
    # What colors to use for types of claims on the map. Remove a pairing to hide claims of that type.
    colors:
      CLAIMS: '#ffff55'
      CHILD_CLAIMS: '#ffffff'
      ADMIN_CLAIMS: '#aa0000'
    # The name of the marker set key
    marker_set_name: Claims
    # The label format for markers. '%s' will be replaced with the claim owner's name
    label_format: Claim by %s
```

</details>

<details>
<summary>server.yml</summary>

```yaml
# ┏━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━┓
# ┃  HuskClaims Server ID config ┃
# ┃    Developed by William278   ┃
# ┣━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━┛
# ┣╸ This file should contain the ID of this server as defined in your proxy config.
# ┗╸ If you join it using /server alpha, then set it to 'alpha' (case-sensitive)
name: beta
```

</details>