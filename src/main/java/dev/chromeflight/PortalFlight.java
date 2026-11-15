package dev.chromeflight;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import org.lwjgl.glfw.GLFW;

public final class PortalFlight implements ClientModInitializer {
    private static KeyMapping speedKey, boatKey, portalKey;
    private static boolean speed, boatFly, portalScan;
    private static int scanTick;
    private static BlockPos nearestFrame;
    private static final double SPEED_FACTOR = 1.8;

    @Override public void onInitializeClient() {
        // KeyMapping.Category constructor details vary by mappings; use the vanilla miscellaneous category.
        speedKey = KeyBindingHelper.registerKeyBinding(new KeyMapping("key.portalflight.speed", GLFW.GLFW_KEY_V, KeyMapping.Category.MISC));
        boatKey = KeyBindingHelper.registerKeyBinding(new KeyMapping("key.portalflight.boat", GLFW.GLFW_KEY_B, KeyMapping.Category.MISC));
        portalKey = KeyBindingHelper.registerKeyBinding(new KeyMapping("key.portalflight.portal", GLFW.GLFW_KEY_N, KeyMapping.Category.MISC));
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (speedKey.consumeClick()) { speed = !speed; message(client, "Speed " + (speed ? "ON" : "OFF")); }
            while (boatKey.consumeClick()) { boatFly = !boatFly; message(client, "Boat Fly " + (boatFly ? "ON" : "OFF")); }
            while (portalKey.consumeClick()) { portalScan = !portalScan; nearestFrame = null; message(client, "Portal locator " + (portalScan ? "ON" : "OFF")); }
            LocalPlayer player = client.player;
            if (player == null || client.level == null) return;
            if (speed && player.onGround() && player.input != null && player.input.hasForwardImpulse()) {
                Vec3 v = player.getDeltaMovement();
                player.setDeltaMovement(v.x * SPEED_FACTOR, v.y, v.z * SPEED_FACTOR);
            }
            if (boatFly && player.getVehicle() != null && player.getVehicle().getType().toString().contains("boat")) {
                var vehicle = player.getVehicle();
                Vec3 v = vehicle.getDeltaMovement();
                double up = client.options.keyJump.isDown() ? 0.35 : (client.options.keyShift.isDown() ? -0.25 : 0.04);
                vehicle.setDeltaMovement(v.x, up, v.z);
            }
            if (portalScan && ++scanTick % 30 == 0) {
                BlockPos center = player.blockPosition();
                BlockPos best = null;
                double distance = Double.MAX_VALUE;
                // Loaded blocks only; no chunk loading, server requests or hidden-world access.
                for (int dx = -24; dx <= 24; dx++) for (int dy = -16; dy <= 16; dy++) for (int dz = -24; dz <= 24; dz++) {
                    BlockPos pos = center.offset(dx, dy, dz);
                    if (!client.level.hasChunkAt(pos)) continue;
                    if (!client.level.getBlockState(pos).is(Blocks.END_PORTAL_FRAME)) continue;
                    double d = pos.distSqr(center);
                    if (d < distance) { distance = d; best = pos.immutable(); }
                }
                nearestFrame = best;
                if (best != null) player.displayClientMessage(Component.literal("§dPortal frame: " + best.getX() + ", " + best.getY() + ", " + best.getZ() + " §7(" + (int)Math.sqrt(distance) + "m)"), true);
            }
        });
    }
    private static void message(Minecraft client, String text) {
        if (client.player != null) client.player.displayClientMessage(Component.literal("§b[PortalFlight] §f" + text), true);
    }
}
