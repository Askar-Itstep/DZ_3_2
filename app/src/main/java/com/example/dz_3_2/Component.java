package com.example.dz_3_2;

import android.graphics.Bitmap;

import java.io.File;

public abstract class Component {
    public Bitmap bmp;
    public String title;
    public String typeFile;

    public Component() {
    }

    public Component(Bitmap bmp, String title) {
        this.bmp = bmp;
        this.title = title;
    }

    public Component(String title, String typeFile) {
        this.title = title;
        this.typeFile = typeFile;
    }

    public Component(Bitmap bmp, String title, String typeFile) {
        this.bmp = bmp;
        this.title = title;
        this.typeFile = typeFile;
    }

    public abstract void Add(Component ...component) throws Exception;
    public abstract void Remove(Component ...component) throws Exception;

    public abstract Component getComponent(int index) throws Exception;
}
