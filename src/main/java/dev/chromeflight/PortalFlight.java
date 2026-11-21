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
import dev.chromeflight.HomeCommands;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;
import net.minecraft.network.protocol.game.ServerboundPlayerCommandPacket;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.item.Items;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.resources.Identifier;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import java.util.UUID;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import java.util.Deque;
import java.util.Map;
import java.util.HashMap;
import net.minecraft.world.phys.Vec3;
import org.lwjgl.glfw.GLFW;

public final class PortalFlight implements ClientModInitializer {
    private static KeyMapping speedKey, boatKey, portalKey, freecamKey, flyKey, noFallKey, hungerKey, privacyKey, swimKey, boatDescendKey, playerEspKey, mobEspKey, storageFinderKey, autoSprintKey, tracerKey, orbitCombatKey, noclipKey, clickGuiKey;
    private static boolean weaponReach, orbitTargetMobs, speed, boatFly, portalScan, fly, noFall, hungerSaver, privacyMode = true, spectatorNearby, swimBoost, playerEsp, mobEsp, storageFinder, autoSprint, espTracers = true, orbitCombat, noclip, autoTotem, autoLeave, autoWalk, autoJump, highJump, fastFall, waterWalk, autoSneak, noSlow, elytraBoost;
    public static boolean freecam;
    public static Vec3 freecamPosition = Vec3.ZERO;
    public static Vec3 freecamPreviousPosition = Vec3.ZERO;
    private static Vec3 freecamVelocity = Vec3.ZERO;
    private static final Map<UUID, Vec3> lastSeenPlayerPositions = new HashMap<>();
    private static String lastSeenWorldKey = "";
    private static Vec3 coordinateDestination;
    private static boolean cruisePhaseApplied;
    private static int coordinateFlightStuckTicks;
    private static Vec3 coordinateFlightLastPosition;
    private static int coordinateFlightStage;
    private static double coordinateFlightCruiseY;
    private static double coordinateFlightMaxSpeed = 16.0;
    private static Vec3 pendingTeleportDestination;
    private static int pendingTeleportCheckTicks;
    private static String coordinateTeleportError = "";
    private static Vec3 pendingTeleportOrigin;
    private static String lastTeleportStatus = "No teleport attempted yet.";
    // Fly to Player has a separate extreme client-side speed (50,000 blocks/second).
    private static final double PLAYER_TRAVEL_SPEED_PER_TICK = 2500.0;
    private static boolean highSpeedPlayerTravel;
    private static final double COORDINATE_FLIGHT_ARRIVAL_RADIUS = 0.8;
    private static final double FREECAM_SPEED = 0.75;
    private static final double FREECAM_ACCELERATION = 0.32;
    private static final double FREECAM_FRICTION = 0.70;
    private static final double FLY_SPEED = 0.40;
    private static final double SPRINT_FLY_SPEED = 1.20;
    private static final double SWIM_BOOST_SPEED = 1.65;
    private static int scanTick;
    private static int combatAttackTicks;
    private static final Identifier WEAPON_REACH_MODIFIER = Identifier.fromNamespaceAndPath("portalflight", "weapon_reach");
    private static final double[] WEAPON_REACH_VALUES = {6.0, 12.0, 24.0, 48.0};
    private static int weaponReachIndex = 1;
    private static int orbitAttackCooldownTicks;
    private static boolean orbitDiving;
    private static int orbitDiveRecoveryTicks;
    private static Vec3 orbitSmoothedVelocity = Vec3.ZERO;
    private static Vec3 orbitLastTargetPosition;
    private static UUID orbitLastTargetId;
    private static UUID combatTargetId;
    private static int combatOrbitDirection = 1;
    private static double orbitAngleRadians;
    private static final double ORBIT_STEP_RADIANS = 0.19;
    // Maximum orbit movement speed: 1280 blocks/sec at 20 ticks/sec.
    private static final double ORBIT_SPEED = 64.0;
    private static final double ORBIT_ACCELERATION = 0.20;
    private static final double ORBIT_MAX_HORIZONTAL_SPEED = 64.0;
    // A high launch followed by a very fast dive for mace smash attempts.
    private static final double ORBIT_DIVE_HEIGHT = 12.0;
    private static final double ORBIT_DIVE_ASCEND_SPEED = 3.5;
    private static final double ORBIT_DIVE_DESCEND_SPEED = 7.0;
    private static final double ORBIT_RADIUS = 3.7;
    private static final double COMBAT_VERTICAL_SPEED = 0.42;
    private static final double NOCLIP_SPEED = 0.55;
    private static int elytraPacketTicks;
    private static int storageTick;
    private static boolean autoJumpWasGrounded;
    private static int autoJumpIntervalTicks;
    private static int autoTotemDelayTicks;
    // Minecraft health points: 1 HP = half a heart, 2 HP = one heart.
    private static float autoLeaveThreshold = 2.0f;
    private static final List<BlockPos> nearbyStorage = new ArrayList<>();
    // Build the next scan separately so refreshes do not blank all ESP boxes.
    private static final List<BlockPos> storageScanResults = new ArrayList<>();
    private static final int STORAGE_RADIUS = 256;
    private static final int STORAGE_MAX_RESULTS = 4096;
    // Reading 24 loaded chunk block-entity maps per tick is far cheaper
    // than iterating every block in a 512-block-wide region.
    private static final int STORAGE_SCAN_CHUNKS_PER_TICK = 24;
    private static int storageScanIndex;
    private static boolean storageScanInProgress;
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
        HomeCommands.register();
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
                // On the very first scan, show newly found blocks immediately.
                List<BlockPos> visibleStorage = storageScanInProgress && nearbyStorage.isEmpty()
                        ? storageScanResults : nearbyStorage;
                for (BlockPos pos : visibleStorage) {
                    double dx = pos.getX() - mc.player.getX();
                    double dz = pos.getZ() - mc.player.getZ();
                    if (!mc.level.hasChunkAt(pos)
                            || dx * dx + dz * dz > STORAGE_RADIUS * STORAGE_RADIUS) continue;
                    var state = mc.level.getBlockState(pos);
                    if (!isStorageBlock(state)) continue;
                    // Storage gets warm amber tracers; barrel boxes use a
                    // slightly darker variant to distinguish container types.
                    int red = 255;
                    int green = state.is(Blocks.BARREL) ? 175 : 215;
                    int blue = state.is(Blocks.BARREL) ? 45 : 65;
                    AABB bounds = new AABB(pos).inflate(0.015)
                            .move(-camera.x, -camera.y, -camera.z);
                    drawEspBox(lines, pose, bounds, red, green, blue);
                    if (espTracers) {
                        Vec3 from = tracerStart.subtract(camera);
                        Vec3 target = Vec3.atCenterOf(pos).subtract(camera);
                        drawEspLine(lines, pose, from.x, from.y, from.z,
                                target.x, target.y, target.z, red, green, blue);
                    }
                }
            }
            if (!playerEsp && !mobEsp) return;
            for (var entity : mc.level.entitiesForRendering()) {
                if (entity == mc.player || !(entity instanceof LivingEntity) || !entity.isAlive()) continue;
                boolean isPlayer = entity instanceof Player;
                if (!(isPlayer ? playerEsp : mobEsp)) continue;
                // Render every tracked living entity received by the client.
                // The server still controls which distant entities exist locally.
                // Player ESP: bright cyan; Mob ESP: lime green.
                // Storage ESP: amber (configured above).
                int red = isPlayer ? 50 : 110;
                int green = isPlayer ? 225 : 255;
                int blue = isPlayer ? 255 : 95;
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
        // First re-lock opportunity, before the normal client movement tick.
        // START and END are phases of the same 20 TPS tick, not an exact
        // 25 ms timer. Both call only ordinary client/network code.
        ClientTickEvents.START_CLIENT_TICK.register(client -> {
            if (!orbitCombat || orbitDiving || noclip || freecam || fly
                    || client.player == null || client.level == null
                    || client.player.getVehicle() != null || client.screen != null
                    || combatTargetId == null || client.getConnection() == null) return;
            for (var entity : client.level.entitiesForRendering()) {
                if (entity instanceof LivingEntity other
                        && other.getUUID().equals(combatTargetId)
                        && validOrbitTarget(other, client.player)) {
                    Vec3 leadPosition = predictOrbitTarget(other, orbitLastTargetPosition,
                            orbitLastTargetId);
                    // During a charged dive do not reset fall distance with a TP.
                    attemptOrbitSnap(client, client.player, other, leadPosition);
                    break;
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
                orbitAttackCooldownTicks = 0;
                orbitDiving = false;
                orbitDiveRecoveryTicks = 0;
                combatTargetId = null;
                orbitSmoothedVelocity = Vec3.ZERO;
                orbitLastTargetPosition = null;
                orbitLastTargetId = null;
                if (!orbitCombat && client.player != null && !noclip) client.player.setNoGravity(false);
                message(client, "Orbit Combat " + (orbitCombat ? "ON" : "OFF"));
            }
            while (tracerKey.consumeClick()) { espTracers = !espTracers; message(client, "ESP Tracers " + (espTracers ? "ON" : "OFF")); }
            while (playerEspKey.consumeClick()) { playerEsp = !playerEsp; message(client, "Player ESP " + (playerEsp ? "ON" : "OFF")); }
            while (mobEspKey.consumeClick()) { mobEsp = !mobEsp; message(client, "Mob ESP " + (mobEsp ? "ON" : "OFF")); }
            while (storageFinderKey.consumeClick()) {
                storageFinder = !storageFinder;
                nearbyStorage.clear();
                storageScanResults.clear();
                lastStorageScanOrigin = null;
                storageScanInProgress = false;
                storageScanIndex = 0;
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
                if (freecam && player != null) beginFreecam(player);
                message(client, "Freecam " + (freecam ? "ON" : "OFF"));
            }
            if (player == null || client.level == null) {
                spectatorNearby = false; spectatorWarning = ""; noclip = false;
                staffWatchEvents.clear(); watchWorldKey = ""; lastObservedServerMessage = "";
                coordinateDestination = null; highSpeedPlayerTravel = false;
                coordinateFlightLastPosition = null; coordinateFlightStuckTicks = 0;
                elytraPacketTicks = 0;
                nearbyStorage.clear(); storageScanResults.clear();
                lastStorageScanOrigin = null; storageWorldKey = "";
                storageScanInProgress = false; storageScanIndex = 0;
                lastSeenPlayerPositions.clear(); lastSeenWorldKey = "";
                cruisePhaseApplied = false;
                pendingTeleportDestination = null;
                pendingTeleportOrigin = null;
                pendingTeleportCheckTicks = 0;
                autoTotemDelayTicks = 0;
                autoJumpWasGrounded = false;
                autoJumpIntervalTicks = 0;
                return;
            }
            // Refresh the client-side entity reach attribute for vanilla
            // crosshair target selection and normal attack input. A server
            // can still reject any hit beyond its own accepted range.
            updateWeaponReach(player);
            // AutoLeave is checked before combat and movement so that low-health
            // disconnects are not delayed by other movement modules.
            if (autoLeave && player.isAlive() && player.getHealth() > 0
                    && player.getHealth() <= autoLeaveThreshold
                    && client.getConnection() != null) {
                client.getConnection().getConnection().disconnect(Component.literal(
                        "PortalFlight AutoLeave: " + player.getHealth()
                                + " HP (threshold " + autoLeaveThreshold + " HP)"));
                return;
            }
            // AutoTotem uses a vanilla inventory SWAP with hotbar key 40 (offhand).
            // Run only while the player inventory menu is active; don't interfere
            // with chests, crafting screens or items carried on the cursor.
            if (autoTotem && client.gameMode != null
                    && player.containerMenu == player.inventoryMenu
                    && player.inventoryMenu.getCarried().isEmpty()
                    && !player.getOffhandItem().is(Items.TOTEM_OF_UNDYING)
                    && --autoTotemDelayTicks <= 0) {
                autoTotemDelayTicks = 10;
                for (int inventorySlot = 0; inventorySlot < 36; inventorySlot++) {
                    if (!player.getInventory().getItem(inventorySlot)
                            .is(Items.TOTEM_OF_UNDYING)) continue;
                    int menuSlot = inventorySlot < 9
                            ? 36 + inventorySlot : inventorySlot;
                    client.gameMode.handleInventoryMouseClick(
                            player.inventoryMenu.containerId, menuSlot,
                            40, ClickType.SWAP, player);
                    break;
                }
            }
            // A movement packet is a request, not server confirmation.
            // Record whether the client was corrected away from the requested
            // position. The actual server rejection reason is not in the packet.
            if (pendingTeleportDestination != null) {
                double distanceFromTarget = player.position().distanceTo(pendingTeleportDestination);
                if (--pendingTeleportCheckTicks <= 0) {
                    if (distanceFromTarget < 3.0) {
                        reportTeleport(client, "TP near destination: server acceptance not guaranteed.");
                    } else {
                        double fromStart = pendingTeleportOrigin == null
                                ? Double.POSITIVE_INFINITY
                                : player.position().distanceTo(pendingTeleportOrigin);
                        String detail = fromStart < 8.0
                                ? "Returned near starting position: likely server correction."
                                : "Player is not at destination: server correction or movement after TP.";
                        reportTeleport(client, "TP failed (" + (int) Math.round(distanceFromTarget)
                                + " blocks from target). " + detail
                                + " Server did not provide an exact reason. No cruise fallback.");
                    }
                    pendingTeleportDestination = null;
                    pendingTeleportOrigin = null;
                }
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
            // Additional movement modules: never override cruise, noclip, orbit,
            // freecam, manual fly, or vehicle movement. Server physics are final.
            boolean regularMovement = !noclip && !freecam && !fly
                    && !orbitCombat && coordinateDestination == null
                    && player.getVehicle() == null && client.screen == null;
            if (regularMovement) {
                boolean grounded = player.onGround();
                boolean movingForward = client.options.keyUp.isDown();
                boolean movingBackward = client.options.keyDown.isDown();
                boolean movingLeft = client.options.keyLeft.isDown();
                boolean movingRight = client.options.keyRight.isDown();
                boolean movingManually = movingForward || movingBackward || movingLeft || movingRight;
                if (autoWalk && !movingManually && !player.isShiftKeyDown()) {
                    double yaw = Math.toRadians(player.getYRot());
                    Vec3 velocity = player.getDeltaMovement();
                    player.setDeltaMovement(-Math.sin(yaw) * 0.23,
                            velocity.y, Math.cos(yaw) * 0.23);
                    player.setSprinting(true);
                }
                // Jump once on ground contact; do not auto-jump every air tick.
                if (autoJump && grounded && !autoJumpWasGrounded
                        && !client.options.keyShift.isDown()) {
                    Vec3 v = player.getDeltaMovement();
                    player.setDeltaMovement(v.x, highJump ? 0.85 : 0.42, v.z);
                    autoJumpIntervalTicks = 3;
                }
                if (autoJumpIntervalTicks > 0) autoJumpIntervalTicks--;
                if (autoJump && grounded && autoJumpIntervalTicks == 0
                        && (autoWalk || movingManually)
                        && !client.options.keyShift.isDown()) {
                    Vec3 v = player.getDeltaMovement();
                    player.setDeltaMovement(v.x, highJump ? 0.85 : 0.42, v.z);
                    autoJumpIntervalTicks = 8;
                }
                if (highJump && grounded && client.options.keyJump.isDown()
                        && !autoJump) {
                    Vec3 v = player.getDeltaMovement();
                    if (v.y <= 0.42) player.setDeltaMovement(v.x, 0.85, v.z);
                }
                if (fastFall && !grounded && !player.isInWater()
                        && !player.isInLava() && !player.isFallFlying()
                        && !client.options.keyJump.isDown()) {
                    Vec3 v = player.getDeltaMovement();
                    if (v.y < -0.08) player.setDeltaMovement(
                            v.x, Math.max(-2.4, v.y - 0.12), v.z);
                }
                if (waterWalk && player.isInWater() && !player.isShiftKeyDown()
                        && !player.isFallFlying()) {
                    Vec3 v = player.getDeltaMovement();
                    if (v.y < 0.12) player.setDeltaMovement(v.x, 0.12, v.z);
                }
                autoJumpWasGrounded = grounded;
            } else {
                autoJumpWasGrounded = false;
                autoJumpIntervalTicks = 0;
            }
            // Additional Meteor-style movement options. The normal movement
            // modules avoid overriding teleport, orbit, flight and freecam.
            if (regularMovement && autoSneak) {
                player.setShiftKeyDown(true);
            } else if (autoSneak && !client.options.keyShift.isDown()) {
                player.setShiftKeyDown(false);
            }
            if (regularMovement && noSlow && player.isUsingItem()) {
                Vec3 v = player.getDeltaMovement();
                player.setDeltaMovement(v.x * 1.18, v.y, v.z * 1.18);
            }
            if (elytraBoost && player.isFallFlying() && !noclip && !freecam
                    && !orbitCombat && coordinateDestination == null
                    && client.options.keyUp.isDown()) {
                Vec3 v = player.getDeltaMovement();
                Vec3 look = player.getLookAngle();
                player.setDeltaMovement(v.add(look.scale(0.08)));
            }
            // Record only positions actually sent to this client. The tab list
            // supplies names, not coordinates. Never invent hidden players' positions.
            String observedWorld = client.level.dimension().toString();
            if (!observedWorld.equals(lastSeenWorldKey)) {
                lastSeenPlayerPositions.clear();
                lastSeenWorldKey = observedWorld;
            }
            for (Player observed : client.level.players()) {
                if (observed != player && !observed.isRemoved()) {
                    lastSeenPlayerPositions.put(observed.getUUID(), observed.position());
                }
            }
            // Discover storage by inspecting the block entities in already
            // loaded chunks, rather than checking millions of individual blocks.
            // Cover the full vertical range of each loaded chunk.
            if (storageFinder) {
                BlockPos origin = player.blockPosition();
                String dimensionKey = client.level.dimension().toString();
                boolean changedWorld = !dimensionKey.equals(storageWorldKey);
                boolean movedFar = lastStorageScanOrigin == null
                        || origin.distSqr(lastStorageScanOrigin) > 16.0 * 16.0;
                if (changedWorld || movedFar
                        || (!storageScanInProgress && ++storageTick >= 60)) {
                    if (changedWorld) nearbyStorage.clear();
                    storageTick = 0;
                    storageWorldKey = dimensionKey;
                    lastStorageScanOrigin = origin.immutable();
                    storageScanResults.clear();
                    storageScanIndex = 0;
                    storageScanInProgress = true;
                }
                if (storageScanInProgress && lastStorageScanOrigin != null) {
                    // Radius is measured in blocks; iterate neighboring chunk
                    // coordinates, starting in the center and moving outward.
                    int chunkRadius = (STORAGE_RADIUS + 15) / 16;
                    int diameter = chunkRadius * 2 + 1;
                    int total = diameter * diameter;
                    int originChunkX = lastStorageScanOrigin.getX() >> 4;
                    int originChunkZ = lastStorageScanOrigin.getZ() >> 4;
                    int scannedChunks = 0;
                    while (storageScanIndex < total
                            && scannedChunks < STORAGE_SCAN_CHUNKS_PER_TICK
                            && storageScanResults.size() < STORAGE_MAX_RESULTS) {
                        int index = storageScanIndex++;
                        int chunkOffsetX = centeredScanOffset(index / diameter);
                        int chunkOffsetZ = centeredScanOffset(index % diameter);
                        int chunkX = originChunkX + chunkOffsetX;
                        int chunkZ = originChunkZ + chunkOffsetZ;
                        if (!client.level.hasChunk(chunkX, chunkZ)) continue;
                        scannedChunks++;
                        for (BlockPos pos : client.level.getChunk(chunkX, chunkZ)
                                .getBlockEntities().keySet()) {
                            double dx = pos.getX() - lastStorageScanOrigin.getX();
                            double dz = pos.getZ() - lastStorageScanOrigin.getZ();
                            if (dx * dx + dz * dz > STORAGE_RADIUS * STORAGE_RADIUS) continue;
                            if (isStorageBlock(client.level.getBlockState(pos))) {
                                storageScanResults.add(pos.immutable());
                                if (storageScanResults.size() >= STORAGE_MAX_RESULTS) break;
                            }
                        }
                    }
                    if (storageScanIndex >= total
                            || storageScanResults.size() >= STORAGE_MAX_RESULTS) {
                        nearbyStorage.clear();
                        nearbyStorage.addAll(storageScanResults);
                        storageScanInProgress = false;
                        storageTick = 0;
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
                // Client-only fall-distance reset. Never send fake on-ground
                // movement packets: those are detectable and can trigger
                // server anti-cheat kicks even though the player is falling.
                // This does not prevent server-authoritative fall damage.
                player.fallDistance = 0;
            }
            // Orbit Combat follows its target with a flying circle and attacks frequently.
            // Server-side damage cooldowns and critical hit rules still apply.
            if (orbitCombat && !noclip && !freecam && !fly && player.getVehicle() == null
                    && client.gameMode != null && client.screen == null) {
                LivingEntity target = null;
                // Players are the default. Target Mobs switches targeting to
                // loaded Mob entities so combat can be tested without PvP.
                if (combatTargetId != null) {
                    for (var entity : client.level.entitiesForRendering()) {
                        if (entity instanceof LivingEntity other
                                && other.getUUID().equals(combatTargetId)
                                && validOrbitTarget(other, player)) {
                            target = other;
                            break;
                        }
                    }
                }
                if (target == null) {
                    double nearest = Double.POSITIVE_INFINITY;
                    for (var entity : client.level.entitiesForRendering()) {
                        if (!(entity instanceof LivingEntity other)
                                || !validOrbitTarget(other, player)) continue;
                        double distance = player.distanceToSqr(other);
                        if (distance < nearest) {
                            nearest = distance;
                            target = other;
                        }
                    }
                    UUID newId = target == null ? null : target.getUUID();
                    if (newId != null && !newId.equals(combatTargetId)) {
                        combatOrbitDirection = -combatOrbitDirection;
                        orbitAngleRadians = Math.atan2(player.getZ() - target.getZ(),
                                player.getX() - target.getX());
                        orbitDiving = false;
                        orbitDiveRecoveryTicks = 0;
                        orbitSmoothedVelocity = player.getDeltaMovement();
                        orbitLastTargetPosition = null;
                        orbitLastTargetId = null;
                    }
                    combatTargetId = newId;
                }
                if (target != null) {
                    combatAttackTicks++;
                    if (orbitAttackCooldownTicks > 0) orbitAttackCooldownTicks--;
                    if (orbitDiveRecoveryTicks > 0) orbitDiveRecoveryTicks--;

                    // Extrapolate from the latest received movement. Fast gliding
                    // targets need a larger lead than walking targets, but limit
                    // prediction error when movement packets jump unexpectedly.
                    Vec3 targetPosition = target.position();
                    Vec3 predictedTarget = predictOrbitTarget(target,
                            orbitLastTargetPosition, orbitLastTargetId);
                    orbitLastTargetId = target.getUUID();
                    orbitLastTargetPosition = targetPosition;
                    Vec3 delta = targetPosition.subtract(player.position());
                    double radius = Math.hypot(delta.x, delta.z);
                    boolean wieldingMace = player.getMainHandItem().is(Items.MACE);

                    // If a flying player escapes the smash by several blocks,
                    // abandon that dive and immediately reacquire their position.
                    if (orbitDiving && player.distanceToSqr(target) > 10.0 * 10.0) {
                        orbitDiving = false;
                        orbitSmoothedVelocity = Vec3.ZERO;
                    }
                    // Reposition right beside the tracked flying target when
                    // not diving, so the attack packet is sent from close range.
                    if (!orbitDiving && attemptOrbitSnap(client, player, target, predictedTarget)) {
                        orbitSmoothedVelocity = Vec3.ZERO;
                        delta = target.position().subtract(player.position());
                        radius = Math.hypot(delta.x, delta.z);
                    }
                    {
                    // Use smoothed motion only after arriving near the target.
                    Vec3 inward = radius > 0.001
                            ? new Vec3(delta.x / radius, 0, delta.z / radius)
                            : new Vec3(-Math.sin(Math.toRadians(player.getYRot())),
                                    0, Math.cos(Math.toRadians(player.getYRot())));
                    Vec3 tangent = new Vec3(-inward.z, 0, inward.x)
                            .scale(combatOrbitDirection);
                    double desiredRadius = wieldingMace ? 1.4 : ORBIT_RADIUS;
                    double correction = Math.max(-0.55, Math.min(0.95,
                            (radius - desiredRadius) * 0.24));
                    // Orbit contribution fades naturally when far away.
                    double orbitBlend = Math.max(0.0, Math.min(1.0, 13.0 / Math.max(13.0, radius)));
                    // 64 blocks/tick is the upper limit, not a forced speed:
                    // at close range brake before overshooting the small orbit.
                    // Target pursuit remains teleport-based, not cruise flight.
                    Vec3 horizontal = tangent.scale(ORBIT_SPEED * orbitBlend)
                            .add(inward.scale(correction));
                    double horizontalLimit = Math.min(ORBIT_MAX_HORIZONTAL_SPEED,
                            Math.max(wieldingMace ? 3.5 : 0.75,
                                    radius * (wieldingMace ? 0.85 : 0.32)));
                    if (horizontal.lengthSqr() > horizontalLimit * horizontalLimit) {
                        horizontal = horizontal.normalize().scale(horizontalLimit);
                    }

                    double vertical;
                    if (wieldingMace) {
                        // Rise quickly, but only start a dive after attack
                        // recharge. A mace hit is attempted on the descent, not
                        // merely because the orbit passes close to the target.
                        double heightAboveTarget = player.getY() - predictedTarget.y;
                        boolean weaponCharged = player.getAttackStrengthScale(0.0f) >= 0.99f;
                        if (!orbitDiving && orbitDiveRecoveryTicks == 0
                                && weaponCharged && radius < 6.0
                                && heightAboveTarget >= ORBIT_DIVE_HEIGHT - 0.35) {
                            orbitDiving = true;
                        }
                        if (orbitDiving) {
                            // Do not reset fallDistance while diving: the
                            // server determines whether the hit is a smash.
                            player.setNoGravity(false);
                            vertical = -Math.min(ORBIT_DIVE_DESCEND_SPEED,
                                    Math.max(2.5, heightAboveTarget * 0.85));
                            if (heightAboveTarget < -0.8 || player.onGround()) {
                                orbitDiving = false;
                                orbitDiveRecoveryTicks = 3;
                            }
                        } else {
                            player.setNoGravity(true);
                            vertical = Math.max(-0.10, Math.min(ORBIT_DIVE_ASCEND_SPEED,
                                    (predictedTarget.y + ORBIT_DIVE_HEIGHT - player.getY()) * 0.65));
                            player.fallDistance = 0;
                        }
                    } else {
                        orbitDiving = false;
                        player.setNoGravity(true);
                        // Orbit alongside the target at roughly the same
                        // altitude, not above their head.
                        double desiredHeight = predictedTarget.y + 0.3;
                        vertical = Math.max(-COMBAT_VERTICAL_SPEED,
                                Math.min(COMBAT_VERTICAL_SPEED,
                                        (desiredHeight - player.getY()) * 0.35));
                        player.fallDistance = 0;
                    }
                    // During a mace dive actively track the predicted flight path,
                    // rather than circling the target and falling behind.
                    if (wieldingMace && orbitDiving) {
                        Vec3 follow = predictedTarget.subtract(player.position());
                        Vec3 pursuit = new Vec3(follow.x, 0, follow.z).scale(0.72);
                        double maxPursuit = 12.0;
                        if (pursuit.lengthSqr() > maxPursuit * maxPursuit) {
                            pursuit = pursuit.normalize().scale(maxPursuit);
                        }
                        horizontal = pursuit;
                    }
                    // Limit acceleration while permitting fast course correction
                    // against targets moving many blocks in a single tick.
                    Vec3 wantedVelocity = new Vec3(horizontal.x, vertical, horizontal.z);
                    Vec3 velocityDifference = wantedVelocity.subtract(orbitSmoothedVelocity);
                    double accelerationLimit = wieldingMace
                            ? (orbitDiving ? 8.0 : 4.0) : ORBIT_ACCELERATION;
                    if (velocityDifference.lengthSqr() > accelerationLimit * accelerationLimit) {
                        velocityDifference = velocityDifference.normalize().scale(accelerationLimit);
                    }
                    orbitSmoothedVelocity = orbitSmoothedVelocity.add(velocityDifference);
                    player.setDeltaMovement(orbitSmoothedVelocity);
                    }

                    // Aim at the target's hitbox instead of chasing its eyes.
                    // Predict movement while far away, but use the current
                    // hitbox during a close strike to avoid leading past it.
                    double targetDistance = player.distanceTo(target);
                    double predictionBlend = Math.max(0.0,
                            Math.min(1.0, (targetDistance - 2.5) / 9.0));
                    // During a quick mace dive, prioritise the live hitbox.
                    if (wieldingMace && orbitDiving && targetDistance < 7.0) {
                        predictionBlend *= 0.25;
                    }
                    Vec3 hitboxCenter = target.getBoundingBox().getCenter();
                    Vec3 aimPoint = hitboxCenter.add(
                            predictedTarget.subtract(targetPosition).scale(predictionBlend));
                    Vec3 aim = aimPoint.subtract(player.getEyePosition());
                    double flatAim = Math.hypot(aim.x, aim.z);
                    if (aim.lengthSqr() > 0.0001) {
                        float desiredYaw = (float) Math.toDegrees(Math.atan2(-aim.x, aim.z));
                        float desiredPitch = (float) -Math.toDegrees(Math.atan2(aim.y, flatAim));
                        float yawDifference = (desiredYaw - player.getYRot() + 540.0f) % 360.0f - 180.0f;
                        float pitchDifference = desiredPitch - player.getXRot();
                        // Error-proportional, capped turning: small changes are
                        // smooth, while large target dodges catch up rapidly.
                        // Keep pitch slightly slower to avoid camera jolts.
                        float yawStep = Math.max(-14.0f, Math.min(14.0f, yawDifference * 0.32f));
                        float pitchStep = Math.max(-10.0f, Math.min(10.0f, pitchDifference * 0.27f));
                        player.setYRot(player.getYRot() + yawStep);
                        player.setXRot(Math.max(-90.0f, Math.min(90.0f,
                                player.getXRot() + pitchStep)));
                    }

                    // Never limit the attack packet by client distance, but
                    // the server still validates reach. In particular, allow
                    // an ordinary charged mace hit when the target is flying
                    // away: requiring a full smash for every hit can cause
                    // Orbit Combat to miss indefinitely.
                    boolean descendingSmash = orbitDiving
                            && !player.onGround()
                            && player.getDeltaMovement().y < -0.10
                            && player.fallDistance > 1.5f;
                    boolean maceReady = !wieldingMace || descendingSmash;
                    if (player.hasLineOfSight(target)
                            && maceReady && orbitAttackCooldownTicks == 0
                            && player.getAttackStrengthScale(0.0f) >= 0.99f) {
                        client.gameMode.attack(player, target);
                        player.swing(InteractionHand.MAIN_HAND);
                        orbitAttackCooldownTicks = 5;
                        if (wieldingMace) {
                            orbitDiving = false;
                            orbitDiveRecoveryTicks = 3;
                        }
                    }
                } else {
                    // No nearby target: do not leave the player hovering indefinitely.
                    combatTargetId = null;
                    combatAttackTicks = 0;
                    orbitAttackCooldownTicks = 0;
                    orbitDiving = false;
                    orbitDiveRecoveryTicks = 0;
                    orbitSmoothedVelocity = Vec3.ZERO;
                    orbitLastTargetPosition = null;
                    orbitLastTargetId = null;
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
                // Maintain a separate camera path, easing starts/stops instead of
                // jumping 0.75 blocks once every 50 ms. The renderer interpolates
                // between these tick positions for smooth motion at high FPS.
                Vec3 targetVelocity = movement.lengthSqr() > 0
                        ? movement.normalize().scale(FREECAM_SPEED) : Vec3.ZERO;
                freecamPreviousPosition = freecamPosition;
                if (movement.lengthSqr() > 0) {
                    freecamVelocity = freecamVelocity.lerp(targetVelocity, FREECAM_ACCELERATION);
                } else {
                    freecamVelocity = freecamVelocity.scale(FREECAM_FRICTION);
                    if (freecamVelocity.lengthSqr() < 0.00001) freecamVelocity = Vec3.ZERO;
                }
                freecamPosition = freecamPosition.add(freecamVelocity);
                // Do not force key mappings up here: that causes alternating input
                // states and visibly jerky camera movement while keys are held.
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
                // Client-side collision phasing for both normal cruise and
                // TP-to-XYZ cruise fallback. Server collision still applies.
                player.noPhysics = true;
                cruisePhaseApplied = true;
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
                    double speedLimit = highSpeedPlayerTravel
                            ? PLAYER_TRAVEL_SPEED_PER_TICK : coordinateFlightMaxSpeed;
                    // Do not stall near a waypoint when the configured cruise speed
                    // is high. Bound each velocity to the remaining distance.
                    double travelSpeed = highSpeedPlayerTravel
                            ? Math.min(speedLimit, remaining)
                            : Math.min(speedLimit, Math.max(0.30, remaining * 0.35));
                    player.setNoGravity(true);
                    player.setDeltaMovement(remaining > 0.001
                            ? delta.normalize().scale(travelSpeed) : Vec3.ZERO);
                    player.setOnGround(false);
                    player.fallDistance = 0;
                    if (coordinateFlightLastPosition != null
                            && currentPosition.distanceToSqr(coordinateFlightLastPosition) < 0.0025) {
                        coordinateFlightStuckTicks++;
                    } else {
                        coordinateFlightStuckTicks = 0;
                    }
                    coordinateFlightLastPosition = currentPosition;
                    if (coordinateFlightStuckTicks > 60) {
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
    // Visit the closest columns first (0, +1, -1, +2, -2 ...).
    // This shows nearby storage sooner than scanning from the far corner.
    private static int centeredScanOffset(int index) {
        if (index == 0) return 0;
        int offset = (index + 1) / 2;
        return (index & 1) == 1 ? offset : -offset;
    }

    private static boolean isStorageBlock(net.minecraft.world.level.block.state.BlockState state) {
        return state.is(Blocks.CHEST) || state.is(Blocks.TRAPPED_CHEST)
                || state.is(Blocks.BARREL) || state.is(Blocks.ENDER_CHEST)
                || state.is(BlockTags.SHULKER_BOXES)
                || state.is(Blocks.HOPPER) || state.is(Blocks.DROPPER)
                || state.is(Blocks.DISPENSER) || state.is(Blocks.FURNACE)
                || state.is(Blocks.BLAST_FURNACE) || state.is(Blocks.SMOKER)
                || state.is(Blocks.BREWING_STAND) || state.is(Blocks.CRAFTER);
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
        fly = true; // Hold position in mid-air instead of requiring ground below.
        boatFly = false;
        orbitCombat = false;
        freecam = false;
        speed = false;
        coordinateDestination = new Vec3(x, y, z);
        highSpeedPlayerTravel = false;
        coordinateFlightCruiseY = Math.max(200.0, Math.max(mc.player.getY(), y));
        coordinateFlightStage = mc.player.getY() < coordinateFlightCruiseY - 0.8 ? 0 : 1;
        coordinateFlightStuckTicks = 0;
        coordinateFlightLastPosition = null;
        return true;
    }

    /**
     * Attempts a direct coordinate teleport through the normal player movement
     * protocol. No server command or guaranteed permission is involved: the
     * server remains authoritative and may rubber-band the player.
     */
    public static String getCoordinateTeleportError() {
        return coordinateTeleportError;
    }

    public static String getLastTeleportStatus() {
        return lastTeleportStatus;
    }

    private static void reportTeleport(Minecraft mc, String status) {
        lastTeleportStatus = status;
        if (mc.player != null) {
            // Chat is persistent; action-bar-only messages were easy to miss.
            mc.player.displayClientMessage(
                    Component.literal("§b[PortalFlight TP] §f" + status), false);
        }
    }

    public static boolean attemptCoordinateTeleport(double x, double y, double z) {
        Minecraft mc = Minecraft.getInstance();
        coordinateTeleportError = "Invalid coordinates or not in a world.";
        if (mc.player == null || mc.level == null || mc.getConnection() == null
                || mc.player.getVehicle() != null
                || !Double.isFinite(x) || !Double.isFinite(y) || !Double.isFinite(z)
                || Math.abs(x) > 29999984 || Math.abs(z) > 29999984
                || y < -2048 || y > 2048) return false;

        // Never place the player inside blocks when collision data is available.
        // A solid floor is not required: the destination may be in mid-air.
        BlockPos feet = BlockPos.containing(x, y, z);
        if (mc.level.hasChunkAt(feet)) {
            boolean occupied = !mc.level.getBlockState(feet)
                    .getCollisionShape(mc.level, feet).isEmpty()
                    || !mc.level.getBlockState(feet.above())
                    .getCollisionShape(mc.level, feet.above()).isEmpty();
            if (occupied) {
                coordinateTeleportError = "Destination blocked (feet or head). TP not sent.";
                return false;
            }
        }
        // Open air is a valid destination: Fly is enabled immediately.
        // Unloaded chunks cannot be collision-checked client-side;
        // the server may still reject the teleport or correct a bad position.

        stopCoordinateFlight();
        disableNoclip(mc);
        fly = true;
        boatFly = false;
        orbitCombat = false;
        freecam = false;
        speed = false;
        pendingTeleportOrigin = mc.player.position();
        Vec3 destination = new Vec3(x, y, z);
        double distance = pendingTeleportOrigin.distanceTo(destination);

        // Meteor ClickTP-style packet sequence applied to typed XYZ instead
        // of a clicked block. For distances up to 200 blocks send up to 19
        // on-ground status packets, then a single position packet.
        // Meteor sends no extra status packets when more than 19 would be
        // needed. This is not a server teleport command or a guarantee.
        int statusPackets = (int) Math.ceil(distance / 10.0) - 1;
        if (statusPackets > 19) statusPackets = 0;
        statusPackets = Math.max(0, statusPackets);

        mc.player.setDeltaMovement(Vec3.ZERO);
        mc.player.setNoGravity(true);
        for (int i = 0; i < statusPackets; i++) {
            mc.getConnection().send(new ServerboundMovePlayerPacket.StatusOnly(true, true));
        }
        mc.getConnection().send(new ServerboundMovePlayerPacket.Pos(
                x, y, z, true, true));
        mc.player.setPos(x, y, z);
        mc.player.setNoGravity(true);
        // This local reset cannot disable any fall damage assessed by AVAS.
        mc.player.fallDistance = 0;

        pendingTeleportDestination = destination;
        pendingTeleportCheckTicks = 40;
        coordinateTeleportError = "";
        reportTeleport(mc, "Meteor-style XYZ TP sent (" + statusPackets
                + " status packets + 1 position packet) to X=" + (int) Math.floor(x)
                + " Y=" + (int) Math.floor(y) + " Z=" + (int) Math.floor(z)
                + ". Fly enabled; checking for correction in ~2 seconds.");
        return true;
    }

    /** Fly to a client-visible player's current coordinates at up to 50,000 blocks/s.
     * This is movement, not teleportation; the server may reject it.
     */
    public static Vec3 getLastSeenPlayerPosition(UUID playerId) {
        return lastSeenPlayerPositions.get(playerId);
    }

    public static boolean startPlayerTravel(double x, double y, double z) {
        if (!startCoordinateFlight(x, y, z)) return false;
        highSpeedPlayerTravel = true;
        // Player travel ascends to Y=400 before crossing X/Z, then descends.
        // Stay above 400 if already higher, or if the target is higher.
        Minecraft mc = Minecraft.getInstance();
        coordinateFlightCruiseY = Math.max(400.0, Math.max(mc.player.getY(), y));
        coordinateFlightStage = mc.player.getY() < coordinateFlightCruiseY - 0.8 ? 0 : 1;
        return true;
    }

    public static void stopCoordinateFlight() {
        if (coordinateDestination == null && !cruisePhaseApplied) return;
        coordinateDestination = null;
        Minecraft mc = Minecraft.getInstance();
        if (cruisePhaseApplied && mc.player != null && !noclip) {
            mc.player.noPhysics = false;
        }
        cruisePhaseApplied = false;
        highSpeedPlayerTravel = false;
        coordinateFlightStage = 0;
        coordinateFlightStuckTicks = 0;
        coordinateFlightLastPosition = null;
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
            case "reach" -> weaponReach;
            case "orbitmobs" -> orbitTargetMobs;
            case "nofall" -> noFall;
            case "hunger" -> hungerSaver;
            case "privacy" -> privacyMode;
            case "portal" -> portalScan;
            case "autototem" -> autoTotem;
            case "autoleave" -> autoLeave;
            case "autowalk" -> autoWalk;
            case "autojump" -> autoJump;
            case "highjump" -> highJump;
            case "fastfall" -> fastFall;
            case "waterwalk" -> waterWalk;
            case "autosneak" -> autoSneak;
            case "noslow" -> noSlow;
            case "elytraboost" -> elytraBoost;
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
                    beginFreecam(mc.player);
                }
            }
            case "players" -> playerEsp = !playerEsp;
            case "mobs" -> mobEsp = !mobEsp;
            case "tracers" -> espTracers = !espTracers;
            case "storage" -> {
                storageFinder = !storageFinder;
                nearbyStorage.clear();
                storageScanResults.clear();
                lastStorageScanOrigin = null;
                storageScanInProgress = false;
                storageScanIndex = 0;
                storageTick = 0;
            }
            case "orbitmobs" -> {
                orbitTargetMobs = !orbitTargetMobs;
                combatTargetId = null;
                orbitLastTargetPosition = null;
                orbitLastTargetId = null;
                orbitDiving = false;
                orbitSmoothedVelocity = Vec3.ZERO;
            }
            case "reach" -> {
                weaponReach = !weaponReach;
                if (mc.player != null) updateWeaponReach(mc.player);
            }
            case "orbit" -> {
                orbitCombat = !orbitCombat;
                combatAttackTicks = 0;
                orbitAttackCooldownTicks = 0;
                orbitDiving = false;
                orbitDiveRecoveryTicks = 0;
                combatTargetId = null;
                orbitSmoothedVelocity = Vec3.ZERO;
                orbitLastTargetPosition = null;
                orbitLastTargetId = null;
                if (orbitCombat) { disableNoclip(mc); fly = false; freecam = false; }
                else if (mc.player != null) mc.player.setNoGravity(false);
            }
            case "nofall" -> noFall = !noFall;
            case "hunger" -> hungerSaver = !hungerSaver;
            case "privacy" -> privacyMode = !privacyMode;
            case "portal" -> { portalScan = !portalScan; nearestFrame = null; }
            case "autototem" -> { autoTotem = !autoTotem; autoTotemDelayTicks = 0; }
            case "autoleave" -> autoLeave = !autoLeave;
            case "autowalk" -> autoWalk = !autoWalk;
            case "autojump" -> { autoJump = !autoJump; autoJumpWasGrounded = false; autoJumpIntervalTicks = 0; }
            case "highjump" -> highJump = !highJump;
            case "fastfall" -> fastFall = !fastFall;
            case "waterwalk" -> waterWalk = !waterWalk;
            case "autosneak" -> { autoSneak = !autoSneak; if (!autoSneak && mc.player != null && !mc.options.keyShift.isDown()) mc.player.setShiftKeyDown(false); }
            case "noslow" -> noSlow = !noSlow;
            case "elytraboost" -> elytraBoost = !elytraBoost;
        }
    }

    public static double getWeaponReachDistance() {
        return WEAPON_REACH_VALUES[weaponReachIndex];
    }

    public static void cycleWeaponReachDistance() {
        weaponReachIndex = (weaponReachIndex + 1) % WEAPON_REACH_VALUES.length;
        Minecraft mc = Minecraft.getInstance();
        if (mc.player != null) updateWeaponReach(mc.player);
    }

    private static void updateWeaponReach(LocalPlayer player) {
        AttributeInstance attribute = player.getAttribute(Attributes.ENTITY_INTERACTION_RANGE);
        if (attribute == null) return;
        AttributeModifier existing = attribute.getModifier(WEAPON_REACH_MODIFIER);
        if (!weaponReach) {
            if (existing != null) attribute.removeModifier(WEAPON_REACH_MODIFIER);
            return;
        }
        double additionalReach = getWeaponReachDistance() - 3.0;
        if (existing == null || Math.abs(existing.amount() - additionalReach) > 0.001) {
            attribute.addOrUpdateTransientModifier(new AttributeModifier(
                    WEAPON_REACH_MODIFIER, additionalReach, AttributeModifier.Operation.ADD_VALUE));
        }
    }

    public static float getAutoLeaveThreshold() {
        return autoLeaveThreshold;
    }

    public static void toggleAutoLeaveThreshold() {
        autoLeaveThreshold = autoLeaveThreshold == 2.0f ? 1.0f : 2.0f;
    }

    /**
     * One re-lock position attempt beside and above a client-known target.
     * The server remains authoritative and can reject the movement packet.
     */
    private static boolean validOrbitTarget(LivingEntity target, LocalPlayer localPlayer) {
        if (target == localPlayer || !target.isAlive() || target.isRemoved()) return false;
        if (orbitTargetMobs) return target instanceof Mob;
        return target instanceof Player other && !other.isSpectator();
    }

    private static Vec3 predictOrbitTarget(LivingEntity target,
            Vec3 previousPosition, UUID previousId) {
        Vec3 now = target.position();
        Vec3 displacement = target.getDeltaMovement();
        if (previousPosition != null && target.getUUID().equals(previousId)) {
            Vec3 observed = now.subtract(previousPosition);
            if (observed.lengthSqr() > 0.0001) displacement = observed;
        }
        // Extrapolate at most 12 blocks/tick of server-supplied movement.
        // An enormous position correction should not cause a huge overshoot.
        double speed = displacement.length();
        if (speed > 12.0) displacement = displacement.scale(12.0 / speed);
        double ticksAhead = Math.min(2.0, 0.75 + speed * 0.12);
        return now.add(displacement.scale(ticksAhead));
    }

    private static boolean attemptOrbitSnap(Minecraft client, LocalPlayer player,
            LivingEntity target, Vec3 predictedTarget) {
        if (client.getConnection() == null || client.level == null
                || target.isRemoved() || !target.isAlive()) return false;
        // Rotate the snap destination around the target instead of repeatedly
        // teleporting to the same spot above/alongside them. The non-mace
        // orbit stays at target height; mace mode retains its high launch.
        boolean mace = player.getMainHandItem().is(Items.MACE);
        double angle = orbitAngleRadians + combatOrbitDirection * ORBIT_STEP_RADIANS;
        double offset = mace ? 0.85 : ORBIT_RADIUS;
        Vec3 destination = predictedTarget.add(
                Math.cos(angle) * offset,
                mace ? ORBIT_DIVE_HEIGHT : 0.3,
                Math.sin(angle) * offset);
        if (player.position().distanceToSqr(destination) < 0.20 * 0.20) {
            return false; // Avoid resending identical teleports.
        }
        BlockPos feet = BlockPos.containing(destination);
        if (!client.level.hasChunkAt(feet)
                || !client.level.getBlockState(feet)
                        .getCollisionShape(client.level, feet).isEmpty()
                || !client.level.getBlockState(feet.above())
                        .getCollisionShape(client.level, feet.above()).isEmpty()) return false;
        orbitAngleRadians = angle;
        player.setPos(destination.x, destination.y, destination.z);
        player.setDeltaMovement(Vec3.ZERO);
        player.fallDistance = 0;
        client.getConnection().send(new ServerboundMovePlayerPacket.PosRot(
                destination.x, destination.y, destination.z,
                player.getYRot(), player.getXRot(), false, false));
        return true;
    }

    private static void beginFreecam(LocalPlayer player) {
        freecamPosition = player.getEyePosition();
        freecamPreviousPosition = freecamPosition;
        freecamVelocity = Vec3.ZERO;
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
