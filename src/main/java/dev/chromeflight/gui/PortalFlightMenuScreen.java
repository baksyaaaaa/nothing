package dev.chromeflight.gui;

import dev.chromeflight.PortalFlight;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Original Meteor-inspired, six-category PortalFlight menu.
 * Category names and search/navigation follow the same organizational ideas,
 * but the UI and gameplay modules are PortalFlight implementations.
 */
public final class PortalFlightMenuScreen extends Screen {
    private static final String[] CATEGORIES =
            {"Combat", "Movement", "Render", "Player", "World", "Misc"};
    private static final String[][] IDS = {
            {"orbit", "orbitmobs", "autototem", "autoleave"},
            {"fly", "speed", "boat", "swim", "noclip", "sprint",
             "autowalk", "autojump", "highjump", "fastfall", "waterwalk",
             "autosneak", "noslow", "elytraboost", "nofall"},
            {"freecam", "players", "mobs", "tracers", "storage"},
            {"hunger", "privacy"},
            {"portal"},
            {}
    };
    private static final String[][] NAMES = {
            {"Orbit Combat", "Target Mobs", "AutoTotem", "AutoLeave"},
            {"Fly", "Speed", "Boat Fly", "Swim Boost", "Noclip", "Auto Sprint",
             "AutoWalk", "AutoJump", "HighJump", "FastFall", "WaterWalk",
             "AutoSneak", "NoSlow", "Elytra Boost", "NoFall"},
            {"Freecam", "Player ESP", "Mob ESP", "Tracers", "Storage ESP"},
            {"Hunger Saver", "Spectator Privacy"},
            {"Portal Locator"},
            {}
    };
    private static final int PER_PAGE = 8;
    private final int category;
    private final int page;
    private final String filter;
    private int panelX;
    private int panelY;
    private int panelWidth;
    private int panelHeight;
    private int moduleTitleY;
    private int visibleCount;
    private EditBox searchBox;

    public PortalFlightMenuScreen() {
        this(1, 0, "");
    }

    private PortalFlightMenuScreen(int category, int page, String filter) {
        super(Component.literal("PortalFlight Modules"));
        this.category = category;
        this.page = page;
        this.filter = filter;
    }

