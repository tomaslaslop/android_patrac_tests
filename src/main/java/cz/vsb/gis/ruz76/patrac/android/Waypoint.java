package cz.vsb.gis.ruz76.patrac.android;

import java.util.Date;

/**
 * Created by jencek on 12.2.18.
 */

public class Waypoint {
    public double lat;
    public double lon;
    public Date timeutc;

    public Waypoint(double lat, double lon, Date timeutc) {
        this.lat = lat;
        this.lon = lon;
        this.timeutc = timeutc;
    }

    public double getLat() {
        return lat;
    }

    public void setLat(double lat) {
        this.lat = lat;
    }

    public double getLon() {
        return lon;
    }

    public void setLon(double lon) {
        this.lon = lon;
    }

    public Date getTimeutc() {
        return timeutc;
    }

    public void setTimeutc(Date timeutc) {
        this.timeutc = timeutc;
    }
}
