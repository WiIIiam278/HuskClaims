package net.william278.huskclaims.mixins;

import net.minecraft.entity.Entity;
import net.minecraft.entity.decoration.DisplayEntity;
import net.minecraft.entity.player.PlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import static net.william278.huskclaims.highlighter.FabricBlockDisplayHighlighter.DisplayHighlightBlock.VISIBLE_TO;

@Mixin(Entity.class)
public abstract class EntityMixin {

    @Inject(method = "isInvisibleTo", at = @At("HEAD"), cancellable = true)
    private void isInvisibleTo(PlayerEntity player, CallbackInfoReturnable<Boolean> cir) {
        if ((Object) this instanceof DisplayEntity.BlockDisplayEntity block) {
            final String visibleToUuid = block.getDataTracker().get(VISIBLE_TO);
            if (visibleToUuid == null) {
                return;
            }
            final String playerUuid = player.getGameProfile().getId().toString();
            if (visibleToUuid.equals(playerUuid)) {
                cir.setReturnValue(true);
                cir.cancel();
            }
        }
    }

}
