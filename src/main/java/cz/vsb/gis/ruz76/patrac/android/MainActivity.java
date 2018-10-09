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
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.ActivityCompat;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;

import java.io.UnsupportedEncodingException;
import java.lang.reflect.Array;
import java.net.URLEncoder;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

/**
 * Main Activity.
 */
public class MainActivity extends Activity implements LocationListener, GetRequestUpdate {
    public static String StatusMessages = null;
    public static ArrayList<Waypoint> waypoints;
    public static RequestMode mode = RequestMode.SLEEPING;
    private TextView mDataText;
    private TextView mStatusText;
    private ListView messagesListView;
    private Intent callOnDuty;
    public static String sessionId = null;
    public static String searchid;
    public static String endPoint;
    public double lat = 0;
    public double lon = 0;
    public double latFromListener = 0;
    public double lonFromListener = 0;
    int positionCount = 0;
    int loggedPositionCount = 0;
    int sendPositionCount = 0;
    int messagesCount = 0;
    int errorsCount = 0;
    boolean connected = false;

    int lastLoggedPositionId = 0;

    Timer timerPosition;
    PositionTask myPositionTask;
    Timer timerMessage;
    MessageTask myMessageTask;
    Timer timerSearch;
    SearchTask mySearchTask;
    Timer timerCallOnDuty;
    CallOnDutyTask myCallOnDutyTask;
    SharedPreferences sharedPrefs;

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

    //Methods from Location Listener
    @Override
    public void onLocationChanged(Location location) throws SecurityException {
        Location locationGPS = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
        if (locationGPS != null) {
            latFromListener = locationGPS.getLatitude();
            lonFromListener = locationGPS.getLongitude();
        }
    }

    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {}

    @Override
    public void onProviderEnabled(String provider) {}

    @Override
    public void onProviderDisabled(String provider) {}

