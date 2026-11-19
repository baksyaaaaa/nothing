# PortalFlight — Fabric 1.21.11

Client-side Minecraft utility mod source project.

## Storage ESP range and performance

Storage ESP now checks **loaded chunks up to 256 blocks away horizontally**, across **the full world height loaded in those chunks**. Instead of examining every block in a large radius, it reads the existing **block entity positions** from up to **24 loaded chunks per client tick**, with refreshes about every 3 seconds. It retains existing outlines/tracers during refreshes, and supports up to **4,096** detected containers.

Detected storage includes chests, trapped chests, ender chests, barrels, shulker boxes, hoppers, droppers, dispensers, furnaces, blast furnaces, smokers, brewing stands, and crafters. Only storage blocks represented as loaded **block entities** are discovered. The **server's chunk/view distance remains the real limit**: containers in chunks not sent to the client cannot be detected, no matter how high the setting. These changes have not been build-tested.

## Color-coded ESP tracers

With **Tracers** enabled (F11 or Right Ctrl → Render → Tracers), PortalFlight draws camera-to-target lines for **every enabled ESP category**, not just players:

- **Player ESP** (F7): cyan outlines and cyan tracers for server-visible players
- **Mob ESP** (F8): lime-green outlines and lime-green tracers for client-loaded living non-player entities
- **Storage ESP** (F9): amber/orange outlines and tracers to nearby scanned containers (barrels use a deeper orange)

Each category's ESP switch controls whether its own outlines and tracer lines render. The main **Tracers** switch controls the lines independently of ESP outlines. Player and Mob ESP now have **no extra client-side distance cutoff**: any living entity already tracked and sent to the client can render. Storage ESP scans **64 blocks horizontally and 32 vertically**, up to 1,024 results, spread across multiple ticks to avoid lag spikes. The server still controls player/mob tracking distance, and storage searches only inspect client-loaded chunks; expanding ESP does not reveal unloaded or server-hidden data.

## New ESP and utility modules

These are original PortalFlight implementations inspired by common functionality in Anubis, Meteor and Krypton; no third-party client source code was copied. Entity ESP outlines only entities the server sends to your client. Storage ESP draws wireframe boxes around nearby client-loaded chests, trapped chests, barrels and Ender Chests (64-block horizontal / 32-block vertical incremental scan); it is not guaranteed to render through walls with the current line render pass. No auto-combat or anti-cheat bypass added.

## Additional Movement modules

Open **Right Ctrl → Movement** and use **Next modules** to see the new movement toggles:

- **AutoWalk:** moves forward automatically when no movement key is held
- **AutoJump:** repeats jumps while moving and grounded; combines with HighJump
- **HighJump:** increases the initial jump velocity when jumping
- **FastFall:** accelerates downward while falling, except in water or lava
- **WaterWalk:** applies gentle upward motion while swimming to stay near the surface

All are optional and disabled by default. The movement batch avoids overriding PortalFlight's manual Fly, Coordinate Cruise, Orbit Combat, Noclip and Freecam. Server-side movement checks may correct or reject the client-proposed velocity. Use **Prev modules** to return to the original movement toggles. These are original, limited implementations rather than exact copies of Meteor Client's modules.

**Flying-player hit reliability update:** Client-side attack attempts were already uncapped, so there was no higher reach number to set. Instead, mace re-lock now aims only about **3.4 blocks above** and **0.85 blocks beside** the target rather than 6 blocks above, reducing distance at the moment of an attack. If the mace is charged and the target is within 4.5 blocks, Orbit Combat can try an ordinary hit even without a complete smash dive, preventing endless missed attempts against flying opponents. Server attack-range checks, damage rules and teleport acceptance are unchanged. Actual longer reach cannot be granted by the client alone.

## Weapon Reach

Use **Right Ctrl → Combat → Weapon Reach** to enable extended range for **ordinary manual attacks** (not just Orbit Combat). The separate **Weapon Reach distance** button cycles **6 / 12 / 24 / 48 blocks**, default **12**. This applies a temporary modifier to the local player's entity-interaction-range attribute and removes only that modifier when disabled. It does not permanently modify the player's attributes or change the server's settings.

