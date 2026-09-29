package dev.createarchitect.mixin;

import com.simibubi.create.content.schematics.cannon.SchematicannonBlockEntity;
import com.simibubi.create.content.schematics.cannon.SchematicannonInventory;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(SchematicannonInventory.class)
abstract class SchematicannonInventoryMixin {
    @Shadow @Final private SchematicannonBlockEntity blockEntity;

    @Inject(method = "onContentsChanged", at = @At("TAIL"), remap = false)
    private void createarchitect$syncPreviewSlot(int slot, CallbackInfo ci) {
        if (slot == 0 && blockEntity.getLevel() != null && !blockEntity.getLevel().isClientSide())
            blockEntity.notifyUpdate();
    }
}
