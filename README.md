# PortalFlight — Fabric 1.21.11

Client-side Minecraft utility mod source project.

## New ESP and utility modules

These are original PortalFlight implementations inspired by common functionality in Anubis, Meteor and Krypton; no third-party client source code was copied. Entity ESP outlines only entities the server sends to your client. Storage ESP draws wireframe boxes around nearby client-loaded chests, trapped chests, barrels and Ender Chests (24-block horizontal / 12-block vertical scan); it is not guaranteed to render through walls with the current line render pass. No auto-combat or anti-cheat bypass added.

## ClickTP (experimental)

Press **Right Ctrl → Movement → ClickTP (R-Click)** or **K** to toggle ClickTP. With ClickTP on, aim at a block within **200 blocks** and **right-click** to send a player-position movement packet to the block's top surface. No /tp command or operator permission is used. ClickTP checks that the destination chunk is loaded and has two blocks of headroom. It makes one attempt per click, not a stream of teleport packets. This is a client request: **the server can reject or correct it**, especially across large distances. Right-click still performs ordinary item interactions, so switch the module off when not needed. This is not Fly to Player and cannot discover players' hidden coordinates.

## Orbit Combat

Press **F12** or use **Right Ctrl → Combat → Orbit Combat**. The aggressive aerial mode acquires client-visible players up to your configured Minecraft **render distance** (16 blocks per chunk), subject to the server's entity-tracking range, flies in a roughly **4.25-block radius** at up to **2.3 blocks per tick** horizontally, and swoops vertically around the target. It attempts attacks within **6 blocks** with line of sight and nearly full weapon cooldown. The server still checks actual reach, attack damage, movement speed and physics; even without a dedicated anti-cheat, extra reach and fast flying may not work. It is not a guaranteed one-hit or remote-damage exploit.

## ClickGUI

Press **Right Ctrl** to open the dark PortalFlight menu. Click **Movement**, **Render**, **Combat**, or **Utility**, then click a module to turn it ON/OFF. Press **Esc** to close. The menu controls the same toggles as the keyboard shortcuts and includes all actual implemented modules; Boat Fly Descend (X) remains a hold-to-use key in Minecraft's Key Binds screen. The interface is original and uses no NiceWurst code or modules.

## Keybinds

- **Right Ctrl** — Open the PortalFlight ClickGUI (rebindable under Miscellaneous)

- **C** — Toggle experimental local Noclip: movement keys fly through client-side collision, Space up and Shift down. This is intended for singleplayer; a multiplayer server can reject the movement. Toggle off before leaving a wall to avoid becoming stuck. Minecraft's server may still perform collision checks.

- **F12** — Orbit Combat: follows nearby non-spectator players up to 8 blocks away while flying in an orbit, auto-aims, and attempts frequent melee attacks within 3 blocks and line of sight. A vertical orbit cycle may create falling-hit opportunities. Attacks are no longer gated by the client's weapon cooldown, but servers still apply normal damage/critical rules. Toggle off with F12. Avoid using in combination with Fly or Freecam.

All PortalFlight module toggles and Boat Fly Descend are rebindable in **Options → Controls → Key Binds**, under **Miscellaneous**. Movement, jump, sneak and sprint use Minecraft's own rebindable controls.

- **F7** — Player ESP (client-side cyan bounding boxes, optional tracer lines)
- **F8** — Mob ESP (client-side orange bounding boxes, optional tracer lines)
- **F11** — Toggle ESP Tracers (enabled by default; draws a line from your view to outlined players and mobs)
- **F9** — Storage Finder (coordinates of nearby loaded chests/barrels)
- **F10** — Auto Sprint
- **R** — Toggle very fast Swim Boost (in water; use movement keys and Jump/Sneak to steer vertically)
- **V** — Speed boost
- **B** — Toggle Boat Fly; Space rises, **X descends** (rebindable in controls; Shift still dismounts). Boat Fly now accelerates vertically smoothly and pauses if a large unexpected vehicle position change suggests a server correction.
- **N** — Find nearby *loaded* end portal frames (coordinates only, not through-wall ESP)
- **G** — Toggle normal Fly; Space rises, Shift descends
- **Ctrl while moving** — Faster Fly / Sprint Fly
- **F6** — Toggle Freecam
- **P** — Toggle Spectator Privacy / Staff Watch (enabled by default). Checks every 0.5 seconds for spectator-mode players and invisible player entities within 128 blocks. Shows a persistent action-bar warning while a signal is present. Visible spectators trigger automatic disabling of movement/combat modules; invisible-player sightings only warn because invisibility does not prove someone is staff.
- **J** — Toggle Safe Fall (enabled by default). While airborne and falling, sends an on-ground status packet and resets client fall distance; this does not guarantee fall protection on servers.
- **H** — Toggle Hunger Saver indicator; this does not disable sprinting or change server hunger

