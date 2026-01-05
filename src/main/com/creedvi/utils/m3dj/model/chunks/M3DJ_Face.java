package com.creedvi.utils.m3dj.model.chunks;

import com.creedvi.utils.m3dj.M3DJ;

public class M3DJ_Face {

    public int materialId;
    public int[] vertices;
    public int[] normals;
    public int[] texCoords;

    public int paramId;
    public int[] vertMax;

    public M3DJ_Face() {
        this.materialId = -1;
        this.vertices = new int[]{-1, -1, -1};
        this.normals = new int[]{-1, -1, -1};
        this.texCoords = new int[]{-1, -1, -1};

        this.paramId = -1;
        this.vertMax = new int[]{-1, -1, -1};
    }

}
