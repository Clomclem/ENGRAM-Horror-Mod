package horror.blueice129.mixin;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import net.minecraft.entity.attribute.DefaultAttributeContainer;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.player.PlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(PlayerEntity.class)
public abstract class PlayerEntityMixin {
    @ModifyReturnValue(method = "createPlayerAttributes", at = @At("RETURN"))
    private static DefaultAttributeContainer.Builder addPlayerAttributes(DefaultAttributeContainer.Builder original) {
        return original.add(EntityAttributes.GENERIC_FOLLOW_RANGE, 35.0D);
    }
}
