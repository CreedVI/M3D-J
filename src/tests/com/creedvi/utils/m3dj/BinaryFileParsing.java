package com.creedvi.utils.m3dj;

import com.creedvi.utils.m3dj.io.Tracelog;
import com.creedvi.utils.m3dj.model.M3DJ_Model;

import java.io.IOException;
import java.util.Scanner;

public class BinaryFileParsing {

    private static final boolean DEBUG = true;
    private static final String ASSET_DIRECTORY = "assets/";

    public static void main(String[] args) {
        int failCount = 0;
        Scanner in = new Scanner(System.in);
        M3DJ parser;
        if(DEBUG) {
            parser = new M3DJ(Tracelog.LogLevel.LEVEL_DEBUG);
        }
        else {
            parser = new M3DJ(Tracelog.LogLevel.LEVEL_ERROR);
        }

        String[] filePath = new String[]{
                "aliveai_character.m3d",
                "bezier.m3d",
                "CesiumMan.m3d",
                "cube.m3d",
                "cube_normals.m3d",
                "cube_usemtl.m3d",
                "cube_with_vertexcolors.m3d",
                "lantea.m3d",
                "mobs_dwarves_character.m3d",
                "nurbs.m3d",
                "suzanne.m3d",
                "voxel.m3d",
                "WusonBlitz0.m3d",
                "WusonBlitz1.m3d",
                "WusonBlitz2.m3d",
        };

        for(String s : filePath) {
            System.out.println("======");
            System.out.println("Model under test: " + s);

            M3DJ_Model model;

            try {
                model = parser.LoadFile(ASSET_DIRECTORY + s);
            } catch(IOException e) {
                throw new RuntimeException(e);
            }

            if(model == null) {
                System.out.println("M3D-J :: TEST :: Model file (" + s + ") failed to load.\nExiting...");
                failCount++;
                continue;
            }
            else {
                System.out.println("M3D-J :: TEST :: Model file (" + s + ") loaded successfully.");
            }

            if(DEBUG) {
                try {
                    System.out.println("Write model dump file? Y/N");
                    System.out.println("Any input besides 'y' or 'yes' will result in no dump file being written.");
                    String input = in.next();

                    if(input.equalsIgnoreCase("y") | input.equalsIgnoreCase("yes")) {
                        parser.DumpModel(model, s + ".dump");
                    }

                } catch(IOException e) {
                    throw new RuntimeException(e);
                }
            }
        }

        System.out.println("M3D-J :: TEST :: " + failCount + " model(s) failed to load of " + filePath.length + " file(s) provided.");

    }

}
