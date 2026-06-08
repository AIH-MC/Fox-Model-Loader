package com.elfmcys.yesstevemodel.client.gui;

import com.elfmcys.yesstevemodel.client.gui.button.FlatColorButton;
import com.elfmcys.yesstevemodel.client.gui.resource.ResourceDownloadManager;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class DownloadScreen extends Screen {
    private static final int PANEL_WIDTH = 420;
    private static final int PANEL_HEIGHT = 235;

    private final PlayerModelScreen parentScreen;
    private final Screen resourceStationScreen;
    private int guiLeft;
    private int guiTop;

    public DownloadScreen(PlayerModelScreen modelScreen) {
        this(modelScreen, null);
    }

    public DownloadScreen(PlayerModelScreen modelScreen, Screen resourceStationScreen) {
        super(Component.translatable("gui.yes_steve_model.resource_station.downloads"));
        this.parentScreen = modelScreen;
        this.resourceStationScreen = resourceStationScreen;
    }

    @Override
    protected void init() {
        clearWidgets();
        this.guiLeft = (this.width - PANEL_WIDTH) / 2;
        this.guiTop = (this.height - PANEL_HEIGHT) / 2;
        addRenderableWidget(new FlatColorButton(this.guiLeft + 8, this.guiTop + PANEL_HEIGHT - 22, 58, 16, Component.translatable("gui.yes_steve_model.model.return"), button -> {
            Minecraft.getInstance().setScreen(this.resourceStationScreen == null ? this.parentScreen : this.resourceStationScreen);
        }));
        addRenderableWidget(new FlatColorButton(this.guiLeft + 72, this.guiTop + PANEL_HEIGHT - 22, 70, 16, Component.translatable("gui.yes_steve_model.resource_station.clear_finished"), button -> ResourceDownloadManager.clearFinished()));
        ResourceDownloadManager.Snapshot snapshot = ResourceDownloadManager.snapshot();
        FlatColorButton cancelButton = new FlatColorButton(this.guiLeft + 148, this.guiTop + PANEL_HEIGHT - 22, 70, 16, Component.translatable("gui.yes_steve_model.resource_station.cancel_current"), button -> {
            ResourceDownloadManager.cancelCurrent();
            init();
        });
        cancelButton.active = snapshot.currentTask() != null;
        addRenderableWidget(cancelButton);
    }

    @Override
    public void tick() {
        super.tick();
        ResourceDownloadManager.tick();
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        renderTransparentBackground(guiGraphics);
        guiGraphics.fill(this.guiLeft, this.guiTop, this.guiLeft + PANEL_WIDTH, this.guiTop + PANEL_HEIGHT, 0xE0202020);
        guiGraphics.fill(this.guiLeft, this.guiTop, this.guiLeft + PANEL_WIDTH, this.guiTop + 2, 0xFFB15D2B);
        guiGraphics.drawString(this.font, this.title, this.guiLeft + 10, this.guiTop + 10, 0xFFE9E0D0, false);
        renderTasks(guiGraphics);
        super.render(guiGraphics, mouseX, mouseY, partialTick);
    }

    private void renderTasks(GuiGraphics guiGraphics) {
        ResourceDownloadManager.Snapshot snapshot = ResourceDownloadManager.snapshot();
        int y = this.guiTop + 30;
        int statusColor = snapshot.statusColor().getColor() == null ? 0xFFBDBDBD : snapshot.statusColor().getColor();
        guiGraphics.drawString(this.font, snapshot.status(), this.guiLeft + 10, y, statusColor, false);
        y += 16;

        List<ResourceDownloadManager.TaskSnapshot> rows = new ArrayList<>();
        rows.addAll(snapshot.unfinishedTasks());
        rows.addAll(snapshot.finishedTasks().stream().limit(8).toList());
        if (rows.isEmpty()) {
            guiGraphics.drawCenteredString(this.font, Component.translatable("gui.yes_steve_model.resource_station.no_downloads"), this.guiLeft + PANEL_WIDTH / 2, this.guiTop + 92, 0xFF8F8F8F);
            return;
        }

        int maxRows = Math.min(9, rows.size());
        for (int i = 0; i < maxRows; i++) {
            ResourceDownloadManager.TaskSnapshot row = rows.get(i);
            int rowY = y + i * 18;
            guiGraphics.fill(this.guiLeft + 10, rowY - 2, this.guiLeft + PANEL_WIDTH - 10, rowY + 15, i % 2 == 0 ? 0x66313131 : 0x66262626);
            guiGraphics.drawString(this.font, trim(row.name(), 140), this.guiLeft + 14, rowY + 2, 0xFFEDE1CC, false);
            guiGraphics.drawString(this.font, stateLabel(row.state()), this.guiLeft + 158, rowY + 2, stateColor(row.state()), false);
            int barX = this.guiLeft + 232;
            int barY = rowY + 3;
            guiGraphics.fill(barX, barY, barX + 78, barY + 6, 0xAA101010);
            int fill = Math.max(0, Math.min(78, (int) (row.progress() * 78f)));
            guiGraphics.fill(barX, barY, barX + fill, barY + 6, 0xFFB15D2B);
            guiGraphics.drawString(this.font, trim(row.message().getString(), 88), this.guiLeft + 316, rowY + 2, 0xFF9FA8A6, false);
        }
    }

    private Component stateLabel(ResourceDownloadManager.TaskState state) {
        return Component.translatable("gui.yes_steve_model.resource_station.state." + state.name().toLowerCase(Locale.ROOT));
    }

    private int stateColor(ResourceDownloadManager.TaskState state) {
        return switch (state) {
            case DONE -> ChatFormatting.GREEN.getColor();
            case FAILED -> ChatFormatting.RED.getColor();
            case CANCELLED -> ChatFormatting.GRAY.getColor();
            default -> ChatFormatting.YELLOW.getColor();
        };
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
}
