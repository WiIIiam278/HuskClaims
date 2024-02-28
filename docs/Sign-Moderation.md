HuskClaims offers a moderation feature for spying on the text contents of signs and hanging signs placed by players, and automatically filtering contents against a prohibited words list. Sign moderation works on all types of text-entry signs (hanging signs, wall signs, and standing signs&mdash;and on both sides) and requires a Minecraft 1.19.4+ Paper server. This system can be configured under the `moderation.signs` section of `config.yml`:

<details>
<summary>Sign Moderation &mdash; config.yml</summary>

```yaml
  signs:
    # Whether to notify users with /signspy on when signs are placed.edited. Requires Minecraft 1.19.4+
    notify_moderators: true
    # Whether to filter messages
    filter_messages: false
    # Whether sign notifications should be limited to just filtered signs
    only_notify_if_filtered: false
    # Single character to replace filtered message content with
    replacement_character: '#'
    # List of words to filter out of signs
    filtered_words: []
```
</details>

## Sign spying
To start sign spying, use `/signspy [on|off]` (see [[Commands]]). This requires the `huskclaims.command.signspy` permission to use. You will then begin receiving messages whenever users place a sign or edit sign text (except if the sign is blank) &mdash; this includes signs placed on other servers if you are using cross-server mode.

If you click on the location of a sign in the chat message, you will be teleported to the sign's location if the [HuskHomes hook is in use](hooks#HuskHomes).

## Sign filtering
You can choose to filter signs against a configured list of prohibited words. If a sign contains any of the prohibited words, the words in the sign text will automatically be censored and replaced with a chosen glyph. The user who placed the sign will also be informed their sign was filtered in chat.

When a sign is filtered, a notification warning will appear for all those spying on signs. You can additionally choose to only have moderators spy on signs that have required filtering to reduce clutter in chat.