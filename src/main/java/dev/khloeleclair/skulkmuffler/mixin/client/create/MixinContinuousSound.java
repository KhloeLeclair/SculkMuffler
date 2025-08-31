package dev.khloeleclair.skulkmuffler.mixin.client.create;

import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@OnlyIn(Dist.CLIENT)
@Mixin(targets = "com.simibubi.create.foundation.sound.ContinuousSound", priority = 0)
public abstract class MixinContinuousSound extends AbstractTickableSoundInstance {

    protected MixinContinuousSound(SoundEvent soundEvent, SoundSource source, RandomSource random) {
        super(soundEvent, source, random);
    }

    // Create's ContinuousSound class doesn't store its volume on itself, instead
    // calling back to a soundscape object whenever getVolume() is called. As such,
    // we can't really modify it like we do normal AbstractTickableSoundInstances.

    // Instead, we'll just make sure the underlying volume value is set to 1 every
    // frame so our existing ATSI code works, and then add a postfix to multiply
    // the volume by the last tick's adjusted volume.

    @Inject(method = "getVolume", at = @At("RETURN"), cancellable = true)
    protected void onGetVolume(CallbackInfoReturnable<Float> cir) {
        if (!isStopped())
            cir.setReturnValue(cir.getReturnValueF() * volume);
    }

    @Inject(method = "tick", at = @At("RETURN"))
    protected void onTick(CallbackInfo ci) {
        this.volume = 1.0f;
    }

}
