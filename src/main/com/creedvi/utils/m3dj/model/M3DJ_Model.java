package com.creedvi.utils.m3dj.model;

import com.creedvi.utils.m3dj.model.chunks.*;

import java.util.ArrayList;

public class M3DJ_Model {

    public M3DJ_Preview preview;
    public M3DJ_Header header;
    public M3DJ_ColorMap colorMap;
    public M3DJ_TextureMap textureMap;
    public ArrayList<M3DJ_Vertex> vertices;
    public M3DJ_Skeleton skeleton;
    public ArrayList<M3DJ_Material> materials;
    public ArrayList<M3DJ_Face> faces;
    public ArrayList<M3DJ_Parameter> parameters;

    public M3DJ_Model() {
        this.preview = new M3DJ_Preview();
        this.header = new M3DJ_Header();
        this.colorMap = new M3DJ_ColorMap();
        this.textureMap = new M3DJ_TextureMap();
        this.vertices = new ArrayList<>();
        this.skeleton = new M3DJ_Skeleton();
        this.materials = new ArrayList<>();
        this.faces = new ArrayList<>();
        this.parameters = new ArrayList<>();
    }

}
