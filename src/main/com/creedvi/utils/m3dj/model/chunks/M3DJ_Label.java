package com.creedvi.utils.m3dj.model.chunks;

import java.nio.ByteBuffer;

import static com.creedvi.utils.m3dj.M3DJ.M3D_UNDEF;

public class M3DJ_Label {
    public String name;
    public String language;
    public String text;
    public int colorId;
    public int vertexId;

    public M3DJ_Label() {
        this.name = "";
        this.language = "";
        this.text = "";
        this.colorId = M3D_UNDEF;
        this.vertexId = M3D_UNDEF;
    }
}
