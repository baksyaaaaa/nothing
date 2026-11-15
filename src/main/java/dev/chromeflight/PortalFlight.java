package dev.chromeflight;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderEvents;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import net.minecraft.world.phys.Vec3;
import org.lwjgl.glfw.GLFW;

public final class PortalFlight implements ClientModInitializer {
    private static KeyMapping speedKey, boatKey, portalKey, freecamKey, flyKey, noFallKey, hungerKey, privacyKey, swimKey, boatDescendKey, playerEspKey, mobEspKey, storageFinderKey, autoSprintKey, tracerKey;
    private static boolean speed, boatFly, portalScan, fly, noFall = true, hungerSaver, privacyMode = true, spectatorNearby, swimBoost, playerEsp, mobEsp, storageFinder, autoSprint, espTracers = true;
    public static boolean freecam;
    public static Vec3 freecamPosition = Vec3.ZERO;
    private static final double FREECAM_SPEED = 0.75;
    private static final double FLY_SPEED = 0.40;
    private static final double SPRINT_FLY_SPEED = 1.20;
    private static final double SWIM_BOOST_SPEED = 1.65;
    private static int scanTick;
    private static int espTick;
    private static int storageTick;
    private static final Set<UUID> glowingByMod = new HashSet<>();
    private static int spectatorCheckTick;
    private static BlockPos nearestFrame;
    private static final double SPEED_FACTOR = 1.8;

