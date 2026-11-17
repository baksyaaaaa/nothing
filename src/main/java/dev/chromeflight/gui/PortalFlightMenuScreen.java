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
            {"orbit", "autototem", "autoleave"},
            {"fly", "speed", "boat", "swim", "noclip", "sprint",
             "autowalk", "autojump", "highjump", "fastfall", "waterwalk",
             "autosneak", "noslow", "elytraboost", "nofall"},
            {"freecam", "players", "mobs", "tracers", "storage"},
            {"hunger", "privacy"},
            {"portal"},
            {}
    };
    private static final String[][] NAMES = {
            {"Orbit Combat", "AutoTotem", "AutoLeave"},
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
        panelWidth = Math.min(480, this.width - 20);
        panelX = (this.width - panelWidth) / 2;
        panelY = Math.max(5, (this.height - 378) / 2);
        int inner = panelWidth - 20;
        int columnWidth = (inner - 10) / 3;
        for (int i = 0; i < CATEGORIES.length; i++) {
            final int choice = i;
            int col = i % 3;
            int row = i / 3;
            addRenderableWidget(Button.builder(
                    Component.literal((category == i ? "● " : "") + CATEGORIES[i]), btn -> {
                if (this.minecraft != null) {
                    this.minecraft.setScreen(new PortalFlightMenuScreen(choice, 0, filter));
                }
            }).bounds(panelX + 10 + col * (columnWidth + 5),
                    panelY + 36 + row * 27, columnWidth, 23).build());
        }

        searchBox = new EditBox(this.font, panelX + 10, panelY + 96,
                inner - 76, 20, Component.literal("Module search"));
        searchBox.setMaxLength(48);
        searchBox.setValue(filter);
        addRenderableWidget(searchBox);
        addRenderableWidget(Button.builder(Component.literal("Find"), btn -> {
            if (this.minecraft != null) this.minecraft.setScreen(
                    new PortalFlightMenuScreen(category, 0, searchBox.getValue().trim()));
        }).bounds(panelX + inner - 60, panelY + 96, 60, 20).build());

        List<Integer> matches = matchesForCategory();
        int pages = Math.max(1, (matches.size() + PER_PAGE - 1) / PER_PAGE);
        int visiblePage = Math.min(page, pages - 1);
        for (int index = visiblePage * PER_PAGE;
                index < Math.min(matches.size(), (visiblePage + 1) * PER_PAGE); index++) {
            int idIndex = matches.get(index);
            String id = IDS[category][idIndex];
            String name = NAMES[category][idIndex];
            int visible = index - visiblePage * PER_PAGE;
            int buttonWidth = (inner - 8) / 2;
            int x = panelX + 10 + (visible % 2) * (buttonWidth + 8);
            int y = panelY + 132 + (visible / 2) * 29;
            addRenderableWidget(Button.builder(moduleLabel(id, name), btn -> {
                PortalFlight.toggleModule(id);
                btn.setMessage(moduleLabel(id, name));
            }).bounds(x, y, buttonWidth, 25).build());
        }

        Button previous = Button.builder(Component.literal("< Prev"), btn -> {
            navigate(category, visiblePage - 1);
        }).bounds(panelX + 10, panelY + 258, 80, 22).build();
        previous.active = visiblePage > 0;
        addRenderableWidget(previous);
        Button next = Button.builder(Component.literal("Next >"), btn -> {
            navigate(category, visiblePage + 1);
        }).bounds(panelX + inner - 80, panelY + 258, 80, 22).build();
        next.active = visiblePage + 1 < pages;
        addRenderableWidget(next);

        if (category == 1) {
            addRenderableWidget(Button.builder(Component.literal("Coordinate Flight / TP Settings"),
                    btn -> {
                        if (this.minecraft != null) this.minecraft.setScreen(
                                new CoordinateFlightScreen());
                    }).bounds(panelX + 10, panelY + 295, inner, 21).build());
            addRenderableWidget(Button.builder(Component.literal("Fly to Player"),
                    btn -> {
                        if (this.minecraft != null) this.minecraft.setScreen(
                                new PlayerTravelScreen());
                    }).bounds(panelX + 10, panelY + 321, inner, 21).build());
        } else if (category == 0) {
            addRenderableWidget(Button.builder(autoLeaveThresholdLabel(), btn -> {
                PortalFlight.toggleAutoLeaveThreshold();
                btn.setMessage(autoLeaveThresholdLabel());
            }).bounds(panelX + 10, panelY + 295, inner, 22).build());
        } else if (category == 5) {
            addRenderableWidget(Button.builder(Component.literal("Staff Watch"),
                    btn -> {
                        if (this.minecraft != null) this.minecraft.setScreen(
                                new StaffWatchScreen());
                    }).bounds(panelX + 10, panelY + 295, inner, 22).build());
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
        int bottom = panelY + 378;
        graphics.fill(0, 0, this.width, this.height, 0xB4000000);
        graphics.fill(panelX - 2, panelY - 2,
                panelX + panelWidth + 2, bottom + 2, 0xFF10151F);
        graphics.fill(panelX, panelY, panelX + panelWidth, panelY + 30, 0xFF1E2939);
        graphics.fill(panelX, panelY + 29, panelX + panelWidth,
                panelY + 31, 0xFF4BBED9);
        graphics.drawString(this.font, "PORTALFLIGHT  /  MODULES",
                panelX + 12, panelY + 11, 0xFFEAF9FF);
        graphics.drawString(this.font, CATEGORIES[category].toUpperCase()
                        + "    |    " + matchesForCategory().size() + " modules",
                panelX + 12, panelY + 122, 0xFF8FCADC);
        graphics.drawString(this.font, "ESC to close  •  Right Ctrl opens",
                panelX + 12, bottom - 17, 0xFFA5B9C9);
        super.render(graphics, mouseX, mouseY, partialTick);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
