package dev.createarchitect.mixin;

import com.simibubi.create.content.schematics.cannon.SchematicannonBlockEntity;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(SchematicannonBlockEntity.class)
abstract class SchematicannonBlockEntityMixin {
    private static final String PREVIEW_SCHEMATIC_KEY = "CreateArchitectPreviewSchematic";

    @Inject(method = "write", at = @At("TAIL"), remap = false)
    private void createarchitect$writePreviewSchematic(CompoundTag tag, HolderLookup.Provider registries,
                                                       boolean clientPacket, CallbackInfo ci) {
        if (!clientPacket)
            return;
        SchematicannonBlockEntity cannon = (SchematicannonBlockEntity) (Object) this;
        ItemStack schematic = cannon.inventory.getStackInSlot(0);
        if (!schematic.isEmpty())
            tag.put(PREVIEW_SCHEMATIC_KEY, schematic.save(registries));
    }

    @Inject(method = "read", at = @At("TAIL"), remap = false)
    private void createarchitect$readPreviewSchematic(CompoundTag tag, HolderLookup.Provider registries,
                                                      boolean clientPacket, CallbackInfo ci) {
        if (!clientPacket)
            return;
        SchematicannonBlockEntity cannon = (SchematicannonBlockEntity) (Object) this;
        ItemStack schematic = ItemStack.parseOptional(registries, tag.getCompound(PREVIEW_SCHEMATIC_KEY));
        cannon.inventory.setStackInSlot(0, schematic);
    }
}
