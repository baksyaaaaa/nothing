# PortalFlight — Fabric 1.21.11

Client-side Minecraft utility mod source project.

## New ESP and utility modules

These are original PortalFlight implementations inspired by common functionality in Anubis, Meteor and Krypton; no third-party client source code was copied. Entity ESP outlines only entities the server sends to your client. Storage Finder reports the nearest loaded chest/barrel coordinates, not through-wall block outlines. No auto-combat or anti-cheat bypass added.

## ClickGUI

Press **Right Ctrl** to open the dark PortalFlight menu. Click **Movement**, **Render**, **Combat**, or **Utility**, then click a module to turn it ON/OFF. Press **Esc** to close. The menu controls the same toggles as the keyboard shortcuts and includes all actual implemented modules; Boat Fly Descend (X) remains a hold-to-use key in Minecraft's Key Binds screen. The interface is original and uses no NiceWurst code or modules.

## Keybinds

- **Right Ctrl** — Open the PortalFlight ClickGUI (rebindable under Miscellaneous)

- **C** — Toggle experimental local Noclip: movement keys fly through client-side collision, Space up and Shift down. This is intended for singleplayer; a multiplayer server can reject the movement. Toggle off before leaving a wall to avoid becoming stuck. Minecraft's server may still perform collision checks.

- **F12** — Orbit Combat: follows nearby non-spectator players up to 8 blocks away while flying in an orbit, auto-aims, and attempts frequent melee attacks within 3 blocks and line of sight. A vertical orbit cycle may create falling-hit opportunities. Attacks are no longer gated by the client's weapon cooldown, but servers still apply normal damage/critical rules. Toggle off with F12. Avoid using in combination with Fly or Freecam.

All PortalFlight module toggles and Boat Fly Descend are rebindable in **Options → Controls → Key Binds**, under **Miscellaneous**. Movement, jump, sneak and sprint use Minecraft's own rebindable controls.

- **F7** — Player ESP (client-side glowing outlines, optional tracer lines)
- **F8** — Mob ESP (client-side glowing outlines, optional tracer lines)
- **F11** — Toggle ESP Tracers (enabled by default; draws a line from your view to outlined players and mobs)
- **F9** — Storage Finder (coordinates of nearby loaded chests/barrels)
- **F10** — Auto Sprint
- **R** — Toggle very fast Swim Boost (in water; use movement keys and Jump/Sneak to steer vertically)
- **V** — Speed boost
- **B** — Toggle Boat Fly; Space rises, **X descends** (rebindable in controls) (Shift dismounts as usual)
- **N** — Find nearby *loaded* end portal frames (coordinates only, not through-wall ESP)
- **G** — Toggle normal Fly; Space rises, Shift descends
- **Ctrl while moving** — Faster Fly / Sprint Fly
- **F6** — Toggle Freecam
- **P** — Toggle Spectator Privacy (on by default): check for visible nearby spectators and automatically switch off Fly, Speed, Boat Fly, Freecam, and Safe Fall when one is detected
- **J** — Toggle Safe Fall (enabled by default). While airborne and falling, sends an on-ground status packet and resets client fall distance; this does not guarantee fall protection on servers.
- **H** — Toggle Hunger Saver indicator; this does not disable sprinting or change server hunger

## Building

Every push to main invokes GitHub Actions **Build PortalFlight**. Get the built JAR from the `PortalFlight-Mod` artifact on the [Actions page](https://github.com/baksyaaaaa/nothing/actions) **only if the workflow succeeds**. Use the JAR without `-sources` in your Fabric mods folder, alongside compatible Fabric API.

**Important limitations:** This is experimental, not yet verified to compile or work in-game. Movement and damage/hunger are server-authoritative on multiplayer servers; this mod doesn't grant server permissions or bypass server restrictions. Freecam is experimental. Hunger Saver currently does not implement server-side hunger prevention.

## Spectator detection limitations

Spectator detection only works for nearby spectator player entities that the multiplayer server sends to your client (48-block radius). Servers can hide spectator entities, including staff; those viewers cannot be detected by client-only code. **This mod does not make your player invisible to spectators.** Player visibility is controlled by the server, so hiding your player from other clients requires permissions/server-side functionality.
