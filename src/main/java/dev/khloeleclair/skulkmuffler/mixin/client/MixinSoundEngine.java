package dev.khloeleclair.skulkmuffler.mixin.client;

import dev.khloeleclair.skulkmuffler.client.SculkMufflerClient;
import dev.khloeleclair.skulkmuffler.common.Config;
import dev.khloeleclair.skulkmuffler.common.TagCache;
import dev.khloeleclair.skulkmuffler.common.blockentities.MufflerBlockEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.*;
import net.minecraft.client.sounds.ChannelAccess;
import net.minecraft.client.sounds.SoundEngine;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.apache.commons.lang3.tuple.Pair;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

import java.util.Iterator;
import java.util.Map;

@OnlyIn(Dist.CLIENT)
@Mixin(SoundEngine.class)
public abstract class MixinSoundEngine {

    @Shadow
    private Map<SoundInstance, ChannelAccess.ChannelHandle> instanceToChannel;

    @Shadow
    abstract float calculateVolume(SoundInstance sound);

    @Inject(
            method = "tick",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/sounds/SoundEngine;tickNonPaused()V",
                    ordinal = 0,
                    shift = At.Shift.AFTER
            )
    )
    public void SM$inTick(CallbackInfo ci) {
        // Let's update all playing record sounds.

        final var tracker = SculkMufflerClient.Instance == null ? null : SculkMufflerClient.Instance.Tracker;
        final var mc = Minecraft.getInstance();
        final var level = mc.level;
        if (level == null || tracker == null)
            return;

        // Unless Records are immune from muffling.
        final var immune = Config.Client.immuneSources.get();
        if (!immune.isEmpty() && immune.contains(SoundSource.RECORDS.toString()))
            return;

        final var listenerPos = mc.cameraEntity == null ? null : mc.cameraEntity.getEyePosition();

        for (Map.Entry<SoundInstance, ChannelAccess.ChannelHandle> entry : this.instanceToChannel.entrySet()) {
            final var sound = entry.getKey();
            if (
                sound.getSource() != SoundSource.RECORDS ||
                (sound instanceof AbstractTickableSoundInstance) ||
                TagCache.getIgnoreSounds().contains(sound.getLocation())
            )
                continue;

            final var pos = new Vec3(sound.getX(), sound.getY(), sound.getZ());
            final var pair = tracker.getNearbyAndVolume(level, pos, listenerPos, sound.getLocation(), sound.getSource());
            final double volume = pair.getLeft();
            if (volume < 1) {
                final var mbe = pair.getRight();
                if (mbe != null)
                    mbe.drawSculkParticle(pos);
            }

            float vol = sound.getVolume();
            if (sound instanceof AbstractSoundInstance asi)
                SculkMufflerClient.handleSoundVolume(asi, (float) volume);

            float new_vol = sound.getVolume();
            if (new_vol != vol) {
                float f = this.calculateVolume(sound);
                entry.getValue().execute(c -> c.setVolume(f));
            }
        }
    }

    @Inject(
            method = "tickNonPaused",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/resources/sounds/TickableSoundInstance;tick()V",
                    ordinal = 0,
                    shift = At.Shift.AFTER
            ),
            locals = LocalCapture.CAPTURE_FAILSOFT
    )
    public void SM$inTickNotPaused(CallbackInfo ci, Iterator<?> iterator, TickableSoundInstance tickablesoundinstance) {
        // For now, force mute to see if it works.
        if (tickablesoundinstance instanceof AbstractTickableSoundInstance atsi && !atsi.isStopped()) {
            if (TagCache.getIgnoreSounds().contains(atsi.getLocation()))
                return;

            final var tracker = SculkMufflerClient.Instance != null ? SculkMufflerClient.Instance.Tracker : null;
            final var mc = Minecraft.getInstance();
            final var level = mc.level;
            if (tracker != null && level != null) {
                final var pos = atsi instanceof RidingMinecartSoundInstance rmsi
                        ? rmsi.minecart.position()
                        : new Vec3(atsi.getX(), atsi.getY(), atsi.getZ());

                final var listenerPos = mc.cameraEntity == null ? null : mc.cameraEntity.getEyePosition();

                Pair<Double, MufflerBlockEntity> pair = tracker.getNearbyAndVolume(level, pos, listenerPos, atsi.getLocation(), atsi.getSource());

                final double volume = pair.getLeft();
                if (volume < 1) {
                    final var mbe = pair.getRight();
                    if (mbe != null)
                        mbe.drawSculkParticle(pos);
                }

                SculkMufflerClient.handleSoundVolume(atsi, (float) volume);
            }
        }
    }

}
