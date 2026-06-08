package com.elfmcys.yesstevemodel.client.gui;

import com.elfmcys.yesstevemodel.YesSteveModel;
import com.elfmcys.yesstevemodel.client.gui.button.FlatColorButton;
import com.elfmcys.yesstevemodel.client.gui.resource.ModelRepoClient;
import com.elfmcys.yesstevemodel.client.gui.resource.ModelRepoEntry;
import com.elfmcys.yesstevemodel.client.gui.resource.ResourceDownloadManager;
import com.elfmcys.yesstevemodel.client.gui.resource.ResourceStationConfig;
import com.elfmcys.yesstevemodel.client.texture.OuterFileTexture;
import com.elfmcys.yesstevemodel.client.upload.ModelUploadSession;
import com.elfmcys.yesstevemodel.mixin.client.ScreenAccessor;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.HexFormat;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class ResourceStationScreen extends Screen {
    private static final int PANEL_WIDTH = 460;
    private static final int PANEL_HEIGHT = 250;
    private static final int ENTRY_HEIGHT = 36;
    private static final int PREVIEW_SIZE = 28;
    private static final ExecutorService RESOURCE_EXECUTOR = Executors.newCachedThreadPool(runnable -> {
        Thread thread = new Thread(runnable, "YSM Resource Station");
        thread.setDaemon(true);
        return thread;
    });

    private final PlayerModelScreen parentScreen;
    private final List<ModelRepoEntry> entries = new ArrayList<>();
    private final Map<String, ResourceLocation> previewTextures = new HashMap<>();
    private final Set<String> loadingPreviews = new HashSet<>();
    private ResourceStationConfig.State config;
    private EditBox urlBox;
    private EditBox searchBox;
    private int guiLeft;
    private int guiTop;
    private int page;
    private int entriesPerPage;
    private int listRequestId;
    private int screenGeneration;
    private boolean closed;
    private boolean listLoading;
    private SortMode sortMode = SortMode.NAME;
    private Component status = Component.empty();
    private ChatFormatting statusColor = ChatFormatting.GRAY;

    public ResourceStationScreen(PlayerModelScreen parentScreen) {
        super(Component.translatable("gui.yes_steve_model.resource_station.title"));
        this.parentScreen = parentScreen;
        this.config = ResourceStationConfig.load();
    }

    @Override
    protected void init() {
        clearWidgets();
        this.closed = false;
        this.guiLeft = (this.width - PANEL_WIDTH) / 2;
        this.guiTop = (this.height - PANEL_HEIGHT) / 2;
        this.entriesPerPage = Math.max(1, (PANEL_HEIGHT - 106) / ENTRY_HEIGHT);

        String oldUrl = this.urlBox == null ? this.config.selectedUrl() : this.urlBox.getValue();
        String oldSearch = this.searchBox == null ? "" : this.searchBox.getValue();
        boolean urlFocused = this.urlBox != null && this.urlBox.isFocused();
        boolean searchFocused = this.searchBox != null && this.searchBox.isFocused();

        this.urlBox = new EditBox(this.font, this.guiLeft + 46, this.guiTop + 9, 184, 16, Component.translatable("gui.yes_steve_model.resource_station.url"));
        this.urlBox.setMaxLength(2048);
        this.urlBox.setValue(oldUrl);
        this.urlBox.setTextColor(0xFFF3F3E0);
        this.urlBox.setFocused(urlFocused);
        addWidget(this.urlBox);

        this.searchBox = new EditBox(this.font, this.guiLeft + 46, this.guiTop + 32, 134, 16, Component.translatable("gui.yes_steve_model.resource_station.search"));
        this.searchBox.setMaxLength(256);
        this.searchBox.setValue(oldSearch);
        this.searchBox.setTextColor(0xFFF3F3E0);
        this.searchBox.setFocused(searchFocused);
        addWidget(this.searchBox);
        if (searchFocused) {
            setFocused(this.searchBox);
        } else if (urlFocused) {
            setFocused(this.urlBox);
        }

        FlatColorButton previousSiteButton = new FlatColorButton(this.guiLeft + 234, this.guiTop + 8, 18, 18, Component.literal("<"), button -> switchSite(-1))
                .setTooltipText("gui.yes_steve_model.resource_station.previous_site");
        previousSiteButton.active = this.config.urls().size() > 1;
        addRenderableWidget(previousSiteButton);
        FlatColorButton nextSiteButton = new FlatColorButton(this.guiLeft + 256, this.guiTop + 8, 18, 18, Component.literal(">"), button -> switchSite(1))
                .setTooltipText("gui.yes_steve_model.resource_station.next_site");
        nextSiteButton.active = this.config.urls().size() > 1;
        addRenderableWidget(nextSiteButton);
        addRenderableWidget(new FlatColorButton(this.guiLeft + 278, this.guiTop + 8, 48, 18, Component.translatable("gui.yes_steve_model.resource_station.refresh"), button -> refresh()));
        addRenderableWidget(new FlatColorButton(this.guiLeft + 330, this.guiTop + 8, 40, 18, Component.translatable("gui.yes_steve_model.resource_station.save"), button -> saveUrl()));
        addRenderableWidget(new FlatColorButton(this.guiLeft + 374, this.guiTop + 8, 54, 18, Component.translatable("gui.yes_steve_model.resource_station.queue_all"), button -> enqueueVisible()));
        addRenderableWidget(new FlatColorButton(this.guiLeft + 184, this.guiTop + 31, 58, 18, Component.translatable("gui.yes_steve_model.resource_station.sort"), button -> cycleSort())
                .setTooltipLines(List.of(Component.translatable("gui.yes_steve_model.resource_station.sort_mode", this.sortMode.label()))));
        addRenderableWidget(new FlatColorButton(this.guiLeft + 246, this.guiTop + 31, 62, 18, modeLabel(), button -> toggleMode())
                .setTooltipText("gui.yes_steve_model.resource_station.mode.tooltip"));
        addRenderableWidget(new FlatColorButton(this.guiLeft + 10, this.guiTop + PANEL_HEIGHT - 22, 58, 16, Component.translatable("gui.yes_steve_model.model.return"), button -> Minecraft.getInstance().setScreen(this.parentScreen)));
        addRenderableWidget(new FlatColorButton(this.guiLeft + 74, this.guiTop + PANEL_HEIGHT - 22, 74, 16, Component.translatable("gui.yes_steve_model.resource_station.download_page"), button -> Minecraft.getInstance().setScreen(new DownloadScreen(this.parentScreen, this))));
        addRenderableWidget(new FlatColorButton(this.guiLeft + 190, this.guiTop + PANEL_HEIGHT - 22, 52, 16, Component.translatable("gui.yes_steve_model.pre_page"), button -> {
            if (this.page > 0) {
                this.page--;
                init();
            }
        }));
        addRenderableWidget(new FlatColorButton(this.guiLeft + 258, this.guiTop + PANEL_HEIGHT - 22, 52, 16, Component.translatable("gui.yes_steve_model.next_page"), button -> {
            if (this.page < maxPage(filteredEntries().size())) {
                this.page++;
                init();
            }
        }));

        List<ModelRepoEntry> visible = filteredEntries();
        clampPage(visible.size());
        int start = this.page * this.entriesPerPage;
        for (int i = 0; i < this.entriesPerPage; i++) {
            int index = start + i;
            if (index >= visible.size()) {
                break;
            }
            ModelRepoEntry entry = visible.get(index);
            int y = entryY(i);
            FlatColorButton download = new FlatColorButton(this.guiLeft + PANEL_WIDTH - 72, y + 8, 58, 18,
                    ResourceDownloadManager.isQueued(entry)
                            ? Component.translatable("gui.yes_steve_model.resource_station.queued_short")
                            : Component.translatable("gui.yes_steve_model.resource_station.download"),
                    button -> enqueue(entry));
            download.active = !ResourceDownloadManager.isQueued(entry);
            addRenderableWidget(download);
            ensurePreview(entry);
        }

        if (this.entries.isEmpty() && !this.listLoading && this.status.getString().isEmpty()) {
            this.status = Component.translatable("gui.yes_steve_model.resource_station.empty_hint");
            this.statusColor = ChatFormatting.GRAY;
        }
    }

    @Override
    public void tick() {
        super.tick();
        ResourceDownloadManager.tick();
    }

    @Override
    public void removed() {
        this.closed = true;
        this.screenGeneration++;
        this.listRequestId++;
        this.listLoading = false;
        this.loadingPreviews.clear();
        if (this.entries.isEmpty()) {
            this.status = Component.empty();
        }
        super.removed();
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        renderTransparentBackground(guiGraphics);
        guiGraphics.fill(this.guiLeft, this.guiTop, this.guiLeft + PANEL_WIDTH, this.guiTop + PANEL_HEIGHT, 0xE0202020);
        guiGraphics.fill(this.guiLeft, this.guiTop, this.guiLeft + PANEL_WIDTH, this.guiTop + 2, 0xFFB15D2B);
        guiGraphics.drawString(this.font, this.title, this.guiLeft + 10, this.guiTop - 14, 0xFFE9E0D0, false);
        guiGraphics.drawString(this.font, Component.translatable("gui.yes_steve_model.resource_station.url"), this.guiLeft + 10, this.guiTop + 12, 0xFFAFAFAF, false);
        guiGraphics.drawString(this.font, Component.translatable("gui.yes_steve_model.resource_station.search"), this.guiLeft + 10, this.guiTop + 35, 0xFFAFAFAF, false);
        this.urlBox.render(guiGraphics, mouseX, mouseY, partialTick);
        this.searchBox.render(guiGraphics, mouseX, mouseY, partialTick);

        renderEntries(guiGraphics);
        renderFooter(guiGraphics);
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        renderButtonTooltips(guiGraphics, mouseX, mouseY);
    }

    private void renderEntries(GuiGraphics guiGraphics) {
        int listTop = this.guiTop + 58;
        int listBottom = this.guiTop + PANEL_HEIGHT - 30;
        guiGraphics.fill(this.guiLeft + 8, listTop - 2, this.guiLeft + PANEL_WIDTH - 8, listBottom, 0x66000000);
        List<ModelRepoEntry> visible = filteredEntries();
        if (this.listLoading) {
            guiGraphics.drawCenteredString(this.font, Component.translatable("gui.yes_steve_model.resource_station.loading"), this.guiLeft + PANEL_WIDTH / 2, listTop + 42, 0xFFE8D9B8);
            return;
        }
        if (visible.isEmpty()) {
            guiGraphics.drawCenteredString(this.font, Component.translatable("gui.yes_steve_model.resource_station.no_results"), this.guiLeft + PANEL_WIDTH / 2, listTop + 42, 0xFF8F8F8F);
            return;
        }
        int start = this.page * this.entriesPerPage;
        for (int i = 0; i < this.entriesPerPage; i++) {
            int index = start + i;
            if (index >= visible.size()) {
                break;
            }
            ModelRepoEntry entry = visible.get(index);
            int y = entryY(i);
            int bg = (i & 1) == 0 ? 0x77313131 : 0x77262626;
            guiGraphics.fill(this.guiLeft + 10, y, this.guiLeft + PANEL_WIDTH - 10, y + ENTRY_HEIGHT - 2, bg);
            ResourceLocation preview = this.previewTextures.get(entry.url());
            if (preview != null) {
                guiGraphics.blit(preview, this.guiLeft + 14, y + 4, 0.0f, 0.0f, PREVIEW_SIZE, PREVIEW_SIZE, PREVIEW_SIZE, PREVIEW_SIZE);
            } else {
                guiGraphics.fill(this.guiLeft + 14, y + 4, this.guiLeft + 14 + PREVIEW_SIZE, y + 4 + PREVIEW_SIZE, 0xAA101010);
            }
            int textX = this.guiLeft + 48;
            guiGraphics.drawString(this.font, trim(entry.name(), 210), textX, y + 4, 0xFFEDE1CC, false);
            String detail = detailLine(entry);
            guiGraphics.drawString(this.font, trim(detail, 248), textX, y + 15, 0xFF9FA8A6, false);
            if (!entry.description().isBlank()) {
                guiGraphics.drawString(this.font, trim(entry.description(), 248), textX, y + 26, 0xFF8C8C8C, false);
            }
        }
    }

    private void renderFooter(GuiGraphics guiGraphics) {
        ResourceDownloadManager.Snapshot snapshot = ResourceDownloadManager.snapshot();
        Component message = snapshot.status().getString().isBlank() ? this.status : snapshot.status();
        ChatFormatting color = snapshot.status().getString().isBlank() ? this.statusColor : snapshot.statusColor();
        int colorValue = color.getColor() == null ? 0xFFBDBDBD : color.getColor();
        guiGraphics.drawString(this.font, message, this.guiLeft + 314, this.guiTop + PANEL_HEIGHT - 18, colorValue, false);
        int total = filteredEntries().size();
        String pageText = (Math.min(this.page, maxPage(total)) + 1) + "/" + (maxPage(total) + 1) + " (" + total + ")";
        guiGraphics.drawString(this.font, pageText, this.guiLeft + PANEL_WIDTH - 62, this.guiTop + 35, 0xFF9A9A9A, false);
    }

    private void refresh() {
        saveUrl();
        int requestId = ++this.listRequestId;
        int generation = this.screenGeneration;
        ResourceStationConfig.State requestConfig = this.config;
        this.listLoading = true;
        this.status = Component.translatable("gui.yes_steve_model.resource_station.loading");
        this.statusColor = ChatFormatting.YELLOW;
        this.entries.clear();
        this.page = 0;
        CompletableFuture.supplyAsync(() -> {
            try {
                return ModelRepoClient.list(requestConfig.selectedUrl(), requestConfig);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }, RESOURCE_EXECUTOR).orTimeout(Math.max(15_000L, requestConfig.timeoutMs() * 3L), TimeUnit.MILLISECONDS).whenComplete((result, error) ->
                ((Executor) Minecraft.getInstance()).execute(() -> {
                    if (this.closed || generation != this.screenGeneration || requestId != this.listRequestId) {
                        return;
                    }
                    this.listLoading = false;
                    if (error != null) {
                        this.status = Component.translatable("gui.yes_steve_model.resource_station.error", rootMessage(error));
                        this.statusColor = ChatFormatting.RED;
                    } else {
                        this.entries.clear();
                        this.entries.addAll(result);
                        sortEntries();
                        this.status = Component.translatable("gui.yes_steve_model.resource_station.loaded", result.size());
                        this.statusColor = ChatFormatting.GREEN;
                    }
                    init();
                }));
    }

    private void saveUrl() {
        String selected = this.urlBox == null ? this.config.selectedUrl() : this.urlBox.getValue().trim();
        if (selected.isBlank()) {
            return;
        }
        List<String> urls = new ArrayList<>(this.config.urls());
        if (!urls.contains(selected)) {
            urls.add(0, selected);
        }
        this.config = new ResourceStationConfig.State(urls, selected, this.config.timeoutMs(), this.config.maxDownloadBytes(), this.config.preferGithubAccelerator(), this.config.githubAccelerators());
        ResourceStationConfig.save(this.config);
        this.status = Component.translatable("gui.yes_steve_model.resource_station.saved");
        this.statusColor = ChatFormatting.GRAY;
    }

    private void switchSite(int offset) {
        saveUrl();
        List<String> urls = this.config.urls();
        if (urls.size() <= 1) {
            return;
        }
        int currentIndex = urls.indexOf(this.config.selectedUrl());
        if (currentIndex < 0) {
            currentIndex = 0;
        }
        int nextIndex = Math.floorMod(currentIndex + offset, urls.size());
        String selected = urls.get(nextIndex);
        this.config = new ResourceStationConfig.State(urls, selected, this.config.timeoutMs(), this.config.maxDownloadBytes(), this.config.preferGithubAccelerator(), this.config.githubAccelerators());
        ResourceStationConfig.save(this.config);
        if (this.urlBox != null) {
            this.urlBox.setValue(selected);
        }
        this.entries.clear();
        this.previewTextures.clear();
        this.loadingPreviews.clear();
        this.page = 0;
        this.status = Component.translatable("gui.yes_steve_model.resource_station.site_selected", nextIndex + 1, urls.size());
        this.statusColor = ChatFormatting.GRAY;
        init();
    }

    private void cycleSort() {
        this.sortMode = this.sortMode.next();
        sortEntries();
        init();
    }

    private void toggleMode() {
        this.config = new ResourceStationConfig.State(this.config.urls(), this.config.selectedUrl(), this.config.timeoutMs(), this.config.maxDownloadBytes(),
                !this.config.preferGithubAccelerator(), this.config.githubAccelerators());
        ResourceStationConfig.save(this.config);
        init();
    }

    private Component modeLabel() {
        return this.config.preferGithubAccelerator()
                ? Component.translatable("gui.yes_steve_model.resource_station.mode.mainland")
                : Component.translatable("gui.yes_steve_model.resource_station.mode.native");
    }

    private void enqueue(ModelRepoEntry entry) {
        if (ResourceDownloadManager.enqueue(entry, this.config)) {
            this.status = Component.translatable("gui.yes_steve_model.resource_station.queued", entry.name());
            this.statusColor = ChatFormatting.YELLOW;
        }
        init();
    }

    private void enqueueVisible() {
        int added = ResourceDownloadManager.enqueueAll(filteredEntries(), this.config);
        this.status = Component.translatable("gui.yes_steve_model.resource_station.queue_added", added);
        this.statusColor = added > 0 ? ChatFormatting.YELLOW : ChatFormatting.GRAY;
        init();
    }

    private List<ModelRepoEntry> filteredEntries() {
        String query = this.searchBox == null ? "" : this.searchBox.getValue().trim().toLowerCase(Locale.ROOT);
        if (query.isBlank()) {
            return new ArrayList<>(this.entries);
        }
        List<ModelRepoEntry> result = new ArrayList<>();
        for (ModelRepoEntry entry : this.entries) {
            if (entry.name().toLowerCase(Locale.ROOT).contains(query)
                    || entry.fileName().toLowerCase(Locale.ROOT).contains(query)
                    || entry.description().toLowerCase(Locale.ROOT).contains(query)
                    || entry.author().toLowerCase(Locale.ROOT).contains(query)
                    || entry.tags().toLowerCase(Locale.ROOT).contains(query)) {
                result.add(entry);
            }
        }
        return result;
    }

    private void sortEntries() {
        Comparator<ModelRepoEntry> comparator = switch (this.sortMode) {
            case SIZE -> Comparator.comparingLong(entry -> entry.size() < 0 ? Long.MAX_VALUE : entry.size());
            case AUTHOR -> Comparator.comparing(entry -> entry.author().toLowerCase(Locale.ROOT));
            case NAME -> Comparator.comparing(entry -> entry.name().toLowerCase(Locale.ROOT));
        };
        this.entries.sort(comparator.thenComparing(ModelRepoEntry::fileName));
    }

    private void ensurePreview(ModelRepoEntry entry) {
        if (entry.previewUrl().isBlank() || this.previewTextures.containsKey(entry.url()) || !this.loadingPreviews.add(entry.url())) {
            return;
        }
        ResourceStationConfig.State requestConfig = this.config;
        int generation = this.screenGeneration;
        CompletableFuture.supplyAsync(() -> {
            try {
                return ModelRepoClient.downloadPreview(entry, requestConfig);
            } catch (Exception e) {
                return null;
            }
        }, RESOURCE_EXECUTOR).orTimeout(Math.max(5_000L, requestConfig.timeoutMs()), TimeUnit.MILLISECONDS).whenComplete((data, error) ->
                ((Executor) Minecraft.getInstance()).execute(() -> {
                    if (this.closed || generation != this.screenGeneration) {
                        return;
                    }
                    this.loadingPreviews.remove(entry.url());
                    if (data == null || error != null) {
                        return;
                    }
                    ResourceLocation id = ResourceLocation.fromNamespaceAndPath(YesSteveModel.MOD_ID, "resource_preview/" + shortHash(entry.url()));
                    Minecraft.getInstance().getTextureManager().register(id, new OuterFileTexture(data));
                    this.previewTextures.put(entry.url(), id);
                }));
    }

    private int entryY(int index) {
        return this.guiTop + 58 + index * ENTRY_HEIGHT;
    }

    private int maxPage(int total) {
        if (total <= 0) {
            return 0;
        }
        return Math.max(0, (total - 1) / this.entriesPerPage);
    }

    private void clampPage(int total) {
        this.page = Math.max(0, Math.min(this.page, maxPage(total)));
    }

    private String detailLine(ModelRepoEntry entry) {
        List<String> parts = new ArrayList<>();
        parts.add(entry.fileName());
        if (entry.size() > 0) {
            parts.add(ModelUploadSession.formatBytes((int) Math.min(Integer.MAX_VALUE, entry.size())));
        }
        if (!entry.author().isBlank()) {
            parts.add(entry.author());
        }
        if (!entry.tags().isBlank()) {
            parts.add(entry.tags());
        }
        return String.join("  |  ", parts);
    }

    private String trim(String value, int maxWidth) {
        if (this.font.width(value) <= maxWidth) {
            return value;
        }
        String ellipsis = "...";
        int keep = value.length();
        while (keep > 0 && this.font.width(value.substring(0, keep) + ellipsis) > maxWidth) {
            keep--;
        }
        return value.substring(0, Math.max(0, keep)) + ellipsis;
    }

    private void renderButtonTooltips(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        for (var renderable : ((ScreenAccessor) this).ysm$getRenderables()) {
            if (renderable instanceof FlatColorButton button) {
                button.renderTooltip(guiGraphics, this, mouseX, mouseY);
            }
        }
    }

    private static String rootMessage(Throwable throwable) {
        Throwable current = throwable;
        while (current.getCause() != null) {
            current = current.getCause();
        }
        return current.getMessage() == null ? current.getClass().getSimpleName() : current.getMessage();
    }

    private static String shortHash(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-1");
            return HexFormat.of().formatHex(digest.digest(value.getBytes(java.nio.charset.StandardCharsets.UTF_8))).substring(0, 16);
        } catch (Exception e) {
            return Integer.toHexString(value.hashCode());
        }
    }

    private enum SortMode {
        NAME("name"),
        SIZE("size"),
        AUTHOR("author");

        private final String key;

        SortMode(String key) {
            this.key = key;
        }

        private Component label() {
            return Component.translatable("gui.yes_steve_model.resource_station.sort." + this.key);
        }

        private SortMode next() {
            SortMode[] values = values();
            return values[(ordinal() + 1) % values.length];
        }
    }
}
