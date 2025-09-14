package dev.khloeleclair.skulkmuffler.client.gui.widgets;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.sounds.SoundManager;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.function.Function;
import java.util.function.Supplier;


@OnlyIn(Dist.CLIENT)
public class IconButton extends AbstractButton {

    protected static final Button.CreateNarration DEFAULT_NARRATION = Supplier::get;

    protected final OnPress onPress;
    @Nullable
    protected final OnFocusChange onFocusChange;
    protected final Button.CreateNarration createNarration;

    private boolean last_hovered_or_focused;

    @Nullable
    protected ResourceLocation spriteLocation;
    @Nullable
    protected ResourceLocation spriteSelectedLocation;
    protected int spriteWidth;
    protected int spriteHeight;

    protected boolean enableClickSound;

    public static IconButton.Builder builder(Component message, IconButton.OnPress onPress) {
        return new IconButton.Builder(message, onPress);
    }

    public static IconButton.Builder builder(ResourceLocation sprite, IconButton.OnPress onPress) {
        return new IconButton.Builder(sprite, onPress);
    }

    public static IconButton.Builder builder(ResourceLocation sprite, Component message, IconButton.OnPress onPress) {
        return new IconButton.Builder(sprite, message, onPress);
    }

    public static IconButton.Builder builder(ResourceLocation sprite, ResourceLocation spriteSelected, IconButton.OnPress onPress) {
        return new IconButton.Builder(sprite, spriteSelected, onPress);
    }

    public static IconButton.Builder builder(ResourceLocation sprite, ResourceLocation spriteSelected, Component message, IconButton.OnPress onPress) {
        return new IconButton.Builder(sprite, spriteSelected, message, onPress);
    }

    protected IconButton(
            int x, int y, int width, int height, Component message, @Nullable ResourceLocation spriteLocation, @Nullable ResourceLocation spriteSelectedLocation, int spriteWidth, int spriteHeight, OnPress onPress, OnFocusChange onFocusChange, Button.CreateNarration createNarration
    ) {
        super(x, y, width, height, message);
        this.spriteLocation = spriteLocation;
        this.spriteSelectedLocation = spriteSelectedLocation;
        this.spriteWidth = spriteWidth;
        this.spriteHeight = spriteHeight;
        this.onPress = onPress;
        this.onFocusChange = onFocusChange;
        this.createNarration = createNarration;
    }

    protected IconButton(Builder builder) {
        super(builder.x, builder.y, builder.width, builder.height, builder.message);
        this.setTooltip(builder.tooltip);
        this.spriteLocation = builder.sprite;
        this.spriteWidth = builder.spriteWidth;
        this.spriteHeight = builder.spriteHeight;
        this.spriteSelectedLocation = builder.spriteSelected;
        this.onPress = builder.onPress;
        this.onFocusChange = builder.onFocusChange;
        this.createNarration = builder.createNarration;
        this.enableClickSound = builder.enableClickSound;
    }

    public IconButton setSprite(@Nullable ResourceLocation location) {
        this.spriteLocation = location;
        return this;
    }

    public IconButton setSprite(@Nullable ResourceLocation location, int width, int height) {
        this.spriteLocation = location;
        this.spriteWidth = width;
        this.spriteHeight = height;
        return this;
    }

    public IconButton setSprite(@Nullable ResourceLocation location, @Nullable ResourceLocation selectedLocation) {
        this.spriteLocation = location;
        this.spriteSelectedLocation = selectedLocation;
        return this;
    }

    public IconButton setSprite(@Nullable ResourceLocation location, @Nullable ResourceLocation selectedLocation, int width, int height) {
        this.spriteLocation = location;
        this.spriteSelectedLocation = selectedLocation;
        this.spriteWidth = width;
        this.spriteHeight = height;
        return this;
    }

    public IconButton clickSound() {
        enableClickSound = true;
        return this;
    }

    public IconButton clickSound(boolean enabled) {
        enableClickSound = enabled;
        return this;
    }

    @Override
    public void setFocused(boolean focused) {
        super.setFocused(focused);
        final var hovered_or_focused = isHoveredOrFocused();
        if (last_hovered_or_focused != hovered_or_focused) {
            last_hovered_or_focused = hovered_or_focused;
            if (onFocusChange != null)
                onFocusChange.onFocusChange(this);
        }
    }

    @Override
    protected void renderWidget(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        final var hovered_or_focused = isHoveredOrFocused();
        if (last_hovered_or_focused != hovered_or_focused) {
            last_hovered_or_focused = hovered_or_focused;
            if (onFocusChange != null)
                onFocusChange.onFocusChange(this);
        }
        super.renderWidget(guiGraphics, mouseX, mouseY, partialTick);
    }

