package dev.khloeleclair.skulkmuffler.client.screens;

import dev.khloeleclair.skulkmuffler.common.Config;
import dev.khloeleclair.skulkmuffler.common.blockentities.MufflerBlockEntity;
import dev.khloeleclair.skulkmuffler.common.network.CustomPackets;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.StringWidget;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.layouts.FrameLayout;
import net.minecraft.client.gui.layouts.GridLayout;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.client.gui.widget.ExtendedSlider;
import net.neoforged.neoforge.network.PacketDistributor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class MufflerScreen extends Screen {

    private final MufflerBlockEntity mbe;
    private int waitingTicks = 0;

    @Nullable
    private ExtendedSlider rangeSlider;
    @Nullable
    private ExtendedSlider volumeSlider;

    public MufflerScreen(MufflerBlockEntity mbe) {
        super(mbe.getBlockState().getBlock().getName());
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

        helper.addChild(new StringWidget(Component.translatable("sculkmuffler.gui.range"), font));
        helper.addChild(rangeSlider);

        helper.addChild(Button.builder(Component.literal("R"), e -> {
            mbe.toggleDebug();
            scheduleUpdate();
        }).width(20).tooltip(Tooltip.create(Component.translatable("sculkmuffler.gui.show-range"))).build());

        helper.addChild(new StringWidget(Component.translatable("sculkmuffler.gui.volume"), font));
        helper.addChild(volumeSlider, 2);

        layout.arrangeElements();
        FrameLayout.alignInRectangle(layout, 0, 0, this.width, this.height, 0.5f, 0.5f);
        layout.visitWidgets(this::addRenderableWidget);
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

        PacketDistributor.sendToServer(new CustomPackets.UpdateMuffler(
                this.mbe.getBlockPos(),
                range,
                volume,
                this.mbe.getDebug()
        ));
    }
}
