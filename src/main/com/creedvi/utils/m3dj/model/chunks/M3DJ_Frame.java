package com.creedvi.utils.m3dj.model.chunks;

public class M3DJ_Frame {

    public int timestamp;
    public int transformsCount;
    public M3DJ_Transform[] transforms;

    public M3DJ_Frame(int timestamp, int transformsCount) {
        this.timestamp = timestamp;
        this.transformsCount = transformsCount;
        this.transforms = new M3DJ_Transform[transformsCount];
        for (int i = 0; i < transformsCount; i++) {
            this.transforms[i] = new M3DJ_Transform();
        }
    }

}
