package dev.chromeflight;

import com.mojang.brigadier.arguments.StringArgumentType;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.world.phys.Vec3;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Properties;

/**
 * Per-server, per-dimension client-only home markers.
 * All commands execute locally; no server commands or teleport permissions.
 */
public final class HomeCommands {
    private static final Path FILE = FabricLoader.getInstance().getConfigDir()
            .resolve("portalflight-homes.properties");
    private static final Properties HOMES = new Properties();
    private static boolean loaded;

    private HomeCommands() { }

    public static void register() {
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> {
            dispatcher.register(ClientCommandManager.literal("sethome")
                    .executes(ctx -> setHome("home"))
                    .then(ClientCommandManager.argument("name", StringArgumentType.word())
                            .executes(ctx -> setHome(StringArgumentType.getString(ctx, "name")))));
            dispatcher.register(ClientCommandManager.literal("home")
                    .executes(ctx -> goHome("home"))
                    .then(ClientCommandManager.argument("name", StringArgumentType.word())
                            .executes(ctx -> goHome(StringArgumentType.getString(ctx, "name")))));
            dispatcher.register(ClientCommandManager.literal("delhome")
                    .executes(ctx -> deleteHome("home"))
                    .then(ClientCommandManager.argument("name", StringArgumentType.word())
                            .executes(ctx -> deleteHome(StringArgumentType.getString(ctx, "name")))));
            dispatcher.register(ClientCommandManager.literal("homes")
                    .executes(ctx -> listHomes()));
        });
    }

    private static synchronized void load() {
        if (loaded) return;
        loaded = true;
        if (!Files.isRegularFile(FILE)) return;
        try (InputStream in = Files.newInputStream(FILE)) {
            HOMES.load(in);
        } catch (IOException ex) {
            notifyPlayer("Couldn't load saved homes: " + ex.getMessage());
        }
    }

    private static synchronized boolean save() {
        try {
            Files.createDirectories(FILE.getParent());
            try (OutputStream out = Files.newOutputStream(FILE)) {
                HOMES.store(out, "PortalFlight client-side homes");
            }
            return true;
        } catch (IOException ex) {
            notifyPlayer("Couldn't save homes: " + ex.getMessage());
            return false;
        }
    }

    private static String scope() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null) return null;
        // Server IP and dimension prevent accidental travel between unrelated worlds.
        // Singleplayer worlds are kept in a separate local namespace.
        String server = mc.getCurrentServer() != null
                ? mc.getCurrentServer().ip.toLowerCase(Locale.ROOT) : "singleplayer";
        return server + "|" + mc.level.dimension().toString() + "|";
    }

    private static String normalizeName(String name) {
        String normalized = name.toLowerCase(Locale.ROOT);
        return normalized.matches("[a-z0-9_-]{1,32}") ? normalized : null;
    }

    private static int setHome(String name) {
        Minecraft mc = Minecraft.getInstance();
        String prefix = scope();
        String safeName = normalizeName(name);
        if (prefix == null || safeName == null) {
            notifyPlayer("Join a world and use a name with 1-32 letters, digits, _ or -.");
            return 0;
        }
        load();
        Vec3 p = mc.player.position();
        String key = prefix + safeName;
        String previous = HOMES.getProperty(key);
        HOMES.setProperty(key, p.x + "," + p.y + "," + p.z);
        if (!save()) {
            if (previous == null) HOMES.remove(key);
            else HOMES.setProperty(key, previous);
            return 0;
        }
        notifyPlayer("Saved home '" + safeName + "' at " + format(p));
        return 1;
    }

    private static int goHome(String name) {
        String prefix = scope();
        String safeName = normalizeName(name);
        if (prefix == null || safeName == null) {
            notifyPlayer("Invalid home name or no world loaded.");
            return 0;
        }
        load();
        String value = HOMES.getProperty(prefix + safeName);
        if (value == null) {
            notifyPlayer("Home '" + safeName + "' not found. Use /homes.");
            return 0;
        }
        Vec3 p;
        try {
            String[] coords = value.split(",", -1);
            if (coords.length != 3) throw new IllegalArgumentException("Wrong XYZ count");
            p = new Vec3(Double.parseDouble(coords[0]),
                    Double.parseDouble(coords[1]), Double.parseDouble(coords[2]));
        } catch (RuntimeException ex) {
            notifyPlayer("Home '" + safeName + "' has invalid coordinates.");
            return 0;
        }
        if (!PortalFlight.startCoordinateFlight(p.x, p.y, p.z)) {
            notifyPlayer("Couldn't start cruise to '" + safeName + "'.");
            return 0;
        }
        notifyPlayer("Cruising to home '" + safeName + "' (" + format(p) + ")");
        return 1;
    }

    private static int deleteHome(String name) {
        String prefix = scope();
        String safeName = normalizeName(name);
        if (prefix == null || safeName == null) {
            notifyPlayer("Invalid home name or no world loaded.");
            return 0;
        }
        load();
        String old = (String) HOMES.remove(prefix + safeName);
        if (old == null) {
            notifyPlayer("Home '" + safeName + "' not found.");
            return 0;
        }
        if (!save()) {
            HOMES.setProperty(prefix + safeName, old);
            return 0;
        }
        notifyPlayer("Deleted home '" + safeName + "'.");
        return 1;
    }

    private static int listHomes() {
        String prefix = scope();
        if (prefix == null) {
            notifyPlayer("Join a world to view homes.");
            return 0;
        }
        load();
        List<String> names = new ArrayList<>();
        for (String key : HOMES.stringPropertyNames()) {
            if (key.startsWith(prefix)) names.add(key.substring(prefix.length()));
        }
        Collections.sort(names);
        notifyPlayer(names.isEmpty()
                ? "No homes saved here. Use /sethome [name]."
                : "Homes: " + String.join(", ", names));
        return 1;
    }

    private static String format(Vec3 p) {
        return String.format(Locale.ROOT, "%.1f %.1f %.1f", p.x, p.y, p.z);
    }

    private static void notifyPlayer(String message) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player != null) {
            mc.player.displayClientMessage(Component.literal("§b[PortalFlight Homes] §f" + message), false);
        }
    }
}
