package com.creedvi.utils.m3dj;

import com.creedvi.utils.m3dj.io.Tracelog;
import com.creedvi.utils.m3dj.model.M3DJ_Model;

import java.io.IOException;

public class fileParsing {

    public static void main(String[] args) {
        int failCount = 0;

        M3DJ parser = new M3DJ(Tracelog.LogLevel.LEVEL_ERROR);

        String[] filePath = new String[] {
                "assets/aliveai_character.m3d",
                "assets/bezier.m3d",
                "assets/CesiumMan.m3d",
                "assets/cube.m3d",
                "assets/cube_normals.m3d",
                "assets/cube_usemtl.m3d",
                "assets/cube_with_vertex_colors.m3d",
                "assets/lantea.m3d",
                "assets/mobs_dwarves_character.m3d",
                "assets/nurbs.m3d",
                "assets/suzanne.m3d",
        };

        for (String s : filePath) {
            System.out.println("======");
            System.out.println("Model under test: " + s);

            M3DJ_Model model;

            try {
                model = parser.LoadFile(s);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }

            if (model == null) {
                System.out.println("M3D-J :: TEST :: Model file (" + s + ") failed to load.\nExiting...");
                failCount++;
                continue;
            } else {
                System.out.println("M3D-J :: TEST :: Model file (" + s + ") loaded successfully.");
            }
        }

        System.out.println("M3D-J :: TEST :: " + failCount + "models failed to load of " + filePath.length + " files provided.");
    }

}
