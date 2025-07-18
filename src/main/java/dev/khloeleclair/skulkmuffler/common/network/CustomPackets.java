package dev.khloeleclair.skulkmuffler.common.network;

import dev.khloeleclair.skulkmuffler.SculkMufflerMod;
import dev.khloeleclair.skulkmuffler.client.screens.MufflerScreen;
import dev.khloeleclair.skulkmuffler.common.blockentities.MufflerBlockEntity;
import io.netty.buffer.ByteBuf;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.neoforged.neoforge.network.registration.HandlerThread;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import org.jetbrains.annotations.NotNull;

public class CustomPackets {

    public record OpenMufflerMenu(BlockPos pos) implements CustomPacketPayload {

        public static final CustomPacketPayload.Type<OpenMufflerMenu> TYPE = new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(SculkMufflerMod.MODID, "open_menu"));

        public static final StreamCodec<ByteBuf, OpenMufflerMenu> STREAM_CODEC = StreamCodec.composite(
                BlockPos.STREAM_CODEC,
                OpenMufflerMenu::pos,
                OpenMufflerMenu::new
        );

        @Override
        public @NotNull Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    public record UpdateMuffler(BlockPos pos, int range, double volume, int debug) implements CustomPacketPayload {

        public static final CustomPacketPayload.Type<UpdateMuffler> TYPE = new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(SculkMufflerMod.MODID, "update_muffler"));

        public static final StreamCodec<ByteBuf, UpdateMuffler> STREAM_CODEC = StreamCodec.composite(
                BlockPos.STREAM_CODEC,
                UpdateMuffler::pos,
                ByteBufCodecs.INT,
                UpdateMuffler::range,
                ByteBufCodecs.DOUBLE,
                UpdateMuffler::volume,
                ByteBufCodecs.INT,
                UpdateMuffler::debug,
                UpdateMuffler::new
        );

        @Override
        public @NotNull Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    public static void register(final RegisterPayloadHandlersEvent event) {

        final PayloadRegistrar registrar = event.registrar("1")
                .executesOn(HandlerThread.MAIN);

        registrar.playToClient(OpenMufflerMenu.TYPE, OpenMufflerMenu.STREAM_CODEC, (message, access) -> {
            onOpenMufflerMenu(message, access);
        });

        registrar.playToServer(UpdateMuffler.TYPE, UpdateMuffler.STREAM_CODEC, (message, access) -> {
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
    public static void onOpenMufflerMenu(OpenMufflerMenu message, IPayloadContext access) {
        final var level = access.player().level();
        if (level == null || !(level.getBlockEntity(message.pos) instanceof MufflerBlockEntity mbe))
            return;

        Minecraft.getInstance().setScreen(new MufflerScreen(mbe));
    }

}