    @Override
    protected void init() {
        // Fit the available scaled GUI dimensions instead of assuming a
        // 378-pixel-tall Minecraft window.
        panelWidth = Math.min(480, Math.max(180, this.width - 8));
        panelHeight = Math.min(378, Math.max(145, this.height - 8));
        panelX = (this.width - panelWidth) / 2;
        panelY = (this.height - panelHeight) / 2;
        int inner = panelWidth - 16;
        int top = panelY + 27;
        int categoryColumns = inner >= 440 ? 6 : 3;
        int categoryRows = (CATEGORIES.length + categoryColumns - 1) / categoryColumns;
        int categoryWidth = (inner - (categoryColumns - 1) * 3) / categoryColumns;
        int categoryHeight = 19;
        for (int i = 0; i < CATEGORIES.length; i++) {
            final int choice = i;
            int col = i % categoryColumns;
            int row = i / categoryColumns;
            addRenderableWidget(Button.builder(
                    Component.literal((category == i ? "● " : "") + CATEGORIES[i]), btn -> {
                if (this.minecraft != null) {
                    this.minecraft.setScreen(new PortalFlightMenuScreen(
                            choice, 0, searchBox == null ? filter : searchBox.getValue().trim()));
                }
            }).bounds(panelX + 8 + col * (categoryWidth + 3),
                    top + row * 21, categoryWidth, categoryHeight).build());
        }

        int searchY = top + categoryRows * 21 + 2;
        searchBox = new EditBox(this.font, panelX + 8, searchY,
                inner - 54, 18, Component.literal("Module search"));
        searchBox.setMaxLength(48);
        searchBox.setValue(filter);
        addRenderableWidget(searchBox);
        addRenderableWidget(Button.builder(Component.literal("Find"), btn -> {
            if (this.minecraft != null) this.minecraft.setScreen(
                    new PortalFlightMenuScreen(category, 0, searchBox.getValue().trim()));
        }).bounds(panelX + inner - 42, searchY, 42, 18).build());

        moduleTitleY = searchY + 23;
        int moduleStart = moduleTitleY + 12;
        int footerY = panelY + panelHeight - 15;
        int actionCount = category == 1 ? 2 : (category == 0 || category == 5 ? 1 : 0);
        int actionHeight = 19;
        int actionGap = 3;
        int actionStart = footerY - 5 - actionCount * (actionHeight + actionGap);
        int pagingY = actionStart - 22;
        int availableRows = Math.max(1, (pagingY - moduleStart - 2) / 23);
        int columns = inner >= 230 ? 2 : 1;
        visibleCount = Math.max(1, Math.min(PER_PAGE, availableRows * columns));

        List<Integer> matches = matchesForCategory();
        int pages = Math.max(1, (matches.size() + visibleCount - 1) / visibleCount);
        int visiblePage = Math.max(0, Math.min(page, pages - 1));
        int buttonWidth = (inner - (columns - 1) * 4) / columns;
        for (int index = visiblePage * visibleCount;
                index < Math.min(matches.size(), (visiblePage + 1) * visibleCount); index++) {
            int idIndex = matches.get(index);
            String id = IDS[category][idIndex];
            String name = NAMES[category][idIndex];
            int visible = index - visiblePage * visibleCount;
            int x = panelX + 8 + (visible % columns) * (buttonWidth + 4);
            int y = moduleStart + (visible / columns) * 23;
            addRenderableWidget(Button.builder(moduleLabel(id, name), btn -> {
                PortalFlight.toggleModule(id);
                btn.setMessage(moduleLabel(id, name));
            }).bounds(x, y, buttonWidth, 20).build());
        }

        Button previous = Button.builder(Component.literal("< Prev"), btn -> {
            navigate(category, visiblePage - 1);
        }).bounds(panelX + 8, pagingY, Math.min(70, inner / 3), 18).build();
        previous.active = visiblePage > 0;
        addRenderableWidget(previous);
        Button next = Button.builder(Component.literal("Next >"), btn -> {
            navigate(category, visiblePage + 1);
        }).bounds(panelX + 8 + inner - Math.min(70, inner / 3), pagingY,
                Math.min(70, inner / 3), 18).build();
        next.active = visiblePage + 1 < pages;
        addRenderableWidget(next);

        if (category == 1) {
            addRenderableWidget(Button.builder(Component.literal("Coordinate Flight / TP Settings"),
                    btn -> {
                        if (this.minecraft != null) this.minecraft.setScreen(
                                new CoordinateFlightScreen());
                    }).bounds(panelX + 8, actionStart, inner, actionHeight).build());
            addRenderableWidget(Button.builder(Component.literal("Fly to Player"),
                    btn -> {
                        if (this.minecraft != null) this.minecraft.setScreen(
                                new PlayerTravelScreen());
                    }).bounds(panelX + 8, actionStart + actionHeight + actionGap,
                            inner, actionHeight).build());
        } else if (category == 0) {
            addRenderableWidget(Button.builder(autoLeaveThresholdLabel(), btn -> {
                PortalFlight.toggleAutoLeaveThreshold();
                btn.setMessage(autoLeaveThresholdLabel());
            }).bounds(panelX + 8, actionStart, inner, actionHeight).build());
        } else if (category == 5) {
            addRenderableWidget(Button.builder(Component.literal("Staff Watch"),
                    btn -> {
                        if (this.minecraft != null) this.minecraft.setScreen(
                                new StaffWatchScreen());
                    }).bounds(panelX + 8, actionStart, inner, actionHeight).build());
        }
    }

    private List<Integer> matchesForCategory() {
        List<Integer> matches = new ArrayList<>();
        String query = filter.toLowerCase(Locale.ROOT);
        for (int i = 0; i < IDS[category].length; i++) {
            if (query.isEmpty() || NAMES[category][i].toLowerCase(Locale.ROOT).contains(query)
                    || IDS[category][i].contains(query)) {
                matches.add(i);
            }
        }
        return matches;
    }

    private void navigate(int newCategory, int newPage) {
        if (this.minecraft != null) this.minecraft.setScreen(
                new PortalFlightMenuScreen(newCategory, newPage,
                        searchBox == null ? filter : searchBox.getValue().trim()));
    }

    private static Component moduleLabel(String id, String name) {
        return Component.literal((PortalFlight.isModuleEnabled(id) ? "[ON] " : "[OFF] ") + name);
    }

    private static Component autoLeaveThresholdLabel() {
        return Component.literal(PortalFlight.getAutoLeaveThreshold() == 2.0f
                ? "AutoLeave limit: 1 heart (2 HP)"
                : "AutoLeave limit: half a heart (1 HP)");
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        int bottom = panelY + panelHeight;
        graphics.fill(0, 0, this.width, this.height, 0xB4000000);
        graphics.fill(panelX - 1, panelY - 1,
                panelX + panelWidth + 1, bottom + 1, 0xFF10151F);
        graphics.fill(panelX, panelY, panelX + panelWidth, panelY + 23, 0xFF1E2939);
        graphics.fill(panelX, panelY + 22, panelX + panelWidth,
                panelY + 24, 0xFF4BBED9);
        graphics.drawString(this.font, "PORTALFLIGHT / MODULES",
                panelX + 8, panelY + 8, 0xFFEAF9FF);
        graphics.drawString(this.font, CATEGORIES[category].toUpperCase()
                        + " | " + matchesForCategory().size() + " modules",
                panelX + 8, moduleTitleY, 0xFF8FCADC);
        graphics.drawString(this.font, "ESC closes",
                panelX + 8, bottom - 12, 0xFFA5B9C9);
        super.render(graphics, mouseX, mouseY, partialTick);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
