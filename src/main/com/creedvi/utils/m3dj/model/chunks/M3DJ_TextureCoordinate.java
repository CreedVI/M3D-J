package com.creedvi.utils.m3dj.model.chunks;

public class M3DJ_TextureCoordinate {

    public double u;
    public double v;

    public M3DJ_TextureCoordinate(double u, double v) {
        this.u = 0.0f;
        this.v = 0.0f;
    }

    public M3DJ_TextureCoordinate(float u, float v) {
        this.u = u;
        this.v = v;
    }

}
