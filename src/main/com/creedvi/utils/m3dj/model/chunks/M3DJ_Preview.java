package com.creedvi.utils.m3dj.model.chunks;

import java.nio.ByteBuffer;

public class M3DJ_Preview {

    public ByteBuffer imageData;
    public boolean hasPreview;

    public M3DJ_Preview() {
        this.hasPreview = false;
    }

    public void allocateImageBuffer(int size) {
        this.imageData = ByteBuffer.allocate(size);
    }

}
