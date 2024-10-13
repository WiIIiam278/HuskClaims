HuskClaims provides a suite of commands to aid claiming, including the ability to trust users. Use `/huskclaims help` to
view a list of commands in-game.

## Command Reference

<table align="right">
    <thead>
        <tr><th colspan="2">Key</th></tr>
    </thead>
    <tbody>
        <tr><td>✅</td><td>Accessible by all players by default</td></tr>
        <tr><td>❌</td><td>Accessible only by server operators by default</td></tr>
    </tbody>
</table>

This is a table of HuskClaims commands, how to use them, their required permission nodes, and whether the permission is granted by default. Check the [[Permissions]] page for more details on plugin permissions.

<table>
    <thead>
        <tr>
            <th colspan="2">Command</th>
            <th>Description</th>
            <th>Permission</th>
            <th>Default</th>
        </tr>
    </thead>
    <tbody>
        <!-- /claim command -->
        <tr><th colspan="5">Claiming Commands</th></tr>
        <tr><td colspan="5" align="center">Commands for claiming land&mdash;some require holding the claim tool to use.</td></tr>
        <tr>
            <td rowspan="2"><code>/claim</code></td>
            <td><code>/claim</code></td>
            <td>Toggle regular claiming mode</td>
            <td rowspan="2"><code>huskclaims.claim</code></td>
            <td align="center">✅</td>
        </tr>
        <tr>
            <td><code>/claim &lt;radius&gt;</code></td>
            <td>Create a claim around your position</td>
            <td align="center">✅</td>
        </tr>
        <!-- /childclaim command -->
        <tr>
            <td rowspan="2"><code>/childclaim</code></td>
            <td><code>/childclaim</code></td>
            <td>Toggle child claiming mode</td>
            <td rowspan="2"><code>huskclaims.child_claim</code></td>
            <td align="center">✅</td>
        </tr>
        <tr>
            <td><code>/childclaim &lt;radius&gt;</code></td>
            <td>Create a child claim in a parent claim around your position</td>
            <td align="center">✅</td>
        </tr>
        <!-- /adminclaim command -->
        <tr>
            <td rowspan="2"><code>/adminclaim</code></td>
            <td><code>/adminclaim</code></td>
            <td>Toggle admin claiming mode</td>
            <td rowspan="2"><code>huskclaims.admin_claim</code></td>
            <td align="center">❌</td>
        </tr>
        <tr>
            <td><code>/adminclaim &lt;radius&gt;</code></td>
            <td>Create an admin claim in around your position</td>
            <td align="center">❌</td>
        </tr>
        <!-- /claimslist command -->
        <tr>
            <td rowspan="2"><code>/claimslist</code></td>
            <td><code>/claimslist</code></td>
            <td>View the list of your claims</td>
            <td><code>huskclaims.command.claimslist</code></td>
            <td align="center">✅</td>
        </tr>
        <tr>
            <td><code>/claimslist &lt;username&gt;</code></td>
            <td>View the list of another player's claims</td>
            <td><code>huskclaims.command.claimslist.other</code></td>
            <td align="center">❌</td>
        </tr>
        <!-- /adminclaimslist command -->
        <tr>
            <td><code>/adminclaimslist</code></td>
            <td><code>/adminclaimslist</code></td>
            <td>View the list of admin claims</td>
            <td><code>huskclaims.command.adminclaimslist</code></td>
            <td align="center">❌</td>
        </tr>
        <!-- /extendclaim command -->
        <tr>
            <td rowspan="2"><code>/extendclaim</code></td>
            <td rowspan="2"><code>/extendclaim &lt;blocks&gt;</code></td>
            <td>Extend your claim by a number of blocks in the direction you are facing</td>
            <td><code>huskclaims.command.extendclaim</code></td>
            <td align="center">✅</td>
        </tr>
        <tr>
            <td>Extend another user's claim (uses your own claim blocks)</td>
            <td><code>huskclaims.command.extendclaim.other</code></td>
            <td align="center">❌</td>
        </tr>
        <!-- /unclaim command -->
        <tr>
            <td rowspan="2"><code>/unclaim</code></td>
            <td rowspan="2"><code>/unclaim</code></td>
            <td>Remove the claim you are standing in</td>
            <td><code>huskclaims.command.unclaim</code></td>
            <td align="center">✅</td>
        </tr>
        <tr>
            <td>Remove another user's claim that you are standing in</td>
            <td><code>huskclaims.command.unclaim.other</code></td>
            <td align="center">❌</td>
        </tr>
        <!-- /unclaimall command -->
        <tr>
            <td rowspan="2"><code>/unclaimall</code></td>
            <td><code>/unclaimall [confirm]</code></td>
            <td>Removes all your claims. If in admin claiming mode, removes all admin claims.</td>
            <td><code>huskclaims.command.unclaimall</code></td>
            <td align="center">✅</td>
        </tr>
        <tr>
            <td><code>/unclaimall &lt;username&gt; [confirm]</code></td>
            <td>Remove all of another user's claims.</td>
            <td><code>huskclaims.command.unclaimall.other</code></td>
            <td align="center">❌</td>
        </tr>
        <!-- /trust level commands -->
        <tr><th colspan="5">Trust level commands</th></tr>
        <tr><td colspan="5" align="center">Commands for granting access to the claim you are in. Trust levels are customizable; below are the default levels.</td></tr>
        <tr>
            <td rowspan="2"><code>/permissiontrust</code></td>
            <td rowspan="2"><code>/permissiontrust &lt;usernames|@groups|#tags&hellip;&gt;</code></td>
            <td>Grants management (trust others, make child claims) trust to users/groups/tags</td>
            <td><code>huskclaims.command.permissiontrust</code></td>
            <td align="center">✅</td>
        </tr>
        <tr>
            <td>Permission-trust a user on someone else's claim</td>
            <td><code>huskclaims.command.permissiontrust.other</code></td>
            <td align="center">❌</td>
        </tr>
        <tr>
            <td rowspan="2"><code>/trust</code></td>
            <td rowspan="2"><code>/trust &lt;usernames|@groups|#tags&hellip;&gt;</code></td>
            <td>Grant build (place/break blocks) trust to users/groups/tags</td>
            <td><code>huskclaims.command.trust</code></td>
            <td align="center">✅</td>
        </tr>
        <tr>
            <td>Build-trust a user on someone else's claim</td>
            <td><code>huskclaims.command.trust.other</code></td>
            <td align="center">❌</td>
        </tr>
        <tr>
            <td rowspan="2"><code>/containertrust</code></td>
            <td rowspan="2"><code>/containertrust &lt;usernames|@groups|#tags&hellip;&gt;</code></td>
            <td>Grant container (chests, hoppers, etc) trust to users/groups/tags</td>
            <td><code>huskclaims.command.containertrust</code></td>
            <td align="center">✅</td>
        </tr>
        <tr>
            <td>Container-trust a user on someone else's claim</td>
            <td><code>huskclaims.command.containertrust.other</code></td>
            <td align="center">❌</td>
        </tr>
        <tr>
            <td rowspan="2"><code>/accesstrust</code></td>
            <td rowspan="2"><code>/accesstrust &lt;usernames|@groups|#tags&hellip;&gt;</code></td>
            <td>Grant access (doors, buttons, etc) trust to users/groups/tags</td>
            <td><code>huskclaims.command.accesstrust</code></td>
            <td align="center">✅</td>
        </tr>
        <tr>
            <td>Access-trust a user on someone else's claim</td>
            <td><code>huskclaims.command.accesstrust.other</code></td>
            <td align="center">❌</td>
        </tr>
        <!-- /trustlist command -->
        <tr><th colspan="5">Claim management commands</th></tr>
        <tr><td colspan="5" align="center">Commands for managing the claim you are in.</td></tr>
        <tr>
            <td rowspan="2" colspan="2"><code>/trustlist</code></td>
            <td>View a list of trusted users/groups/tags</td>
            <td><code>huskclaims.command.trustlist</code></td>
            <td align="center">✅</td>
        </tr>
        <tr>
            <td>View the trust list of another user''s claim</td>
            <td><code>huskclaims.command.trustlist.other</code></td>
            <td align="center">❌</td>
        </tr>
        <!-- /untrust command -->
        <tr>
            <td rowspan="2"><code>/untrust</code></td>
            <td rowspan="2"><code>/untrust &lt;usernames|@groups|#tags&hellip;&gt;</code></td>
            <td>Revoke the trust level of users/groups/tags</td>
            <td><code>huskclaims.command.untrust</code></td>
            <td align="center">✅</td>
        </tr>
        <tr>
            <td>Revoke the trust level of a user on someone else's claim</td>
            <td><code>huskclaims.command.untrust.other</code></td>
            <td align="center">❌</td>
        </tr>
        <!-- /restrictclaim command -->
        <tr>
            <td rowspan="2"><code>/restrictclaim</code></td>
            <td rowspan="2"><code>/restrictclaim [on|off]</code></td>
            <td>Restrict the child claim you are in from inheriting parent trust access rights</td>
            <td><code>huskclaims.command.restrictclaim</code></td>
            <td align="center">✅</td>
        </tr>
        <tr>
            <td>Restrict the child claim within someone else's claim</td>
            <td><code>huskclaims.command.restrictclaim.other</code></td>
            <td align="center">❌</td>
        </tr>
        <!-- /transferclaim command -->
        <tr>
            <td rowspan="2"><code>/transferclaim</code></td>
            <td rowspan="2"><code>/transferclaim &lt;username&gt;</code></td>
            <td>Transfer ownership of a claim to another player</td>
            <td><code>huskclaims.command.transferclaim</code></td>
            <td align="center">❌</td>
        </tr>
        <tr>
            <td>Transfer ownership of someone else's claim</td>
            <td><code>huskclaims.command.transferclaim.other</code></td>
            <td align="center">❌</td>
        </tr>
        <!-- /claimflags command -->
        <tr>
            <td rowspan="3"><code>/claimflags</code></td>
            <td rowspan="3"><code>/claimflags &lt;list|set&gt;</code></td>
            <td>Manage the flags of the claim you are in</td>
            <td><code>huskclaims.command.claimflags</code></td>
            <td align="center">❌</td>
        </tr>
        <tr>
            <td>Manage the flags of someone else's claim</td>
            <td><code>huskclaims.command.claimflags.other</code></td>
            <td align="center">❌</td>
        </tr>
        <tr>
            <td>Manage the flags of the claim world you are in</td>
            <td><code>huskclaims.command.claimflags.world</code></td>
            <td align="center">❌</td>
        </tr>
        <!-- /claimexplosions command -->
        <tr><th colspan="5">Operation group commands</th></tr>
        <tr><td colspan="5" align="center">Lets users toggle groups of operation type flags. Only one is configured by default:</td></tr>
        <tr>
            <td rowspan="2"><code>/claimexplosions</code></td>
            <td rowspan="2"><code>/claimexplosions [on|off]</code></td>
            <td>Toggle allowing explosion damage flags in the claim</td>
            <td><code>huskclaims.command.claimexplosions</code></td>
            <td align="center">✅</td>
        </tr>
        <tr>
            <td>Toggle claim explosions in someone else's claim</td>
            <td><code>huskclaims.command.claimexplosions.other</code></td>
            <td align="center">❌</td>
        </tr>
        <!-- /claimban command -->
        <tr>
            <td rowspan="2"><code>/claimban</code></td>
            <td rowspan="2"><code>/claimban &lt;ban|unban|list&gt; [username]</code></td>
            <td>Ban a user from the claim you are standing in</td>
            <td><code>huskclaims.command.claimban</code></td>
            <td align="center">✅</td>
        </tr>
        <tr>
            <td>Ban a user from someone else's claim</td>
            <td><code>huskclaims.command.claimban.other</code></td>
            <td align="center">❌</td>
        </tr>
        <!-- /usergroup command -->
        <tr><th colspan="5">User group command</th></tr>
        <tr><td colspan="5" align="center">Lets users create groups of players to easily & centrally manage claim permissions.</td></tr>
        <tr>
            <td rowspan="4"><code>/usergroup</code></td>
            <td><code>/usergroup</code></td>
            <td>View a list of your user groups</td>
            <td rowspan="4"><code>huskclaims.command.usergroup</code></td>
            <td rowspan="4" align="center">✅</td>
        </tr>
        <tr>
            <td><code>/usergroup &lt;name&gt; [list]</code></td>
            <td>View a list of members of a user group</td>
        </tr>
        <tr>
            <td><code>/usergroup &lt;name&gt; delete</code></td>
            <td>Delete a user group</td>
        </tr>
        <tr>
            <td><code>/usergroup &lt;name&gt; &lt;add|remove&gt; &lt;usernames&hellip;&gt;</code></td>
            <td>Add or remove player(s) from a user group.</td>
        </tr>
        <tr><th colspan="5">Claim blocks commands</th></tr>
        <tr><td colspan="5" align="center">Lets users/administrators buy, view, and manage claim blocks.</td></tr>
        <tr>
            <td rowspan="4"><code>/claimblocks</code></td>
            <td><code>/claimblocks</code></td>
            <td>View your claim block balance</td>
            <td><code>huskclaims.command.claimblocks</code></td>
            <td align="center">✅</td>
        </tr>
        <tr>
            <td><code>/claimblocks &lt;username&gt; [show]</code></td>
            <td>View another user''s claim block balance</td>
            <td><code>huskclaims.command.claimblocks.other</code></td>
            <td align="center">❌</td>
        </tr>
        <tr>
            <td><code>/claimblocks &lt;username&gt; gift &lt;amount&gt;</code></td>
            <td>Gift (send) a user an amount of your claim blocks.</td>
            <td><code>huskclaims.command.claimblocks.gift</code></td>
            <td align="center">❌</td>
        </tr>
        <tr>
            <td><code>/claimblocks &lt;username&gt; &lt;add|remove|set&gt; &lt;amount&gt;</code></td>
            <td>Edit a claim block balance. Also requires the 'other' permission to edit others' balances.</td>
            <td><code>huskclaims.command.claimblocks.edit</code></td>
            <td align="center">❌</td>
        </tr>
        <tr>
            <td><code>/buyclaimblocks</code></td>
            <td><code>/buyclaimblocks &lt;amount&gt;</code></td>
            <td>Buy claim blocks for money. Requires the Vault hook to use.</td>
            <td><code>huskclaims.command.buyclaimblocks</code></td>
            <td align="center">✅</td>
        </tr>
        <!-- /transferpet command -->
        <tr><th colspan="5">Pet commands</th></tr>
        <tr><td colspan="5" align="center">Manage protection of tamed animals.</td></tr>
        <tr>
            <td rowspan="2"><code>/transferpet</code></td>
            <td rowspan="2"><code>/transferpet &lt;username&gt;</code></td>
            <td>Transfer ownership of a tamed animal (pet) to another player</td>
            <td><code>huskclaims.command.transferpet</code></td>
            <td align="center">✅</td>
        </tr>
        <tr>
            <td>Transfer ownership of someone else's pet</td>
            <td><code>huskclaims.command.transferpet.other</code></td>
            <td align="center">❌</td>
        </tr>
        <!-- /unlockdrops command -->
        <tr><th colspan="5">Death drop commands</th></tr>
        <tr><td colspan="5" align="center">Manage the locking of dropped items on death.</td></tr>
        <tr>
            <td rowspan="2"><code>/unlockdrops</code></td>
            <td><code>/unlockdrops</code></td>
            <td>Unlock locked item drops from when you died</td>
            <td><code>huskclaims.command.unlockdrops</code></td>
            <td align="center">✅</td>
        </tr>
        <tr>
            <td><code>/unlockdrops &lt;username&gt;</code></td>
            <td>Unlock someone else's death drops</td>
            <td><code>huskclaims.command.unlockdrops.other</code></td>
            <td align="center">❌</td>
        </tr>
        <!-- /trapped command -->
        <tr><th colspan="5">Trapped in a claim command</th></tr>
        <tr><td colspan="5" align="center">Let users teleport outside claims they dont have build trust in.</td></tr>
        <tr>
            <td colspan="2"><code>/trapped</code></td>
            <td>Teleports you outside a claim you can't build in.</td>
            <td><code>huskclaims.command.trapped</code></td>
            <td align="center">✅</td>
        </tr>
        <tr><th colspan="5">Other administrator commands</th></tr>
        <tr><td colspan="5" align="center">Moderation and plugin management utilities.</td></tr>
        <tr>
            <td><code>/ignoreclaims</code></td>
            <td><code>/ignoreclaims [on|off]</code></td>
            <td>Toggle ignoring claim rules/trust levels. Note you must constantly have the permission to keep ignoring claims.</td>
            <td><code>huskclaims.command.ignoreclaims</code></td>
            <td align="center">❌</td>
        </tr>
        <tr>
            <td><code>/signspy</code></td>
            <td><code>/signspy [on|off]</code></td>
            <td>Toggle receiving sign moderation notifications when users place or edit signs.</td>
            <td><code>huskclaims.command.signspy</code></td>
            <td align="center">❌</td>
        </tr>
        <tr>
            <td rowspan="9"><code>/huskclaims</code></td>
            <td><code>/huskclaims</code></td>
            <td>Use plugin management commands</td>
            <td><code>huskclaims.command.huskclaims</code></td>
            <td align="center">✅</td>
        </tr>
        <tr>
            <td><code>/huskclaims about</code></td>
            <td>View the plugin about menu</td>
            <td><code>huskclaims.command.huskclaims.about</code></td>
            <td align="center">✅</td>
        </tr>
        <tr>
            <td><code>/huskclaims help [page]</code></td>
            <td>View the list of plugin commands</td>
            <td><code>huskclaims.command.huskclaims.help</code></td>
            <td align="center">✅</td>
        </tr>
        <tr>
            <td><code>/huskclaims update</code></td>
            <td>Check for plugin updates</td>
            <td><code>huskclaims.command.huskclaims.update</code></td>
            <td align="center">❌</td>
        </tr>
        <tr>
            <td><code>/huskclaims reload</code></td>
            <td>Reload the plugin locales and config file</td>
            <td><code>huskclaims.command.huskclaims.reload</code></td>
            <td align="center">❌</td>
        </tr>
        <tr>
            <td><code>/huskclaims import</code></td>
            <td>Import data from another plugin</td>
            <td><code>huskclaims.command.huskclaims.import</code></td>
            <td align="center">❌</td>
        </tr>
        <tr>
            <td><code>/huskclaims teleport [coordinates]</code></td>
            <td>Teleport to a claim at a position. Requires the HuskHomes hook to use.</td>
            <td><code>huskclaims.command.huskclaims.teleport</code></td>
            <td align="center">❌</td>
        </tr>
        <tr>
            <td><code>/huskclaims logs &lt;username&gt;</code></td>
            <td>View audit logs for a player, such as claim block transaction receipts.</td>
            <td><code>huskclaims.command.huskclaims.logs</code></td>
            <td align="center">❌</td>
        </tr>
        <tr>
            <td><code>/huskclaims status</code></td>
            <td>View the system status debug info screen.</td>
            <td><code>huskclaims.command.huskclaims.status</code></td>
            <td align="center">❌</td>
        </tr>
    </tbody>
</table>

### Command Aliases
The following commands have aliases that can also be used for convenience:

| Command            | Aliases                                    |
|--------------------|--------------------------------------------|
| `/claimslist`      | `/claims`                                  |
| `/trustlist`       | `/claiminfo`                               |
| `/adminclaimslist` | `/adminclaims`                             |
| `/unclaim`         | `/abandonclaim`                            |
| `/unclaimall`      | `/abandonallclaims`                        |
| `/childclaim`      | `/subdivideclaims`                         |
| `/restrictclaim`   | `/restrictchildclaim`, `/restrictsubclaim` |
| `/claimblocks`     | `/adjustclaimblocks`                       |
| `/permissiontrust` | `/managetrust` &dagger;                    |
| `/transferpet`     | `/givepet`                                 |

&dagger; You can customize or change the [Trust Levels](trust#trust-levels) entirely if you wish, including command aliases.