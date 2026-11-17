package dev.chromeflight.gui;

import dev.chromeflight.PortalFlight;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public final class CoordinateFlightScreen extends Screen {
    private EditBox xField;
    private EditBox yField;
    private EditBox zField;
    private EditBox speedField;
    private String feedback = "";
    private int left;
    private int top;
    private int panelWidth;
    private int panelHeight;
    private int[] fieldPositions = new int[3];
    private int speedLabelY;
    private int feedbackY;
    private int destinationY;

    public CoordinateFlightScreen() {
        super(Component.literal("PortalFlight Coordinate Flight"));
    }

    @Override
    protected void init() {
        panelWidth = Math.min(304, Math.max(160, this.width - 8));
        panelHeight = Math.min(320, Math.max(155, this.height - 8));
        left = (this.width - panelWidth) / 2;
        top = (this.height - panelHeight) / 2;
        int inner = panelWidth - 16;
        int gap = 3;
        int fieldWidth = (inner - 2 * gap) / 3;
        boolean compact = panelHeight < 286;
        int coordinateY = top + (compact ? 33 : 50);
        for (int i = 0; i < 3; i++) {
            fieldPositions[i] = left + 8 + i * (fieldWidth + gap);
        }
        xField = coordinateField(fieldPositions[0], coordinateY, "X", fieldWidth);
        yField = coordinateField(fieldPositions[1], coordinateY, "Y", fieldWidth);
        zField = coordinateField(fieldPositions[2], coordinateY, "Z", fieldWidth);
        if (this.minecraft != null && this.minecraft.player != null) {
            var pos = this.minecraft.player.position();
            xField.setValue(Integer.toString((int) Math.floor(pos.x)));
            yField.setValue(Integer.toString((int) Math.floor(pos.y)));
            zField.setValue(Integer.toString((int) Math.floor(pos.z)));
        }
        int speedY = coordinateY + (compact ? 26 : 54);
        speedLabelY = speedY - 11;
        speedField = new EditBox(this.font, left + 8, speedY, inner / 2 - 3, 18,
                Component.literal("Flight speed in blocks/second"));
        speedField.setMaxLength(12);
        speedField.setValue(String.format(java.util.Locale.ROOT, "%.2f",
                PortalFlight.getCoordinateFlightSpeed()));
        addRenderableWidget(speedField);
        addRenderableWidget(Button.builder(Component.literal("Apply Speed"), button -> applySpeed())
                .bounds(left + 8 + inner / 2 + 3, speedY, inner / 2 - 3, 18).build());

        int actionY = speedY + 23;
        int actionGap = compact ? 17 : 29;
        addRenderableWidget(Button.builder(Component.literal("Fly to Coordinates"), button -> startFlight())
                .bounds(left + 8, actionY, inner, 18).build());
        addRenderableWidget(Button.builder(Component.literal("TP to Coordinates (Instant Attempt)"),
                button -> teleportToCoordinates())
                .bounds(left + 8, actionY + actionGap, inner, 18).build());
        addRenderableWidget(Button.builder(Component.literal("STOP Flight"), button -> {
            PortalFlight.stopCoordinateFlight();
            feedback = "Coordinate flight stopped.";
        }).bounds(left + 8, actionY + actionGap * 2, inner, 18).build());
        addRenderableWidget(Button.builder(Component.literal("Back"), button -> {
            if (this.minecraft != null) this.minecraft.setScreen(new PortalFlightMenuScreen());
        }).bounds(left + 8, actionY + actionGap * 3, inner, 18).build());
        feedbackY = Math.min(top + panelHeight - 25, actionY + actionGap * 3 + 23);
        destinationY = Math.min(top + panelHeight - 13, feedbackY + 11);
    }

    private EditBox coordinateField(int x, int y, String axis, int fieldWidth) {
        EditBox field = new EditBox(this.font, x, y, fieldWidth, 18, Component.literal(axis));
        field.setMaxLength(22);
        addRenderableWidget(field);
        return field;
    }

    private void applySpeed() {
        try {
            double value = Double.parseDouble(speedField.getValue().trim());
            if (PortalFlight.setCoordinateFlightSpeed(value)) {
                feedback = "Speed saved: " + String.format(java.util.Locale.ROOT, "%.2f", value)
                        + " blocks/second";
            } else {
                feedback = "Enter a speed above 0, up to 1280 blocks/sec.";
            }
        } catch (NumberFormatException exception) {
            feedback = "Enter a speed like 320, 800, or 1280.";
        }
    }

    private void startFlight() {
        try {
            double x = Double.parseDouble(xField.getValue().trim());
            double y = Double.parseDouble(yField.getValue().trim());
            double z = Double.parseDouble(zField.getValue().trim());
            if (!PortalFlight.startCoordinateFlight(x, y, z)) {
                feedback = "Cannot fly here; check location and world state.";
                return;
            }
            feedback = "Flight started. Close this menu to watch.";
        } catch (NumberFormatException exception) {
            feedback = "Enter valid numeric X, Y and Z coordinates.";
        }
    }

    private void teleportToCoordinates() {
        try {
            double x = Double.parseDouble(xField.getValue().trim());
            double y = Double.parseDouble(yField.getValue().trim());
            double z = Double.parseDouble(zField.getValue().trim());
            if (!PortalFlight.attemptCoordinateTeleport(x, y, z)) {
                feedback = PortalFlight.getCoordinateTeleportError();
                return;
            }
            feedback = "Teleport packet sent. Checking position...";
            // Closing the GUI immediately reveals whether the client moved.
            // PortalFlight reports a follow-up status after 2 seconds.
            if (minecraft != null) minecraft.setScreen(null);
        } catch (NumberFormatException exception) {
            feedback = "Enter valid numeric X, Y and Z coordinates.";
        }
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        graphics.fill(0, 0, this.width, this.height, 0xAA000000);
        graphics.fill(left - 1, top - 1, left + panelWidth + 1,
                top + panelHeight + 1, 0xFF101622);
        graphics.fill(left, top, left + panelWidth, top + 23, 0xFF202C41);
        graphics.drawString(this.font, "COORDINATE FLIGHT / TP",
                left + 8, top + 8, 0xFF81D4FA);
        for (int i = 0; i < 3; i++) {
            graphics.drawString(this.font, "XYZ".substring(i, i + 1),
                    fieldPositions[i], yField.getY() - 11, 0xFFE0F0FF);
        }
        graphics.drawString(this.font, "Speed (blocks/sec, max 1280)",
                left + 8, speedLabelY, 0xFFE0F0FF);
        String summary = feedback.isEmpty() ? PortalFlight.getLastTeleportStatus() : feedback;
        if (panelHeight >= 195) {
            graphics.drawString(this.font, this.font.plainSubstrByWidth(summary, panelWidth - 16),
                    left + 8, feedbackY, 0xFFFFCC88);
            graphics.drawString(this.font, this.font.plainSubstrByWidth(
                            "Destination: " + PortalFlight.getCoordinateFlightDestination(), panelWidth - 16),
                    left + 8, destinationY, 0xFFA5BED4);
        }
        super.render(graphics, mouseX, mouseY, partialTick);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
