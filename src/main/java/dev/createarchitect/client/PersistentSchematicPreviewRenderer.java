package dev.createarchitect.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.simibubi.create.AllDataComponents;
import com.simibubi.create.AllItems;
import com.simibubi.create.content.schematics.SchematicInstances;
import com.simibubi.create.content.schematics.cannon.SchematicannonBlockEntity;
import com.simibubi.create.content.schematics.client.SchematicRenderer;
import dev.createarchitect.CreateArchitect;
import net.createmod.catnip.levelWrappers.SchematicLevel;
import net.createmod.catnip.render.DefaultSuperRenderTypeBuffer;
import net.createmod.catnip.render.SuperRenderTypeBuffer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Milestone 1 proof of concept. The cannon remains authoritative for the
 * deployed schematic ItemStack; this class only caches Create renderers.
 */
@EventBusSubscriber(modid = CreateArchitect.MOD_ID, value = Dist.CLIENT)
public final class PersistentSchematicPreviewRenderer {
    private static final int REFRESH_INTERVAL_FRAMES = 20;
    private static final double MAX_RENDER_DISTANCE_SQR = 192.0 * 192.0;
    private static final Map<BlockPos, Preview> PREVIEWS = new HashMap<>();
    private static int framesUntilRefresh;
    private static ClientLevel cachedLevel;

    private PersistentSchematicPreviewRenderer() {}

    @SubscribeEvent
    public static void render(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_PARTICLES)
            return;

        Minecraft minecraft = Minecraft.getInstance();
        ClientLevel level = minecraft.level;
        if (level == null || minecraft.player == null) {
            clear();
            return;
        }

        if (cachedLevel != level) {
            clear();
            cachedLevel = level;
        }
        if (--framesUntilRefresh <= 0) {
            refresh(level, minecraft.player.blockPosition());
            framesUntilRefresh = REFRESH_INTERVAL_FRAMES;
        }

        PoseStack poseStack = event.getPoseStack();
        Vec3 camera = minecraft.gameRenderer.getMainCamera().getPosition();
        Frustum frustum = event.getFrustum();
        SuperRenderTypeBuffer buffers = DefaultSuperRenderTypeBuffer.getInstance();

        poseStack.pushPose();
        for (Preview preview : PREVIEWS.values()) {
            if (camera.distanceToSqr(Vec3.atCenterOf(preview.cannonPos)) > MAX_RENDER_DISTANCE_SQR)
                continue;
            AABB worldBounds = preview.bounds.move(preview.anchor);
            if (frustum != null && !frustum.isVisible(worldBounds))
                continue;
            poseStack.pushPose();
            poseStack.translate(preview.anchor.getX() - camera.x,
                    preview.anchor.getY() - camera.y,
                    preview.anchor.getZ() - camera.z);
            preview.renderer.render(poseStack, buffers);
            poseStack.popPose();
        }
        buffers.draw();
        poseStack.popPose();
    }

    private static void refresh(ClientLevel level, BlockPos playerPos) {
        Set<BlockPos> seen = new HashSet<>();
        int chunkRadius = Math.min(Minecraft.getInstance().options.getEffectiveRenderDistance(), 12);
        int centerX = playerPos.getX() >> 4;
        int centerZ = playerPos.getZ() >> 4;

        for (int x = centerX - chunkRadius; x <= centerX + chunkRadius; x++) {
            for (int z = centerZ - chunkRadius; z <= centerZ + chunkRadius; z++) {
                LevelChunk chunk = level.getChunkSource().getChunk(x, z, false);
                if (chunk == null)
                    continue;
                for (BlockEntity blockEntity : chunk.getBlockEntities().values()) {
                    if (!(blockEntity instanceof SchematicannonBlockEntity cannon))
                        continue;
                    BlockPos cannonPos = cannon.getBlockPos().immutable();
                    ItemStack schematic = cannon.inventory.getStackInSlot(0);
                    if (!isPreviewable(schematic))
                        continue;
                    seen.add(cannonPos);
                    int hash = SchematicInstances.getHash(schematic);
                    Preview previous = PREVIEWS.get(cannonPos);
                    if (previous == null || previous.schematicHash != hash)
                        createPreview(level, cannonPos, schematic, hash);
                }
            }
        }
        PREVIEWS.keySet().removeIf(position -> !seen.contains(position));
    }

    private static boolean isPreviewable(ItemStack stack) {
        return !stack.isEmpty()
                && AllItems.SCHEMATIC.isIn(stack)
                && stack.getOrDefault(AllDataComponents.SCHEMATIC_DEPLOYED, false)
                && stack.has(AllDataComponents.SCHEMATIC_ANCHOR);
    }

    private static void createPreview(ClientLevel level, BlockPos cannonPos, ItemStack schematic, int hash) {
        SchematicLevel schematicLevel = SchematicInstances.get(level, schematic);
        if (schematicLevel == null) {
            PREVIEWS.remove(cannonPos);
            return;
        }
        BlockPos anchor = schematic.get(AllDataComponents.SCHEMATIC_ANCHOR);
        AABB bounds = AABB.of(schematicLevel.getBounds()).move(-anchor.getX(), -anchor.getY(), -anchor.getZ());
        PREVIEWS.put(cannonPos, new Preview(hash, cannonPos, anchor, bounds,
                new SchematicRenderer(schematicLevel)));
    }

    private static void clear() {
        PREVIEWS.clear();
        cachedLevel = null;
        framesUntilRefresh = 0;
    }

    private record Preview(int schematicHash, BlockPos cannonPos, BlockPos anchor,
                           AABB bounds, SchematicRenderer renderer) {}
}
