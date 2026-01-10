package com.creedvi.utils.m3dj.model;

import com.creedvi.utils.m3dj.M3DJ;
import com.creedvi.utils.m3dj.model.chunks.*;

import java.util.ArrayList;

public class M3DJ_Model {

    public M3DJ_Preview preview;
    public M3DJ_Header header;
    public ArrayList<M3DJ_Asset> assets;
    public ArrayList<M3DJ_Color> colors;
    public ArrayList<M3DJ_TextureCoordinate> textureMap;
    public ArrayList<M3DJ_Vertex> vertices;
    public ArrayList<M3DJ_Bone> bones;
    public ArrayList<M3DJ_Material> materials;
    public ArrayList<M3DJ_Texture> textures;
    public ArrayList<M3DJ_Face> faces;
    public ArrayList<M3DJ_Parameter> parameters;
    public ArrayList<M3DJ_Skin> skins;
    public ArrayList<M3DJ_Label> labels;
    public ArrayList<M3DJ_Action> actions;
    public ArrayList<M3DJ_Extra> extras;

    public M3DJ_Model() {
        this.preview = new M3DJ_Preview();
        this.header = new M3DJ_Header();
        this.assets = new ArrayList<>();
        this.colors = new ArrayList<>();
        this.textureMap = new ArrayList<>();
        this.vertices = new ArrayList<>();
        this.bones = new ArrayList<>();
        this.materials = new ArrayList<>();
        this.textures = new ArrayList<>();
        this.faces = new ArrayList<>();
        this.parameters = new ArrayList<>();
        this.skins = new ArrayList<>();
        this.labels = new ArrayList<>();
        this.actions = new ArrayList<>();
        this.extras = new ArrayList<>();
    }

}
