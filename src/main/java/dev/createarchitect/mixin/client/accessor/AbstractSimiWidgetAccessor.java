package dev.createarchitect.mixin.client.accessor;

import net.createmod.catnip.gui.widget.AbstractSimiWidget;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.List;

@Mixin(AbstractSimiWidget.class)
public interface AbstractSimiWidgetAccessor {
    @Accessor("toolTip")
    List<Component> createarchitect$getToolTip();
}
