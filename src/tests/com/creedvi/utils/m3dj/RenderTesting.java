package com.creedvi.utils.m3dj;

import com.creedvi.utils.m3dj.model.M3DJ_Model;
import com.creedvi.utils.m3dj.model.chunks.M3DJ_Property;
import com.raylib.java.Raylib;
import com.raylib.java.core.rcamera.Camera3D;
import com.raylib.java.models.*;
import com.raylib.java.raymath.Vector3;

import java.io.IOException;

import static com.creedvi.utils.m3dj.M3DJ.M3D_UNDEF;
import static com.raylib.java.core.Color.*;
import static com.raylib.java.core.input.Mouse.MouseButton.MOUSE_BUTTON_LEFT;
import static com.raylib.java.core.rcamera.Camera3D.CameraProjection.CAMERA_PERSPECTIVE;
import static com.raylib.java.utils.Tracelog.Tracelog;
import static com.raylib.java.utils.Tracelog.TracelogType.LOG_INFO;

public class RenderTesting {

    static Raylib rlj;

    public static void main(String[] args) {
        // Initialization
        //--------------------------------------------------------------------------------------
        int screenWidth = 800;
        int screenHeight = 450;

        rlj = new Raylib(screenWidth, screenHeight, "raylib-j [models] example - models loading, M3D");

        // Define the camera to look into our 3d world
        Camera3D camera = new Camera3D();
        camera.position = new Vector3(0, 8, 16); // Camera position
        camera.target = new Vector3(0.0f, 0, 0.0f);     // Camera looking at point
        camera.up = new Vector3(0.0f, 1.0f, 0.0f);          // Camera up vector (rotation towards target)
        camera.fovy = 45.0f;                                         // Camera field-of-view Y
        camera.projection = CAMERA_PERSPECTIVE;                      // Camera mode type

        M3DJ_Model m3dj;

        try {
             m3dj = M3DJ.M3DJ_Load("assets/suzanne.m3d");
        }
        catch (IOException e) {
            throw new RuntimeException(e);
        }

        Model model = ConvertToRaylib(m3dj);                 // Load model

        Vector3 position = new Vector3(0,0,0);                    // Set model position

        BoundingBox bounds = rlj.models.GetMeshBoundingBox(model.meshes[0]);   // Set model bounds

        // NOTE: bounds are calculated from the original size of the model,
        // if model is scaled on drawing, bounds must be also scaled

        //camera.SetCameraMode(CAMERA_FREE);     // Set a free camera mode

        boolean selected = false;          // Selected object flag

        rlj.core.SetTargetFPS(60);               // Set our game to run at 60 frames-per-second
        //--------------------------------------------------------------------------------------

        // Main game loop
        while (!rlj.core.WindowShouldClose()) {   // Detect window close button or ESC key
            // Update
            //----------------------------------------------------------------------------------
            camera.UpdateCamera();

            // Select model on mouse click
            if (rlj.core.IsMouseButtonPressed(MOUSE_BUTTON_LEFT)) {
                // Check collision between ray and box
                if (rlj.models.GetRayCollisionBox(rlj.core.GetMouseRay(rlj.core.GetMousePosition(), camera), bounds).hit) {
                    selected = !selected;
                }
                else {
                    selected = false;
                }
            }
            //----------------------------------------------------------------------------------

            // Draw
            //----------------------------------------------------------------------------------
            rlj.core.BeginDrawing();

            rlj.core.ClearBackground(RAYWHITE);

            rlj.core.BeginMode3D(camera);

            rlj.models.DrawModel(model, position, 1.0f, WHITE);        // Draw 3d model with texture

            rlj.models.DrawGrid(20, 10.0f);         // Draw a grid

            if (selected) {
                rlj.models.DrawBoundingBox(bounds, GREEN);   // Draw selection box
            }

            rlj.core.EndMode3D();

            if (selected) {
                rlj.text.DrawText("MODEL SELECTED", rlj.core.GetScreenWidth() - 110, 10, 10, GREEN);
            }

            rlj.text.DrawFPS(10, 10);

            rlj.core.EndDrawing();
            //----------------------------------------------------------------------------------
        }
    }

