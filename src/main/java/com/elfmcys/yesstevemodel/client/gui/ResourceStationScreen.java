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
    private int guiLeft;
    private int guiTop;
    private int guiWidth;
    private int guiHeight;
    private int entriesPerPage = 1;
    private int page;
    private int sourceIndex;
    private boolean loading;
    private boolean active;
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
        FlatColorButton nativeModeButton = new FlatColorButton(modeX, modeY, nativeWidth, 18, Component.translatable("gui.yes_steve_model.resource_station.mode.native"), b -> setMainlandChinaMode(false));
        nativeModeButton.setSelected(!this.config.mainlandChinaMode());
        nativeModeButton.setTooltipText("gui.yes_steve_model.resource_station.mode.native.tooltip");
        addRenderableWidget(nativeModeButton);
        FlatColorButton mainlandModeButton = new FlatColorButton(modeX + nativeWidth, modeY, mainlandWidth, 18, Component.translatable("gui.yes_steve_model.resource_station.mode.mainland"), b -> setMainlandChinaMode(true));
        mainlandModeButton.setSelected(this.config.mainlandChinaMode());
        mainlandModeButton.setTooltipText("gui.yes_steve_model.resource_station.mode.mainland.tooltip");
        addRenderableWidget(mainlandModeButton);
        int footerY = this.guiTop + this.guiHeight - 25;
        addRenderableWidget(new FlatColorButton(this.guiLeft + this.guiWidth - 68, footerY, 58, 16, Component.translatable("gui.yes_steve_model.model.return"), b -> Minecraft.getInstance().setScreen(this.parentScreen)));
        addRenderableWidget(new FlatColorButton(this.guiLeft + this.guiWidth - 132, footerY, 58, 16, Component.translatable("gui.yes_steve_model.resource_station.download_page"), b -> Minecraft.getInstance().setScreen(new DownloadScreen(this.parentScreen, this))));
        addRenderableWidget(new FlatColorButton(this.guiLeft + Math.max(10, this.guiWidth / 2 - 40), footerY, 52, 16, Component.translatable("gui.yes_steve_model.pre_page"), b -> {
            if (this.page > 0) {
                this.page--;
                init();
            }
        }));
        addRenderableWidget(new FlatColorButton(this.guiLeft + Math.min(this.guiWidth - 130, this.guiWidth / 2 + 72), footerY, 52, 16, Component.translatable("gui.yes_steve_model.next_page"), b -> {
            int maxPage = Math.max(0, (filteredEntries().size() - 1) / this.entriesPerPage);
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
    }

    @Override
    public void tick() {
        super.tick();
        ResourceDownloadManager.tick();
    }

    private void refresh() {
        if (this.loading) {
            return;
        }
        saveUrl();
        String requestUrl = this.config.selectedUrl();
        this.loading = true;
        this.status = Component.translatable("gui.yes_steve_model.resource_station.loading");
        this.statusColor = ChatFormatting.YELLOW;
        CompletableFuture.supplyAsync(() -> {
            try {
                return ModelRepoClient.list(requestUrl, this.config);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }, RESOURCE_EXECUTOR).orTimeout(taskTimeoutMs(), TimeUnit.MILLISECONDS).whenComplete((result, error) -> Minecraft.getInstance().execute(() -> {
            if (!this.active) {
                return;
            }
            this.loading = false;
            if (!Objects.equals(this.config.selectedUrl(), requestUrl)) {
                refresh();
                return;
            }
            if (error != null) {
                this.status = Component.translatable("gui.yes_steve_model.resource_station.error", rootMessage(error));
                this.statusColor = ChatFormatting.RED;
            } else {
                this.entries.clear();
                this.entries.addAll(result);
                this.page = 0;
                this.status = Component.translatable("gui.yes_steve_model.resource_station.loaded", result.size());
                this.statusColor = ChatFormatting.GREEN;
            }
            init();
        }));
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
        int maxPage = Math.max(0, (entryCount - 1) / Math.max(1, this.entriesPerPage));
        this.page = Math.min(this.page, maxPage);
    }

    private int entryY(int row) {
        return this.guiTop + headerHeight() + row * ENTRY_HEIGHT;
    }

    private int entryButtonX() {
        return this.guiLeft + this.guiWidth - ENTRY_BUTTON_AREA_WIDTH - 10;
    }

    private int entryPanelRight() {
        return entryButtonX() - 8;
    }

    private void setMainlandChinaMode(boolean mainlandChinaMode) {
        if (this.config.mainlandChinaMode() == mainlandChinaMode) {
            return;
        }
        this.config = this.config.withMainlandChinaMode(mainlandChinaMode);
        ResourceStationConfig.save(this.config);
        this.status = Component.translatable(this.config.mainlandChinaMode()
                ? "gui.yes_steve_model.resource_station.mode.mainland.selected"
                : "gui.yes_steve_model.resource_station.mode.native.selected");
        this.statusColor = ChatFormatting.YELLOW;
        init();
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
        this.config = this.config.withSelectedUrl(urls, url);
        clearSourceEntries();
        ResourceStationConfig.save(this.config);
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
        this.config = this.config.withSelectedUrl(urls, selected);
        clearSourceEntries();
        ResourceStationConfig.save(this.config);
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
        this.config = this.config.withSelectedUrl(urls, selected);
        clearSourceEntries();
        ResourceStationConfig.save(this.config);
        this.urlBox.setValue(selected);
        refresh();
    }

    private void clearSourceEntries() {
        this.entries.clear();
        this.previewTextures.clear();
        this.loadingPreviews.clear();
        this.page = 0;
        init();
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
        CompletableFuture.supplyAsync(() -> {
            try {
                return ModelRepoClient.downloadPreview(entry, this.config);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }, RESOURCE_EXECUTOR).orTimeout(Math.max(10_000L, this.config.timeoutMs() * 2L), TimeUnit.MILLISECONDS).whenComplete((data, error) -> Minecraft.getInstance().execute(() -> {
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
        int maxPage = Math.max(0, (visible.size() - 1) / Math.max(1, this.entriesPerPage));
        int footerY = this.guiTop + this.guiHeight - 21;
        extractor.text(this.font, Component.literal((this.page + 1) + "/" + (maxPage + 1)), this.guiLeft + this.guiWidth / 2 + 18, footerY + 4, 0xFFF3F3E0);
        renderQueueStatus(extractor);
        if (!Objects.equals(this.status, Component.empty())) {
            int statusWidth = Math.max(90, this.guiWidth / 2 - 16);
            extractor.text(this.font, this.font.split(this.status.copy().withStyle(this.statusColor), statusWidth).get(0), this.guiLeft + 12, footerY + 4, 0xFFF3F3E0);
        }
        super.extractRenderState(extractor, mouseX, mouseY, partialTick);
        ((ScreenAccessor) this).ysm$getRenderables().stream().filter(renderable -> renderable instanceof FlatColorButton).forEach(renderable -> ((FlatColorButton) renderable).renderTooltip(extractor, this, mouseX, mouseY));
    }

    private void renderEntry(GuiGraphicsExtractor extractor, ModelRepoEntry entry, int y) {
        int left = this.guiLeft + 10;
        int right = entryPanelRight();
        int textLeft = left + 38;
        int textWidth = Math.max(40, right - textLeft - 4);
        extractor.fillGradient(left, y - 2, right, y + 28, -12369342, -12369342);
        Identifier preview = entry.previewUrl() == null ? null : this.previewTextures.get(entry.previewUrl());
        if (preview != null) {
            extractor.blit(preview, left + 4, y + 1, left + 32, y + 29, 0f, 1f, 0f, 1f);
        } else {
            extractor.fillGradient(left + 4, y + 1, left + 32, y + 29, -14540254, -14540254);
        }
        List<FormattedCharSequence> nameLines = this.font.split(Component.literal(entry.name()), textWidth);
        if (!nameLines.isEmpty()) {
            extractor.text(this.font, nameLines.get(0), textLeft, y + 1, 0xFFF3F3E0);
        }
        String meta = entryMeta(entry);
        extractor.text(this.font, this.font.split(Component.literal(meta).withStyle(ChatFormatting.GRAY), textWidth).get(0), textLeft, y + 11, 0xFFAAAAAA);
        if (!entry.description().isBlank()) {
            extractor.text(this.font, this.font.split(Component.literal(entry.description()).withStyle(ChatFormatting.DARK_GRAY), textWidth).get(0), textLeft, y + 21, 0xFF888888);
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
        ResourceDownloadManager.TaskSnapshot currentTask = snapshot.currentTask();
        if (currentTask != null) {
            int x = this.guiLeft + 12;
            int y = this.guiTop + this.guiHeight - 39;
            int w = Math.max(80, Math.min(245, this.guiWidth / 2 - 12));
            extractor.fillGradient(x, y, x + w, y + 6, -16777216, -16777216);
            extractor.fillGradient(x, y, x + (int) (w * currentTask.progress()), y + 6, -14774017, -14774017);
            text += "  " + currentTask.state() + " " + Math.round(currentTask.progress() * 100f) + "%";
            if (!Objects.equals(currentTask.message(), Component.empty())) {
                text += "  " + currentTask.message().getString();
            }
        }
        int queueX = this.guiLeft + Math.max(12, this.guiWidth / 2 - 40);
        int queueWidth = Math.max(90, this.guiWidth - (queueX - this.guiLeft) - 80);
        extractor.text(this.font, this.font.split(Component.literal(text).withStyle(ChatFormatting.GRAY), queueWidth).get(0), queueX, this.guiTop + this.guiHeight - 39, 0xFFAAAAAA);
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
}
