package dev.khloeleclair.skulkmuffler.mixin;

import dev.khloeleclair.skulkmuffler.SculkMufflerMod;
import dev.khloeleclair.skulkmuffler.common.Config;
import dev.khloeleclair.skulkmuffler.common.blockentities.MufflerBlockEntity;
import dev.khloeleclair.skulkmuffler.common.utilities.MathHelpers;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BellBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

@Mixin(BellBlockEntity.class)
public abstract class MixinBellBlockEntity extends BlockEntity {

    public MixinBellBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState blockState) {
        super(type, pos, blockState);
    }

    @Shadow
    private long lastRingTimestamp;

    @Shadow
    private List<LivingEntity> nearbyEntities;

    @Inject(method = "updateEntities", at = @At("HEAD"), cancellable = true)
    protected void onUpdateEntities(CallbackInfo ci) {
        final var level = getLevel();
        final var tracker = SculkMufflerMod.Instance == null ? null : SculkMufflerMod.Instance.Tracker;
        if (tracker == null || level == null)
            return;

        final var pos = getBlockPos();
        double volume = MathHelpers.dBtoLinear(tracker.getNearbyMufflers(level, pos)
                .map(MufflerBlockEntity::getVolumeDB)
                .reduce(MathHelpers.linearToDb(1.0), Float::sum));

        // If the volume isn't low enough, resume.
        if (volume > Config.Common.bellHeardVolume.get())
            return;

        // Cancel the original, but update the nearby entities.
        ci.cancel();

        if (level.getGameTime() > this.lastRingTimestamp + 60L || this.nearbyEntities == null) {
            this.lastRingTimestamp = level.getGameTime();
            AABB aabb = new AABB(pos).inflate(48.0);
            this.nearbyEntities = level.getEntitiesOfClass(LivingEntity.class, aabb);
        }
    }

    @Inject(method = "makeRaidersGlow", at = @At("HEAD"), cancellable = true)
    private static void onMakeRaidersGlow(
            Level level,
            BlockPos pos,
            List<LivingEntity> raiders,
            CallbackInfo ci
    ) {
        final var tracker = SculkMufflerMod.Instance == null ? null : SculkMufflerMod.Instance.Tracker;
        if (tracker == null || level == null)
            return;

        double volume = MathHelpers.dBtoLinear(tracker.getNearbyMufflers(level, pos)
                .map(MufflerBlockEntity::getVolumeDB)
                .reduce(MathHelpers.linearToDb(1.0), Float::sum));

        // If the volume is low enough, cancel this.
        if (volume <= Config.Common.bellHighlightVolume.get())
            ci.cancel();
    }

}
