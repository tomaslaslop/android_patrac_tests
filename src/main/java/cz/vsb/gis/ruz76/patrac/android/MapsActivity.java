package cz.vsb.gis.ruz76.patrac.android;

import android.app.ActionBar;
import android.content.Intent;
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
public class MapsActivity extends Activity {

    private List<String> gpx_list;
    private List<String> gpx_list_files;
    private ArrayAdapter<String> arrayAdapter;
    private ListView gpxListView;
    private TextView mTextStatus;

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
                            startActivity(i);
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

    private void downloadFromUrl(String url, String fileName) {
        DownloadFileFromURL downloadFileFromURL = new DownloadFileFromURL();
        downloadFileFromURL.setActivity(this);
        downloadFileFromURL.setOpenFile(true);
        downloadFileFromURL.setTextStatus(mTextStatus);
        downloadFileFromURL.setFileName(fileName);
        downloadFileFromURL.execute(url);
    }
}
