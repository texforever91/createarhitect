package dev.createarchitect.mixin.client;

import com.simibubi.create.content.schematics.client.tools.SchematicToolBase;
import dev.createarchitect.client.ArchitectHologramSession;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = SchematicToolBase.class, remap = false)
abstract class SchematicToolBaseMixin {
    @Unique private Vec3 createarchitect$playerPosition;
    @Unique private float createarchitect$playerYaw;
    @Unique private float createarchitect$playerPitch;

    @Inject(method = "updateTargetPos", at = @At("HEAD"))
    private void createarchitect$useFreeCameraForSelection(CallbackInfo ci) {
        Entity camera = ArchitectHologramSession.camera();
        LocalPlayer player = Minecraft.getInstance().player;
        if (camera == null || player == null)
            return;
        createarchitect$playerPosition = player.position();
        createarchitect$playerYaw = player.getYRot();
        createarchitect$playerPitch = player.getXRot();
        player.setPos(camera.position().subtract(0, player.getEyeHeight(), 0));
        player.setYRot(camera.getYRot());
        player.setXRot(camera.getXRot());
    }

    @Inject(method = "updateTargetPos", at = @At("RETURN"))
    private void createarchitect$restorePlayerAfterSelection(CallbackInfo ci) {
        if (createarchitect$playerPosition == null)
            return;
        LocalPlayer player = Minecraft.getInstance().player;
        if (player != null) {
            player.setPos(createarchitect$playerPosition);
            player.setYRot(createarchitect$playerYaw);
            player.setXRot(createarchitect$playerPitch);
        }
        createarchitect$playerPosition = null;
    }
}
