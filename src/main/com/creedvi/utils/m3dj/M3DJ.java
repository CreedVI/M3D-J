package com.creedvi.utils.m3dj;

import com.creedvi.utils.m3dj.io.IO;
import com.creedvi.utils.m3dj.io.Tracelog;
import com.creedvi.utils.m3dj.model.M3DJ_Model;
import com.creedvi.utils.m3dj.model.chunks.*;

import java.io.*;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Arrays;
import java.util.zip.*;

import static com.creedvi.utils.m3dj.model.chunks.VariableTypes.VariableType.UNDEFINED;
import static com.creedvi.utils.m3dj.model.chunks.M3DJ_Property.*;

public class M3DJ {

    public static final int M3D_UNDEF = -1;
    public static final int M3D_NUMBONE = 4;

    private final int MAGIC_LENGTH = 4;
    private final int M3D_BONEMAXLEVEL = 64;

    private boolean DEBUG = false;
    private boolean VERTEX_MAX = false;
    private boolean CMAP_Loaded = false;
    private boolean TMAP_Loaded = false;
    private boolean VRTS_Loaded = false;
    private boolean BONE_Loaded = false;
    private boolean VOXT_Loaded = false;


    private Tracelog logger;

    /**
     * Create new M3DJ Parser using the default log verbosity.
     */
    public M3DJ() {
        this.logger = new Tracelog(Tracelog.LogLevel.LEVEL_ERROR);
    }

    /**
     * Create new M3DJ Parser specifying the desired log verbosity.
     * Tracelog.LogLevel provides static values for verbosity.
     * They are, from least to most verbose, LEVEL_INFO, LEVEL_WARNING, LEVEL_ERROR, LEVEL_DEBUG.
     * Each level will produce all entries up to its value.
     * @param verbosity Logging level to be output
     */
    public M3DJ(int verbosity) {
        this.logger = new Tracelog(verbosity);
    }


    /**
     * Set to configure the parser to evaluate vertex maximums.
     * Default is disabled.
     * @param b true enables maximum parsing; false disables maximum parsing.
     */
    public void EnableVertexMax(boolean b) {
        VERTEX_MAX = b;
    }

    /**
     * Loads a 3D model from an M3D format file (.m3d, .a3d).
     * @param fileName String path to the file location.
     * @return M3DJ object of the model specified by the given file.
     * @throws IOException if the file fails to load into memory.
     */
    public M3DJ_Model LoadFile(String fileName) throws IOException {
        M3DJ_Model result = new M3DJ_Model();
        int fileSize;

        CMAP_Loaded = false;
        TMAP_Loaded = false;
        VRTS_Loaded = false;
        BONE_Loaded = false;
        VOXT_Loaded = false;

        if (fileName.substring(fileName.lastIndexOf(".")).equalsIgnoreCase(".m3d") ||
            fileName.substring(fileName.lastIndexOf(".")).equalsIgnoreCase(".a3d")) {
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
                logger.out(Tracelog.LogType.LOG_WARNING, "ASCII parsing is not supported at this time! Object returned will be null...");
                result = M3DJ_LoadAscii(fileData);
            }
            else {
                logger.out(Tracelog.LogType.LOG_WARNING, "Bad magic identified. Returning null object.");
                return null;
            }
        }

