package com.creedvi.utils.m3dj;

import com.raylib.java.Raylib;
import com.raylib.java.core.rcamera.Camera3D;
import com.raylib.java.models.*;
import com.raylib.java.structs.*;

import static com.raylib.java.core.input.Keyboard.*;
import static com.raylib.java.core.rcamera.Camera3D.CameraMode.CAMERA_FREE;
import static com.raylib.java.structs.Color.*;
import static com.raylib.java.core.rcamera.Camera3D.CameraProjection.CAMERA_PERSPECTIVE;

public class AnimationTesting {

    final static int M3D_ANIMDELAY = 17;

    static Raylib rlj;

    static Vector3 position;
    static boolean drawMesh, drawSkeleton, animPlaying;

    static String modelPath = "assets/CesiumMan.m3d";

    public static void main(String[] args) {
        // Initialization
        //--------------------------------------------------------------------------------------
        int screenWidth = 800;
        int screenHeight = 450;

        rlj = new Raylib(screenWidth, screenHeight, "raylib-j [models] example - models loading, M3D");

        // Define the camera to look into our 3d world
        Camera3D camera = new Camera3D(rlj);
        camera.position = new Vector3(1.5f, 1.5f, 1.5f); // Camera position
        camera.target = new Vector3(0.0f, 0.4f, 0.0f);     // Camera looking at point
        camera.up = new Vector3(0.0f, 1.0f, 0.0f);          // Camera up vector (rotation towards target)
        camera.fovy = 45.0f;                                         // Camera field-of-view Y
        camera.projection = CAMERA_PERSPECTIVE;                      // Camera mode type

        position = new Vector3();
        drawMesh = false;
        drawSkeleton = false;
        animPlaying = false;

        Model model = RaylibHelperMethods.M3DJ_LoadModel(modelPath, rlj);                 // Load model

        int animFrameCounter = 0;
        int animId = 0;
        ModelAnimation[] anims = RaylibHelperMethods.M3DJ_LoadModelAnimations(modelPath, rlj);
        int animsCount = anims.length;


        rlj.core.SetTargetFPS(60);               // Set our game to run at 60 frames-per-second
        //--------------------------------------------------------------------------------------

        // Main game loop
        while(!rlj.core.WindowShouldClose()) {   // Detect window close button or ESC key
            // Main game loop
            // Update
            //----------------------------------------------------------------------------------
            camera.Update(CAMERA_FREE);

            if (anims.length > 0) {
                // Play animation when spacebar is held down (or step one frame with N)
                if (rlj.core.IsKeyDown(KEY_SPACE) || rlj.core.IsKeyPressed(KEY_N)) {
                    animFrameCounter++;

                    if (animFrameCounter >= anims[animId].frameCount) {
                        animFrameCounter = 0;
                    }

                    rlj.models.UpdateModelAnimation(model, anims[animId], animFrameCounter);
                    animPlaying = true;
                }

                // Select animation by pressing C
                if (rlj.core.IsKeyPressed(KEY_C)) {
                    animFrameCounter = 0;
                    animId++;

                    if (animId >= animsCount) {
                        animId = 0;
                    }
                    rlj.models.UpdateModelAnimation(model, anims[animId], 0);
                    animPlaying = true;
                }
            }

            // Toggle skeleton drawing
            if (rlj.core.IsKeyPressed(KEY_B)) {
                drawSkeleton = !drawSkeleton;
            }

            // Toggle mesh drawing
            if (rlj.core.IsKeyPressed(KEY_M)) {
                drawMesh = !drawMesh;
            }
            //----------------------------------------------------------------------------------

            // Draw
            //----------------------------------------------------------------------------------
            rlj.core.BeginDrawing();

            rlj.core.ClearBackground(RAYWHITE);

            rlj.core.BeginMode3D(camera);

            // Draw 3d model with texture
            if (drawMesh) {
                rlj.models.DrawModel(model, position, 1.0f, WHITE);
            }

            // Draw the animated skeleton
            if (drawSkeleton) {
                // Loop to (boneCount - 1) because the last one is a special "no bone" bone,
                // needed to workaround buggy models
                // without a -1, we would always draw a cube at the origin
                for (int i = 0; i < model.boneCount - 1; i++) {
                    // By default the model is loaded in bind-pose by LoadModel()
                    // But if UpdateModelAnimation() has been called at least once
                    // then the model is already in animation pose, so we need the animated skeleton
                    if (!animPlaying || anims.length ==  0) {
                        // Display the bind-pose skeleton
                        rlj.models.DrawCube(model.bindPose[i].translation, 0.04f, 0.04f, 0.04f, RED);

                        if (model.bones[i].parent >= 0) {
                            rlj.models.DrawLine3D(model.bindPose[i].translation, model.bindPose[model.bones[i].parent].translation, RED);
                        }
                    }
                    else {
                        // Display the frame-pose skeleton
                        rlj.models.DrawCube(anims[animId].framePoses[animFrameCounter][i].translation, 0.05f, 0.05f, 0.05f, RED);

                        if (anims[animId].bones[i].parent >= 0) {
                            rlj.models.DrawLine3D(anims[animId].framePoses[animFrameCounter][i].translation, anims[animId].framePoses[animFrameCounter][anims[animId].bones[i].parent].translation, RED);
                        }
                    }
                }
            }

            rlj.models.DrawGrid(10, 1.0f);         // Draw a grid

            rlj.core.EndMode3D();

            rlj.text.DrawText("FRAME: " + animFrameCounter, 10, 10, 10, MAROON);
            rlj.text.DrawText("PRESS SPACE to PLAY MODEL ANIMATION", 10, rlj.core.GetScreenHeight() - 80, 10, MAROON);
            rlj.text.DrawText("PRESS N to STEP ONE ANIMATION FRAME", 10, rlj.core.GetScreenHeight() - 60, 10, DARKGRAY);
            rlj.text.DrawText("PRESS C to CYCLE THROUGH ANIMATIONS", 10, rlj.core.GetScreenHeight() - 40, 10, DARKGRAY);
            rlj.text.DrawText("PRESS M to toggle MESH, B to toggle SKELETON DRAWING", 10, rlj.core.GetScreenHeight() - 20, 10, DARKGRAY);
            rlj.text.DrawText("(c) CesiumMan model by KhronosGroup", rlj.core.GetScreenWidth() - 210, rlj.core.GetScreenHeight() - 20, 10, GRAY);

            rlj.core.EndDrawing();
            //----------------------------------------------------------------------------------
        }

        rlj.core.CloseWindow();
    }

}