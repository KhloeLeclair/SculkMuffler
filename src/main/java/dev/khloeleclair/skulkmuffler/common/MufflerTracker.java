package dev.khloeleclair.skulkmuffler.common;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import com.google.common.collect.Multimaps;
import dev.khloeleclair.skulkmuffler.common.blockentities.MufflerBlockEntity;
import dev.khloeleclair.skulkmuffler.common.utilities.MathHelpers;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.apache.commons.lang3.tuple.Pair;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.WeakHashMap;

public class MufflerTracker {

    private final WeakHashMap<Level, Multimap<ChunkPos, Pair<BlockPos, BlockPos>>> MufflerMap;
    private final WeakHashMap<MufflerBlockEntity, Pair<BlockPos, BlockPos>> KnownMufflers;

    public MufflerTracker() {
        MufflerMap = new WeakHashMap<>();
        KnownMufflers = new WeakHashMap<>();
    }

    public void addMuffler(@NotNull MufflerBlockEntity muffler) {
        final var target = muffler.getCenter();
        final var pos = muffler.getBlockPos();
        final var pair = Pair.of(pos, target);

        var existing = KnownMufflers.get(muffler);
        if (existing != null) {
            if (existing == pair)
                return;

            // Remove the old entry so we can add a new one.
            removeMuffler(muffler);
        }

        final var level = muffler.getLevel();
        if (level == null)
            return;

        var map = MufflerMap.get(level);
        if (map == null) {
            map = Multimaps.synchronizedSetMultimap(HashMultimap.create());
            MufflerMap.put(level, map);
        }

        KnownMufflers.put(muffler, pair);
        ChunkPos chunk = new ChunkPos(target);
        synchronized (map) {
            map.put(chunk, pair);
        }
    }

    public void removeMuffler(@Nullable MufflerBlockEntity muffler) {
        if (muffler == null)
            return;

        var target = KnownMufflers.remove(muffler);
        final var level = muffler.getLevel();
        if (level == null)
            return;

        final var map = MufflerMap.get(level);
        if (map == null)
            return;

        if (target == null)
            target = Pair.of(muffler.getBlockPos(), muffler.getCenter());

        final ChunkPos chunk = new ChunkPos(target.getRight());
        synchronized (map) {
           map.remove(chunk, target);
        }
    }

    public boolean isEmpty(@NotNull Level level) {
        final var map = MufflerMap.get(level);
        return map == null;
    }

    public double getVolume(@NotNull Level level, @NotNull Vec3 pos, @Nullable Vec3 listenerPos, @Nullable ResourceLocation sound, @Nullable SoundSource soundSource) {
        final var map = MufflerMap.get(level);
        if (map == null)
            return 1.0;

        final int range = Config.Common.maxRange.get();
        final int minX = SectionPos.blockToSectionCoord(pos.x - range);
        final int maxX = SectionPos.blockToSectionCoord(pos.x + range);
        final int minZ = SectionPos.blockToSectionCoord(pos.z - range);
        final int maxZ = SectionPos.blockToSectionCoord(pos.z + range);

        float volume = MathHelpers.linearToDb(1.0);

        for(int x = minX; x <= maxX; x++) {
            for(int z = minZ; z <= maxZ; z++) {
                for(var pair : map.get(new ChunkPos(x, z))) {
                    var be = level.getBlockEntity(pair.getLeft());
                    if (be instanceof MufflerBlockEntity mbe && mbe.isInRange(pos)) {
                        if (sound != null)
                            mbe.recordNearbySound(sound);

                        if ((listenerPos == null || mbe.shouldAffectListener(listenerPos)) && (sound == null || mbe.shouldAffectSound(sound, soundSource))) {
                            volume += mbe.getVolumeDB();
                        }
                    }
                }
            }
        }

        return Math.max(0, MathHelpers.dBtoLinear(volume));
    }

    public Pair<Double, MufflerBlockEntity> getNearbyAndVolume(@NotNull Level level, @NotNull Vec3 pos, @Nullable Vec3 listenerPos, @Nullable ResourceLocation sound, @Nullable SoundSource soundSource) {
        final var map = MufflerMap.get(level);
        if (map == null)
            return Pair.of(1.0,null);

        final int range = Config.Common.maxRange.get();
        final int minX = SectionPos.blockToSectionCoord(pos.x - range);
        final int maxX = SectionPos.blockToSectionCoord(pos.x + range);
        final int minZ = SectionPos.blockToSectionCoord(pos.z - range);
        final int maxZ = SectionPos.blockToSectionCoord(pos.z + range);

        MufflerBlockEntity nearest = null;
        double dist = Double.POSITIVE_INFINITY;
        float volume = MathHelpers.linearToDb(1.0);

        for(int x = minX; x <= maxX; x++) {
            for(int z = minZ; z <= maxZ; z++) {
                for(var pair : map.get(new ChunkPos(x, z))) {
                    var be = level.getBlockEntity(pair.getLeft());
                    if (be instanceof MufflerBlockEntity mbe && mbe.isInRange(pos)) {
                        if (sound != null)
                            mbe.recordNearbySound(sound);
                        if ((listenerPos == null || mbe.shouldAffectListener(listenerPos)) && (sound == null || mbe.shouldAffectSound(sound, soundSource))) {
                            volume += mbe.getVolumeDB();
                            double d = pos.distanceToSqr(pair.getLeft().getCenter());
                            if (d < dist) {
                                dist = d;
                                nearest = mbe;
                            }
                        }
                    }
                }
            }
        }

        return Pair.of(Math.max(0, MathHelpers.dBtoLinear(volume)), nearest);
    }

}
