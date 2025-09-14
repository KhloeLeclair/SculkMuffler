package dev.khloeleclair.skulkmuffler.client.screens;

import dev.khloeleclair.skulkmuffler.client.gui.widgets.IconButton;
import dev.khloeleclair.skulkmuffler.common.blockentities.MufflerBlockEntity;
import dev.khloeleclair.skulkmuffler.common.network.CustomPackets;
import dev.khloeleclair.skulkmuffler.common.utilities.Constants;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.StringWidget;
import net.minecraft.client.gui.layouts.FrameLayout;
import net.minecraft.client.gui.layouts.GridLayout;
import net.minecraft.client.gui.layouts.SpacerElement;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.client.sounds.SoundManager;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.PacketDistributor;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public class MufflerSoundListScreen extends Screen {

    enum ListingMode {
        ALL,
        NEARBY,
        SELECTED
    }

    private static final ResourceLocation PLAY_NORMAL = ResourceLocation.withDefaultNamespace("transferable_list/select");
    private static final ResourceLocation PLAY_SELECTED = ResourceLocation.withDefaultNamespace("transferable_list/select_highlighted");
    private static final ResourceLocation STOP = ResourceLocation.withDefaultNamespace("pending_invite/reject");

    private final SoundManager soundManager;

    private final MufflerBlockEntity mbe;
    private int waitingTicks = 0;

    private final List<ResourceLocation> all_sounds;
    private final List<ResourceLocation> filtered_sounds;

    private final IconButton[] playButtons;
    private final StringWidget[] labels;
    private final IconButton[] selectButtons;

    private ListingMode viewMode;

    //@Nullable
    //private StringWidget lblWarning;

    @Nullable
    private IconButton btnPrevious;
    private IconButton btnNext;

    @Nullable
    private IconButton btnMode;

    @Nullable
    private IconButton btnAll;
    @Nullable
    private IconButton btnNearby;
    @Nullable
    private IconButton btnSelected;

    @Nullable
    private EditBox txtSearch;
    private String query;

    private int page;
    private int pages;

    @Nullable
    private SoundInstance currentlyPlayingSound;
    private int currentlyPlaying;

    public MufflerSoundListScreen(MufflerBlockEntity mbe) {
        super(mbe.getBlockState().getBlock().getName());
        filtered_sounds = new ArrayList<>();
        all_sounds = new ArrayList<>();
        this.mbe = mbe;

        soundManager = Minecraft.getInstance().getSoundManager();

        // Defaults
        currentlyPlaying = -1;
        page = -1;
        viewMode = ListingMode.NEARBY;
        query = null;

        // Create our widget storage with 10 slots each.
        playButtons = new IconButton[10];
        labels = new StringWidget[10];
        selectButtons = new IconButton[10];

        refreshSounds();
    }

    private void refreshSounds() {
        all_sounds.clear();

        final var heard = mbe.getClientHeard();

        switch (viewMode) {
            case NEARBY:
                if (heard != null)
                    all_sounds.addAll(heard);
                break;
            case ALL:
                all_sounds.addAll(BuiltInRegistries.SOUND_EVENT.keySet());
                break;
            case SELECTED:
                final var targets = mbe.getTargets();
                if (targets != null)
                    all_sounds.addAll(targets);
        }

        all_sounds.sort((a, b) -> {
            if (heard != null) {
                if (heard.contains(a) && !heard.contains(b))
                    return -1;
                if (!heard.contains(a) && heard.contains(b))
                    return 1;
            }

            String aN = a.getNamespace();
            String bN = b.getNamespace();

            int result = aN.compareTo(bN);
            if (result == 0)
                result = a.getPath().compareTo(b.getPath());

            return result;
        });

        updateFiltered();
    }

    private void updateFiltered() {
        filtered_sounds.clear();

        if (query != null && ! query.isBlank()) {
            for (ResourceLocation location : all_sounds) {
                if (location.toString().contains(query))
                    filtered_sounds.add(location);
            }
        } else
            filtered_sounds.addAll(all_sounds);

        pages = Math.max(1, Math.ceilDiv(filtered_sounds.size(), 10));
    }

    @Override
    protected void init() {
        page = 0;
        layoutPage();
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

    private MutableComponent getLabel(ResourceLocation location, boolean selected) {
        boolean whitelist = mbe.isTargetAllowlist();

        var cmp = Component.literal(location.getNamespace()).append(String.valueOf(ResourceLocation.NAMESPACE_SEPARATOR));
        cmp.setStyle(selected ? (whitelist ? Constants.DARK_GREEN : Constants.DARK_RED) : Constants.GRAY);

        var cmp2 = Component.literal(location.getPath());
        cmp2.setStyle(selected ? (whitelist ? Constants.GREEN : Constants.RED) : Style.EMPTY.withColor(ChatFormatting.WHITE));

        cmp.append(cmp2);
        return cmp;
    }

    private Component getMutedLabel(boolean muted) {
        if (!mbe.isTargetAllowlist())
            muted = ! muted;

        return muted ? Component.translatable("sculkmuffler.gui.mute") : Component.translatable("sculkmuffler.gui.unmute");
    }

    private static Component getModeLabel(boolean target_whitelist) {
        return Component.translatable(target_whitelist ? "sculkmuffler.gui.target-mode.allow" : "sculkmuffler.gui.target-mode.deny");
    }

    private IconButton getSelectButton(int id) {
        return id >= 0 && id < selectButtons.length ? selectButtons[id] : null;
    }

    private StringWidget getLabel(int id) {
        return id >= 0 && id < labels.length ? labels[id] : null;
    }

    private IconButton getPlayButton(int id) {
        return id >= 0 && id < playButtons.length ? playButtons[id] : null;
    }

    public void soundIsStopped(SoundInstance instance) {
        if (currentlyPlayingSound == instance) {
            var btn = getPlayButton(currentlyPlaying);
            if (btn != null)
                btn.setSprite(PLAY_NORMAL, PLAY_SELECTED);

            currentlyPlaying = -1;
            currentlyPlayingSound = null;
        }
    }

    private void playSound(int i, ResourceLocation location) {
        boolean was_playing_this = currentlyPlaying == i;

        if (currentlyPlayingSound != null) {
            soundManager.stop(currentlyPlayingSound);
            soundIsStopped(currentlyPlayingSound);
            if (was_playing_this)
                return;
        }

        final var sound = BuiltInRegistries.SOUND_EVENT.get(location);
        if (sound == null)
            return;

        currentlyPlaying = i;
        currentlyPlayingSound = SimpleSoundInstance.forUI(sound, 1f);
        soundManager.play(currentlyPlayingSound);
        var btn = getPlayButton(i);
        if (btn != null)
            btn.setSprite(STOP, null);
    }

    private void selectSound(int i, ResourceLocation location) {
        if (mbe.hasTarget(location)) {
            if (! mbe.removeTargetSound(location))
                return;
        } else {
            if (!mbe.addTargetSound(location))
                return;
        }

        //lblWarning.visible = !mbe.isTargetWhitelist() && !mbe.hasTargets();

        var muted = mbe.hasTarget(location);
        PacketDistributor.sendToServer(new CustomPackets.ModifyMuffledList(mbe.getBlockPos(), muted, location));

        var btn = getSelectButton(i);
        var lbl = getLabel(i);
        if (btn != null)
            btn.setMessage(getMutedLabel(muted));
        if (lbl != null)
            lbl.setMessage(getLabel(location, muted));
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
            btnMode = IconButton.builder(getModeLabel(mbe.isTargetAllowlist()), m -> {
                mbe.setTargetAllowlist(! mbe.isTargetAllowlist());
                btnMode.setMessage(getModeLabel(mbe.isTargetAllowlist()));
                //lblWarning.visible = !mbe.isTargetWhitelist() && !mbe.hasTargets();

                // Update all the labels of all the mute buttons.
                for(int i = 0; i < 10; i++) {
                    int idx = (page * 10) + i;
                    if (idx >= filtered_sounds.size())
                        break;

                    final var location = filtered_sounds.get(idx);
                    final var muted = mbe.hasTarget(location);
                    var lbl = labels[i];
                    if (lbl != null)
                        lbl.setMessage(getLabel(location, muted));
                    var btn = selectButtons[i];
                    if (btn != null)
                        btn.setMessage(getMutedLabel(muted));
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
        /*if (lblWarning == null) {
            lblWarning = new StringWidget(Component.translatable("sculkmuffler.gui.target-mode.deny.notice").setStyle(Constants.YELLOW), font);
            lblWarning.alignRight();
        }

        lblWarning.visible = !mbe.isTargetWhitelist() && !mbe.hasTargets();

        helper.addChild(lblWarning);*/
        helper.addChild(new SpacerElement(50, 10), 4);

        // Build the widgets
        for(int i = 0; i < 10; i++) {
            int idx = (page * 10) + i;
            if (idx >= filtered_sounds.size()) {
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
                playButtons[i] = null;
                continue;
            }

            final var location = filtered_sounds.get(idx);
            final var muted = mbe.hasTarget(location);

            var label = new StringWidget(getLabel(location, muted), font);
            label.setWidth(400);
            label.alignLeft();
            labels[i] = label;

            helper.addChild(label);
            helper.addChild(new SpacerElement(50, 20));

            final int finalI = i;
            var playButton = IconButton.builder(PLAY_NORMAL, PLAY_SELECTED, m -> playSound(finalI, location))
                    .width(50)
                    .clickSound(false)
                    .tooltip(Component.translatable("sculkmuffler.gui.preview"))
                    .build();
            playButtons[i] = playButton;
            helper.addChild(playButton);

            var selectButton = IconButton.builder(getMutedLabel(muted), e -> selectSound(finalI, location))
                    .width(50)
                    .build();
            selectButtons[i] = selectButton;
            helper.addChild(selectButton);
        }

        helper.addChild(new SpacerElement(50, 10), 4);

        if (txtSearch == null) {
            txtSearch = new EditBox(font, 0, 0, 400, 20, Component.translatable("sculkmuffler.gui.search"));
            txtSearch.setHint(Component.translatable("sculkmuffler.gui.search"));
            txtSearch.setResponder(value -> {
                query = value;
                updateFiltered();
                page = -1;
                changePage(0);
            });
        }

        helper.addChild(txtSearch);

        if (btnNearby == null)
            btnNearby = IconButton.builder(Component.translatable("sculkmuffler.gui.view.nearby"), m -> changeViewMode(ListingMode.NEARBY))
                .tooltip(Component.translatable("sculkmuffler.gui.view.nearby.about"))
                .width(50).build();

        if (btnAll == null)
            btnAll = IconButton.builder(Component.translatable("sculkmuffler.gui.view.all"), m -> changeViewMode(ListingMode.ALL))
                .tooltip(Component.translatable("sculkmuffler.gui.view.all.about"))
                .width(50).build();

        if (btnSelected == null)
            btnSelected = IconButton.builder(Component.translatable("sculkmuffler.gui.view.selected"), m -> changeViewMode(ListingMode.SELECTED))
                .tooltip(Component.translatable("sculkmuffler.gui.view.selected.about"))
                .width(50).build();

        btnNearby.active = viewMode != ListingMode.NEARBY;
        btnAll.active = viewMode != ListingMode.ALL;
        btnSelected.active = viewMode != ListingMode.SELECTED;

        helper.addChild(btnNearby);
        helper.addChild(btnAll);
        helper.addChild(btnSelected);

        layout.arrangeElements();
        FrameLayout.alignInRectangle(layout, 0, 0, this.width, this.height, 0.5f, 0.5f);
        layout.visitWidgets(this::addRenderableWidget);

        if (current_focus != null)
            changeFocus(current_focus);
    }

    private void changeViewMode(ListingMode mode) {
        if (viewMode == mode)
            return;

        viewMode = mode;
        page = 0;
        refreshSounds();
        layoutPage();
    }


    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public void removed() {
        super.removed();

        if (currentlyPlayingSound != null) {
            soundManager.stop(currentlyPlayingSound);
            currentlyPlayingSound = null;
            currentlyPlaying = -1;
        }
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

        if (currentlyPlayingSound != null && ! soundManager.isActive(currentlyPlayingSound))
            soundIsStopped(currentlyPlayingSound);

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
        CustomPackets.UpdateMuffler.sendUpdate(mbe);
    }

}
