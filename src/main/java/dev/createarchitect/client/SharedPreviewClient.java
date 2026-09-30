package dev.createarchitect.client;

import com.simibubi.create.AllDataComponents;
import com.simibubi.create.foundation.utility.CreatePaths;
import dev.createarchitect.CreateArchitect;
import dev.createarchitect.network.DownloadSharedPreviewPayload;
import dev.createarchitect.network.RequestSharedPreviewManifestPayload;
import dev.createarchitect.network.SharedPreviewChunkPayload;
import dev.createarchitect.network.SharedPreviewManifestPayload;
import dev.createarchitect.network.SharedPreviewStartPayload;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.ConfirmScreen;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.NbtAccounter;
import net.minecraft.nbt.NbtIo;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.neoforged.neoforge.network.PacketDistributor;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.HexFormat;
import java.util.Map;
import java.util.Properties;
import java.util.UUID;

public final class SharedPreviewClient {
    private static final int MAX_COMPRESSED_BYTES = 32 * 1024 * 1024;
    private static final long MAX_UNCOMPRESSED_BYTES = 256L * 1024 * 1024;
    private static final Path CACHE_DIRECTORY = CreatePaths.SCHEMATICS_DIR.resolve("createarchitect-cache");
    private static final Path INDEX_FILE = CACHE_DIRECTORY.resolve("index.properties");
    private static final Properties INDEX = new Properties();
    private static final Map<UUID, Transfer> TRANSFERS = new HashMap<>();
    private static boolean indexLoaded;

    private SharedPreviewClient() {}

    public static boolean hasPreview(ItemStack schematic) {
        return localSchematicPath(schematic) != null || cachedSchematicPath(schematic) != null;
    }

    public static ItemStack resolvePreviewStack(ItemStack schematic) {
        if (localSchematicPath(schematic) != null)
            return schematic.copy();
        Path cached = cachedSchematicPath(schematic);
        if (cached == null)
            return schematic.copy();
        ItemStack copy = schematic.copy();
        copy.set(AllDataComponents.SCHEMATIC_FILE,
                CreatePaths.SCHEMATICS_DIR.relativize(cached).toString().replace('\\', '/'));
        return copy;
    }

    public static String owner(ItemStack schematic) {
        String owner = schematic.get(AllDataComponents.SCHEMATIC_OWNER);
        return owner == null || owner.isBlank() ? "Unknown" : owner;
    }

    public static String fileName(ItemStack schematic) {
        String fileName = schematic.get(AllDataComponents.SCHEMATIC_FILE);
        return fileName == null || fileName.isBlank() ? "Unknown schematic" : fileName;
    }

    public static void requestManifest(BlockPos cannonPos) {
        PacketDistributor.sendToServer(new RequestSharedPreviewManifestPayload(cannonPos));
    }