    @Override public void onInitializeClient() {
        // KeyMapping.Category constructor details vary by mappings; use the vanilla miscellaneous category.
        speedKey = KeyBindingHelper.registerKeyBinding(new KeyMapping("key.portalflight.speed", GLFW.GLFW_KEY_V, KeyMapping.Category.MISC));
        boatKey = KeyBindingHelper.registerKeyBinding(new KeyMapping("key.portalflight.boat", GLFW.GLFW_KEY_B, KeyMapping.Category.MISC));
        portalKey = KeyBindingHelper.registerKeyBinding(new KeyMapping("key.portalflight.portal", GLFW.GLFW_KEY_N, KeyMapping.Category.MISC));
        flyKey = KeyBindingHelper.registerKeyBinding(new KeyMapping("key.portalflight.fly", GLFW.GLFW_KEY_G, KeyMapping.Category.MISC));
        noFallKey = KeyBindingHelper.registerKeyBinding(new KeyMapping("key.portalflight.nofall", GLFW.GLFW_KEY_J, KeyMapping.Category.MISC));
        hungerKey = KeyBindingHelper.registerKeyBinding(new KeyMapping("key.portalflight.hunger", GLFW.GLFW_KEY_H, KeyMapping.Category.MISC));
        freecamKey = KeyBindingHelper.registerKeyBinding(new KeyMapping("key.portalflight.freecam", GLFW.GLFW_KEY_F6, KeyMapping.Category.MISC));
        privacyKey = KeyBindingHelper.registerKeyBinding(new KeyMapping("key.portalflight.privacy", GLFW.GLFW_KEY_P, KeyMapping.Category.MISC));
        swimKey = KeyBindingHelper.registerKeyBinding(new KeyMapping("key.portalflight.swim", GLFW.GLFW_KEY_R, KeyMapping.Category.MISC));
        boatDescendKey = KeyBindingHelper.registerKeyBinding(new KeyMapping("key.portalflight.boat_descend", GLFW.GLFW_KEY_X, KeyMapping.Category.MISC));
        playerEspKey = KeyBindingHelper.registerKeyBinding(new KeyMapping("key.portalflight.player_esp", GLFW.GLFW_KEY_F7, KeyMapping.Category.MISC));
        mobEspKey = KeyBindingHelper.registerKeyBinding(new KeyMapping("key.portalflight.mob_esp", GLFW.GLFW_KEY_F8, KeyMapping.Category.MISC));
        storageFinderKey = KeyBindingHelper.registerKeyBinding(new KeyMapping("key.portalflight.storage_finder", GLFW.GLFW_KEY_F9, KeyMapping.Category.MISC));
        autoSprintKey = KeyBindingHelper.registerKeyBinding(new KeyMapping("key.portalflight.auto_sprint", GLFW.GLFW_KEY_F10, KeyMapping.Category.MISC));
        tracerKey = KeyBindingHelper.registerKeyBinding(new KeyMapping("key.portalflight.tracers", GLFW.GLFW_KEY_F11, KeyMapping.Category.MISC));
        // Tracers follow Player/Mob ESP only for entities the server actually sends.
        WorldRenderEvents.BEFORE_DEBUG_RENDER.register(context -> {
            if (!espTracers || (!playerEsp && !mobEsp)) return;
            Minecraft mc = Minecraft.getInstance();
            if (mc.level == null || mc.player == null || context.consumers() == null) return;
            Vec3 camera = mc.gameRenderer.getMainCamera().getPosition();
            var lines = context.consumers().getBuffer(RenderType.lines());
            for (var entity : mc.level.entitiesForRendering()) {
                if (entity == mc.player || !(entity instanceof LivingEntity)) continue;
                boolean selected = entity instanceof Player ? playerEsp : mobEsp;
                if (!selected || entity.distanceToSqr(mc.player) >= 96.0 * 96.0) continue;
                Vec3 end = entity.position().add(0, entity.getBbHeight() * 0.5, 0).subtract(camera);
                // Start at the camera, end at the entity outline. Player = cyan, mob = orange.
                boolean isPlayer = entity instanceof Player;
                int red = isPlayer ? 50 : 255;
                int green = isPlayer ? 225 : 170;
                int blue = isPlayer ? 255 : 50;
                lines.addVertex(0.0f, 0.0f, 0.0f).setColor(red, green, blue, 255).setNormal(0, 1, 0);
                lines.addVertex((float) end.x, (float) end.y, (float) end.z).setColor(red, green, blue, 255).setNormal(0, 1, 0);
            }
        });
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (tracerKey.consumeClick()) { espTracers = !espTracers; message(client, "ESP Tracers " + (espTracers ? "ON" : "OFF")); }
            while (playerEspKey.consumeClick()) { playerEsp = !playerEsp; message(client, "Player ESP " + (playerEsp ? "ON" : "OFF")); }
            while (mobEspKey.consumeClick()) { mobEsp = !mobEsp; message(client, "Mob ESP " + (mobEsp ? "ON" : "OFF")); }
            while (storageFinderKey.consumeClick()) { storageFinder = !storageFinder; message(client, "Storage Finder " + (storageFinder ? "ON" : "OFF")); }
            while (autoSprintKey.consumeClick()) { autoSprint = !autoSprint; message(client, "Auto Sprint " + (autoSprint ? "ON" : "OFF")); }
            while (swimKey.consumeClick()) { swimBoost = !swimBoost; message(client, "Swim Boost " + (swimBoost ? "ON" : "OFF")); }
            while (privacyKey.consumeClick()) { privacyMode = !privacyMode; message(client, "Spectator Privacy " + (privacyMode ? "ON" : "OFF")); }
            while (noFallKey.consumeClick()) { noFall = !noFall; message(client, "Safe Fall " + (noFall ? "ON" : "OFF")); }
            while (hungerKey.consumeClick()) { hungerSaver = !hungerSaver; message(client, "Hunger Saver (no sprint restriction) " + (hungerSaver ? "ON" : "OFF")); }
            while (flyKey.consumeClick()) { fly = !fly; message(client, "Fly " + (fly ? "ON" : "OFF")); }
            while (speedKey.consumeClick()) { speed = !speed; message(client, "Speed " + (speed ? "ON" : "OFF")); }
            while (boatKey.consumeClick()) { boatFly = !boatFly; message(client, "Boat Fly " + (boatFly ? "ON" : "OFF")); }
            while (portalKey.consumeClick()) { portalScan = !portalScan; nearestFrame = null; message(client, "Portal locator " + (portalScan ? "ON" : "OFF")); }
            LocalPlayer player = client.player;
            while (freecamKey.consumeClick()) {
                freecam = !freecam;
                if (freecam && player != null) freecamPosition = player.getEyePosition();
                message(client, "Freecam " + (freecam ? "ON" : "OFF"));
            }
            if (player == null || client.level == null) { spectatorNearby = false; glowingByMod.clear(); return; }
            if (autoSprint && !freecam && !player.isShiftKeyDown() && player.getVehicle() == null && client.options.keyUp.isDown()) {
                player.setSprinting(true);
            }
            // Local glowing outlines for visible-to-client entities only. Restore our own changes.
            if (++espTick % 5 == 0) {
                Set<UUID> stillGlowing = new HashSet<>();
                for (var entity : client.level.entitiesForRendering()) {
                    if (entity == player || !(entity instanceof LivingEntity)) continue;
                    boolean selected = entity instanceof Player ? playerEsp : mobEsp;
                    UUID id = entity.getUUID();
                    if (selected && entity.distanceToSqr(player) < 96.0 * 96.0) {
                        if (!entity.hasGlowingTag()) {
                            entity.setGlowingTag(true);
                            glowingByMod.add(id);
                        }
                        if (glowingByMod.contains(id)) stillGlowing.add(id);
                    } else if (glowingByMod.contains(id)) {
                        entity.setGlowingTag(false);
                    }
                }
                glowingByMod.retainAll(stillGlowing);
            }
            // Coordinate-based storage finder for blocks in already-loaded chunks.
            // This is not through-wall block ESP.
            if (storageFinder && ++storageTick % 40 == 0) {
                BlockPos origin = player.blockPosition();
                BlockPos nearest = null;
                double bestDistance = Double.MAX_VALUE;
                for (int dx = -16; dx <= 16; dx++) for (int dy = -8; dy <= 8; dy++) for (int dz = -16; dz <= 16; dz++) {
                    BlockPos pos = origin.offset(dx, dy, dz);
                    if (!client.level.hasChunkAt(pos)) continue;
                    var state = client.level.getBlockState(pos);
                    if (!state.is(Blocks.CHEST) && !state.is(Blocks.TRAPPED_CHEST) && !state.is(Blocks.BARREL)) continue;
                    double d = pos.distSqr(origin);
                    if (d < bestDistance) { bestDistance = d; nearest = pos.immutable(); }
                }
                if (nearest != null)
                    player.displayClientMessage(Component.literal("§6Storage: " + nearest.getX() + ", " + nearest.getY() + ", " + nearest.getZ()), true);
            }
            // Only spectator players whose entities the server sends can be detected.
            // Hidden moderators or remote spectator cameras cannot be discovered client-side.
            if (++spectatorCheckTick % 20 == 0) {
                boolean found = false;
                for (var other : client.level.players()) {
                    if (other != player && other.isSpectator() && other.distanceToSqr(player) <= 48.0 * 48.0) {
                        found = true;
                        break;
                    }
                }
                if (found != spectatorNearby) {
                    spectatorNearby = found;
                    message(client, found ? "Spectator detected nearby (if visible to client)!" : "No nearby visible spectators detected");
                }
                if (found && privacyMode) {
                    if (fly || speed || boatFly || freecam || noFall) {
                        fly = false; speed = false; boatFly = false; freecam = false; noFall = false;
                        message(client, "Privacy: movement modules and Safe Fall disabled");
                    }
                }
            }
            if (noFall) {
                // Local fall-distance reset and an explicit grounded-status packet during falls.
                // Servers may still calculate fall damage from authoritative movement history.
                boolean falling = !player.onGround() && player.getDeltaMovement().y < -0.10;
                if (falling && player.getVehicle() == null && !freecam && client.getConnection() != null) {
                    client.getConnection().send(new ServerboundMovePlayerPacket.StatusOnly(true, false));
                }
                player.fallDistance = 0;
            }
            // Hunger Saver intentionally does not interfere with sprinting.
            if (freecam) {
                boolean forward = client.options.keyUp.isDown();
                boolean back = client.options.keyDown.isDown();
                boolean left = client.options.keyLeft.isDown();
                boolean right = client.options.keyRight.isDown();
                boolean up = client.options.keyJump.isDown();
                boolean down = client.options.keyShift.isDown();
                double yaw = Math.toRadians(player.getYRot());
                Vec3 forwardVec = new Vec3(-Math.sin(yaw), 0, Math.cos(yaw));
                Vec3 rightVec = new Vec3(-forwardVec.z, 0, forwardVec.x);
                Vec3 movement = forwardVec.scale((forward ? 1 : 0) - (back ? 1 : 0))
                    .add(rightVec.scale((right ? 1 : 0) - (left ? 1 : 0)))
                    .add(0, (up ? 1 : 0) - (down ? 1 : 0), 0);
                if (movement.lengthSqr() > 0) freecamPosition = freecamPosition.add(movement.normalize().scale(FREECAM_SPEED));
                client.options.keyUp.setDown(false);
                client.options.keyDown.setDown(false);
                client.options.keyLeft.setDown(false);
                client.options.keyRight.setDown(false);
                client.options.keyJump.setDown(false);
                client.options.keyShift.setDown(false);
                player.setDeltaMovement(Vec3.ZERO);
            }
            if (!freecam && swimBoost && !fly && player.getVehicle() == null && player.isInWater()) {
                // Move in the direction of the player's look vector while swimming.
                // Use Minecraft's rebound movement keys; avoid hardcoded WASD / Space / Shift.
                double forwardInput = (client.options.keyUp.isDown() ? 1 : 0) - (client.options.keyDown.isDown() ? 1 : 0);
                double sidewaysInput = (client.options.keyRight.isDown() ? 1 : 0) - (client.options.keyLeft.isDown() ? 1 : 0);
                double yaw = Math.toRadians(player.getYRot());
                Vec3 forwardVec = new Vec3(-Math.sin(yaw), 0, Math.cos(yaw));
                Vec3 rightVec = new Vec3(-forwardVec.z, 0, forwardVec.x);
                Vec3 swimDirection = forwardVec.scale(forwardInput).add(rightVec.scale(sidewaysInput));
                double vertical = (client.options.keyJump.isDown() ? 1 : 0) - (client.options.keyShift.isDown() ? 1 : 0);
                swimDirection = swimDirection.add(0, vertical, 0);
                if (swimDirection.lengthSqr() > 0) {
                    Vec3 velocity = swimDirection.normalize().scale(SWIM_BOOST_SPEED);
                    player.setDeltaMovement(velocity);
                }
            }
            if (!freecam && fly && player.getVehicle() == null) {
                double yaw = Math.toRadians(player.getYRot());
                Vec3 forwardVec = new Vec3(-Math.sin(yaw), 0, Math.cos(yaw));
                Vec3 rightVec = new Vec3(-forwardVec.z, 0, forwardVec.x);
                double forwardInput = (client.options.keyUp.isDown() ? 1 : 0) - (client.options.keyDown.isDown() ? 1 : 0);
                double sidewaysInput = (client.options.keyRight.isDown() ? 1 : 0) - (client.options.keyLeft.isDown() ? 1 : 0);
                double verticalInput = (client.options.keyJump.isDown() ? 1 : 0) - (client.options.keyShift.isDown() ? 1 : 0);
                boolean sprinting = client.options.keySprint.isDown() || player.isSprinting();
                double flySpeed = sprinting ? SPRINT_FLY_SPEED : FLY_SPEED;
                Vec3 movement = forwardVec.scale(forwardInput).add(rightVec.scale(sidewaysInput)).add(0, verticalInput, 0);
                if (movement.lengthSqr() > 0) movement = movement.normalize().scale(flySpeed);
                player.setDeltaMovement(movement);
                player.setOnGround(false);
                player.fallDistance = 0;
            }
            if (!freecam && speed && player.onGround() && player.input != null && player.input.hasForwardImpulse()) {
                Vec3 v = player.getDeltaMovement();
                player.setDeltaMovement(v.x * SPEED_FACTOR, v.y, v.z * SPEED_FACTOR);
            }
            if (boatFly && player.getVehicle() != null && player.getVehicle().getType().toString().contains("boat")) {
                var vehicle = player.getVehicle();
                Vec3 v = vehicle.getDeltaMovement();
                double up = client.options.keyJump.isDown() ? 0.35 : (boatDescendKey.isDown() ? -0.25 : 0.04);
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
