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
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.preference.PreferenceManager;
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
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

/**
 * Sample Activity demonstrating how to connect to the network and fetch raw
 * HTML. It uses a Fragment that encapsulates the network operations on an AsyncTask.
 *
 * This sample uses a TextView to display output.
 */
public class MainActivity extends FragmentActivity implements DownloadCallback, LocationListener {

    // Reference to the TextView showing fetched data, so we can clear it with a button
    // as necessary.
    private TextView mDataText;
    private ListView messagesListView;
    String testX = "";
    String sessionId = null;
    String searchid = "AAA111BBB";
    double lat = 0;
    double lon = 0;
    int positionCount = 0;
    int messagesCount = 0;

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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.sample_main);
        mDataText = (TextView) findViewById(R.id.data_text);
        messagesListView = (ListView) findViewById(R.id.messagesListView);
        mNetworkFragment = NetworkFragment.getInstance(getSupportFragmentManager(), "http://158.196.143.122/patrac/mserver.php?operation=getid");
        context = this.getApplicationContext();

        sharedPrefs = PreferenceManager.getDefaultSharedPreferences(this);
        //sharedPrefs = this.getSharedPreferences("pref_general", Context.MODE_PRIVATE);

        String[] messages = new String[] { "Seznam zpráv" };
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
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            // When the user clicks FETCH, fetch the first 500 characters of
            // raw HTML from www.google.com.
            case R.id.fetch_action:
                //testX = startDownload();
                if(timerPosition != null){
                    timerPosition.cancel();
                }

                timerPosition = new Timer();
                myPositionTask = new PositionTask();
                timerPosition.schedule(myPositionTask, 1000, 5000);

                if(timerMessage != null){
                    timerMessage.cancel();
                }

                timerMessage = new Timer();
                myMessageTask = new MessageTask();
                timerMessage.schedule(myMessageTask, 30000, 30000);

                return true;
            // Clear the text and cancel download.
            case R.id.clear_action:
                //finishDownloading();
                //mDataText.setText("");
                Intent appInfo = new Intent(MainActivity.this, SettingsActivity.class);
                startActivity(appInfo);
                return true;
        }
        return false;
    }

    private String startDownload() {
        String test = "";
        double newlat = 0;
        double newlon = 0;
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            test = "XX";
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 0);
        } else {
            test = "ZZ";
        }
        LocationManager locationManager;
        try {
            locationManager = (LocationManager) context.getSystemService(LOCATION_SERVICE);
            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, this);
            for (int i =0; i < locationManager.getProviders(true).size(); i++) {
                test = test + "\n++++++++++ " + locationManager.getProviders(true).get(i);
            }
            Location locationGPS = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
            //Location locationGPS = locationManager.getLastKnownLocation("gps");
            test = test + "\n******************* " + locationGPS.getLatitude();
            newlat = locationGPS.getLatitude();
            newlon = locationGPS.getLongitude();

        } catch (Exception e) {
          test = test + "\n" + e.getMessage();
        }
        //
        //mDataText.setText("AAA" + locationGPS.getLatitude());
        if (!mDownloading && mNetworkFragment != null) {
            // Execute the async download.
            if (sessionId == null) {
                String user_name = sharedPrefs.getString("user_name", "NN");
                try {
                    mNetworkFragment.startDownload("http://158.196.143.122/patrac/mserver.php?operation=getid&searchid=" + searchid + "&user_name=" + URLEncoder.encode(user_name, "UTF-8"));
                    mDownloading = true;
                } catch (UnsupportedEncodingException e) {
                    e.printStackTrace();
                }
            } else {
                if (Math.hypot(lat - newlat, lon - newlon) > 0.0001) {
                    lat = newlat;
                    lon = newlon;
                    mNetworkFragment.startDownload("http://158.196.143.122/patrac/mserver.php?operation=sendlocation&searchid=" + searchid + "&id=" + sessionId + "&lat=" + lat + "&lon=" + lon);
                    mDownloading = true;
                }

            }
        }


        return test;
    }

    private String startDownloadMessages() {
        String test = "";
        if (!mDownloading && mNetworkFragment != null) {
            // Execute the async download.
            if (sessionId == null) {
                //TODO ověřit zda tato situace může nastat
                mNetworkFragment.startDownload("http://158.196.143.122/patrac/mserver.php?operation=getid");
                mDownloading = true;
            } else {
                mNetworkFragment.startDownload("http://158.196.143.122/patrac/mserver.php?operation=getmessages&searchid=" + searchid + "&id=" + sessionId);
                mDownloading = true;
            }
        }


        return test;
    }

    @Override
    public void updateFromDownload(String result) {
        if (result != null) {
            if (result.startsWith("ID:")) {
                sessionId = result.substring(3);
                mDataText.setText(mDataText.getText() + "SessionID: " + sessionId + "\n");
                mDataText.setText(mDataText.getText() + "Počet odeslaných pozic: " + positionCount + "\n");
                //mDataText.setText(mDataText.getText() + "AAA: " + result.substring(4) + "\n");
                //mDataText.setText(mDataText.getText() + "ZZZ: " + result + "\n");
            }  else if (result.startsWith("M")) {
                messagesCount++;
                String items[] = result.split(";");
                //messages_list.add("Loquat");
                //mDataText.setText(mDataText.getText() + items[4].substring(0, items[4].length() - 1) + " Zpráva: " + items[2] + "\n");
                //mDataText.setText(mDataText.getText() + items[4].substring(0, items[4].length() - 1) + " Soubor: " + items[3] + "\n");
                MessageFile mf = new MessageFile("Kdy: " + items[4].substring(0, items[4].length() - 1) + "\nZpráva: " + items[2], items[3]);
                messages_list_full.add(mf);
                messages_list.add(items[4].substring(0, items[4].length() - 1) + ": " + items[2] + " (@)");
                //messages_list.add(items[4].substring(0, items[4].length() - 1) + " Soubor: " + items[3]);
                arrayAdapter.notifyDataSetChanged();
                new DownloadFileFromURL().execute("http://158.196.143.122/patrac/mserver.php?operation=getfile&searchid=" + searchid + "&id=" + sessionId + "&filename=" + items[3]);
            } else {
                String replaceFrom = "Počet odeslaných pozic: " + positionCount;
                positionCount++;
                String replaceTo = "Počet odeslaných pozic: " + positionCount;
                String content = String.valueOf(mDataText.getText());
                //messagesListView.addView(new ViewView());
                content = content.replaceAll(replaceFrom, replaceTo);
                mDataText.setText(content);
            }
        } else {
            mDataText.setText(getString(R.string.connection_error));
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
        switch(progressCode) {
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

            runOnUiThread(new Runnable(){

                @Override
                public void run() {
                    startDownload();
                }});
        }

    }

    class MessageTask extends TimerTask {

        @Override
        public void run() {

            runOnUiThread(new Runnable(){

                @Override
                public void run() {
                    startDownloadMessages();
                }});
        }

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
        }

        /**
         * Downloading file in background thread
         * */
        @Override
        protected String doInBackground(String... f_url) {
            int count;
            try {
                URL url = new URL(f_url[0]);
                URLConnection conection = url.openConnection();
                conection.connect();

                // this will be useful so that you can show a tipical 0-100%
                // progress bar
                int lenghtOfFile = conection.getContentLength();

                // download the file
                InputStream input = new BufferedInputStream(url.openStream(),
                        8192);

                // Output stream
                String f_url_parts[] = f_url[0].split("/");
                String filename = f_url_parts[f_url_parts.length - 1];
                OutputStream output = new FileOutputStream(Environment
                        .getExternalStorageDirectory().toString()
                        + "/" + filename);

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
                //final File file = new File(Environment.getExternalStorageDirectory().getAbsolutePath(), filename);
                //long x = file.getTotalSpace();

            } catch (Exception e) {
                Log.e("Error: ", e.getMessage());
            }

            return null;
        }

        /**
         * Updating progress bar
         * */
        protected void onProgressUpdate(String... progress) {

        }

        /**
         * After completing background task Dismiss the progress dialog
         * **/
        @Override
        protected void onPostExecute(String file_url) {


        }

    }

}

