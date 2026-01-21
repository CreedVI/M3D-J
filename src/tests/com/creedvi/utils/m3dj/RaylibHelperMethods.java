package com.creedvi.utils.m3dj;

import com.creedvi.utils.m3dj.model.M3DJ_Model;
import com.creedvi.utils.m3dj.model.chunks.M3DJ_Bone;
import com.creedvi.utils.m3dj.model.chunks.M3DJ_Property;
import com.raylib.java.Raylib;
import com.raylib.java.raymath.Raymath;
import com.raylib.java.structs.*;

import java.io.IOException;

import static com.creedvi.utils.m3dj.AnimationTesting.M3D_ANIMDELAY;
import static com.creedvi.utils.m3dj.M3DJ.M3D_UNDEF;
import static com.raylib.java.Config.SUPPORT_MESH_GENERATION;
import static com.raylib.java.models.rModels.MaterialMapIndex.*;
import static com.raylib.java.models.rModels.MaterialMapIndex.MATERIAL_MAP_ROUGHNESS;
import static com.raylib.java.raymath.Raymath.MatrixIdentity;
import static com.raylib.java.rlgl.RLGL.rlPixelFormat.*;
import static com.raylib.java.rlgl.RLGL.rlPixelFormat.RL_PIXELFORMAT_UNCOMPRESSED_GRAYSCALE;
import static com.raylib.java.structs.Color.WHITE;
import static com.raylib.java.utils.Tracelog.TRACELOG;
import static com.raylib.java.utils.Tracelog.TracelogType.LOG_INFO;
import static com.raylib.java.utils.Tracelog.TracelogType.LOG_WARNING;

public class RaylibHelperMethods {


