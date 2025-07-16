package dev.khloeleclair.skulkmuffler.common.blockentities;

import dev.khloeleclair.skulkmuffler.SculkMufflerMod;
import dev.khloeleclair.skulkmuffler.client.SculkMufflerClient;
import dev.khloeleclair.skulkmuffler.common.Config;
import dev.khloeleclair.skulkmuffler.common.blocks.MufflerBlock;
import dev.khloeleclair.skulkmuffler.common.utilities.Constants;
import dev.khloeleclair.skulkmuffler.common.utilities.MathHelpers;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.particles.DustColorTransitionOptions;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.FaceAttachedHorizontalDirectionalBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.fml.loading.FMLLoader;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3f;
import software.bernie.geckolib.animatable.GeoBlockEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.*;
import software.bernie.geckolib.util.GeckoLibUtil;

public class MufflerBlockEntity extends BlockEntity implements GeoBlockEntity {

    public static final Vector3f DIMGRAY_PARTICLE_COLOR = new Vector3f(0.2f, 0.2f, 0.2f);
    public static final DustColorTransitionOptions SOUND_ABSORBED_PARTICLE = new DustColorTransitionOptions(
            DustColorTransitionOptions.SCULK_PARTICLE_COLOR,
            DIMGRAY_PARTICLE_COLOR,
            1.0f
    );

    private double volume;
    private double volume_linear;
    private float volume_db;
    private int range;
    private long lastParticleTime = -1;
    private int debug_draw = -1;

    public MufflerBlockEntity(BlockPos pos, BlockState state) {
        super(SculkMufflerMod.MUFFLER_BLOCK_ENTITY.get(), pos, state);
        range = Config.Common.defaultRange.get();
        volume = Config.Common.minVolume.get();
        updateVolumes();
    }

    private void updateVolumes() {
        volume_linear = MathHelpers.logToLinear(volume);
        volume_db = MathHelpers.linearToDb(volume_linear);
    }

    //region Data

    @Override
    protected void loadAdditional(@NotNull CompoundTag tag, HolderLookup.@NotNull Provider registries) {
        super.loadAdditional(tag, registries);
        range = Config.Common.defaultRange.get();
        if (tag.contains("radius"))
            range = tag.getInt("radius");
        range = Math.clamp(range, 0, Config.Common.maxRange.get());

        volume = Config.Common.minVolume.get();
        if (tag.contains("volume"))
            volume = Math.clamp(tag.getDouble("volume"), volume, 1.0);

        debug_draw = -1;
        if (tag.contains("debug"))
            setDebug(tag.getInt("debug"));

        updateVolumes();
        updateMuffler();
    }

    @Override
    protected void saveAdditional(@NotNull CompoundTag tag, HolderLookup.@NotNull Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putInt("radius", range);
        tag.putDouble("volume", volume);
        if (debug_draw != -1)
            tag.putInt("debug", debug_draw);
    }

    //endregion

    //region Updates

    @Override
    public @NotNull CompoundTag getUpdateTag(HolderLookup.@NotNull Provider registries) {
        var tag = super.getUpdateTag(registries);
        saveAdditional(tag, registries);
        return tag;
    }

    @Override
    public @Nullable Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public void handleUpdateTag(@NotNull CompoundTag tag, HolderLookup.@NotNull Provider lookupProvider) {
        super.handleUpdateTag(tag, lookupProvider);
        loadAdditional(tag, lookupProvider);
    }

    //endregion

    //region GeoBlockEntity

