package com.creedvi.utils.m3dj.model.chunks;

import java.nio.ByteBuffer;

public class M3DJ_Preview {

    public ByteBuffer imageData;

    public M3DJ_Preview() {

    }

    public void allocateImageBuffer(int size) {
        this.imageData = ByteBuffer.allocate(size);
    }

}
