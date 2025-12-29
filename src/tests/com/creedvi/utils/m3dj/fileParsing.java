package com.creedvi.utils.m3dj;

import com.creedvi.utils.m3dj.model.M3DJ_Model;

import java.io.IOException;

public class fileParsing {

    static M3DJ_Model model;

    public static void main(String[] args) {
        M3DJ.__SetDebug(true);
        String filePath = "assets/suzanne.m3d";

        try {
            model = M3DJ.M3DJ_Load(filePath);
        }
        catch (IOException e) {
            throw new RuntimeException(e);
        }

        if (model == null) {
            System.out.println("M3D-J :: TEST :: Model file (" + filePath + ") failed to load.\nExiting...");
            System.exit(-1);
        }
        System.out.println("M3D-J :: TEST :: Model file (" + filePath + ") loaded successfully.");
    }

}
