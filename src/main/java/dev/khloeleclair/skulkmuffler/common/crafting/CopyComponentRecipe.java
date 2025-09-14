package dev.khloeleclair.skulkmuffler.common.crafting;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.khloeleclair.skulkmuffler.SculkMufflerMod;
import dev.khloeleclair.skulkmuffler.common.data.CustomComponents;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.*;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;

public class CopyComponentRecipe extends CustomRecipe {

    @NotNull
    final Ingredient ingredient;

    public CopyComponentRecipe(@NotNull Ingredient ingredient) {
        super(CraftingBookCategory.MISC);
        this.ingredient = ingredient;
    }

    @NotNull
    public Ingredient getIngredient() {
        return ingredient;
    }

    @Override
    public @NotNull NonNullList<Ingredient> getIngredients() {
        return NonNullList.of(ingredient);
    }

    @Override
    public boolean matches(@NotNull CraftingInput input, @NotNull Level level) {

        int configured = 0;
        int unconfigured = 0;

        for(ItemStack stack : input.items()) {
            if (stack.isEmpty())
                continue;

            if (!ingredient.test(stack))
                return false;

            var components = stack.getComponents();
            if (components.has(CustomComponents.TARGET_RESOURCE.value()))
                configured++;
            else
                unconfigured++;
        }

        if (configured != 1)
            return false;

        return unconfigured > 0;

    }

    @Override
    @NotNull
    public ItemStack getResultItem(@NotNull HolderLookup.Provider registries) {
        var items = ingredient.getItems();
        if (items != null && items.length > 0)
            return items[0];

        return ItemStack.EMPTY;
    }

    @Override
    @NotNull
    public ItemStack assemble(@NotNull CraftingInput input, @NotNull HolderLookup.Provider registries) {
        ItemStack configured = ItemStack.EMPTY;
        int unconfigured = 0;

        for(ItemStack stack : input.items()) {
            if (stack.isEmpty())
                continue;

            if (!ingredient.test(stack))
                return ItemStack.EMPTY;

            var components = stack.getComponents();
            if (components.has(CustomComponents.TARGET_RESOURCE.value())) {
                configured = stack.copy();
                configured.setCount(1);
            } else
                unconfigured++;
        }

        if (!configured.isEmpty() && unconfigured > 0)
            configured.setCount(1 + unconfigured);

        return configured;
    }

    @Override
    public boolean canCraftInDimensions(int width, int height) {
        return width * height >= 2;
    }

    @Override
    @NotNull
    public RecipeSerializer<?> getSerializer() {
        return SculkMufflerMod.COPY_COMPONENT_RECIPE_SERIALIZER.get();
    }


    public static class Serializer implements RecipeSerializer<CopyComponentRecipe> {

        private static final MapCodec<CopyComponentRecipe> CODEC = RecordCodecBuilder.mapCodec(builder -> builder.group(
                Ingredient.CODEC.fieldOf("ingredient").forGetter(CopyComponentRecipe::getIngredient)
        ).apply(builder, CopyComponentRecipe::new));

        public static final StreamCodec<RegistryFriendlyByteBuf, CopyComponentRecipe> STREAM_CODEC = StreamCodec.composite(
                Ingredient.CONTENTS_STREAM_CODEC, CopyComponentRecipe::getIngredient,
                CopyComponentRecipe::new
        );

        @Override
        public @NotNull MapCodec<CopyComponentRecipe> codec() {
            return CODEC;
        }

        @Override
        public @NotNull StreamCodec<RegistryFriendlyByteBuf, CopyComponentRecipe> streamCodec() {
            return STREAM_CODEC;
        }

    }

}
