package com.creedvi.utils.m3dj;

import com.raylib.java.Raylib;
import com.raylib.java.core.rcamera.Camera3D;
import com.raylib.java.models.*;
import com.raylib.java.structs.*;

import static com.raylib.java.structs.Color.*;
import static com.raylib.java.core.input.Mouse.MouseButton.MOUSE_BUTTON_LEFT;
import static com.raylib.java.core.rcamera.Camera3D.CameraMode.CAMERA_FREE;
import static com.raylib.java.core.rcamera.Camera3D.CameraProjection.CAMERA_PERSPECTIVE;

public class RenderTesting {

    static Raylib rlj;

    public static void main(String[] args) {
        // Initialization
        //--------------------------------------------------------------------------------------
        int screenWidth = 800;
        int screenHeight = 450;

        rlj = new Raylib(screenWidth, screenHeight, "raylib-j [models] example - models loading, M3D");

        // Define the camera to look into our 3d world
        Camera3D camera = new Camera3D(rlj);
        camera.position = new Vector3(0, 8, 16); // Camera position
        camera.target = new Vector3(0.0f, 0, 0.0f);     // Camera looking at point
        camera.up = new Vector3(0.0f, 1.0f, 0.0f);          // Camera up vector (rotation towards target)
        camera.fovy = 45.0f;                                         // Camera field-of-view Y
        camera.projection = CAMERA_PERSPECTIVE;                      // Camera mode type

        Model model = RaylibHelperMethods.M3DJ_LoadModel("assets/suzanne.m3d", rlj);                 // Load model

        Vector3 position = new Vector3(0, 0, 0);                    // Set model position

        BoundingBox bounds = rlj.models.GetMeshBoundingBox(model.meshes[0]);   // Set model bounds

        // NOTE: bounds are calculated from the original size of the model,
        // if model is scaled on drawing, bounds must be also scaled

        boolean selected = false;          // Selected object flag

        rlj.core.SetTargetFPS(60);               // Set our game to run at 60 frames-per-second
        //--------------------------------------------------------------------------------------

        // Main game loop
        while(!rlj.core.WindowShouldClose()) {   // Detect window close button or ESC key
            // Update
            //----------------------------------------------------------------------------------
            camera.Update(CAMERA_FREE);

            // Select model on mouse click
            if(rlj.core.IsMouseButtonPressed(MOUSE_BUTTON_LEFT.ordinal())) {
                // Check collision between ray and box
                if(rlj.models.GetRayCollisionBox(rlj.core.GetMouseRay(rlj.core.GetMousePosition(), camera), bounds).hit) {
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

            if(selected) {
                rlj.models.DrawBoundingBox(bounds, GREEN);   // Draw selection box
            }

            rlj.core.EndMode3D();

            if(selected) {
                rlj.text.DrawText("MODEL SELECTED", rlj.core.GetScreenWidth() - 110, 10, 10, GREEN);
            }

            rlj.text.DrawFPS(10, 10);

            rlj.core.EndDrawing();
            //----------------------------------------------------------------------------------
        }
    }
}