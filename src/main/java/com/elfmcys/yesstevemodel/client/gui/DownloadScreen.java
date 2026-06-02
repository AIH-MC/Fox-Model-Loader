package com.elfmcys.yesstevemodel.client.gui;

import com.elfmcys.yesstevemodel.client.gui.button.FlatColorButton;
import com.elfmcys.yesstevemodel.client.gui.resource.ResourceDownloadManager;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;

import java.util.List;
import java.util.Objects;


public class DownloadScreen extends Screen {
    private static final int OUTER_MARGIN = 8;
    private static final int MIN_PANEL_WIDTH = 320;
    private static final int MAX_PANEL_WIDTH = 620;
    private static final int MIN_PANEL_HEIGHT = 220;
    private static final int MAX_PANEL_HEIGHT = 420;
    private static final int HEADER_HEIGHT = 36;
    private static final int FOOTER_HEIGHT = 34;
    private static final int ROW_HEIGHT = 30;

    private final PlayerModelScreen parentScreen;
    private final ResourceStationScreen resourceStationScreen;
    private int guiLeft;
    private int guiTop;
    private int guiWidth;
    private int guiHeight;
    private int rows;
    private int page;

    public DownloadScreen(PlayerModelScreen modelScreen) {
        this(modelScreen, null);
    }

    public DownloadScreen(PlayerModelScreen modelScreen, ResourceStationScreen resourceStationScreen) {
        super(Component.translatable("gui.yes_steve_model.resource_station.download_page.title"));
        this.parentScreen = modelScreen;
        this.resourceStationScreen = resourceStationScreen;
    }

    @Override
    public void init() {
        clearWidgets();
        updateLayout();
        int topY = this.guiTop + 8;
        int right = this.guiLeft + this.guiWidth - 10;
        addRenderableWidget(new FlatColorButton(this.guiLeft + 10, topY, 58, 18, Component.translatable("gui.yes_steve_model.resource_station.title"), b -> {
            Minecraft.getInstance().setScreen(this.resourceStationScreen == null ? new ResourceStationScreen(this.parentScreen) : this.resourceStationScreen);
        }));
        addRenderableWidget(new FlatColorButton(right - 138, topY, 74, 18, Component.translatable("gui.yes_steve_model.resource_station.clear_finished"), b -> {
            ResourceDownloadManager.clearFinished();
            this.page = 0;
            init();
        }));
        addRenderableWidget(new FlatColorButton(right - 58, topY, 58, 18, Component.translatable("gui.yes_steve_model.model.return"), b -> Minecraft.getInstance().setScreen(this.parentScreen)));
        int footerY = this.guiTop + this.guiHeight - 25;
        addRenderableWidget(new FlatColorButton(this.guiLeft + Math.max(10, this.guiWidth / 2 - 60), footerY, 52, 16, Component.translatable("gui.yes_steve_model.pre_page"), b -> {
            if (this.page > 0) {
                this.page--;
                init();
            }
        }));
        addRenderableWidget(new FlatColorButton(this.guiLeft + Math.min(this.guiWidth - 70, this.guiWidth / 2 + 16), footerY, 52, 16, Component.translatable("gui.yes_steve_model.next_page"), b -> {
            int maxPage = maxPage(ResourceDownloadManager.snapshot().tasks().size());
            if (this.page < maxPage) {
                this.page++;
                init();
            }
        }));
    }

    @Override
    public void tick() {
        super.tick();
        ResourceDownloadManager.tick();
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor extractor, int mouseX, int mouseY, float partialTick) {
        extractTransparentBackground(extractor);
        ResourceDownloadManager.Snapshot snapshot = ResourceDownloadManager.snapshot();
        extractor.fillGradient(this.guiLeft, this.guiTop, this.guiLeft + this.guiWidth, this.guiTop + this.guiHeight, -14540254, -14540254);
        extractor.text(this.font, Component.translatable("gui.yes_steve_model.resource_station.download_page.title"), this.guiLeft + 78, this.guiTop + 12, 0xFFF3F3E0);
        renderCurrentTask(extractor, snapshot);
        renderHistory(extractor, snapshot);
        renderFooter(extractor, snapshot);
        super.extractRenderState(extractor, mouseX, mouseY, partialTick);
    }

