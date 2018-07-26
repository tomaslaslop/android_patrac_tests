package cz.vsb.gis.ruz76.patrac.android;

import android.widget.ArrayAdapter;

import java.util.ArrayList;

/**
 * Class for filling ArrayAdapter with objects from list.
 */
public class AdapterHelper {
    /**
     * Updates adapter with list of objects. Clears the adapter first.
     * @param arrayAdapter adapter to update
     * @param listOfObject list of objects to add
     */
    @SuppressWarnings({ "rawtypes", "unchecked" })
    public void update(ArrayAdapter arrayAdapter, ArrayList<Object> listOfObject){
        arrayAdapter.clear();
        for (Object object : listOfObject){
            arrayAdapter.add(object);
        }
    }
}
