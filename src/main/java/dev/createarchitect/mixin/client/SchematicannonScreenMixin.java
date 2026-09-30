package dev.createarchitect.mixin.client;

import com.simibubi.create.content.schematics.cannon.SchematicannonScreen;
import com.simibubi.create.foundation.gui.AllIcons;
import com.simibubi.create.foundation.gui.widget.IconButton;
import dev.createarchitect.client.ArchitectHologramSession;
import dev.createarchitect.client.SharedPreviewClient;
import dev.createarchitect.SchematicannonPreviewState;
import dev.createarchitect.mixin.client.accessor.AbstractContainerScreenAccessor;
import dev.createarchitect.mixin.client.accessor.AbstractSimiWidgetAccessor;
import dev.createarchitect.mixin.client.accessor.ScreenInvoker;
import dev.createarchitect.network.SetCannonPreviewPayload;
import net.minecraft.network.chat.Component;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.PacketDistributor;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(SchematicannonScreen.class)
abstract class SchematicannonScreenMixin {
    @Shadow protected abstract boolean placementSettingsHidden();

    @Unique private IconButton createarchitect$freecamButton;
    @Unique private IconButton createarchitect$previewButton;
    @Unique private IconButton createarchitect$sharedPreviewButton;
    @Unique private boolean createarchitect$sharedPreviewAvailable;

    @Inject(method = "init", at = @At("TAIL"), remap = false)
    private void createarchitect$addFreecamButton(CallbackInfo ci) {
        SchematicannonScreen screen = (SchematicannonScreen) (Object) this;
        AbstractContainerScreenAccessor position = (AbstractContainerScreenAccessor) screen;
        int leftPos = position.createarchitect$getLeftPos();
        int topPos = position.createarchitect$getTopPos();
        createarchitect$freecamButton = new IconButton(leftPos + 28, topPos + 111, AllIcons.I_VIEW_SCHEDULE);
        createarchitect$setTooltip(createarchitect$freecamButton,
                Component.translatable("gui.createarchitect.open_freecam").withStyle(ChatFormatting.GOLD),
                Component.translatable("gui.createarchitect.freecam_description").withStyle(ChatFormatting.GRAY),
                Component.translatable("gui.createarchitect.freecam_movement").withStyle(ChatFormatting.DARK_GRAY),
                ArchitectHologramSession.freecamTooltipControls().copy().withStyle(ChatFormatting.DARK_GRAY));
        createarchitect$freecamButton.withCallback(() -> {
            var cannon = screen.getMenu().contentHolder;
            ArchitectHologramSession.openFreecam(cannon);
        });
        ((ScreenInvoker) screen).createarchitect$addRenderableWidget(createarchitect$freecamButton);

        var cannon = screen.getMenu().contentHolder;
        SchematicannonPreviewState previewState = (SchematicannonPreviewState) cannon;
        boolean enabled = previewState.createarchitect$isPreviewEnabled();
        createarchitect$previewButton = new IconButton(leftPos + 50, topPos + 111,
                enabled ? AllIcons.I_ACTIVE : AllIcons.I_DISABLE);
        createarchitect$updatePreviewButton(createarchitect$previewButton, enabled);
        createarchitect$previewButton.withCallback(() -> {
            boolean next = !previewState.createarchitect$isPreviewEnabled();
            previewState.createarchitect$setPreviewEnabled(next);
            createarchitect$updatePreviewButton(createarchitect$previewButton, next);
            ArchitectHologramSession.previewVisibilityChanged(cannon, next);
            PacketDistributor.sendToServer(new SetCannonPreviewPayload(cannon.getBlockPos(), next));
        });
        ((ScreenInvoker) screen).createarchitect$addRenderableWidget(createarchitect$previewButton);

        ItemStack schematic = cannon.inventory.getStackInSlot(0);
        createarchitect$sharedPreviewAvailable = SharedPreviewClient.hasPreview(schematic);
        createarchitect$sharedPreviewButton = new IconButton(leftPos + 72, topPos + 111,
                createarchitect$sharedPreviewAvailable ? AllIcons.I_CONFIRM : AllIcons.I_OPEN_FOLDER);
        createarchitect$updateSharedPreviewButton(schematic);
        createarchitect$sharedPreviewButton.withCallback(() -> {
            ItemStack current = cannon.inventory.getStackInSlot(0);
            if (SharedPreviewClient.hasPreview(current)) {
                if (Minecraft.getInstance().player != null)
                    Minecraft.getInstance().player.displayClientMessage(Component.translatable(
                            "message.createarchitect.shared_preview_available"), true);
                return;
            }
            SharedPreviewClient.requestManifest(cannon.getBlockPos());
        });
        ((ScreenInvoker) screen).createarchitect$addRenderableWidget(createarchitect$sharedPreviewButton);
    }

