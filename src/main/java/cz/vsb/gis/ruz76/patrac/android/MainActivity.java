/*
 * Based on The Android Open Source Project
 * com.example.android.networkconnect
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package cz.vsb.gis.ruz76.patrac.android;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.support.annotation.RequiresApi;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.io.UnsupportedEncodingException;
import java.math.RoundingMode;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.text.DateFormat;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;
import java.util.Timer;
import java.util.TimerTask;

/**
 * Sample Activity demonstrating how to connect to the network and fetch raw
 * HTML. It uses a Fragment that encapsulates the network operations on an AsyncTask.
 * <p>
 * This sample uses a TextView to display output.
 */
public class MainActivity extends FragmentActivity implements DownloadCallback, LocationListener {

    // Reference to the TextView showing fetched data, so we can clear it with a button
    // as necessary.
    public static String MM = null;
    public static ArrayList<Waypoint> waypoints;
    private TextView mDataText;
    private TextView mStatusText;
    private ListView messagesListView;
    String testX = "";
    public static String sessionId = null;
    //Zatim hardcoded - později z nastaveni
    public static String searchid = "AAA111BBB";
    double lat = 0;
    double lon = 0;
    int positionCount = 0;
    int loggedPositionCount = 0;
    int sendPositionCount = 0;
    int messagesCount = 0;
    int errorsCount = 0;
    boolean connected = false;

    int lastLoggedPositionId = 0;
    int lastSendedPositionId = 0;
    //PrintStream local_gpx_fragment = null;

    Timer timerPosition;
    PositionTask myPositionTask;
    Timer timerMessage;
    MessageTask myMessageTask;
    SharedPreferences sharedPrefs;

    // Keep a reference to the NetworkFragment which owns the AsyncTask object
    // that is used to execute network ops.
    private NetworkFragment mNetworkFragment;

    // Boolean telling us whether a download is in progress, so we don't trigger overlapping
    // downloads with consecutive button clicks.
    private boolean mDownloading = false;

    protected Context context;

    // Create a List from String Array elements
    private List<String> messages_list;
    private List<MessageFile> messages_list_full;

    // Create an ArrayAdapter from List
    private ArrayAdapter<String> arrayAdapter;

    LocationManager locationManager;

