package pl.asie.computronics.tape;

import pl.asie.computronics.Computronics;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class TapeStorage {
    private static final Map<String, TapeStorage> CACHE = new ConcurrentHashMap<>();
    private static Path storageDirectory = null;

    private final String id;
    private final int capacity;
    private byte[] data;
    private int position = 0;
    private boolean dirty = false;

    public static void setStorageDirectory(Path path) {
        storageDirectory = path;
    }

    public static TapeStorage get(String id, int capacity) {
        if (id == null) return null;
        return CACHE.computeIfAbsent(id, k -> new TapeStorage(k, capacity));
    }

    private TapeStorage(String id, int capacity) {
        this.id = id;
        this.capacity = capacity;
        this.data = new byte[capacity];
        load();
    }

    public int getCapacity() {
        return capacity;
    }

    public int getPosition() {
        return position;
    }

    public int seek(int offset) {
        int target = position + offset;
        if (target < 0) target = 0;
        if (target > capacity) target = capacity;
        int diff = target - position;
        position = target;
        return diff;
    }

    public void setPosition(int newPos) {
        if (newPos < 0) newPos = 0;
        if (newPos > capacity) newPos = capacity;
        this.position = newPos;
    }

    public synchronized byte[] read(int length) {
        if (position >= capacity || length <= 0) {
            return new byte[0];
        }
        int available = Math.min(length, capacity - position);
        byte[] result = new byte[available];
        System.arraycopy(data, position, result, 0, available);
        position += available;
        return result;
    }

    public synchronized int write(byte[] bytes) {
        if (position >= capacity || bytes == null || bytes.length == 0) {
            return 0;
        }
        int toWrite = Math.min(bytes.length, capacity - position);
        System.arraycopy(bytes, 0, data, position, toWrite);
        position += toWrite;
        dirty = true;
        save();
        return toWrite;
    }

    private void load() {
        if (storageDirectory == null) return;
        Path file = storageDirectory.resolve("tape_" + id + ".bin");
        if (Files.exists(file)) {
            try {
                byte[] loaded = Files.readAllBytes(file);
                int copyLen = Math.min(loaded.length, capacity);
                System.arraycopy(loaded, 0, data, 0, copyLen);
            } catch (IOException e) {
                Computronics.LOGGER.error("Failed to load tape storage for ID " + id, e);
            }
        }
    }

    public synchronized void save() {
        if (storageDirectory == null || !dirty) return;
        try {
            Files.createDirectories(storageDirectory);
            Path file = storageDirectory.resolve("tape_" + id + ".bin");
            Files.write(file, data);
            dirty = false;
        } catch (IOException e) {
            Computronics.LOGGER.error("Failed to save tape storage for ID " + id, e);
        }
    }
}
