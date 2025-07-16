package dev.khloeleclair.skulkmuffler.common;

import io.wispforest.owo.ui.core.Sizing;
import net.minecraft.client.Minecraft;
import net.minecraft.sounds.SoundSource;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.common.ModConfigSpec;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.random.RandomGenerator;

// An example config class. This is not required, but it's a good idea to have one to keep your config organized.
// Demonstrates how to use Neo's config APIs
public class Config {

    public enum RangeRenderer {
        DISABLED,
        SOLID,
        LINES
    }

    public static void register(ModContainer modContainer) {
        modContainer.registerConfig(ModConfig.Type.CLIENT, clientSpec);
        modContainer.registerConfig(ModConfig.Type.COMMON, commonSpec);
    }

    public static class _Common {
        public final ModConfigSpec.IntValue maxRange;
        public final ModConfigSpec.IntValue defaultRange;
        public final ModConfigSpec.DoubleValue minVolume;
        public final ModConfigSpec.DoubleValue vibrationVolume;
        public final ModConfigSpec.DoubleValue bellHighlightVolume;
        public final ModConfigSpec.DoubleValue bellHeardVolume;

        _Common(ModConfigSpec.Builder builder) {
            builder.comment("Common Configuration").push("common");

            maxRange = builder
                    .comment("Maximum range for sound muffling from a placed muffler.")
                    .translation("sculkmuffler.config.muffling.range.max")
                    .worldRestart()
                    .defineInRange("maxRange", 16, 1, 16);

            defaultRange = builder
                    .comment("Default range for newly placed sound mufflers.")
                    .translation("sculkmuffler.config.muffling.range.default")
                    .defineInRange("defaultRange", 3, 0, 16);

            minVolume = builder
                    .comment("Minimum volume adjustment for muffled sounds. Set to zero to completely disable sounds.")
                    .translation("sculkmuffler.config.muffling.volume")
                    .worldRestart()
                    .defineInRange("minVolume", 0.0, 0.0, 1.0);

            vibrationVolume = builder
                    .comment("Vibrations will be prevented if sounds are this volume or lower.")
                    .translation("sculkmuffler.config.muffling.vibration")
                    .defineInRange("vibrationVolume", 0.01, 0.0, 1.0);

            bellHeardVolume = builder
                    .comment("Bells will not be heard by nearby entities if sounds are this volume or lower.")
                    .translation("sculkmuffler.config.muffling.bell.heard")
                    .defineInRange("bellHeardVolume", 0.01, 0.0, 1.0);

            bellHighlightVolume = builder
                    .comment("Bells will not highlight nearby raiders if sounds are this volume or lower.")
                    .translation("sculkmuffler.config.muffling.bell.highlight")
                    .defineInRange("bellHighlightVolume", 0.01, 0.0, 1.0);

            builder.pop();
        }
    }

    public static class _Client {
        public final ModConfigSpec.ConfigValue<List<? extends String>> immuneSources;
        public final ModConfigSpec.EnumValue<RangeRenderer> rangeRenderer;

        _Client(ModConfigSpec.Builder builder) {
            builder.comment("Client-Only Configuration").push("client");

            immuneSources = builder
                    .comment("Sounds with this source will not be silenced. Does not affect vibrations.")
                    .translation("sculkmuffler.config.muffling.sources.immune")
                    .defineListAllowEmpty("immuneSources", new ArrayList<>(), () -> {
                        var vals = SoundSource.values();
                        return vals[RandomGenerator.getDefault().nextInt(vals.length)].toString();
                    }, o -> {
                        if (!(o instanceof String ostr))
                            return false;
                        try {
                            SoundSource.valueOf(ostr);
                            return true;
                        } catch(IllegalArgumentException e) {
                            return false;
                        }
                    });

            rangeRenderer = builder
                    .comment("How the range is rendered when requested. Solid can cause issues due to translucency rendering.")
                    .translation("sculkmuffler.config.muffling.range.renderer")
                    .defineEnum("rangeRenderer", RangeRenderer.LINES);

            builder.pop();
        }
    }

    static final ModConfigSpec clientSpec;
    public static final _Client Client;

    static {
        var pair = new ModConfigSpec.Builder().configure(_Client::new);
        clientSpec = pair.getRight();
        Client = pair.getLeft();
    }

    static final ModConfigSpec commonSpec;
    public static final _Common Common;

    static {
        var pair = new ModConfigSpec.Builder().configure(_Common::new);
        commonSpec = pair.getRight();
        Common = pair.getLeft();
    }

}