This extends **client-side crosshair targeting and attack attempts**. **The server still controls real weapon reach and hit registration**. There is no guarantee a player beyond the server's attack range will take damage. Weapons with their own attack-range components may behave differently.

## Horizontal circling around targets

When **Orbit Combat** uses an ordinary weapon, re-lock teleports now advance around a **3.7-block horizontal circle** (rather than repeatedly snapping above the opponent or to the same side). The orbital path follows the target's predicted X/Z position and stays about **0.3 blocks above their feet**, so it moves **around** the target rather than on top of them. The position angle advances on each successful snap while the client has the target loaded. With a **mace**, the existing high launch (12 blocks above) and fast descending smash are preserved as a distinct attack pattern. Server movement validation and collision checks still apply.

## Adaptive Orbit Combat aim

Orbit Combat now aims at the **center of the target's collision box** instead of its eyes. It blends between current and predicted positions: little or no lead at close range, more lead while the target is distant, and reduced lead during a close-range mace dive. Camera tracking is **error-proportional** with capped movement (**14° yaw, 10° pitch per tick**) so small changes stay smooth while large turns catch up faster. The client does not alter the server's damage or reach checks.

## High-speed mace smash and smooth camera

Orbit Combat's mace mode now **launches about 12 blocks above the predicted target position**, rises at up to **3.5 blocks/tick**, then dives toward the target at up to **7 blocks/tick** (up to 140 blocks/sec at 20 TPS). While diving it pursues a flying target horizontally and **does not send re-lock teleports**, so the player's fall distance can accumulate. On a successful-looking descent, the client attempts a fully charged mace attack; actual smash damage, server-accepted velocities, and hit range are server-controlled. The previous close-range ordinary-mace fallback was removed in favor of prioritizing the aerial smash.

The combat camera now turns at no more than **6 degrees yaw and 4 degrees pitch per tick** instead of snapping by up to 90/70 degrees. Rapid location teleports can still make the camera's *position* jump; this change primarily smooths the viewing direction. The implementation has not yet been verified in-game.

## Orbit Combat — fast charged mace dives and predictive aim

**Test on mobs:** Open **Right Ctrl → Combat**, enable **Target Mobs** and **Orbit Combat** (or press **F12** to toggle Orbit Combat). With Target Mobs ON, the orbit locks onto the nearest currently client-loaded mob (such as a zombie, skeleton, or cow) and uses the same snap, tracking, orbit, and charged mace dive routines. Turning Target Mobs OFF restores player-only targeting; changing modes clears the old target lock. This is an exclusive target mode, not both players and mobs simultaneously. Attack range, movement and damage remain server-controlled.


Use **F12** or **Right Ctrl → Combat → Orbit Combat**. Acquires **any currently client-loaded player entity**, with no additional client-side distance cap. Server entity tracking still limits which players and live positions are visible. With Orbit Combat active, it attempts a teleport-style position packet beside and above the locked target at both the **start and end of each client tick** (up to 40 attempts per second at 20 TPS), rather than approaching by ordinary flight. The two phases are not evenly spaced 25 ms apart. Teleport attempts pause during the actual mace descent to preserve fall distance for smash attacks. Re-lock uses bounded movement prediction; the server may reject or correct every position packet.

**Accuracy:** The orbit uses a bounded lead based on recent target displacement and rotates toward the predicted eye position with faster, still capped turning. Within 4.5 blocks, it aims at the current target eye position. Its preferred orbit radius is about 3.7 blocks. **Orbit movement has a 1280-block/sec maximum (64 blocks/tick)**, with automatic close-range braking to avoid flying past the target. Because it constantly teleports back near the target, actual circling speed will usually be far below that ceiling.

**Mace:** With a mace equipped, it climbs toward approximately **6 blocks above the target** at up to **1.2 blocks/tick**, and begins a rapid descent up to **1.65 blocks/tick** once the weapon is at least **99% charged**. It attempts a mace attack only while airborne, falling, in line of sight, and with more than 1.5 blocks of client fall distance. Following a hit it quickly resets for the next climb. **Attack attempts have no client-side distance cap** (but still check line of sight, mace descent, and cooldown). This does not grant infinite real reach: the server can reject attacks outside its allowed range. The server controls damage, attack cooldowns, movement acceptance, and mace smash mechanics. A hit can fail even when the client attempts one.


