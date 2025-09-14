package dev.khloeleclair.skulkmuffler.common.blocks;

import com.mojang.serialization.MapCodec;
import dev.khloeleclair.skulkmuffler.common.data.CustomComponents;
import dev.khloeleclair.skulkmuffler.common.utilities.Constants;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class AdvancedMufflerBlock extends MufflerBlock {

    public static final MapCodec<AdvancedMufflerBlock> CODEC = simpleCodec(AdvancedMufflerBlock::new);

    public AdvancedMufflerBlock(Properties properties) {
        super(properties);
    }

    @Override
    public @NotNull MapCodec<? extends AdvancedMufflerBlock> codec() { return CODEC; }

    public int getOffset(int number_of_lines) {
        if (number_of_lines <= 5)
            return 0;

        int max_offset = number_of_lines - 5;
        long duration = (long) max_offset * 250 + 1250;

        int offset = (int) (System.currentTimeMillis() % duration) / 250;
        if (offset > max_offset)
            return max_offset;
        return offset;
    }

    @Override
    public void appendHoverText(@NotNull ItemStack stack, @NotNull Item.TooltipContext context, @NotNull List<Component> tooltipComponents, @NotNull TooltipFlag tooltipFlag) {
        super.appendHoverText(stack, context, tooltipComponents, tooltipFlag);
        var targets = stack.getComponents().get(CustomComponents.TARGET_RESOURCE.value());
        if (targets != null) {
            var list = targets.targets();
            tooltipComponents.add(Component.translatable("sculkmuffler.gui.sound-" + (targets.is_allowlist() ? "allowlist" : "denylist"),
                    Component.translatable("sculkmuffler.gui.sound-count", list.size()).setStyle(Constants.GRAY)).setStyle(targets.is_allowlist() ? Constants.GREEN : Constants.RED));
            if (tooltipFlag.isAdvanced() && !list.isEmpty()) {
                if (tooltipFlag.hasShiftDown()) {
                    int i_start = getOffset(list.size());
                    int i_limit = Math.min(list.size(), i_start + 5);

                    for(int i = i_start; i < i_limit; i++) {
                        tooltipComponents.add(Component.literal("- ").setStyle(Constants.GRAY).append(list.get(i).toString()));
                    }
                } else {
                    tooltipComponents.add(Component.translatable("sculkmuffler.gui.sounds-hold", Component.translatable("key.keyboard.left.shift").setStyle(Constants.RESET)).setStyle(Constants.GRAY));
                }
            }
        }

        var categories = stack.getComponents().get(CustomComponents.TARGET_CATEGORY.value());
        if (categories != null) {
            var list = categories.targets();
            tooltipComponents.add(Component.translatable("sculkmuffler.gui.category-" + (categories.is_allowlist() ? "allowlist" : "denylist"),
                    Component.translatable("sculkmuffler.gui.sound-count", list.size()).setStyle(Constants.GRAY)).setStyle(categories.is_allowlist() ? Constants.GREEN : Constants.RED));
            if (tooltipFlag.isAdvanced() && !list.isEmpty()) {
                if (tooltipFlag.hasShiftDown()) {
                    int i_start = getOffset(list.size());
                    int i_limit = Math.min(list.size(), i_start + 5);

                    for(int i = i_start; i < i_limit; i++) {
                        tooltipComponents.add(Component.literal("- ").setStyle(Constants.GRAY).append(list.get(i)));
                    }
                } else {
                    tooltipComponents.add(Component.translatable("sculkmuffler.gui.sounds-hold", Component.translatable("key.keyboard.left.shift").setStyle(Constants.RESET)).setStyle(Constants.GRAY));
                }
            }
        }
    }

}
