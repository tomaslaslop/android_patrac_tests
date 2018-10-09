package cz.vsb.gis.ruz76.patrac.android;

import android.annotation.SuppressLint;
import android.app.ActionBar;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Bundle;
import android.app.Activity;
import android.os.Environment;
import android.os.StrictMode;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import org.osmdroid.api.IMapController;
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.Marker;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import java.text.SimpleDateFormat;
import java.util.TimeZone;

/**
 * Class for downloading GPX tracks.
 */
public class MapsActivity extends Activity implements LocationListener, GetRequestUpdate {

    private List<String> gpx_list;
    private List<String> gpx_list_files;
    private ArrayAdapter<String> arrayAdapter;
    private ListView gpxListView;
    private TextView mTextStatus;
    private MapView map = null;
    private LocationManager locationManager;
    private Context context;
    private Marker startMarker;

    /**
     * Set up the {@link android.app.ActionBar}, if the API is available.
     */
    private void setupActionBar() {
        ActionBar actionBar = getActionBar();
        if (actionBar != null) {
            // Show the Up button in the action bar.
            actionBar.setDisplayHomeAsUpEnabled(true);
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == android.R.id.home) {
            finish();
        }
        return true;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);
        setTitle(getString(R.string.tracks));

        mTextStatus = (TextView) findViewById(R.id.textViewStatus);

        StrictMode.VmPolicy.Builder builder = new StrictMode.VmPolicy.Builder();
        StrictMode.setVmPolicy(builder.build());

        setUpMapView();

        String[] gpxs = new String[] { getString(R.string.local_track), getString(R.string.searchers_positions), getString(R.string.searchers_tracks) };
        gpx_list = new ArrayList<String>(Arrays.asList(gpxs));
        gpx_list_files = new ArrayList<String>();
        gpx_list_files.add("local.gpx");
        gpx_list_files.add("server_last.gpx");
        gpx_list_files.add("server.gpx");

        arrayAdapter = new ArrayAdapter<String>
                (this, android.R.layout.simple_list_item_1, gpx_list);

        gpxListView = (ListView) findViewById(R.id.gpxListView);
        gpxListView.setAdapter(arrayAdapter);

        setupActionBar();

        gpxListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                if (position == 0) {
                    mTextStatus.setText(getString(R.string.preparing_data_wait));
                    int log = createLocalGpx();
                    if (log == 0) {
                        mTextStatus.setText(R.string.preparing_data_open);
                        Intent i = new Intent();
                        i.setAction(android.content.Intent.ACTION_VIEW);
                        i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                        i.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                        String fileName = gpx_list_files.get(position);
                        String path = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MOVIES).toString() + "/" + fileName;
                        i.setDataAndType(Uri.parse("file://" + path), "application/gpx+xml");
                        File file = new File(path);
                        if (file.exists()) {
                            try {
                                startActivity(i);
                            } catch (ActivityNotFoundException exception) {
                                Toast toast = Toast.makeText(MapsActivity.this, R.string.can_not_open_activity, Toast.LENGTH_LONG);
                                toast.show();
                            }
                            mTextStatus.setText(R.string.ready_for_download);
                        } else {
                            mTextStatus.setText(R.string.error_data_download);
                        }
                    }
                    if (log == 1) {
                        mTextStatus.setText(R.string.less_than_two_positions);
                    }
                    if (log == 2) {
                        mTextStatus.setText(R.string.problem_to_create_log);
                    }
                }

                if (position == 1) {
                    mTextStatus.setText(R.string.downloading_wait);
                    downloadFromUrl(MainActivity.endPoint + "operation=getgpx_last&searchid=" + MainActivity.searchid, "server_last.gpx");
                }

                if (position == 2) {
                    mTextStatus.setText(R.string.downloading_wait);
                    downloadFromUrl(MainActivity.endPoint + "operation=getgpx&searchid=" + MainActivity.searchid, "server.gpx");
                }

            }

            private int createLocalGpx() {
                //if (MainActivity.waypoints.size() < 2) return 1;
                String path = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MOVIES).toString() + "/local.gpx";
                try {
                    PrintStream local_gpx = new PrintStream(new FileOutputStream(path));
                    String header = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n";
                    header += "<gpx version=\"1.1\" creator=\"Patrac\"\n";
                    header += "xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xmlns=\"http://www.topografix.com/GPX/1/1\"\n";
                    header += "xsi:schemaLocation=\"http://www.topografix.com/GPX/1/1 http://www.topografix.com/GPX/1/1/gpx.xsd\"\n";
                    header += "xmlns:gpxtpx=\"http://www.garmin.com/xmlschemas/TrackPointExtension/v1\"><trk><name>Toto zařízení</name><trkseg>\n";
                    local_gpx.println(header);
                    for (int i=0; i < MainActivity.waypoints.size(); i++) {
                        Waypoint wp = MainActivity.waypoints.get(i);
                        SimpleDateFormat dateFormatGmt = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                        dateFormatGmt.setTimeZone(TimeZone.getTimeZone("GMT"));
                        String timeutc = dateFormatGmt.format(wp.timeutc).toString().replaceAll(" ", "T") + "Z";
                        local_gpx.println("<trkpt lat=\"" + wp.lat + "\" lon=\"" + wp.lon + "\"><name>" + R.string.this_device + "</name><time>" + timeutc + "</time></trkpt>");
                    }
                    local_gpx.println("</trkseg></trk></gpx>");
                    local_gpx.close();
                    return 0;
                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                    return 2;
                }
            }

        });

    }

    @Override
    public void onResume(){
        super.onResume();
        //this will refresh the osmdroid configuration on resuming.
        //if you make changes to the configuration, use
        //SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        //Configuration.getInstance().load(this, PreferenceManager.getDefaultSharedPreferences(this));
        map.onResume(); //needed for compass, my location overlays, v6.0.0 and up
    }

    @Override
    public void onPause(){
        super.onPause();
        //this will refresh the osmdroid configuration on resuming.
        //if you make changes to the configuration, use
        //SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        //Configuration.getInstance().save(this, prefs);
        map.onPause();  //needed for compass, my location overlays, v6.0.0 and up
    }

    @SuppressLint("MissingPermission")
    private void setUpMapView() {

        context = this.getApplicationContext();
        locationManager = (LocationManager) context.getSystemService(LOCATION_SERVICE);
        locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, this);
        Location locationGPS = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
        double newlat = 49.8;
        double newlon = 18.1;
        if (locationGPS != null) {
            newlat = locationGPS.getLatitude();
            newlon = locationGPS.getLongitude();
        }

        map = (MapView) findViewById(R.id.map);
        map.setTileSource(TileSourceFactory.MAPNIK);
        map.setBuiltInZoomControls(true);
        map.setMultiTouchControls(true);
        IMapController mapController = map.getController();
        GeoPoint startPoint = new GeoPoint(newlat, newlon);
        mapController.setCenter(startPoint);
        mapController.setZoom(12d);

        startMarker = new Marker(map);
        startMarker.setPosition(startPoint);
        startMarker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);
        startMarker.setIcon(context.getResources().getDrawable(R.drawable.green_square_24dp));
        startMarker.setTitle(getString(R.string.this_device));
        map.getOverlays().add(startMarker);

        getLocations();

        //startMarker.setIcon(context.getResources().getDrawable(R.drawable.ic_info_black_24dp));
        //mapController.zoomTo(9, 1000L);
    }

    private void downloadFromUrl(String url, String fileName) {
        DownloadFileFromUrl downloadFileFromUrl = new DownloadFileFromUrl();
        downloadFileFromUrl.setActivity(this);
        downloadFileFromUrl.setOpenFile(true);
        downloadFileFromUrl.setTextStatus(mTextStatus);
        downloadFileFromUrl.setFileName(fileName);
        downloadFileFromUrl.execute(url);
    }

    private void getLocations() {
        ServerGetRequest serverGetRequest = new ServerGetRequest();
        serverGetRequest.setActivity(this);
        serverGetRequest.setTextStatus(mTextStatus);
        serverGetRequest.execute(MainActivity.endPoint + "operation=getlocations&searchid=" + MainActivity.searchid);
    }

    @Override
    public void onLocationChanged(Location location) {
        GeoPoint startPoint = new GeoPoint(location.getLatitude(), location.getLongitude());
        startMarker.setPosition(startPoint);
        map.getController().setCenter(startPoint);
        map.invalidate();
    }

    @Override
    public void onStatusChanged(String s, int i, Bundle bundle) {

    }

    @Override
    public void onProviderEnabled(String s) {

    }

    @Override
    public void onProviderDisabled(String s) {

    }

    @Override
    public void processResponse(String result) {
        if (result != null) {
            String[] searchers = result.split("\n");
            for (int i = 0; i < searchers.length; i++) {
                String[] items = searchers[i].split(";");
                if (items.length > 5) {
                    Marker itemMarker = new Marker(map);
                    double lon = Double.parseDouble(items[4].split(" ")[0]);
                    double lat = Double.parseDouble(items[4].split(" ")[1]);
                    GeoPoint startPoint = new GeoPoint(lat, lon);
                    itemMarker.setPosition(startPoint);
                    itemMarker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);
                    itemMarker.setIcon(context.getResources().getDrawable(R.drawable.ic_info_black_24dp));
                    itemMarker.setTitle(items[3]);
                    map.getOverlays().add(itemMarker);
                }
            }
            map.invalidate();
        }
    }
}
