package dev.khloeleclair.skulkmuffler.common.network;

import dev.khloeleclair.skulkmuffler.SculkMufflerMod;
import dev.khloeleclair.skulkmuffler.client.screens.MufflerScreen;
import dev.khloeleclair.skulkmuffler.common.blockentities.MufflerBlockEntity;
import io.wispforest.owo.network.OwoNetChannel;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.Level;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

public class CustomPackets {

    public static final OwoNetChannel CHANNEL = OwoNetChannel.create(ResourceLocation.fromNamespaceAndPath(SculkMufflerMod.MODID, "main"));

    public record OpenMufflerMenu(BlockPos pos) {}

    public record UpdateMuffler(BlockPos pos, int range, double volume, int debug) {}

    public static void register() {
        CHANNEL.registerClientboundDeferred(OpenMufflerMenu.class);
        CHANNEL.registerServerbound(UpdateMuffler.class, (message, access) -> {

            final var level = access.player().level();
            if (level == null || !(level.getBlockEntity(message.pos) instanceof MufflerBlockEntity mbe))
                return;

            mbe.setRange(message.range);
            mbe.setVolume(message.volume);
            mbe.setDebug(message.debug);
            mbe.sendClientUpdate();
            mbe.setChanged();
        });
    }

    @OnlyIn(Dist.CLIENT)
    public static void registerClient() {
        CHANNEL.registerClientbound(OpenMufflerMenu.class, ((message, access) -> {
            final var level = access.player().level();
            if (level == null || !(level.getBlockEntity(message.pos) instanceof MufflerBlockEntity mbe))
                return;

            Minecraft.getInstance().setScreen(new MufflerScreen(mbe));
        }));
    }

}
