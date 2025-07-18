package dev.khloeleclair.skulkmuffler.common.advancements;

import dev.khloeleclair.skulkmuffler.SculkMufflerMod;
import net.minecraft.advancements.CriterionTrigger;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.function.Supplier;

public class Triggers {

    public static final DeferredRegister<CriterionTrigger<?>> TRIGGER_TYPES = DeferredRegister.create(Registries.TRIGGER_TYPE, SculkMufflerMod.MODID);

    public static final Supplier<CustomTrigger> MUTE_SONIC_BOOM = TRIGGER_TYPES.register("mute_sonic_boom", CustomTrigger::new);

    public static final Supplier<CustomTrigger> SILENCE_NEAR_WARDEN = TRIGGER_TYPES.register("silence_near_warden", CustomTrigger::new);

    public static void muteSonicBoom(ServerPlayer player) {
        MUTE_SONIC_BOOM.get().trigger(player);
    }

    public static void silenceNearWarden(ServerPlayer player) {
        SILENCE_NEAR_WARDEN.get().trigger(player);
    }

}