    private static Model ConvertToRaylib(M3DJ_Model m3dj) {
        Model model = new Model();
        int i, j, k, l, n, mi = -2, vcolor = 0;
        M3DJ_Property property; 

        // no faces? this is probably just a material library
        if (m3dj.faces.isEmpty()) {
            return model;
        }

        if (!m3dj.materials.isEmpty()) {
            model.meshCount = model.materialCount = m3dj.materials.size();
            Tracelog(LOG_INFO, "MODEL: model has " + model.materialCount + " material meshes");
        }
        else {
            model.meshCount = 1; model.materialCount = 0;
            Tracelog(LOG_INFO, "MODEL: No materials, putting all meshes in a default material");
        }

        // We always need a default material, so we add +1
        model.materialCount++;
        
        model.meshes = new Mesh[model.meshCount];
        for (int m = 0; m < model.meshes.length; m++) {
            model.meshes[m] = new Mesh();
        }

        model.meshMaterial = new int[model.meshCount];

        model.materials = new Material[model.materialCount + 1];
        for (int m = 0; m < model.materials.length; m++) {
            model.materials[m] = new Material();
        }

        // Map no material to index 0 with default shader, everything else materialId + 1
        model.materials[0] = rlj.models.LoadMaterialDefault();

        for (i = l = 0, k = -1; i < m3dj.faces.size(); i++, l++) {
            // Materials are grouped together
            if (mi != m3dj.faces.get(i).materialId) {
                // There should be only one material switch per material kind,
                // but be bulletproof for non-optimal model files
                if (k + 1 >= model.meshCount) {
                    model.meshCount++;

                    // Create a second buffer for mesh re-allocation
                    Mesh[] tempMeshes = new Mesh[model.meshCount];
                    System.arraycopy(model.meshes, 0, tempMeshes, 0, model.meshCount - 1);
                    model.meshes = tempMeshes;
                    for (int m = 0; m < model.meshes.length; m++) {
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
                for (j = i, l = vcolor = 0; (j < m3dj.faces.size()) && (mi == m3dj.faces.get(j).materialId); j++, l++) {
                    if (
                            m3dj.vertices.get(m3dj.faces.get(j).vertices[0]).colorIndex == -1 ||
                            m3dj.vertices.get(m3dj.faces.get(j).vertices[1]).colorIndex == -1 ||
                            m3dj.vertices.get(m3dj.faces.get(j).vertices[2]).colorIndex == -1
                    ) {
                        vcolor = 1;
                    }
                }

                model.meshes[k].vertexCount = l*3;
                model.meshes[k].triangleCount = l;
                model.meshes[k].vertices = new float[model.meshes[k].vertexCount*3];
                model.meshes[k].texcoords = new float[model.meshes[k].vertexCount*2];
                model.meshes[k].normals = new float[model.meshes[k].vertexCount*3];

                // If no map is provided, or we have colors defined, we allocate storage for vertices colors
                // M3D specs only consider vertices colors if no material is provided, however raylib uses both and mixes the colors
                if ((mi == M3D_UNDEF) || vcolor != 0) {
                    model.meshes[k].colors = new byte[model.meshes[k].vertexCount * 4];
                }

                // If no map is provided and we allocated vertices colors, set them to white
                if ((mi == M3D_UNDEF) && (model.meshes[k].colors != null)) {
                    for (int c = 0; c < model.meshes[k].vertexCount*4; c++) {
                        model.meshes[k].colors[c] = (byte) 255;
                    }
                }

                if (!m3dj.bones.isEmpty() && !m3dj.skins.isEmpty()) {
                    model.meshes[k].boneIds = new byte[model.meshes[k].vertexCount*4];
                    model.meshes[k].boneWeights = new float[model.meshes[k].vertexCount*4];
                    model.meshes[k].animVertices = new float[model.meshes[k].vertexCount*3];
                    model.meshes[k].animNormals = new float[model.meshes[k].vertexCount*3];
                }

                model.meshMaterial[k] = mi + 1;
                l = 0;
            }

            // Process meshes per material, add triangles
            model.meshes[k].vertices[l*9 + 0] = (float) (m3dj.vertices.get(m3dj.faces.get(i).vertices[0]).x*m3dj.header.scale);
            model.meshes[k].vertices[l*9 + 1] = (float) (m3dj.vertices.get(m3dj.faces.get(i).vertices[0]).y*m3dj.header.scale);
            model.meshes[k].vertices[l*9 + 2] = (float) (m3dj.vertices.get(m3dj.faces.get(i).vertices[0]).z*m3dj.header.scale);
            model.meshes[k].vertices[l*9 + 3] = (float) (m3dj.vertices.get(m3dj.faces.get(i).vertices[1]).x*m3dj.header.scale);
            model.meshes[k].vertices[l*9 + 4] = (float) (m3dj.vertices.get(m3dj.faces.get(i).vertices[1]).y*m3dj.header.scale);
            model.meshes[k].vertices[l*9 + 5] = (float) (m3dj.vertices.get(m3dj.faces.get(i).vertices[1]).z*m3dj.header.scale);
            model.meshes[k].vertices[l*9 + 6] = (float) (m3dj.vertices.get(m3dj.faces.get(i).vertices[2]).x*m3dj.header.scale);
            model.meshes[k].vertices[l*9 + 7] = (float) (m3dj.vertices.get(m3dj.faces.get(i).vertices[2]).y*m3dj.header.scale);
            model.meshes[k].vertices[l*9 + 8] = (float) (m3dj.vertices.get(m3dj.faces.get(i).vertices[2]).z*m3dj.header.scale);

            // Without vertices color (full transparency), we use the default color
            if (model.meshes[k].colors != null) {
                if ((m3dj.vertices.get(m3dj.faces.get(i).vertices[0]).colorIndex & 0xff000000) != 0) {
                    model.meshes[k].colors[l * 12 + 0] = (byte) m3dj.colors.get(m3dj.vertices.get(m3dj.faces.get(i).vertices[0]).colorIndex).r;
                    model.meshes[k].colors[l * 12 + 1] = (byte) m3dj.colors.get(m3dj.vertices.get(m3dj.faces.get(i).vertices[0]).colorIndex).g;
                    model.meshes[k].colors[l * 12 + 2] = (byte) m3dj.colors.get(m3dj.vertices.get(m3dj.faces.get(i).vertices[0]).colorIndex).b;
                    model.meshes[k].colors[l * 12 + 3] = (byte) m3dj.colors.get(m3dj.vertices.get(m3dj.faces.get(i).vertices[0]).colorIndex).a;
                }
                if ((m3dj.vertices.get(m3dj.faces.get(i).vertices[1]).colorIndex & 0xff000000) != 0) {
                    model.meshes[k].colors[l * 12 + 0] = (byte) m3dj.colors.get(m3dj.vertices.get(m3dj.faces.get(i).vertices[1]).colorIndex).r;
                    model.meshes[k].colors[l * 12 + 1] = (byte) m3dj.colors.get(m3dj.vertices.get(m3dj.faces.get(i).vertices[1]).colorIndex).g;
                    model.meshes[k].colors[l * 12 + 2] = (byte) m3dj.colors.get(m3dj.vertices.get(m3dj.faces.get(i).vertices[1]).colorIndex).b;
                    model.meshes[k].colors[l * 12 + 3] = (byte) m3dj.colors.get(m3dj.vertices.get(m3dj.faces.get(i).vertices[1]).colorIndex).a;
                }
                if ((m3dj.vertices.get(m3dj.faces.get(i).vertices[2]).colorIndex & 0xff000000) != 0) {
                    model.meshes[k].colors[l * 12 + 0] = (byte) m3dj.colors.get(m3dj.vertices.get(m3dj.faces.get(i).vertices[2]).colorIndex).r;
                    model.meshes[k].colors[l * 12 + 1] = (byte) m3dj.colors.get(m3dj.vertices.get(m3dj.faces.get(i).vertices[2]).colorIndex).g;
                    model.meshes[k].colors[l * 12 + 2] = (byte) m3dj.colors.get(m3dj.vertices.get(m3dj.faces.get(i).vertices[2]).colorIndex).b;
                    model.meshes[k].colors[l * 12 + 3] = (byte) m3dj.colors.get(m3dj.vertices.get(m3dj.faces.get(i).vertices[2]).colorIndex).a;
                }
            }

            if (m3dj.faces.get(i).texCoords[0] != M3D_UNDEF) {
                model.meshes[k].texcoords[l*6 + 0] = (float) m3dj.textureMap.get(m3dj.faces.get(i).texCoords[0]).u;
                model.meshes[k].texcoords[l*6 + 1] = (float) (1.0f - m3dj.textureMap.get(m3dj.faces.get(i).texCoords[0]).v);
                model.meshes[k].texcoords[l*6 + 2] = (float) m3dj.textureMap.get(m3dj.faces.get(i).texCoords[1]).u;
                model.meshes[k].texcoords[l*6 + 3] = (float) (1.0f - m3dj.textureMap.get(m3dj.faces.get(i).texCoords[1]).v);
                model.meshes[k].texcoords[l*6 + 4] = (float) m3dj.textureMap.get(m3dj.faces.get(i).texCoords[2]).u;
                model.meshes[k].texcoords[l*6 + 5] = (float) (1.0f - m3dj.textureMap.get(m3dj.faces.get(i).texCoords[2]).v);
            }

            if (m3dj.faces.get(i).normals[0] != M3D_UNDEF) {
                model.meshes[k].normals[l*9 + 0] = (float) m3dj.vertices.get(m3dj.faces.get(i).normals[0]).x;
                model.meshes[k].normals[l*9 + 1] = (float) m3dj.vertices.get(m3dj.faces.get(i).normals[0]).y;
                model.meshes[k].normals[l*9 + 2] = (float) m3dj.vertices.get(m3dj.faces.get(i).normals[0]).z;
                model.meshes[k].normals[l*9 + 3] = (float) m3dj.vertices.get(m3dj.faces.get(i).normals[1]).x;
                model.meshes[k].normals[l*9 + 4] = (float) m3dj.vertices.get(m3dj.faces.get(i).normals[1]).y;
                model.meshes[k].normals[l*9 + 5] = (float) m3dj.vertices.get(m3dj.faces.get(i).normals[1]).z;
                model.meshes[k].normals[l*9 + 6] = (float) m3dj.vertices.get(m3dj.faces.get(i).normals[2]).x;
                model.meshes[k].normals[l*9 + 7] = (float) m3dj.vertices.get(m3dj.faces.get(i).normals[2]).y;
                model.meshes[k].normals[l*9 + 8] = (float) m3dj.vertices.get(m3dj.faces.get(i).normals[2]).z;
            }

            // Add skin (vertices / bone weight pairs)
            if (!m3dj.bones.isEmpty() /*&& !m3dj.skins.isEmpty*/) {
                for (n = 0; n < 3; n++) {
                    int skinid = m3dj.vertices.get(m3dj.faces.get(i).vertices[n]).skinIndex;

                    // Check if there is a skin for this mesh, should be, just failsafe
                    if ((skinid != M3D_UNDEF) && (skinid < (int)m3dj.skins.size())) {
                        for (j = 0; j < 4; j++) {
                            model.meshes[k].boneIds[l*12 + n*4 + j] = (byte) m3dj.skins.get(skinid).boneIds[j];
                            model.meshes[k].boneWeights[l*12 + n*4 + j] = m3dj.skins.get(skinid).weights[j];
                        }
                    }
                    else {
                        // raylib does not handle boneless meshes with skeletal animations, so
                        // we put all vertices without a bone into a special "no bone" bone
                        model.meshes[k].boneIds[l*12 + n*4] = (byte) m3dj.bones.size();
                        model.meshes[k].boneWeights[l*12 + n*4] = 1.0f;
                    }
                }
            }
        }

        /* TODO: not relevant for test
        // Load materials
        for (i = 0; i < (int)m3dj.materials.size(); i++) {
            model.materials[i + 1] = rlj.models.LoadMaterialDefault();

            for (j = 0; j < m3dj.materials.get(i).properties.size(); j++) {
                property = m3dj.materials.get(i).properties.get(j);

                if (M3DJ_Property.propertyTypes.equals(m3dp_Kd)) {
                    System.arraycopy( & model.materials[i + 1].maps[MATERIAL_MAP_DIFFUSE].color, &property.value.color, 4);
                    model.materials[i + 1].maps[MATERIAL_MAP_DIFFUSE].value = 0.0f;
                }
                else if (M3DJ_Property.propertyTypes.equals(m3dp_Ks)) {
                    System.arraycopy( & model.materials[i + 1].maps[MATERIAL_MAP_SPECULAR].color, &property.value.color, 4);
                }
                else if (M3DJ_Property.propertyTypes.equals(m3dp_Ns)) {
                    model.materials[i + 1].maps[MATERIAL_MAP_SPECULAR].value = property.value.fnum;
                }
                else if (M3DJ_Property.propertyTypes.equals(m3dp_Ke)) {
                    System.arraycopy( & model.materials[i + 1].maps[MATERIAL_MAP_EMISSION].color, &property.value.color, 4);
                    model.materials[i + 1].maps[MATERIAL_MAP_EMISSION].value = 0.0f;
                }
                else if (M3DJ_Property.propertyTypes.equals(m3dp_Pm)) {
                    model.materials[i + 1].maps[MATERIAL_MAP_METALNESS].value = property.value.fnum;
                }
                else if (M3DJ_Property.propertyTypes.equals(m3dp_Pr)) {
                    model.materials[i + 1].maps[MATERIAL_MAP_ROUGHNESS].value = property.value.fnum;
                }
                else if (M3DJ_Property.propertyTypes.equals(m3dp_Ps)) {
                    model.materials[i + 1].maps[MATERIAL_MAP_NORMAL].color = WHITE;
                    model.materials[i + 1].maps[MATERIAL_MAP_NORMAL].value = property.value.fnum;
                }
                else {
                    if (property.type >= 128) {
                        Image image = {0};
                        image.data = m3dj.texture[property.value.textureid].d;
                        image.width = m3dj.texture[property.value.textureid].w;
                        image.height = m3dj.texture[property.value.textureid].h;
                        image.mipmaps = 1;
                        image.format = (m3dj.texture[property.value.textureid].f == 4) ? PIXELFORMAT_UNCOMPRESSED_R8G8B8A8 :
                                ((m3dj.texture[property.value.textureid].f == 3) ? PIXELFORMAT_UNCOMPRESSED_R8G8B8 :
                                        ((m3dj.texture[property.value.textureid].f == 2) ? PIXELFORMAT_UNCOMPRESSED_GRAY_ALPHA : PIXELFORMAT_UNCOMPRESSED_GRAYSCALE));

                        switch (property.type) {
                            case m3dp_map_Kd:
                                model.materials[i + 1].maps[MATERIAL_MAP_DIFFUSE].texture = LoadTextureFromImage(image);
                                break;
                            case m3dp_map_Ks:
                                model.materials[i + 1].maps[MATERIAL_MAP_SPECULAR].texture = LoadTextureFromImage(image);
                                break;
                            case m3dp_map_Ke:
                                model.materials[i + 1].maps[MATERIAL_MAP_EMISSION].texture = LoadTextureFromImage(image);
                                break;
                            case m3dp_map_Km:
                                model.materials[i + 1].maps[MATERIAL_MAP_NORMAL].texture = LoadTextureFromImage(image);
                                break;
                            case m3dp_map_Ka:
                                model.materials[i + 1].maps[MATERIAL_MAP_OCCLUSION].texture = LoadTextureFromImage(image);
                                break;
                            case m3dp_map_Pm:
                                model.materials[i + 1].maps[MATERIAL_MAP_ROUGHNESS].texture = LoadTextureFromImage(image);
                                break;
                            default:
                                break;
                        }
                    }
                }
            }
        }

        // Load bones
        if (!m3dj.bones.isEmpty()) {
            model.boneCount = m3dj.bones.size() + 1;
            model.bones = new BoneInfo[model.boneCount];
            model.bindPose = new Transform[model.boneCount];

            for (i = 0; i < (int)m3dj.bones.size(); i++) {
                model.bones[i].parent = m3dj.bone[i].parent;
                strncpy(model.bones[i].name, m3dj.bone[i].name, sizeof(model.bones[i].name) - 1);
                model.bindPose[i].translation.x = m3dj.vertices[m3dj.bone[i].pos].x*m3dj.header.scale;
                model.bindPose[i].translation.y = m3dj.vertices[m3dj.bone[i].pos].y*m3dj.header.scale;
                model.bindPose[i].translation.z = m3dj.vertices[m3dj.bone[i].pos].z*m3dj.header.scale;
                model.bindPose[i].rotation.x = m3dj.vertices[m3dj.bone[i].ori].x;
                model.bindPose[i].rotation.y = m3dj.vertices[m3dj.bone[i].ori].y;
                model.bindPose[i].rotation.z = m3dj.vertices[m3dj.bone[i].ori].z;
                model.bindPose[i].rotation.w = m3dj.vertices[m3dj.bone[i].ori].w;

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
            model.bones[i].parent = -1;
            model.bones[i].name = "NO BONE";
            model.bindPose[i].translation.x = 0.0f;
            model.bindPose[i].translation.y = 0.0f;
            model.bindPose[i].translation.z = 0.0f;
            model.bindPose[i].rotation.x = 0.0f;
            model.bindPose[i].rotation.y = 0.0f;
            model.bindPose[i].rotation.z = 0.0f;
            model.bindPose[i].rotation.w = 1.0f;
            model.bindPose[i].scale.x = model.bindPose[i].scale.y = model.bindPose[i].scale.z = 1.0f;
        }
        */

        /*
        // Load bone-pose default mesh into animation vertices. These will be updated when UpdateModelAnimation gets
        // called, but not before, however DrawMesh uses these if they exist (so not good if they are left empty)
        if (!m3dj.bones.isEmpty() /*&& !m3dj.skins.isEmpty()*) {
            for (i = 0; i < model.meshCount; i++) {
                System.arraycopy(model.meshes[i].animVertices, model.meshes[i].vertices, model.meshes[i].vertexCount*3*sizeof(float));
                System.arraycopy(model.meshes[i].animNormals, model.meshes[i].normals, model.meshes[i].vertexCount*3*sizeof(float));

                model.meshes[i].boneCount = model.boneCount;
                model.meshes[i].boneMatrices = Matrix[model.meshes[i].boneCount];
                for (j = 0; j < model.meshes[i].boneCount; j++) {
                    model.meshes[i].boneMatrices[j] = Raymath.MatrixIdentity();
                }
            }
        }
        */

        return model;
    }

}