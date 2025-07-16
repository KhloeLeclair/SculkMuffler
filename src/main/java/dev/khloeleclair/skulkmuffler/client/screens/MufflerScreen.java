package dev.khloeleclair.skulkmuffler.client.screens;

import dev.khloeleclair.skulkmuffler.SculkMufflerMod;
import dev.khloeleclair.skulkmuffler.common.Config;
import dev.khloeleclair.skulkmuffler.common.blockentities.MufflerBlockEntity;
import dev.khloeleclair.skulkmuffler.common.network.CustomPackets;
import io.wispforest.owo.ui.base.BaseOwoScreen;
import io.wispforest.owo.ui.component.Components;
import io.wispforest.owo.ui.component.DiscreteSliderComponent;
import io.wispforest.owo.ui.container.Containers;
import io.wispforest.owo.ui.container.FlowLayout;
import io.wispforest.owo.ui.core.*;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;

public class MufflerScreen extends BaseOwoScreen<FlowLayout> {

    private MufflerBlockEntity mbe;

    private DiscreteSliderComponent rangeSlider;
    private DiscreteSliderComponent volumeSlider;

    private int waitingTicks = 0;

    public MufflerScreen(MufflerBlockEntity mbe) {
        super(mbe.getBlockState().getBlock().getName());
        this.mbe = mbe;
    }

    @Override
    protected @NotNull OwoUIAdapter<FlowLayout> createAdapter() {
        return OwoUIAdapter.create(this, Containers::verticalFlow);
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
        CustomPackets.CHANNEL.clientHandle().send(new CustomPackets.UpdateMuffler(
                this.mbe.getBlockPos(),
                (int) rangeSlider.discreteValue(),
                logToLinear(volumeSlider.value()),
                this.mbe.getDebug()
        ));
    }

    public static double linearToLog(double input) {
        return Math.sqrt(input);
    }

    public static double logToLinear(double input) {
        return Math.pow(input, 2.0);
    }

    @Override
    protected void build(FlowLayout rootComponent) {
        rootComponent
                .surface(Surface.VANILLA_TRANSLUCENT)
                .horizontalAlignment(HorizontalAlignment.CENTER)
                .verticalAlignment(VerticalAlignment.CENTER);

        rangeSlider = Components.discreteSlider(Sizing.fixed(100), 0, Config.Common.maxRange.get())
                .setFromDiscreteValue(mbe.getRange())
                .snap(true);

        rangeSlider.message(s -> Component.literal(String.format("%d", 1+2*(int)rangeSlider.discreteValue())));
        rangeSlider.onChanged().subscribe(val -> {
            mbe.setRange((int) val);
            scheduleUpdate();
        });

        int minVolume = (int) Math.floor(linearToLog(Config.Common.minVolume.get()) * 100);

        volumeSlider = Components.discreteSlider(Sizing.fixed(100), minVolume, 100)
                .snap(true);

        volumeSlider.value(linearToLog(mbe.getVolume()));
        volumeSlider.message(s -> Component.literal(s + "%"));
        volumeSlider.onChanged().subscribe(val -> {
            mbe.setVolume(logToLinear(volumeSlider.value()));
            scheduleUpdate();
        });

        rootComponent.child(
            Containers.horizontalFlow(Sizing.content(), Sizing.content())
                .child(Containers.verticalFlow(Sizing.content(), Sizing.content())
                    .child(Components.label(title))
                    .child(Components.label(Component.literal(" ")))
                    .child(Containers.grid(Sizing.content(), Sizing.content(), 3, 2)
                        .child(Components.label(Component.translatable("sculkmuffler.gui.range").append(" ")), 0, 0)
                        .child(rangeSlider, 0, 1)
                        .child(Components.label(Component.literal(" ")), 1, 0)
                        .child(Components.label(Component.translatable("sculkmuffler.gui.volume").append(" ")), 2, 0)
                        .child(volumeSlider, 2, 1)
                        .padding(Insets.of(0))
                        .verticalAlignment(VerticalAlignment.CENTER)
                        .horizontalAlignment(HorizontalAlignment.CENTER)
                    )
                    .padding(Insets.of(10))
                    .surface(Surface.DARK_PANEL)
                    .verticalAlignment(VerticalAlignment.CENTER)
                    .horizontalAlignment(HorizontalAlignment.LEFT)
                )
                .child(Components.label(Component.literal(" ")))
                .child(Components.button(Component.literal("R"), btn -> {
                    mbe.toggleDebug();
                    scheduleUpdate();
                }).tooltip(Component.translatable("sculkmuffler.gui.show-range")))
                .horizontalAlignment(HorizontalAlignment.CENTER)
                .verticalAlignment(VerticalAlignment.TOP)
        );

    }
}
