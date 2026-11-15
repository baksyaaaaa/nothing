package dev.chromeflight.mixin;

import dev.chromeflight.PortalFlight;
import net.minecraft.client.Camera;
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
    private void portalflight$freecam(CallbackInfo ci) {
        if (PortalFlight.freecam) {
            Vec3 p = PortalFlight.freecamPosition;
            setPosition(p.x, p.y, p.z);
        }
    }
}