**Flying-target tracking upgrade:** prediction now leads fast client-visible targets by up to two ticks, based on up to 12 blocks/tick of observed movement. START/END tick re-locks snap closer beside flying players (about 1.8 blocks for ordinary attacks, 1.15 blocks when using a mace). During a mace descent, horizontal pursuit aims at the predicted moving target instead of completing a normal circular orbit; the dive is abandoned and a new re-lock attempted if the target gets more than 10 blocks away. Aim turning is also faster. The server still decides whether attacks are in reach, whether teleport packets are accepted, and whether mace smash damage applies. This does not guarantee kills.

## AutoTotem and AutoLeave

Open **Right Ctrl → Utility** to enable **AutoTotem** or **AutoLeave** independently. AutoTotem looks for a Totem of Undying in your normal inventory or hotbar and uses the vanilla inventory offhand-swap action to equip it when no totem is equipped. It does not create totems, override a server's inventory, or run while another container is open.

AutoLeave disconnects from the current server when your health is at or below the selected limit. The default is **1 heart (2 Minecraft HP)**; press **AutoLeave threshold** in the Utility screen to switch to **half a heart (1 HP)**, or back. Both modules are OFF until enabled. Disconnecting is not guaranteed to save you if a hit kills you in one blow or the server processes damage before the client sees your health update. The threshold is a session setting and resets to 1 heart when restarting the client.

## Meteor-inspired six-category ClickGUI

Press **Right Ctrl** to open the redesigned PortalFlight module menu. The UI is an original dark, teal-accented implementation inspired by Meteor Client's **Combat, Movement, Render, Player, World, and Misc** category organization (not a copy of its assets or code). Modules have visible ON/OFF state, a text **Find** filter and **Prev/Next** pagination. Press **Find** after entering a search phrase. Shortcuts such as **F12** for Orbit Combat continue to work.

- **Combat:** Orbit Combat, AutoTotem, AutoLeave (configurable 0.5/1-heart threshold)
- **Movement:** Fly, Speed, Boat Fly, Swim Boost, Noclip, Auto Sprint, AutoWalk, AutoJump, HighJump, FastFall, WaterWalk, **AutoSneak, NoSlow, Elytra Boost**, and NoFall
- **Render:** Freecam, Player ESP, Mob ESP, Tracers, Storage ESP
- **Player:** Hunger Saver, Spectator Privacy
- **World:** Portal Locator
- **Misc:** Staff Watch screen

The Movement screen retains **Coordinate Flight / TP Settings** and **Fly to Player** actions. Newly added implementations: **AutoSneak** holds sneaking while ordinary movement is available; **NoSlow** applies a modest horizontal speed correction when using an item; **Elytra Boost** adds gentle acceleration in the look direction while gliding forward. These are client-side behaviors, not guaranteed multiplayer bypasses. All three start OFF. This update does **not** include every Meteor Client module; only functioning PortalFlight modules are listed.

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

Open **Right Ctrl → Movement → Coordinate Flight Settings** (or Utility → Coordinate Flight), enter X/Y/Z, and press **TP to Coordinates (Instant Attempt)** instead of **Fly to Coordinates**. This sends Meteor-style on-ground status packets and a final XYZ position packet requesting those coordinates, without following the climb/cruise/descent route. The client moves there optimistically, but the server can reject the position and rubber-band you. After about two seconds, PortalFlight reports whether the client is near the requested destination. If the attempt fails, it does not automatically start cruise flight. This does not bypass server movement validation; no OP permissions or /tp commands are used. Coordinates are validated to Minecraft's world border range and reasonable vertical bounds.

## Meteor-style TP to Coordinates

**Right Ctrl → Movement → Coordinate Flight / TP Settings → TP to Coordinates** now uses the Meteor ClickTP-style movement packet sequence for the entered **X/Y/Z** (instead of requiring a clicked block). It sends up to 19 `StatusOnly(true, true)` packets based on the distance (approximately one per 10 blocks, minus the final packet); for journeys requiring more than 19 status packets it sends none, matching Meteor's behavior. The final movement packet is `Pos(x, y, z, true, true)`. The existing button and typed-coordinate input are unchanged. All original downward-drop, falling, collision and landing-area safety checks still run before any packet is sent. There is **no automatic flight fallback**. Packet acceptance and damage remain server-controlled; even on-ground flags do not ensure zero fall damage.

