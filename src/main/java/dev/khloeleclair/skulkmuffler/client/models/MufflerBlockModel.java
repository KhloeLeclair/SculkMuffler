package dev.khloeleclair.skulkmuffler.client.models;

import dev.khloeleclair.skulkmuffler.SculkMufflerMod;
import dev.khloeleclair.skulkmuffler.common.blockentities.MufflerBlockEntity;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

public class MufflerBlockModel extends GeoModel<MufflerBlockEntity> {

    private final ResourceLocation MODEL = ResourceLocation.fromNamespaceAndPath(SculkMufflerMod.MODID, "geo/muffler.geo.json");
    private final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath("minecraft", "textures/item/ender_pearl.png");
    private final ResourceLocation ANIMATIONS = ResourceLocation.fromNamespaceAndPath(SculkMufflerMod.MODID, "animations/muffler.animation.json");

    @Override
    public ResourceLocation getModelResource(MufflerBlockEntity animatable) {
        return MODEL;
    }

    @Override
    public ResourceLocation getTextureResource(MufflerBlockEntity animatable) {
        return TEXTURE;
    }

    @Override
    public ResourceLocation getAnimationResource(MufflerBlockEntity animatable) {
        return ANIMATIONS;
    }

}
