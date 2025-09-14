package dev.khloeleclair.skulkmuffler.client;

import dev.khloeleclair.skulkmuffler.SculkMufflerMod;
import dev.khloeleclair.skulkmuffler.client.renderers.MufflerBlockEntityRenderer;
import dev.khloeleclair.skulkmuffler.common.Config;
import dev.khloeleclair.skulkmuffler.common.MufflerTracker;
import dev.khloeleclair.skulkmuffler.common.TagCache;
import dev.khloeleclair.skulkmuffler.common.blockentities.MufflerBlockEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.*;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.sound.PlaySoundEvent;
import net.neoforged.neoforge.client.gui.ConfigurationScreen;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;
import org.apache.commons.lang3.tuple.Pair;

import java.util.WeakHashMap;

@Mod(value = SculkMufflerMod.MODID, dist = Dist.CLIENT)
@EventBusSubscriber(modid = SculkMufflerMod.MODID, value = Dist.CLIENT)
public class SculkMufflerClient {

    public static SculkMufflerClient Instance;

    public final MufflerTracker Tracker;

    private final WeakHashMap<SoundInstance, Pair<Float, Float>> KnownVolumes;

    public SculkMufflerClient(ModContainer container) {
        Instance = this;
        Tracker = new MufflerTracker();
        KnownVolumes = new WeakHashMap<>();

        // Allows NeoForge to create a config screen for this mod's configs.
        // The config screen is accessed by going to the Mods screen > clicking on your mod > clicking on config.
        // Do not forget to add translations for your config options to the en_us.json file.
        container.registerExtensionPoint(IConfigScreenFactory.class, ConfigurationScreen::new);
    }

    @SubscribeEvent
    public static void registerRenderers(final EntityRenderersEvent.RegisterRenderers event) {
        event.registerBlockEntityRenderer(SculkMufflerMod.MUFFLER_BLOCK_ENTITY.get(), MufflerBlockEntityRenderer::new);
    }

    public static void handleSoundVolume(AbstractSoundInstance asi, float muffle_amount) {
        if (Instance == null)
            return;

        final var map = Instance.KnownVolumes;
        final float old_volume = asi.volume;

        var existing = map.get(asi);
        if (existing != null) {
            final float initial = existing.getLeft();
            final float previous = existing.getRight();

            if (asi.volume == previous) {
                asi.volume = Math.clamp(initial, 0f, 1f) * muffle_amount;
                if (asi.volume != previous) {
                    if (muffle_amount >= 1f)
                        map.remove(asi);
                    else
                        map.put(asi, Pair.of(initial, asi.volume));
                }
                return;
            }

        } else if (muffle_amount >= 1f)
            return;

        // If we got here, we either had no existing volume or the volume was
        // modified by an outside source so we need to store the new volume
        // as the new original.
        asi.volume = Math.clamp(old_volume, 0f, 1f) * muffle_amount;
        map.put(asi, Pair.of(old_volume, asi.volume));
    }

    @SubscribeEvent
    static void onSoundPlaying(PlaySoundEvent event) {
        if (Instance == null || Instance.Tracker == null)
            return;

        final var mc = Minecraft.getInstance();
        final var level = mc.level;
        final var sound = event.getSound();
        if (level == null || sound == null)
            return;

        final var immune = Config.Client.immuneSources.get();
        if (!immune.isEmpty() && immune.contains(sound.getSource().toString()))
            return;

        if (TagCache.getIgnoreSounds().contains(sound.getLocation()))
            return;

        final var listenerPos = mc.cameraEntity == null ? null : mc.cameraEntity.getEyePosition();

        final var pos = sound instanceof RidingMinecartSoundInstance rmsi
                ? rmsi.minecart.position()
                : new Vec3(sound.getX(), sound.getY(), sound.getZ());

        Pair<Double, MufflerBlockEntity> pair = Instance.Tracker.getNearbyAndVolume(level, pos, listenerPos, sound.getLocation(), sound.getSource());

        final double volume = pair.getLeft();
        if (volume < 1) {
            final var mbe = pair.getRight();
            if (mbe != null)
                mbe.drawSculkParticle(pos);
        }

        // We do ATSI separately to ensure they aren't nulled.
        if (sound instanceof AbstractTickableSoundInstance atsi) {
            handleSoundVolume(atsi, (float) volume);

        } else if (sound instanceof AbstractSoundInstance asi && sound.getSource() == SoundSource.RECORDS) {
            // We also handle record sounds separately.
            handleSoundVolume(asi, (float) volume);

        } else if (volume >= 1) {
            // Intentionally do nothing.

        } else if (volume < 0.0001) {
            // nullify other sounds that are too quiet.
            event.setSound(null);

        } else if (sound instanceof AbstractSoundInstance asi) {
            asi.volume = Math.clamp(asi.volume, 0f, 1f) * (float) volume;

        } else {
            // Don't know what kind of sound we have, replace it and hope for the best.
            sound.resolve(event.getEngine().soundManager);
            float oldVolume = sound.getVolume();
            event.setSound(new SimpleSoundInstance(
                    sound.getLocation(),
                    sound.getSource(),
                    Math.clamp(oldVolume, 0f, 1f) * (float) volume,
                    sound.getPitch(),
                    SoundInstance.createUnseededRandom(),
                    sound.isLooping(),
                    sound.getDelay(),
                    sound.getAttenuation(),
                    sound.getX(),
                    sound.getY(),
                    sound.getZ(),
                    false
            ));
        }
    }
}
