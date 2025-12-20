package com.creedvi.utils.m3dj.model.chunks;

public class M3DJ_Vertex {
    public double x;
    public double y;
    public double z;
    public double w;

    public int colorIndex;
    public int skinIndex;

    public M3DJ_Vertex() {
        colorIndex = -1;
        skinIndex = -1;
    }
}
