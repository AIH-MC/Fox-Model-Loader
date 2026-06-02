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
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.FormattedCharSequence;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.HexFormat;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class ResourceStationScreen extends Screen {
    private static final int OUTER_MARGIN = 8;
    private static final int MIN_PANEL_WIDTH = 320;
    private static final int MAX_PANEL_WIDTH = 620;
    private static final int MIN_PANEL_HEIGHT = 220;
    private static final int MAX_PANEL_HEIGHT = 420;
    private static final int HEADER_HEIGHT = 56;
    private static final int COMPACT_HEADER_HEIGHT = 78;
    private static final int FOOTER_HEIGHT = 45;
    private static final int ENTRY_HEIGHT = 32;
    private static final int ENTRY_BUTTON_AREA_WIDTH = 92;
    private static final ExecutorService RESOURCE_EXECUTOR = Executors.newCachedThreadPool(runnable -> {
        Thread thread = new Thread(runnable, "YSM Resource Station");
        thread.setDaemon(true);
        return thread;
    });

    private final PlayerModelScreen parentScreen;
    private ResourceStationConfig.State config;
    private final List<ModelRepoEntry> entries = new ArrayList<>();
    private final Map<String, Identifier> previewTextures = new HashMap<>();
    private final Set<String> loadingPreviews = new HashSet<>();
    private EditBox urlBox;
    private EditBox searchBox;
    private FlatColorButton nativeModeButton;
    private FlatColorButton mainlandModeButton;
    private final Object configSaveLock = new Object();
    private int guiLeft;
    private int guiTop;
    private int guiWidth;
    private int guiHeight;
    private int entriesPerPage = 1;
    private int page;
    private int sourceIndex;
    private boolean listLoading;
    private boolean active;
    private boolean queuedListRefresh;
    private int listRequestId;
    private volatile int configSaveRequestId;
    private volatile ListResult pendingListResult;
    private SortMode sortMode = SortMode.NAME;
    private Component status = Component.empty();
    private ChatFormatting statusColor = ChatFormatting.GRAY;

    public ResourceStationScreen(PlayerModelScreen parentScreen) {
        super(Component.translatable("gui.yes_steve_model.resource_station.title"));
        this.parentScreen = parentScreen;
        this.config = ResourceStationConfig.load();
        this.sourceIndex = Math.max(0, this.config.urls().indexOf(this.config.selectedUrl()));
    }

    @Override
    public void init() {
        this.active = true;
        clearWidgets();
        updateLayout();
        String urlValue = this.urlBox == null ? this.config.selectedUrl() : this.urlBox.getValue();
        String searchValue = this.searchBox == null ? "" : this.searchBox.getValue();
        boolean urlFocused = this.urlBox != null && this.urlBox.isFocused();
        boolean searchFocused = this.searchBox != null && this.searchBox.isFocused();

        int labelWidth = Math.min(52, Math.max(42, this.guiWidth / 8));
        int topButtonY = this.guiTop + 7;
        int navRight = this.guiLeft + this.guiWidth - 10;
        int nextX = navRight - 18;
        int prevX = nextX - 22;
        int deleteX = prevX - 38;
        int saveX = deleteX - 38;
        int refreshX = saveX - 46;
        int urlX = this.guiLeft + 10 + labelWidth;
        int urlW = Math.max(70, refreshX - urlX - 6);
        this.urlBox = new EditBox(this.font, urlX, this.guiTop + 8, urlW, 16, Component.literal("Resource URL"));
        this.urlBox.setMaxLength(2048);
        this.urlBox.setValue(urlValue);
        this.urlBox.setTextColor(0xFFF3F3E0);
        this.urlBox.setFocused(urlFocused);
        addWidget(this.urlBox);

        int searchX = urlX;
        int searchW = Math.max(84, Math.min(180, this.guiWidth / 3));
        this.searchBox = new EditBox(this.font, searchX, this.guiTop + 30, searchW, 16, Component.literal("Search"));
        this.searchBox.setMaxLength(2048);
        this.searchBox.setValue(searchValue);
        this.searchBox.setTextColor(0xFFF3F3E0);
        this.searchBox.setFocused(searchFocused);
        addWidget(this.searchBox);
        if (searchFocused) {
            setFocused(this.searchBox);
        } else if (urlFocused) {
            setFocused(this.urlBox);
        }

        addRenderableWidget(new FlatColorButton(refreshX, topButtonY, 42, 18, Component.translatable("gui.yes_steve_model.resource_station.refresh"), b -> refresh()));
        addRenderableWidget(new FlatColorButton(saveX, topButtonY, 34, 18, Component.translatable("gui.yes_steve_model.resource_station.save"), b -> saveUrl()));
        addRenderableWidget(new FlatColorButton(deleteX, topButtonY, 34, 18, Component.translatable("gui.yes_steve_model.resource_station.delete_source"), b -> deleteSource()));
        addRenderableWidget(new FlatColorButton(prevX, topButtonY, 18, 18, Component.literal("<"), b -> switchSource(-1)).setTooltipText("gui.yes_steve_model.resource_station.prev_source"));
        addRenderableWidget(new FlatColorButton(nextX, topButtonY, 18, 18, Component.literal(">"), b -> switchSource(1)).setTooltipText("gui.yes_steve_model.resource_station.next_source"));
        FlatColorButton sortButton = new FlatColorButton(searchX + searchW + 6, this.guiTop + 29, 52, 18, Component.translatable("gui.yes_steve_model.resource_station.sort"), b -> cycleSort());
        sortButton.setTooltipLines(List.of(Component.translatable("gui.yes_steve_model.resource_station.sort_mode", this.sortMode.label())));
        addRenderableWidget(sortButton);
        addRenderableWidget(new FlatColorButton(searchX + searchW + 62, this.guiTop + 29, 62, 18, Component.translatable("gui.yes_steve_model.resource_station.queue_all"), b -> enqueueAllVisible()));

        boolean compactHeader = compactHeader();
        int modeWidth = compactHeader ? Math.min(156, this.guiWidth - 20) : 156;
        int modeX = compactHeader ? this.guiLeft + 10 : this.guiLeft + this.guiWidth - modeWidth - 10;
        int modeY = compactHeader ? this.guiTop + 52 : this.guiTop + 29;
        int nativeWidth = modeWidth / 2;
        int mainlandWidth = modeWidth - nativeWidth;
        this.nativeModeButton = new FlatColorButton(modeX, modeY, nativeWidth, 18, Component.translatable("gui.yes_steve_model.resource_station.mode.native"), b -> setMainlandMode(false));
        this.nativeModeButton.setSelected(!this.config.preferGithubAccelerator());
        this.nativeModeButton.setTooltipText("gui.yes_steve_model.resource_station.mode.native.tooltip");
        addRenderableWidget(this.nativeModeButton);
        this.mainlandModeButton = new FlatColorButton(modeX + nativeWidth, modeY, mainlandWidth, 18, Component.translatable("gui.yes_steve_model.resource_station.mode.mainland"), b -> setMainlandMode(true));
        this.mainlandModeButton.setSelected(this.config.preferGithubAccelerator());
        this.mainlandModeButton.setTooltipText("gui.yes_steve_model.resource_station.mode.mainland.tooltip");
        addRenderableWidget(this.mainlandModeButton);

        int footerY = footerButtonY();
        addRenderableWidget(new FlatColorButton(this.guiLeft + this.guiWidth - 68, footerY, 58, 16, Component.translatable("gui.yes_steve_model.model.return"), b -> Minecraft.getInstance().setScreen(this.parentScreen)));
        addRenderableWidget(new FlatColorButton(this.guiLeft + this.guiWidth - 132, footerY, 58, 16, Component.translatable("gui.yes_steve_model.resource_station.download_page"), b -> Minecraft.getInstance().setScreen(new DownloadScreen(this.parentScreen, this))));
        addRenderableWidget(new FlatColorButton(this.guiLeft + Math.max(10, this.guiWidth / 2 - 40), footerY, 52, 16, Component.translatable("gui.yes_steve_model.pre_page"), b -> {
            if (this.page > 0) {
                this.page--;
                init();
            }
        }));
        addRenderableWidget(new FlatColorButton(this.guiLeft + Math.min(this.guiWidth - 130, this.guiWidth / 2 + 72), footerY, 52, 16, Component.translatable("gui.yes_steve_model.next_page"), b -> {
            int maxPage = maxPage(filteredEntries().size());
            if (this.page < maxPage) {
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
            int buttonX = entryButtonX();
            int retryW = this.guiWidth >= 420 ? 44 : 32;
            int downloadW = this.guiWidth >= 420 ? 44 : 42;
            addRenderableWidget(new FlatColorButton(buttonX, y + 5, downloadW, 18, Component.translatable("gui.yes_steve_model.resource_station.download"), b -> enqueue(entry)));
            addRenderableWidget(new FlatColorButton(buttonX + downloadW + 4, y + 5, retryW, 18, Component.translatable("gui.yes_steve_model.resource_station.retry"), b -> enqueue(entry)));
            ensurePreview(entry);
        }
    }

    @Override
    public void removed() {
        this.active = false;
        this.listRequestId++;
        this.pendingListResult = null;
    }

    @Override
    public void tick() {
        super.tick();
        applyPendingListResult();
        ResourceDownloadManager.tick();
    }

    private void refresh() {
        saveUrl();
        if (this.listLoading) {
            this.queuedListRefresh = true;
            this.status = Component.translatable("gui.yes_steve_model.resource_station.loading");
            this.statusColor = ChatFormatting.YELLOW;
            return;
        }
        startListRefresh();
    }

    private void startListRefresh() {
        ResourceStationConfig.State requestConfig = this.config;
        String requestUrl = requestConfig.selectedUrl();
        int requestId = ++this.listRequestId;
        this.listLoading = true;
        this.queuedListRefresh = false;
        this.entries.clear();
        this.page = 0;
        this.status = Component.translatable("gui.yes_steve_model.resource_station.loading");
        this.statusColor = ChatFormatting.YELLOW;
        if (ResourceStationConfig.monitorLogEnabled()) {
            YesSteveModel.LOGGER.info("[YSM-RESOURCE] UI list request start id={} source={}", requestId, requestUrl);
        }
        CompletableFuture.supplyAsync(() -> {
            try {
                return ModelRepoClient.list(requestUrl, requestConfig);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }, RESOURCE_EXECUTOR).orTimeout(taskTimeoutMs(), TimeUnit.MILLISECONDS).whenComplete((result, error) -> {
            this.pendingListResult = new ListResult(requestId, requestUrl, result, error);
            if (ResourceStationConfig.monitorLogEnabled()) {
                YesSteveModel.LOGGER.info("[YSM-RESOURCE] UI list request pending id={} source={} result={} error={}",
                        requestId, requestUrl, result == null ? -1 : result.size(), error == null ? "none" : rootMessage(error));
            }
        });
    }

    private void applyPendingListResult() {
        ListResult pending = this.pendingListResult;
        if (pending == null) {
            return;
        }
        this.pendingListResult = null;
        if (!this.active || pending.requestId != this.listRequestId) {
            return;
        }
        this.listLoading = false;
        if (this.queuedListRefresh || !Objects.equals(this.config.selectedUrl(), pending.sourceUrl)) {
            if (ResourceStationConfig.monitorLogEnabled()) {
                YesSteveModel.LOGGER.info("[YSM-RESOURCE] UI list request superseded id={} requestSource={} currentSource={} queued={}",
                        pending.requestId, pending.sourceUrl, this.config.selectedUrl(), this.queuedListRefresh);
            }
            startListRefresh();
            return;
        }
        if (pending.error != null) {
            this.status = Component.translatable("gui.yes_steve_model.resource_station.error", rootMessage(pending.error));
            this.statusColor = ChatFormatting.RED;
            if (ResourceStationConfig.monitorLogEnabled()) {
                YesSteveModel.LOGGER.info("[YSM-RESOURCE] UI list request error id={} source={} error={}", pending.requestId, pending.sourceUrl, rootMessage(pending.error));
            }
        } else {
            this.entries.clear();
            this.entries.addAll(pending.entries);
            this.page = 0;
            this.status = Component.translatable("gui.yes_steve_model.resource_station.loaded", pending.entries.size());
            this.statusColor = ChatFormatting.GREEN;
            if (ResourceStationConfig.monitorLogEnabled()) {
                YesSteveModel.LOGGER.info("[YSM-RESOURCE] UI list request applied id={} source={} entries={}", pending.requestId, pending.sourceUrl, pending.entries.size());
            }
        }
        init();
    }

    private void enqueue(ModelRepoEntry entry) {
        if (ResourceDownloadManager.enqueue(entry, this.config)) {
            this.status = Component.translatable("gui.yes_steve_model.resource_station.queued", entry.name());
            this.statusColor = ChatFormatting.YELLOW;
            Minecraft.getInstance().setScreen(new DownloadScreen(this.parentScreen, this));
        }
    }

    private void enqueueAllVisible() {
        int added = ResourceDownloadManager.enqueueAll(filteredEntries(), this.config);
        if (added > 0) {
            this.status = Component.translatable("gui.yes_steve_model.resource_station.queued", added);
            this.statusColor = ChatFormatting.YELLOW;
            Minecraft.getInstance().setScreen(new DownloadScreen(this.parentScreen, this));
        }
    }

    private boolean isQueued(ModelRepoEntry entry) {
        return ResourceDownloadManager.isQueued(entry);
    }

    private void saveUrl() {
        String url = this.urlBox.getValue().trim();
        if (url.isBlank()) {
            return;
        }
        ArrayList<String> urls = new ArrayList<>(this.config.urls());
        if (!urls.contains(url)) {
            urls.add(0, url);
        }
        this.sourceIndex = urls.indexOf(url);
        this.config = new ResourceStationConfig.State(urls, url, this.config.timeoutMs(), this.config.maxDownloadBytes(), this.config.preferGithubAccelerator(), this.config.githubAccelerators());
        clearSourceEntries();
        saveConfigNow();
    }

    private void deleteSource() {
        ArrayList<String> urls = new ArrayList<>(this.config.urls());
        String current = this.urlBox.getValue().trim();
        urls.remove(current);
        if (urls.isEmpty()) {
            return;
        }
        this.sourceIndex = Math.min(this.sourceIndex, urls.size() - 1);
        String selected = urls.get(this.sourceIndex);
        this.config = new ResourceStationConfig.State(urls, selected, this.config.timeoutMs(), this.config.maxDownloadBytes(), this.config.preferGithubAccelerator(), this.config.githubAccelerators());
        clearSourceEntries();
        saveConfigNow();
        this.urlBox.setValue(selected);
        refresh();
    }

    private void switchSource(int delta) {
        List<String> urls = this.config.urls();
        if (urls.isEmpty()) {
            return;
        }
        this.sourceIndex = Math.floorMod(this.sourceIndex + delta, urls.size());
        String selected = urls.get(this.sourceIndex);
        this.config = new ResourceStationConfig.State(urls, selected, this.config.timeoutMs(), this.config.maxDownloadBytes(), this.config.preferGithubAccelerator(), this.config.githubAccelerators());
        clearSourceEntries();
        saveConfigNow();
        this.urlBox.setValue(selected);
        refresh();
    }

    private void clearSourceEntries() {
        this.entries.clear();
        this.previewTextures.clear();
        this.loadingPreviews.clear();
        this.page = 0;
        this.pendingListResult = null;
        init();
    }

    private void setMainlandMode(boolean enabled) {
        if (this.config.preferGithubAccelerator() == enabled) {
            return;
        }
        ResourceStationConfig.State newConfig = new ResourceStationConfig.State(this.config.urls(), this.config.selectedUrl(), this.config.timeoutMs(),
                this.config.maxDownloadBytes(), enabled, this.config.githubAccelerators());
        this.config = newConfig;
        saveConfigAsync(newConfig);
        updateModeButtonSelection();
        this.status = Component.translatable(enabled
                ? "gui.yes_steve_model.resource_station.mode.mainland.selected"
                : "gui.yes_steve_model.resource_station.mode.native.selected");
        this.statusColor = ChatFormatting.YELLOW;
    }

    private void updateModeButtonSelection() {
        if (this.nativeModeButton != null) {
            this.nativeModeButton.setSelected(!this.config.preferGithubAccelerator());
        }
        if (this.mainlandModeButton != null) {
            this.mainlandModeButton.setSelected(this.config.preferGithubAccelerator());
        }
    }

    private void saveConfigNow() {
        this.configSaveRequestId++;
        synchronized (this.configSaveLock) {
            ResourceStationConfig.save(this.config);
        }
    }

    private void saveConfigAsync(ResourceStationConfig.State state) {
        int requestId = ++this.configSaveRequestId;
        CompletableFuture.runAsync(() -> {
            synchronized (this.configSaveLock) {
                if (requestId == this.configSaveRequestId) {
                    ResourceStationConfig.save(state);
                }
            }
        }, RESOURCE_EXECUTOR);
    }

    private void cycleSort() {
        this.sortMode = switch (this.sortMode) {
            case NAME -> SortMode.SIZE;
            case SIZE -> SortMode.SOURCE;
            case SOURCE -> SortMode.NAME;
        };
        this.page = 0;
        init();
    }

    private List<ModelRepoEntry> filteredEntries() {
        String query = this.searchBox == null ? "" : this.searchBox.getValue().trim().toLowerCase(Locale.ROOT);
        List<ModelRepoEntry> result = new ArrayList<>(this.entries);
        if (!query.isBlank()) {
            result.removeIf(entry -> !searchText(entry).contains(query));
        }
        Comparator<ModelRepoEntry> comparator = switch (this.sortMode) {
            case SIZE -> Comparator.comparingLong(entry -> entry.size() < 0 ? Long.MAX_VALUE : entry.size());
            case SOURCE -> Comparator.comparing(ModelRepoEntry::description, String.CASE_INSENSITIVE_ORDER).thenComparing(ModelRepoEntry::name, String.CASE_INSENSITIVE_ORDER);
            case NAME -> Comparator.comparing(ModelRepoEntry::name, String.CASE_INSENSITIVE_ORDER);
        };
        result.sort(comparator);
        return result;
    }

    private String searchText(ModelRepoEntry entry) {
        return (entry.name() + " " + entry.fileName() + " " + entry.description() + " " + entry.author() + " " + entry.tags()).toLowerCase(Locale.ROOT);
    }

    private void ensurePreview(ModelRepoEntry entry) {
        if (entry.previewUrl() == null || entry.previewUrl().isBlank() || this.previewTextures.containsKey(entry.previewUrl()) || !this.loadingPreviews.add(entry.previewUrl())) {
            return;
        }
        ResourceStationConfig.State requestConfig = this.config;
        CompletableFuture.supplyAsync(() -> {
            try {
                return ModelRepoClient.downloadPreview(entry, requestConfig);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }, RESOURCE_EXECUTOR).orTimeout(Math.max(10_000L, requestConfig.timeoutMs() * 2L), TimeUnit.MILLISECONDS).whenComplete((data, error) -> ((Executor) Minecraft.getInstance()).execute(() -> {
            this.loadingPreviews.remove(entry.previewUrl());
            if (!this.active || error != null) {
                return;
            }
            Identifier id = Identifier.fromNamespaceAndPath(YesSteveModel.MOD_ID, "resource_station/" + sha1(entry.previewUrl()));
            OuterFileTexture texture = new OuterFileTexture(data);
            texture.doLoad();
            Minecraft.getInstance().getTextureManager().register(id, texture);
            this.previewTextures.put(entry.previewUrl(), id);
        }));
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor extractor, int mouseX, int mouseY, float partialTick) {
        extractTransparentBackground(extractor);
        extractor.fillGradient(this.guiLeft, this.guiTop, this.guiLeft + this.guiWidth, this.guiTop + this.guiHeight, -14540254, -14540254);
        extractor.text(this.font, Component.translatable("gui.yes_steve_model.resource_station.url"), this.guiLeft + 10, this.guiTop + 12, 0xFFF3F3E0);
        extractor.text(this.font, Component.translatable("gui.yes_steve_model.resource_station.search"), this.guiLeft + 10, this.guiTop + 34, 0xFFF3F3E0);
        this.urlBox.extractWidgetRenderState(extractor, mouseX, mouseY, partialTick);
        this.searchBox.extractWidgetRenderState(extractor, mouseX, mouseY, partialTick);
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
            renderEntry(extractor, entry, y);
        }
        int maxPage = maxPage(visible.size());
        Component pageText = Component.literal((this.page + 1) + "/" + (maxPage + 1));
        int footerY = this.guiTop + this.guiHeight - 21;
        extractor.text(this.font, pageText, this.guiLeft + this.guiWidth / 2 + 18, footerY + 4, 0xFFF3F3E0);
        renderQueueStatus(extractor);
        if (!Objects.equals(this.status, Component.empty())) {
            int statusWidth = Math.max(90, this.guiWidth / 2 - 16);
            drawFirstLine(extractor, this.status.copy().withStyle(this.statusColor), statusWidth, this.guiLeft + 12, footerY + 4, 0xFFF3F3E0);
        }
        super.extractRenderState(extractor, mouseX, mouseY, partialTick);
        ((ScreenAccessor) this).ysm$getRenderables().stream().filter(renderable -> renderable instanceof FlatColorButton).forEach(renderable -> ((FlatColorButton) renderable).renderTooltip(extractor, this, mouseX, mouseY));
    }

    private void renderEntry(GuiGraphicsExtractor extractor, ModelRepoEntry entry, int y) {
        int entryLeft = this.guiLeft + 10;
        int entryRight = entryPanelRight();
        int textLeft = entryLeft + 38;
        int textWidth = Math.max(40, entryRight - textLeft - 4);
        extractor.fillGradient(entryLeft, y - 2, entryRight, y + 28, -12369342, -12369342);
        Identifier preview = entry.previewUrl() == null ? null : this.previewTextures.get(entry.previewUrl());
        if (preview != null) {
            extractor.blit(preview, entryLeft + 4, y + 1, entryLeft + 32, y + 29, 0f, 1f, 0f, 1f);
        } else {
            extractor.fillGradient(entryLeft + 4, y + 1, entryLeft + 32, y + 29, -14540254, -14540254);
        }
        drawFirstLine(extractor, Component.literal(entry.name()), textWidth, textLeft, y + 1, 0xFFF3F3E0);
        String meta = entryMeta(entry);
        drawFirstLine(extractor, Component.literal(meta).withStyle(ChatFormatting.GRAY), textWidth, textLeft, y + 11, 0xFFAAAAAA);
        if (!entry.description().isBlank()) {
            drawFirstLine(extractor, Component.literal(entry.description()).withStyle(ChatFormatting.DARK_GRAY), textWidth, textLeft, y + 21, 0xFF888888);
        }
    }

    private void drawFirstLine(GuiGraphicsExtractor extractor, Component component, int width, int x, int y, int color) {
        if (width <= 0) {
            return;
        }
        List<FormattedCharSequence> lines = this.font.split(component, width);
        if (!lines.isEmpty()) {
            extractor.text(this.font, lines.get(0), x, y, color);
        }
    }

    private String entryMeta(ModelRepoEntry entry) {
        List<String> parts = new ArrayList<>();
        if (entry.size() > 0) {
            parts.add(ModelUploadSession.formatBytes((int) Math.min(Integer.MAX_VALUE, entry.size())));
        }
        if (!entry.author().isBlank()) {
            parts.add(entry.author());
        }
        if (!entry.tags().isBlank()) {
            parts.add(entry.tags());
        }
        return String.join("  ", parts);
    }

    private void renderQueueStatus(GuiGraphicsExtractor extractor) {
        ResourceDownloadManager.Snapshot snapshot = ResourceDownloadManager.snapshot();
        int queued = snapshot.queued();
        long failed = snapshot.failed();
        long done = snapshot.done();
        String text = Component.translatable("gui.yes_steve_model.resource_station.queue_status", queued, done, failed).getString();
        int queueY = this.guiTop + this.guiHeight - 39;
        ResourceDownloadManager.TaskSnapshot currentTask = snapshot.currentTask();
        if (currentTask != null) {
            int x = this.guiLeft + 12;
            int w = Math.max(80, Math.min(245, this.guiWidth / 2 - 12));
            extractor.fillGradient(x, queueY, x + w, queueY + 6, -16777216, -16777216);
            extractor.fillGradient(x, queueY, x + (int) (w * currentTask.progress()), queueY + 6, -14774017, -14774017);
            text += "  " + currentTask.state() + " " + Math.round(currentTask.progress() * 100f) + "%";
            if (!Objects.equals(currentTask.message(), Component.empty())) {
                text += "  " + currentTask.message().getString();
            }
        }
        int queueX = this.guiLeft + Math.max(12, this.guiWidth / 2 - 40);
        int queueWidth = Math.max(90, this.guiWidth - (queueX - this.guiLeft) - 80);
        drawFirstLine(extractor, Component.literal(text).withStyle(ChatFormatting.GRAY), queueWidth, queueX, queueY, 0xFFAAAAAA);
    }

    private void updateLayout() {
        this.guiWidth = Math.min(MAX_PANEL_WIDTH, Math.max(MIN_PANEL_WIDTH, this.width - OUTER_MARGIN * 2));
        this.guiHeight = Math.min(MAX_PANEL_HEIGHT, Math.max(MIN_PANEL_HEIGHT, this.height - OUTER_MARGIN * 2));
        this.guiLeft = Math.max(0, (this.width - this.guiWidth) / 2);
        this.guiTop = Math.max(0, (this.height - this.guiHeight) / 2);
        int availableEntryHeight = Math.max(ENTRY_HEIGHT, this.guiHeight - headerHeight() - FOOTER_HEIGHT);
        this.entriesPerPage = Math.max(1, availableEntryHeight / ENTRY_HEIGHT);
        clampPage(this.entries.size());
    }

    private boolean compactHeader() {
        return this.guiWidth < 500;
    }

    private int headerHeight() {
        return compactHeader() ? COMPACT_HEADER_HEIGHT : HEADER_HEIGHT;
    }

    private void clampPage(int entryCount) {
        this.page = Math.min(this.page, maxPage(entryCount));
    }

    private int entryY(int row) {
        return this.guiTop + headerHeight() + row * ENTRY_HEIGHT;
    }

    private int footerButtonY() {
        return this.guiTop + this.guiHeight - 25;
    }

    private int entryButtonX() {
        return this.guiLeft + this.guiWidth - ENTRY_BUTTON_AREA_WIDTH - 10;
    }

    private int entryPanelRight() {
        return entryButtonX() - 8;
    }

    private int maxPage(int entryCount) {
        return Math.max(0, (entryCount - 1) / Math.max(1, this.entriesPerPage));
    }

    @Override
    public boolean charTyped(net.minecraft.client.input.CharacterEvent event) {
        String search = this.searchBox.getValue();
        boolean handled = this.urlBox.charTyped(event) || this.searchBox.charTyped(event) || super.charTyped(event);
        if (!Objects.equals(search, this.searchBox.getValue())) {
            this.page = 0;
            init();
        }
        return handled;
    }

    @Override
    public boolean keyPressed(net.minecraft.client.input.KeyEvent event) {
        String search = this.searchBox.getValue();
        boolean handled = this.urlBox.keyPressed(event) || this.searchBox.keyPressed(event) || super.keyPressed(event);
        if (!Objects.equals(search, this.searchBox.getValue())) {
            this.page = 0;
            init();
        }
        return handled;
    }

    @Override
    public boolean mouseClicked(net.minecraft.client.input.MouseButtonEvent event, boolean flag) {
        if (this.urlBox.mouseClicked(event, flag)) {
            setFocused(this.urlBox);
            return true;
        }
        if (this.searchBox.mouseClicked(event, flag)) {
            setFocused(this.searchBox);
            return true;
        }
        return super.mouseClicked(event, flag);
    }

    private static String rootMessage(Throwable throwable) {
        Throwable current = throwable;
        while (current.getCause() != null) {
            current = current.getCause();
        }
        return current.getMessage() == null ? current.getClass().getSimpleName() : current.getMessage();
    }

    private long taskTimeoutMs() {
        return Math.max(15_000L, this.config.timeoutMs() * 4L);
    }

    private static int progressTotal(int contentLength, long entrySize) {
        if (contentLength > 0) {
            return contentLength;
        }
        if (entrySize > 0 && entrySize <= Integer.MAX_VALUE) {
            return (int) entrySize;
        }
        return 0;
    }

    private static String sha1(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-1");
            return HexFormat.of().formatHex(digest.digest(value.getBytes(StandardCharsets.UTF_8))).substring(0, 16);
        } catch (Exception ignored) {
            return Integer.toHexString(value.hashCode());
        }
    }

    private enum SortMode {
        NAME("name"),
        SIZE("size"),
        SOURCE("source");

        private final String label;

        SortMode(String label) {
            this.label = label;
        }

        private String label() {
            return this.label;
        }
    }

    private record ListResult(int requestId, String sourceUrl, List<ModelRepoEntry> entries, Throwable error) {
    }
}
