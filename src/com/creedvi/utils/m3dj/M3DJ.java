package com.creedvi.utils.m3dj;

import com.creedvi.utils.m3dj.io.IO;
import com.creedvi.utils.m3dj.io.Tracelog;
import com.creedvi.utils.m3dj.model.M3DJ_Model;

import java.io.IOException;
import java.nio.ByteBuffer;

public class M3DJ {

    private static final int MAGIC_LENGTH = 4;

    public static M3DJ_Model M3DJ_Load(String fileName) throws IOException {
        M3DJ_Model result = new M3DJ_Model();
        Tracelog logger = new Tracelog(Tracelog.LogLevel.LEVEL_DEBUG);
        int fileSize;

        if (fileName.substring(fileName.lastIndexOf(".")).equalsIgnoreCase(".m3d") ||
            fileName.substring(fileName.lastIndexOf(".")).equals(".a3d")) {
            ByteBuffer fileData = ByteBuffer.wrap(IO.LoadFileData(fileName));

            StringBuilder magic = new StringBuilder();
            for (int i = 0; i < MAGIC_LENGTH; i++) {
                magic.append(Character.highSurrogate(fileData.get()));
            }

            if (magic.toString().equals("3DMO")) {
                fileSize = fileData.getInt();
                logger.out(Tracelog.LogType.LOG_DEBUG, "Binary magic found. File size: " + fileSize);
                result = M3DJ_LoadBinary(fileData);
            }
            else if (magic.toString().equals("3dmo")) {
                fileSize = fileData.getInt();
                logger.out(Tracelog.LogType.LOG_DEBUG, "ASCII magic found. File size: " + fileSize);
                result = M3DJ_LoadAscii(fileData);
            }
            else {
                logger.out(Tracelog.LogType.LOG_WARNING, "Bad magic identified. Returning null object.");
                return null;
            }
        }

        return result;
    }

    private static M3DJ_Model M3DJ_LoadAscii(ByteBuffer fileData) {
        M3DJ_Model result = new M3DJ_Model();

        int prt = MAGIC_LENGTH * 2; // Account for file header

        while (fileData.hasRemaining()) {

        }

        return result;
    }

    private static M3DJ_Model M3DJ_LoadBinary(ByteBuffer fileData) {
        M3DJ_Model result = new M3DJ_Model();

        int ptr = MAGIC_LENGTH * 2; // Account for file header
        int chunkSize;

        while (fileData.hasRemaining()) {
            StringBuilder magic = new StringBuilder();
            for (int i = 0; i < MAGIC_LENGTH; i++) {
                magic.append(Character.highSurrogate(fileData.get()));
            }
            ptr += MAGIC_LENGTH;

            if (magic.toString().equals("PRVW")) {
                chunkSize = fileData.getInt();
                ptr += Integer.BYTES;

                for (int i = 0; i < chunkSize; i++) {
                    // todo: load preview png
                }
                ptr += chunkSize;
            }
            else if (magic.toString().equals("HEAD")) {
                chunkSize = fileData.getInt();
                ptr += Integer.BYTES;

                for (int i = 0; i < chunkSize; i++) {
                    // todo: load header chunk
                }
                ptr += chunkSize;
            }
            else if (magic.toString().equals("CMAP")) {
                chunkSize = fileData.getInt();
                ptr += Integer.BYTES;

                for (int i = 0; i < chunkSize; i++) {
                    // todo: load color map
                }
                ptr += chunkSize;
            }
            else if (magic.toString().equals("TMAP")) {
                chunkSize = fileData.getInt();
                ptr += Integer.BYTES;

                for (int i = 0; i < chunkSize; i++) {
                    // todo: load texture map
                }
                ptr += chunkSize;
            }
            else if (magic.toString().equals("VRTS")) {
                chunkSize = fileData.getInt();
                ptr += Integer.BYTES;

                for (int i = 0; i < chunkSize; i++) {
                    // todo: load vertices
                }
                ptr += chunkSize;
            }
            else if (magic.toString().equals("BONE")) {
                chunkSize = fileData.getInt();
                ptr += Integer.BYTES;

                for (int i = 0; i < chunkSize; i++) {
                    // todo: load bones
                }
                ptr += chunkSize;
            }
            else if (magic.toString().equals("MTRL")) {
                chunkSize = fileData.getInt();
                ptr += Integer.BYTES;

                for (int i = 0; i < chunkSize; i++) {
                    // todo: load materials
                }
                ptr += chunkSize;
            }
            else if (magic.toString().equals("PROC")) {
                chunkSize = fileData.getInt();
                ptr += Integer.BYTES;

                for (int i = 0; i < chunkSize; i++) {
                    // todo: load procedural surfaces
                }
                ptr += chunkSize;
            }
            else if (magic.toString().equals("MESH")) {
                chunkSize = fileData.getInt();
                ptr += Integer.BYTES;

                for (int i = 0; i < chunkSize; i++) {
                    // todo: load meshes
                }
                ptr += chunkSize;
            }
            else if (magic.toString().equals("SHPE")) {
                chunkSize = fileData.getInt();
                ptr += Integer.BYTES;

                for (int i = 0; i < chunkSize; i++) {
                    // todo: load shapes
                }
                ptr += chunkSize;
            }
            else if (magic.toString().equals("VOXT")) {
                chunkSize = fileData.getInt();
                ptr += Integer.BYTES;

                for (int i = 0; i < chunkSize; i++) {
                    // todo: load voxel types
                }
                ptr += chunkSize;
            }
            else if (magic.toString().equals("VOXD")) {
                chunkSize = fileData.getInt();
                ptr += Integer.BYTES;

                for (int i = 0; i < chunkSize; i++) {
                    // todo: load voxel data
                }
                ptr += chunkSize;
            }
            else if (magic.toString().equals("LBLS")) {
                chunkSize = fileData.getInt();
                ptr += Integer.BYTES;

                for (int i = 0; i < chunkSize; i++) {
                    // todo: load animation labels
                }
                ptr += chunkSize;
            }
            else if (magic.toString().equals("ACTN")) {
                chunkSize = fileData.getInt();
                ptr += Integer.BYTES;

                for (int i = 0; i < chunkSize; i++) {
                    // todo: load actions and animations
                }
                ptr += chunkSize;
            }
            else if (magic.toString().equals("ASET")) {
                chunkSize = fileData.getInt();
                ptr += Integer.BYTES;

                for (int i = 0; i < chunkSize; i++) {
                    // todo: load assets
                }
                ptr += chunkSize;
            }
            else if (magic.toString().equals("OMD3")) {
                break;
            }

            fileData.position(ptr);
        }

        return result;
    }

}