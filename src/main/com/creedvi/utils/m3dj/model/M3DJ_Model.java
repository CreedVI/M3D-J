package com.creedvi.utils.m3dj.model;

import com.creedvi.utils.m3dj.model.chunks.M3DJ_Header;
import com.creedvi.utils.m3dj.model.chunks.M3DJ_Preview;

public class M3DJ_Model {

    public M3DJ_Preview preview;
    public M3DJ_Header header;

    public M3DJ_Model() {
        this.preview = new M3DJ_Preview();
        this.header = new M3DJ_Header();
    }

}
