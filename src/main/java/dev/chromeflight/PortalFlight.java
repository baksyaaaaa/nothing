package dev.chromeflight;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.loader.api.FabricLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents;
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
import net.minecraft.network.protocol.game.ServerboundPlayerCommandPacket;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import java.util.UUID;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import java.util.Deque;
import net.minecraft.world.phys.Vec3;
import org.lwjgl.glfw.GLFW;

public final class PortalFlight implements ClientModInitializer {
    private static KeyMapping speedKey, boatKey, portalKey, freecamKey, flyKey, noFallKey, hungerKey, privacyKey, swimKey, boatDescendKey, playerEspKey, mobEspKey, storageFinderKey, autoSprintKey, tracerKey, orbitCombatKey, noclipKey, clickGuiKey;
    private static boolean speed, boatFly, portalScan, fly, noFall = true, hungerSaver, privacyMode = true, spectatorNearby, swimBoost, playerEsp, mobEsp, storageFinder, autoSprint, espTracers = true, orbitCombat, noclip;
    public static boolean freecam;
    public static Vec3 freecamPosition = Vec3.ZERO;
    private static Vec3 coordinateDestination;
    private static int coordinateFlightStuckTicks;
    private static Vec3 coordinateFlightLastPosition;
    private static int coordinateFlightStage;
    private static double coordinateFlightCruiseY;
    private static double coordinateFlightMaxSpeed = 16.0;
    private static final double COORDINATE_FLIGHT_ARRIVAL_RADIUS = 0.8;
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
    private static int elytraPacketTicks;
    private static int storageTick;
    private static final List<BlockPos> nearbyStorage = new ArrayList<>();
    private static final int STORAGE_RADIUS = 24;
    private static final int STORAGE_VERTICAL_RADIUS = 12;
    private static final int STORAGE_MAX_RESULTS = 256;
    private static BlockPos lastStorageScanOrigin;
    private static String storageWorldKey = "";
    private static int spectatorCheckTick;
    private static String spectatorWarning = "";
    private static final Deque<String> staffWatchEvents = new ArrayDeque<>();
    private static String lastObservedServerMessage = "";
    private static String watchWorldKey = "";
    private static final double WATCH_RADIUS = 128.0;
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
        loadCoordinateFlightSpeed();
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
        // Read only messages the server actually delivers to this client.
        // Other users' private slash commands are not exposed to ordinary clients.
        ClientReceiveMessageEvents.GAME.register((message, overlay) -> {
            if (message == null) return;
            String observed = message.getString();
            String lower = observed.toLowerCase(java.util.Locale.ROOT);
            // Only treat explicit server announcements as evidence that the word
            // "vanish" appeared; this does NOT identify the player who ran /vanish.
            if (lower.contains("vanish") && !observed.equals(lastObservedServerMessage)) {
                lastObservedServerMessage = observed;
                addStaffWatchEvent("Server message mentions vanish: " + truncateWatch(observed));
            }
        });
        // Render outlines and optional tracers for entities the client already knows about.
        // WorldRenderContext matrices must be used when submitting vertices in 1.21.11.
        WorldRenderEvents.BEFORE_DEBUG_RENDER.register(context -> {
            if (!playerEsp && !mobEsp && !storageFinder) return;
            Minecraft mc = Minecraft.getInstance();
            if (mc.level == null || mc.player == null || context.consumers() == null || context.matrices() == null) return;
            Vec3 camera = mc.gameRenderer.getMainCamera().position();
            Matrix4f pose = context.matrices().last().pose();
            VertexConsumer lines = context.consumers().getBuffer(RenderTypes.lines());
            Vec3 tracerStart = camera.add(mc.player.getLookAngle().scale(0.65));
            if (storageFinder) {
                // Draw client-known container blocks in world space, camera-relative.
                for (BlockPos pos : nearbyStorage) {
                    if (!mc.level.hasChunkAt(pos)) continue;
                    var state = mc.level.getBlockState(pos);
                    if (!isStorageBlock(state)) continue;
                    int red = state.is(Blocks.BARREL) ? 255 : 255;
                    int green = state.is(Blocks.BARREL) ? 190 : 215;
                    int blue = state.is(Blocks.BARREL) ? 65 : 40;
                    AABB bounds = new AABB(pos).inflate(0.015)
                            .move(-camera.x, -camera.y, -camera.z);
                    drawEspBox(lines, pose, bounds, red, green, blue);
                }
            }
            if (!playerEsp && !mobEsp) return;
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
            while (storageFinderKey.consumeClick()) {
                storageFinder = !storageFinder;
                nearbyStorage.clear();
                lastStorageScanOrigin = null;
                message(client, "Storage ESP " + (storageFinder ? "ON" : "OFF"));
            }
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
                spectatorNearby = false; spectatorWarning = ""; noclip = false;
                staffWatchEvents.clear(); watchWorldKey = ""; lastObservedServerMessage = "";
                coordinateDestination = null; coordinateFlightLastPosition = null; coordinateFlightStuckTicks = 0;
                elytraPacketTicks = 0;
                nearbyStorage.clear(); lastStorageScanOrigin = null; storageWorldKey = "";
                return;
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
            // Refresh an inexpensive cache of only client-loaded containers.
            // Rendering uses this cache rather than scanning the world every frame.
            if (storageFinder) {
                BlockPos origin = player.blockPosition();
                String dimensionKey = client.level.dimension().toString();
                boolean movedFar = lastStorageScanOrigin == null
                        || origin.distSqr(lastStorageScanOrigin) > 64.0;
                if (!dimensionKey.equals(storageWorldKey) || movedFar
                        || ++storageTick >= 40) {
                    storageTick = 0;
                    storageWorldKey = dimensionKey;
                    lastStorageScanOrigin = origin.immutable();
                    nearbyStorage.clear();
                    BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
                    for (int dx = -STORAGE_RADIUS; dx <= STORAGE_RADIUS; dx++) {
                        for (int dz = -STORAGE_RADIUS; dz <= STORAGE_RADIUS; dz++) {
                            for (int dy = -STORAGE_VERTICAL_RADIUS; dy <= STORAGE_VERTICAL_RADIUS; dy++) {
                                cursor.set(origin.getX() + dx, origin.getY() + dy, origin.getZ() + dz);
                                if (!client.level.hasChunkAt(cursor)) continue;
                                if (!isStorageBlock(client.level.getBlockState(cursor))) continue;
                                nearbyStorage.add(cursor.immutable());
                                if (nearbyStorage.size() >= STORAGE_MAX_RESULTS) break;
                            }
                            if (nearbyStorage.size() >= STORAGE_MAX_RESULTS) break;
                        }
                        if (nearbyStorage.size() >= STORAGE_MAX_RESULTS) break;
                    }
                }
            }
            // Old evidence must not be carried across dimension/world transitions.
            String activeWorld = client.level.dimension().toString();
            if (!activeWorld.equals(watchWorldKey)) {
                watchWorldKey = activeWorld;
                staffWatchEvents.clear();
                lastObservedServerMessage = "";
                spectatorWarning = "";
                spectatorNearby = false;
                stopCoordinateFlight();
            }
            // Staff Watch: monitor only evidence that the client actually receives.
            // Server-side vanish can fully hide staff, so no signal means UNKNOWN,
            // never "no staff watching you". Do not confuse heuristics with proof.
            if (++spectatorCheckTick % 10 == 0) {
                String warning = "";
                boolean visibleSpectator = false;
                for (var other : client.level.players()) {
                    if (other == player || other.distanceToSqr(player) > WATCH_RADIUS * WATCH_RADIUS) continue;
                    if (other.isSpectator()) {
                        visibleSpectator = true;
                        warning = "VISIBLE SPECTATOR NEARBY: " + other.getName().getString();
                        break;
                    }
                    // Invisible players are not necessarily staff; potions and
                    // gameplay effects also cause invisibility.
                    if (warning.isEmpty() && other.isInvisible()) {
                        warning = "INVISIBLE PLAYER NEARBY (identity unknown): " + other.getName().getString();
                    }
                }
                boolean nowWatching = !warning.isEmpty();
                if (nowWatching && (!spectatorNearby || !warning.equals(spectatorWarning))) {
                    message(client, "WARNING: " + warning);
                    addStaffWatchEvent(warning);
                }
                if (!nowWatching && spectatorNearby)
                    addStaffWatchEvent("Visible spectator/invisible player signal ended");
                spectatorNearby = nowWatching;
                spectatorWarning = warning;
                if (visibleSpectator && privacyMode) {
                    if (fly || speed || boatFly || freecam || noFall || noclip || orbitCombat || coordinateDestination != null) {
                        fly = false; speed = false; boatFly = false; freecam = false;
                        stopCoordinateFlight();
                        noFall = false; orbitCombat = false;
                        disableNoclip(client);
                        resetBoatTracking();
                        message(client, "Spectator Privacy: movement and combat modules disabled");
                    }
                }
            }
            // Keep the warning visible while the signal is still present.
            if (privacyMode && spectatorNearby && !spectatorWarning.isEmpty()) {
                player.displayClientMessage(Component.literal("§c§l[STAFF WATCH?] §e" + spectatorWarning), true);
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
            } else if (!noclip && coordinateDestination == null && player.isNoGravity()) {
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
                // Freecam uses camera orientation, not the grounded player's horizontal movement.
                // Positive strafe must point right (the previous vector was reversed).
                double yaw = Math.toRadians(player.getYRot());
                double pitch = Math.toRadians(player.getXRot());
                Vec3 forwardVec = new Vec3(-Math.sin(yaw) * Math.cos(pitch),
                        -Math.sin(pitch), Math.cos(yaw) * Math.cos(pitch));
                Vec3 rightVec = new Vec3(Math.cos(yaw), 0, Math.sin(yaw));
                Vec3 movement = forwardVec.scale((forward ? 1 : 0) - (back ? 1 : 0))
                    .add(rightVec.scale((right ? 1 : 0) - (left ? 1 : 0)))
                    .add(0, (up ? 1 : 0) - (down ? 1 : 0), 0);
                if (movement.lengthSqr() > 0) {
                    freecamPosition = freecamPosition.add(movement.normalize().scale(FREECAM_SPEED));
                }
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
            if (coordinateDestination != null) {
                Vec3 currentPosition = player.position();
                Vec3 waypoint;
                String flightPhase;
                if (coordinateFlightStage == 0) {
                    waypoint = new Vec3(currentPosition.x, coordinateFlightCruiseY, currentPosition.z);
                    flightPhase = "Climbing";
                    if (Math.abs(coordinateFlightCruiseY - currentPosition.y) < 0.8) {
                        coordinateFlightStage = 1;
                    }
                } else if (coordinateFlightStage == 1) {
                    waypoint = new Vec3(coordinateDestination.x, coordinateFlightCruiseY, coordinateDestination.z);
                    flightPhase = "Cruising";
                    double horizontalDistance = Math.hypot(coordinateDestination.x - currentPosition.x,
                            coordinateDestination.z - currentPosition.z);
                    if (horizontalDistance < 0.8
                            && Math.abs(currentPosition.y - coordinateFlightCruiseY) < 1.2) {
                        coordinateFlightStage = 2;
                    }
                } else {
                    waypoint = coordinateDestination;
                    flightPhase = "Descending";
                }
                // Recalculate immediately when moving from one flight phase to the next.
                if (coordinateFlightStage == 1) {
                    waypoint = new Vec3(coordinateDestination.x, coordinateFlightCruiseY, coordinateDestination.z);
                    flightPhase = "Cruising";
                } else if (coordinateFlightStage == 2) {
                    waypoint = coordinateDestination;
                    flightPhase = "Descending";
                }
                Vec3 delta = waypoint.subtract(currentPosition);
                double remaining = delta.length();
                if (coordinateFlightStage == 2 && remaining <= COORDINATE_FLIGHT_ARRIVAL_RADIUS) {
                    stopCoordinateFlight();
                    player.setDeltaMovement(Vec3.ZERO);
                    message(client, "Coordinate Flight: arrived");
                } else if (player.getVehicle() != null) {
                    stopCoordinateFlight();
                    message(client, "Coordinate Flight stopped: dismount required");
                } else {
                    // Fast, smooth flight in three stages: up, horizontal, down.
                    // This is not teleportation; servers may reject/correct movement.
                    double travelSpeed = Math.min(coordinateFlightMaxSpeed,
                            Math.max(0.001, remaining * 0.22));
                    player.setNoGravity(true);
                    player.setDeltaMovement(remaining > 0.001
                            ? delta.normalize().scale(travelSpeed) : Vec3.ZERO);
                    player.setOnGround(false);
                    player.fallDistance = 0;
                    if (coordinateFlightLastPosition != null
                            && currentPosition.distanceToSqr(coordinateFlightLastPosition) < 0.0004) {
                        coordinateFlightStuckTicks++;
                    } else {
                        coordinateFlightStuckTicks = 0;
                    }
                    coordinateFlightLastPosition = currentPosition;
                    if (coordinateFlightStuckTicks > 40) {
                        stopCoordinateFlight();
                        player.setDeltaMovement(Vec3.ZERO);
                        message(client, "Coordinate Flight stopped: blocked or corrected by server");
                    } else if (spectatorCheckTick % 20 == 0) {
                        player.displayClientMessage(Component.literal("§bCoordinate Flight §f"
                                + flightPhase + " | " + (int) currentPosition.distanceTo(coordinateDestination)
                                + " blocks to destination"), true);
                    }
                }
            }
            if (!noclip && !freecam && fly && coordinateDestination == null && player.getVehicle() == null) {
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
            // Request Elytra fall-flying while manual Fly or Coordinate Flight is enabled.
            // START_FALL_FLYING is a request, not proof of gliding; the server checks
            // equipment and player state. Avoid conflicting onGround flight packets.
            boolean flightRequested = !noclip && !freecam
                    && (fly || coordinateDestination != null)
                    && player.getVehicle() == null && client.getConnection() != null;
            if (flightRequested) {
                if (++elytraPacketTicks == 1 || elytraPacketTicks >= 20) {
                    elytraPacketTicks = 1;
                    if (!player.isFallFlying()) {
                        client.getConnection().send(new ServerboundPlayerCommandPacket(
                                player, ServerboundPlayerCommandPacket.Action.START_FALL_FLYING));
                    }
                }
            } else {
                elytraPacketTicks = 0;
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
    private static boolean isStorageBlock(net.minecraft.world.level.block.state.BlockState state) {
        return state.is(Blocks.CHEST) || state.is(Blocks.TRAPPED_CHEST)
                || state.is(Blocks.BARREL) || state.is(Blocks.ENDER_CHEST);
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

    public static List<String> getStaffWatchEvents() {
        return new ArrayList<>(staffWatchEvents);
    }

    public static String getStaffWatchStatus() {
        return spectatorNearby ? spectatorWarning : "Unknown - no current client-visible signals";
    }

    private static String truncateWatch(String value) {
        String cleaned = value.replace('\n', ' ').replace('\r', ' ');
        return cleaned.length() > 110 ? cleaned.substring(0, 110) + "..." : cleaned;
    }

    private static void addStaffWatchEvent(String value) {
        if (staffWatchEvents.size() >= 12) staffWatchEvents.removeLast();
        staffWatchEvents.addFirst(java.time.LocalTime.now().withNano(0) + " - " + value);
    }

    /** Speed shown to players is in blocks per second (20 game ticks/second). */
    public static double getCoordinateFlightSpeed() {
        return coordinateFlightMaxSpeed * 20.0;
    }

    /** Accept any positive speed up to 1280 blocks/second. */
    public static boolean setCoordinateFlightSpeed(double blocksPerSecond) {
        if (!Double.isFinite(blocksPerSecond) || blocksPerSecond <= 0.0
                || blocksPerSecond > 1280.0) {
            return false;
        }
        coordinateFlightMaxSpeed = blocksPerSecond / 20.0;
        saveCoordinateFlightSpeed();
        return true;
    }

    private static Path coordinateFlightConfigPath() {
        return FabricLoader.getInstance().getConfigDir().resolve("portalflight.properties");
    }

    private static void loadCoordinateFlightSpeed() {
        Path path = coordinateFlightConfigPath();
        if (!Files.exists(path)) return;
        try (var stream = Files.newInputStream(path)) {
            Properties properties = new Properties();
            properties.load(stream);
            double value = Double.parseDouble(properties.getProperty("coordinateFlightSpeed", "16.0"));
            if (Double.isFinite(value) && value > 0.0 && value <= 64.0) {
                coordinateFlightMaxSpeed = value;
            }
        } catch (Exception ignored) {
            // Invalid or missing configuration: use the default value.
        }
    }

    private static void saveCoordinateFlightSpeed() {
        try {
            Path path = coordinateFlightConfigPath();
            Files.createDirectories(path.getParent());
            Properties properties = new Properties();
            properties.setProperty("coordinateFlightSpeed", Double.toString(coordinateFlightMaxSpeed));
            try (var stream = Files.newOutputStream(path)) {
                properties.store(stream, "PortalFlight client settings");
            }
        } catch (Exception ignored) {
            // The selected speed remains active for the current session.
        }
    }

    public static boolean isCoordinateFlightActive() {
        return coordinateDestination != null;
    }

    public static String getCoordinateFlightDestination() {
        if (coordinateDestination == null) return "None";
        return String.format(java.util.Locale.ROOT, "%.1f, %.1f, %.1f",
                coordinateDestination.x, coordinateDestination.y, coordinateDestination.z);
    }

    public static boolean startCoordinateFlight(double x, double y, double z) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null || mc.player.getVehicle() != null
                || !Double.isFinite(x) || !Double.isFinite(y) || !Double.isFinite(z)
                || Math.abs(x) > 29999984 || Math.abs(z) > 29999984 || y < -2048 || y > 2048) return false;
        disableNoclip(mc);
        fly = false;
        boatFly = false;
        orbitCombat = false;
        freecam = false;
        speed = false;
        coordinateDestination = new Vec3(x, y, z);
        coordinateFlightCruiseY = Math.max(200.0, Math.max(mc.player.getY(), y));
        coordinateFlightStage = mc.player.getY() < coordinateFlightCruiseY - 0.8 ? 0 : 1;
        coordinateFlightStuckTicks = 0;
        coordinateFlightLastPosition = null;
        return true;
    }

    public static void stopCoordinateFlight() {
        if (coordinateDestination == null) return;
        coordinateDestination = null;
        coordinateFlightStage = 0;
        coordinateFlightStuckTicks = 0;
        coordinateFlightLastPosition = null;
        Minecraft mc = Minecraft.getInstance();
        if (mc.player != null && !noclip && !orbitCombat) mc.player.setNoGravity(false);
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
            case "storage" -> {
                storageFinder = !storageFinder;
                nearbyStorage.clear();
                lastStorageScanOrigin = null;
            }
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
