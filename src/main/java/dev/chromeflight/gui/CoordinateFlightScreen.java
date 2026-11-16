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
    private String feedback = "";
    private int left;
    private int top;

    public CoordinateFlightScreen() {
        super(Component.literal("PortalFlight Coordinate Flight"));
    }

    @Override
    protected void init() {
        left = (this.width - 304) / 2;
        top = Math.max(12, (this.height - 206) / 2);
        xField = coordinateField(left + 12, top + 50, "X");
        yField = coordinateField(left + 108, top + 50, "Y");
        zField = coordinateField(left + 204, top + 50, "Z");
        if (this.minecraft != null && this.minecraft.player != null) {
            var pos = this.minecraft.player.position();
            xField.setValue(Integer.toString((int)Math.floor(pos.x)));
            yField.setValue(Integer.toString((int)Math.floor(pos.y)));
            zField.setValue(Integer.toString((int)Math.floor(pos.z)));
        }
        addRenderableWidget(Button.builder(Component.literal("Fly to Coordinates"), button -> startFlight())
                .bounds(left + 12, top + 86, 280, 22).build());
        addRenderableWidget(Button.builder(Component.literal("STOP Flight"), button -> {
            PortalFlight.stopCoordinateFlight();
            feedback = "Coordinate flight stopped.";
        }).bounds(left + 12, top + 116, 280, 22).build());
        addRenderableWidget(Button.builder(Component.literal("Back"), button -> {
            if (this.minecraft != null) this.minecraft.setScreen(new PortalFlightMenuScreen());
        }).bounds(left + 12, top + 146, 280, 22).build());
    }

    private EditBox coordinateField(int x, int y, String axis) {
        EditBox field = new EditBox(this.font, x, y, 88, 20, Component.literal(axis));
        field.setMaxLength(22);
        addRenderableWidget(field);
        return field;
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

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        graphics.fill(0, 0, this.width, this.height, 0xAA000000);
        graphics.fill(left - 2, top - 2, left + 306, top + 208, 0xFF101622);
        graphics.fill(left, top, left + 304, top + 30, 0xFF202C41);
        graphics.drawString(this.font, "COORDINATE FLIGHT", left + 12, top + 11, 0xFF81D4FA);
        graphics.drawString(this.font, "X", left + 12, top + 37, 0xFFE0F0FF);
        graphics.drawString(this.font, "Y", left + 108, top + 37, 0xFFE0F0FF);
        graphics.drawString(this.font, "Z", left + 204, top + 37, 0xFFE0F0FF);
        graphics.drawString(this.font, feedback, left + 12, top + 180, 0xFFFFCC88);
        graphics.drawString(this.font, "Destination: " + PortalFlight.getCoordinateFlightDestination(),
                left + 12, top + 194, 0xFFA5BED4);
        super.render(graphics, mouseX, mouseY, partialTick);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
