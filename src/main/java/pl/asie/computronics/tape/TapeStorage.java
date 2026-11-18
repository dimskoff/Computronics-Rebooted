package pl.asie.computronics.tape;

import pl.asie.computronics.Computronics;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Raw byte storage of one tape, backed by {@code <world>/computronics/tapes/tape_<id>.bin}.
 * <p>
 * The head position is NOT stored here (it belongs to the tape item / drive). Writes only mark the
 * tape dirty; data is flushed by {@link #saveAll()} on world save and server stop, instead of
 * rewriting up to 16 MB of file on every {@code write()} call.
 */
public final class TapeStorage {
    private static final Map<String, TapeStorage> CACHE = new ConcurrentHashMap<>();
    private static volatile Path storageDirectory;

    private final String id;
    private final byte[] data;
    private boolean dirty;

    public static void setStorageDirectory(Path path) {
        storageDirectory = path;
    }

    public static TapeStorage get(String id, int capacity) {
        if (id == null || id.isEmpty() || storageDirectory == null) return null;
        return CACHE.computeIfAbsent(id, k -> new TapeStorage(k, capacity));
    }

    /** Flushes every dirty tape to disk. */
    public static void saveAll() {
        CACHE.values().forEach(TapeStorage::save);
    }

    /** Flushes and forgets everything (server stopped / world switched). */
    public static void shutdown() {
        saveAll();
        CACHE.clear();
        storageDirectory = null;
    }

    private TapeStorage(String id, int capacity) {
        this.id = id;
        byte[] loaded = load(file(id));
        // never truncate an existing tape if the configured capacity shrank
        this.data = new byte[Math.max(capacity, loaded != null ? loaded.length : 0)];
        if (loaded != null) System.arraycopy(loaded, 0, data, 0, loaded.length);
    }

    public int getCapacity() {
        return data.length;
    }

    /** Reads up to {@code length} bytes at {@code position}. */
    public synchronized byte[] read(int position, int length) {
        if (position < 0 || position >= data.length || length <= 0) return new byte[0];
        int n = Math.min(length, data.length - position);
        byte[] out = new byte[n];
        System.arraycopy(data, position, out, 0, n);
        return out;
    }

    public synchronized int readByte(int position) {
        if (position < 0 || position >= data.length) return -1;
        return data[position] & 0xFF;
    }

    /** Writes bytes at {@code position}, returns how many fit. */
    public synchronized int write(int position, byte[] bytes) {
        if (bytes == null || position < 0 || position >= data.length) return 0;
        int n = Math.min(bytes.length, data.length - position);
        System.arraycopy(bytes, 0, data, position, n);
        if (n > 0) dirty = true;
        return n;
    }

    public synchronized void save() {
        if (!dirty || storageDirectory == null) return;
        Path target = file(id);
        try {
            Files.createDirectories(target.getParent());
            Path tmp = target.resolveSibling(target.getFileName() + ".tmp");
            Files.write(tmp, data);
            Files.move(tmp, target, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            dirty = false;
        } catch (IOException e) {
            Computronics.LOGGER.error("Failed to save tape {}", id, e);
        }
    }

    private static Path file(String id) {
        // ids are UUIDs we generate; sanitize anyway so a crafted item can't escape the folder
        String safe = id.replaceAll("[^A-Za-z0-9-]", "_");
        return storageDirectory.resolve("tape_" + safe + ".bin");
    }

    private static byte[] load(Path file) {
        if (!Files.exists(file)) return null;
        try {
            return Files.readAllBytes(file);
        } catch (IOException e) {
            Computronics.LOGGER.error("Failed to load tape {}", file, e);
            return null;
        }
    }
}
