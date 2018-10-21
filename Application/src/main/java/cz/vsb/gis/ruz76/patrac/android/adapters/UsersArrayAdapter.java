package cz.vsb.gis.ruz76.patrac.android.adapters;

import android.content.Context;
import android.graphics.Color;
import android.support.annotation.NonNull;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import java.util.List;

import cz.vsb.gis.ruz76.patrac.android.R;
import cz.vsb.gis.ruz76.patrac.android.domain.User;

/**
 * Created by jencek on 10.10.18.
 */
public class UsersArrayAdapter extends ArrayAdapter<String> {

    Context context;
    int resource;
    List<String> strings;
    List<User> objects;

    public UsersArrayAdapter(@NonNull Context context, int resource, @NonNull List<User> objects, List<String> strings) {
        super(context, resource, strings);
        this.context = context;
        this.resource = resource;
        this.strings = strings;
        this.objects = objects;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {

        LayoutInflater inflater = (LayoutInflater) context
                .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View rowView = inflater.inflate(R.layout.rowbutton, parent, false);
        TextView textView = rowView.findViewById(R.id.label);
        textView.setText(objects.get(position).getName());

        if (objects.get(position).isSelected()) {
            rowView.setBackgroundColor(Color.LTGRAY);// set your color
        } else {
            rowView.setBackgroundColor(Color.TRANSPARENT);
        }

        return rowView;
    }
}
