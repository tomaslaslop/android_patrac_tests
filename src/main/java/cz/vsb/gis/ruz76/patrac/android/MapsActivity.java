package cz.vsb.gis.ruz76.patrac.android;

import android.app.ActionBar;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.app.Activity;
import android.os.Environment;
import android.os.StrictMode;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.net.URL;
import java.net.URLConnection;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import java.text.SimpleDateFormat;
import java.util.TimeZone;

public class MapsActivity extends Activity {

    private List<String> gpx_list;
    private List<String> gpx_list_files;
    private ArrayAdapter<String> arrayAdapter;
    private ListView gpxListView;
    private TextView mTextStatus;
    private String filename = "server.gpx";

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
        setTitle("Stopy a pozice");

        mTextStatus = (TextView) findViewById(R.id.textViewStatus);

        StrictMode.VmPolicy.Builder builder = new StrictMode.VmPolicy.Builder();
        StrictMode.setVmPolicy(builder.build());

        String[] gpxs = new String[] { "Lokální stopa", "Poslední pozice pátračů", "Stopy pátračů" };
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
                    mTextStatus.setText("Připravuji data. Může chvíli trvat.");
                    int log = createLocalGpx();
                    if (log == 0) {
                        mTextStatus.setText("Připravuji otevření dat");
                        Intent i = new Intent();
                        i.setAction(android.content.Intent.ACTION_VIEW);
                        i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                        i.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                        String filename = gpx_list_files.get(position);
                        String path = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MOVIES).toString() + "/" + filename;
                        i.setDataAndType(Uri.parse("file://" + path), "application/gpx+xml");
                        File file = new File(path);
                        if (file.exists()) {
                            startActivity(i);
                            mTextStatus.setText("Připraven");
                        } else {
                            mTextStatus.setText("Nepodařilo se stáhnout data. Připraven");
                        }
                    }
                    if (log == 1) {
                        mTextStatus.setText("V logu jsou méně než dvě pozice.");
                    }
                    if (log == 2) {
                        mTextStatus.setText("Problém s vytvořením logu.");
                    }
                }

                if (position == 1) {
                    filename = "server_last.gpx";
                    mTextStatus.setText("Stahuji data. Může chvíli trvat.");
                    new DownloadFileFromURL().execute("http://gisak.vsb.cz/patrac/mserver.php?operation=getgpx_last&searchid=" + MainActivity.searchid);
                }

                if (position == 2) {
                    filename = "server.gpx";
                    mTextStatus.setText("Stahuji data. Může chvíli trvat.");
                    new DownloadFileFromURL().execute("http://gisak.vsb.cz/patrac/mserver.php?operation=getgpx&searchid=" + MainActivity.searchid);
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
                        local_gpx.println("<trkpt lat=\"" + wp.lat + "\" lon=\"" + wp.lon + "\"><name>Toto zařízení</name><time>" + timeutc + "</time></trkpt>");
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

    /**
     * Background Async Task to download file
     * */
    class DownloadFileFromURL extends AsyncTask<String, String, String> {

        /**
         * Before starting background thread Show Progress Bar Dialog
         * */
        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            StrictMode.VmPolicy.Builder builder = new StrictMode.VmPolicy.Builder();
            StrictMode.setVmPolicy(builder.build());
        }

        /**
         * Downloading file in background thread
         * */
        @Override
        protected String doInBackground(String... f_url) {
            int count;
            String f_url_parts[] = f_url[0].split("/");
            //String filename = f_url[1];
            try {
                URL url = new URL(f_url[0]);
                URLConnection conection = url.openConnection();
                conection.connect();

                int lenghtOfFile = conection.getContentLength();

                InputStream input = new BufferedInputStream(url.openStream(),
                        8192);

                // Output stream
                String path = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MOVIES).toString();
                OutputStream output = new FileOutputStream(path + "/" + filename);

                byte data[] = new byte[1024];
                long total = 0;

                while ((count = input.read(data)) != -1) {
                    total += count;
                    // publishing the progress....
                    // After this onProgressUpdate will be called
                    publishProgress("" + (int) ((total * 100) / lenghtOfFile));
                    // writing data to file
                    output.write(data, 0, count);
                }

                // flushing output
                output.flush();

                // closing streams
                output.close();
                input.close();

            } catch (Exception e) {
                cancel(true);
                mTextStatus.setText("Chyba při stažení dat");
            }

            return null;
        }

        /**
         * Updating progress bar
         * */
        protected void onProgressUpdate(String... progress) {
            mTextStatus.setText("Staženo " + Arrays.toString(progress) + " %");
        }

        /**
         * After completing background task Dismiss the progress dialog
         * **/
        @Override
        protected void onPostExecute(String file_url) {
            mTextStatus.setText("Připravuji otevření dat");
            Intent i = new Intent();
            i.setAction(android.content.Intent.ACTION_VIEW);
            i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            i.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            String path = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MOVIES).toString() + "/" + filename;
            i.setDataAndType(Uri.parse("file://"+path), "application/gpx+xml");
            File file = new File(path);
            if(file.exists()) {
                startActivity(i);
                mTextStatus.setText("Připraven");
            } else {
                mTextStatus.setText("Nepodařilo se stáhnout data. Připraven");
            }
        }

    }

}
