// Copyright (C) 2022 Maxim Gumin, The MIT License (MIT)

package com.jxon.juscore.mjcore.utils;

import java.io.*;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public final class VoxHelper {

    private VoxHelper() {} // Prevent instantiation

    public record LoadVoxResult(int[] data, int MX, int MY, int MZ) {}

    public static LoadVoxResult loadVox(String filename) {
        try (FileInputStream fis = new FileInputStream(filename);
             BufferedInputStream bis = new BufferedInputStream(fis)) {

            int[] result = null;
            int MX = -1, MY = -1, MZ = -1;

            // Read magic number - 4 bytes
            byte[] magic = new byte[4];
            if (bis.read(magic) != 4) {
                return new LoadVoxResult(null, -1, -1, -1);
            }
            String magicStr = new String(magic);
            if (!"VOX ".equals(magicStr)) {
                return new LoadVoxResult(null, -1, -1, -1);
            }

            // Read version - 4 bytes, little endian
            byte[] versionBytes = new byte[4];
            if (bis.read(versionBytes) != 4) {
                return new LoadVoxResult(null, -1, -1, -1);
            }
            int version = ByteBuffer.wrap(versionBytes).order(ByteOrder.LITTLE_ENDIAN).getInt();

            while (bis.available() > 0) {
                // Read chunk header - first byte
                byte[] headBytes = new byte[1];
                int bytesRead = bis.read(headBytes);
                if (bytesRead == 0) break;

                char head = (char) (headBytes[0] & 0xFF);

                if (head == 'S') {
                    // Try to read "IZE" to form "SIZE"
                    byte[] tail = new byte[3];
                    if (bis.read(tail) != 3) continue;
                    String tailStr = new String(tail);
                    if (!"IZE".equals(tailStr)) continue;

                    // Read chunk size - 4 bytes little endian
                    byte[] chunkSizeBytes = new byte[4];
                    if (bis.read(chunkSizeBytes) != 4) continue;
                    int chunkSize = ByteBuffer.wrap(chunkSizeBytes).order(ByteOrder.LITTLE_ENDIAN).getInt();

                    // Skip 4 bytes (children chunk size)
                    bis.skip(4);

                    // Read dimensions - 3 * 4 bytes, little endian
                    byte[] mxBytes = new byte[4];
                    byte[] myBytes = new byte[4];
                    byte[] mzBytes = new byte[4];

                    if (bis.read(mxBytes) != 4 || bis.read(myBytes) != 4 || bis.read(mzBytes) != 4) {
                        continue;
                    }

                    MX = ByteBuffer.wrap(mxBytes).order(ByteOrder.LITTLE_ENDIAN).getInt();
                    MY = ByteBuffer.wrap(myBytes).order(ByteOrder.LITTLE_ENDIAN).getInt();
                    MZ = ByteBuffer.wrap(mzBytes).order(ByteOrder.LITTLE_ENDIAN).getInt();

                    // Skip remaining bytes in chunk
                    long remaining = chunkSize - 12;
                    bis.skip(remaining);

                } else if (head == 'X') {
                    // Try to read "YZI" to form "XYZI"
                    byte[] tail = new byte[3];
                    if (bis.read(tail) != 3) continue;
                    String tailStr = new String(tail);
                    if (!"YZI".equals(tailStr)) continue;

                    if (MX <= 0 || MY <= 0 || MZ <= 0) {
                        return new LoadVoxResult(null, MX, MY, MZ);
                    }

                    result = new int[MX * MY * MZ];
                    // Initialize with -1 (equivalent to C# behavior)
                    Arrays.fill(result, -1);

                    // Skip chunk size and children chunk size (8 bytes)
                    bis.skip(8);

                    // Read number of voxels - 4 bytes little endian
                    byte[] numVoxelsBytes = new byte[4];
                    if (bis.read(numVoxelsBytes) != 4) continue;
                    int numVoxels = ByteBuffer.wrap(numVoxelsBytes).order(ByteOrder.LITTLE_ENDIAN).getInt();

                    // Read voxel data
                    for (int i = 0; i < numVoxels; i++) {
                        // Each voxel is 4 bytes: x, y, z, color_index
                        int x = bis.read() & 0xFF; // Unsigned byte
                        int y = bis.read() & 0xFF;
                        int z = bis.read() & 0xFF;
                        int color = bis.read() & 0xFF;

                        if (x < MX && y < MY && z < MZ) {
                            result[x + y * MX + z * MX * MY] = color;
                        }
                    }
                } else {
                    // Skip unknown chunk type
                    continue;
                }
            }

            return new LoadVoxResult(result, MX, MY, MZ);
        } catch (Exception e) {
            return new LoadVoxResult(null, -1, -1, -1);
        }
    }

    public static void saveVox(byte[] state, byte MX, byte MY, byte MZ, int[] palette, String filename) {
        try {
            List<VoxelData> voxels = new ArrayList<>();
            for (int z = 0; z < MZ; z++) {
                for (int y = 0; y < MY; y++) {
                    for (int x = 0; x < MX; x++) {
                        int i = x + y * MX + z * MX * MY;
                        byte v = state[i];
                        if (v != 0) {
                            // Add 1 to color index (VOX format uses 1-based indexing)
                            voxels.add(new VoxelData((byte)x, (byte)y, (byte)z, (byte)(v + 1)));
                        }
                    }
                }
            }

            try (FileOutputStream fos = new FileOutputStream(filename);
                 BufferedOutputStream bos = new BufferedOutputStream(fos)) {

                // Write header: "VOX " + version (150)
                writeStringBytes(bos, "VOX ");
                writeIntLittleEndian(bos, 150);

                // Write MAIN chunk
                writeStringBytes(bos, "MAIN");
                writeIntLittleEndian(bos, 0); // MAIN chunk content size
                writeIntLittleEndian(bos, 1092 + voxels.size() * 4); // Total children size

                // Write PACK chunk
                writeStringBytes(bos, "PACK");
                writeIntLittleEndian(bos, 4); // Content size
                writeIntLittleEndian(bos, 0); // Children size
                writeIntLittleEndian(bos, 1); // Number of models

                // Write SIZE chunk
                writeStringBytes(bos, "SIZE");
                writeIntLittleEndian(bos, 12); // Content size
                writeIntLittleEndian(bos, 0); // Children size
                writeIntLittleEndian(bos, MX);
                writeIntLittleEndian(bos, MY);
                writeIntLittleEndian(bos, MZ);

                // Write XYZI chunk
                writeStringBytes(bos, "XYZI");
                writeIntLittleEndian(bos, 4 + voxels.size() * 4); // Content size
                writeIntLittleEndian(bos, 0); // Children size
                writeIntLittleEndian(bos, voxels.size()); // Number of voxels

                // Write voxel data
                for (VoxelData voxel : voxels) {
                    bos.write(voxel.x & 0xFF);
                    bos.write(voxel.y & 0xFF);
                    bos.write(voxel.z & 0xFF);
                    bos.write(voxel.color & 0xFF);
                }

                // Write RGBA chunk (palette)
                writeStringBytes(bos, "RGBA");
                writeIntLittleEndian(bos, 1024); // Content size (256 * 4)
                writeIntLittleEndian(bos, 0); // Children size

                // Write palette colors
                for (int c : palette) {
                    bos.write((c & 0xFF0000) >> 16); // R
                    bos.write((c & 0xFF00) >> 8);    // G
                    bos.write(c & 0xFF);             // B
                    bos.write(0xFF);                 // A (fully opaque)
                }

                // Fill remaining palette entries
                for (int i = palette.length; i < 255; i++) {
                    bos.write(0xFF - i - 1);
                    bos.write(0xFF - i - 1);
                    bos.write(0xFF - i - 1);
                    bos.write(0xFF);
                }

                // Write final null entry
                bos.write(0);
                bos.write(0);
                bos.write(0);
                bos.write(0);
            }
        } catch (IOException e) {
            System.out.println("Error saving VOX file: " + e.getMessage());
        }
    }

    private static void writeStringBytes(OutputStream os, String s) throws IOException {
        for (char c : s.toCharArray()) {
            os.write(c & 0xFF);
        }
    }

    private static void writeIntLittleEndian(OutputStream os, int value) throws IOException {
        ByteBuffer buffer = ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN);
        buffer.putInt(value);
        os.write(buffer.array());
    }

    private record VoxelData(byte x, byte y, byte z, byte color) {}
}