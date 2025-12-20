package com.creedvi.utils.m3dj;

import com.creedvi.utils.m3dj.io.IO;
import com.creedvi.utils.m3dj.io.Tracelog;
import com.creedvi.utils.m3dj.model.M3DJ_Model;
import com.creedvi.utils.m3dj.model.chunks.M3DJ_Color;
import com.creedvi.utils.m3dj.model.chunks.M3DJ_TextureCoordinate;
import com.creedvi.utils.m3dj.model.chunks.M3DJ_Vertex;
import com.creedvi.utils.m3dj.model.chunks.VariableTypes;

import java.io.*;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.zip.*;

import static com.creedvi.utils.m3dj.model.chunks.VariableTypes.VariableType.UNDEFINED;

public class M3DJ {

    private static final int MAGIC_LENGTH = 4;
    private static final int M3D_NUMBONE = 4;
    private static final int M3D_BONEMAXLEVEL = 64;
    private static boolean DEBUG = false;
    private static boolean CMAP_Loaded = false;
    private static boolean TMAP_Loaded = false;
    private static boolean VRTS_Loaded = false;
    private static boolean BONE_Loaded = false;
    private static boolean VOXT_Loaded = false;


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
                logger.out(Tracelog.LogType.LOG_INFO, "Binary magic found. File size: " + fileSize + "B");
                result = M3DJ_LoadBinary(fileData);
            }
            else if (magic.toString().equals("3dmo")) {
                fileSize = fileData.getInt();
                logger.out(Tracelog.LogType.LOG_INFO, "ASCII magic found. File size: " + fileSize + "B");
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
            logger.out(Tracelog.LogType.LOG_INFO, "Failed to identify header; assuming compressed data and attempting to decompress...");
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
            if (model.header.scale <= 0.0f) {
                model.header.scale = 1.0f;
            }
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
                    "\tDescription: "  + model.header.description
            );
        }
        else {
            logger.out(Tracelog.LogType.LOG_WARNING, "Bad data found. Failed to identify Header chunk where expected. Returning null object...");
            return null;
        }

        if(model.header.VC_T.size > 4) {
            logger.out(Tracelog.LogType.LOG_WARNING, "Double precision coordinates are not supported, coordinates will be truncated to float...");
        }

        if(model.header.VI_T.size > 4 || model.header.SI_T.size > 4 || model.header.VP_T.size == 4) {
            logger.out(Tracelog.LogType.LOG_ERROR, "Invalid index size, unable to load model. Returning null object...");
            return null;
        }

        int end = fileData.limit() - 4;
        magic = new StringBuilder();
        for (int i = 0; i < MAGIC_LENGTH; i++) {
            magic.append((char) (fileData.get(end + i)));
        }
        if (!magic.toString().equals("OMD3")) {
            logger.out(Tracelog.LogType.LOG_ERROR, "Missing end chunk. Returning null object...");
            return null;
        }

        if(model.header.NB_T.value > M3D_NUMBONE) {
            logger.out(Tracelog.LogType.LOG_ERROR, "Model has more bones per vertex than what importer was configured to support");

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
                    if (CMAP_Loaded){
                        logger.out(Tracelog.LogType.LOG_ERROR, "Additional color map chunk encountered. Color map chunk must be unique.");
                        continue;
                    }
                    if (model.header.TI_T == UNDEFINED) {
                        logger.out(Tracelog.LogType.LOG_ERROR, "Encountered color map chunk while datatype is null.");
                        continue;
                    }
                    CMAP_Loaded = true;

                    // Chunk size includes the length of Magic and Integer value, so we have to account that
                    // we've already processed those bytes by subtracting them from the chunk size.
                    chunkSize = fileData.getInt() - (MAGIC_LENGTH * 2);
                    int colorSize = 4;
                    int numColors = chunkSize / colorSize;

                    logger.out(Tracelog.LogType.LOG_DEBUG, "Chunk size: " + chunkSize);
                    logger.out(Tracelog.LogType.LOG_DEBUG, "Expected Number of Colours: " + numColors);
                    logger.out(Tracelog.LogType.LOG_DEBUG, "Colour unit size: " + colorSize + " bytes");
                    logger.out(Tracelog.LogType.LOG_DEBUG, "Expected End Position: " + (fileData.position() + chunkSize));

                    for (int i = 0; i < chunkSize; i++) {
                        M3DJ_Color color = new M3DJ_Color();
                        color.a = fileData.get();
                        color.b = fileData.get();
                        color.g = fileData.get();
                        color.r = fileData.get();

                        model.colorMap.colors.add(color);
                    }

                    logger.out(Tracelog.LogType.LOG_DEBUG, "Colours Loaded: " + model.colorMap.colors.size());

                    break;

                case "TMAP":
                    if (TMAP_Loaded){
                        logger.out(Tracelog.LogType.LOG_ERROR, "Additional texture map chunk encountered. Texture map chunk must be unique.");
                        continue;
                    }
                    if (model.header.TI_T == UNDEFINED) {
                        logger.out(Tracelog.LogType.LOG_ERROR, "Encountered texture map chunk while datatype is null.");
                        continue;
                    }
                    TMAP_Loaded = true;

                    // Chunk size includes the length of Magic and Integer value, so we have to account that
                    // we've already processed those bytes by subtracting them from the chunk size.
                    chunkSize = fileData.getInt() - (MAGIC_LENGTH * 2);
                    int texCoordSize = (model.header.VC_T.size * 2);
                    int numTexCoords = (chunkSize/texCoordSize);

                    logger.out(Tracelog.LogType.LOG_DEBUG, "Chunk size: " + chunkSize);
                    logger.out(Tracelog.LogType.LOG_DEBUG, "Expected Number of Texture Coordinates: " + numTexCoords);
                    logger.out(Tracelog.LogType.LOG_DEBUG, "Texture coordinate size: " + texCoordSize + " bytes");
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

                    break;

                case "VRTS":
                    if (VRTS_Loaded){
                        logger.out(Tracelog.LogType.LOG_ERROR, "Additional vertex data chunk encountered. Vertex data chunk must be unique.");
                        continue;
                    }
                    if (model.header.CI_T != UNDEFINED && model.header.CI_T.size < 4 && !CMAP_Loaded) {
                        logger.out(Tracelog.LogType.LOG_WARNING, "No Color map loaded prior to vertex data. There may be issues with the model.");
                    }
                    VRTS_Loaded = true;

                    // Chunk size includes the length of Magic and Integer value, so we have to account that
                    // we've already processed those bytes by subtracting them from the chunk size.
                    chunkSize = fileData.getInt() - (MAGIC_LENGTH * 2);
                    int vertexSize = (model.header.VC_T.size * 4) + model.header.CI_T.size + model.header.SK_T.size;
                    int numVertices = chunkSize / vertexSize;

                    logger.out(Tracelog.LogType.LOG_DEBUG, "Chunk size: " + chunkSize);
                    logger.out(Tracelog.LogType.LOG_DEBUG, "Expected Number of Texture Coordinates: " + numVertices);
                    logger.out(Tracelog.LogType.LOG_DEBUG, "Texture coordinate size: " + vertexSize + " bytes");
                    logger.out(Tracelog.LogType.LOG_DEBUG, "Expected End Position: " + (fileData.position() + chunkSize));

                    for (int i = 0; i < numVertices; i++) {
                        M3DJ_Vertex vertex = new M3DJ_Vertex();

                        // Load vector component
                        switch (model.header.VC_T) {
                            case INT8 -> {
                                vertex.x = (fileData.get() / 127.0);
                                vertex.y = (fileData.get() / 127.0);
                                vertex.z = (fileData.get() / 127.0);
                                vertex.w = (fileData.get() / 127.0);
                            }
                            case INT16 -> {
                                vertex.x = (fileData.getShort() / 32767.0);
                                vertex.y = (fileData.getShort() / 32767.0);
                                vertex.z = (fileData.getShort() / 32767.0);
                                vertex.w = (fileData.getShort() / 32767.0);
                            }
                            case FLOAT -> {
                                vertex.x = fileData.getFloat();
                                vertex.y = fileData.getFloat();
                                vertex.z = fileData.getFloat();
                                vertex.w = fileData.getFloat();
                            }
                            case DOUBLE -> {
                                vertex.x = fileData.getDouble();
                                vertex.y = fileData.getDouble();
                                vertex.z = fileData.getDouble();
                                vertex.w = fileData.getDouble();
                            }
                        }

                        // Load colour index component
                        switch (model.header.CI_T) {
                            case UINT8 -> {
                                vertex.colorIndex = fileData.get();
                            }
                            case UINT16 -> {
                                vertex.colorIndex = fileData.getShort();
                            }
                            case UINT32 -> {
                                vertex.colorIndex = fileData.getInt();
                            }
                            case UNDEFINED -> {
                                vertex.colorIndex = -1;
                            }
                        }

                        // Load skin index component
                        switch (model.header.SK_T) {
                            case UINT8 -> {
                                vertex.skinIndex = fileData.get();
                            }
                            case UINT16 -> {
                                vertex.skinIndex = fileData.getShort();
                            }
                            case UINT32 -> {
                                vertex.skinIndex = fileData.getInt();
                            }
                            case UNDEFINED -> {
                                vertex.skinIndex = -1;
                            }
                        }

                        model.vertices.add(vertex);
                    }
                    break;

                case "BONE":
                    if (BONE_Loaded){
                        logger.out(Tracelog.LogType.LOG_ERROR, "Additional bone data chunk encountered. Bone data chunk must be unique.");
                        continue;
                    }
                    if (model.header.BI_T == UNDEFINED) {
                        logger.out(Tracelog.LogType.LOG_ERROR, "Encountered bone data chunk while datatype is null.");
                        continue;
                    }
                    if (!VRTS_Loaded) {
                        logger.out(Tracelog.LogType.LOG_ERROR, "No vertex data was loaded prior to bone data.");
                        break;
                    }
                    TMAP_Loaded = true;
                    
                    // Chunk size includes the length of Magic and Integer value, so we have to account that
                    // we've already processed those bytes by subtracting them from the chunk size.
                    chunkSize = fileData.getInt() - (MAGIC_LENGTH * 2);

                    logger.out(Tracelog.LogType.LOG_DEBUG, "Chunk size: " + chunkSize);

                    for (int i = 0; i < chunkSize; i++) {
                        // todo: load bones
                        fileData.get();
                    }
                    break;

                case "MTRL":
                    // Chunk size includes the length of Magic and Integer value, so we have to account that
                    // we've already processed those bytes by subtracting them from the chunk size.
                    chunkSize = fileData.getInt() - (MAGIC_LENGTH * 2);

                    logger.out(Tracelog.LogType.LOG_DEBUG, "Chunk size: " + chunkSize);

                    for (int i = 0; i < chunkSize; i++) {
                        // todo: load materials
                        fileData.get();
                    }
                    break;

                case "PROC":
                    // Chunk size includes the length of Magic and Integer value, so we have to account that
                    // we've already processed those bytes by subtracting them from the chunk size.
                    chunkSize = fileData.getInt() - (MAGIC_LENGTH * 2);

                    logger.out(Tracelog.LogType.LOG_DEBUG, "Chunk size: " + chunkSize);

                    for (int i = 0; i < chunkSize; i++) {
                        // todo: load procedural surfaces
                        fileData.get();
                    }
                    break;

                case "MESH":
                    // Chunk size includes the length of Magic and Integer value, so we have to account that
                    // we've already processed those bytes by subtracting them from the chunk size.
                    chunkSize = fileData.getInt() - (MAGIC_LENGTH * 2);

                    logger.out(Tracelog.LogType.LOG_DEBUG, "Chunk size: " + chunkSize);

                    for (int i = 0; i < chunkSize; i++) {
                        // todo: load meshes
                        fileData.get();
                    }
                    break;

                case "SHPE":
                    // Chunk size includes the length of Magic and Integer value, so we have to account that
                    // we've already processed those bytes by subtracting them from the chunk size.
                    chunkSize = fileData.getInt() - (MAGIC_LENGTH * 2);

                    logger.out(Tracelog.LogType.LOG_DEBUG, "Chunk size: " + chunkSize);

                    for (int i = 0; i < chunkSize; i++) {
                        // todo: load shapes
                        fileData.get();
                    }
                    break;

                case "VOXT":
                    // Chunk size includes the length of Magic and Integer value, so we have to account that
                    // we've already processed those bytes by subtracting them from the chunk size.
                    chunkSize = fileData.getInt() - (MAGIC_LENGTH * 2);

                    logger.out(Tracelog.LogType.LOG_DEBUG, "Chunk size: " + chunkSize);

                    for (int i = 0; i < chunkSize; i++) {
                        // todo: load voxel types
                        fileData.get();
                    }
                    break;

                case "VOXD":
                    // Chunk size includes the length of Magic and Integer value, so we have to account that
                    // we've already processed those bytes by subtracting them from the chunk size.
                    chunkSize = fileData.getInt() - (MAGIC_LENGTH * 2);

                    logger.out(Tracelog.LogType.LOG_DEBUG, "Chunk size: " + chunkSize);

                    for (int i = 0; i < chunkSize; i++) {
                        // todo: load voxel data
                        fileData.get();
                    }
                    break;

                case "LBLS":
                    // Chunk size includes the length of Magic and Integer value, so we have to account that
                    // we've already processed those bytes by subtracting them from the chunk size.
                    chunkSize = fileData.getInt() - (MAGIC_LENGTH * 2);

                    logger.out(Tracelog.LogType.LOG_DEBUG, "Chunk size: " + chunkSize);

                    for (int i = 0; i < chunkSize; i++) {
                        // todo: load animation labels
                        fileData.get();
                    }
                    break;

                case "ACTN":
                    // Chunk size includes the length of Magic and Integer value, so we have to account that
                    // we've already processed those bytes by subtracting them from the chunk size.
                    chunkSize = fileData.getInt() - (MAGIC_LENGTH * 2);

                    logger.out(Tracelog.LogType.LOG_DEBUG, "Chunk size: " + chunkSize);

                    for (int i = 0; i < chunkSize; i++) {
                        // todo: load actions and animations
                        fileData.get();
                    }
                    break;

                case "ASET":
                    // Chunk size includes the length of Magic and Integer value, so we have to account that
                    // we've already processed those bytes by subtracting them from the chunk size.
                    chunkSize = fileData.getInt() - (MAGIC_LENGTH * 2);

                    logger.out(Tracelog.LogType.LOG_DEBUG, "Chunk size: " + chunkSize);

                    for (int i = 0; i < chunkSize; i++) {
                        // todo: load assets
                        fileData.get();
                    }
                    break;

                case "OMD3":
                    logger.out(Tracelog.LogType.LOG_DEBUG, "End of file reached.");
                    return model;

                default:
                    logger.out(Tracelog.LogType.LOG_WARNING, "Unexpected magic value encountered:" +
                            "\n\t" + magic + " at position " + fileData.position() + ". Attempting to skip and continue parsing...");
                    break;
            }

            logger.out(Tracelog.LogType.LOG_DEBUG, "Current position: " + fileData.position());
        }

        return model;
    }

    private static ByteBuffer DecompressDataBuffer(ByteBuffer compressedData) {
        Inflater inflater = new Inflater();
        inflater.setInput(compressedData);

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        byte[] buffer = new byte[1024];

        try {
            while (inflater.getRemaining() > 0) {
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