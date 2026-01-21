package com.creedvi.utils.m3dj.model.chunks;

public class M3DJ_Bone {

    public int parentIndex;
    public String name;
    public int position;
    public int orientation;
    public float[] matrix4;

    public M3DJ_Bone() {
        this.matrix4 = new float[16];
    }

}
