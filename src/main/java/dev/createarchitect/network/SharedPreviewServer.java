package dev.createarchitect.network;

import com.simibubi.create.AllDataComponents;
import com.simibubi.create.AllItems;
import com.simibubi.create.content.schematics.cannon.SchematicannonBlockEntity;
import com.simibubi.create.foundation.utility.CreatePaths;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.PacketDistributor;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.HexFormat;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

final class SharedPreviewServer {
    static final int MAX_COMPRESSED_BYTES = 32 * 1024 * 1024;
    private static final long MIN_DOWNLOAD_INTERVAL_MS = 2_000;
    private static final Map<UUID, Long> LAST_DOWNLOAD = new ConcurrentHashMap<>();

    private SharedPreviewServer() {}

    static void sendManifest(Player player, BlockPos cannonPos) {
        if (!(player instanceof ServerPlayer serverPlayer))
            return;
        try {
            SchematicSource source = findSource(serverPlayer, cannonPos);
            PacketDistributor.sendToPlayer(serverPlayer, new SharedPreviewManifestPayload(
                    cannonPos, source.owner, source.fileName, source.size, source.hash));
        } catch (PreviewException | IOException exception) {
            error(serverPlayer, exception.getMessage());
        }
    }

    static void sendSchematic(Player player, BlockPos cannonPos, String expectedHash) {
        if (!(player instanceof ServerPlayer serverPlayer))
            return;
        long now = System.currentTimeMillis();
        long previous = LAST_DOWNLOAD.getOrDefault(serverPlayer.getUUID(), 0L);
        if (now - previous < MIN_DOWNLOAD_INTERVAL_MS) {
            error(serverPlayer, "Please wait before requesting another shared preview.");
            return;
        }
        LAST_DOWNLOAD.put(serverPlayer.getUUID(), now);

        try {
            SchematicSource source = findSource(serverPlayer, cannonPos);
            if (!MessageDigest.isEqual(source.hash.getBytes(java.nio.charset.StandardCharsets.US_ASCII),
                    expectedHash.getBytes(java.nio.charset.StandardCharsets.US_ASCII)))
                throw new PreviewException("The schematic changed; request its information again.");

            byte[] bytes = Files.readAllBytes(source.path);
            UUID transferId = UUID.randomUUID();
            int chunkCount = Math.max(1,
                    (bytes.length + SharedPreviewChunkPayload.MAX_CHUNK_BYTES - 1)
                            / SharedPreviewChunkPayload.MAX_CHUNK_BYTES);
            PacketDistributor.sendToPlayer(serverPlayer, new SharedPreviewStartPayload(
                    transferId, source.owner, source.fileName, source.hash, bytes.length, chunkCount));
            for (int index = 0; index < chunkCount; index++) {
                int start = index * SharedPreviewChunkPayload.MAX_CHUNK_BYTES;
                int end = Math.min(bytes.length, start + SharedPreviewChunkPayload.MAX_CHUNK_BYTES);
                PacketDistributor.sendToPlayer(serverPlayer, new SharedPreviewChunkPayload(
                        transferId, index, Arrays.copyOfRange(bytes, start, end)));
            }
        } catch (PreviewException | IOException exception) {
            error(serverPlayer, exception.getMessage());
        }
    }

    private static SchematicSource findSource(ServerPlayer player, BlockPos cannonPos)
            throws PreviewException, IOException {
        if (player.distanceToSqr(Vec3.atCenterOf(cannonPos)) > 16 * 16)
            throw new PreviewException("Move closer to the Schematicannon to acquire its preview.");
        if (!(player.level().getBlockEntity(cannonPos) instanceof SchematicannonBlockEntity cannon))
            throw new PreviewException("The Schematicannon is no longer available.");

        ItemStack schematic = cannon.inventory.getStackInSlot(0);
        if (!AllItems.SCHEMATIC.isIn(schematic)
                || !schematic.getOrDefault(AllDataComponents.SCHEMATIC_DEPLOYED, false))
            throw new PreviewException("This Schematicannon has no deployed schematic.");
        String owner = schematic.get(AllDataComponents.SCHEMATIC_OWNER);
        String fileName = schematic.get(AllDataComponents.SCHEMATIC_FILE);
        if (owner == null || owner.isBlank() || fileName == null || !fileName.endsWith(".nbt"))
            throw new PreviewException("The schematic metadata is incomplete.");

        Path base = CreatePaths.UPLOADED_SCHEMATICS_DIR.toAbsolutePath().normalize();
        Path relative;
        try {
            relative = Paths.get(owner).resolve(fileName).normalize();
        } catch (RuntimeException exception) {
            throw new PreviewException("The schematic path is invalid.");
        }
        Path path = base.resolve(relative).normalize();
        if (!path.startsWith(base) || !Files.isRegularFile(path))
            throw new PreviewException("The server no longer has this schematic file.");
        long size = Files.size(path);
        if (size <= 0 || size > MAX_COMPRESSED_BYTES)
            throw new PreviewException("This schematic is too large to transfer safely.");
        return new SchematicSource(owner, fileName, path, size, sha256(path));
    }

    private static String sha256(Path path) throws IOException {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            try (InputStream input = Files.newInputStream(path)) {
                byte[] buffer = new byte[8192];
                int read;
                while ((read = input.read(buffer)) >= 0)
                    digest.update(buffer, 0, read);
            }
            return HexFormat.of().formatHex(digest.digest());
        } catch (NoSuchAlgorithmException impossible) {
            throw new IllegalStateException(impossible);
        }
    }

    private static void error(ServerPlayer player, String message) {
        PacketDistributor.sendToPlayer(player, new SharedPreviewErrorPayload(
                message == null ? "Unable to acquire the shared preview." : message));
    }

    private record SchematicSource(String owner, String fileName, Path path, long size, String hash) {}

    private static final class PreviewException extends Exception {
        private PreviewException(String message) {
            super(message);
        }
    }
}
