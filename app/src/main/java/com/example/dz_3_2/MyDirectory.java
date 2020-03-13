package com.example.dz_3_2;

import android.graphics.Bitmap;
import android.util.Log;

import java.io.File;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
public class MyDirectory extends Component {

    List<Component> components = new ArrayList<>();
    public MyDirectory() {}

    public MyDirectory(List<Component> components) {
        this.components = components;
    }

    public MyDirectory(Bitmap bmp, String title, List<Component> components) {
        super(bmp, title);
        this.components = components;
    }

    public MyDirectory(String title, String typeFile, List<Component> components) {
        super(title, typeFile);
        this.components = components;
    }

    public MyDirectory(Bitmap bmp, String title, String typeFile) {
        super(bmp, title, typeFile);
    }

    public MyDirectory(Bitmap bmp, String title) {
        super(bmp, title);
    }

    public MyDirectory(String title, String typeFile) {
        super(title, typeFile);
    }

    @Override
    public void Add(Component ...component) throws Exception {
        for (Component comp: component)
            components.add(comp);
    }
    @Override
    public void Remove(Component ...component) throws Exception {
        for (Component comp: component)
            components.remove(comp);
    }

    @Override
    public Component getComponent(int index) throws Exception {
        return components.get(index);
    }
    public int getSize(){
        return components.size();
    }

    public Iterable<Component> getIterable() {
        List<Component> list = new ArrayList<Component>();
        Iterator<Component> iterator = components.iterator();
        while (iterator.hasNext())
            list.add(iterator.next());
        return list;
    }
}
