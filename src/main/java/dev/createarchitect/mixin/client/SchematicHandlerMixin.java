package dev.createarchitect.mixin.client;

import com.simibubi.create.content.schematics.client.SchematicHandler;
import com.simibubi.create.content.schematics.client.SchematicRenderer;
import com.simibubi.create.content.schematics.client.SchematicTransformation;
import com.simibubi.create.content.schematics.client.tools.ISchematicTool;
import com.simibubi.create.AllSpecialTextures;
import com.mojang.blaze3d.vertex.PoseStack;
import dev.createarchitect.client.ArchitectHologramSession;
import net.createmod.catnip.animation.AnimationTickHolder;
import net.createmod.catnip.render.SuperRenderTypeBuffer;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(SchematicHandler.class)
abstract class SchematicHandlerMixin {
    @Shadow private ItemStack activeSchematicItem;
    @Shadow private int activeHotbarSlot;
    @Shadow private SchematicTransformation transformation;

    @Inject(method = "findBlueprintInHand", at = @At("RETURN"), cancellable = true, remap = false)
    private void createarchitect$useCannonSchematic(Player player,
                                                     CallbackInfoReturnable<ItemStack> cir) {
        ItemStack supplemental = ArchitectHologramSession.supplementalStack();
        if (supplemental != null) {
            activeSchematicItem = supplemental;
            activeHotbarSlot = -1;
            cir.setReturnValue(supplemental);
            return;
        }
        if (cir.getReturnValue() != null)
            return;
        ItemStack preview = ArchitectHologramSession.previewStack();
        if (preview == null)
            return;
        activeSchematicItem = preview;
        activeHotbarSlot = -1;
        cir.setReturnValue(preview);
    }

    @Inject(method = "sync", at = @At("HEAD"), cancellable = true, remap = false)
    private void createarchitect$syncCannon(CallbackInfo ci) {
        if (ArchitectHologramSession.isProcessingSupplemental()) {
            ci.cancel();
            return;
        }
        if (activeHotbarSlot != -1)
            return;
        ArchitectHologramSession.sync(transformation);
        ci.cancel();
    }

    @Inject(method = "onMouseInput", at = @At("HEAD"), cancellable = true, remap = false)
    private void createarchitect$lockPassiveMouse(int button, boolean pressed,
                                                   CallbackInfoReturnable<Boolean> cir) {
        if (activeHotbarSlot == -1 && !ArchitectHologramSession.isEditing())
            cir.setReturnValue(false);
    }

    @Inject(method = "mouseScrolled", at = @At("HEAD"), cancellable = true, remap = false)
    private void createarchitect$lockPassiveScroll(double delta, CallbackInfoReturnable<Boolean> cir) {
        if (activeHotbarSlot == -1 && !ArchitectHologramSession.isEditing())
            cir.setReturnValue(false);
    }

    @Inject(method = "onKeyInput", at = @At("HEAD"), cancellable = true, remap = false)
    private void createarchitect$lockPassiveKeys(int key, boolean pressed, CallbackInfo ci) {
        if (activeHotbarSlot == -1 && !ArchitectHologramSession.isEditing())
            ci.cancel();
    }

    @Inject(method = "render(Lnet/minecraft/client/gui/GuiGraphics;Lnet/minecraft/client/DeltaTracker;)V",
            at = @At("HEAD"), cancellable = true, remap = false)
    private void createarchitect$hidePassiveOverlay(GuiGraphics graphics, DeltaTracker tracker, CallbackInfo ci) {
        if (activeHotbarSlot == -1 && !ArchitectHologramSession.isEditing())
            ci.cancel();
    }

    @Redirect(method = "render",
            at = @At(value = "INVOKE",
                    target = "Lcom/simibubi/create/content/schematics/client/tools/ISchematicTool;renderTool(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/createmod/catnip/render/SuperRenderTypeBuffer;Lnet/minecraft/world/phys/Vec3;)V"),
            remap = false)
    private void createarchitect$hidePassiveWorldTool(ISchematicTool tool, PoseStack poseStack,
                                                       SuperRenderTypeBuffer buffer, Vec3 camera) {
        if (!ArchitectHologramSession.isRenderingSupplemental()
                && (activeHotbarSlot != -1 || ArchitectHologramSession.isEditing()))
            tool.renderTool(poseStack, buffer, camera);
    }

    @Redirect(method = "render",
            at = @At(value = "INVOKE",
                    target = "Lcom/simibubi/create/content/schematics/client/SchematicRenderer;render(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/createmod/catnip/render/SuperRenderTypeBuffer;)V"),
            remap = false)
    private void createarchitect$renderIndependentStructure(SchematicRenderer renderer, PoseStack poseStack,
                                                             SuperRenderTypeBuffer buffer) {
        if (activeHotbarSlot != -1 || ArchitectHologramSession.isRenderingSupplemental()
                || ArchitectHologramSession.isEditing())
            renderer.render(poseStack, buffer);
    }

    @Redirect(method = "render",
            at = @At(value = "INVOKE",
                    target = "Lcom/simibubi/create/content/schematics/client/tools/ISchematicTool;renderOnSchematic(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/createmod/catnip/render/SuperRenderTypeBuffer;)V"),
            remap = false)
    private void createarchitect$keepSchematicOutline(ISchematicTool tool, PoseStack poseStack,
                                                       SuperRenderTypeBuffer buffer) {
        if (!ArchitectHologramSession.isRenderingSupplemental()) {
            if (activeHotbarSlot != -1 || ArchitectHologramSession.isEditing())
                tool.renderOnSchematic(poseStack, buffer);
            return;
        }

        var outline = ((SchematicHandler) (Object) this).getOutline();
        outline.getParams()
                .colored(0x6886C5)
                .withFaceTexture(AllSpecialTextures.CHECKERED)
                .lineWidth(1 / 16f);
        outline.render(poseStack, buffer, Vec3.ZERO, AnimationTickHolder.getPartialTicks());
        outline.getParams().clearTextures();
    }

    @Inject(method = "render(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/createmod/catnip/render/SuperRenderTypeBuffer;Lnet/minecraft/world/phys/Vec3;)V",
            at = @At("TAIL"), remap = false)
    private void createarchitect$renderOtherCannons(PoseStack poseStack, SuperRenderTypeBuffer buffer,
                                                     Vec3 camera, CallbackInfo ci) {
        ArchitectHologramSession.renderSupplemental(poseStack, buffer, camera);
    }
}