    public static void receiveManifest(SharedPreviewManifestPayload manifest) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null)
            return;
        var previousScreen = minecraft.screen;
        Component title = Component.translatable("gui.createarchitect.acquire_confirm_title");
        Component message = Component.translatable("gui.createarchitect.acquire_confirm_message",
                manifest.fileName(), manifest.owner(), formatBytes(manifest.compressedBytes()));
        minecraft.setScreen(new ConfirmScreen(accepted -> {
            minecraft.setScreen(previousScreen);
            if (!accepted)
                return;
            PacketDistributor.sendToServer(new DownloadSharedPreviewPayload(
                    manifest.cannonPos(), manifest.hash()));
            minecraft.player.displayClientMessage(
                    Component.translatable("message.createarchitect.shared_preview_started"), true);
        }, title, message, CommonComponents.GUI_YES, CommonComponents.GUI_NO));
    }

    public static void startTransfer(SharedPreviewStartPayload start) {
        if (start.compressedBytes() <= 0 || start.compressedBytes() > MAX_COMPRESSED_BYTES
                || start.chunks() <= 0
                || start.chunks() > (MAX_COMPRESSED_BYTES / SharedPreviewChunkPayload.MAX_CHUNK_BYTES) + 1) {
            receiveError("The server offered an invalid shared preview transfer.");
            return;
        }
        TRANSFERS.clear();
        TRANSFERS.put(start.transferId(), new Transfer(start));
    }

    public static void receiveChunk(SharedPreviewChunkPayload chunk) {
        Transfer transfer = TRANSFERS.get(chunk.transferId());
        if (transfer == null || chunk.index() < 0 || chunk.index() >= transfer.parts.length
                || chunk.data().length > SharedPreviewChunkPayload.MAX_CHUNK_BYTES) {
            TRANSFERS.remove(chunk.transferId());
            receiveError("Received an invalid shared preview chunk.");
            return;
        }
        if (transfer.parts[chunk.index()] == null) {
            transfer.parts[chunk.index()] = chunk.data();
            transfer.received++;
        }
        if (transfer.received != transfer.parts.length)
            return;
        TRANSFERS.remove(chunk.transferId());
        finishTransfer(transfer);
    }

    public static void receiveError(String message) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player != null)
            minecraft.player.displayClientMessage(
                    Component.translatable("message.createarchitect.shared_preview_failed", message), false);
    }

    private static void finishTransfer(Transfer transfer) {
        try {
            ByteArrayOutputStream output = new ByteArrayOutputStream(transfer.start.compressedBytes());
            for (byte[] part : transfer.parts)
                output.write(part);
            byte[] bytes = output.toByteArray();
            if (bytes.length != transfer.start.compressedBytes())
                throw new IOException("Transferred file size does not match the manifest.");
            String actualHash = sha256(bytes);
            if (!MessageDigest.isEqual(actualHash.getBytes(java.nio.charset.StandardCharsets.US_ASCII),
                    transfer.start.hash().getBytes(java.nio.charset.StandardCharsets.US_ASCII)))
                throw new IOException("Transferred file hash does not match the manifest.");

            Minecraft minecraft = Minecraft.getInstance();
            if (minecraft.level == null)
                throw new IOException("The world closed before the transfer completed.");
            var tag = NbtIo.readCompressed(new ByteArrayInputStream(bytes),
                    NbtAccounter.create(MAX_UNCOMPRESSED_BYTES));
            StructureTemplate template = new StructureTemplate();
            template.load(minecraft.level.holderLookup(Registries.BLOCK), tag);
            if (template.getSize().equals(net.minecraft.core.Vec3i.ZERO))
                throw new IOException("The transferred schematic is empty.");

            Files.createDirectories(CACHE_DIRECTORY);
            Path target = CACHE_DIRECTORY.resolve(transfer.start.hash() + ".nbt").normalize();
            if (!target.startsWith(CACHE_DIRECTORY.normalize()))
                throw new IOException("Invalid cache path.");
            Path temporary = Files.createTempFile(CACHE_DIRECTORY, "download-", ".tmp");
            Files.write(temporary, bytes);
            try {
                Files.move(temporary, target, StandardCopyOption.REPLACE_EXISTING,
                        StandardCopyOption.ATOMIC_MOVE);
            } catch (AtomicMoveNotSupportedException ignored) {
                Files.move(temporary, target, StandardCopyOption.REPLACE_EXISTING);
            }

            loadIndex();
            INDEX.setProperty(cacheKey(transfer.start.owner(), transfer.start.fileName()),
                    transfer.start.hash());
            saveIndex();
            ArchitectHologramSession.refreshPreviews();
            if (minecraft.player != null)
                minecraft.player.displayClientMessage(Component.translatable(
                        "message.createarchitect.shared_preview_complete", transfer.start.fileName()), false);
        } catch (Exception exception) {
            CreateArchitect.LOGGER.warn("Failed to cache shared schematic preview", exception);
            receiveError(exception.getMessage() == null ? "Unable to validate the schematic." : exception.getMessage());
        }
    }

    private static Path localSchematicPath(ItemStack schematic) {
        String fileName = schematic.get(AllDataComponents.SCHEMATIC_FILE);
        if (fileName == null)
            return null;
        try {
            Path base = CreatePaths.SCHEMATICS_DIR.toAbsolutePath().normalize();
            Path path = base.resolve(fileName).normalize();
            return path.startsWith(base) && Files.isRegularFile(path) ? path : null;
        } catch (RuntimeException exception) {
            return null;
        }
    }

    private static Path cachedSchematicPath(ItemStack schematic) {
        loadIndex();
        String owner = schematic.get(AllDataComponents.SCHEMATIC_OWNER);
        String fileName = schematic.get(AllDataComponents.SCHEMATIC_FILE);
        if (owner == null || fileName == null)
            return null;
        String hash = INDEX.getProperty(cacheKey(owner, fileName));
        if (hash == null || !hash.matches("[0-9a-f]{64}"))
            return null;
        Path path = CACHE_DIRECTORY.resolve(hash + ".nbt").normalize();
        return path.startsWith(CACHE_DIRECTORY.normalize()) && Files.isRegularFile(path) ? path : null;
    }

    private static synchronized void loadIndex() {
        if (indexLoaded)
            return;
        indexLoaded = true;
        if (!Files.isRegularFile(INDEX_FILE))
            return;
        try (InputStream input = Files.newInputStream(INDEX_FILE)) {
            INDEX.load(input);
        } catch (IOException exception) {
            CreateArchitect.LOGGER.warn("Failed to read the shared preview cache index", exception);
        }
    }

    private static synchronized void saveIndex() throws IOException {
        Files.createDirectories(CACHE_DIRECTORY);
        Path temporary = Files.createTempFile(CACHE_DIRECTORY, "index-", ".tmp");
        try (OutputStream output = Files.newOutputStream(temporary)) {
            INDEX.store(output, "Create: Architect shared preview cache");
        }
        try {
            Files.move(temporary, INDEX_FILE, StandardCopyOption.REPLACE_EXISTING,
                    StandardCopyOption.ATOMIC_MOVE);
        } catch (AtomicMoveNotSupportedException ignored) {
            Files.move(temporary, INDEX_FILE, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    private static String cacheKey(String owner, String fileName) {
        return serverNamespace() + '\u0000' + owner + '\u0000' + fileName;
    }

    private static String serverNamespace() {
        Minecraft minecraft = Minecraft.getInstance();
        var server = minecraft.getCurrentServer();
        if (server != null && server.ip != null && !server.ip.isBlank())
            return sha256(server.ip.trim().toLowerCase(java.util.Locale.ROOT)
                    .getBytes(java.nio.charset.StandardCharsets.UTF_8));
        return minecraft.isLocalServer() ? "integrated-server" : "unknown-server";
    }

    private static String sha256(byte[] bytes) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes));
        } catch (NoSuchAlgorithmException impossible) {
            throw new IllegalStateException(impossible);
        }
    }

    private static String formatBytes(long bytes) {
        if (bytes < 1024)
            return bytes + " B";
        double kib = bytes / 1024d;
        if (kib < 1024)
            return String.format(java.util.Locale.ROOT, "%.1f KiB", kib);
        return String.format(java.util.Locale.ROOT, "%.1f MiB", kib / 1024d);
    }

    private static final class Transfer {
        private final SharedPreviewStartPayload start;
        private final byte[][] parts;
        private int received;

        private Transfer(SharedPreviewStartPayload start) {
            this.start = start;
            this.parts = new byte[start.chunks()][];
        }
    }
}
