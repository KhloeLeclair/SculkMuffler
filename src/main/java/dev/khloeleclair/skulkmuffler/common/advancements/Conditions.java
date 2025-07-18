package dev.khloeleclair.skulkmuffler.common.advancements;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.khloeleclair.skulkmuffler.SculkMufflerMod;
import dev.khloeleclair.skulkmuffler.common.Config;
import net.neoforged.neoforge.common.conditions.ICondition;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;
import org.jetbrains.annotations.NotNull;

import java.util.function.Supplier;

public class Conditions {

    public static final DeferredRegister<MapCodec<? extends ICondition>> CONDITION_CODECS = DeferredRegister.create(NeoForgeRegistries.CONDITION_SERIALIZERS, SculkMufflerMod.MODID);
    public static final Supplier<MapCodec<ConfigEnabledCondition>> CONFIG_ENABLED = CONDITION_CODECS.register("config_enabled", () -> ConfigEnabledCondition.CODEC);

    public record ConfigEnabledCondition(String key) implements ICondition {

        public static final MapCodec<ConfigEnabledCondition> CODEC = RecordCodecBuilder.mapCodec(inst -> inst.group(
                Codec.STRING.fieldOf("key").forGetter(ConfigEnabledCondition::key)
        ).apply(inst, ConfigEnabledCondition::new));

        @Override
        public boolean test(@NotNull IContext context) {
            switch(key) {
                case "wardenBlockVibrations":
                    return Config.Common.wardenBlockVibrations.get();
                case "wardenSonicNullifyVolume":
                    return Config.Common.wardenSonicNullifyVolume.get() >= 0;
                case "wardenSonicDamage":
                    return Config.Common.wardenSonicDamage.get() != Config.SonicDamageMode.DISABLED;
                case "bellHeardVolume":
                    return Config.Common.bellHeardVolume.get() >= 0;
                case "bellHighlightVolume":
                    return Config.Common.bellHighlightVolume.get() >= 0;
                case "vibrationVolume":
                    return Config.Common.vibrationVolume.get() >= 0;
            }

            return false;
        }

        @Override
        public @NotNull MapCodec<? extends ICondition> codec() {
            return CODEC;
        }
    }

}
