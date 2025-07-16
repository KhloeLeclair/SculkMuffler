package dev.khloeleclair.skulkmuffler.mixin;

import dev.khloeleclair.skulkmuffler.SculkMufflerMod;
import dev.khloeleclair.skulkmuffler.common.Config;
import dev.khloeleclair.skulkmuffler.common.blockentities.MufflerBlockEntity;
import dev.khloeleclair.skulkmuffler.common.utilities.MathHelpers;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.gameevent.GameEventListener;
import net.minecraft.world.level.gameevent.vibrations.VibrationSystem;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(VibrationSystem.Listener.class)
public abstract class MixinVibrationSystem_Listener implements GameEventListener {

    @Inject(method = "scheduleVibration", at = @At("HEAD"), cancellable = true)
    protected void onScheduleVibration(
            ServerLevel level,
            VibrationSystem.Data data,
            Holder<GameEvent> gameEvent,
            GameEvent.Context context,
            Vec3 pos,
            Vec3 sensorPos,
            CallbackInfo ci
    ) {
        final var tracker = SculkMufflerMod.Instance == null ? null : SculkMufflerMod.Instance.Tracker;
        if (tracker == null)
            return;

        double volume = tracker.getVolume(level, pos);
        if (volume <= MathHelpers.logToLinear(Config.Common.vibrationVolume.get()))
            ci.cancel();
    }

}
