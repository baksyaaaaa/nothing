package dev.chromeflight.gui;

import dev.chromeflight.PortalFlight;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/**
 * Original PortalFlight ClickGUI. A dark category layout inspired by general
 * Minecraft client GUI conventions; no NiceWurst code or features are included.
 */
public final class PortalFlightMenuScreen extends Screen {
    private static final String[] CATEGORIES = {"Movement", "Render", "Combat", "Utility"};
    private static final String[][] MODULE_IDS = {
        {"fly", "speed", "boat", "swim", "noclip", "sprint"},
        {"freecam", "players", "mobs", "tracers", "storage"},
        {"orbit"},
        {"nofall", "hunger", "privacy", "portal"}
    };
    private static final String[][] MODULE_NAMES = {
        {"Fly", "Speed", "Boat Fly", "Swim Boost", "Noclip", "Auto Sprint"},
        {"Freecam", "Player ESP", "Mob ESP", "ESP Tracers", "Storage Finder"},
        {"Orbit Combat"},
        {"No Fall", "Hunger Saver", "Spectator Privacy", "Portal Locator"}
    };

    private int category;
    private int panelX;
    private int panelY;
    private int panelWidth;

    public PortalFlightMenuScreen() {
        this(0);
    }

    private PortalFlightMenuScreen(int selectedCategory) {
        super(Component.literal("PortalFlight ClickGUI"));
        this.category = selectedCategory;
    }

    @Override
    protected void init() {
        panelWidth = Math.min(390, this.width - 18);
        panelX = (this.width - panelWidth) / 2;
        panelY = Math.max(12, (this.height - 285) / 2);
        int inner = panelWidth - 20;
        int tabWidth = inner / CATEGORIES.length;

        for (int i = 0; i < CATEGORIES.length; i++) {
            final int choice = i;
            int tabX = panelX + 10 + i * tabWidth;
            int width = (i == CATEGORIES.length - 1) ? inner - i * tabWidth : tabWidth - 2;
            addRenderableWidget(Button.builder(Component.literal(CATEGORIES[i]), btn -> {
                if (this.minecraft != null) this.minecraft.setScreen(new PortalFlightMenuScreen(choice));
            }).bounds(tabX, panelY + 38, width, 20).build());
        }

        if (category == 0) {
            addRenderableWidget(Button.builder(Component.literal("Coordinate Flight Settings - Speed / XYZ"), btn -> {
                if (this.minecraft != null) this.minecraft.setScreen(new CoordinateFlightScreen());
            }).bounds(panelX + 10, panelY + 191, inner, 22).build());
        }

        if (category == 3) {
            addRenderableWidget(Button.builder(Component.literal("Staff Watch - View alerts"), btn -> {
                if (this.minecraft != null) this.minecraft.setScreen(new StaffWatchScreen());
            }).bounds(panelX + 10, panelY + 191, inner, 22).build());
            addRenderableWidget(Button.builder(Component.literal("Coordinate Flight - Fly to XYZ"), btn -> {
                if (this.minecraft != null) this.minecraft.setScreen(new CoordinateFlightScreen());
            }).bounds(panelX + 10, panelY + 218, inner, 22).build());
        }

        String[] ids = MODULE_IDS[category];
        String[] labels = MODULE_NAMES[category];
        int buttonWidth = (inner - 8) / 2;
        for (int i = 0; i < ids.length; i++) {
            final String id = ids[i];
            final String label = labels[i];
            int col = i % 2;
            int row = i / 2;
            int x = panelX + 10 + col * (buttonWidth + 8);
            int y = panelY + 72 + row * 29;
            addRenderableWidget(Button.builder(moduleLabel(id, label), btn -> {
                PortalFlight.toggleModule(id);
                btn.setMessage(moduleLabel(id, label));
            }).bounds(x, y, buttonWidth, 24).build());
        }
    }

    private static Component moduleLabel(String id, String label) {
        return Component.literal((PortalFlight.isModuleEnabled(id) ? "[ON]  " : "[OFF]  ") + label);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        int bottom = panelY + 285;
        graphics.fill(0, 0, this.width, this.height, 0x99000000);
        graphics.fill(panelX - 2, panelY - 2, panelX + panelWidth + 2, bottom + 2, 0xFF101622);
        graphics.fill(panelX, panelY, panelX + panelWidth, panelY + 32, 0xFF202C41);
        graphics.fill(panelX, panelY + 31, panelX + panelWidth, panelY + 33, 0xFF50C8ED);
        graphics.drawString(this.font, "PORTALFLIGHT", panelX + 11, panelY + 11, 0xFFECF5FF);
        graphics.drawString(this.font, "RIGHT CTRL  /  ESC TO CLOSE", panelX + 10, bottom - 19, 0xFF9CABBE);
        graphics.drawString(this.font, CATEGORIES[category].toUpperCase(), panelX + 12, panelY + 61, 0xFF7FD9F7);
        super.render(graphics, mouseX, mouseY, partialTick);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
