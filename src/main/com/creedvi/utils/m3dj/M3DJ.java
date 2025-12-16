package com.creedvi.utils.m3dj;

import com.creedvi.utils.m3dj.io.IO;
import com.creedvi.utils.m3dj.io.Tracelog;
import com.creedvi.utils.m3dj.model.M3DJ_Model;
import com.creedvi.utils.m3dj.model.chunks.M3DJ_Color;
import com.creedvi.utils.m3dj.model.chunks.M3DJ_TextureCoordinate;
import com.creedvi.utils.m3dj.model.chunks.VariableTypes;

import java.io.*;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.zip.*;

import static com.creedvi.utils.m3dj.model.chunks.VariableTypes.VariableType.UNDEFINED;

public class M3DJ {

    private static final int MAGIC_LENGTH = 4;
    private static boolean DEBUG = false;

    private static Tracelog logger;

    public static void __SetDebug(boolean b) {
        DEBUG = b;
    }

    public static M3DJ_Model M3DJ_Load(String fileName) throws IOException {
        M3DJ_Model result = new M3DJ_Model();
        if (DEBUG) {
            logger = new Tracelog(Tracelog.LogLevel.LEVEL_DEBUG);
        }
        else {
            logger = new Tracelog(Tracelog.LogLevel.LEVEL_ERROR);
        }
        int fileSize;

        if (fileName.substring(fileName.lastIndexOf(".")).equalsIgnoreCase(".m3d") ||
            fileName.substring(fileName.lastIndexOf(".")).equals(".a3d")) {
            ByteBuffer fileData = ByteBuffer.wrap(IO.LoadFileData(fileName));
            fileData.order(ByteOrder.LITTLE_ENDIAN);

            StringBuilder magic = new StringBuilder();
            for (int i = 0; i < MAGIC_LENGTH; i++) {
                magic.append((char) (fileData.get()));
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

        while (fileData.hasRemaining()) {
            //todo:
        }

        return result;
    }

    private static M3DJ_Model M3DJ_LoadBinary(ByteBuffer fileData) {
        M3DJ_Model model = new M3DJ_Model();
        int chunkSize;

        StringBuilder magic = new StringBuilder();
        for (int i = 0; i < MAGIC_LENGTH; i++) {
            magic.append((char) (fileData.get()));
        }

        if (magic.toString().equals("PRVW")) {
            chunkSize = fileData.getInt();

            model.preview.allocateImageBuffer(chunkSize);

            for (int i = 0; i < chunkSize; i++) {
                model.preview.imageData.put(fileData.get());
            }

            model.preview.imageData.flip();

            magic = new StringBuilder();
            for (int i = 0; i < MAGIC_LENGTH; i++) {
                magic.append((char) (fileData.get()));
            }
        }

        if (!magic.toString().equals("HEAD")) {
            logger.out(Tracelog.LogType.LOG_INFO, "Assumed compressed data; attempting to decompress...");
            fileData = DecompressDataBuffer(fileData.slice(fileData.position() - (Byte.BYTES * 4), fileData.remaining()));

            magic = new StringBuilder();
            for (int i = 0; i < MAGIC_LENGTH; i++) {
                magic.append((char) (fileData.get()));
            }
        }

        if (magic.toString().equals("HEAD")) {
            chunkSize = fileData.getInt();
            logger.out(Tracelog.LogType.LOG_DEBUG, "Header chunk size: " + chunkSize);

            model.header.scale = fileData.getFloat();
            logger.out(Tracelog.LogType.LOG_DEBUG, "Scaling factor: " + model.header.scale);

            int bitField = fileData.getInt();
            model.header.VC_T = VariableTypes.GetVertexCoordTypeByBytePattern((byte) (1 << ((bitField >> 0) & 3)));
            model.header.VI_T = VariableTypes.GetVariableTypeByBytePattern((byte) (1 << ((bitField >> 2) & 3)));
            model.header.SI_T = VariableTypes.GetVariableTypeByBytePattern((byte) (1 << ((bitField >> 4) & 3)));
            model.header.CI_T = VariableTypes.GetVariableTypeByBytePattern((byte) (1 << ((bitField >> 6) & 3)));
            model.header.TI_T = VariableTypes.GetVariableTypeByBytePattern((byte) (1 << ((bitField >> 8) & 3)));
            model.header.BI_T = VariableTypes.GetVariableTypeByBytePattern((byte) (1 << ((bitField >> 10) & 3)));
            model.header.NB_T = VariableTypes.GetBonesPerVertexByBytePattern((byte) (1 << ((bitField >> 12) & 3)));
            model.header.SK_T = VariableTypes.GetVariableTypeByBytePattern((byte) (1 << ((bitField >> 14) & 3)));
            model.header.FC_T = VariableTypes.GetVariableTypeByBytePattern((byte) (1 << ((bitField >> 16) & 3)));
            model.header.HI_T = VariableTypes.GetVariableTypeByBytePattern((byte) (1 << ((bitField >> 18) & 3)));
            model.header.FI_T = VariableTypes.GetVariableTypeByBytePattern((byte) (1 << ((bitField >> 20) & 3)));
            model.header.VD_T = VariableTypes.GetVariableTypeByBytePattern((byte) (1 << ((bitField >> 22) & 3)));
            model.header.VP_T = VariableTypes.GetVariableTypeByBytePattern((byte) (1 << ((bitField >> 24) & 3)));

            if(model.header.CI_T == null) {
                model.header.CI_T = UNDEFINED;
            }
            if(model.header.TI_T == null) {
                model.header.TI_T = UNDEFINED;
            }
            if(model.header.BI_T == null) {
                model.header.BI_T = UNDEFINED;
            }
            if(model.header.SK_T == null) {
                model.header.SK_T = UNDEFINED;
            }
            if(model.header.FC_T == null) {
                model.header.FC_T = UNDEFINED;
            }
            if(model.header.HI_T == null) {
                model.header.HI_T = UNDEFINED;
            }
            if(model.header.FI_T == null) {
                model.header.FI_T = UNDEFINED;
            }

            model.header.DumpBitField(logger);

            byte b;
            do {
                b = fileData.get();
                if ((char) b != '\0') {
                    model.header.title += (char) b;
                }
            } while ((char) b != '\0');
            do {
                b = fileData.get();
                if ((char) b != '\0') {
                    model.header.licence += (char) b;
                }
            } while ((char) b != '\0');
            do {
                b = fileData.get();
                if ((char) b != '\0') {
                    model.header.author += (char) b;
                }
            } while ((char) b != '\0');
            do {
                b = fileData.get();
                if ((char) b != '\0') {
                    model.header.description += (char) b;
                }
            } while ((char) b != '\0');

            logger.out(Tracelog.LogType.LOG_INFO,
           "Model metadata:\n" +
                    "\tModel: "  + model.header.title + "\n" +
                    "\tLicence: "  + model.header.licence + "\n" +
                    "\tAuthor: "  + model.header.author + "\n" +
                    "\tDescription: "  + model.header.description + "\n"
            );
        }
        else {
            logger.out(Tracelog.LogType.LOG_WARNING, "Bad data found. Failed to identify Header chunk where expected. Returning null object...");
        }

        /*todo: bad file indication handling / variable limit checks
        if(sizeof(M3D_FLOAT) == 4 && model->vc_s > 4) {
            M3D_LOG("Double precision coordinates not supported, truncating to float...");
            model->errcode = M3D_ERR_TRUNC;
        }
        if((sizeof(M3D_INDEX) == 2 && (model->vi_s > 2 || model->si_s > 2 || model->ci_s > 2 || model->ti_s > 2 ||
                model->bi_s > 2 || model->sk_s > 2 || model->fc_s > 2 || model->hi_s > 2 || model->fi_s > 2)) ||
                (sizeof(M3D_VOXEL) < (size_t)model->vp_s && model->vp_s != 8)) {
            M3D_LOG("32 bit indices not supported, unable to load model");
            M3D_FREE(model);
            return NULL;
        }
        if(model->vi_s > 4 || model->si_s > 4 || model->vp_s == 4) {
            M3D_LOG("Invalid index size, unable to load model");
            M3D_FREE(model);
            return NULL;
        }
        if(model->nb_s > M3D_NUMBONE) {
            M3D_LOG("Model has more bones per vertex than what importer was configured to support");
            model->errcode = M3D_ERR_TRUNC;
        }
        */

        int end = fileData.limit() - 4;
        magic = new StringBuilder();
        for (int i = 0; i < MAGIC_LENGTH; i++) {
            magic.append((char) (fileData.get(end + i)));
        }
        if (!magic.toString().equals("OMD3")) {
            logger.out(Tracelog.LogType.LOG_ERROR, "Missing end chunk. Returning null object...");
            return null;
        }

        while (fileData.hasRemaining()) {
            magic = new StringBuilder();
            for (int i = 0; i < MAGIC_LENGTH; i++) {
                magic.append((char) (fileData.get()));
            }

            logger.out(Tracelog.LogType.LOG_DEBUG, "===");
            logger.out(Tracelog.LogType.LOG_DEBUG, "Magic reads: " + magic);
            logger.out(Tracelog.LogType.LOG_DEBUG, "Current position: " + fileData.position());

            switch (magic.toString()) {
                case "CMAP":
                    chunkSize = fileData.getInt();
                    logger.out(Tracelog.LogType.LOG_DEBUG, "Chunk size: " + chunkSize);

                    for (int i = 0; i < chunkSize; i++) {
                        M3DJ_Color color = new M3DJ_Color();
                        color.a = fileData.get();
                        color.b = fileData.get();
                        color.g = fileData.get();
                        color.r = fileData.get();

                        model.colorMap.colors.add(color);
                    }
                    break;

                case "TMAP":
                    chunkSize = fileData.getInt();
                    int numTexCoords = (chunkSize/(model.header.VC_T.size * 2));
                    logger.out(Tracelog.LogType.LOG_DEBUG, "Chunk size: " + chunkSize);
                    logger.out(Tracelog.LogType.LOG_DEBUG, "Current Position: " + fileData.position());

                    logger.out(Tracelog.LogType.LOG_DEBUG, "Expected Number of Texture Coordinates: " + numTexCoords);
                    logger.out(Tracelog.LogType.LOG_DEBUG, "Expected End Position: " + (fileData.position() + chunkSize));

                    for (int i = 0; i < numTexCoords; i++) {
                        switch (model.header.VC_T) {
                            case INT8 -> {
                                byte u = fileData.get();
                                byte v = fileData.get();
                                model.textureMap.map.add(new M3DJ_TextureCoordinate(u/255.0, v/255.0));
                            }
                            case INT16 -> {
                                short u = fileData.getShort();
                                short v = fileData.getShort();
                                model.textureMap.map.add(new M3DJ_TextureCoordinate(u/35535.0, v/35535.0));
                            }
                            case FLOAT -> {
                                float u = fileData.getFloat();
                                float v = fileData.getFloat();
                                model.textureMap.map.add(new M3DJ_TextureCoordinate(u, v));
                            }
                            case DOUBLE -> {
                                double u = fileData.getDouble();
                                double v = fileData.getDouble();
                                model.textureMap.map.add(new M3DJ_TextureCoordinate(u, v));
                            }
                        }
                    }
                    logger.out(Tracelog.LogType.LOG_DEBUG, "Texture Coordinates Loaded: " + model.textureMap.map.size());
                    logger.out(Tracelog.LogType.LOG_DEBUG, "Current position: " + fileData.position());

                    break;

                case "VRTS":
                    chunkSize = fileData.getInt();
                    logger.out(Tracelog.LogType.LOG_DEBUG, "Chunk size: " + chunkSize);

                    for (int i = 0; i < chunkSize; i++) {
                        // todo: load vertices
                        fileData.get();
                    }
                    break;

                case "BONE":
                    chunkSize = fileData.getInt();
                    logger.out(Tracelog.LogType.LOG_DEBUG, "Chunk size: " + chunkSize);

                    for (int i = 0; i < chunkSize; i++) {
                        // todo: load bones
                        fileData.get();
                    }
                    break;

                case "MTRL":
                    chunkSize = fileData.getInt();
                    logger.out(Tracelog.LogType.LOG_DEBUG, "Chunk size: " + chunkSize);

                    for (int i = 0; i < chunkSize; i++) {
                        // todo: load materials
                        fileData.get();
                    }
                    break;

                case "PROC":
                    chunkSize = fileData.getInt();
                    logger.out(Tracelog.LogType.LOG_DEBUG, "Chunk size: " + chunkSize);

                    for (int i = 0; i < chunkSize; i++) {
                        // todo: load procedural surfaces
                        fileData.get();
                    }
                    break;

                case "MESH":
                    chunkSize = fileData.getInt();
                    logger.out(Tracelog.LogType.LOG_DEBUG, "Chunk size: " + chunkSize);

                    for (int i = 0; i < chunkSize; i++) {
                        // todo: load meshes
                        fileData.get();
                    }
                    break;

                case "SHPE":
                    chunkSize = fileData.getInt();
                    logger.out(Tracelog.LogType.LOG_DEBUG, "Chunk size: " + chunkSize);

                    for (int i = 0; i < chunkSize; i++) {
                        // todo: load shapes
                        fileData.get();
                    }
                    break;

                case "VOXT":
                    chunkSize = fileData.getInt();
                    logger.out(Tracelog.LogType.LOG_DEBUG, "Chunk size: " + chunkSize);

                    for (int i = 0; i < chunkSize; i++) {
                        // todo: load voxel types
                        fileData.get();
                    }
                    break;

                case "VOXD":
                    chunkSize = fileData.getInt();
                    logger.out(Tracelog.LogType.LOG_DEBUG, "Chunk size: " + chunkSize);

                    for (int i = 0; i < chunkSize; i++) {
                        // todo: load voxel data
                        fileData.get();
                    }
                    break;

                case "LBLS":
                    chunkSize = fileData.getInt();
                    logger.out(Tracelog.LogType.LOG_DEBUG, "Chunk size: " + chunkSize);

                    for (int i = 0; i < chunkSize; i++) {
                        // todo: load animation labels
                        fileData.get();
                    }
                    break;

                case "ACTN":
                    chunkSize = fileData.getInt();
                    logger.out(Tracelog.LogType.LOG_DEBUG, "Chunk size: " + chunkSize);

                    for (int i = 0; i < chunkSize; i++) {
                        // todo: load actions and animations
                        fileData.get();
                    }
                    break;

                case "ASET":
                    chunkSize = fileData.getInt();
                    logger.out(Tracelog.LogType.LOG_DEBUG, "Chunk size: " + chunkSize);

                    for (int i = 0; i < chunkSize; i++) {
                        // todo: load assets
                        fileData.get();
                    }
                    break;

                case "OMD3":
                    //todo: exit
                    break;

                default:
                    break;
            }
        }

        return model;
    }

    private static ByteBuffer DecompressDataBuffer(ByteBuffer compressedData) {
        Inflater inflater = new Inflater();
        inflater.setInput(compressedData);

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        byte[] buffer = new byte[1024];

        try {
            while (!inflater.needsInput()) {
                int decompressedSize = inflater.inflate(buffer);
                outputStream.write(buffer, 0, decompressedSize);
            }
        }
        catch (DataFormatException e) {
            throw new RuntimeException(e);
        }
        finally {
            inflater.end();
        }

        ByteBuffer result = ByteBuffer.allocateDirect(outputStream.size());
        result.put(outputStream.toByteArray());
        result.order(ByteOrder.LITTLE_ENDIAN);
        result.flip();

        return result;
    }

}