    @Override
    public void renderString(@NotNull GuiGraphics guiGraphics, @NotNull Font font, int color) {
        int offset = 2;
        Component msg = getMessage();
        String msg_string = msg.getString();
        boolean no_message = msg_string == null || msg_string.isEmpty();

        if (spriteLocation != null) {
            ResourceLocation sprite = isHoveredOrFocused() && spriteSelectedLocation != null ? spriteSelectedLocation : spriteLocation;

            if (no_message)
                offset = (width - spriteWidth) / 2;

            int offsetY = (height - spriteHeight) / 2;

            guiGraphics.blitSprite(sprite, getX() + offset, getY() + offsetY, spriteWidth, spriteHeight);
            offset += 2 + spriteWidth;
        }

        if (!no_message)
            this.renderScrollingString(guiGraphics, font, offset, color);
    }

    @Override
    public void onPress() { this.onPress.onPress(this); }

    @Override
    protected MutableComponent createNarrationMessage() {
        return this.createNarration.createNarrationMessage(() -> super.createNarrationMessage());
    }

    @Override
    public void playDownSound(@NotNull SoundManager handler) {
        if (enableClickSound)
            super.playDownSound(handler);
    }

    @Override
    protected void updateWidgetNarration(@NotNull NarrationElementOutput narrationElementOutput) {
        this.defaultButtonNarrationText(narrationElementOutput);
    }

    @OnlyIn(Dist.CLIENT)
    public static class Builder {
        @Nullable
        private ResourceLocation sprite;
        private Component message;
        private final OnPress onPress;
        @Nullable OnFocusChange onFocusChange;
        @Nullable Tooltip tooltip;
        @Nullable
        private ResourceLocation spriteSelected;
        private int x;
        private int y;
        private int width = 150;
        private int height = 20;
        private int spriteWidth = 16;
        private int spriteHeight = 16;
        private boolean enableClickSound = true;
        private Button.CreateNarration createNarration = DEFAULT_NARRATION;

        public Builder(Component message, OnPress onPress) {
            this.message = message;
            this.sprite = null;
            this.spriteSelected = null;
            this.onPress = onPress;
        }

        public Builder(@Nullable ResourceLocation sprite, @Nullable ResourceLocation spriteSelected, OnPress onPress) {
            this.sprite = sprite;
            this.spriteSelected = spriteSelected;
            this.message = Component.empty();
            this.onPress = onPress;
        }

        public Builder(@Nullable ResourceLocation sprite, OnPress onPress) {
            this.sprite = sprite;
            this.message = Component.empty();
            this.onPress = onPress;
        }

        public Builder(@Nullable ResourceLocation sprite, @Nullable ResourceLocation spriteSelected, Component message, OnPress onPress) {
            this.sprite = sprite;
            this.spriteSelected = spriteSelected;
            this.message = message;
            this.onPress = onPress;
        }

        public Builder(@Nullable ResourceLocation sprite, Component message, OnPress onPress) {
            this.sprite = sprite;
            this.message = message;
            this.onPress = onPress;
        }

        public Builder focusChange(OnFocusChange onFocusChange) {
            this.onFocusChange = onFocusChange;
            return this;
        }

        public Builder clickSound(boolean enabled) {
            this.enableClickSound = enabled;
            return this;
        }

        public Builder sprite(ResourceLocation sprite) {
            this.sprite = sprite;
            return this;
        }

        public Builder sprite(ResourceLocation sprite, int width, int height) {
            this.sprite = sprite;
            this.spriteWidth = width;
            this.spriteHeight = height;
            return this;
        }

        public Builder spriteSelected(ResourceLocation spriteSelected) {
            this.spriteSelected = spriteSelected;
            return this;
        }

        public Builder message(Component message) {
            this.message = message;
            return this;
        }

        public Builder spriteSize(int width, int height) {
            this.spriteWidth = width;
            this.spriteHeight = height;
            return this;
        }

        public Builder pos(int x, int y) {
            this.x = x;
            this.y = y;
            return this;
        }

        public Builder width(int width) {
            this.width = width;
            return this;
        }

        public Builder size(int width, int height) {
            this.width = width;
            this.height = height;
            return this;
        }

        public Builder bounds(int x, int y, int width, int height) {
            return this.pos(x, y).size(width, height);
        }

        public Builder tooltip(@Nullable Tooltip tooltip) {
            this.tooltip = tooltip;
            return this;
        }

        public Builder tooltip(@Nullable Component message) {
            this.tooltip = message == null ? null : Tooltip.create(message);
            return this;
        }

        public Builder createNarration(Button.CreateNarration createNarration) {
            this.createNarration = createNarration;
            return this;
        }

        @NotNull
        public IconButton build() { return build(IconButton::new); }

        public IconButton build(Function<Builder, IconButton> builder) { return builder.apply(this); }

    }

    @OnlyIn(Dist.CLIENT)
    public interface OnPress {
        void onPress(IconButton button);
    }

    @OnlyIn(Dist.CLIENT)
    public interface OnFocusChange {
        void onFocusChange(IconButton button);
    }

}
