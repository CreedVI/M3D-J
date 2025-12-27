package com.creedvi.utils.m3dj.model.chunks;

import java.util.ArrayList;

public class M3DJ_Material {

    public String name;
    public ArrayList<M3DJ_Property> properties;

    public M3DJ_Material() {
        this.properties = new ArrayList<>();
    }
}
