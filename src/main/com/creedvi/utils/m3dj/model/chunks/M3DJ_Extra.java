package com.creedvi.utils.m3dj.model.chunks;

import java.nio.ByteBuffer;

public class M3DJ_Extra {

    public String key;
    public ByteBuffer data;

    public M3DJ_Extra(String key, int bufferSize) {
        this.key = key;
        this.data = ByteBuffer.allocate(bufferSize);
    }

}
