HuskClaims supports automatically pruning/expiring claims made by users if they have not logged in within a configurable period of time (in days). After being enabled, pruning will take place on each server during startup. Claims created by inactive users will have their claim blocks refunded.

Please note that inactivity pruning will begin occurring immediately after enabling the setting. Take caution when turning this on, and consider making a backup of your database if necessary.

## Automatic pruning
To enable automatic claim pruning, edit the `inactivity_pruning` part of the claims section of your [`config.yml` file](config), setting `enabled` to true.

<details>
<summary>Automatic pruning (config.yml)</summary>

```yaml
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
```
</details>

Here, you can additionally customize the number of days a user must not log in for them to be considered inactive (defaults to 60 days), and provide a list of claim world names that should not be pruned. 

<details>
<summary>Further pruning customisation (config.yml)</summary>

```yaml
    # List of worlds to exclude from being pruned.
    excluded_worlds: [ 'world_the_end' ]
    # List of users (by either UUID or username) to exclude from inactive claim pruning
    excluded_users: [ 'William278', '2c9e9697-0517-4e7a-825d-916a2eaebd64' ]
```
</details>

Finally, you may supply a list of UUIDs _or_ usernames of users to exclude from automatic pruning.