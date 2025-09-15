package dev.khloeleclair.skulkmuffler.client.models;

import dev.khloeleclair.skulkmuffler.SculkMufflerMod;
import dev.khloeleclair.skulkmuffler.common.blockentities.MufflerBlockEntity;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;
import software.bernie.geckolib.model.GeoModel;
import software.bernie.geckolib.renderer.GeoRenderer;

public class MufflerBlockModel extends GeoModel<MufflerBlockEntity> {

    private final ResourceLocation MODEL = ResourceLocation.fromNamespaceAndPath(SculkMufflerMod.MODID, "geo/muffler.geo.json");
    private final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath("minecraft", "textures/item/ender_pearl.png");
    private final ResourceLocation ANIMATIONS = ResourceLocation.fromNamespaceAndPath(SculkMufflerMod.MODID, "animations/muffler.animation.json");
    private final ResourceLocation ADVANCED_ANIMATIONS = ResourceLocation.fromNamespaceAndPath(SculkMufflerMod.MODID, "animations/advanced_muffler.animation.json");

    private final ResourceLocation MODEL_CONTAINMENT = ResourceLocation.fromNamespaceAndPath(SculkMufflerMod.MODID, "geo/muffler_containment.geo.json");
    private final ResourceLocation TEXTURE_CONTAINMENT = ResourceLocation.fromNamespaceAndPath("minecraft", "textures/block/slime_block.png");

    private boolean containment_pass;

    public boolean isContainmentPass() {
        return containment_pass;
    }

    public void setContainmentPass(boolean enabled) {
        containment_pass = enabled;
    }

    @Override
    public @Nullable RenderType getRenderType(MufflerBlockEntity animatable, ResourceLocation texture) {
        if (containment_pass)
            return RenderType.entityTranslucent(texture);
        return super.getRenderType(animatable, texture);
    }

    /*@Override
    public ResourceLocation getModelResource(MufflerBlockEntity animatable) {
        return containment_pass ? MODEL_CONTAINMENT : MODEL;
    }

    @Override
    public ResourceLocation getTextureResource(MufflerBlockEntity animatable) {
        return containment_pass ? TEXTURE_CONTAINMENT : TEXTURE;
    }*/

    @Override
    public ResourceLocation getModelResource(MufflerBlockEntity animatable, @Nullable GeoRenderer<MufflerBlockEntity> renderer) {
        return containment_pass ? MODEL_CONTAINMENT : MODEL;
    }

    @Override
    public ResourceLocation getTextureResource(MufflerBlockEntity animatable, @Nullable GeoRenderer<MufflerBlockEntity> renderer) {
        return containment_pass ? TEXTURE_CONTAINMENT : TEXTURE;
    }

    @Override
    public ResourceLocation getAnimationResource(MufflerBlockEntity animatable) {
        return animatable.isAdvanced ? ADVANCED_ANIMATIONS : ANIMATIONS;
    }

}
