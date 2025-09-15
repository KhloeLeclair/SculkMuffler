package dev.khloeleclair.skulkmuffler.client.screens;

import dev.khloeleclair.skulkmuffler.client.gui.widgets.IconButton;
import dev.khloeleclair.skulkmuffler.common.blockentities.MufflerBlockEntity;
import dev.khloeleclair.skulkmuffler.common.network.CustomPackets;
import dev.khloeleclair.skulkmuffler.common.utilities.Constants;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.StringWidget;
import net.minecraft.client.gui.layouts.FrameLayout;
import net.minecraft.client.gui.layouts.GridLayout;
import net.minecraft.client.gui.layouts.SpacerElement;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.sounds.SoundSource;
import net.neoforged.neoforge.network.PacketDistributor;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class MufflerCategoryListScreen extends Screen {

    private final MufflerBlockEntity mbe;
    private int waitingTicks = 0;

    private final List<SoundSource> all_categories;

    private final StringWidget[] labels;
    private final IconButton[] selectButtons;

    @Nullable
    private IconButton btnPrevious;
    private IconButton btnNext;

    @Nullable
    private IconButton btnMode;

    private int page;
    private int pages;
    private int perPage;

    private int labelWidth;
    private int spacerHeight;

    public MufflerCategoryListScreen(MufflerBlockEntity mbe) {
        super(mbe.getBlockState().getBlock().getName());
        all_categories = new ArrayList<>();
        this.mbe = mbe;

        // Defaults
        page = -1;
        perPage = 10;

        // Create our widget storage with 10 slots each.
        labels = new StringWidget[10];
        selectButtons = new IconButton[10];

        refreshCategories();
    }

    private void refreshCategories() {
        all_categories.clear();

        all_categories.addAll(Arrays.asList(SoundSource.values()));

        all_categories.sort((a, b) -> {
            String aN = Component.translatable("soundCategory." + a.getName()).getString();
            String bN = Component.translatable("soundCategory." + b.getName()).getString();

            return aN.compareToIgnoreCase(bN);
        });

        pages = Math.max(1, Math.ceilDiv(all_categories.size(), perPage));

    }

    @Override
    protected void init() {
        page = 0;
        resizeElements();
        layoutPage();
    }

    @Override
    public void resize(Minecraft minecraft, int width, int height) {
        super.resize(minecraft, width, height);
        resizeElements();
        layoutPage();
    }

    private void resizeElements() {
        // If the screen is smaller than 640x360, we need to compact things.
        perPage = 10;
        spacerHeight = 10;
        labelWidth = 400;

        if (width < 640)
            labelWidth = Math.max(100, width - 172);

        if (height < 360) {
            spacerHeight = 5;
            perPage = Math.clamp((height - 64) / 22, 2, 10);
        }

        pages = Math.max(1, Math.ceilDiv(all_categories.size(), perPage));
    }

    private void changePage(int p) {
        if (p < 0)
            p = pages - 1;
        if (p >= pages)
            p = 0;

        if (page == p)
            return;

        page = p;
        layoutPage();
    }

    private Component getLabel(SoundSource category, boolean selected) {
        boolean allowlist = mbe.isTargetCategoryAllowlist();

        var cmp = Component.translatable("soundCategory." + category.getName());
        cmp.setStyle(selected ? (allowlist ? Constants.GREEN : Constants.RED) : Style.EMPTY.withColor(ChatFormatting.WHITE));

        return cmp;
    }

    private Component getMutedLabel(boolean muted) {
        if (!mbe.isTargetCategoryAllowlist())
            muted = ! muted;

        return muted ? Component.translatable("sculkmuffler.gui.mute") : Component.translatable("sculkmuffler.gui.unmute");
    }

    private static Component getModeLabel(boolean is_allowlist) {
        return Component.translatable(is_allowlist ? "sculkmuffler.gui.target-mode.allow" : "sculkmuffler.gui.target-mode.deny");
    }

    private IconButton getSelectButton(int id) {
        return id >= 0 && id < selectButtons.length ? selectButtons[id] : null;
    }

    private StringWidget getLabel(int id) {
        return id >= 0 && id < labels.length ? labels[id] : null;
    }

    private void selectCategory(int i, SoundSource category) {
        if (mbe.hasTargetCategory(category)) {
            if (! mbe.removeTargetCategory(category))
                return;
        } else {
            if (!mbe.addTargetCategory(category))
                return;
        }

        //lblWarning.visible = !mbe.isTargetCategoryAllowlist() && !mbe.hasTargetCategories();

        var selected = mbe.hasTargetCategory(category);
        PacketDistributor.sendToServer(new CustomPackets.ModifyMuffledCategoryList(mbe.getBlockPos(), selected, category.name()));

        var btn = getSelectButton(i);
        var lbl = getLabel(i);
        if (btn != null)
            btn.setMessage(getMutedLabel(selected));
        if (lbl != null)
            lbl.setMessage(getLabel(category, selected));
    }

    private void layoutPage() {
        final var font = Minecraft.getInstance().font;
        final var current_focus = getCurrentFocusPath();

        clearWidgets();

        GridLayout layout = new GridLayout();
        layout.defaultCellSetting().padding(2, 2, 2, 0).alignVerticallyMiddle();
        var helper = layout.createRowHelper(4);

        helper.addChild(new StringWidget(title, font), 1);

        helper.addChild(IconButton.builder(Component.translatable("sculkmuffler.gui.back"), m -> onClose()).width(50).build());

        if (btnMode == null)
            btnMode = IconButton.builder(getModeLabel(mbe.isTargetCategoryAllowlist()), m -> {
                mbe.setTargetCategoryAllowlist(! mbe.isTargetCategoryAllowlist());
                btnMode.setMessage(getModeLabel(mbe.isTargetCategoryAllowlist()));

                // Update all the labels of all the mute buttons.
                for(int i = 0; i < 10; i++) {
                    int idx = (page * perPage) + i;
                    if (idx >= all_categories.size())
                        break;

                    final var source = all_categories.get(idx);
                    final var selected = mbe.hasTargetCategory(source);
                    var lbl = labels[i];
                    if (lbl != null)
                        lbl.setMessage(getLabel(source, selected));
                    var btn = selectButtons[i];
                    if (btn != null)
                        btn.setMessage(getMutedLabel(selected));
                }

                scheduleUpdate();
            }).width(104).tooltip(Component.translatable("sculkmuffler.gui.target-mode.about")).build();

        helper.addChild(btnMode, 2);

        helper.addChild(new StringWidget(Component.translatable("sculkmuffler.gui.page", page+1, pages), font), 1);
        helper.addChild(new SpacerElement(50, 20));

        if (btnPrevious == null)
            btnPrevious = IconButton.builder(Component.translatable("sculkmuffler.gui.page.previous"), m -> {
                changePage(page - 1);
            }).width(50).build();

        helper.addChild(btnPrevious);
        btnPrevious.active = page > 0;

        if (btnNext == null)
            btnNext = IconButton.builder(Component.translatable("sculkmuffler.gui.page.next"), m -> {
                changePage(page + 1);
            }).width(50).build();

        helper.addChild(btnNext);
        btnNext.active = page < pages - 1;

        // Spacer Line / Warning
        helper.addChild(new SpacerElement(50, spacerHeight), 4);

        // Build the widgets
        for(int i = 0; i < 10; i++) {
            if (i >= perPage) {
                labels[i] = null;
                selectButtons[i] = null;
                continue;
            }

            int idx = (page * perPage) + i;
            if (idx >= all_categories.size()) {
                if (idx == 0) {
                    var widget = new StringWidget(Component.translatable("sculkmuffler.gui.no-matches"), font);
                    //widget.setWidth(500);
                    widget.alignLeft();
                    helper.addChild(widget);
                    helper.addChild(new SpacerElement(50, 20), 3);
                } else
                    helper.addChild(new SpacerElement(50, 20), 4);

                labels[i] = null;
                selectButtons[i] = null;
                continue;
            }

            final var category = all_categories.get(idx);
            final var selected = mbe.hasTargetCategory(category);

            var label = new StringWidget(getLabel(category, selected), font);
            label.setWidth(labelWidth);
            label.alignLeft();
            labels[i] = label;

            helper.addChild(label);
            helper.addChild(new SpacerElement(50, 20));
            helper.addChild(new SpacerElement(50, 20));

            final int finalI = i;

            var selectButton = IconButton.builder(getMutedLabel(selected), e -> selectCategory(finalI, category))
                    .width(50)
                    .build();
            selectButtons[i] = selectButton;
            helper.addChild(selectButton);
        }

        helper.addChild(new SpacerElement(50, spacerHeight), 4);

        layout.arrangeElements();
        FrameLayout.alignInRectangle(layout, 0, 0, this.width, this.height, 0.5f, 0.5f);
        layout.visitWidgets(this::addRenderableWidget);

        if (current_focus != null)
            changeFocus(current_focus);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public void onClose() {
        if (waitingTicks > 0)
            sendUpdate();

        Minecraft.getInstance().setScreen(new MufflerScreen(mbe));
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
        waitingTicks = -1;
        mbe.sendUpdatePacket();
    }

}
