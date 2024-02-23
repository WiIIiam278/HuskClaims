HuskClaims offers a simple tamed animal ("pet") protection and transfer system. This system is enabled by default and can be toggled by editing the `pets` section of the config file:

<details>
<summary>Pets &mdash; config.yml</summary>

```yaml
# Settings for protecting tamed animals (pets). Docs: https://william278.net/docs/huskclaims/pets
pets:
  # Whether to enable protecting tamed animals to only be harmed/used by their owner
  enabled: true
```
</details>

Disabling pets will also disable the `/transferpet` command.

## Protecting pets
Pets will automatically be protected from harm by other players when they are tamed.

## Transferring pets
> **Note:** The recipient of a pet has to have played on the server where the pet exists before.

You can transfer ownership of a pet to another player by looking directly at it and using the `/transferpet <username>` command. This will allow the new owner to interact with the pet as if they had tamed it themselves.

Admins can transfer animals tamed by other players with the `huskclaims.command.transferpet.other` permission.