## Teleport diagnostics

**TP to Coordinates** now writes a persistent **[PortalFlight TP]** status to Minecraft chat as soon as its position packet is sent and again after roughly two seconds. On failure, it reports the distance remaining and distinguishes a return near the starting location (**likely server correction**) from ending elsewhere. The exact server-side rejection reason cannot be recovered unless the server provides it. The Coordinate Flight screen also displays the latest teleport status when reopened. Local safety blocks (unsafe drop, occupied landing, unloaded downward destination) explain why a packet was **not** sent. **Failed TP never starts cruise automatically.**

## Mid-air and downward TP

**TP to Coordinates** permits destinations **in empty air**, regardless of whether the target Y is above or below the current Y. There is no downward-drop or already-falling rejection. The mod **enables Fly immediately** when sending the TP, so it tries to hold position in mid-air. If the destination chunk is loaded, the mod still blocks coordinates inside solid blocks at the feet or head. An unloaded destination cannot be checked locally.

This removes the **client-side** drop restriction; it does **not** guarantee immunity from server-calculated fall damage or guarantee that a teleport packet is accepted. Failed TP does not start cruise flight.

## Client-side homes

PortalFlight registers local commands via Fabric, with **no server command or operator permission required**:

- `/sethome` saves your current XYZ as `home`; `/sethome base` saves a named location
- `/home` or `/home base` starts **Coordinate Flight cruise** toward a saved home (not an instant server teleport)
- `/homes` lists saved home names for your current server and dimension
- `/delhome` or `/delhome base` deletes a saved home

Saved homes survive restarts in `config/portalflight-homes.properties` and are separated by server address and dimension. Home names use letters, digits, underscores or hyphens. Server plugins may register the same command names; client/server command conflicts depend on the command tree. Flight remains subject to server movement checks and inherits the current cruise phasing behavior.

## Coordinate Flight autopilot

Open **Right Ctrl → Utility → Coordinate Flight - Fly to XYZ**. Enter destination coordinates and click **Fly to Coordinates**. PortalFlight follows three phases: (1) ascend to **Y=200** (or stay higher if already above it or the destination is higher), (2) fly horizontally toward the target X/Z at a configurable maximum speed (default 320 blocks/second; enter any positive custom speed up to 1280 blocks/second in **Right Ctrl → Movement → Coordinate Flight Settings**, saved to `config/portalflight.properties`) of client-predicted movement, then (3) descend or ascend to the target Y. The mode slows near waypoints and stops on arrival or after persistent lack of movement. **STOP Flight** cancels it.

During manually started cruise, the client temporarily disables local block collision to attempt phasing through obstacles. Collision is restored when flight stops, arrives, or is canceled. **This does not bypass server collision or movement validation**: on multiplayer servers the player may still be blocked or corrected. Canceling at a high altitude or inside a wall can result in a fall or suffocation.

## Spectator detection limitations

Staff Watch now reports client-visible spectators and invisible players (which may simply be using an invisibility effect). It **cannot** see staff fully hidden by server-side vanish, distinguish ordinary invisible players from moderators, or know whether anyone is watching through a hidden camera. Absence of warnings is **not** evidence that no staff are watching. Alerts depend on the server sending the player entity to your client. Staff Watch is not an anti-ban guarantee.


## ESP rendering notes

Player ESP (F7) and Mob ESP (F8) render wireframe bounding boxes around entities already available to the Minecraft client. ESP Tracers (F11, enabled by default) draws an optional line toward each selected target. The rendering uses camera-relative coordinates and the Fabric 1.21.11 render-context matrix. This is not guaranteed through-wall visibility and does not reveal hidden or unloaded entities.

## Boat Fly stability

Boat Fly adjusts vertical velocity gradually while leaving vanilla horizontal boat controls intact. It tracks the boat between ticks and automatically turns off if an unexpectedly large position change may indicate a server correction. This is a heuristic, not guaranteed detection. It uses vanilla networking and does **not** send fake grounded packets or bypass server anti-cheat. Multiplayer servers can still reject the motion.
