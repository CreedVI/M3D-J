package com.creedvi.utils.m3dj;

import com.creedvi.utils.m3dj.io.IO;
import com.creedvi.utils.m3dj.io.Tracelog;
import com.creedvi.utils.m3dj.model.M3DJ_Model;
import com.creedvi.utils.m3dj.model.chunks.*;

import java.io.*;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.zip.*;

import static com.creedvi.utils.m3dj.model.chunks.VariableTypes.VariableType.UNDEFINED;
import static com.creedvi.utils.m3dj.model.chunks.M3DJ_Property.*;

public class M3DJ {

    public static final int M3D_UNDEF = -1;
    public static final int M3D_NUMBONE = 4;

    private final int MAGIC_LENGTH = 4;
    private final int M3D_BONEMAXLEVEL = 64;

    private boolean processVertexMax = false;
    private boolean processExtras = false;
    private boolean CMAP_Loaded = false;
    private boolean TMAP_Loaded = false;
    private boolean VRTS_Loaded = false;
    private boolean BONE_Loaded = false;
    private boolean VOXT_Loaded = false;


    private final Tracelog logger;

    /**
     * Create new M3DJ Parser using the default log verbosity.
     */
    public M3DJ() {
        this.logger = new Tracelog(Tracelog.LogLevel.LEVEL_ERROR);
    }

    /**
     * Create new M3DJ Parser specifying the desired log verbosity.
     * Tracelog.LogLevel provides static values for verbosity.
     * They are, from least to most verbose, LEVEL_ERROR, LEVEL_WARNING, LEVEL_INFO,  LEVEL_DEBUG.
     * Each level will produce all entries up to its value.
     *
     * @param verbosity Logging level to be output
     */
    public M3DJ(int verbosity) {
        this.logger = new Tracelog(verbosity);
    }

    /**
     * Keeps the M3DJ Parser from pushing any log info to the console.
     */
    public void Mute() {
        this.logger.SetMute(true);
    }

    /**
     * Sets the M3DJ Parser to output logs at the default level.
     */
    public void Unmute() {
        this.logger.SetMute(false);
    }

    /**
     * Sets the M3DJ Parser to output logs at the desired log verbosity.
     * Tracelog.LogLevel provides static values for verbosity.
     * They are, from least to most verbose, LEVEL_ERROR, LEVEL_WARNING, LEVEL_INFO,  LEVEL_DEBUG.
     * Each level will produce all entries up to its value.
     */
    public void SetVerbosity(int verbosity) {
        this.logger.SetLogLevel(verbosity);
    }


    /**
     * Configure the parser to evaluate vertex maximums.
     * Default is disabled.
     *
     * @param b true enables maximum parsing; false disables maximum parsing.
     */
    public void ProcessVertexMax(boolean b) {
        processVertexMax = b;
    }


    /**
     * Configures the parser to Extras as defined in the M3D specification.
     * Default is disabled.
     *
     * @param b true enables Extras parsing; false disables Extras parsing.
     */
    public void ProcessExtras(boolean b) {
        processExtras = b;
    }

    /**
     * Loads a 3D model from an M3D format file (.m3d, .a3d).
     *
     * @param fileName String path to the file location.
     * @return M3DJ object of the model specified by the given file. NULL in case where m3d model is not formatted properly.
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

        if(fileName.substring(fileName.lastIndexOf(".")).equalsIgnoreCase(".m3d") ||
                fileName.substring(fileName.lastIndexOf(".")).equalsIgnoreCase(".a3d")) {
            ByteBuffer fileData = ByteBuffer.wrap(IO.LoadFileData(fileName));
            fileData.order(ByteOrder.LITTLE_ENDIAN);

            StringBuilder magic = new StringBuilder();
            for(int i = 0; i < MAGIC_LENGTH; i++) {
                magic.append((char) (fileData.get()));
            }

            if(magic.toString().equals("3DMO")) {
                fileSize = fileData.getInt();
                logger.Out(Tracelog.LogType.LOG_INFO, "Binary magic found. File size: " + fileSize + "B");
                result = M3DJ_LoadBinary(fileData);
            }
            else if(magic.toString().equals("3dmo")) {
                fileSize = fileData.getInt();
                logger.Out(Tracelog.LogType.LOG_INFO, "ASCII magic found. File size: " + fileSize + "B");
                logger.Out(Tracelog.LogType.LOG_WARNING, "ASCII parsing is not supported at this time! Object returned will be null...");
                result = M3DJ_LoadAscii(fileData);
            }
            else {
                logger.Out(Tracelog.LogType.LOG_WARNING, "Bad magic identified. Returning null object.");
                return null;
            }
        }

        return result;
    }

    /**
     * Parses model information from valid ASCII encoded M3D file
     *
     * @param fileData binary data of ASCII file
     * @return M3D Model defined by file.
     */
    private M3DJ_Model M3DJ_LoadAscii(ByteBuffer fileData) {
        M3DJ_Model result = new M3DJ_Model();

        while(fileData.hasRemaining()) {
            //todo:
        }

        return null;
    }


