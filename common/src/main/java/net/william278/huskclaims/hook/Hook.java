package net.william278.huskclaims.hook;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import net.william278.huskclaims.HuskClaims;

@Getter
@AllArgsConstructor(access = AccessLevel.PROTECTED)
public abstract class Hook {

    protected final HuskClaims plugin;
    protected final String name;
    protected final String requiredDependency;

    public abstract void load();
    public abstract void unload();

}