    protected static Model M3DJ_LoadModel(String filePath, Raylib rlj) {
        M3DJ parser = new M3DJ();
        M3DJ_Model m3dj;

        try {
            m3dj = parser.LoadFile(filePath);
        } catch(IOException e) {
            throw new RuntimeException(e);
        }

        Model model = new Model();
        int i, j, k, l, n, mi = -2, vcolor;

        // no faces? this is probably just a material library
        if(m3dj.faces.isEmpty()) {
            return model;
        }

        if(!m3dj.materials.isEmpty()) {
            model.meshCount = model.materialCount = m3dj.materials.size();
            TRACELOG(LOG_INFO, "MODEL: model has " + model.materialCount + " material meshes");
        }
        else {
            model.meshCount = 1;
            model.materialCount = 0;
            TRACELOG(LOG_INFO, "MODEL: No materials, putting all meshes in a default material");
        }

        // We always need a default material, so we add +1
        model.materialCount++;

        // Faces must be in non-decreasing materialid order. Verify that quickly, sorting them otherwise
        // WARNING: Sorting is not needed, valid M3D model files should already be sorted
        // Just keeping the sorting function for reference (Check PR #3363 #3385)
        /*
        for (i = 1; i < m3d->numface; i++)
        {
            if (m3d->face[i-1].materialid <= m3d->face[i].materialid) continue;

            // face[i-1] > face[i].  slide face[i] lower
            m3df_t slider = m3d->face[i];
            j = i-1;

            do
            {   // face[j] > slider, face[j+1] is svailable vacant gap
                m3d->face[j+1] = m3d->face[j];
                j = j-1;
            }
            while (j >= 0 && m3d->face[j].materialid > slider.materialid);

            m3d->face[j+1] = slider;
        }
        */

        model.meshes = new Mesh[model.meshCount];
        for(int m = 0; m < model.meshes.length; m++) {
            model.meshes[m] = new Mesh();
        }

        model.meshMaterial = new int[model.meshCount];

        model.materials = new Material[model.materialCount + 1];
        for(int m = 0; m < model.materials.length; m++) {
            model.materials[m] = new Material();
        }

        // Map no material to index 0 with default shader, everything else materialId + 1
        model.materials[0] = rlj.models.LoadMaterialDefault();

        for(i = l = 0, k = -1; i < m3dj.faces.size(); i++, l++) {
            // Materials are grouped together
            if(mi != m3dj.faces.get(i).materialId) {
                // There should be only one material switch per material kind,
                // but be bulletproof for non-optimal model files
                if(k + 1 >= model.meshCount) {
                    model.meshCount++;

                    // Create a second buffer for mesh re-allocation
                    Mesh[] tempMeshes = new Mesh[model.meshCount];
                    System.arraycopy(model.meshes, 0, tempMeshes, 0, model.meshCount - 1);
                    model.meshes = tempMeshes;
                    for(int m = 0; m < model.meshes.length; m++) {
                        model.meshes[m] = new Mesh();
                    }

                    // Create a second buffer for material re-allocation
                    int[] tempMeshMaterial = new int[model.meshCount];
                    System.arraycopy(model.meshMaterial, 0, tempMeshMaterial, 0, model.meshCount - 1);
                    model.meshMaterial = tempMeshMaterial;
                }

                k++;
                mi = m3dj.faces.get(i).materialId;

                // Only allocate colors VertexBuffer if there's a color vertices in the model for this material batch
                // if all colors are fully transparent black for all vertices of this material, then we assume no vertices colors
                for(j = i, l = vcolor = 0; (j < m3dj.faces.size()) && (mi == m3dj.faces.get(j).materialId); j++, l++) {
                    if(
                        m3dj.vertices.get(m3dj.faces.get(j).vertices[0]).colorIndex != M3D_UNDEF ||
                        m3dj.vertices.get(m3dj.faces.get(j).vertices[1]).colorIndex != M3D_UNDEF ||
                        m3dj.vertices.get(m3dj.faces.get(j).vertices[2]).colorIndex != M3D_UNDEF
                    ) {
                        vcolor = 1;
                    }
                }

                model.meshes[k].vertexCount = l * 3;
                model.meshes[k].triangleCount = l;
                model.meshes[k].vertices = new float[model.meshes[k].vertexCount * 3];
                model.meshes[k].texcoords = new float[model.meshes[k].vertexCount * 2];
                model.meshes[k].normals = new float[model.meshes[k].vertexCount * 3];

                // If no map is provided, or we have colors defined, we allocate storage for vertices colors
                // M3D specs only consider vertices colors if no material is provided, however raylib uses both and mixes the colors
                if((mi == M3D_UNDEF) || vcolor != 0) {
                    model.meshes[k].colors = new byte[model.meshes[k].vertexCount * 4];
                }

                // If no map is provided, and we allocated vertices colors, set them to white
                if((mi == M3D_UNDEF) && (model.meshes[k].colors != null)) {
                    for(int c = 0; c < model.meshes[k].vertexCount * 4; c++) {
                        model.meshes[k].colors[c] = (byte) 255;
                    }
                }

                if(!m3dj.bones.isEmpty() && !m3dj.skins.isEmpty()) {
                    model.meshes[k].boneIds = new byte[model.meshes[k].vertexCount * 4];
                    model.meshes[k].boneWeights = new float[model.meshes[k].vertexCount * 4];
                    model.meshes[k].animVertices = new float[model.meshes[k].vertexCount * 3];
                    model.meshes[k].animNormals = new float[model.meshes[k].vertexCount * 3];
                }

                model.meshMaterial[k] = mi + 1;
                l = 0;
            }

            // Process meshes per material, add triangles
            model.meshes[k].vertices[l * 9 + 0] = (float) (m3dj.vertices.get(m3dj.faces.get(i).vertices[0]).x * m3dj.header.scale);
            model.meshes[k].vertices[l * 9 + 1] = (float) (m3dj.vertices.get(m3dj.faces.get(i).vertices[0]).y * m3dj.header.scale);
            model.meshes[k].vertices[l * 9 + 2] = (float) (m3dj.vertices.get(m3dj.faces.get(i).vertices[0]).z * m3dj.header.scale);
            model.meshes[k].vertices[l * 9 + 3] = (float) (m3dj.vertices.get(m3dj.faces.get(i).vertices[1]).x * m3dj.header.scale);
            model.meshes[k].vertices[l * 9 + 4] = (float) (m3dj.vertices.get(m3dj.faces.get(i).vertices[1]).y * m3dj.header.scale);
            model.meshes[k].vertices[l * 9 + 5] = (float) (m3dj.vertices.get(m3dj.faces.get(i).vertices[1]).z * m3dj.header.scale);
            model.meshes[k].vertices[l * 9 + 6] = (float) (m3dj.vertices.get(m3dj.faces.get(i).vertices[2]).x * m3dj.header.scale);
            model.meshes[k].vertices[l * 9 + 7] = (float) (m3dj.vertices.get(m3dj.faces.get(i).vertices[2]).y * m3dj.header.scale);
            model.meshes[k].vertices[l * 9 + 8] = (float) (m3dj.vertices.get(m3dj.faces.get(i).vertices[2]).z * m3dj.header.scale);

            // Without vertices color (full transparency), we use the default color
            if(model.meshes[k].colors != null && !m3dj.colors.isEmpty()) {
                if(m3dj.colors.get(m3dj.vertices.get(m3dj.faces.get(i).vertices[0]).colorIndex).a > 0) {
                    model.meshes[k].colors[l * 12 + 0] = (byte) m3dj.colors.get(m3dj.vertices.get(m3dj.faces.get(i).vertices[0]).colorIndex).a;
                    model.meshes[k].colors[l * 12 + 1] = (byte) m3dj.colors.get(m3dj.vertices.get(m3dj.faces.get(i).vertices[0]).colorIndex).b;
                    model.meshes[k].colors[l * 12 + 2] = (byte) m3dj.colors.get(m3dj.vertices.get(m3dj.faces.get(i).vertices[0]).colorIndex).g;
                    model.meshes[k].colors[l * 12 + 3] = (byte) m3dj.colors.get(m3dj.vertices.get(m3dj.faces.get(i).vertices[0]).colorIndex).r;
                }
                if(m3dj.colors.get(m3dj.vertices.get(m3dj.faces.get(i).vertices[1]).colorIndex).a > 0) {
                    model.meshes[k].colors[l * 12 + 4] = (byte) m3dj.colors.get(m3dj.vertices.get(m3dj.faces.get(i).vertices[1]).colorIndex).a;
                    model.meshes[k].colors[l * 12 + 5] = (byte) m3dj.colors.get(m3dj.vertices.get(m3dj.faces.get(i).vertices[1]).colorIndex).b;
                    model.meshes[k].colors[l * 12 + 6] = (byte) m3dj.colors.get(m3dj.vertices.get(m3dj.faces.get(i).vertices[1]).colorIndex).g;
                    model.meshes[k].colors[l * 12 + 7] = (byte) m3dj.colors.get(m3dj.vertices.get(m3dj.faces.get(i).vertices[1]).colorIndex).r;
                }
                if(m3dj.colors.get(m3dj.vertices.get(m3dj.faces.get(i).vertices[2]).colorIndex).a > 0) {
                    model.meshes[k].colors[l * 12 + 8] = (byte) m3dj.colors.get(m3dj.vertices.get(m3dj.faces.get(i).vertices[2]).colorIndex).a;
                    model.meshes[k].colors[l * 12 + 9] = (byte) m3dj.colors.get(m3dj.vertices.get(m3dj.faces.get(i).vertices[2]).colorIndex).b;
                    model.meshes[k].colors[l * 12 + 10] = (byte) m3dj.colors.get(m3dj.vertices.get(m3dj.faces.get(i).vertices[2]).colorIndex).g;
                    model.meshes[k].colors[l * 12 + 11] = (byte) m3dj.colors.get(m3dj.vertices.get(m3dj.faces.get(i).vertices[2]).colorIndex).r;
                }
            }

            if(m3dj.faces.get(i).texCoords[0] != M3D_UNDEF) {
                model.meshes[k].texcoords[l * 6 + 0] = (float) m3dj.textureMap.get(m3dj.faces.get(i).texCoords[0]).u;
                model.meshes[k].texcoords[l * 6 + 1] = (float) (1.0f - m3dj.textureMap.get(m3dj.faces.get(i).texCoords[0]).v);
                model.meshes[k].texcoords[l * 6 + 2] = (float) m3dj.textureMap.get(m3dj.faces.get(i).texCoords[1]).u;
                model.meshes[k].texcoords[l * 6 + 3] = (float) (1.0f - m3dj.textureMap.get(m3dj.faces.get(i).texCoords[1]).v);
                model.meshes[k].texcoords[l * 6 + 4] = (float) m3dj.textureMap.get(m3dj.faces.get(i).texCoords[2]).u;
                model.meshes[k].texcoords[l * 6 + 5] = (float) (1.0f - m3dj.textureMap.get(m3dj.faces.get(i).texCoords[2]).v);
            }

            if(m3dj.faces.get(i).normals[0] != M3D_UNDEF) {
                model.meshes[k].normals[l * 9 + 0] = (float) m3dj.vertices.get(m3dj.faces.get(i).normals[0]).x;
                model.meshes[k].normals[l * 9 + 1] = (float) m3dj.vertices.get(m3dj.faces.get(i).normals[0]).y;
                model.meshes[k].normals[l * 9 + 2] = (float) m3dj.vertices.get(m3dj.faces.get(i).normals[0]).z;
                model.meshes[k].normals[l * 9 + 3] = (float) m3dj.vertices.get(m3dj.faces.get(i).normals[1]).x;
                model.meshes[k].normals[l * 9 + 4] = (float) m3dj.vertices.get(m3dj.faces.get(i).normals[1]).y;
                model.meshes[k].normals[l * 9 + 5] = (float) m3dj.vertices.get(m3dj.faces.get(i).normals[1]).z;
                model.meshes[k].normals[l * 9 + 6] = (float) m3dj.vertices.get(m3dj.faces.get(i).normals[2]).x;
                model.meshes[k].normals[l * 9 + 7] = (float) m3dj.vertices.get(m3dj.faces.get(i).normals[2]).y;
                model.meshes[k].normals[l * 9 + 8] = (float) m3dj.vertices.get(m3dj.faces.get(i).normals[2]).z;
            }

            // Add skin (vertices / bone weight pairs)
            if(!m3dj.bones.isEmpty() && !m3dj.skins.isEmpty()) {
                for(n = 0; n < 3; n++) {
                    int skinId = m3dj.vertices.get(m3dj.faces.get(i).vertices[n]).skinIndex;

                    // Check if there is a skin for this mesh, should be, just failsafe
                    if((skinId != M3D_UNDEF) && (skinId < m3dj.skins.size())) {
                        for(j = 0; j < 4; j++) {
                            model.meshes[k].boneIds[l * 12 + n * 4 + j] = (byte) m3dj.skins.get(skinId).boneIds[j];
                            model.meshes[k].boneWeights[l * 12 + n * 4 + j] = m3dj.skins.get(skinId).weights[j];
                        }
                    }
                    else {
                        // raylib does not handle boneless meshes with skeletal animations, so
                        // we put all vertices without a bone into a special "no bone" bone
                        model.meshes[k].boneIds[l * 12 + n * 4] = (byte) m3dj.bones.size();
                        model.meshes[k].boneWeights[l * 12 + n * 4] = 1.0f;
                    }
                }
            }
        }

        // Load materials
        for (i = 0; i < m3dj.materials.size(); i++) {
            model.materials[i + 1] = rlj.models.LoadMaterialDefault();

            for (j = 0; j < m3dj.materials.get(i).properties.size(); j++) {
                M3DJ_Property property = m3dj.materials.get(i).properties.get(j);

                if (property.key.equals("m3dp_Kd")) {
                    model.materials[i + 1].maps[MATERIAL_MAP_DIFFUSE].color = rlj.textures.GetColor((int) property.GetPropertyValue());
                    model.materials[i + 1].maps[MATERIAL_MAP_DIFFUSE].value = 0.0f;
                }
                else if (property.key.equals("m3dp_Ks")) {
                    model.materials[i + 1].maps[MATERIAL_MAP_SPECULAR].color = rlj.textures.GetColor((int) property.GetPropertyValue());
                }
                else if (property.key.equals("m3dp_Ns")) {
                    model.materials[i + 1].maps[MATERIAL_MAP_SPECULAR].value = (int) property.GetPropertyValue();
                }
                else if (property.key.equals("m3dp_Ke")) {
                    model.materials[i + 1].maps[MATERIAL_MAP_EMISSION].color = rlj.textures.GetColor((int) property.GetPropertyValue());
                    model.materials[i + 1].maps[MATERIAL_MAP_EMISSION].value = 0.0f;
                }
                else if (property.key.equals("m3dp_Pm")) {
                    model.materials[i + 1].maps[MATERIAL_MAP_METALNESS].value = (float) property.GetPropertyValue();
                }
                else if (property.key.equals("m3dp_Pr")) {
                    model.materials[i + 1].maps[MATERIAL_MAP_ROUGHNESS].value = (float) property.GetPropertyValue();
                }
                else if (property.key.equals("m3dp_Ps")) {
                    model.materials[i + 1].maps[MATERIAL_MAP_NORMAL].color = WHITE;
                    model.materials[i + 1].maps[MATERIAL_MAP_NORMAL].value = (float) property.GetPropertyValue();
                }
                else if (property.id >= 128) {
                    Image image = new Image();
                    image.data = m3dj.textures.get((int) property.GetPropertyValue()).textureData;
                    image.width = m3dj.textures.get((int) property.GetPropertyValue()).width;
                    image.height = m3dj.textures.get((int) property.GetPropertyValue()).height;
                    image.mipmaps = 1;
                    image.format =
                        (m3dj.textures.get((int) property.GetPropertyValue()).format == 4) ? RL_PIXELFORMAT_UNCOMPRESSED_R8G8B8A8 :
                        ((m3dj.textures.get((int) property.GetPropertyValue()).format == 3) ? RL_PIXELFORMAT_UNCOMPRESSED_R8G8B8 :
                        ((m3dj.textures.get((int) property.GetPropertyValue()).format == 2) ? RL_PIXELFORMAT_UNCOMPRESSED_GRAY_ALPHA : RL_PIXELFORMAT_UNCOMPRESSED_GRAYSCALE));

                    if (property.key.equals("m3dp_map_Kd")) {
                        model.materials[i + 1].maps[MATERIAL_MAP_DIFFUSE].texture = rlj.textures.LoadTextureFromImage(image);
                    }
                    else if (property.key.equals("m3dp_map_Ks")) {
                        model.materials[i + 1].maps[MATERIAL_MAP_SPECULAR].texture = rlj.textures.LoadTextureFromImage(image);
                    }
                    else if (property.key.equals("m3dp_map_Ke")) {
                        model.materials[i + 1].maps[MATERIAL_MAP_EMISSION].texture = rlj.textures.LoadTextureFromImage(image);
                    }
                    else if (property.key.equals("m3dp_map_Km")) {
                        model.materials[i + 1].maps[MATERIAL_MAP_NORMAL].texture = rlj.textures.LoadTextureFromImage(image);
                    }
                    else if (property.key.equals("m3dp_map_Ka")) {
                        model.materials[i + 1].maps[MATERIAL_MAP_OCCLUSION].texture = rlj.textures.LoadTextureFromImage(image);
                    }
                    else if (property.key.equals("m3dp_map_Pm")) {
                        model.materials[i + 1].maps[MATERIAL_MAP_ROUGHNESS].texture = rlj.textures.LoadTextureFromImage(image);
                    }
                }
            }
        }

        // Load bones
        if (!m3dj.bones.isEmpty()) {
            model.boneCount = m3dj.bones.size() + 1;
            model.bones = new BoneInfo[model.boneCount];
            model.bindPose = new Transform[model.boneCount];

            for (i = 0; i < m3dj.bones.size(); i++) {
                model.bones[i] = new BoneInfo(m3dj.bones.get(i).name, m3dj.bones.get(i).parentIndex);
                model.bindPose[i] = new Transform();
                model.bindPose[i].translation.x = (float) (m3dj.vertices.get((int) m3dj.bones.get(i).position).x*m3dj.header.scale);
                model.bindPose[i].translation.y = (float) (m3dj.vertices.get((int) m3dj.bones.get(i).position).y*m3dj.header.scale);
                model.bindPose[i].translation.z = (float) (m3dj.vertices.get((int) m3dj.bones.get(i).position).z*m3dj.header.scale);
                model.bindPose[i].rotation.x = (float) m3dj.vertices.get((int) m3dj.bones.get(i).orientation).x;
                model.bindPose[i].rotation.y = (float) m3dj.vertices.get((int) m3dj.bones.get(i).orientation).y;
                model.bindPose[i].rotation.z = (float) m3dj.vertices.get((int) m3dj.bones.get(i).orientation).z;
                model.bindPose[i].rotation.w = (float) m3dj.vertices.get((int) m3dj.bones.get(i).orientation).w;

                // TODO: If the orientation quaternion is not normalized, then that's encoding scaling
                model.bindPose[i].rotation = Raymath.QuaternionNormalize(model.bindPose[i].rotation);
                model.bindPose[i].scale.x = model.bindPose[i].scale.y = model.bindPose[i].scale.z = 1.0f;

                // Child bones are stored in parent bone relative space, convert that into model space
                if (model.bones[i].parent >= 0) {
                    model.bindPose[i].rotation = Raymath.QuaternionMultiply(model.bindPose[model.bones[i].parent].rotation, model.bindPose[i].rotation);
                    model.bindPose[i].translation = Raymath.Vector3RotateByQuaternion(model.bindPose[i].translation, model.bindPose[model.bones[i].parent].rotation);
                    model.bindPose[i].translation = Raymath.Vector3Add(model.bindPose[i].translation, model.bindPose[model.bones[i].parent].translation);
                    model.bindPose[i].scale = Raymath.Vector3Multiply(model.bindPose[i].scale, model.bindPose[model.bones[i].parent].scale);
                }
            }

            // Add a special "no bone" bone
            model.bones[i] = new BoneInfo("NO BONE", -1);
            model.bindPose[i] = new Transform();
            model.bindPose[i].translation.x = 0.0f;
            model.bindPose[i].translation.y = 0.0f;
            model.bindPose[i].translation.z = 0.0f;
            model.bindPose[i].rotation.x = 0.0f;
            model.bindPose[i].rotation.y = 0.0f;
            model.bindPose[i].rotation.z = 0.0f;
            model.bindPose[i].rotation.w = 1.0f;
            model.bindPose[i].scale.x = model.bindPose[i].scale.y = model.bindPose[i].scale.z = 1.0f;
        }

        // Load bone-pose default mesh into animations[a] vertices. These will be updated when UpdateModelAnimation gets
        // called, but not before, however DrawMesh uses these if they exist (so not good if they are left empty)
        if (!m3dj.bones.isEmpty() && !m3dj.skins.isEmpty()) {
            for (i = 0; i < model.meshCount; i++) {
                System.arraycopy(model.meshes[i].vertices, 0, model.meshes[i].animVertices, 0, model.meshes[i].vertexCount*3);
                System.arraycopy(model.meshes[i].normals, 0, model.meshes[i].animNormals, 0, model.meshes[i].vertexCount*3);
            }
        }

        // Make sure model transform is set to identity matrix!
        model.transform = MatrixIdentity();

        if(model.meshCount == 0) {
            model.meshCount = 1;
            model.meshes = new Mesh[model.meshCount];
            if(SUPPORT_MESH_GENERATION) {
                model.meshes[0] = rlj.models.GenMeshCube(1.0f, 1.0f, 1.0f);
            }
        }
        else {
            // Upload vertex data to GPU (static mesh)
            for(int z = 0; z < model.meshCount; z++) {
                rlj.models.UploadMesh(model.meshes[z], false);
            }
        }

        if(model.materialCount == 0) {

            model.materialCount = 1;
            model.materials = new Material[model.materialCount];
            model.materials[0] = rlj.models.LoadMaterialDefault();

            if(model.meshMaterial == null) {
                model.meshMaterial = new int[model.meshCount];
            }
        }

        return model;
    }

