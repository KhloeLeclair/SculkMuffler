package dev.khloeleclair.skulkmuffler.common.blockentities;

import dev.khloeleclair.skulkmuffler.SculkMufflerMod;
import dev.khloeleclair.skulkmuffler.client.SculkMufflerClient;
import dev.khloeleclair.skulkmuffler.common.Config;
import dev.khloeleclair.skulkmuffler.common.blocks.AdvancedMufflerBlock;
import dev.khloeleclair.skulkmuffler.common.blocks.MufflerBlock;
import dev.khloeleclair.skulkmuffler.common.data.CustomComponents;
import dev.khloeleclair.skulkmuffler.common.utilities.Constants;
import dev.khloeleclair.skulkmuffler.common.utilities.MathHelpers;
import it.unimi.dsi.fastutil.objects.ObjectArraySet;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.particles.DustColorTransitionOptions;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.Nameable;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.FaceAttachedHorizontalDirectionalBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.AttachFace;
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

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class MufflerBlockEntity extends BlockEntity implements GeoBlockEntity, Nameable {

    public static final Vector3f DIMGRAY_PARTICLE_COLOR = new Vector3f(0.2f, 0.2f, 0.2f);
    public static final DustColorTransitionOptions SOUND_ABSORBED_PARTICLE = new DustColorTransitionOptions(
            DustColorTransitionOptions.SCULK_PARTICLE_COLOR,
            DIMGRAY_PARTICLE_COLOR,
            1.0f
    );

    @Nullable
    private Component name;

    public final boolean isAdvanced;

    private boolean target_editing;
    private boolean target_allowlist;
    @Nullable
    private final HashSet<ResourceLocation> targets;
    private boolean categories_allowlist;
    @Nullable
    private final Set<SoundSource> categories;

    private int offset_x;
    private int offset_y;
    private int offset_z;

    private boolean containmentMode;
    private double volume;
    private double volume_linear;
    private float volume_db;
    private int range;
    private long lastParticleTime = -1;
    private int debug_draw = -1;

    @Nullable
    private HashSet<ResourceLocation> client_heard;

    public MufflerBlockEntity(BlockPos pos, BlockState state) {
        super(SculkMufflerMod.MUFFLER_BLOCK_ENTITY.get(), pos, state);
        offset_x = 0;
        offset_y = 0;
        offset_z = 0;
        range = Config.Common.defaultRange.get();
        volume = Config.Common.minVolume.get();
        containmentMode = false;
        this.isAdvanced = state.getBlock() instanceof AdvancedMufflerBlock;

        target_allowlist = true;
        categories_allowlist = true;
        targets = isAdvanced ? new HashSet<>() : null;
        categories = isAdvanced ? new ObjectArraySet<>() : null;

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

        offset_x = 0;
        offset_y = 0;
        offset_z = 0;
        if (tag.contains("offset_x", CompoundTag.TAG_INT))
            setOffsetX(tag.getInt("offset_x"), false);
        if (tag.contains("offset_y", CompoundTag.TAG_INT))
            setOffsetY(tag.getInt("offset_y"), false);
        if (tag.contains("offset_z", CompoundTag.TAG_INT))
            setOffsetZ(tag.getInt("offset_z"), false);

        volume = Config.Common.minVolume.get();
        if (tag.contains("volume"))
            volume = Math.clamp(tag.getDouble("volume"), volume, 1.0);

        debug_draw = -1;
        if (tag.contains("debug"))
            setDebug(tag.getInt("debug"));
        containmentMode = isAdvanced && tag.contains("containment_mode");

        target_allowlist = !tag.contains("target_allowlist") || tag.getBoolean("target_allowlist");
        if (targets != null) {
            targets.clear();
            if (tag.contains("targets", CompoundTag.TAG_LIST)) {
                ListTag list = tag.getList("targets", CompoundTag.TAG_STRING);
                for(int i = 0; i < list.size(); i++) {
                    var location = ResourceLocation.tryParse(list.getString(i));
                    if (location != null)
                        targets.add(location);
                }
            }
        }

        categories_allowlist = !tag.contains("categories_allowlist") || tag.getBoolean("categories_allowlist");
        if (categories != null) {
            categories.clear();
            if (tag.contains("categories", CompoundTag.TAG_LIST)) {
                ListTag list = tag.getList("categories", CompoundTag.TAG_STRING);
                for(int i = 0; i < list.size(); i++) {
                    SoundSource source;
                    try {
                        source = SoundSource.valueOf(list.getString(i));
                    } catch(IllegalArgumentException ex) {
                        continue;
                    }
                    categories.add(source);
                }
            }
        }

        if (tag.contains("CustomName", Tag.TAG_STRING))
            name = parseCustomNameSafe(tag.getString("CustomName"), registries);

        updateVolumes();
        updateMuffler();
    }

    @Override
    protected void saveAdditional(@NotNull CompoundTag tag, HolderLookup.@NotNull Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putInt("radius", range);
        if (offset_x != 0)
            tag.putInt("offset_x", offset_x);
        if (offset_y != 0)
            tag.putInt("offset_y", offset_y);
        if (offset_z != 0)
            tag.putInt("offset_z", offset_z);
        tag.putDouble("volume", volume);
        if (debug_draw != -1)
            tag.putInt("debug", debug_draw);
        if (containmentMode)
            tag.putBoolean("containment_mode", true);
        if (name != null)
            tag.putString("CustomName", Component.Serializer.toJson(name, registries));
        if (!target_allowlist)
            tag.putBoolean("target_allowlist", false);
        if (targets != null && !targets.isEmpty()) {
            ListTag list = new ListTag(targets.size());
            for(ResourceLocation location: targets) {
                list.add(StringTag.valueOf(location.toString()));
            }
            tag.put("targets", list);
        }
        if (!categories_allowlist)
            tag.putBoolean("categories_allowlist", false);
        if (categories != null && !categories.isEmpty()) {
            ListTag list = new ListTag(categories.size());
            for(SoundSource source: categories)
                list.add(StringTag.valueOf(source.name()));
            tag.put("categories", list);
        }
    }

    public void setCustomName(@Nullable Component name) { this.name = name; }

    @Override
    @Nullable
    public Component getCustomName() { return name; }

    @Override
    public @NotNull Component getDisplayName() { return this.getName(); }

    @Override
    public @NotNull Component getName() { return name != null ? name : getBlockState().getBlock().getName(); }

    @Override
    protected void applyImplicitComponents(@NotNull DataComponentInput componentInput) {
        super.applyImplicitComponents(componentInput);
        this.name = componentInput.get(DataComponents.CUSTOM_NAME);

        if (isAdvanced) {
            var data = componentInput.get(CustomComponents.TARGET_RESOURCE);
            if (data != null) {
                target_allowlist = data.is_allowlist();
                if (targets != null) {
                    targets.clear();
                    targets.addAll(data.targets());
                }
            }
            var d2 = componentInput.get(CustomComponents.TARGET_CATEGORY);
            if (d2 != null) {
                categories_allowlist = d2.is_allowlist();
                if (categories != null) {
                    categories.clear();
                    for(String source : d2.targets()) {
                        SoundSource src;
                        try {
                            src = SoundSource.valueOf(source);
                        } catch(IllegalArgumentException ex) {
                            continue;
                        }
                        categories.add(src);
                    }
                }
            }
        }
    }

    @Override
    protected void collectImplicitComponents(@NotNull DataComponentMap.Builder components) {
        super.collectImplicitComponents(components);
        components.set(DataComponents.CUSTOM_NAME, this.name);
        if (isAdvanced) {
            if (!target_allowlist || (targets != null && !targets.isEmpty())) {
                components.set(CustomComponents.TARGET_RESOURCE, new CustomComponents.TargetResourceData(
                        target_allowlist,
                        targets == null ? List.of() : List.copyOf(targets)
                ));
            } else
                components.set(CustomComponents.TARGET_RESOURCE, null);

            if (!categories_allowlist || (categories != null && !categories.isEmpty())) {
                components.set(CustomComponents.TARGET_CATEGORY, new CustomComponents.TargetCategoryData(
                        categories_allowlist,
                        categories == null ? List.of() : categories.stream().map(Enum::name).toList()
                ));
            } else
                components.set(CustomComponents.TARGET_CATEGORY, null);
        }
    }

    @Override
    public void removeComponentsFromTag(CompoundTag tag) {
        super.removeComponentsFromTag(tag);
        tag.remove("CustomName");
        tag.remove("targets");
        tag.remove("target_allowlist");
        tag.remove("categories");
        tag.remove("categories_allowlist");
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

    //region Offset Calculations

    public int getOffset(Direction.Axis axis) {
        return switch (axis) {
            case Direction.Axis.X -> offset_x;
            case Direction.Axis.Y -> offset_y;
            case Direction.Axis.Z -> offset_z;
        };
    }

    public boolean setOffset(Direction.Axis axis, int value, boolean update) {
        return switch (axis) {
            case Direction.Axis.X -> setOffsetX(value, update);
            case Direction.Axis.Y -> setOffsetY(value, update);
            case Direction.Axis.Z -> setOffsetZ(value, update);
        };
    }

    public int getOffsetX() { return offset_x; }
    public int getOffsetY() { return offset_y; }
    public int getOffsetZ() { return offset_z; }

    public void setOffset(int x, int y, int z) {
        boolean update = setOffsetX(x, false);
        update = setOffsetY(y, false) || update;
        update = setOffsetZ(z, update) || update;
        if (update)
            updateMuffler();
    }

    public boolean setOffsetX(int value, boolean update) {
        int old = offset_x;
        if (isAdvanced)
            offset_x = Math.clamp(value, getMinOffset(Direction.Axis.X), getMaxOffset(Direction.Axis.X));
        else
            offset_x = 0;
        if (offset_x != old) {
            if (update)
                updateMuffler();
            return true;
        }
        return false;
    }

    public boolean setOffsetY(int value, boolean update) {
        int old = offset_y;
        if (isAdvanced)
            offset_y = Math.clamp(value, getMinOffset(Direction.Axis.Y), getMaxOffset(Direction.Axis.Y));
        else
            offset_y = 0;
        if (offset_y != old) {
            if (update)
                updateMuffler();
            return true;
        }
        return false;
    }

    public boolean setOffsetZ(int value, boolean update) {
        int old = offset_z;
        if (isAdvanced)
            offset_z = Math.clamp(value, getMinOffset(Direction.Axis.Z), getMaxOffset(Direction.Axis.Z));
        else
            offset_z = 0;
        if (offset_z != old) {
            if (update)
                updateMuffler();
            return true;
        }
        return false;
    }

    public int getMinOffset(Direction.Axis axis) {
        if (range == 0)
            return 0;

        final var state = getBlockState();
        final var face = state.getValue(FaceAttachedHorizontalDirectionalBlock.FACE);
        final var facing = face == AttachFace.FLOOR
                ? Direction.UP
                : face == AttachFace.CEILING
                ? Direction.DOWN
                : state.getValue(FaceAttachedHorizontalDirectionalBlock.FACING);

        if (axis == facing.getAxis())
            return facing.getAxisDirection() == Direction.AxisDirection.NEGATIVE ? -1 : 0;
        return -range;
    }

    public int getMaxOffset(Direction.Axis axis) {
        if (range == 0)
            return 0;

        final var state = getBlockState();
        final var face = state.getValue(FaceAttachedHorizontalDirectionalBlock.FACE);
        final var facing = face == AttachFace.FLOOR
                ? Direction.UP
                : face == AttachFace.CEILING
                ? Direction.DOWN
                : state.getValue(FaceAttachedHorizontalDirectionalBlock.FACING);

        if (axis == facing.getAxis())
            return facing.getAxisDirection() == Direction.AxisDirection.POSITIVE ? 1 : 0;
        return range;
    }

    //endregion

    //region Muffler Logic

    public BlockPos getCenter() {
        final var state = getBlockState();
        final var pos = getBlockPos().offset(offset_x, offset_y, offset_z);
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

    public void toggleContainmentMode() {
        if (isAdvanced)
            containmentMode = ! containmentMode;
        else
            containmentMode = false;
    }

    public boolean getContainmentMode() {
        return containmentMode;
    }

    public void setContainmentMode(boolean enabled) {
        containmentMode = isAdvanced && enabled;
    }

    public boolean addTargetCategory(SoundSource source) {
        if (isAdvanced && categories != null) {
            if (categories.add(source)) {
                setChanged();
                return true;
            }
        }

        return false;
    }

    public boolean removeTargetCategory(SoundSource source) {
        if (isAdvanced && categories != null) {
            if (categories.remove(source)) {
                setChanged();
                return true;
            }
        }

        return false;
    }

    @Nullable
    public Set<SoundSource> getTargetCategories() {
        return isAdvanced ? categories : null;
    }

    public boolean hasTargetCategories() {
        return isAdvanced && categories != null && ! categories.isEmpty();
    }

    public boolean hasTargetCategory(SoundSource source) {
        return isAdvanced && categories != null && categories.contains(source);
    }

    public boolean isTargetCategoryAllowlist() {
        return categories_allowlist;
    }

    public boolean setTargetCategoryAllowlist(boolean is_allowlist) {
        is_allowlist = is_allowlist && isAdvanced;
        if (is_allowlist != categories_allowlist) {
            categories_allowlist = is_allowlist;
            setChanged();
            return true;
        }

        return false;
    }

    public boolean addTargetSound(ResourceLocation location) {
        if (isAdvanced && targets != null) {
            if (targets.add(location)) {
                setChanged();
                return true;
            }
        }

        return false;
    }

    @Nullable
    public HashSet<ResourceLocation> getTargets() {
        return isAdvanced ? targets : null;
    }

    public boolean hasTargets() {
        return isAdvanced && targets != null && ! targets.isEmpty();
    }

    public boolean hasTarget(ResourceLocation location) {
        return isAdvanced && targets != null && targets.contains(location);
    }

    public boolean removeTargetSound(ResourceLocation location) {
        if (isAdvanced && targets != null) {
            if (targets.remove(location)) {
                setChanged();
                return true;
            }
        }

        return false;
    }

    public boolean isTargetAllowlist() {
        return target_allowlist;
    }

    public boolean setTargetAllowlist(boolean is_allowlist) {
        is_allowlist = is_allowlist && isAdvanced;
        if (is_allowlist != target_allowlist) {
            target_allowlist = is_allowlist;
            setChanged();
            return true;
        }

        return false;
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
        setOffsetX(offset_x, false);
        setOffsetY(offset_y, false);
        setOffsetZ(offset_z, false);
        updateMuffler();
    }

    public int getRange() { return range; }

    public boolean shouldAffectListener(Vec3 position) {
        if (!containmentMode)
            return true;

        return ! isInRange(position);
    }

    @Nullable
    public HashSet<ResourceLocation> getClientHeard() {
        return client_heard;
    }

    public void recordNearbySound(ResourceLocation location) {
        if (level != null && level.isClientSide) {
            if (client_heard == null)
                client_heard = new HashSet<>();
            client_heard.add(location);
        }
    }

    public boolean shouldAffectSound(ResourceLocation location, SoundSource source) {
        if (isAdvanced) {
            // This method returns true if a sound SHOULD be muffled.
            // Returning true = deny sound
            // Returning false = allow sound

            // First, if we matched the specific sound, then apply the
            // appropriate action.
            boolean matched_target = targets != null && targets.contains(location);
            if (matched_target)
                return !target_allowlist;

            // Next, check if the source is matched.
            boolean matched_category = categories != null && categories.contains(source);
            if (matched_category)
                return !categories_allowlist;

            // If the sounds list or category list is in Denylist mode,
            // and it wasn't a match, then we should allow it.
            return target_allowlist && categories_allowlist;

            // If those lists are both in Allowlist mode, and
            // we got this far, then the sound should be denied.
        }

        return true;
    }

    public boolean isInRange(Vec3 position) {
        final var center = Vec3.atCenterOf(getCenter());
        double r = range == 0 ? 0.5 : range + 0.5;

        return Math.abs(position.x - center.x) <= r &&
                Math.abs(position.y - center.y) <= r &&
                Math.abs(position.z - center.z) <= r;
    }

    public void updateMuffler() {
        final BlockState state = this.getBlockState();
        final Level level = this.getLevel();
        boolean removed = this.isRemoved();

        boolean should_enable = this.volume < 1 && !removed && state.getBlock() instanceof MufflerBlock && state.getValue(MufflerBlock.ENABLED) && level != null;
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
