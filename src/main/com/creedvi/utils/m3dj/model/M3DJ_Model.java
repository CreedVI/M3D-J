package com.creedvi.utils.m3dj.model;

import com.creedvi.utils.m3dj.model.chunks.M3DJ_ColorMap;
import com.creedvi.utils.m3dj.model.chunks.M3DJ_Header;
import com.creedvi.utils.m3dj.model.chunks.M3DJ_Preview;
import com.creedvi.utils.m3dj.model.chunks.M3DJ_TextureMap;

public class M3DJ_Model {

    public M3DJ_Preview preview;
    public M3DJ_Header header;
    public M3DJ_ColorMap colorMap;
    public M3DJ_TextureMap textureMap;

    public M3DJ_Model() {
        this.preview = new M3DJ_Preview();
        this.header = new M3DJ_Header();
        this.colorMap = new M3DJ_ColorMap();
        this.textureMap = new M3DJ_TextureMap();
    }

}
