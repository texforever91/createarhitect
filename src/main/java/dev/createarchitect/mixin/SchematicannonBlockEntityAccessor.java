package dev.createarchitect.mixin;

import com.simibubi.create.content.schematics.cannon.SchematicannonBlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(SchematicannonBlockEntity.class)
public interface SchematicannonBlockEntityAccessor {
    @Invoker(value = "resetPrinter", remap = false)
    void createarchitect$resetPrinter();
}