    protected static final RawAnimation ANIM_ACTIVATE = RawAnimation.begin().thenPlay("activate");
    protected static final RawAnimation ANIM_DEACTIVATE = RawAnimation.begin().thenPlay("deactivate");
    protected static final RawAnimation ANIM_IDLE = RawAnimation.begin().thenLoop("idle");
    protected static final RawAnimation ANIM_INACTIVE = RawAnimation.begin().thenLoop("inactive");

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(
                new AnimationController<>(this, "ctl", this::deployAnimController)
                        .triggerableAnim("activate", ANIM_ACTIVATE)
                        .triggerableAnim("deactivate", ANIM_DEACTIVATE)
        );
    }

    protected <E extends MufflerBlockEntity> PlayState deployAnimController(final AnimationState<E> state) {
        final boolean is_enabled = volume < 1 && getBlockState().getValue(MufflerBlock.ENABLED);
        return state.setAndContinue(is_enabled ? ANIM_IDLE : ANIM_INACTIVE);
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return this.cache;
    }

    //endregion

    //region Muffler Logic

    public BlockPos getCenter() {
        final var state = getBlockState();
        final var pos = getBlockPos();
        final var face = state.getValue(FaceAttachedHorizontalDirectionalBlock.FACE);
        final var facing = state.getValue(FaceAttachedHorizontalDirectionalBlock.FACING);

        int r = range == 0 ? 1 : range;

        switch(face) {
            case CEILING:
                return pos.below(r);
            case WALL:
                switch(facing) {
                    case NORTH:
                        return pos.north(r);
                    case EAST:
                        return pos.east(r);
                    case SOUTH:
                        return pos.south(r);
                    case WEST:
                        return pos.west(r);
                }
            case FLOOR:
            default:
                return pos.above(r);
        }
    }

    public void drawSculkParticle(Vec3 source) {
        if (level == null)
            return;

        final var now = level.getGameTime();
        if (now - lastParticleTime < 1)
            return;

        lastParticleTime = now;

        final var random = level.getRandom();
        final var pos = Vec3.atCenterOf(getBlockPos());
        final var target = pos.vectorTo(source).normalize().scale(0.5).add(pos);

        double ySpeed = (double)random.nextFloat() * 0.04;

        level.addParticle(
                SOUND_ABSORBED_PARTICLE,
                target.x, target.y, target.z,
                0, ySpeed, 0
        );
    }

    @OnlyIn(Dist.CLIENT)
    public int effectiveDebug() {
        if (Config.Client.rangeRenderer.get() == Config.RangeRenderer.DISABLED)
            return -1;
        return debug_draw;
    }

    public int getDebug() { return debug_draw; }
    public void setDebug(int value) {
        if (value < 0 || value >= Constants.AREAS.length)
            debug_draw = -1;
        else
            debug_draw = value;
    }

    public void toggleDebug() {
        if (debug_draw == -1)
            setDebug(level.getRandom().nextInt(Constants.AREAS.length));
        else
            debug_draw = -1;
    }

    public void cycleDebug() {
        debug_draw++;
        if (debug_draw >= Constants.AREAS.length)
            debug_draw = -1;
    }

    public void sendClientUpdate() {
        if (level == null)
            return;
        final var state = getBlockState();
        level.sendBlockUpdated(getBlockPos(), state, state, 6);
    }

    public void setVolume(double volume) {
        this.volume = Math.clamp(volume, Config.Common.minVolume.get(), 1);
        updateVolumes();
        updateMuffler();
    }

    public double getVolumeLog() { return volume_linear; }

    public float getVolumeDB() { return volume_db; }

    public double getVolume() { return volume; }

    public void setRange(int range) {
        this.range = Math.clamp(range, 0, Config.Common.maxRange.get());
        updateMuffler();
    }

    public int getRange() { return range; }

    public boolean isInRange(Vec3 position) {
        final var center = Vec3.atCenterOf(getCenter());
        double r = range == 0 ? 0.5 : range + 0.5;

        return Math.abs(position.x - center.x) <= r &&
                Math.abs(position.y - center.y) <= r &&
                Math.abs(position.z - center.z) <= r;

        // Sphere Mode
        //return Vec3.atCenterOf(getCenter()).distanceToSqr(position) <= radiusSquared;
    }

    public void updateMuffler() {
        final BlockState state = this.getBlockState();
        final Level level = this.getLevel();
        boolean removed = this.isRemoved();

        boolean should_enable = this.volume < 1 && !removed && state.is(SculkMufflerMod.MUFFLER_BLOCK) && state.getValue(MufflerBlock.ENABLED) && level != null;
        //SculkMufflerMod.LOGGER.info("updateMuffler: {}-- {}, {}", removed, should_enable, isRegistered);

        updateServerMap(should_enable);
        if (FMLLoader.getDist() == Dist.CLIENT && level != null) {
            updateClientMap(should_enable);
            triggerAnim("ctl", should_enable ? "activate" : "deactivate");
        }
    }

    private void updateServerMap(boolean enabled) {
        //SculkMufflerMod.LOGGER.info("updateServerMap: {}", enabled);
        final var level = this.getLevel();
        final var tracker = SculkMufflerMod.Instance.Tracker;
        if (level != null && tracker != null) {
            if (enabled)
                tracker.addMuffler(this);
            else
                tracker.removeMuffler(this);
        }
    }

    @OnlyIn(Dist.CLIENT)
    private void updateClientMap(boolean enabled) {
        //SculkMufflerMod.LOGGER.info("updateClientMap: {}", enabled);
        final var level = this.getLevel();
        final var inst = SculkMufflerClient.Instance;
        if (inst != null && level != null) {
            if (enabled)
                inst.Tracker.addMuffler(this);
            else
                inst.Tracker.removeMuffler(this);
        }
    }

    //endregion

    //region Update Detection

    @SuppressWarnings("deprecation")
    @Override
    public void setBlockState(@NotNull BlockState blockState) {
        super.setBlockState(blockState);
        updateMuffler();
    }

    @Override
    public void setRemoved() {
        super.setRemoved();
        updateMuffler();
    }

    @Override
    public void clearRemoved() {
        super.clearRemoved();
        updateMuffler();
    }

    //endregion
}
