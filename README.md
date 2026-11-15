# PortalFlight — Fabric 1.21.11

Client-side Minecraft utility mod source project.

## Keybinds

All PortalFlight module toggles and Boat Fly Descend are rebindable in **Options → Controls → Key Binds**, under **Miscellaneous**. Movement, jump, sneak and sprint use Minecraft's own rebindable controls.

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
