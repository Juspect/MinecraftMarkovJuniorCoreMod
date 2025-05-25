// Copyright (C) 2022 Maxim Gumin, The MIT License (MIT)

package com.jxon.juscore.mjcore;

import java.io.*;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.List;

public final class VoxHelper {

    private VoxHelper() {} // Prevent instantiation

    public record LoadVoxResult(int[] data, int MX, int MY, int MZ) {
    }

    public static LoadVoxResult loadVox(String filename) {
        try (FileInputStream fis = new FileInputStream(filename);
             BufferedInputStream bis = new BufferedInputStream(fis)) {

            int[] result = null;
            int MX = -1, MY = -1, MZ = -1;

            // Read magic number
            byte[] magic = new byte[4];
            bis.read(magic);
            String magicStr = new String(magic);

            // Read version
            byte[] versionBytes = new byte[4];
            bis.read(versionBytes);
            int version = ByteBuffer.wrap(versionBytes).order(ByteOrder.LITTLE_ENDIAN).getInt();

            while (bis.available() > 0) {
                byte[] headBytes = new byte[1];
                int bytesRead = bis.read(headBytes);
                if (bytesRead == 0) break;

                char head = (char) headBytes[0];

                if (head == 'S') {
                    byte[] tail = new byte[3];
                    bis.read(tail);
                    String tailStr = new String(tail);
                    if (!tailStr.equals("IZE")) continue;

                    byte[] chunkSizeBytes = new byte[4];
                    bis.read(chunkSizeBytes);
                    int chunkSize = ByteBuffer.wrap(chunkSizeBytes).order(ByteOrder.LITTLE_ENDIAN).getInt();

                    bis.skip(4); // Skip 4 bytes

                    byte[] mxBytes = new byte[4];
                    bis.read(mxBytes);
                    MX = ByteBuffer.wrap(mxBytes).order(ByteOrder.LITTLE_ENDIAN).getInt();

                    byte[] myBytes = new byte[4];
                    bis.read(myBytes);
                    MY = ByteBuffer.wrap(myBytes).order(ByteOrder.LITTLE_ENDIAN).getInt();

                    byte[] mzBytes = new byte[4];
                    bis.read(mzBytes);
                    MZ = ByteBuffer.wrap(mzBytes).order(ByteOrder.LITTLE_ENDIAN).getInt();

                    bis.skip(chunkSize - 12); // Skip remaining bytes
                } else if (head == 'X') {
                    byte[] tail = new byte[3];
                    bis.read(tail);
                    String tailStr = new String(tail);
                    if (!tailStr.equals("YZI")) continue;

                    if (MX <= 0 || MY <= 0 || MZ <= 0) {
                        return new LoadVoxResult(null, MX, MY, MZ);
                    }

                    result = new int[MX * MY * MZ];
                    for (int i = 0; i < result.length; i++) {
                        result[i] = -1;
                    }

                    bis.skip(8); // Skip 8 bytes

                    byte[] numVoxelsBytes = new byte[4];
                    bis.read(numVoxelsBytes);
                    int numVoxels = ByteBuffer.wrap(numVoxelsBytes).order(ByteOrder.LITTLE_ENDIAN).getInt();

                    for (int i = 0; i < numVoxels; i++) {
                        byte x = (byte) bis.read();
                        byte y = (byte) bis.read();
                        byte z = (byte) bis.read();
                        byte color = (byte) bis.read();
                        result[x + y * MX + z * MX * MY] = color;
                    }
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
            for (byte z = 0; z < MZ; z++) {
                for (byte y = 0; y < MY; y++) {
                    for (byte x = 0; x < MX; x++) {
                        int i = x + y * MX + z * MX * MY;
                        byte v = state[i];
                        if (v != 0) {
                            voxels.add(new VoxelData(x, y, z, (byte) (v + 1)));
                        }
                    }
                }
            }

            try (FileOutputStream fos = new FileOutputStream(filename);
                 BufferedOutputStream bos = new BufferedOutputStream(fos)) {

                // Write header
                writeString(bos, "VOX ");
                writeInt(bos, 150);

                writeString(bos, "MAIN");
                writeInt(bos, 0);
                writeInt(bos, 1092 + voxels.size() * 4);

                writeString(bos, "PACK");
                writeInt(bos, 4);
                writeInt(bos, 0);
                writeInt(bos, 1);

                writeString(bos, "SIZE");
                writeInt(bos, 12);
                writeInt(bos, 0);
                writeInt(bos, MX);
                writeInt(bos, MY);
                writeInt(bos, MZ);

                writeString(bos, "XYZI");
                writeInt(bos, 4 + voxels.size() * 4);
                writeInt(bos, 0);
                writeInt(bos, voxels.size());

                for (VoxelData voxel : voxels) {
                    bos.write(voxel.x);
                    bos.write(voxel.y);
                    bos.write(voxel.z);
                    bos.write(voxel.color);
                }

                writeString(bos, "RGBA");
                writeInt(bos, 1024);
                writeInt(bos, 0);

                for (int c : palette) {
                    bos.write((c & 0xff0000) >> 16);
                    bos.write((c & 0xff00) >> 8);
                    bos.write(c & 0xff);
                    bos.write(0);
                }

                for (int i = palette.length; i < 255; i++) {
                    bos.write(0xff - i - 1);
                    bos.write(0xff - i - 1);
                    bos.write(0xff - i - 1);
                    bos.write(0xff);
                }
                bos.write(0);
            }
        } catch (IOException e) {
            System.out.println("Error saving VOX file: " + e.getMessage());
        }
    }

    private static void writeString(OutputStream os, String s) throws IOException {
        for (char c : s.toCharArray()) {
            os.write(c);
        }
    }

    private static void writeInt(OutputStream os, int value) throws IOException {
        ByteBuffer buffer = ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN);
        buffer.putInt(value);
        os.write(buffer.array());
    }

    private record VoxelData(byte x, byte y, byte z, byte color) {
    }
}