    @Inject(method = "containerTick", at = @At("TAIL"), remap = false)
    private void createarchitect$hideButtonsBehindSettings(CallbackInfo ci) {
        if (createarchitect$freecamButton == null || createarchitect$previewButton == null
                || createarchitect$sharedPreviewButton == null)
            return;
        SchematicannonScreen screen = (SchematicannonScreen) (Object) this;
        ItemStack schematic = screen.getMenu().contentHolder.inventory.getStackInSlot(0);
        boolean available = SharedPreviewClient.hasPreview(schematic);
        if (available != createarchitect$sharedPreviewAvailable) {
            createarchitect$sharedPreviewAvailable = available;
            createarchitect$updateSharedPreviewButton(schematic);
        }
        boolean visible = placementSettingsHidden();
        createarchitect$freecamButton.visible = visible;
        createarchitect$freecamButton.active = visible;
        createarchitect$previewButton.visible = visible;
        createarchitect$previewButton.active = visible;
        createarchitect$sharedPreviewButton.visible = visible;
        createarchitect$sharedPreviewButton.active = visible;
    }

    private void createarchitect$updateSharedPreviewButton(ItemStack schematic) {
        createarchitect$sharedPreviewButton.setIcon(createarchitect$sharedPreviewAvailable
                ? AllIcons.I_CONFIRM : AllIcons.I_OPEN_FOLDER);
        createarchitect$sharedPreviewButton.green = createarchitect$sharedPreviewAvailable;
        createarchitect$setTooltip(createarchitect$sharedPreviewButton,
                Component.translatable(createarchitect$sharedPreviewAvailable
                        ? "gui.createarchitect.shared_preview_ready"
                        : "gui.createarchitect.acquire_shared_preview").withStyle(ChatFormatting.GOLD),
                Component.translatable("gui.createarchitect.shared_preview_identity",
                        SharedPreviewClient.fileName(schematic), SharedPreviewClient.owner(schematic))
                        .withStyle(ChatFormatting.GRAY),
                Component.translatable(createarchitect$sharedPreviewAvailable
                        ? "gui.createarchitect.shared_preview_cached"
                        : "gui.createarchitect.shared_preview_missing").withStyle(ChatFormatting.DARK_GRAY));
    }

    private static void createarchitect$updatePreviewButton(IconButton button, boolean enabled) {
        button.setIcon(enabled ? AllIcons.I_ACTIVE : AllIcons.I_DISABLE);
        button.green = enabled;
        createarchitect$setTooltip(button,
                Component.translatable(enabled
                        ? "gui.createarchitect.hide_hologram"
                        : "gui.createarchitect.show_hologram").withStyle(ChatFormatting.GOLD),
                Component.translatable("gui.createarchitect.hologram_description")
                        .withStyle(ChatFormatting.GRAY),
                Component.translatable("gui.createarchitect.hologram_persistence")
                        .withStyle(ChatFormatting.DARK_GRAY));
    }

    private static void createarchitect$setTooltip(IconButton button, Component... lines) {
        var tooltip = ((AbstractSimiWidgetAccessor) button).createarchitect$getToolTip();
        tooltip.clear();
        tooltip.addAll(java.util.List.of(lines));
    }
}
