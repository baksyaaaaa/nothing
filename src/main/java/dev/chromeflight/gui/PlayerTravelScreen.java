package dev.chromeflight.gui;

import dev.chromeflight.PortalFlight;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.phys.Vec3;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

/**
 * Lists online names received from the tab list, including players who are not
 * rendered nearby. Coordinates are only available for previously observed players.
 * No /tp, permissions, hidden-location lookup, or fabricated positions.
 */
public final class PlayerTravelScreen extends Screen {
    private record Target(UUID id, String name) { }

    private final int page;
    private final List<Target> targets = new ArrayList<>();
    private int left;
    private int top;
    private String info = "";

    public PlayerTravelScreen() {
        this(0);
    }

    private PlayerTravelScreen(int page) {
        super(Component.literal("PortalFlight Player Travel"));
        this.page = page;
    }

    @Override
    protected void init() {
        left = (width - 344) / 2;
        top = Math.max(10, (height - 283) / 2);
        targets.clear();

        if (minecraft != null && minecraft.getConnection() != null && minecraft.player != null) {
            for (var entry : minecraft.getConnection().getOnlinePlayers()) {
                var profile = entry.getProfile();
                if (!profile.getId().equals(minecraft.player.getUUID())) {
                    targets.add(new Target(profile.getId(), profile.getName()));
                }
            }
        }
        targets.sort(Comparator.comparing(t -> t.name().toLowerCase(java.util.Locale.ROOT)));

        int start = page * 6;
        for (int i = start; i < Math.min(start + 6, targets.size()); ++i) {
            Target target = targets.get(i);
            Vec3 cached = PortalFlight.getLastSeenPlayerPosition(target.id());
            int y = top + 64 + (i - start) * 29;
            Button button = Button.builder(Component.literal(
                    target.name() + (cached == null ? " - coords unknown" : " - Fly to last known XYZ")),
                    ignored -> {
                        Vec3 destination = PortalFlight.getLastSeenPlayerPosition(target.id());
                        if (destination == null) {
                            info = "No coordinates received for " + target.name();
                            return;
                        }
                        if (PortalFlight.startPlayerTravel(
                                destination.x, destination.y, destination.z)) {
                            if (minecraft != null) minecraft.setScreen(null);
                        } else {
                            info = "Could not begin flight.";
                        }
                    }).bounds(left + 12, y, 320, 23).build();
            button.active = cached != null;
            addRenderableWidget(button);
        }

        Button previous = Button.builder(Component.literal("Previous"), button -> {
            if (minecraft != null) minecraft.setScreen(new PlayerTravelScreen(page - 1));
        }).bounds(left + 12, top + 240, 101, 21).build();
        previous.active = page > 0;
        addRenderableWidget(previous);

        Button next = Button.builder(Component.literal("Next"), button -> {
            if (minecraft != null) minecraft.setScreen(new PlayerTravelScreen(page + 1));
        }).bounds(left + 121, top + 240, 101, 21).build();
        next.active = (page + 1) * 6 < targets.size();
        addRenderableWidget(next);

        addRenderableWidget(Button.builder(Component.literal("Refresh"), button -> {
            if (minecraft != null) minecraft.setScreen(new PlayerTravelScreen(page));
        }).bounds(left + 231, top + 240, 101, 21).build());
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        graphics.fill(0, 0, width, height, 0x99000000);
        graphics.fill(left - 2, top - 2, left + 346, top + 285, 0xFF101622);
        graphics.fill(left, top, left + 344, top + 31, 0xFF202C41);
        graphics.drawString(font, "FLY TO PLAYER - UP TO 50K BLOCKS/S",
                left + 12, top + 11, 0xFF81D4FA);
        graphics.drawString(font, "Online names; fly only to known XYZ (Y=400 route)",
                left + 12, top + 38, 0xFFD0DEE9);
        if (targets.isEmpty()) {
            graphics.drawString(font, "No online players listed by the server.",
                    left + 12, top + 82, 0xFFFFCC88);
        }
        if (!info.isEmpty()) {
            graphics.drawString(font, info, left + 12, top + 223, 0xFFFFCC88);
        }
        super.render(graphics, mouseX, mouseY, partialTick);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
