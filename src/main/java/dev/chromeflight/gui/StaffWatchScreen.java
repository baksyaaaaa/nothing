package dev.chromeflight.gui;

import dev.chromeflight.PortalFlight;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import java.util.List;

/**
 * Shows client-visible observations only. Private commands and vanished staff
 * are not inherently visible to a Minecraft client.
 */
public final class StaffWatchScreen extends Screen {
    private int panelX;
    private int panelY;
    private int panelWidth;

    public StaffWatchScreen() {
        super(Component.literal("PortalFlight Staff Watch"));
    }

    @Override
    protected void init() {
        panelWidth = Math.min(460, this.width - 16);
        panelX = (this.width - panelWidth) / 2;
        panelY = Math.max(10, (this.height - 250) / 2);
        addRenderableWidget(Button.builder(Component.literal("Back to PortalFlight"), btn -> {
            if (this.minecraft != null) this.minecraft.setScreen(new PortalFlightMenuScreen());
        }).bounds(panelX + 10, panelY + 212, panelWidth - 20, 22).build());
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        graphics.fill(0, 0, this.width, this.height, 0xAA000000);
        graphics.fill(panelX - 2, panelY - 2, panelX + panelWidth + 2, panelY + 250, 0xFF101622);
        graphics.fill(panelX, panelY, panelX + panelWidth, panelY + 31, 0xFF202C41);
        graphics.drawString(this.font, "PORTALFLIGHT | STAFF WATCH", panelX + 10, panelY + 11, 0xFF81D4FA);
        graphics.drawString(this.font, "Status: " + shorten(PortalFlight.getStaffWatchStatus(), 65),
                panelX + 10, panelY + 42, 0xFFFFC67E);
        graphics.drawString(this.font, "Recent observations (current world):", panelX + 10, panelY + 65, 0xFFE6F0FA);
        List<String> entries = PortalFlight.getStaffWatchEvents();
        if (entries.isEmpty()) {
            graphics.drawString(this.font, "No client-visible observations yet.", panelX + 10, panelY + 84, 0xFF9CACBC);
        } else {
            for (int i = 0; i < Math.min(8, entries.size()); i++) {
                graphics.drawString(this.font, shorten(entries.get(i), 69),
                        panelX + 10, panelY + 84 + i * 15, 0xFFD1E4F5);
            }
        }
        graphics.drawString(this.font, "Cannot see private /vanish commands or fully hidden staff.",
                panelX + 10, panelY + 195, 0xFFFFA7A7);
        super.render(graphics, mouseX, mouseY, partialTick);
    }

    private static String shorten(String text, int limit) {
        return text.length() <= limit ? text : text.substring(0, limit - 3) + "...";
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