    /**
     * Creates instances from saved state.
     * Sets permissions or requests permissions.
     * Sets gui based on layout activity_main.
     * @param savedInstanceState
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) throws SecurityException {
        super.onCreate(savedInstanceState);
        setPermissions();
        setContentView(R.layout.activity_main);
        setSearchTimer();
        context = this.getApplicationContext();
        sharedPrefs = PreferenceManager.getDefaultSharedPreferences(this);
        endPoint = sharedPrefs.getString("endpoint", getString(R.string.pref_default_endpoint));
        locationManager = (LocationManager) context.getSystemService(LOCATION_SERVICE);
        locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, this);
    }

    /**
     * Ads items to the menu.
     * @param menu
     * @return
     */
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    /**
     * Invoked on menu click.
     * @param item
     * @return
     */
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.connect_disconnect_action:
                if (!connected) {
                    connect();
                    item.setTitle(getString(R.string.disconnect));
                } else {
                    disconnect();
                    item.setTitle(getString(R.string.connect));
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

    /**
     * Sets or requests permissions for the application.
     */
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

    /**
     * Resets items to empty or zero values.
     */
    private void resetItems() {

        lat = 0;
        lon = 0;
        positionCount = 0;
        loggedPositionCount = 0;
        sendPositionCount = 0;
        messagesCount = 0;
        errorsCount = 0;

        mode = RequestMode.TRACKING;

        sessionId = null;

        mDataText = (TextView) findViewById(R.id.data_text);
        mStatusText = (TextView) findViewById(R.id.status_text);
        messagesListView = (ListView) findViewById(R.id.messagesListView);

        waypoints = new ArrayList<Waypoint>();
        searchid = sharedPrefs.getString("searchid", getString(R.string.pref_default_searchid));

        String[] messages = new String[]{getString(R.string.messages_list_title)};
        messages_list = new ArrayList<String>(Arrays.asList(messages));

        MessageFile mf = new MessageFile(getString(R.string.messages_list_title), getString(R.string.messages_no_attachment));
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

    /**
     * It is triggered when the download of the data from Download is finished.
     * @param result response from the server
     */
    public void processResponse(String result) {
        switch (mode) {
            case SLEEPING:
                processSearchesResponse(result);
                break;
            case WAITING:
                processCallOnDutyResponse(result);
                break;
            case SELECTED:
                processSelectedResponse(result);
                break;
            case TRACKING:
                processTrackingResponse(result);
                break;
            default:
                // processSearchesResponse(result);
                // do nothing
        }

    }

    /**
     * Reads results from server in mode tracking.
     * There can be three four.
     * ID, M, P and null.
     * ID - we have obtained session identifier from the server.
     * M - we have obtained message from server.
     * P - we have obtained information that position was saved.
     * null - some error occured
     *
     * @param result result to process
     */
    private void processTrackingResponse(String result) {
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
            //The response is empty
            errorsCount++;
            DateFormat dateFormat = new SimpleDateFormat("HH:mm:ss");
            Date date = new Date();
            mStatusText.setText(getString(R.string.connection_error) + " v " + dateFormat.format(date) + ". Celkem chyb: " + errorsCount);
            mDownloading = false;
            setInfo();
        }
    }

    /**
     * Reads response in sleeping mode.
     * May contain have four states: *, #, empty, null
     * * - there is at least one active search on the server
     * # - the position of device was stored to the database, so we can switch to waiting mode
     * empty - nothing to do
     * null - some error occurred - nothing to do
     *
     * @param result result to process
     */
    private void processSearchesResponse(String result) {
        if (result != null) {
            if (!result.isEmpty() && RequestMode.SLEEPING == mode) {
                if (result.startsWith("*")) {
                    // there is a new search
                    String id = sharedPrefs.getString("id", null);
                    if (id != null) {
                        sendGetRequest(endPoint + "operation=searches&id=" + id + "&lat=" + getShortCoord(latFromListener) + "&lon=" + getShortCoord(lonFromListener));
                    }
                }
                if (result.startsWith("#")) {
                    // the coordinates where saved at the server
                    mode = RequestMode.WAITING;
                    setCallOnDutyTimer();
                }
            }
        }
    }

    /**
     * Reads response in waiting mode.
     * May contain three states: list, empty, null
     * list - list of active searches on the server
     * empty - no searches on call
     * null - some error occurred - nothing to do
     *
     * @param result result to process
     */
    private void processCallOnDutyResponse(String result) {
        if (result != null) {
            if (!result.isEmpty() && RequestMode.WAITING == mode) {
                // reads lists of searches
                String[] searches = result.split("\n");
                // TODO do it better
                if (callOnDuty == null) {
                    playRing();
                    callOnDuty = new Intent(MainActivity.this, CallOnDutyActivity.class);
                    callOnDuty.putExtra("searches", searches);
                    startActivity(callOnDuty);
                }
            }
        }
    }

    /**
     * Reads response in selected mode. User has selected the search.
     * May contain two states: some data, null
     * some data - server knows that you are ready
     * null - some error occurred - nothing to do TODO - do something, otherwise the call is not connected
     *
     * @param result result to process
     */
    private void processSelectedResponse(String result) {
        if (result != null) {
            if (!result.isEmpty() && RequestMode.SELECTED == mode) {
                // the searchid was saved at the server
                SharedPreferences.Editor editor = sharedPrefs.edit();
                editor.putString("searchid", searchid);
                editor.commit();
                mode = RequestMode.TRACKING;
            }
        }
    }

    /**
     * Timer for sleeping and waiting mode.
     */
    private void setSearchTimer() {
        if (timerSearch != null) {
            timerSearch.cancel();
        }

        timerSearch = new Timer();
        mySearchTask = new SearchTask();
        timerSearch.schedule(mySearchTask, 30000, 1000 * 60 * 1);
    }

    /**
     * Timer for call on duty state.
     */
    private void setCallOnDutyTimer() {
        if (timerCallOnDuty != null) {
            timerCallOnDuty.cancel();
        }

        timerCallOnDuty = new Timer();
        myCallOnDutyTask = new CallOnDutyTask();
        timerCallOnDuty.schedule(myCallOnDutyTask, 0, 1000 * 5);
    }

    /**
     * Initialize the connection.
     */
    private void connect() {

        resetItems();

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

    /**
     * Stops timers.
     */
    private void disconnect() {
        connected = false;
        if (timerPosition != null) {
            timerPosition.cancel();
        }
        if (timerMessage != null) {
            timerMessage.cancel();
        }
    }

    /**
     * Requests for session id.
     */
    private void getSessionId() {
        //Maybe put phone number instead of NN and random
        String user_name = sharedPrefs.getString("user_name", "NN " + Math.round(Math.random() * 10000));
        try {
            if (!mDownloading) {
                sendGetRequest(endPoint + "operation=getid&searchid=" + searchid + "&user_name=" + URLEncoder.encode(user_name, "UTF-8") + "&lat=" + getShortCoord(lat) + "&lon=" + getShortCoord(lon));
                mDownloading = true;
            }
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
    }

    /**
     * Reads location from system and compares it to the previous position.
     * @return true if the position should be logged
     * @throws SecurityException
     */
    private boolean trackLocation() throws SecurityException {
        double newlat = 0;
        double newlon = 0;
        boolean logit = false;
        Location locationGPS = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
        if (locationGPS == null) {
            // we do not have location yet, so wait for location to the next interval
            return false;
        }
        newlat = locationGPS.getLatitude();
        newlon = locationGPS.getLongitude();
        positionCount++;
        //TODO do it better
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

    /**
     * Sends current location or cached 100 locations to the server.
     */
    private void sendTrack() {
        if ((sendPositionCount == (loggedPositionCount - 1))) {
            sendGetRequest(endPoint + "operation=sendlocation&searchid=" + searchid + "&id=" + sessionId + "&lat=" + getShortCoord(lat) + "&lon=" + getShortCoord(lon));
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
            if (notSendedCoords.length() > 10) {
                notSendedCoords = notSendedCoords.substring(0, notSendedCoords.length() - 2);
                sendGetRequest(endPoint + "operation=sendlocations&searchid=" + searchid + "&id=" + sessionId + "&coords=" + notSendedCoords);
            }
        }
        mDownloading = true;
    }

    /**
     * Shows the information that the posision is the same.
     */
    private void showInfoSamePosition() {
        setInfo();
        String content = String.valueOf(mDataText.getText());
        DateFormat dateFormat = new SimpleDateFormat("HH:mm:ss");
        Date date = new Date();
        content += " " + getString(R.string.same_position_label) + ": " + dateFormat.format(date);
        mDataText.setText(content);
    }

    /**
     * Sends coordinates to the server.
     * @throws SecurityException
     */
    private void startSync() throws SecurityException {
        boolean logit = trackLocation();
        mDownloading = false;
        if (MainActivity.StatusMessages != null) mStatusText.setText(MainActivity.StatusMessages);
        if (!mDownloading) {
            // First we have to obtain the sessionId.
            if (sessionId == null) {
                getSessionId();
            } else {
                if (logit) {
                    if (!mDownloading) {
                        sendTrack();
                    }
                } else {
                    showInfoSamePosition();
                }
            }
        }
    }

    /**
     * Flat the coordinate to has just 6 decimal points.
     * @param coord coordinate to flat
     * @return flatted coordinate
     */
    private String getShortCoord(double coord) {
        String coordLong = Double.toString(coord);
        String parts[] = coordLong.split("\\.");
        if (parts[1].length() > 6) {
            return parts[0] + "." + parts[1].substring(0, 6);
        } else {
            return parts[0] + "." + parts[1];
        }
    }

    /**
     * Starts downloading a message.
     */
    private void startDownloadMessages() {
        if (!mDownloading) {
            // we do not have session id yet, so ask for it
            if (sessionId == null) {
                sendGetRequest(endPoint + "operation=getid&searchid=" + searchid);
                mDownloading = true;
            } else {
                boolean messages_switch = sharedPrefs.getBoolean("messages_switch", true);
                if (messages_switch) {
                    sendGetRequest(endPoint + "operation=getmessages&searchid=" + searchid + "&id=" + sessionId);
                    mDownloading = true;
                }
            }
        }
    }

    /**
     * Checks new search.
     */
    private void checkSearches() {
        String id = sharedPrefs.getString("id", null);
        if (id != null && RequestMode.SLEEPING == mode) {
            sendGetRequest(endPoint + "operation=searches&id=" + id);
        }
    }

    /**
     * Check call on duty request.
     */
    public void checkCallOnDuty() {
        String id = sharedPrefs.getString("id", null);
        if (id != null && RequestMode.WAITING == mode) {
            sendGetRequest(endPoint + "operation=searches&id=" + id);
        }
        if (id != null && RequestMode.SELECTED == mode) {
            sendGetRequest(endPoint + "operation=searches&id=" + id + "&searchid=" + searchid);
        }
    }

    /**
     * Shows the information in the status bar.
     */
    private void setInfo() {
        String content = getString(R.string.sessionid_label) + ": "
                + sessionId + " "
                + getString(R.string.searchid_label)
                + ": " + searchid + "\n";
        content += getString(R.string.positions_label)
                + ": " + positionCount
                + "/" + loggedPositionCount
                + "/" + sendPositionCount + "\n";
        content += getString(R.string.longitude_label) + ": "
                + (double) Math.round(lon * 100000d) / 100000d
                + " " + getString(R.string.latitude_label) + ": "
                + (double) Math.round(lat * 100000d) / 100000d + "\n";
        mDataText.setText(content);
    }

    /**
     * Process the new message. If there is an attachment it is downloaded.
     * @param result reponse from the server
     */
    private void processMessage(String result) {
        //New mesage is on the way
        messagesCount++;
        String items[] = result.split(";");
        MessageFile mf = new MessageFile(getString(R.string.message_when) + ": " + items[4] + "\n" + getString(R.string.message) + ": " + items[2], items[3]);
        messages_list_full.add(0, mf);
        if (items[3].length() > 1) {
            messages_list.add(0, items[4].substring(0, items[4].length() - 3).split(" ")[1] + ": " + items[2] + " (@)");
            //Shared file
            String shared = items[5].replace("\n", "");
            if (shared.equalsIgnoreCase("1")) {
                downloadFromUrl(endPoint + "operation=getfile&searchid=" + searchid + "&id=shared&filename=" + items[3], items[3]);
            } else {
                //Individual file
                downloadFromUrl(endPoint + "operation=getfile&searchid=" + searchid + "&id=" + sessionId + "&filename=" + items[3], items[3]);
            }
        } else {
            messages_list.add(0, items[4].substring(0, items[4].length() - 3).split(" ")[1] + ": " + items[2]);
        }
        new AdapterHelper().update((ArrayAdapter) arrayAdapter, new ArrayList<Object>(messages_list));
        arrayAdapter.notifyDataSetChanged();
        playRing();
    }

    /**
     * Plays the ring tone.
     */
    private void playRing() {
        try {
            Uri notification = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
            Ringtone r = RingtoneManager.getRingtone(getApplicationContext(), notification);
            r.play();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private boolean startActivity(Context packageContext, Class<?> appToStart) {
        Intent appToStartIntent = new Intent(packageContext, appToStart);
        startActivity(appToStartIntent);
        return true;
    }

    private void downloadFromUrl(String url, String file) {
        DownloadFileFromUrl downloadFileFromUrl = new DownloadFileFromUrl();
        downloadFileFromUrl.execute(url, file);
    }

    private void sendGetRequest(String url) {
        ServerGetRequest serverGetRequest = new ServerGetRequest();
        serverGetRequest.setActivity(this);
        serverGetRequest.execute(url);
    }

    /**
     * Timer task for synchronization of positions.
     */
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

    /**
     * Timer task for synchronization of messages.
     */
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
     * Timer task for synchronization of messages.
     */
    class SearchTask extends TimerTask {

        @Override
        public void run() {
            runOnUiThread(new Runnable() {

                @Override
                public void run() {
                    checkSearches();
                }
            });
        }

    }

    /**
     * Timer task for synchronization of messages.
     */
    class CallOnDutyTask extends TimerTask {

        @Override
        public void run() {
            runOnUiThread(new Runnable() {

                @Override
                public void run() {
                    checkCallOnDuty();
                }
            });
        }

    }

}

