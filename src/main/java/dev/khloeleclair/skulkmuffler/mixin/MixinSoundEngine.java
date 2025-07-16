package dev.khloeleclair.skulkmuffler.mixin;

import dev.khloeleclair.skulkmuffler.client.SculkMufflerClient;
import dev.khloeleclair.skulkmuffler.common.blockentities.MufflerBlockEntity;
import dev.khloeleclair.skulkmuffler.common.utilities.MathHelpers;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.client.resources.sounds.TickableSoundInstance;
import net.minecraft.client.sounds.SoundEngine;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.apache.commons.lang3.tuple.Pair;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

import java.util.Iterator;

@OnlyIn(Dist.CLIENT)
@Mixin(SoundEngine.class)
public class MixinSoundEngine {

    @Inject(method = "tickNonPaused", at = @At(value = "INVOKE", target = "tick", ordinal = 0, shift = At.Shift.AFTER), locals = LocalCapture.CAPTURE_FAILSOFT)
    public void inTickNotPaused(CallbackInfo ci, Iterator<?> var1, TickableSoundInstance tickablesoundinstance) {
        // For now, force mute to see if it works.
        if (tickablesoundinstance instanceof AbstractTickableSoundInstance atsi && !atsi.isStopped()) {
            final var tracker = SculkMufflerClient.Instance != null ? SculkMufflerClient.Instance.Tracker : null;
            final var level = Minecraft.getInstance().level;
            if (tracker != null && level != null) {
                final var pos = new Vec3(atsi.getX(), atsi.getY(), atsi.getZ());
                Pair<Float, MufflerBlockEntity> pair = tracker.getNearbyMufflers(level, pos)
                        .map(mbe -> Pair.of(mbe.getVolumeDB(), mbe))
                        .reduce(Pair.of(MathHelpers.linearToDb(1.0), null), (a, b) -> {
                            final var a_mbe = a.getRight();
                            final var b_mbe = b.getRight();

                            final var a_dist = a_mbe == null
                                    ? Double.POSITIVE_INFINITY
                                    : pos.distanceToSqr(a_mbe.getBlockPos().getCenter());
                            final var b_dist = b_mbe == null
                                    ? Double.POSITIVE_INFINITY
                                    : pos.distanceToSqr(b_mbe.getBlockPos().getCenter());

                            return Pair.of(a.getLeft() + b.getLeft(), a_dist < b_dist ? a.getRight() : b.getRight());
                        });

                final var volume = MathHelpers.dBtoLinear(pair.getLeft());
                if (volume >= 1)
                    return;

                final var mbe = pair.getRight();
                if (mbe != null)
                    mbe.drawSculkParticle(pos);

                atsi.volume = atsi.volume * (float) volume;
            }
        }
    }

}
