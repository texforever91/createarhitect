package dev.createarchitect.mixin.client;

import com.simibubi.create.content.schematics.client.SchematicHandler;
import com.simibubi.create.content.schematics.client.SchematicTransformation;
import dev.createarchitect.client.ArchitectHologramSession;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
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
}
