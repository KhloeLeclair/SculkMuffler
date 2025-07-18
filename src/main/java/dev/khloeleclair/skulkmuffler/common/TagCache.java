package dev.khloeleclair.skulkmuffler.common;

import dev.khloeleclair.skulkmuffler.SculkMufflerMod;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.TagsUpdatedEvent;

import java.util.Set;
import java.util.stream.Collectors;

public class TagCache {

    private static Set<ResourceLocation> ignoreSounds = null;

    public static Set<ResourceLocation> getIgnoreSounds() {
        if (ignoreSounds == null) {
            ignoreSounds = BuiltInRegistries.SOUND_EVENT.getOrCreateTag(SculkMufflerMod.IGNORE_SOUND_TAG)
                    .stream()
                    .map(x -> x.getKey().location()).collect(Collectors.toUnmodifiableSet());
        }
        return ignoreSounds;
    }

    @SubscribeEvent
    public static void onTagsUpdated(TagsUpdatedEvent evt) {
        ignoreSounds = null;
    }

}
