package dev.chromeflight;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderEvents;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import com.mojang.blaze3d.vertex.VertexConsumer;
import org.joml.Matrix4f;
import net.minecraft.world.phys.AABB;
import net.minecraft.client.KeyMapping;
import dev.chromeflight.gui.PortalFlightMenuScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import java.util.UUID;
import net.minecraft.world.phys.Vec3;
import org.lwjgl.glfw.GLFW;

public final class PortalFlight implements ClientModInitializer {
    private static KeyMapping speedKey, boatKey, portalKey, freecamKey, flyKey, noFallKey, hungerKey, privacyKey, swimKey, boatDescendKey, playerEspKey, mobEspKey, storageFinderKey, autoSprintKey, tracerKey, orbitCombatKey, noclipKey, clickGuiKey;
    private static boolean speed, boatFly, portalScan, fly, noFall = true, hungerSaver, privacyMode = true, spectatorNearby, swimBoost, playerEsp, mobEsp, storageFinder, autoSprint, espTracers = true, orbitCombat, noclip;
    public static boolean freecam;
    public static Vec3 freecamPosition = Vec3.ZERO;
    private static final double FREECAM_SPEED = 0.75;
    private static final double FLY_SPEED = 0.40;
    private static final double SPRINT_FLY_SPEED = 1.20;
    private static final double SWIM_BOOST_SPEED = 1.65;
    private static int scanTick;
    private static int combatAttackTicks;
    private static UUID combatTargetId;
    private static int combatOrbitDirection = 1;
    private static final double COMBAT_RANGE = 8.0;
    private static final double ORBIT_SPEED = 0.32;
    private static final double ORBIT_RADIUS = 2.4;
    private static final double COMBAT_VERTICAL_SPEED = 0.27;
    private static final double NOCLIP_SPEED = 0.55;
    private static int storageTick;
    private static int spectatorCheckTick;
    private static BlockPos nearestFrame;
    private static final double SPEED_FACTOR = 1.8;
    // Client-side comfort limits; they do not override the server's vehicle simulation.
    private static final double BOAT_VERTICAL_ACCELERATION = 0.045;
    private static final double BOAT_ASCEND_SPEED = 0.30;
    private static final double BOAT_DESCEND_SPEED = -0.25;
    private static final double BOAT_CORRECTION_THRESHOLD = 4.5;
    private static int trackedBoatId = -1;
    private static Vec3 lastBoatPosition;
    private static Vec3 expectedBoatMovement = Vec3.ZERO;

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
        orbitCombatKey = KeyBindingHelper.registerKeyBinding(new KeyMapping("key.portalflight.orbit_combat", GLFW.GLFW_KEY_F12, KeyMapping.Category.MISC));
        noclipKey = KeyBindingHelper.registerKeyBinding(new KeyMapping("key.portalflight.noclip", GLFW.GLFW_KEY_C, KeyMapping.Category.MISC));
        clickGuiKey = KeyBindingHelper.registerKeyBinding(new KeyMapping("key.portalflight.clickgui", GLFW.GLFW_KEY_RIGHT_CONTROL, KeyMapping.Category.MISC));
        // Render outlines and optional tracers for entities the client already knows about.
        // WorldRenderContext matrices must be used when submitting vertices in 1.21.11.
        WorldRenderEvents.BEFORE_DEBUG_RENDER.register(context -> {
            if (!playerEsp && !mobEsp) return;
            Minecraft mc = Minecraft.getInstance();
            if (mc.level == null || mc.player == null || context.consumers() == null || context.matrices() == null) return;
            Vec3 camera = mc.gameRenderer.getMainCamera().position();
            Matrix4f pose = context.matrices().last().pose();
            VertexConsumer lines = context.consumers().getBuffer(RenderTypes.lines());
            Vec3 tracerStart = camera.add(mc.player.getLookAngle().scale(0.65));
            for (var entity : mc.level.entitiesForRendering()) {
                if (entity == mc.player || !(entity instanceof LivingEntity) || !entity.isAlive()) continue;
                boolean isPlayer = entity instanceof Player;
                if (!(isPlayer ? playerEsp : mobEsp)) continue;
                if (entity.distanceToSqr(mc.player) > 96.0 * 96.0) continue;
                int red = isPlayer ? 50 : 255;
                int green = isPlayer ? 225 : 170;
                int blue = isPlayer ? 255 : 50;
                // Bounding box is world-space; translate to camera-relative space.
                AABB bounds = entity.getBoundingBox().inflate(0.04).move(-camera.x, -camera.y, -camera.z);
                drawEspBox(lines, pose, bounds, red, green, blue);
                if (espTracers) {
                    Vec3 target = entity.getBoundingBox().getCenter().subtract(camera);
                    Vec3 from = tracerStart.subtract(camera);
                    drawEspLine(lines, pose, from.x, from.y, from.z,
                            target.x, target.y, target.z, red, green, blue);
                }
            }
        });
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (clickGuiKey.consumeClick()) {
                if (client.screen == null) client.setScreen(new PortalFlightMenuScreen());
            }
            while (noclipKey.consumeClick()) {
                noclip = !noclip;
                if (noclip) { fly = false; orbitCombat = false; freecam = false; }
                if (!noclip && client.player != null) {
                    client.player.noPhysics = false;
                    client.player.setNoGravity(false);
                }
                message(client, "Noclip " + (noclip ? "ON" : "OFF") + " (singleplayer/local only)");
            }
            while (orbitCombatKey.consumeClick()) {
                orbitCombat = !orbitCombat;
                combatAttackTicks = 0;
                combatTargetId = null;
                if (!orbitCombat && client.player != null && !noclip) client.player.setNoGravity(false);
                message(client, "Orbit Combat " + (orbitCombat ? "ON" : "OFF"));
            }
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
            while (boatKey.consumeClick()) {
                boatFly = !boatFly;
                resetBoatTracking();
                message(client, "Boat Fly " + (boatFly ? "ON" : "OFF"));
            }
            while (portalKey.consumeClick()) { portalScan = !portalScan; nearestFrame = null; message(client, "Portal locator " + (portalScan ? "ON" : "OFF")); }
            LocalPlayer player = client.player;
            while (freecamKey.consumeClick()) {
                freecam = !freecam;
                if (freecam && player != null) freecamPosition = player.getEyePosition();
                message(client, "Freecam " + (freecam ? "ON" : "OFF"));
            }
            if (player == null || client.level == null) {
                spectatorNearby = false; noclip = false; return;
            }
            if (noclip) {
                player.noPhysics = true;
                player.setNoGravity(true);
                player.fallDistance = 0;
                double yaw = Math.toRadians(player.getYRot());
                Vec3 forwardVec = new Vec3(-Math.sin(yaw), 0, Math.cos(yaw));
                Vec3 rightVec = new Vec3(-forwardVec.z, 0, forwardVec.x);
                double forward = (client.options.keyUp.isDown() ? 1 : 0) - (client.options.keyDown.isDown() ? 1 : 0);
                double sideways = (client.options.keyRight.isDown() ? 1 : 0) - (client.options.keyLeft.isDown() ? 1 : 0);
                double vertical = (client.options.keyJump.isDown() ? 1 : 0) - (client.options.keyShift.isDown() ? 1 : 0);
                Vec3 direction = forwardVec.scale(forward).add(rightVec.scale(sideways)).add(0, vertical, 0);
                player.setDeltaMovement(direction.lengthSqr() > 0 ? direction.normalize().scale(NOCLIP_SPEED) : Vec3.ZERO);
            }
            if (autoSprint && !noclip && !freecam && !player.isShiftKeyDown() && player.getVehicle() == null && client.options.keyUp.isDown()) {
                player.setSprinting(true);
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
            if (noFall && !orbitCombat) {
                // Local fall-distance reset and an explicit grounded-status packet during falls.
                // Servers may still calculate fall damage from authoritative movement history.
                boolean falling = !player.onGround() && player.getDeltaMovement().y < -0.10;
                if (falling && player.getVehicle() == null && !freecam && client.getConnection() != null) {
                    client.getConnection().send(new ServerboundMovePlayerPacket.StatusOnly(true, false));
                }
                player.fallDistance = 0;
            }
            // Orbit Combat follows its target with a flying circle and attacks frequently.
            // Server-side damage cooldowns and critical hit rules still apply.
            if (orbitCombat && !noclip && !freecam && !fly && player.getVehicle() == null
                    && client.gameMode != null && client.screen == null) {
                Player target = null;
                double maxRangeSqr = COMBAT_RANGE * COMBAT_RANGE;
                if (combatTargetId != null) {
                    for (Player other : client.level.players()) {
                        if (other.getUUID().equals(combatTargetId) && other != player
                                && other.isAlive() && !other.isSpectator()
                                && player.distanceToSqr(other) <= maxRangeSqr) {
                            target = other;
                            break;
                        }
                    }
                }
                if (target == null) {
                    double nearest = maxRangeSqr;
                    for (Player other : client.level.players()) {
                        if (other == player || !other.isAlive() || other.isSpectator()) continue;
                        double distance = player.distanceToSqr(other);
                        if (distance < nearest) { nearest = distance; target = other; }
                    }
                    UUID newId = target == null ? null : target.getUUID();
                    if (newId != null && !newId.equals(combatTargetId)) {
                        combatOrbitDirection = -combatOrbitDirection;
                    }
                    combatTargetId = newId;
                }
                if (target != null) {
                    combatAttackTicks++;
                    // Aim at the opponent while following them in the air.
                    Vec3 aim = target.getEyePosition().subtract(player.getEyePosition());
                    double flatAim = Math.sqrt(aim.x * aim.x + aim.z * aim.z);
                    if (flatAim > 0.0001) {
                        player.setYRot((float)Math.toDegrees(Math.atan2(-aim.x, aim.z)));
                        player.setXRot((float)-Math.toDegrees(Math.atan2(aim.y, flatAim)));
                    }

                    Vec3 delta = target.position().subtract(player.position());
                    double radius = Math.sqrt(delta.x * delta.x + delta.z * delta.z);
                    Vec3 horizontal = Vec3.ZERO;
                    if (radius > 0.001) {
                        Vec3 inward = new Vec3(delta.x / radius, 0, delta.z / radius);
                        Vec3 tangent = new Vec3(-inward.z, 0, inward.x).scale(combatOrbitDirection);
                        // Move around a moving target; close large gaps without teleporting.
                        double correction = Math.max(-0.22, Math.min(0.45, (radius - ORBIT_RADIUS) * 0.24));
                        horizontal = tangent.scale(ORBIT_SPEED).add(inward.scale(correction));
                    }
                    // A small descending cycle attempts legitimate falling critical hits.
                    // This does not forge critical packets or override server damage rules.
                    double cycle = Math.sin(combatAttackTicks * 0.35);
                    double desiredHeight = target.getY() + 0.85 + cycle * 0.45;
                    double vertical = Math.max(-COMBAT_VERTICAL_SPEED,
                            Math.min(COMBAT_VERTICAL_SPEED, (desiredHeight - player.getY()) * 0.40));
                    player.setNoGravity(true);
                    player.setDeltaMovement(horizontal.x, vertical, horizontal.z);
                    player.fallDistance = vertical < 0 ? Math.max(player.fallDistance, 0.15f) : 0;

                    boolean inReach = player.distanceToSqr(target) <= 3.0 * 3.0;
                    if (inReach && player.hasLineOfSight(target)) {
                        // No client-side weapon-charge wait: attempt one attack per game tick.
                        // Servers may reduce damage for uncharged hits.
                        client.gameMode.attack(player, target);
                        player.swing(InteractionHand.MAIN_HAND);
                    }
                } else {
                    // No nearby target: do not leave the player hovering indefinitely.
                    combatTargetId = null;
                    combatAttackTicks = 0;
                    player.setNoGravity(false);
                }
            } else if (!noclip && player.isNoGravity()) {
                // Restore gravity when Orbit Combat is turned off.
                player.setNoGravity(false);
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
            if (!noclip && !freecam && swimBoost && !fly && player.getVehicle() == null && player.isInWater()) {
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
            if (!noclip && !freecam && fly && player.getVehicle() == null) {
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
            if (!noclip && !freecam && speed && player.onGround() && player.input != null && player.input.hasForwardImpulse()) {
                Vec3 v = player.getDeltaMovement();
                player.setDeltaMovement(v.x * SPEED_FACTOR, v.y, v.z * SPEED_FACTOR);
            }
            if (boatFly && player.getVehicle() != null
                    && player.getVehicle().getType().toString().contains("boat")) {
                var vehicle = player.getVehicle();
                Vec3 position = vehicle.position();
                if (trackedBoatId != vehicle.getId()) {
                    trackedBoatId = vehicle.getId();
                    lastBoatPosition = position;
                    expectedBoatMovement = vehicle.getDeltaMovement();
                } else if (lastBoatPosition != null) {
                    // A large jump inconsistent with expected movement is a possible
                    // server correction. Stop forcing motion instead of fighting it.
                    Vec3 actualDisplacement = position.subtract(lastBoatPosition);
                    double discrepancy = actualDisplacement.subtract(expectedBoatMovement).length();
                    if (discrepancy > BOAT_CORRECTION_THRESHOLD) {
                        boatFly = false;
                        resetBoatTracking();
                        message(client, "Boat Fly paused: possible server position correction");
                    }
                }
                if (boatFly) {
                    Vec3 velocity = vehicle.getDeltaMovement();
                    // Leave horizontal boat steering to vanilla, smoothly approach
                    // requested vertical motion without spikes or packet spoofing.
                    double targetY = client.options.keyJump.isDown() ? BOAT_ASCEND_SPEED
                            : boatDescendKey.isDown() ? BOAT_DESCEND_SPEED : 0.0;
                    double diff = Math.max(-BOAT_VERTICAL_ACCELERATION,
                            Math.min(BOAT_VERTICAL_ACCELERATION, targetY - velocity.y));
                    Vec3 newVelocity = new Vec3(velocity.x, velocity.y + diff, velocity.z);
                    vehicle.setDeltaMovement(newVelocity);
                    expectedBoatMovement = newVelocity;
                    lastBoatPosition = position;
                }
            } else {
                resetBoatTracking();
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
    private static void drawEspLine(VertexConsumer lines, Matrix4f pose,
            double x1, double y1, double z1, double x2, double y2, double z2,
            int red, int green, int blue) {
        lines.addVertex(pose, (float)x1, (float)y1, (float)z1)
                .setColor(red, green, blue, 255).setNormal(0, 1, 0).setLineWidth(2.0f);
        lines.addVertex(pose, (float)x2, (float)y2, (float)z2)
                .setColor(red, green, blue, 255).setNormal(0, 1, 0).setLineWidth(2.0f);
    }

    private static void drawEspBox(VertexConsumer lines, Matrix4f pose,
            AABB b, int red, int green, int blue) {
        double a = b.minX, c = b.maxX, d = b.minY, e = b.maxY, f = b.minZ, g = b.maxZ;
        drawEspLine(lines, pose, a,d,f, c,d,f, red,green,blue);
        drawEspLine(lines, pose, c,d,f, c,d,g, red,green,blue);
        drawEspLine(lines, pose, c,d,g, a,d,g, red,green,blue);
        drawEspLine(lines, pose, a,d,g, a,d,f, red,green,blue);
        drawEspLine(lines, pose, a,e,f, c,e,f, red,green,blue);
        drawEspLine(lines, pose, c,e,f, c,e,g, red,green,blue);
        drawEspLine(lines, pose, c,e,g, a,e,g, red,green,blue);
        drawEspLine(lines, pose, a,e,g, a,e,f, red,green,blue);
        drawEspLine(lines, pose, a,d,f, a,e,f, red,green,blue);
        drawEspLine(lines, pose, c,d,f, c,e,f, red,green,blue);
        drawEspLine(lines, pose, c,d,g, c,e,g, red,green,blue);
        drawEspLine(lines, pose, a,d,g, a,e,g, red,green,blue);
    }

    // The ClickGUI uses the same state as the keybinds; there is no separate copy of each toggle.
    public static boolean isModuleEnabled(String id) {
        return switch (id) {
            case "fly" -> fly;
            case "speed" -> speed;
            case "boat" -> boatFly;
            case "swim" -> swimBoost;
            case "noclip" -> noclip;
            case "sprint" -> autoSprint;
            case "freecam" -> freecam;
            case "players" -> playerEsp;
            case "mobs" -> mobEsp;
            case "tracers" -> espTracers;
            case "storage" -> storageFinder;
            case "orbit" -> orbitCombat;
            case "nofall" -> noFall;
            case "hunger" -> hungerSaver;
            case "privacy" -> privacyMode;
            case "portal" -> portalScan;
            default -> false;
        };
    }

    public static void toggleModule(String id) {
        Minecraft mc = Minecraft.getInstance();
        switch (id) {
            case "fly" -> { fly = !fly; if (fly) disableNoclip(mc); }
            case "speed" -> speed = !speed;
            case "boat" -> { boatFly = !boatFly; resetBoatTracking(); }
            case "swim" -> swimBoost = !swimBoost;
            case "noclip" -> {
                noclip = !noclip;
                if (noclip) { fly = false; orbitCombat = false; freecam = false; }
                else disableNoclip(mc);
            }
            case "sprint" -> autoSprint = !autoSprint;
            case "freecam" -> {
                freecam = !freecam;
                if (freecam && mc.player != null) {
                    disableNoclip(mc);
                    fly = false;
                    freecamPosition = mc.player.getEyePosition();
                }
            }
            case "players" -> playerEsp = !playerEsp;
            case "mobs" -> mobEsp = !mobEsp;
            case "tracers" -> espTracers = !espTracers;
            case "storage" -> storageFinder = !storageFinder;
            case "orbit" -> {
                orbitCombat = !orbitCombat;
                if (orbitCombat) { disableNoclip(mc); fly = false; freecam = false; }
            }
            case "nofall" -> noFall = !noFall;
            case "hunger" -> hungerSaver = !hungerSaver;
            case "privacy" -> privacyMode = !privacyMode;
            case "portal" -> { portalScan = !portalScan; nearestFrame = null; }
        }
    }

    private static void resetBoatTracking() {
        trackedBoatId = -1;
        lastBoatPosition = null;
        expectedBoatMovement = Vec3.ZERO;
    }

    private static void disableNoclip(Minecraft client) {
        noclip = false;
        if (client.player != null) {
            client.player.noPhysics = false;
            client.player.setNoGravity(false);
        }
    }

    private static void message(Minecraft client, String text) {
        if (client.player != null) client.player.displayClientMessage(Component.literal("§b[PortalFlight] §f" + text), true);
    }
}
