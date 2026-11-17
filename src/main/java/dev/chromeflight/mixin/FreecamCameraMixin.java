package dev.chromeflight.mixin;

import dev.chromeflight.PortalFlight;
import net.minecraft.client.Camera;
import net.minecraft.world.level.Level;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Camera.class)
public abstract class FreecamCameraMixin {
    @Shadow protected abstract void setPosition(double x, double y, double z);

    @Inject(method = "setup", at = @At("TAIL"))
    private void portalflight$freecam(Level level, Entity cameraEntity,
            boolean detached, boolean mirrored, float partialTick, CallbackInfo ci) {
        if (PortalFlight.freecam) {
            Vec3 p = PortalFlight.freecamPreviousPosition.lerp(
                    PortalFlight.freecamPosition, Math.max(0.0f, Math.min(1.0f, partialTick)));
            setPosition(p.x, p.y, p.z);
        }
    }
}
