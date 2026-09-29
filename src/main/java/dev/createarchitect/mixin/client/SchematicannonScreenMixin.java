package dev.createarchitect.mixin.client;

import com.simibubi.create.content.schematics.cannon.SchematicannonScreen;
import com.simibubi.create.foundation.gui.AllIcons;
import com.simibubi.create.foundation.gui.widget.IconButton;
import dev.createarchitect.client.ArchitectHologramSession;
import dev.createarchitect.mixin.client.accessor.AbstractContainerScreenAccessor;
import dev.createarchitect.mixin.client.accessor.ScreenInvoker;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(SchematicannonScreen.class)
abstract class SchematicannonScreenMixin {
    @Inject(method = "init", at = @At("TAIL"), remap = false)
    private void createarchitect$addFreecamButton(CallbackInfo ci) {
        SchematicannonScreen screen = (SchematicannonScreen) (Object) this;
        AbstractContainerScreenAccessor position = (AbstractContainerScreenAccessor) screen;
        int leftPos = position.createarchitect$getLeftPos();
        int topPos = position.createarchitect$getTopPos();
        IconButton button = new IconButton(leftPos + 28, topPos + 111, AllIcons.I_VIEW_SCHEDULE);
        button.setToolTip(Component.translatable("gui.createarchitect.open_freecam"));
        button.withCallback(() -> {
            var cannon = screen.getMenu().contentHolder;
            ArchitectHologramSession.openFreecam(cannon);
        });
        ((ScreenInvoker) screen).createarchitect$addRenderableWidget(button);
    }
}
