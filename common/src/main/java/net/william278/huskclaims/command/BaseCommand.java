package net.william278.huskclaims.command;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import net.william278.huskclaims.HuskClaims;

@AllArgsConstructor(access = AccessLevel.PROTECTED)
public abstract class BaseCommand {

    protected final HuskClaims plugin;

}
