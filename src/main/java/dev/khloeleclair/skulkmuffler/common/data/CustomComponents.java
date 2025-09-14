package dev.khloeleclair.skulkmuffler.common.data;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.khloeleclair.skulkmuffler.SculkMufflerMod;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.List;

public class CustomComponents {

    public static final DeferredRegister.DataComponents REGISTRAR = DeferredRegister.createDataComponents(Registries.DATA_COMPONENT_TYPE, SculkMufflerMod.MODID);

    public static final DeferredHolder<DataComponentType<?>, DataComponentType<TargetResourceData>> TARGET_RESOURCE = REGISTRAR.registerComponentType(
            "target_resource",
            builder -> builder
                    .persistent(TargetResourceData.CODEC)
    );

    public static final DeferredHolder<DataComponentType<?>, DataComponentType<TargetCategoryData>> TARGET_CATEGORY = REGISTRAR.registerComponentType(
            "target_category",
            builder -> builder
                    .persistent(TargetCategoryData.CODEC)
    );

    public static void register(IEventBus modBus) {
        REGISTRAR.register(modBus);
    }

    public record TargetResourceData(boolean is_allowlist, List<ResourceLocation> targets) {

        public static final Codec<TargetResourceData> CODEC = RecordCodecBuilder.create(instance ->
                instance.group(
                        Codec.BOOL.fieldOf("is_allowlist").forGetter(TargetResourceData::is_allowlist),
                        ResourceLocation.CODEC.listOf().fieldOf("targets").forGetter(TargetResourceData::targets)
                ).apply(instance, TargetResourceData::new));

    }

    public record TargetCategoryData(boolean is_allowlist, List<String> targets) {

        public static final Codec<TargetCategoryData> CODEC = RecordCodecBuilder.create(instance ->
                instance.group(
                        Codec.BOOL.fieldOf("is_allowlist").forGetter(TargetCategoryData::is_allowlist),
                        Codec.STRING.listOf().fieldOf("targets").forGetter(TargetCategoryData::targets)
                ).apply(instance, TargetCategoryData::new));

    }

}
