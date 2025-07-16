package dev.khloeleclair.skulkmuffler;

import dev.khloeleclair.skulkmuffler.common.MufflerTracker;
import dev.khloeleclair.skulkmuffler.common.blocks.MufflerBlock;
import dev.khloeleclair.skulkmuffler.common.blockentities.MufflerBlockEntity;
import dev.khloeleclair.skulkmuffler.common.Config;
import dev.khloeleclair.skulkmuffler.common.network.CustomPackets;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;

import com.mojang.logging.LogUtils;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.event.server.ServerStartingEvent;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

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
        BLOCKS.register(modEventBus);
        ITEMS.register(modEventBus);
        BLOCK_ENTITY_TYPES.register(modEventBus);

        // Register ourselves for server and other game events we are interested in.
        // Note that this is necessary if and only if we want *this* class (SculkMuffler) to respond directly to events.
        // Do not add this line if there are no @SubscribeEvent-annotated functions in this class, like onServerStarting() below.
        NeoForge.EVENT_BUS.register(this);

        // Register the item to a creative tab
        modEventBus.addListener(this::addCreative);

        // Register our packets
        CustomPackets.register();

        // Register our mod's ModConfigSpec so that FML can create and load the config file for us
        Config.register(modContainer);
    }

    // Add the example block item to the building blocks tab
    private void addCreative(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey() == CreativeModeTabs.REDSTONE_BLOCKS) {
            event.accept(MUFFLER_BLOCK_ITEM);
        }
    }

    // You can use SubscribeEvent and let the Event Bus discover methods to call
    @SubscribeEvent
    public void onServerStarting(ServerStartingEvent event) {
        // Do something when the server starts
        Server = new WeakReference<>(event.getServer());
    }
}
