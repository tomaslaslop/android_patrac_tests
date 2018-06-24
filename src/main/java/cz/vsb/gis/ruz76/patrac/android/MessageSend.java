package cz.vsb.gis.ruz76.patrac.android;

import android.app.ActionBar;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.app.Activity;
import android.os.Environment;
import android.os.StrictMode;
import android.support.v4.app.FragmentActivity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

public class MessageSend extends FragmentActivity {

    private List<String> messages_list;
    private List<MessageFile> messages_list_full;
    // Create an ArrayAdapter from List
    private ArrayAdapter<String> arrayAdapter;
    private ListView messagesListView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        StrictMode.VmPolicy.Builder builder = new StrictMode.VmPolicy.Builder();
        StrictMode.setVmPolicy(builder.build());
        setContentView(R.layout.activity_message_send);
        String[] messages = new String[] { "Seznam odeslaných zpráv" };
        messages_list = new ArrayList<String>(Arrays.asList(messages));

        MessageFile mf = new MessageFile("Seznam odeslaných zpráv", null);
        messages_list_full = new ArrayList<MessageFile>();
        messages_list_full.add(mf);

        arrayAdapter = new ArrayAdapter<String>
                (this, android.R.layout.simple_list_item_1, messages_list);

        messagesListView = (ListView) findViewById(R.id.messagesSentListView);
        // DataBind ListView with items from ArrayAdapter
        messagesListView.setAdapter(arrayAdapter);

        setupActionBar();
    }

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
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.message_send, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                finish();
                return true;
            case R.id.send_message_action:
                EditText messageTextSend=(EditText)findViewById(R.id.messageTextSend);
                String message = messageTextSend.getText().toString();
                try {
                    message = URLEncoder.encode(message, "UTF-8");
                    Date dateSent = new Date();
                    MessageFile mf = new MessageFile("Kdy: " + dateSent + "\nZpráva: " + message, null);
                    messages_list_full.add(0, mf);
                    SimpleDateFormat dateFormat = new SimpleDateFormat("hh:mm");
                    messages_list.add(0, dateFormat.format(dateSent) + ": " + message);
                    new AdapterHelper().update((ArrayAdapter) arrayAdapter, new ArrayList<Object>(messages_list));
                    arrayAdapter.notifyDataSetChanged();
                    new SendMessageToURL().execute("http://gisak.vsb.cz/patrac/mserver.php?operation=insertmessage&searchid=" + MainActivity.searchid + "&from_id=" + MainActivity.sessionId + "&message=" + message + "&id=coordinator" + MainActivity.searchid);
                } catch (UnsupportedEncodingException e) {
                    e.printStackTrace();
                }
                return true;
        }
        return false;
    }


    /**
     * Background Async Task to download file
     * */
    class SendMessageToURL extends AsyncTask<String, String, String> {

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
                int lengthOfFile = conection.getContentLength();


            } catch (Exception e) {
                cancel(true);
            }

            return null;
        }


    }
}