## Building

Every push to main invokes GitHub Actions **Build PortalFlight**. Get the built JAR from the `PortalFlight-Mod` artifact on the [Actions page](https://github.com/baksyaaaaa/nothing/actions) **only if the workflow succeeds**. Use the JAR without `-sources` in your Fabric mods folder, alongside compatible Fabric API.

**Important limitations:** This is experimental, not yet verified to compile or work in-game. Movement and damage/hunger are server-authoritative on multiplayer servers; this mod doesn't grant server permissions or bypass server restrictions. Freecam is experimental. Hunger Saver currently does not implement server-side hunger prevention.

## Staff Watch panel and public vanish mentions

Open **Right Ctrl → Utility → Staff Watch - View alerts** to review current-world warning history (up to 12 recent entries) and the latest client-visible status. The panel records detected spectator entities, invisible player entities, and chat/game messages received by your client that contain the word "vanish". These messages are **not proof** someone used the `/vanish` command: normal players can also type that word, and servers can send arbitrary messages.

Minecraft clients generally cannot see other players' private commands. If a server doesn't broadcast the use of `/vanish` or expose some observable change, PortalFlight cannot know that the command was executed. It never labels a hidden moderator as confirmed solely from this hint.

## Fly to Player (no commands required)

Open **Right Ctrl → Movement → Fly to Player** to see online player names from the server tab list, including players outside render distance. The Fly button works only when the client previously saw that player's coordinates in the current world and session; otherwise it shows **coords unknown**. Last-known coordinates may be outdated. Selecting one starts the dedicated Fly to Player mode: climb to Y=400 (or higher if already above 400 or if the target is higher), travel toward the player's X/Z at a configured maximum of 50,000 blocks/second, then descend to their last known Y. This is a theoretical client-side speed, not a guaranteed server-accepted speed. It requires no server commands or operator privileges, but is **not instant teleportation**. The server may correct or reject movement. The client cannot discover coordinates of distant or hidden players that the server never sends to it, and the destination does not automatically update if a player moves.

## Teleport to XYZ (experimental)

Open **Right Ctrl → Movement → Coordinate Flight Settings** (or Utility → Coordinate Flight), enter X/Y/Z, and press **TP to Coordinates (Instant Attempt)** instead of **Fly to Coordinates**. This immediately sends a position packet requesting those coordinates, without following the climb/cruise/descent route. The client moves there optimistically, but the server can reject the position and rubber-band you. If the client is not near the destination after about two seconds, PortalFlight automatically falls back to normal climb/cruise/descent flight and reports the switch. This does not bypass server movement validation; no OP permissions or /tp commands are used. Coordinates are validated to Minecraft's world border range and reasonable vertical bounds.

## Coordinate Flight autopilot

Open **Right Ctrl → Utility → Coordinate Flight - Fly to XYZ**. Enter destination coordinates and click **Fly to Coordinates**. PortalFlight follows three phases: (1) ascend to **Y=200** (or stay higher if already above it or the destination is higher), (2) fly horizontally toward the target X/Z at a configurable maximum speed (default 320 blocks/second; enter any positive custom speed up to 1280 blocks/second in **Right Ctrl → Movement → Coordinate Flight Settings**, saved to `config/portalflight.properties`) of client-predicted movement, then (3) descend or ascend to the target Y. The mode slows near waypoints and stops on arrival or after persistent lack of movement. **STOP Flight** cancels it.

This does not teleport, bypass server movement rules, or guarantee a clear path. It can collide with obstacles and multiplayer servers may rubber-band, block or reject the movement. Canceling at a high altitude can result in a fall.

## Spectator detection limitations

Staff Watch now reports client-visible spectators and invisible players (which may simply be using an invisibility effect). It **cannot** see staff fully hidden by server-side vanish, distinguish ordinary invisible players from moderators, or know whether anyone is watching through a hidden camera. Absence of warnings is **not** evidence that no staff are watching. Alerts depend on the server sending the player entity to your client. Staff Watch is not an anti-ban guarantee.


## ESP rendering notes

Player ESP (F7) and Mob ESP (F8) render wireframe bounding boxes around entities already available to the Minecraft client. ESP Tracers (F11, enabled by default) draws an optional line toward each selected target. The rendering uses camera-relative coordinates and the Fabric 1.21.11 render-context matrix. This is not guaranteed through-wall visibility and does not reveal hidden or unloaded entities.

## Boat Fly stability

Boat Fly adjusts vertical velocity gradually while leaving vanilla horizontal boat controls intact. It tracks the boat between ticks and automatically turns off if an unexpectedly large position change may indicate a server correction. This is a heuristic, not guaranteed detection. It uses vanilla networking and does **not** send fake grounded packets or bypass server anti-cheat. Multiplayer servers can still reject the motion.
