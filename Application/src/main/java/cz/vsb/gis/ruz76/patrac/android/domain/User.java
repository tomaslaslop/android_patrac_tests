package cz.vsb.gis.ruz76.patrac.android.domain;

/**
 * Created by jencek on 10.10.18.
 */

public class User {
    private String id;
    private String name;
    private boolean selected;

    public User(String id, String name, boolean selected) {
        this.id = id;
        this.name = name;
        this.selected = selected;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public boolean isSelected() {
        return selected;
    }

    public void setSelected(boolean selected) {
        this.selected = selected;
    }
}