    protected static ModelAnimation[] M3DJ_LoadModelAnimations(String filePath, Raylib rlj) {
        M3DJ parser = new M3DJ();
        M3DJ_Model m3dj;
        int i, j;

        try {
            m3dj = parser.LoadFile(filePath);
        } catch(IOException e) {
            throw new RuntimeException(e);
        }

        if (m3dj == null) {
            TRACELOG(LOG_WARNING, "MODEL: [" + filePath + "] Failed to load M3D.");
            return null;
        }
        else {
            TRACELOG(LOG_INFO, "MODEL: [" + filePath + "] Loaded successfully: " +
                    m3dj.actions.size() + " animations[a](s), " + m3dj.bones.size() + " bones, " + m3dj.skins.size() + " skins.");
        }

        if (m3dj.actions.isEmpty() || m3dj.bones.isEmpty() || m3dj.skins.isEmpty()) {
            return null;
        }

        ModelAnimation[] animations = new ModelAnimation[m3dj.actions.size()];
        
        for (int a = 0; a < m3dj.actions.size(); a++) {
            animations[a] = new ModelAnimation();

            animations[a].frameCount = m3dj.actions.get(a).animationLength / M3D_ANIMDELAY;
            animations[a].boneCount = m3dj.bones.size() + 1;
            animations[a].bones = new BoneInfo[animations[a].boneCount];
            animations[a].framePoses = new Transform[animations[a].frameCount][];

            TRACELOG(LOG_INFO, "MODEL: [" + filePath + "] animations[a] #" + a + ": " + m3dj.actions.get(a).animationLength +
                    " msec, " + animations[a].frameCount + " frames.");

            for (i = 0; i < m3dj.bones.size(); i++) {
                animations[a].bones[i] = new BoneInfo(m3dj.bones.get(i).name, m3dj.bones.get(i).parentIndex);
            }

            // A special, never transformed "no bone" bone, used for boneless vertices
            animations[a].bones[i] = new BoneInfo("NO BONE", -1);

            // M3D stores frames at arbitrary intervals with sparse skeletons. We need full skeletons at
            // regular intervals, so let the M3D SDK do the heavy lifting and calculate interpolated bones
            for (i = 0; i < animations[a].frameCount; i++) {
                animations[a].framePoses[i] = new Transform[m3dj.bones.size() + 1];

                M3DJ_Bone[] pose = parser.Pose(m3dj, a, i * M3D_ANIMDELAY);

                if (pose != null) {
                    for (j = 0; j < m3dj.bones.size(); j++) {
                        animations[a].framePoses[i][j] = new Transform();
                        animations[a].framePoses[i][j].translation.x = (float) (m3dj.vertices.get(pose[j].position).x * m3dj.header.scale);
                        animations[a].framePoses[i][j].translation.y = (float) (m3dj.vertices.get(pose[j].position).y * m3dj.header.scale);
                        animations[a].framePoses[i][j].translation.z = (float) (m3dj.vertices.get(pose[j].position).z * m3dj.header.scale);
                        animations[a].framePoses[i][j].rotation.x = (float) m3dj.vertices.get(pose[j].orientation).x;
                        animations[a].framePoses[i][j].rotation.y = (float) m3dj.vertices.get(pose[j].orientation).y;
                        animations[a].framePoses[i][j].rotation.z = (float) m3dj.vertices.get(pose[j].orientation).z;
                        animations[a].framePoses[i][j].rotation.w = (float) m3dj.vertices.get(pose[j].orientation).w;
                        animations[a].framePoses[i][j].rotation = Raymath.QuaternionNormalize(animations[a].framePoses[i][j].rotation);
                        animations[a].framePoses[i][j].scale.x = 1.0f;
                        animations[a].framePoses[i][j].scale.y = 1.0f;
                        animations[a].framePoses[i][j].scale.z = 1.0f;

                        // Child bones are stored in parent bone relative space, convert that into model space
                        if (animations[a].bones[j].parent >= 0) {
                            animations[a].framePoses[i][j].rotation = Raymath.QuaternionMultiply(animations[a].framePoses[i][animations[a].bones[j].parent].rotation, animations[a].framePoses[i][j].rotation);
                            animations[a].framePoses[i][j].translation = Raymath.Vector3RotateByQuaternion(animations[a].framePoses[i][j].translation, animations[a].framePoses[i][animations[a].bones[j].parent].rotation);
                            animations[a].framePoses[i][j].translation = Raymath.Vector3Add(animations[a].framePoses[i][j].translation, animations[a].framePoses[i][animations[a].bones[j].parent].translation);
                            animations[a].framePoses[i][j].scale = Raymath.Vector3Multiply(animations[a].framePoses[i][j].scale, animations[a].framePoses[i][animations[a].bones[j].parent].scale);
                        }
                    }

                    // Default transform for the "no bone" bone
                    animations[a].framePoses[i][j] = new Transform();
                    animations[a].framePoses[i][j].translation.x = 0.0f;
                    animations[a].framePoses[i][j].translation.y = 0.0f;
                    animations[a].framePoses[i][j].translation.z = 0.0f;
                    animations[a].framePoses[i][j].rotation.x = 0.0f;
                    animations[a].framePoses[i][j].rotation.y = 0.0f;
                    animations[a].framePoses[i][j].rotation.z = 0.0f;
                    animations[a].framePoses[i][j].rotation.w = 1.0f;
                    animations[a].framePoses[i][j].scale.x = 1.0f;
                    animations[a].framePoses[i][j].scale.y = 1.0f;
                    animations[a].framePoses[i][j].scale.z = 1.0f;
                }
            }
        }

        return animations;
    }


}
