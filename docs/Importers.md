HuskClaims supports importing data from other plugins through the `/huskclaims import` command.

| Name                                | Supported Import Data | Link                         |
|-------------------------------------|-----------------------|------------------------------|
| [GriefPrevention](#griefprevention) | Claims, Users         | https://griefprevention.com/ |

## GriefPrevention
HuskClaims supports importing Claims, Trust and User data from [GriefPrevention](https://griefprevention.com/) (`v16.18.1`+).

1. [Install the latest version of HuskClaims](setup) on your server. You may uninstall GriefPrevention.
2. Run `/huskclaims import griefprevention set uri:(URI) username:(USERNAME) password:(PASSWORD)`, entering in your GriefPrevention database credentials.
    * If you are using a MySQL database, the URI should be in the format `jdbc:mysql://(HOST):(PORT)/(DATABASE)`.
    * If you are using a SQLite database, the URI should be in the format `jdbc:sqlite:(PATH_TO_DATABASE)`.
3. Run `/huskclaims import griefprevention start` to begin the import process. This may take a while depending on the size of your database.
4. Once the importer has finished, verify that the data has been imported correctly by typing `/adminclaimslist` and `/claimslist <player>`

A server restart is recommended after the import has successfully completed.