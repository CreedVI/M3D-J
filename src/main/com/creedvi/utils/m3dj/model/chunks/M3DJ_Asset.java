package com.creedvi.utils.m3dj.model.chunks;

import java.nio.ByteBuffer;

public class M3DJ_Asset {

    public String name;
    public ByteBuffer assetData;

    public M3DJ_Asset(int bufferSize) {
        this.name = "";
        this.assetData = ByteBuffer.allocate(bufferSize);
    }

}
