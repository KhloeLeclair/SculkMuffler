package dev.khloeleclair.skulkmuffler.common.utilities;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Style;

public class Constants {

    public static final int[] AREAS = {
            0x999999,
            0x009999,
            0x991099
    };

    public static final Style YELLOW = Style.EMPTY.withColor(ChatFormatting.YELLOW);
    public static final Style GRAY = Style.EMPTY.withColor(ChatFormatting.GRAY);
    public static final Style RED = Style.EMPTY.withColor(ChatFormatting.RED);
    public static final Style DARK_RED = Style.EMPTY.withColor(ChatFormatting.DARK_RED);
    public static final Style GREEN = Style.EMPTY.withColor(ChatFormatting.GREEN);
    public static final Style DARK_GREEN = Style.EMPTY.withColor(ChatFormatting.DARK_GREEN);

    public static final Style RESET = Style.EMPTY.withColor(ChatFormatting.RESET);

}