        return result;
    }

    private M3DJ_Model M3DJ_LoadAscii(ByteBuffer fileData) {
        M3DJ_Model result = new M3DJ_Model();

        while (fileData.hasRemaining()) {
            //todo:
        }

        return null;
    }

    private M3DJ_Model M3DJ_LoadBinary(ByteBuffer fileData) {
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
            model.header.VC_T = VariableTypes.GetVertexCoordTypeByBytePattern(((bitField >> 0) & 3));
            model.header.VI_T = VariableTypes.GetVariableTypeByBytePattern(((bitField >> 2) & 3));
            model.header.SI_T = VariableTypes.GetVariableTypeByBytePattern(((bitField >> 4) & 3));
            model.header.CI_T = VariableTypes.GetVariableTypeByBytePattern(((bitField >> 6) & 3));
            model.header.TI_T = VariableTypes.GetVariableTypeByBytePattern(((bitField >> 8) & 3));
            model.header.BI_T = VariableTypes.GetVariableTypeByBytePattern(((bitField >> 10) & 3));
            model.header.NB_T = VariableTypes.GetBonesPerVertexByBytePattern(((bitField >> 12) & 3));
            model.header.SK_T = VariableTypes.GetVariableTypeByBytePattern(((bitField >> 14) & 3));
            model.header.FC_T = VariableTypes.GetVariableTypeByBytePattern(((bitField >> 16) & 3));
            model.header.HI_T = VariableTypes.GetVariableTypeByBytePattern(((bitField >> 18) & 3));
            model.header.FI_T = VariableTypes.GetVariableTypeByBytePattern(((bitField >> 20) & 3));
            model.header.VD_T = VariableTypes.GetVariableTypeByBytePattern(((bitField >> 22) & 3));
            model.header.VP_T = VariableTypes.GetVariableTypeByBytePattern(((bitField >> 24) & 3));

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

            while (fileData.position() < chunkSize) {
                model.header.stringTable.add(ReadString(fileData, 0));
            }

            model.header.title = model.header.stringTable.get(0);
            model.header.licence = model.header.stringTable.get(1);
            model.header.author = model.header.stringTable.get(2);
            model.header.description = model.header.stringTable.get(3);


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

        int endChunkPosition = fileData.limit() - 4;
        magic = new StringBuilder();
        for (int i = 0; i < MAGIC_LENGTH; i++) {
            magic.append((char) (fileData.get(endChunkPosition + i)));
        }
        if (!magic.toString().equals("OMD3")) {
            logger.out(Tracelog.LogType.LOG_ERROR, "Missing end chunk. Returning null object...");
            return null;
        }

        if(model.header.NB_T.value > M3D_NUMBONE) {
            logger.out(Tracelog.LogType.LOG_ERROR, "Model has more bones per vertex than what importer was configured to support");

        }

        int chunkEnd = 0;

        while (fileData.hasRemaining()) {
            magic = new StringBuilder();
            for (int i = 0; i < MAGIC_LENGTH; i++) {
                magic.append((char) (fileData.get()));
            }

            // OMD3 indicated the end of the file and does not have a size component.
            if (!magic.toString().equals("OMD3")) {
                // Chunk size includes the length of Magic and Integer value, so we have to account that
                // we've already processed those bytes by subtracting them from the chunk size.
                chunkSize = fileData.getInt() - (MAGIC_LENGTH * 2);
                chunkEnd = fileData.position() + chunkSize;
            }

            logger.out(Tracelog.LogType.LOG_DEBUG, "===");
            logger.out(Tracelog.LogType.LOG_DEBUG, "Magic reads: " + magic);

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

                    int colorSize = model.header.CI_T.size;
                    int numColors = chunkSize / colorSize;

                    logger.out(Tracelog.LogType.LOG_DEBUG, "Chunk size: " + chunkSize);
                    logger.out(Tracelog.LogType.LOG_DEBUG, "Expected Number of Colours: " + numColors);
                    logger.out(Tracelog.LogType.LOG_DEBUG, "Colour unit size: " + colorSize + " bytes");
                    logger.out(Tracelog.LogType.LOG_DEBUG, "Expected End Position: " + chunkEnd);

                    while (fileData.position() < chunkEnd) {
                        M3DJ_Color color = new M3DJ_Color();
                        color.a = fileData.get();
                        color.b = fileData.get();
                        color.g = fileData.get();
                        color.r = fileData.get();

                        model.colors.add(color);
                    }

                    logger.out(Tracelog.LogType.LOG_DEBUG, "Colours Loaded: " + model.colors.size());

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

                    int texCoordSize = (model.header.VC_T.size * 2);
                    int numTexCoords = (chunkSize/texCoordSize);

                    logger.out(Tracelog.LogType.LOG_DEBUG, "Current position: " + fileData.position());
                    logger.out(Tracelog.LogType.LOG_DEBUG, "Chunk size: " + chunkSize);
                    logger.out(Tracelog.LogType.LOG_DEBUG, "Expected Number of Texture Coordinates: " + numTexCoords);
                    logger.out(Tracelog.LogType.LOG_DEBUG, "Texture coordinate size: " + texCoordSize + " bytes");
                    logger.out(Tracelog.LogType.LOG_DEBUG, "Expected End Position: " + chunkEnd);

                    for (int i = 0; i < numTexCoords; i++) {
                        switch (model.header.VC_T) {
                            case INT8 -> {
                                byte u = fileData.get();
                                byte v = fileData.get();
                                model.textureMap.add(new M3DJ_TextureCoordinate(u/255.0, v/255.0));
                            }
                            case INT16 -> {
                                short u = fileData.getShort();
                                short v = fileData.getShort();
                                model.textureMap.add(new M3DJ_TextureCoordinate(u/35535.0, v/35535.0));
                            }
                            case FLOAT -> {
                                float u = fileData.getFloat();
                                float v = fileData.getFloat();
                                model.textureMap.add(new M3DJ_TextureCoordinate(u, v));
                            }
                            case DOUBLE -> {
                                double u = fileData.getDouble();
                                double v = fileData.getDouble();
                                model.textureMap.add(new M3DJ_TextureCoordinate(u, v));
                            }
                        }
                    }
                    logger.out(Tracelog.LogType.LOG_DEBUG, "Texture Coordinates Loaded: " + model.textureMap.size());

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

                    int vertexSize = (model.header.VC_T.size * 4) + model.header.CI_T.size + model.header.SK_T.size;
                    int numVertices = chunkSize / vertexSize;

                    logger.out(Tracelog.LogType.LOG_DEBUG, "Current position: " + fileData.position());
                    logger.out(Tracelog.LogType.LOG_DEBUG, "Chunk size: " + chunkSize);
                    logger.out(Tracelog.LogType.LOG_DEBUG, "Expected Number of Texture Coordinates: " + numVertices);
                    logger.out(Tracelog.LogType.LOG_DEBUG, "Texture coordinate size: " + vertexSize + " bytes");
                    logger.out(Tracelog.LogType.LOG_DEBUG, "Expected End Position: " + chunkEnd);

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
                                if (!model.colors.isEmpty()) {
                                    vertex.colorIndex = fileData.get();
                                }
                                else {
                                    vertex.colorIndex = 0;
                                }
                            }
                            case UINT16 -> {
                                if (!model.colors.isEmpty()) {
                                    vertex.colorIndex = fileData.getShort();
                                }
                                else {
                                    vertex.colorIndex = 0;
                                }
                            }
                            case UINT32 -> {
                                vertex.colorIndex = fileData.getInt();
                            }
                            case UNDEFINED -> {
                                vertex.colorIndex = 0;
                            }
                        }
                        vertex.skinIndex = GetIndex(fileData, model.header.SK_T.size);

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
                    BONE_Loaded = true;

                    logger.out(Tracelog.LogType.LOG_DEBUG, "Current position: " + fileData.position());
                    logger.out(Tracelog.LogType.LOG_DEBUG, "Chunk size: " + chunkSize);

                    for (int i = 0; i < chunkSize; i++) {
                        // todo: load bones
                        fileData.get();
                    }
                    break;

                case "MTRL":
                    logger.out(Tracelog.LogType.LOG_DEBUG, "Current position: " + fileData.position());
                    logger.out(Tracelog.LogType.LOG_DEBUG, "Chunk size: " + chunkSize);
                    logger.out(Tracelog.LogType.LOG_DEBUG, "Expected End Position: " + chunkEnd);

                    M3DJ_Material material = new M3DJ_Material();
                    material.name = ReadString(fileData, 0);

                    for (M3DJ_Material mat: model.materials) {
                        if (mat.name.equals(material.name)) {
                            logger.out(Tracelog.LogType.LOG_ERROR, "Multiple definitions for material " + material.name + ".");
                            break;
                        }
                    }

                    while (fileData.position() < chunkEnd) {
                        // todo: load materials
                        M3DJ_Property property = new M3DJ_Property();

                        int propValue = Byte.toUnsignedInt(fileData.get());

                        if (propValue >= 128) {
                            property.format = PropertyFormat.MAP;
                        }
                        else {
                            for (int j = 0; j < propertyTypes.length; j++) {
                                if (propValue == propertyTypes[j].id) {
                                    property.format = propertyTypes[j].format;
                                    break;
                                }
                            }
                        }

                        switch (property.format) {
                            case COLOR:
                                switch (model.header.CI_T) {
                                    case UINT8:
                                        if (!model.colors.isEmpty()) {
                                            property.SetPropertyValue((int) fileData.get());
                                        }
                                        else {
                                            property.SetPropertyValue(0);
                                        }
                                        fileData.get();
                                        break;
                                    case UINT16:
                                        if (!model.colors.isEmpty()) {
                                            property.SetPropertyValue(fileData.getShort());
                                        }
                                        else {
                                            property.SetPropertyValue(0);
                                        }
                                        break;
                                    case UINT32:
                                        if (!model.colors.isEmpty()) {
                                            property.SetPropertyValue(fileData.getInt());
                                        }
                                        else {
                                            property.SetPropertyValue(0);
                                        }
                                        break;
                                }
                                break;

                            case UINT8:
                                property.SetPropertyValue(fileData.get());
                                fileData.get();
                                break;
                            case UINT16:
                                property.SetPropertyValue(fileData.getShort());
                                break;
                            case UINT32:
                                property.SetPropertyValue(fileData.getInt());
                                break;
                            case FLOAT:
                                property.SetPropertyValue(fileData.getFloat());
                                break;

                            case MAP:
                                String name = ReadString(fileData, model.header.SI_T.size);
                                //todo: get textureId from string...
                                //property.SetPropertyValue();
                                break;
                            default:
                                logger.out(Tracelog.LogType.LOG_WARNING, "Unknown material property encountered in " + material.name);
                                break;
                        }

                        material.properties.add(property);
                    }

                    model.materials.add(material);
                    break;

                case "PROC":
                    logger.out(Tracelog.LogType.LOG_DEBUG, "Current position: " + fileData.position());
                    logger.out(Tracelog.LogType.LOG_DEBUG, "Chunk size: " + chunkSize);

                    for (int i = 0; i < chunkSize; i++) {
                        // todo: load procedural surfaces
                        fileData.get();
                    }
                    break;

                case "MESH":
                    if (!VRTS_Loaded) {
                        logger.out(Tracelog.LogType.LOG_ERROR, "No vertex data loaded prior to mesh data.");
                    }

                    logger.out(Tracelog.LogType.LOG_DEBUG, "Current position: " + fileData.position());
                    logger.out(Tracelog.LogType.LOG_DEBUG, "Chunk size: " + chunkSize);
                    logger.out(Tracelog.LogType.LOG_DEBUG, "Expected End Position: " + (fileData.position() + chunkSize));

                    int materialIndex = M3D_UNDEF;
                    int parameterIndex = M3D_UNDEF;

                    for (; fileData.position() < chunkEnd; ) {

                        byte recordMagic = fileData.get();
                        byte n = (byte) (recordMagic >> 4);
                        byte k = (byte) (recordMagic & 15);

                        logger.out(Tracelog.LogType.LOG_DEBUG, "Record magic: " + recordMagic);
                        logger.out(Tracelog.LogType.LOG_DEBUG, "n magic: " + n);
                        logger.out(Tracelog.LogType.LOG_DEBUG, "k magic: " + k);

                        if(n == 0) {
                            if (k == 0) {
                                String name = ReadString(fileData, model.header.SI_T.size);
                                if (!name.isEmpty()) {
                                    for (int i = 0; i < model.materials.size(); i++) {
                                        if (name.equals(model.materials.get(i).name)) {
                                            materialIndex = i;
                                            break;
                                        }
                                    }
                                    if (materialIndex == M3D_UNDEF) {
                                        logger.out(Tracelog.LogType.LOG_ERROR, "Model references unknown material: " + name + ".");
                                    }
                                }
                            }
                            else {
                                String name = ReadString(fileData, model.header.SI_T.size);
                                if (VERTEX_MAX) {
                                    if (!name.isEmpty()) {
                                        for (int i = 0; i < model.parameters.size(); i++) {
                                            if (name.equals(model.parameters.get(i).name)) {
                                                parameterIndex = i;
                                                break;
                                            }
                                        }
                                        if (parameterIndex == M3D_UNDEF) {
                                            M3DJ_Parameter p = new M3DJ_Parameter();
                                            p.name = name;
                                            p.count = 0;
                                            model.parameters.add(p);
                                            parameterIndex = model.parameters.size();
                                        }
                                    }
                                }
                                continue;
                            }
                        }

                        if (n != 3) {
                            logger.out(Tracelog.LogType.LOG_ERROR, "Only triangle meshes are supported by M3D SDK at this time. Returning null object...");
                            return null;
                        }

                        M3DJ_Face face = new M3DJ_Face();
                        face.materialId = materialIndex;
                        face.paramId = parameterIndex;

                        int j;
                        for (j = 0; fileData.position() < chunkEnd && j < n; j++) {
                            face.vertices[j] = GetIndex(fileData, model.header.VI_T.size);

                            if((k & 1) != 0) {
                                face.texCoords[j] = GetIndex(fileData, model.header.TI_T.size);
                            }

                            if((k & 2) != 0) {
                                face.normals[j] = GetIndex(fileData, model.header.VI_T.size);
                            }

                            if ((k & 4) != 0) {
                                if(VERTEX_MAX) {
                                    face.vertMax[j] = GetIndex(fileData, model.header.VI_T.size);
                                }
                                else {
                                    fileData.position(fileData.position() + model.header.VI_T.size);
                                }
                            }
                        }
                        if (j != n) {
                            logger.out(Tracelog.LogType.LOG_ERROR, "Invalid mesh found. Returning null object...");
                            return null;
                        }
                        model.faces.add(face);
                    }
                    break;

                case "SHPE":
                    logger.out(Tracelog.LogType.LOG_DEBUG, "Current position: " + fileData.position());
                    logger.out(Tracelog.LogType.LOG_DEBUG, "Chunk size: " + chunkSize);

                    for (int i = 0; i < chunkSize; i++) {
                        // todo: load shapes
                        fileData.get();
                    }
                    break;

                case "VOXT":
                    VOXT_Loaded = true;

                    logger.out(Tracelog.LogType.LOG_DEBUG, "Current position: " + fileData.position());
                    logger.out(Tracelog.LogType.LOG_DEBUG, "Chunk size: " + chunkSize);

                    for (int i = 0; i < chunkSize; i++) {
                        // todo: load voxel types
                        fileData.get();
                    }
                    break;

                case "VOXD":
                    logger.out(Tracelog.LogType.LOG_DEBUG, "Current position: " + fileData.position());
                    logger.out(Tracelog.LogType.LOG_DEBUG, "Chunk size: " + chunkSize);

                    for (int i = 0; i < chunkSize; i++) {
                        // todo: load voxel data
                        fileData.get();
                    }
                    break;

                case "LBLS":
                    logger.out(Tracelog.LogType.LOG_DEBUG, "Current position: " + fileData.position());
                    logger.out(Tracelog.LogType.LOG_DEBUG, "Chunk size: " + chunkSize);

                    for (int i = 0; i < chunkSize; i++) {
                        // todo: load animation labels
                        fileData.get();
                    }
                    break;

                case "ACTN":
                    logger.out(Tracelog.LogType.LOG_DEBUG, "Current position: " + fileData.position());
                    logger.out(Tracelog.LogType.LOG_DEBUG, "Chunk size: " + chunkSize);

                    for (int i = 0; i < chunkSize; i++) {
                        // todo: load actions and animations
                        fileData.get();
                    }
                    break;

                case "ASET":
                    logger.out(Tracelog.LogType.LOG_DEBUG, "Current position: " + fileData.position());
                    logger.out(Tracelog.LogType.LOG_DEBUG, "Chunk size: " + chunkSize);

                    for (int i = 0; i < chunkSize; i++) {
                        // todo: load assets
                        fileData.get();
                    }
                    break;

                case "OMD3":
                    logger.out(Tracelog.LogType.LOG_DEBUG, "Current position: " + fileData.position());
                    logger.out(Tracelog.LogType.LOG_DEBUG, "Expected end position: " + fileData.capacity());
                    logger.out(Tracelog.LogType.LOG_DEBUG, "End of file reached.");
                    return model;

                default:
                    logger.out(Tracelog.LogType.LOG_WARNING, "Unexpected magic value encountered:" +
                            "\n\t" + magic + " at position " + fileData.position() + ". Attempting to skip and continue parsing...");
                    break;
            }

            logger.out(Tracelog.LogType.LOG_DEBUG, "Current position: " + fileData.position());
        }

        // Model is only valid if end chunk exists.
        return null;
    }

    private int GetIndex(ByteBuffer fileData, int indexSize) {
        return switch (indexSize) {
            case 1 -> fileData.get();
            case 2 -> fileData.getShort();
            case 4 -> fileData.getInt();
            default -> 0;
        };
    }

    private String ReadString(ByteBuffer fileData, int stringOffset) {
        String result = "";
        char c;

        do {
            c = (char) fileData.get();
            if (c != '\0') {
                result += c;
            }
        } while(c != '\0');

        for (int i = 0; i < stringOffset; i++) {
            fileData.get();
        }

        return result;
    }

    private ByteBuffer DecompressDataBuffer(ByteBuffer compressedData) {
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