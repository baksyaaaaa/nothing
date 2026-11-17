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

    public CoordinateFlightScreen() {
        super(Component.literal("PortalFlight Coordinate Flight"));
    }

    @Override
    protected void init() {
        left = (this.width - 304) / 2;
        top = Math.max(12, (this.height - 320) / 2);
        xField = coordinateField(left + 12, top + 50, "X");
        yField = coordinateField(left + 108, top + 50, "Y");
        zField = coordinateField(left + 204, top + 50, "Z");
        if (this.minecraft != null && this.minecraft.player != null) {
            var pos = this.minecraft.player.position();
            xField.setValue(Integer.toString((int)Math.floor(pos.x)));
            yField.setValue(Integer.toString((int)Math.floor(pos.y)));
            zField.setValue(Integer.toString((int)Math.floor(pos.z)));
        }
        speedField = new EditBox(this.font, left + 12, top + 104, 125, 20,
                Component.literal("Flight speed in blocks/second"));
        speedField.setMaxLength(12);
        speedField.setValue(String.format(java.util.Locale.ROOT, "%.2f",
                PortalFlight.getCoordinateFlightSpeed()));
        addRenderableWidget(speedField);
        addRenderableWidget(Button.builder(Component.literal("Apply Speed"), button -> applySpeed())
                .bounds(left + 145, top + 104, 147, 20).build());
        addRenderableWidget(Button.builder(Component.literal("Fly to Coordinates"), button -> startFlight())
                .bounds(left + 12, top + 135, 280, 22).build());
        addRenderableWidget(Button.builder(Component.literal("TP to Coordinates (Instant Attempt)"),
                button -> teleportToCoordinates())
                .bounds(left + 12, top + 164, 280, 22).build());
        addRenderableWidget(Button.builder(Component.literal("STOP Flight"), button -> {
            PortalFlight.stopCoordinateFlight();
            feedback = "Coordinate flight stopped.";
        }).bounds(left + 12, top + 193, 280, 22).build());
        addRenderableWidget(Button.builder(Component.literal("Back"), button -> {
            if (this.minecraft != null) this.minecraft.setScreen(new PortalFlightMenuScreen());
        }).bounds(left + 12, top + 222, 280, 22).build());
    }

    private EditBox coordinateField(int x, int y, String axis) {
        EditBox field = new EditBox(this.font, x, y, 88, 20, Component.literal(axis));
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
                feedback = "Invalid coordinates or not in a world.";
                return;
            }
            feedback = "Teleport packet sent. Server may reject it.";
        } catch (NumberFormatException exception) {
            feedback = "Enter valid numeric X, Y and Z coordinates.";
        }
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        graphics.fill(0, 0, this.width, this.height, 0xAA000000);
        graphics.fill(left - 2, top - 2, left + 306, top + 320, 0xFF101622);
        graphics.fill(left, top, left + 304, top + 30, 0xFF202C41);
        graphics.drawString(this.font, "COORDINATE FLIGHT", left + 12, top + 11, 0xFF81D4FA);
        graphics.drawString(this.font, "X", left + 12, top + 37, 0xFFE0F0FF);
        graphics.drawString(this.font, "Y", left + 108, top + 37, 0xFFE0F0FF);
        graphics.drawString(this.font, "Z", left + 204, top + 37, 0xFFE0F0FF);
        graphics.drawString(this.font, "Custom speed (blocks/sec, max 1280):",
                left + 12, top + 91, 0xFFE0F0FF);
        graphics.drawString(this.font, feedback, left + 12, top + 277, 0xFFFFCC88);
        graphics.drawString(this.font, "Destination: " + PortalFlight.getCoordinateFlightDestination(),
                left + 12, top + 296, 0xFFA5BED4);
        super.render(graphics, mouseX, mouseY, partialTick);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
