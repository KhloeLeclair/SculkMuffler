package dev.khloeleclair.skulkmuffler;

import com.mojang.logging.LogUtils;
import dev.khloeleclair.skulkmuffler.common.Config;
import dev.khloeleclair.skulkmuffler.common.MufflerTracker;
import dev.khloeleclair.skulkmuffler.common.TagCache;
import dev.khloeleclair.skulkmuffler.common.advancements.Conditions;
import dev.khloeleclair.skulkmuffler.common.advancements.Triggers;
import dev.khloeleclair.skulkmuffler.common.blockentities.MufflerBlockEntity;
import dev.khloeleclair.skulkmuffler.common.blocks.MufflerBlock;
import dev.khloeleclair.skulkmuffler.common.network.CustomPackets;
import dev.khloeleclair.skulkmuffler.common.utilities.MathHelpers;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.tags.TagKey;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.server.ServerStartingEvent;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;

import java.lang.ref.WeakReference;
import java.util.function.Supplier;

// The value here should match an entry in the META-INF/neoforge.mods.toml file
@Mod(SculkMufflerMod.MODID)
public class SculkMufflerMod {

    public static SculkMufflerMod Instance;

    public static final String MODID = "sculkmuffler";
    public static final Logger LOGGER = LogUtils.getLogger();

    // Registries
    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(MODID);
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(MODID);
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITY_TYPES = DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, SculkMufflerMod.MODID);

    // Tags
    public static final TagKey<SoundEvent> IGNORE_SOUND_TAG = TagKey.create(
            Registries.SOUND_EVENT,
            ResourceLocation.fromNamespaceAndPath(SculkMufflerMod.MODID, "ignore_sound")
    );

    // Muffler Block
    public static final DeferredBlock<MufflerBlock> MUFFLER_BLOCK = BLOCKS.registerBlock(
            "muffler", MufflerBlock::new, BlockBehaviour.Properties.of()
                    .mapColor(MapColor.WOOL)
                    .destroyTime(2.5f)
                    .explosionResistance(2.5f)
                    .sound(SoundType.WOOL));
    public static final DeferredItem<BlockItem> MUFFLER_BLOCK_ITEM = ITEMS.registerSimpleBlockItem("muffler", MUFFLER_BLOCK);
    public static final Supplier<BlockEntityType<MufflerBlockEntity>> MUFFLER_BLOCK_ENTITY = BLOCK_ENTITY_TYPES.register(
            "muffler_entity",
            () -> BlockEntityType.Builder.of(MufflerBlockEntity::new, MUFFLER_BLOCK.get()).build(null)
    );

    public final MufflerTracker Tracker;
    private WeakReference<MinecraftServer> Server;

    @Nullable
    public static MinecraftServer GetServer() {
        return Instance != null && Instance.Server != null ? Instance.Server.get() : null;
    }

    // The constructor for the mod class is the first code that is run when your mod is loaded.
    // FML will recognize some parameter types like IEventBus or ModContainer and pass them in automatically.
    public SculkMufflerMod(IEventBus modEventBus, ModContainer modContainer) {
        Instance = this;
        Tracker = new MufflerTracker();

        // Register our registries.
        Conditions.CONDITION_CODECS.register(modEventBus);
        BLOCKS.register(modEventBus);
        ITEMS.register(modEventBus);
        BLOCK_ENTITY_TYPES.register(modEventBus);
        Triggers.TRIGGER_TYPES.register(modEventBus);

        // Register ourselves for server and other game events we are interested in.
        // Note that this is necessary if and only if we want *this* class (SculkMuffler) to respond directly to events.
        // Do not add this line if there are no @SubscribeEvent-annotated functions in this class, like onServerStarting() below.
        NeoForge.EVENT_BUS.register(this);
        NeoForge.EVENT_BUS.register(TagCache.class);
        modEventBus.addListener(CustomPackets::register);

        // Register the item to a creative tab
        modEventBus.addListener(this::addCreative);

        // Register our mod's ModConfigSpec so that FML can create and load the config file for us
        Config.register(modContainer);
    }

    // Add the example block item to the building blocks tab
    private void addCreative(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey() == CreativeModeTabs.REDSTONE_BLOCKS) {
            event.accept(MUFFLER_BLOCK_ITEM);
        }
    }

    @SubscribeEvent
    public void onDamage(LivingIncomingDamageEvent event) {
        final var source = event.getSource();
        if (source.is(DamageTypes.SONIC_BOOM)) {
            final var mode = Config.Common.wardenSonicDamage.get();
            final var entity = source.getEntity();
            if (mode == Config.SonicDamageMode.DISABLED || entity == null)
                return;

            float amount = event.getAmount();
            final var volume = Tracker.getVolume(entity.level(), entity.position());
            if (volume >= 1)
                return;

            if (volume <= MathHelpers.logToLinear(Config.Common.wardenSonicNullifyVolume.get()))
                event.setCanceled(true);
            else {
                amount = amount * (float) volume;
                event.setAmount(Math.max(amount, (float) Math.min(event.getAmount(), Config.Common.wardenSonicDamageMin.get())));
            }

            if (event.getEntity() instanceof ServerPlayer sp)
                Triggers.muteSonicBoom(sp);
        }
    }

    // You can use SubscribeEvent and let the Event Bus discover methods to call
    @SubscribeEvent
    public void onServerStarting(ServerStartingEvent event) {
        // Do something when the server starts
        Server = new WeakReference<>(event.getServer());
    }
}
