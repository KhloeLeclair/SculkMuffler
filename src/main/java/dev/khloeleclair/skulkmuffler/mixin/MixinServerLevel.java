package dev.khloeleclair.skulkmuffler.mixin;

import dev.khloeleclair.skulkmuffler.SculkMufflerMod;
import dev.khloeleclair.skulkmuffler.common.Config;
import dev.khloeleclair.skulkmuffler.common.utilities.MathHelpers;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.RegistryAccess;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.dimension.DimensionType;
import net.minecraft.world.level.storage.WritableLevelData;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.util.function.Supplier;

@Mixin(ServerLevel.class)
public abstract class MixinServerLevel extends Level {

    protected MixinServerLevel(WritableLevelData levelData, ResourceKey<Level> dimension, RegistryAccess registryAccess, Holder<DimensionType> dimensionTypeRegistration, Supplier<ProfilerFiller> profiler, boolean isClientSide, boolean isDebug, long biomeZoomSeed, int maxChainedNeighborUpdates) {
        super(levelData, dimension, registryAccess, dimensionTypeRegistration, profiler, isClientSide, isDebug, biomeZoomSeed, maxChainedNeighborUpdates);
    }

    @Redirect(
            method = "globalLevelEvent",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/level/GameRules;getBoolean(Lnet/minecraft/world/level/GameRules$Key;)Z"
            )
    )
    public boolean SM$globalLevelEvent$getBoolean(GameRules instance, GameRules.Key<GameRules.BooleanValue> key, int id, BlockPos pos, int data) {
        if (key == GameRules.RULE_GLOBAL_SOUND_EVENTS) {
            // We're checking if we can play a global sound event. Within range of a Sculk Muffler, don't allow that.
            final var tracker = SculkMufflerMod.Instance == null ? null : SculkMufflerMod.Instance.Tracker;
            if (tracker != null) {
                final double volume = tracker.getVolume(this, pos.getCenter());
                if (volume <= MathHelpers.logToLinear(Config.Common.antiGlobalVolume.get()))
                    return false;
            }
        }

        return instance.getBoolean(key);
    }

}