    private void setPermissions() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 0);
        }
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, 0);
        }
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, 0);
        }
    }

    private void setItems() {
        mDataText = (TextView) findViewById(R.id.data_text);
        mStatusText = (TextView) findViewById(R.id.status_text);
        messagesListView = (ListView) findViewById(R.id.messagesListView);
        mNetworkFragment = NetworkFragment.getInstance(getSupportFragmentManager(), "http://gisak.vsb.cz/patrac/mserver.php?operation=getid");
        context = this.getApplicationContext();

        waypoints = new ArrayList<Waypoint>();

        sharedPrefs = PreferenceManager.getDefaultSharedPreferences(this);
        searchid = sharedPrefs.getString("searchid", "AAA111BBB");

        String[] messages = new String[]{"Seznam zpráv"};
        messages_list = new ArrayList<String>(Arrays.asList(messages));

        MessageFile mf = new MessageFile("Seznam zpráv", "Bez přílohy");
        messages_list_full = new ArrayList<MessageFile>();
        messages_list_full.add(mf);

        arrayAdapter = new ArrayAdapter<String>
                (this, android.R.layout.simple_list_item_1, messages_list);

        // DataBind ListView with items from ArrayAdapter
        messagesListView.setAdapter(arrayAdapter);

        messagesListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Intent appInfo = new Intent(MainActivity.this, MessageViewActivity.class);
                appInfo.putExtra("message", messages_list_full.get(position).message);
                appInfo.putExtra("filename", messages_list_full.get(position).filename);
                startActivity(appInfo);
            }

        });
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setPermissions();
        setContentView(R.layout.sample_main);
        setItems();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    private void connect() {
        //testX = startDownload();
        if (timerPosition != null) {
            timerPosition.cancel();
        }
        timerPosition = new Timer();
        myPositionTask = new PositionTask();
        String sync_frequency_gps_string = sharedPrefs.getString("sync_frequency_gps", "5");
        int sync_frequency_gps = Integer.parseInt(sync_frequency_gps_string) * 1000;
        timerPosition.schedule(myPositionTask, 1000, sync_frequency_gps);

        if (timerMessage != null) {
            timerMessage.cancel();
        }

        timerMessage = new Timer();
        myMessageTask = new MessageTask();
        String sync_frequency_string = sharedPrefs.getString("sync_frequency", "30");
        int sync_frequency = Integer.parseInt(sync_frequency_string) * 1000;
        timerMessage.schedule(myMessageTask, 30000, sync_frequency);
        connected = true;
    }

    private boolean startActivity(Context packageContext, Class<?> appToStart) {
        Intent appToStartIntent = new Intent(packageContext, appToStart);
        startActivity(appToStartIntent);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.fetch_action:
                if (!connected) {
                    connect();
                    item.setTitle("Odpojit");
                } else {
                    if (timerMessage != null) {
                        timerMessage.cancel();
                    }
                    item.setTitle("Připojit");
                    connected = false;
                }
                return true;
            case R.id.clear_action:
                return startActivity(MainActivity.this, SettingsActivity.class);

            case R.id.map_action:
                return startActivity(MainActivity.this, MapsActivity.class);

            case R.id.send_message_action:
                return startActivity(MainActivity.this, MessageSend.class);

        }
        return false;
    }

    private boolean trackLocation() throws SecurityException {
        double newlat = 0;
        double newlon = 0;
        boolean logit = false;
        locationManager = (LocationManager) context.getSystemService(LOCATION_SERVICE);
        locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, this);
        Location locationGPS = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
        newlat = locationGPS.getLatitude();
        newlon = locationGPS.getLongitude();
        positionCount++;
        if (Math.hypot(lat - newlat, lon - newlon) >= 0.0001) { //0.0001
            lat = newlat;
            lon = newlon;
            logit = true;
            Waypoint wp = new Waypoint(newlat, newlon, new Date());
            waypoints.add(wp);
            loggedPositionCount++;
            lastLoggedPositionId++;
        }
        setInfo();
        return logit;
    }

    private void getSessionId() {
        //Maybe put phone number instead of NN and random
        String user_name = sharedPrefs.getString("user_name", "NN " + Math.round(Math.random() * 10000));
        try {
            if (!mDownloading && mNetworkFragment != null) {
                mNetworkFragment.startDownload("http://gisak.vsb.cz/patrac/mserver.php?operation=getid&searchid=" + searchid + "&user_name=" + URLEncoder.encode(user_name, "UTF-8") + "&lat=" + getShortCoord(lat) + "&lon=" + getShortCoord(lon));
                mDownloading = true;
            }
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
    }

    private void sendTrack() {
        if ((sendPositionCount == (loggedPositionCount - 1))) {
            mNetworkFragment.startDownload("http://gisak.vsb.cz/patrac/mserver.php?operation=sendlocation&searchid=" + searchid + "&id=" + sessionId + "&lat=" + getShortCoord(lat) + "&lon=" + getShortCoord(lon));
        } else {
            //Some time out of network. Must send more coords from memory.
            //TODO change to POST to be able send more than 100 coords
            String notSendedCoords = "";
            int startPosition = sendPositionCount + 1;
            //When is more than 100 not sent positions in memory then only last 100 is sent to server
            if ((MainActivity.waypoints.size() - startPosition) > 100) {
                startPosition = MainActivity.waypoints.size() - 100;
            }
            for (int i = startPosition; i < MainActivity.waypoints.size(); i++) {
                Waypoint wp = MainActivity.waypoints.get(i);
                notSendedCoords += getShortCoord(wp.getLon()) + ";" + getShortCoord(wp.getLat()) + ",";
            }
            notSendedCoords = notSendedCoords.substring(0, notSendedCoords.length() - 2);
            mNetworkFragment.startDownload("http://gisak.vsb.cz/patrac/mserver.php?operation=sendlocations&searchid=" + searchid + "&id=" + sessionId + "&coords=" + notSendedCoords);
        }
        mDownloading = true;
    }

    private void showInfoSamePosition() {
        setInfo();
        String content = String.valueOf(mDataText.getText());
        DateFormat dateFormat = new SimpleDateFormat("HH:mm:ss");
        Date date = new Date();
        content += " Pozice je stejná v čase: " + dateFormat.format(date);
        mDataText.setText(content);
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    private void startSync() throws SecurityException {
        boolean logit = trackLocation();
        mDownloading = false;
        if (MainActivity.MM != null) mStatusText.setText(MainActivity.MM);
        if (!mDownloading && mNetworkFragment != null) {
            // Execute the async download.
            if (sessionId == null) {
                getSessionId();
            } else {
                if (logit) {
                    if (!mDownloading && mNetworkFragment != null) {
                        sendTrack();
                    }
                } else {
                    showInfoSamePosition();
                }
            }
        }
    }

    private String getShortCoord(double coord) {
        String coordLong = Double.toString(coord);
        String parts[] = coordLong.split("\\.");
        if (parts[1].length() > 6) {
            return parts[0] + "." + parts[1].substring(0, 6);
        } else {
            return parts[0] + "." + parts[1];
        }
    }

    private String startDownloadMessages() {
        String test = "";
        if (!mDownloading && mNetworkFragment != null) {
            // Execute the async download.
            if (sessionId == null) {
                //TODO ověřit zda tato situace může nastat
                mNetworkFragment.startDownload("http://gisak.vsb.cz/patrac/mserver.php?operation=getid&searchid=" + searchid);
                mDownloading = true;
            } else {
                boolean messages_switch = sharedPrefs.getBoolean("messages_switch", true);
                if (messages_switch) {
                    mNetworkFragment.startDownload("http://gisak.vsb.cz/patrac/mserver.php?operation=getmessages&searchid=" + searchid + "&id=" + sessionId);
                    mDownloading = true;
                }
            }
        }

        return test;
    }

    private void setInfo() {
        String content = "SessionId: " + sessionId + " SearchId: " + searchid + "\n";
        content += "Pozice získané/logované/odeslané: " + positionCount + "/" + loggedPositionCount + "/" + sendPositionCount + "\n";
        content += "Longitude: " + (double) Math.round(lon * 100000d) / 100000d + " Latitude: " + (double) Math.round(lat * 100000d) / 100000d + "\n";
        mDataText.setText(content);
    }

    private void processMessage(String result) {
        //New mesage is on the way
        messagesCount++;
        String items[] = result.split(";");
        MessageFile mf = new MessageFile("Kdy: " + items[4] + "\nZpráva: " + items[2], items[3]);
        messages_list_full.add(0, mf);
        if (items[3].length() > 1) {
            messages_list.add(0, items[4].substring(0, items[4].length() - 3).split(" ")[1] + ": " + items[2] + " (@)");
            //Shared file
            String shared = items[5].replace("\n", "");
            if (shared.equalsIgnoreCase("1")) {
                new DownloadFileFromURL().execute("http://gisak.vsb.cz/patrac/mserver.php?operation=getfile&searchid=" + searchid + "&id=shared&filename=" + items[3], items[3]);
                //Individual file
            } else {
                new DownloadFileFromURL().execute("http://gisak.vsb.cz/patrac/mserver.php?operation=getfile&searchid=" + searchid + "&id=" + sessionId + "&filename=" + items[3], items[3]);
            }
        } else {
            messages_list.add(0, items[4].substring(0, items[4].length() - 3).split(" ")[1] + ": " + items[2]);
        }
        new AdapterHelper().update((ArrayAdapter) arrayAdapter, new ArrayList<Object>(messages_list));
        arrayAdapter.notifyDataSetChanged();
        try {
            Uri notification = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
            Ringtone r = RingtoneManager.getRingtone(getApplicationContext(), notification);
            r.play();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void updateFromDownload(String result) {
        if (result != null) {
            if (result.startsWith("ID:")) {
                sendPositionCount++;
                sessionId = result.substring(3);
            } else if (result.startsWith("M")) {
                processMessage(result);
            } else if (result.startsWith("P")) {
                sendPositionCount = loggedPositionCount;
                setInfo();
            }
        } else {
            errorsCount++;
            DateFormat dateFormat = new SimpleDateFormat("HH:mm:ss");
            Date date = new Date();
            mStatusText.setText(getString(R.string.connection_error) + " v " + dateFormat.format(date) + ". Celkem chyb: " + errorsCount);
            mDownloading = false;
            setInfo();
        }
    }

    @Override
    public NetworkInfo getActiveNetworkInfo() {
        ConnectivityManager connectivityManager =
                (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = connectivityManager.getActiveNetworkInfo();
        return networkInfo;
    }

    @Override
    public void finishDownloading() {
        mDownloading = false;
        if (mNetworkFragment != null) {
            mNetworkFragment.cancelDownload();
        }
        //mDataText.setText(mDataText.getText() + "\n******************" + testX);
    }

    @Override
    public void onProgressUpdate(int progressCode, int percentComplete) {
        switch (progressCode) {
            // You can add UI behavior for progress updates here.
            case Progress.ERROR:
                break;
            case Progress.CONNECT_SUCCESS:
                break;
            case Progress.GET_INPUT_STREAM_SUCCESS:
                break;
            case Progress.PROCESS_INPUT_STREAM_IN_PROGRESS:
                //mDataText.setText("" + percentComplete + "%");
                break;
            case Progress.PROCESS_INPUT_STREAM_SUCCESS:
                break;
        }
    }

    @Override
    public void onLocationChanged(Location location) {

    }

    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {

    }

    @Override
    public void onProviderEnabled(String provider) {

    }

    @Override
    public void onProviderDisabled(String provider) {

    }

    class PositionTask extends TimerTask {

        @Override
        public void run() {

            runOnUiThread(new Runnable() {

                @Override
                public void run() {
                    startSync();
                }
            });
        }

    }

    class MessageTask extends TimerTask {

        @Override
        public void run() {

            runOnUiThread(new Runnable() {

                @Override
                public void run() {
                    startDownloadMessages();
                }
            });
        }

    }

    /**
     * Background Async Task to download file
     */
    class DownloadFileFromURL extends AsyncTask<String, String, String> {

        /**
         * Before starting background thread Show Progress Bar Dialog
         */
        @Override
        protected void onPreExecute() {
            super.onPreExecute();
        }

        /**
         * Downloading file in background thread
         */
        @Override
        protected String doInBackground(String... f_url) {
            int count;
            String f_url_parts[] = f_url[0].split("/");
            String filename = f_url[1];
            try {
                URL url = new URL(f_url[0]);
                URLConnection conection = url.openConnection();
                conection.connect();
                int lenghtOfFile = conection.getContentLength();
                InputStream input = new BufferedInputStream(url.openStream(),
                        8192);
                String path = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MOVIES).toString();
                OutputStream output = new FileOutputStream(path + "/" + filename);
                byte data[] = new byte[1024];
                long total = 0;
                while ((count = input.read(data)) != -1) {
                    total += count;
                    publishProgress("" + (int) ((total * 100) / lenghtOfFile));
                    output.write(data, 0, count);
                }
                output.flush();
                output.close();
                input.close();
            } catch (Exception e) {
                MainActivity.MM = e.getMessage();
                Log.e("Error: ", e.getMessage());
            }
            return null;
        }

        /**
         * Updating progress bar
         */
        protected void onProgressUpdate(String... progress) {

        }

        /**
         * After completing background task Dismiss the progress dialog
         **/
        @Override
        protected void onPostExecute(String file_url) {


        }

    }

}