    private void renderCurrentTask(GuiGraphicsExtractor extractor, ResourceDownloadManager.Snapshot snapshot) {
        int x = this.guiLeft + 10;
        int y = this.guiTop + HEADER_HEIGHT;
        int w = this.guiWidth - 20;
        extractor.fillGradient(x, y, x + w, y + 46, -12369342, -12369342);
        ResourceDownloadManager.TaskSnapshot task = snapshot.currentTask();
        if (task == null) {
            drawFirstLine(extractor, Component.translatable("gui.yes_steve_model.resource_station.download_page.idle").withStyle(ChatFormatting.GRAY), w - 12, x + 6, y + 8, 0xFFAAAAAA);
            return;
        }
        drawFirstLine(extractor, Component.literal(task.name()), w - 12, x + 6, y + 5, 0xFFF3F3E0);
        String meta = task.state() + "  " + Math.round(task.progress() * 100f) + "%";
        if (!Objects.equals(task.message(), Component.empty())) {
            meta += "  " + task.message().getString();
        }
        drawFirstLine(extractor, Component.literal(meta).withStyle(ChatFormatting.GRAY), w - 12, x + 6, y + 17, 0xFFAAAAAA);
        int barX = x + 6;
        int barY = y + 34;
        int barW = w - 12;
        extractor.fillGradient(barX, barY, barX + barW, barY + 6, -16777216, -16777216);
        extractor.fillGradient(barX, barY, barX + (int) (barW * task.progress()), barY + 6, -14774017, -14774017);
    }

    private void renderHistory(GuiGraphicsExtractor extractor, ResourceDownloadManager.Snapshot snapshot) {
        List<ResourceDownloadManager.TaskSnapshot> tasks = snapshot.tasks();
        int startY = this.guiTop + HEADER_HEIGHT + 54;
        int start = this.page * this.rows;
        for (int i = 0; i < this.rows; i++) {
            int index = start + i;
            if (index >= tasks.size()) {
                break;
            }
            ResourceDownloadManager.TaskSnapshot task = tasks.get(index);
            int y = startY + i * ROW_HEIGHT;
            int x = this.guiLeft + 10;
            int w = this.guiWidth - 20;
            extractor.fillGradient(x, y, x + w, y + ROW_HEIGHT - 3, -13421773, -13421773);
            drawFirstLine(extractor, Component.literal(task.name()), w - 12, x + 6, y + 3, 0xFFF3F3E0);
            String text = task.state() + "  " + Math.round(task.progress() * 100f) + "%";
            if (!Objects.equals(task.message(), Component.empty())) {
                text += "  " + task.message().getString();
            }
            drawFirstLine(extractor, Component.literal(text).withStyle(statusStyle(task.state())), w - 12, x + 6, y + 15, 0xFFAAAAAA);
        }
    }

    private void renderFooter(GuiGraphicsExtractor extractor, ResourceDownloadManager.Snapshot snapshot) {
        int maxPage = maxPage(snapshot.tasks().size());
        int footerY = this.guiTop + this.guiHeight - 21;
        extractor.text(this.font, Component.literal((this.page + 1) + "/" + (maxPage + 1)), this.guiLeft + this.guiWidth / 2 - 2, footerY + 4, 0xFFF3F3E0);
        String queue = Component.translatable("gui.yes_steve_model.resource_station.queue_status", snapshot.queued(), snapshot.done(), snapshot.failed()).getString();
        drawFirstLine(extractor, Component.literal(queue).withStyle(ChatFormatting.GRAY), Math.max(80, this.guiWidth / 2 - 12), this.guiLeft + 12, footerY - 12, 0xFFAAAAAA);
        if (!Objects.equals(snapshot.status(), Component.empty())) {
            drawFirstLine(extractor, snapshot.status().copy().withStyle(snapshot.statusColor()), Math.max(100, this.guiWidth / 2 - 16), this.guiLeft + Math.max(90, this.guiWidth / 2 + 24), footerY - 12, 0xFFF3F3E0);
        }
    }

    private ChatFormatting statusStyle(ResourceDownloadManager.TaskState state) {
        return switch (state) {
            case DONE -> ChatFormatting.GREEN;
            case FAILED -> ChatFormatting.RED;
            default -> ChatFormatting.GRAY;
        };
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

    private void updateLayout() {
        this.guiWidth = Math.min(MAX_PANEL_WIDTH, Math.max(MIN_PANEL_WIDTH, this.width - OUTER_MARGIN * 2));
        this.guiHeight = Math.min(MAX_PANEL_HEIGHT, Math.max(MIN_PANEL_HEIGHT, this.height - OUTER_MARGIN * 2));
        this.guiLeft = Math.max(0, (this.width - this.guiWidth) / 2);
        this.guiTop = Math.max(0, (this.height - this.guiHeight) / 2);
        int historyHeight = Math.max(ROW_HEIGHT, this.guiHeight - HEADER_HEIGHT - FOOTER_HEIGHT - 54);
        this.rows = Math.max(1, historyHeight / ROW_HEIGHT);
        this.page = Math.min(this.page, maxPage(ResourceDownloadManager.snapshot().tasks().size()));
    }

    private int maxPage(int taskCount) {
        return Math.max(0, (taskCount - 1) / Math.max(1, this.rows));
    }
}
