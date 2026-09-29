package dev.createarchitect.mixin.client;

import dev.createarchitect.client.ArchitectHologramSession;
import net.minecraft.client.MouseHandler;
import net.minecraft.client.player.LocalPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(MouseHandler.class)
abstract class MouseHandlerMixin {
    @Redirect(method = "turnPlayer",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/client/player/LocalPlayer;turn(DD)V"))
    private void createarchitect$turnFreeCamera(LocalPlayer player, double yaw, double pitch) {
        if (!ArchitectHologramSession.turnCamera(yaw, pitch))
            player.turn(yaw, pitch);
    }
}
