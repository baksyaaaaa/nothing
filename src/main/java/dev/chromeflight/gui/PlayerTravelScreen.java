package dev.chromeflight.gui;

import dev.chromeflight.PortalFlight;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Permission-free movement toward the last known coordinates of players
 * currently sent to the client by the server. This is not teleportation.
 */
public final class PlayerTravelScreen extends Screen {
    private final int page;
    private final List<Player> targets = new ArrayList<>();
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
        if (minecraft != null && minecraft.level != null && minecraft.player != null) {
            for (Player other : minecraft.level.players()) {
                if (other != minecraft.player && !other.isRemoved()) targets.add(other);
            }
        }
        targets.sort(Comparator.comparing(p -> p.getName().getString().toLowerCase(java.util.Locale.ROOT)));
        int start = page * 6;
        for (int i = start; i < Math.min(start + 6, targets.size()); ++i) {
            Player other = targets.get(i);
            int y = top + 64 + (i - start) * 29;
            String name = other.getName().getString();
            addRenderableWidget(Button.builder(Component.literal("Fly to " + name), button -> {
                if (minecraft == null || minecraft.level == null || other.isRemoved()
                        || !minecraft.level.players().contains(other)) {
                    info = "Player is no longer visible to your client.";
                    return;
                }
                var pos = other.position();
                if (PortalFlight.startPlayerTravel(pos.x, pos.y, pos.z)) {
                    minecraft.setScreen(null);
                } else {
                    info = "Could not begin flight.";
                }
            }).bounds(left + 12, y, 320, 23).build());
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
        addRenderableWidget(Button.builder(Component.literal("Back"), button -> {
            if (minecraft != null) minecraft.setScreen(new PortalFlightMenuScreen());
        }).bounds(left + 231, top + 240, 101, 21).build());
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        graphics.fill(0, 0, width, height, 0x99000000);
        graphics.fill(left - 2, top - 2, left + 346, top + 285, 0xFF101622);
        graphics.fill(left, top, left + 344, top + 31, 0xFF202C41);
        graphics.drawString(font, "FLY TO PLAYER - UP TO 50K BLOCKS/S", left + 12, top + 11, 0xFF81D4FA);
        graphics.drawString(font, "Only players whose positions are client-visible", left + 12, top + 38, 0xFFD0DEE9);
        if (targets.isEmpty()) {
            graphics.drawString(font, "No nearby/loaded player positions known.", left + 12, top + 82, 0xFFFFCC88);
        }
        if (!info.isEmpty()) graphics.drawString(font, info, left + 12, top + 223, 0xFFFFCC88);
        super.render(graphics, mouseX, mouseY, partialTick);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
