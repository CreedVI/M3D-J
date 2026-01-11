package com.creedvi.utils.m3dj.model.chunks;

import java.util.ArrayList;

public class M3DJ_Action {

    public String name;
    public int frameCount;
    public int animationLength;
    public ArrayList<M3DJ_Frame> frames;

    public M3DJ_Action() {
        this.frames = new ArrayList<>();
    }

}
