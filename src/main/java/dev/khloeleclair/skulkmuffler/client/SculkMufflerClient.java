package dev.khloeleclair.skulkmuffler.client;

import dev.khloeleclair.skulkmuffler.SculkMufflerMod;
import dev.khloeleclair.skulkmuffler.client.renderers.MufflerBlockEntityRenderer;
import dev.khloeleclair.skulkmuffler.common.Config;
import dev.khloeleclair.skulkmuffler.common.MufflerTracker;
import dev.khloeleclair.skulkmuffler.common.blockentities.MufflerBlockEntity;
import dev.khloeleclair.skulkmuffler.common.network.CustomPackets;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.*;
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

@Mod(value = SculkMufflerMod.MODID, dist = Dist.CLIENT)
@EventBusSubscriber(modid = SculkMufflerMod.MODID, value = Dist.CLIENT)
public class SculkMufflerClient {

    public static SculkMufflerClient Instance;

    public final MufflerTracker Tracker;

    public SculkMufflerClient(ModContainer container) {
        Instance = this;
        Tracker = new MufflerTracker();

        // Allows NeoForge to create a config screen for this mod's configs.
        // The config screen is accessed by going to the Mods screen > clicking on your mod > clicking on config.
        // Do not forget to add translations for your config options to the en_us.json file.
        container.registerExtensionPoint(IConfigScreenFactory.class, ConfigurationScreen::new);

        CustomPackets.registerClient();
    }

    @SubscribeEvent
    public static void registerRenderers(final EntityRenderersEvent.RegisterRenderers event) {
        event.registerBlockEntityRenderer(SculkMufflerMod.MUFFLER_BLOCK_ENTITY.get(), MufflerBlockEntityRenderer::new);
    }

    @SubscribeEvent
    static void onSoundPlaying(PlaySoundEvent event) {
        if (Instance == null || Instance.Tracker == null)
            return;

        final var level = Minecraft.getInstance().level;
        final var sound = event.getSound();
        if (level == null || sound == null)
            return;

        final var immune = Config.Client.immuneSources.get();
        if (!immune.isEmpty() && immune.contains(sound.getSource().toString()))
            return;

        final var pos = sound instanceof RidingMinecartSoundInstance rmsi
            ? rmsi.minecart.position()
            : new Vec3(sound.getX(), sound.getY(), sound.getZ());
        Pair<Double, MufflerBlockEntity> pair = Instance.Tracker.getNearbyAndVolume(level, pos);

        final double volume = pair.getLeft();
        if (volume >= 1)
            return;

        final var mbe = pair.getRight();

        if (mbe != null)
            mbe.drawSculkParticle(pos);

        // We do ATSI separately to ensure they aren't nulled.
        if (sound instanceof AbstractTickableSoundInstance atsi)
            atsi.volume = atsi.volume * (float) volume;
        else if (volume < 0.0001)
            event.setSound(null);
        else if (sound instanceof AbstractSoundInstance asi) {
            asi.volume = asi.volume * (float) volume;
        } else {
            // Don't know what kind of sound we have, replace it and hope for the best.
            sound.resolve(event.getEngine().soundManager);
            float oldVolume = sound.getVolume();
            event.setSound(new SimpleSoundInstance(
                    sound.getLocation(),
                    sound.getSource(),
                    oldVolume * (float) volume,
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
