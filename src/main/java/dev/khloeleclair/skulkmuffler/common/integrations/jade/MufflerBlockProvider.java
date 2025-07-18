package dev.khloeleclair.skulkmuffler.common.integrations.jade;

import dev.khloeleclair.skulkmuffler.SculkMufflerMod;
import dev.khloeleclair.skulkmuffler.common.blockentities.MufflerBlockEntity;
import dev.khloeleclair.skulkmuffler.common.blocks.MufflerBlock;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;
import net.minecraft.resources.ResourceLocation;
import snownee.jade.api.BlockAccessor;
import snownee.jade.api.IBlockComponentProvider;
import snownee.jade.api.ITooltip;
import snownee.jade.api.config.IPluginConfig;

public class MufflerBlockProvider implements IBlockComponentProvider {

    public static final ResourceLocation MUFFLER = ResourceLocation.fromNamespaceAndPath(SculkMufflerMod.MODID, "muffler");

    public static final Style GREEN = Style.EMPTY.withColor(TextColor.fromLegacyFormat(ChatFormatting.GREEN));
    public static final Style RED = Style.EMPTY.withColor(TextColor.fromLegacyFormat(ChatFormatting.RED));

    @Override
    public ResourceLocation getUid() {
        return MUFFLER;
    }

    @Override
    public void appendTooltip(
            ITooltip tooltip,
            BlockAccessor accessor,
            IPluginConfig config
    ) {
        if (!(accessor.getBlockEntity() instanceof MufflerBlockEntity mbe))
            return;

        final int range = mbe.getRange() * 2 + 1;
        final int volume = (int) Math.floor(mbe.getVolume() * 100.0);
        final var enabled = accessor.getBlockState().getValue(MufflerBlock.ENABLED);

        tooltip.add(Component.translatable("sculkmuffler.gui.state").append(": "));
        if (enabled)
            tooltip.append(Component.translatable("sculkmuffler.gui.state.enabled").setStyle(GREEN));
        else
            tooltip.append(Component.translatable("sculkmuffler.gui.state.disabled").setStyle(RED));

        tooltip.add(Component.translatable("sculkmuffler.gui.range").append(": ").append(String.format("%d", range)));
        tooltip.add(Component.translatable("sculkmuffler.gui.volume").append(": ").append(String.format("%d%%", volume)));
    }
}