    /**
     * Parses model information from valid Binary encoded M3D file
     *
     * @param fileData binary data of M3D model
     * @return M3D Model defined by file.
     */
    private M3DJ_Model M3DJ_LoadBinary(ByteBuffer fileData) {
        M3DJ_Model model = new M3DJ_Model();
        int chunkSize = 0;
        int chunkEnd = 0;
        int i = 0;

        StringBuilder magic = new StringBuilder();
        for(i = 0; i < MAGIC_LENGTH; i++) {
            magic.append((char) (fileData.get()));
        }

        if(magic.toString().equals("PRVW")) {
            chunkSize = fileData.getInt();

            model.preview.allocateImageBuffer(chunkSize);

            for(i = 0; i < chunkSize; i++) {
                model.preview.imageData.put(fileData.get());
            }

            model.preview.imageData.flip();
            model.preview.hasPreview = true;

            magic = new StringBuilder();
            for(i = 0; i < MAGIC_LENGTH; i++) {
                magic.append((char) (fileData.get()));
            }
        }

        if(!magic.toString().equals("HEAD")) {
            logger.Out(Tracelog.LogType.LOG_INFO, "Failed to identify header; assuming compressed data and attempting to decompress...");
            fileData = DecompressDataBuffer(fileData.slice(fileData.position() - (Byte.BYTES * 4), fileData.remaining()));

            magic = new StringBuilder();
            for(i = 0; i < MAGIC_LENGTH; i++) {
                magic.append((char) (fileData.get()));
            }
        }

        if(magic.toString().equals("HEAD")) {
            chunkSize = fileData.getInt();
            logger.Out(Tracelog.LogType.LOG_DEBUG, "Header chunk size: " + chunkSize);

            model.header.scale = fileData.getFloat();
            if(model.header.scale <= 0.0f) {
                model.header.scale = 1.0f;
            }
            logger.Out(Tracelog.LogType.LOG_DEBUG, "Scaling factor: " + model.header.scale);

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

            while(fileData.position() < chunkSize) {
                String s = "";
                char c;
                do {
                    c = (char) fileData.get();
                    if(c != '\0') {
                        s += c;
                    }
                } while(c != '\0');

                model.header.stringTable.add(s);
            }

            model.header.title = model.header.stringTable.get(0);
            model.header.licence = model.header.stringTable.get(1);
            model.header.author = model.header.stringTable.get(2);
            model.header.description = model.header.stringTable.get(3);


            logger.Out(Tracelog.LogType.LOG_INFO,
                       "Model metadata:\n" +
                            "\tModel: " + model.header.title + "\n" +
                            "\tLicence: " + model.header.licence + "\n" +
                            "\tAuthor: " + model.header.author + "\n" +
                            "\tDescription: " + model.header.description
            );
        }
        else {
            logger.Out(Tracelog.LogType.LOG_WARNING, "Bad data found. Failed to identify Header chunk where expected. Returning null object...");
            return null;
        }

        // Basic file validation
        if(model.header.VC_T.size > 4) {
            logger.Out(Tracelog.LogType.LOG_WARNING, "Double precision coordinates are not supported, coordinates will be truncated to float...");
        }

        if(model.header.VI_T.size > 4 || model.header.SI_T.size > 4 || model.header.VP_T.size == 4) {
            logger.Out(Tracelog.LogType.LOG_ERROR, "Invalid index size, unable to load model. Returning null object...");
            return null;
        }

        int endChunkPosition = fileData.limit() - 4;
        magic = new StringBuilder();
        for(i = 0; i < MAGIC_LENGTH; i++) {
            magic.append((char) (fileData.get(endChunkPosition + i)));
        }
        if(!magic.toString().equals("OMD3")) {
            logger.Out(Tracelog.LogType.LOG_ERROR, "Missing end chunk. Returning null object...");
            return null;
        }

        if(model.header.NB_T.value > M3D_NUMBONE) {
            logger.Out(Tracelog.LogType.LOG_ERROR, "Model has more bones per vertex than what importer was configured to support");

        }

        // Read through file data to preload in-lined assets
        int headerEndPosition = fileData.position();
        while (fileData.hasRemaining()) {
            magic = new StringBuilder();
            for(i = 0; i < MAGIC_LENGTH; i++) {
                magic.append((char) (fileData.get()));
            }

            // OMD3 indicated the end of the file and does not have a size component.
            if(!magic.toString().equals("OMD3")) {
                // Chunk size includes the length of Magic and Integer value, so we have to account that
                // we've already processed those bytes by subtracting them from the chunk size.
                chunkSize = fileData.getInt() - (MAGIC_LENGTH * 2);
                chunkEnd = fileData.position() + chunkSize;
            }
            else {
                break;
            }

            if (magic.toString().equals("ASET")) {
                M3DJ_Asset asset = new M3DJ_Asset(chunkSize);
                asset.name = GetString(fileData, model.header.SI_T.size);
                while (fileData.position() < chunkEnd) {
                    asset.assetData.put(fileData.get());
                }
                model.assets.add(asset);
            }
            else {
                fileData.position(chunkEnd);
            }
        }

        fileData.position(headerEndPosition);

        while(fileData.hasRemaining()) {
            magic = new StringBuilder();
            for(i = 0; i < MAGIC_LENGTH; i++) {
                magic.append((char) (fileData.get()));
            }

            // OMD3 indicated the end of the file and does not have a size component.
            if(!magic.toString().equals("OMD3")) {
                // Chunk size includes the length of Magic and Integer value, so we have to account that
                // we've already processed those bytes by subtracting them from the chunk size.
                chunkSize = fileData.getInt() - (MAGIC_LENGTH * 2);
                chunkEnd = fileData.position() + chunkSize;
            }

            logger.Out(Tracelog.LogType.LOG_DEBUG, "===");
            logger.Out(Tracelog.LogType.LOG_DEBUG, "Magic reads: " + magic);

            switch(magic.toString()) {
                case "CMAP":
                    if(CMAP_Loaded) {
                        logger.Out(Tracelog.LogType.LOG_ERROR, "Additional color map chunk encountered. Color map chunk must be unique.");
                        continue;
                    }
                    if(model.header.CI_T == UNDEFINED) {
                        logger.Out(Tracelog.LogType.LOG_ERROR, "Encountered color map chunk while datatype is null.");
                        continue;
                    }
                    CMAP_Loaded = true;

                    int colorSize = model.header.CI_T.size;
                    int numColors = chunkSize / colorSize;

                    logger.Out(Tracelog.LogType.LOG_DEBUG, "Chunk size: " + chunkSize);
                    logger.Out(Tracelog.LogType.LOG_DEBUG, "Expected Number of Colours: " + numColors);
                    logger.Out(Tracelog.LogType.LOG_DEBUG, "Colour unit size: " + colorSize + " bytes");
                    logger.Out(Tracelog.LogType.LOG_DEBUG, "Expected End Position: " + chunkEnd);

                    while(fileData.position() < chunkEnd) {
                        M3DJ_Color color = new M3DJ_Color();
                        color.r = Byte.toUnsignedInt(fileData.get());
                        color.g = Byte.toUnsignedInt(fileData.get());
                        color.b = Byte.toUnsignedInt(fileData.get());
                        color.a = Byte.toUnsignedInt(fileData.get());

                        model.colors.add(color);
                    }

                    logger.Out(Tracelog.LogType.LOG_DEBUG, "Colours Loaded: " + model.colors.size());

                    break;

                case "TMAP":
                    if(TMAP_Loaded) {
                        logger.Out(Tracelog.LogType.LOG_ERROR, "Additional texture map chunk encountered. Texture map chunk must be unique.");
                        continue;
                    }
                    if(model.header.TI_T == UNDEFINED) {
                        logger.Out(Tracelog.LogType.LOG_ERROR, "Encountered texture map chunk while datatype is null.");
                        continue;
                    }
                    TMAP_Loaded = true;

                    int texCoordSize = (model.header.VC_T.size * 2);
                    int numTexCoords = (chunkSize / texCoordSize);

                    logger.Out(Tracelog.LogType.LOG_DEBUG, "Current position: " + fileData.position());
                    logger.Out(Tracelog.LogType.LOG_DEBUG, "Chunk size: " + chunkSize);
                    logger.Out(Tracelog.LogType.LOG_DEBUG, "Expected Number of Texture Coordinates: " + numTexCoords);
                    logger.Out(Tracelog.LogType.LOG_DEBUG, "Texture coordinate size: " + texCoordSize + " bytes");
                    logger.Out(Tracelog.LogType.LOG_DEBUG, "Expected End Position: " + chunkEnd);

                    for(i = 0; i < numTexCoords; i++) {
                        switch(model.header.VC_T) {
                            case INT8 -> {
                                float u = Byte.toUnsignedInt(fileData.get());
                                float v = Byte.toUnsignedInt(fileData.get());
                                model.textureMap.add(new M3DJ_TextureCoordinate(u / 255.0f, v / 255.0f));
                            }
                            case INT16 -> {
                                float u = fileData.getShort();
                                float v = fileData.getShort();
                                model.textureMap.add(new M3DJ_TextureCoordinate(u / 35535.0f, v / 35535.0f));
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
                    logger.Out(Tracelog.LogType.LOG_DEBUG, "Texture Coordinates Loaded: " + model.textureMap.size());

                    break;

                case "VRTS":
                    if(VRTS_Loaded) {
                        logger.Out(Tracelog.LogType.LOG_ERROR, "Additional vertex data chunk encountered. Vertex data chunk must be unique.");
                        continue;
                    }
                    if(model.header.CI_T != UNDEFINED && model.header.CI_T.size < 4 && !CMAP_Loaded) {
                        logger.Out(Tracelog.LogType.LOG_WARNING, "No Color map loaded prior to vertex data. There may be issues with the model.");
                    }
                    VRTS_Loaded = true;

                    int vertexSize = (model.header.VC_T.size * 4) + model.header.CI_T.size + model.header.SK_T.size;
                    int numVertices = chunkSize / vertexSize;

                    logger.Out(Tracelog.LogType.LOG_DEBUG, "Current position: " + fileData.position());
                    logger.Out(Tracelog.LogType.LOG_DEBUG, "Chunk size: " + chunkSize);
                    logger.Out(Tracelog.LogType.LOG_DEBUG, "Expected Number of Texture Coordinates: " + numVertices);
                    logger.Out(Tracelog.LogType.LOG_DEBUG, "Texture coordinate size: " + vertexSize + " bytes");
                    logger.Out(Tracelog.LogType.LOG_DEBUG, "Expected End Position: " + chunkEnd);

                    for(i = 0; i < numVertices; i++) {
                        M3DJ_Vertex vertex = new M3DJ_Vertex();

                        // Load vector component
                        switch(model.header.VC_T) {
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
                        switch(model.header.CI_T) {
                            case UINT8 -> {
                                if(!model.colors.isEmpty()) {
                                    vertex.colorIndex = fileData.get();
                                }
                                else {
                                    vertex.colorIndex = 0;
                                }
                            }
                            case UINT16 -> {
                                if(!model.colors.isEmpty()) {
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
                    if(BONE_Loaded) {
                        logger.Out(Tracelog.LogType.LOG_ERROR, "Additional bone data chunk encountered. Bone data chunk must be unique.");
                        continue;
                    }
                    if(model.header.BI_T == UNDEFINED) {
                        logger.Out(Tracelog.LogType.LOG_ERROR, "Encountered bone data chunk while datatype is null.");
                        continue;
                    }
                    if(!VRTS_Loaded) {
                        logger.Out(Tracelog.LogType.LOG_ERROR, "No vertex data was loaded prior to bone data.");
                        break;
                    }
                    BONE_Loaded = true;

                    int boneCount = GetIndex(fileData, model.header.BI_T.size);
                    int skinCount = GetIndex(fileData, model.header.SK_T.size);

                    logger.Out(Tracelog.LogType.LOG_DEBUG, "Current position: " + fileData.position());
                    logger.Out(Tracelog.LogType.LOG_DEBUG, "Chunk size: " + chunkSize);
                    logger.Out(Tracelog.LogType.LOG_DEBUG, "Expected number of bone records: " + boneCount);
                    logger.Out(Tracelog.LogType.LOG_DEBUG, "Expected number of skin records: " + skinCount);
                    logger.Out(Tracelog.LogType.LOG_DEBUG, "Expected End Position: " + chunkEnd);

                    for(i = 0; fileData.position() < chunkEnd && i < boneCount; i++) {
                        M3DJ_Bone bone = new M3DJ_Bone();
                        bone.parentIndex = GetIndex(fileData, model.header.BI_T.size);
                        bone.name = GetString(fileData, model.header.SI_T.size);
                        bone.position = GetIndex(fileData, model.header.VI_T.size);
                        bone.orientation = GetIndex(fileData, model.header.VI_T.size);

                        model.bones.add(bone);
                    }

                    if(i != boneCount) {
                        logger.Out(Tracelog.LogType.LOG_ERROR, "Malformed Bone Chunk. Expected bone count: "
                                + boneCount + ". Number of bones parsed: " + i);
                        return null;
                    }

                    if(skinCount > 0) {
                        for(i = 0; fileData.position() < chunkEnd && i < skinCount; i++) {
                            M3DJ_Skin skin = new M3DJ_Skin();
                            float[] weights = new float[M3D_NUMBONE];

                            for(int j = 0; j < M3D_NUMBONE; j++) {
                                skin.boneIds[j] = M3D_UNDEF;
                                skin.weights[j] = 0.0f;
                            }

                            if(model.header.NB_T.value == 1) {
                                weights[0] = 255.0f;
                            }
                            else {
                                weights[0] = fileData.get();
                                weights[1] = fileData.get();
                                weights[2] = fileData.get();
                                weights[3] = fileData.get();
                            }

                            float w = 0.0f;
                            for(int j = 0; j < model.header.NB_T.value; j++) {
                                if(weights[j] != 0.0f) {
                                    if(j >= M3D_NUMBONE) {
                                        GetIndex(fileData, model.header.NB_T.value);
                                    }
                                    else {
                                        skin.weights[j] = weights[j] / 255.0f;
                                        w += skin.weights[j];
                                        skin.boneIds[j] = GetIndex(fileData, model.header.BI_T.size);
                                    }
                                }
                            }

                            if(w != 1.0f && w != 0.0f) {
                                for(int j = 0; j < M3D_NUMBONE; j++) {
                                    skin.weights[j] = skin.weights[j] / w;
                                }
                            }

                            model.skins.add(skin);
                        }

                        if(i != skinCount) {
                            logger.Out(Tracelog.LogType.LOG_ERROR, "Malformed Skin within Bone Chunk. Expected skin count: "
                                    + skinCount + ". Number of skins parsed: " + i);
                            return null;
                        }
                    }
                    break;

                case "MTRL":
                    logger.Out(Tracelog.LogType.LOG_DEBUG, "Current position: " + fileData.position());
                    logger.Out(Tracelog.LogType.LOG_DEBUG, "Chunk size: " + chunkSize);
                    logger.Out(Tracelog.LogType.LOG_DEBUG, "Expected End Position: " + chunkEnd);

                    M3DJ_Material material = new M3DJ_Material();
                    material.name = GetString(fileData, model.header.SI_T.size);

                    logger.Out(Tracelog.LogType.LOG_DEBUG, "Material name: " + material.name);

                    for(M3DJ_Material mat : model.materials) {
                        if(mat.name.equals(material.name)) {
                            logger.Out(Tracelog.LogType.LOG_ERROR, "Multiple definitions for material " + material.name + ".");
                            break;
                        }
                    }

                    while(fileData.position() < chunkEnd) {
                        M3DJ_Property property = new M3DJ_Property();

                        byte propValue = fileData.get();

                        if(Byte.toUnsignedInt(propValue) >= 128) {
                            property.format = PropertyFormat.MAP;
                        }
                        else {
                            for(int j = 0; j < propertyTypes.length; j++) {
                                if(propValue == propertyTypes[j].id) {
                                    property.format = propertyTypes[j].format;
                                    property.key = propertyTypes[j].key;
                                    property.id = propertyTypes[j].id;
                                    break;
                                }
                            }
                        }

                        switch(property.format) {
                            case COLOR:
                                switch(model.header.CI_T) {
                                    case UINT8:
                                        if(!model.colors.isEmpty()) {
                                            property.SetPropertyValue((int) fileData.get());
                                        }
                                        else {
                                            property.SetPropertyValue(0);
                                            fileData.get();
                                        }
                                        break;
                                    case UINT16:
                                        if(!model.colors.isEmpty()) {
                                            property.SetPropertyValue(fileData.getShort());
                                        }
                                        else {
                                            property.SetPropertyValue(0);
                                            fileData.getShort();
                                        }
                                        break;
                                    case UINT32:
                                        if(!model.colors.isEmpty()) {
                                            property.SetPropertyValue(fileData.getInt());
                                        }
                                        else {
                                            property.SetPropertyValue(0);
                                            fileData.getInt();
                                        }
                                        break;
                                }
                                break;

                            case UINT8:
                                property.SetPropertyValue(fileData.get());
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
                                String name = GetString(fileData, model.header.SI_T.size);
                                property.SetPropertyValue(LoadTexture(model, name));
                                if((int) property.GetPropertyValue() == M3D_UNDEF) {
                                    property = null;
                                }
                                break;
                            default:
                                logger.Out(Tracelog.LogType.LOG_WARNING, "Unknown material property encountered in " + material.name);
                                break;
                        }

                        if(property != null) {
                            material.properties.add(property);
                        }
                    }

                    model.materials.add(material);
                    break;

                case "PROC":
                    logger.Out(Tracelog.LogType.LOG_DEBUG, "Current position: " + fileData.position());
                    logger.Out(Tracelog.LogType.LOG_DEBUG, "Chunk size: " + chunkSize);

                    // TODO: Procedural mesh support
                    logger.Out(Tracelog.LogType.LOG_WARNING, "PROC Chunk encountered. This feature is not yet supported and this chunk will be skipped...");
                    fileData.position(chunkEnd);

                    break;

                case "MESH":
                    if(!VRTS_Loaded) {
                        logger.Out(Tracelog.LogType.LOG_ERROR, "No vertex data loaded prior to mesh data.");
                    }

                    logger.Out(Tracelog.LogType.LOG_DEBUG, "Current position: " + fileData.position());
                    logger.Out(Tracelog.LogType.LOG_DEBUG, "Chunk size: " + chunkSize);
                    logger.Out(Tracelog.LogType.LOG_DEBUG, "Expected End Position: " + chunkEnd);

                    int materialIndex = M3D_UNDEF;
                    int parameterIndex = M3D_UNDEF;

                    while(fileData.position() < chunkEnd) {
                        byte recordMagic = fileData.get();
                        byte n = (byte) (recordMagic >> 4);
                        byte k = (byte) (recordMagic & 15);

                        logger.Out(Tracelog.LogType.LOG_DEBUG, "Record magic: " + recordMagic);
                        logger.Out(Tracelog.LogType.LOG_DEBUG, "n magic: " + n);
                        logger.Out(Tracelog.LogType.LOG_DEBUG, "k magic: " + k);

                        if(n == 0) {
                            if(k == 0) {
                                String name = GetString(fileData, model.header.SI_T.size);
                                if(!name.isEmpty()) {
                                    for(i = 0; i < model.materials.size(); i++) {
                                        if(name.equals(model.materials.get(i).name)) {
                                            materialIndex = i;
                                            break;
                                        }
                                    }
                                    if(materialIndex == M3D_UNDEF) {
                                        logger.Out(Tracelog.LogType.LOG_ERROR, "Model references unknown material: " + name + ".");
                                    }
                                }
                            }
                            else {
                                String name = GetString(fileData, model.header.SI_T.size);
                                if(processVertexMax) {
                                    if(!name.isEmpty()) {
                                        for(i = 0; i < model.parameters.size(); i++) {
                                            if(name.equals(model.parameters.get(i).name)) {
                                                parameterIndex = i;
                                                break;
                                            }
                                        }
                                        if(parameterIndex == M3D_UNDEF) {
                                            M3DJ_Parameter p = new M3DJ_Parameter();
                                            p.name = name;
                                            p.count = 0;
                                            model.parameters.add(p);
                                            parameterIndex = model.parameters.size();
                                        }
                                    }
                                }
                            }
                            continue;
                        }

                        if(n != 3) {
                            logger.Out(Tracelog.LogType.LOG_ERROR, "Only triangle meshes are supported by M3D SDK at this time. Returning null object...");
                            return null;
                        }

                        M3DJ_Face face = new M3DJ_Face();
                        face.materialId = materialIndex;
                        face.paramId = parameterIndex;

                        int j;
                        for(j = 0; fileData.position() < chunkEnd && j < n; j++) {
                            face.vertices[j] = GetIndex(fileData, model.header.VI_T.size);

                            if((k & 1) != 0) {
                                face.texCoords[j] = GetIndex(fileData, model.header.TI_T.size);
                            }

                            if((k & 2) != 0) {
                                face.normals[j] = GetIndex(fileData, model.header.VI_T.size);
                            }

                            if((k & 4) != 0) {
                                if(processVertexMax) {
                                    face.vertMax[j] = GetIndex(fileData, model.header.VI_T.size);
                                }
                                else {
                                    fileData.position(fileData.position() + model.header.VI_T.size);
                                }
                            }
                        }
                        if(j != n) {
                            logger.Out(Tracelog.LogType.LOG_ERROR, "Invalid mesh found. Returning null object...");
                            return null;
                        }
                        model.faces.add(face);
                    }
                    break;

                case "SHPE":
                    logger.Out(Tracelog.LogType.LOG_DEBUG, "Current position: " + fileData.position());
                    logger.Out(Tracelog.LogType.LOG_DEBUG, "Chunk size: " + chunkSize);

                    // TODO: Shapes support
                    logger.Out(Tracelog.LogType.LOG_WARNING, "SHPE Chunk encountered. This feature is not yet supported and this chunk will be skipped...");
                    fileData.position(chunkEnd);

                    break;

                case "VOXT":
                    VOXT_Loaded = true;

                    logger.Out(Tracelog.LogType.LOG_DEBUG, "Current position: " + fileData.position());
                    logger.Out(Tracelog.LogType.LOG_DEBUG, "Chunk size: " + chunkSize);

                    // TODO: Voxel support
                    logger.Out(Tracelog.LogType.LOG_WARNING, "VOXT Chunk encountered. This feature is not yet supported and this chunk will be skipped...");
                    fileData.position(chunkEnd);

                    break;

                case "VOXD":
                    logger.Out(Tracelog.LogType.LOG_DEBUG, "Current position: " + fileData.position());
                    logger.Out(Tracelog.LogType.LOG_DEBUG, "Chunk size: " + chunkSize);

                    // TODO: Voxel support
                    logger.Out(Tracelog.LogType.LOG_WARNING, "VOXD Chunk encountered. This feature is not yet supported and this chunk will be skipped...");
                    fileData.position(chunkEnd);

                    break;

                case "LBLS":
                    int recordLength = model.header.VI_T.size + model.header.SI_T.size;
                    i = chunkSize / recordLength;

                    logger.Out(Tracelog.LogType.LOG_DEBUG, "Current position: " + fileData.position());
                    logger.Out(Tracelog.LogType.LOG_DEBUG, "Chunk size: " + chunkSize);
                    logger.Out(Tracelog.LogType.LOG_DEBUG, "Label size: " + recordLength);
                    logger.Out(Tracelog.LogType.LOG_DEBUG, "Expected number of labels: " + i);
                    logger.Out(Tracelog.LogType.LOG_DEBUG, "Expected end position: " + chunkEnd);

                    for(i = 0; i < chunkSize; i++) {
                        String labelName = GetString(fileData, model.header.SI_T.size);
                        String labelLanguage = GetString(fileData, model.header.SI_T.size);
                        if(!labelName.isBlank()) {
                            logger.Out(Tracelog.LogType.LOG_DEBUG, "Label name: " + labelName);
                        }
                        if(!labelLanguage.isBlank()) {
                            logger.Out(Tracelog.LogType.LOG_DEBUG, "Label Language: " + labelLanguage);
                        }
                        if(model.header.CI_T != null && model.header.CI_T.size < 4 && model.colors.isEmpty()) {
                            logger.Out(Tracelog.LogType.LOG_ERROR, "No color map data was loaded prior to encountering labels. Returning null object...");
                            return null;
                        }

                        int k = 0;
                        switch(model.header.CI_T.size) {
                            case 1:
                                k = model.colors.isEmpty() ? (int) fileData.get() : 0;
                                break;
                            case 2:
                                k = model.colors.isEmpty() ? (int) fileData.getShort() : 0;
                                break;
                            case 4:
                                k = fileData.getInt();
                                break;
                            case 8:
                                break;
                        }

                        while(fileData.position() < chunkEnd) {
                            M3DJ_Label label = new M3DJ_Label();

                            label.name = labelName;
                            label.language = labelLanguage;
                            label.colorId = k;
                            label.vertexId = GetIndex(fileData, model.header.VI_T.size);
                            label.text = GetString(fileData, model.header.SI_T.size);

                            model.labels.add(label);
                        }
                    }
                    break;

                case "ACTN":
                    logger.Out(Tracelog.LogType.LOG_DEBUG, "Current position: " + fileData.position());
                    logger.Out(Tracelog.LogType.LOG_DEBUG, "Chunk size: " + chunkSize);
                    logger.Out(Tracelog.LogType.LOG_DEBUG, "Expected end position: " + chunkEnd);

                    M3DJ_Action action = new M3DJ_Action();
                    action.name = GetString(fileData, model.header.SI_T.size);
                    action.frameCount = fileData.getShort();
                    action.animationLength = fileData.getInt();

                    for(i = 0; i < action.frameCount; i++) {
                        int time = fileData.getInt();
                        int count = GetIndex(fileData, model.header.FC_T.size);
                        M3DJ_Frame frame = new M3DJ_Frame(time, count);

                        for (int j = 0; j < frame.transformsCount; j++) {
                            frame.transforms[j].boneId = GetIndex(fileData, model.header.BI_T.size);
                            frame.transforms[j].position = GetIndex(fileData, model.header.VI_T.size);
                            frame.transforms[j].orientation = GetIndex(fileData, model.header.VI_T.size);
                        }

                        action.frames.add(frame);
                    }

                    model.actions.add(action);
                    break;

                case "ASET":
                    logger.Out(Tracelog.LogType.LOG_DEBUG, "Current position: " + fileData.position());
                    logger.Out(Tracelog.LogType.LOG_DEBUG, "Chunk size: " + chunkSize);
                    logger.Out(Tracelog.LogType.LOG_DEBUG, "Expected end position: " + chunkEnd);

                    // Assets are preloaded, so we skip this chunk.
                    fileData.position(chunkEnd);
                    break;

                case "OMD3":
                    logger.Out(Tracelog.LogType.LOG_DEBUG, "Current position: " + fileData.position());
                    logger.Out(Tracelog.LogType.LOG_DEBUG, "Expected end position: " + fileData.capacity());
                    logger.Out(Tracelog.LogType.LOG_DEBUG, "End of file reached.");
                    return model;

                default:
                    if(!processExtras) {
                        logger.Out(Tracelog.LogType.LOG_WARNING, "Unexpected magic value encountered:" +
                                 "\n\t" + magic + " at position " + fileData.position() + ". Attempting to skip and continue parsing...");
                    }
                    else {
                        logger.Out(Tracelog.LogType.LOG_INFO, "Nonstandard magic value encountered:" + magic + " Evaluating as an extra");
                        M3DJ_Extra extra = new M3DJ_Extra(magic.toString(), chunkSize);
                        while(fileData.position() < chunkEnd) {
                            extra.data.put(fileData.get());
                        }
                        extra.data.flip();
                        model.extras.add(extra);
                    }
                    break;
            }

            logger.Out(Tracelog.LogType.LOG_DEBUG, "Current position: " + fileData.position());
        }

        // Model is only valid if end chunk exists.
        return null;
    }

    /**
     * Returns value from data buffer based on index size.
     *
     * @param fileData  File data to fetch value from. File Data will be advanced by number of bytes passed.
     * @param indexSize Size (in bytes) of the value to be fetched.
     * @return Value of next byte(s) within the data buffer cast to int.
     */
    private int GetIndex(ByteBuffer fileData, int indexSize) {
        return switch(indexSize) {
            case 1 -> fileData.get();
            case 2 -> fileData.getShort();
            case 4 -> fileData.getInt();
            default -> 0;
        };
    }

    /**
     * Retrieves a string from the provided data buffer at the start of the string table + offset.
     *
     * @param fileData     File data buffer to fetch data from .
     * @param stringOffset Location within the file's string table to begin reading.
     * @return String from stringOffset to next NULL terminator (\0).
     */
    private String GetString(ByteBuffer fileData, int stringOffset) {
        int position = fileData.position();
        int offset = GetIndex(fileData, stringOffset);

        String s = "";
        char c;
        do {
            c = (char) fileData.get(16 + offset);
            offset++;
            if(c != '\0') {
                s += c;
            }
        } while(c != '\0');

        fileData.position(position + stringOffset);
        return s;
    }

    /**
     * Load model texture from in-line asset if present, or from external file at model location in filesystem.
     *
     * @param model M3D model
     * @param textureName Targeted texture to load or fetch
     * @return ID of texture
     */
    private int LoadTexture(M3DJ_Model model, String textureName) {

        // Failsafe
        if (textureName == null || textureName.isBlank()) {
            return M3D_UNDEF;
        }

        // Check loaded textures
        for (int i = 0; i < model.textures.size(); i++) {
            if (textureName.equals(model.textures.get(i).name)) {
                return i;
            }
        }

        // Check in-line assets for texture
        for (int i = 0; i < model.assets.size(); i++) {
            if (textureName.equals(model.assets.get(i).name)) {
                M3DJ_Texture texture = new M3DJ_Texture();
                texture.name = model.assets.get(i).name;
                texture.textureData = model.assets.get(i).assetData;

                // Once the texture is loaded we can remove it from the assets
                // and move it to the textures; we replace the element with null
                // so that asset indices do not shift.
                model.assets.remove(i);
                model.assets.add(i, null);

                model.textures.add(texture);
                return model.textures.size() - 1;
            }
        }

        // Attempt to load from filesystem
        try {
            byte[] textureData = IO.LoadFileData(textureName);

            M3DJ_Texture texture = new M3DJ_Texture();
            texture.name = textureName;
            texture.textureData = ByteBuffer.wrap(textureData);
            model.textures.add(texture);
        }
        catch (IOException e) {
            logger.Out(Tracelog.LogType.LOG_ERROR, "Failed to load texture from filesystem: " + textureName);
            logger.Out(Tracelog.LogType.LOG_ERROR, "Models dependant on external files must be placed in the same directory.");
        }

        return M3D_UNDEF;
    }

    /**
     * Decompressed binary data using Z-LIB compression schema.
     *
     * @param compressedData Z-LIB compressed binary data.
     * @return Uncompressed/Inflated data buffer.
     */
    private ByteBuffer DecompressDataBuffer(ByteBuffer compressedData) {
        Inflater inflater = new Inflater();
        inflater.setInput(compressedData);

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        byte[] buffer = new byte[1024];

        try {
            while(inflater.getRemaining() > 0) {
                int decompressedSize = inflater.inflate(buffer);
                outputStream.write(buffer, 0, decompressedSize);
            }
        } catch(DataFormatException e) {
            throw new RuntimeException(e);
        } finally {
            inflater.end();
        }

        ByteBuffer result = ByteBuffer.allocateDirect(outputStream.size());
        result.put(outputStream.toByteArray());
        result.order(ByteOrder.LITTLE_ENDIAN);
        result.flip();

        return result;
    }

    /**
     * Write model specifics to evaluate how the parser is reading the provided model.
     *
     * @param model    Parsed model to write.
     * @param filePath Nullable. Writes output to filepath provided. If null, output is set to console.
     * @throws IOException If file fails to write to disk.
     */
    public void DumpModel(M3DJ_Model model, String filePath) throws IOException {
        StringBuilder output = new StringBuilder();
        // Header Bitfield
        output.append(
                        "model = {\n" +
                        "\t VC: " + model.header.VC_T.size + "\n" +
                        "\t VI: " + model.header.VI_T.size + "\n" +
                        "\t SI: " + model.header.SI_T.size + "\n" +
                        "\t CI: " + model.header.CI_T.size + "\n" +
                        "\t TI: " + model.header.TI_T.size + "\n" +
                        "\t BI: " + model.header.BI_T.size + "\n" +
                        "\t NB: " + model.header.NB_T.value + "\n" +
                        "\t SK: " + model.header.SK_T.size + "\n" +
                        "\t FC: " + model.header.FC_T.size + "\n" +
                        "\t HI: " + model.header.HI_T.size + "\n" +
                        "\t FI: " + model.header.FI_T.size + "\n" +
                        "\n"
        );

        // Header Metadata
        output.append(
                        "\tName: " + model.header.title + "\n" +
                        "\tLicense: " + model.header.licence + "\n" +
                        "\tAuthor: " + model.header.author + "\n" +
                        "\tDescription: " + model.header.description + "\n" +
                        "\tScale: " + model.header.scale + "\n" +
                        "\n"
        );

        // Model Information
        output.append(
                        "\tPreview available?: " + model.preview.hasPreview + "\n" +
                        "\tColor map size: " + model.colors.size() + "\n" +
                        "\tTexture map size: " + model.textureMap.size() + "\n" +
                        "\tNumber of textures: " + model.textures.size() + "\n" +
                        "\tNumber of bones: " + model.bones.size() + "\n" +
                        "\tNumber of vertices: " + model.vertices.size() + "\n" +
                        "\tNumber of skins: " + model.skins.size() + "\n" +
                        "\tNumber of materials: " + model.materials.size() + "\n" +
                        "\tNumber of faces: " + model.faces.size() + "\n" +
                        // "\tNumber of Voxel Types: " + model.voxels.size() + "\n" +
                        "\tVoxels not implemented...\n" +
                        // "\tNumber of Voxels: " + model.colors.size() + "\n" +
                        "\tVoxels not implemented...\n" +
                        // "\tNumber of Shapes: " + model.colors.size() + "\n" +
                        "\tShapes not implemented...\n" +
                        "\tNumber of Labels: " + model.labels.size() + "\n" +
                        "\tNumber of Actions: " + model.actions.size() + "\n" +
                        "\tNumber of assets: " + model.assets.size() + "\n" +
                        "\tNumber of Extra Parameters: " + model.extras.size() + "\n" +
                        "}\n\n"
        );

        //Assets
        if (!model.assets.isEmpty()) {
            output.append("model.assets = {\n");
            for(M3DJ_Asset asset : model.assets) {
                output.append("\t{ Name: " + asset.name + ", ");
                output.append("Bytes: " + asset.assetData.capacity() + "}, \n");
            }
            output.append("}\n");
        }

        // Color map
        if(!model.colors.isEmpty()) {
            output.append("model.colors = {\n");
            for(M3DJ_Color color : model.colors) {
                output.append(
                        "\t{ " +
                        "R: " + color.r + " " +
                        "G: " + color.g + " " +
                        "B: " + color.b + " " +
                        "A: " + color.a + " " +
                        " },\n"
                );
            }
            output.append("}\n");
        }

        // Texture Map
        if(!model.textureMap.isEmpty()) {
            output.append("model.textureMap = {\n");
            for(M3DJ_TextureCoordinate textureCoordinate : model.textureMap) {
                output.append(
                        "\t{ " +
                        "U: " + textureCoordinate.u + ", " +
                        "V: " + textureCoordinate.v +
                        " },\n"
                );
            }
            output.append("}\n");
        }

        // Vertices
        if(!model.vertices.isEmpty()) {
            output.append("model.vertices = {\n");
            for(M3DJ_Vertex vertex : model.vertices) {
                output.append(
                        "\t{ " +
                        "X: " + vertex.x + ", " +
                        "Y: " + vertex.y + ", " +
                        "Z: " + vertex.z + ", " +
                        "W: " + vertex.w + ", " +
                        "Skin Index: " + vertex.skinIndex + ", " +
                        "Color Index: " + vertex.colorIndex +
                        " }\n"
                );
            }
            output.append("}\n");
        }

        // Faces
        if(!model.faces.isEmpty()) {
            output.append("model.faces = {\n");
            for(M3DJ_Face face : model.faces) {
                output.append("\t{ " +
                        "Material ID: " + face.materialId + ", " +
                        "Parameter ID: " + face.paramId + ", "
                );
                output.append("Vertex = { " + face.vertices[0] + ", " + face.vertices[1] + ", " + face.vertices[2] + " }, ");
                output.append("Vertex Maximum = { " + face.vertMax[0] + ", " + face.vertMax[1] + ", " + face.vertMax[2] + " }, ");
                output.append("Vertex Normal = { " + face.normals[0] + ", " + face.normals[1] + ", " + face.normals[2] + " }, ");
                output.append("Vertex Texture Coordinate = { " + face.texCoords[0] + ", " + face.texCoords[1] + ", " + face.texCoords[2] + " }, ");
                output.append(" }\n");
            }
            output.append("}\n");
        }

        // Textures
        if (!model.textures.isEmpty()) {
            output.append("model.textures = {\n");
            for(M3DJ_Texture texture : model.textures) {
                output.append("\t{ " + texture.name + "},\n");
            }
            output.append("}\n");
        }

        // Bones
        if(!model.bones.isEmpty()) {
            output.append("model.bones = {\n");
            for(M3DJ_Bone bone : model.bones) {
                output.append("\t{ ");
                output.append("parent: " + bone.parentIndex + ", ");
                output.append("name: " + bone.name + ", ");
                output.append("position: " + bone.position + ", ");
                output.append("orientation: " + bone.orientation + "},");
            }
            output.append("}\n");
        }

        // Skins
        if(!model.skins.isEmpty()) {
            output.append("model.skins = {\n");
            for(M3DJ_Skin skin : model.skins) {
                output.append("\t{ ");
                output.append("Bone IDs = { " + skin.boneIds[0] + ", " + skin.boneIds[1] + ", " + skin.boneIds[2] + ", " + skin.boneIds[3] + " }, ");
                output.append("Weights = { " + skin.weights[0] + ", " + skin.weights[1] + ", " + skin.weights[2] + ", " + skin.weights[3] + " }, ");
                output.append("}\n");
            }
            output.append("}\n");
        }

        // Materials
        if(!model.materials.isEmpty()) {
            output.append("model.materials = {\n");
            int i = 0;
            for(M3DJ_Material material : model.materials) {
                output.append("\t" + i + " = { " + "\n");
                output.append("\t\tName: " + material.name + "\n");
                output.append("\t\tNumber of Properties: " + material.properties.size() + "\n");
                output.append("\t\tProperties = {\n");
                for(M3DJ_Property property : material.properties) {
                    output.append("\t\t\t{ ");
                    output.append("Type: " + property.id + ", ");
                    output.append("Key: " + property.key + ", ");
                    output.append(property.format + ".value: " + property.GetPropertyValue() + " }\n");
                }
                output.append("\t\t}\n");
                output.append("\t}\n");

                i++;
            }
            output.append("}\n");
        }

        // Labels
        if(!model.labels.isEmpty()) {
            output.append("model.labels = {\n");
            for(M3DJ_Label label : model.labels) {
                output.append(
                        "\t{" +
                        "name: " + label.name + ", " +
                        "language: " + label.language + ", " +
                        "text: " + label.text + ", " +
                        "colorId: " + label.colorId + ", " +
                        "vertexId: " + label.vertexId + ", " +
                        "}," +
                        "\n"
                );
            }
            output.append("}\n");
        }

        // Actions
        if(!model.actions.isEmpty()) {
            output.append("model.actions = {\n");
            for(M3DJ_Action action : model.actions) {
                output.append(
                        "\t{ " +
                        "name: " + action.name + ", " +
                        "duration: " + action.animationLength + ", " +
                        "frame count: " + action.frameCount + ", " +
                        "frames = {\n"
                );
                for (M3DJ_Frame frame : action.frames) {
                    output.append(
                            "\t\t{ " +
                            "timestamp: " + frame.timestamp + ", " +
                            "number of transforms: " + frame.transformsCount +  ", " +
                            "transforms = {\n"
                    );
                    for(M3DJ_Transform transform : frame.transforms) {
                        output.append(
                                "\t\t\t{ " +
                                "bone ID: " + transform.boneId +  ", " +
                                "position: " + transform.position +  ", " +
                                "orientation: " + transform.orientation +
                                " },\n"
                        );
                    }
                    output.append("\t\t},\n");
                }
                output.append(
                        "\t}\n"
                );
            }
            output.append("}\n");
        }

        // Extras
        if(!model.extras.isEmpty()) {
            output.append("model.extras = {\n");
            for(M3DJ_Extra extra : model.extras) {
                output.append("\t{ key: " + extra.key + ", ");
                output.append("data: ");
                while(extra.data.hasRemaining()) {
                    output.append(extra.data.get() + ", ");
                }
                output.append(" },\n");
            }
            output.append("}\n");
        }

        if(filePath == null) {
            System.out.println(output);
        }
        else {
            IO.WriteFileText(filePath + ".dump", output.toString());
        }
    }
}