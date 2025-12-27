package com.creedvi.utils.m3dj.model.chunks;

import static com.creedvi.utils.m3dj.M3DJ.M3D_NUMBONE;

public class M3DJ_Skin {

    public int[] boneIds;
    public float[] weights;

    public M3DJ_Skin() {
        this.boneIds = new int[M3D_NUMBONE];
        this.weights = new float[M3D_NUMBONE];
    }

}
