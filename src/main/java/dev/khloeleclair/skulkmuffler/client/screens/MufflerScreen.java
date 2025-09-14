package dev.khloeleclair.skulkmuffler.client.screens;

import dev.khloeleclair.skulkmuffler.SculkMufflerMod;
import dev.khloeleclair.skulkmuffler.client.gui.widgets.AdjustableExtendedSlider;
import dev.khloeleclair.skulkmuffler.client.gui.widgets.IconButton;
import dev.khloeleclair.skulkmuffler.common.Config;
import dev.khloeleclair.skulkmuffler.common.blockentities.MufflerBlockEntity;
import dev.khloeleclair.skulkmuffler.common.network.CustomPackets;
import dev.khloeleclair.skulkmuffler.common.utilities.Constants;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.StringWidget;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.layouts.FrameLayout;
import net.minecraft.client.gui.layouts.GridLayout;
import net.minecraft.client.gui.layouts.SpacerElement;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.client.gui.widget.ExtendedSlider;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class MufflerScreen extends Screen {

    public static final ResourceLocation RANGE_ON = ResourceLocation.fromNamespaceAndPath(SculkMufflerMod.MODID, "widget/redstone_torch");
    public static final ResourceLocation RANGE_OFF = ResourceLocation.fromNamespaceAndPath(SculkMufflerMod.MODID, "widget/redstone_torch_off");

    private final MufflerBlockEntity mbe;
    private int waitingTicks = 0;

    @Nullable
    private IconButton btnRange;
    @Nullable
    private Button btnContainment;

    @Nullable
    private ExtendedSlider rangeSlider;
    @Nullable
    private ExtendedSlider volumeSlider;

    @Nullable
    private StringWidget[] offsetLabels;
    @Nullable
    private AdjustableExtendedSlider[] offsetSlider;

    public MufflerScreen(MufflerBlockEntity mbe) {
        super(mbe.getDisplayName());
        this.mbe = mbe;
    }

    @Override
    protected void init() {
        final var font = Minecraft.getInstance().font;

        GridLayout layout = new GridLayout();
        layout.defaultCellSetting().padding(4, 4, 4, 0).alignVerticallyMiddle();
        var helper = layout.createRowHelper(3);

        helper.addChild(new StringWidget(title, font), 3);

        rangeSlider = new ExtendedSlider(0, 0, 100, 20, Component.empty(), Component.empty(), 0.0, Config.Common.maxRange.get(), mbe.getRange(), 1.0, 1, true) {
            @Override
            public @NotNull String getValueString() {
                return String.format("%d", (int)this.getValue() * 2 + 1);
            }

            @Override
            protected void applyValue() {
                mbe.setRange(getValueInt());
                updateOffsetSliders();
                scheduleUpdate();
            }
        };

        double minVolume = Math.floor(Config.Common.minVolume.get() * 100.0);
        double current = this.mbe.getVolume() * 100.0;

        volumeSlider = new ExtendedSlider(0, 0, 100, 20, Component.empty(), Component.literal("%"), minVolume, 100.0, current, 1.0, 1, true) {
            @Override
            protected void applyValue() {
                mbe.setVolume(getValue() / 100.0);
                scheduleUpdate();
            }
        };

        if (Config.Client.rangeRenderer.get() != Config.RangeRenderer.DISABLED) {
            btnRange = IconButton.builder(Component.empty(), e -> {
                mbe.toggleDebug();
                updateRangeButton();
                scheduleUpdate();
            }).width(100).build();
            updateRangeButton();
        }

        if (mbe.isAdvanced) {
            btnContainment = Button.builder(Component.empty(), e -> {
                mbe.toggleContainmentMode();
                updateContainmentButton();
                scheduleUpdate();
            }).width(100).build();
            updateContainmentButton();
        }

        helper.addChild(new StringWidget(Component.translatable("sculkmuffler.gui.range"), font));
        helper.addChild(rangeSlider, btnRange == null ? 2 : 1);

        if (btnRange != null)
            helper.addChild(btnRange);

        helper.addChild(new StringWidget(Component.translatable("sculkmuffler.gui.volume"), font));
        helper.addChild(volumeSlider, btnContainment == null ? 2 : 1);

        if (btnContainment != null)
            helper.addChild(btnContainment);

        helper.addChild(new SpacerElement(50, 20), 2);
        if (mbe.isAdvanced && mbe.getTargets() != null) {
            var tt = Component.translatable("sculkmuffler.gui.sound-" + (mbe.isTargetAllowlist() ? "allowlist" : "denylist"),
                    Component.translatable("sculkmuffler.gui.sound-count", mbe.getTargets().size())
                    ).append("\n").append(
                    Component.translatable("sculkmuffler.gui.sound.about").setStyle(Constants.GRAY)
            );

            helper.addChild(IconButton.builder(Component.translatable("sculkmuffler.gui.sound"), e -> {
                if (waitingTicks >= 0)
                    sendUpdate();
                Minecraft.getInstance().setScreen(new MufflerSoundListScreen(mbe));
            }).width(100).tooltip(tt).build());
        }

        if (mbe.isAdvanced) {
            var values = Direction.Axis.values();
            offsetLabels = new StringWidget[values.length];
            offsetSlider = new AdjustableExtendedSlider[values.length];

            var tt = Component.translatable("sculkmuffler.gui.category-" + (mbe.isTargetAllowlist() ? "allowlist" : "denylist"),
                    Component.translatable("sculkmuffler.gui.sound-count", mbe.getTargets().size())
            ).append("\n").append(
                    Component.translatable("sculkmuffler.gui.category.about").setStyle(Constants.GRAY)
            );

            var btnCategories = IconButton.builder(Component.translatable("sculkmuffler.gui.category"), e -> {
                if (waitingTicks >= 0)
                    sendUpdate();
                Minecraft.getInstance().setScreen(new MufflerCategoryListScreen(mbe));
            }).width(100).tooltip(tt).build();

            for(int i = 0; i < values.length; i++) {
                final var axis = values[i];

                final var label = new StringWidget(Component.literal(axis.getName()), font);
                offsetLabels[i] = label;
                helper.addChild(label);

                final int min = mbe.getMinOffset(axis);
                final int max = mbe.getMaxOffset(axis);

                final var slider = new AdjustableExtendedSlider(0, 0, 100, 20, Component.empty(), Component.empty(), min, max, mbe.getOffset(axis), 1.0, 1, true) {
                    @Override
                    protected void applyValue() {
                        mbe.setOffset(axis, getValueInt(), true);
                        scheduleUpdate();
                    }
                };
                offsetSlider[i] = slider;

                helper.addChild(slider, i == 0 ? 1 : 2);
                if (i == 0)
                    helper.addChild(btnCategories);

                boolean visible = min < max;
                slider.visible = visible;
                label.visible = visible;
            }

        }

        layout.arrangeElements();
        FrameLayout.alignInRectangle(layout, 0, 0, this.width, this.height, 0.5f, 0.5f);
        layout.visitWidgets(this::addRenderableWidget);
    }

    public void updateOffsetSliders() {
        if (offsetSlider == null || offsetLabels == null)
            return;

        var values = Direction.Axis.values();
        for(int i = 0; i < values.length; i++) {
            final var axis = values[i];
            final var slider = offsetSlider[i];
            final var label = offsetLabels[i];
            if (slider == null || label == null)
                continue;

            final int min = mbe.getMinOffset(axis);
            final int max = mbe.getMaxOffset(axis);

            slider.setMinValue(min);
            slider.setMaxValue(max);
            slider.setValue(mbe.getOffset(axis));

            boolean visible = min < max;
            slider.visible = visible;
            label.visible = visible;
        }
    }


    public void updateRangeButton() {
        if (btnRange == null)
            return;

        var msg = Component.translatable("sculkmuffler.gui.range").append(": ");
        if (mbe.getDebug() != -1)
            //btnRange.setSprite(RANGE_ON);
            msg.append(Component.translatable("sculkmuffler.gui.state.enabled"));
        else
            //btnRange.setSprite(RANGE_OFF);
            msg.append(Component.translatable("sculkmuffler.gui.state.disabled"));

        btnRange.setMessage(msg);
        btnRange.setTooltip(Tooltip.create(Component.translatable("sculkmuffler.gui.show-range").append("\n").append(
                Component.translatable("sculkmuffler.gui.range.about").setStyle(Constants.GRAY)
        )));
    }

    public void updateContainmentButton() {
        if (btnContainment == null)
            return;

        var msg = Component.translatable("sculkmuffler.gui.contain").append(": ");
        if (mbe.getContainmentMode())
            msg.append(Component.translatable("sculkmuffler.gui.state.enabled"));
        else
            msg.append(Component.translatable("sculkmuffler.gui.state.disabled"));

        btnContainment.setMessage(msg);
        btnContainment.setTooltip(Tooltip.create(Component.translatable("sculkmuffler.gui.containment").append("\n").append(
                Component.translatable("sculkmuffler.gui.contain.about").setStyle(Constants.GRAY)
        )));
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public void onClose() {
        sendUpdate();
        super.onClose();
    }

    @Override
    public void tick() {
        super.tick();
        if (waitingTicks > 0) {
            waitingTicks--;
            if (waitingTicks == 0)
                sendUpdate();
        }
    }

    public void scheduleUpdate() {
        waitingTicks = 5;
    }

    public void sendUpdate() {
        final var range = rangeSlider == null ? 0 : rangeSlider.getValueInt();
        final var volume = volumeSlider == null ? 0.0 : volumeSlider.getValue() / 100.0;
        waitingTicks = -1;

        mbe.setRange(range);
        mbe.setVolume(volume);

        CustomPackets.UpdateMuffler.sendUpdate(mbe);
    }
}

