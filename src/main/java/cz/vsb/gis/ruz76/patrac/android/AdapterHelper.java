package cz.vsb.gis.ruz76.patrac.android;

import android.widget.ArrayAdapter;

import java.util.ArrayList;

public class AdapterHelper {
    @SuppressWarnings({ "rawtypes", "unchecked" })
    public void update(ArrayAdapter arrayAdapter, ArrayList<Object> listOfObject){
        arrayAdapter.clear();
        for (Object object : listOfObject){
            arrayAdapter.add(object);
        }
    }
}
