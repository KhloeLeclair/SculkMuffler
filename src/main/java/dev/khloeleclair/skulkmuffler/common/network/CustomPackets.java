package dev.khloeleclair.skulkmuffler.common.network;

import dev.khloeleclair.skulkmuffler.SculkMufflerMod;
import dev.khloeleclair.skulkmuffler.client.screens.MufflerScreen;
import dev.khloeleclair.skulkmuffler.common.blockentities.MufflerBlockEntity;
import dev.khloeleclair.skulkmuffler.common.utilities.BiggerStreams;
import io.netty.buffer.ByteBuf;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.ChunkPos;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.network.PacketDistributor;
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

    public record ModifyMuffledList(BlockPos pos, boolean added, ResourceLocation location) implements CustomPacketPayload {

        public static final CustomPacketPayload.Type<ModifyMuffledList> TYPE = new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(SculkMufflerMod.MODID, "modify_muffled"));

        public static final StreamCodec<ByteBuf, ModifyMuffledList> STREAM_CODEC = StreamCodec.composite(
                BlockPos.STREAM_CODEC,
                ModifyMuffledList::pos,
                ByteBufCodecs.BOOL,
                ModifyMuffledList::added,
                ResourceLocation.STREAM_CODEC,
                ModifyMuffledList::location,
                ModifyMuffledList::new
        );

        @Override
        public @NotNull Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }

    }


    public record ModifyMuffledCategoryList(BlockPos pos, boolean added, String source) implements CustomPacketPayload {

        public static final CustomPacketPayload.Type<ModifyMuffledCategoryList> TYPE = new CustomPacketPayload.Type<>(
                ResourceLocation.fromNamespaceAndPath(SculkMufflerMod.MODID, "modify_muffled_category"));

        public static final StreamCodec<ByteBuf, ModifyMuffledCategoryList> STREAM_CODEC = StreamCodec.composite(
                BlockPos.STREAM_CODEC,
                ModifyMuffledCategoryList::pos,
                ByteBufCodecs.BOOL,
                ModifyMuffledCategoryList::added,
                ByteBufCodecs.STRING_UTF8,
                ModifyMuffledCategoryList::source,
                ModifyMuffledCategoryList::new
        );

        @Override
        public @NotNull Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }

    }


    public record UpdateMuffler(BlockPos pos, int range, int offset_x, int offset_y, int offset_z, double volume, int debug, boolean containment, boolean target_whitelist, boolean category_whitelist) implements CustomPacketPayload {

        public static final CustomPacketPayload.Type<UpdateMuffler> TYPE = new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(SculkMufflerMod.MODID, "update_muffler"));

        public static final StreamCodec<ByteBuf, UpdateMuffler> STREAM_CODEC = BiggerStreams.composite(
                BlockPos.STREAM_CODEC,
                UpdateMuffler::pos,
                ByteBufCodecs.INT,
                UpdateMuffler::range,
                ByteBufCodecs.INT,
                UpdateMuffler::offset_x,
                ByteBufCodecs.INT,
                UpdateMuffler::offset_y,
                ByteBufCodecs.INT,
                UpdateMuffler::offset_z,
                ByteBufCodecs.DOUBLE,
                UpdateMuffler::volume,
                ByteBufCodecs.INT,
                UpdateMuffler::debug,
                ByteBufCodecs.BOOL,
                UpdateMuffler::containment,
                ByteBufCodecs.BOOL,
                UpdateMuffler::target_whitelist,
                ByteBufCodecs.BOOL,
                UpdateMuffler::category_whitelist,
                UpdateMuffler::new
        );

        @Override
        public @NotNull Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }

        public static void sendUpdate(MufflerBlockEntity mbe) {
            PacketDistributor.sendToServer(new UpdateMuffler(
                    mbe.getBlockPos(),
                    mbe.getRange(),
                    mbe.getOffsetX(),
                    mbe.getOffsetY(),
                    mbe.getOffsetZ(),
                    mbe.getVolume(),
                    mbe.getDebug(),
                    mbe.getContainmentMode(),
                    mbe.isTargetAllowlist(),
                    mbe.isTargetCategoryAllowlist()
            ));
        }

    }

    public static void register(final RegisterPayloadHandlersEvent event) {

        final PayloadRegistrar registrar = event.registrar("2")
                .executesOn(HandlerThread.MAIN);

        registrar.playToClient(OpenMufflerMenu.TYPE, OpenMufflerMenu.STREAM_CODEC, (message, access) -> onOpenMufflerMenu(message, access));

        registrar.playBidirectional(ModifyMuffledList.TYPE, ModifyMuffledList.STREAM_CODEC, CustomPackets::onModifyMuffledList);
        registrar.playBidirectional(ModifyMuffledCategoryList.TYPE, ModifyMuffledCategoryList.STREAM_CODEC, CustomPackets::onModifyMuffledCategoryList);

        registrar.playToServer(UpdateMuffler.TYPE, UpdateMuffler.STREAM_CODEC, (message, access) -> {
            final var level = access.player().level();
            if (level == null || !(level.getBlockEntity(message.pos) instanceof MufflerBlockEntity mbe))
                return;

            mbe.setRange(message.range);
            mbe.setVolume(message.volume);
            mbe.setDebug(message.debug);
            mbe.setOffset(message.offset_x, message.offset_y, message.offset_z);
            mbe.setContainmentMode(message.containment);
            mbe.setTargetAllowlist(message.target_whitelist);
            mbe.setTargetCategoryAllowlist(message.category_whitelist);
            mbe.sendClientUpdate();
            mbe.setChanged();
        });

    }

    public static void onModifyMuffledList(ModifyMuffledList message, IPayloadContext access) {
        final var level = access.player().level();
        if (level == null || !(level.getBlockEntity(message.pos) instanceof MufflerBlockEntity mbe))
            return;

        boolean changed;
        if (message.added)
            changed = mbe.addTargetSound(message.location);
        else
            changed = mbe.removeTargetSound(message.location);

        if (changed && (level instanceof ServerLevel sl)) {
            // Send this around to nearby players instead of sending the whole packet.
            PacketDistributor.sendToPlayersTrackingChunk(sl, new ChunkPos(mbe.getBlockPos()), new ModifyMuffledList(message.pos, message.added, message.location));
            mbe.setChanged();
        }
    }

    public static void onModifyMuffledCategoryList(ModifyMuffledCategoryList message, IPayloadContext access) {
        final var level = access.player().level();
        if (level == null || !(level.getBlockEntity(message.pos) instanceof MufflerBlockEntity mbe))
            return;

        SoundSource source;
        try {
            source = SoundSource.valueOf(message.source);
        } catch (IllegalArgumentException ex) {
            SculkMufflerMod.LOGGER.warn("Ignoring ModifyMuffledCategoryList packet with unknown category: {}", message.source);
            return;
        }

        boolean changed;
        if (message.added)
            changed = mbe.addTargetCategory(source);
        else
            changed = mbe.removeTargetCategory(source);

        if (changed && (level instanceof ServerLevel sl)) {
            // Send this around to nearby players instead of sending the whole packet.
            PacketDistributor.sendToPlayersTrackingChunk(sl, new ChunkPos(mbe.getBlockPos()), new ModifyMuffledCategoryList(message.pos, message.added, message.source));
            mbe.setChanged();
        }
    }

    @OnlyIn(Dist.CLIENT)
    public static void onOpenMufflerMenu(OpenMufflerMenu message, IPayloadContext access) {
        final var level = access.player().level();
        if (level == null || !(level.getBlockEntity(message.pos) instanceof MufflerBlockEntity mbe))
            return;

        Minecraft.getInstance().setScreen(new MufflerScreen(mbe));
    }

}
