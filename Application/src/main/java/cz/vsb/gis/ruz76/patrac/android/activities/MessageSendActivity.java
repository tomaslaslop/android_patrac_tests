package cz.vsb.gis.ruz76.patrac.android.activities;

import android.app.ActionBar;
import android.content.Context;
import android.graphics.Color;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.StrictMode;
import android.support.v4.app.FragmentActivity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Toast;

import com.github.angads25.filepicker.controller.DialogSelectionListener;
import com.github.angads25.filepicker.model.DialogConfigs;
import com.github.angads25.filepicker.model.DialogProperties;
import com.github.angads25.filepicker.view.FilePickerDialog;
import com.loopj.android.http.AsyncHttpClient;
import com.loopj.android.http.AsyncHttpResponseHandler;
import com.loopj.android.http.RequestParams;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;

import cz.msebera.android.httpclient.Header;
import cz.vsb.gis.ruz76.patrac.android.adapters.UsersArrayAdapter;
import cz.vsb.gis.ruz76.patrac.android.domain.User;
import cz.vsb.gis.ruz76.patrac.android.helpers.AdapterHelper;
import cz.vsb.gis.ruz76.patrac.android.helpers.GetRequestUpdate;
import cz.vsb.gis.ruz76.patrac.android.R;
import cz.vsb.gis.ruz76.patrac.android.helpers.GetRequest;

public class MessageSendActivity extends FragmentActivity implements GetRequestUpdate {

    private List<User> users = null;
    private List<String> usersNamesList = null;
    private ArrayAdapter<String> arrayAdapter = null;
    private boolean longClick = true;
    static String message;
    static String fileToUploadPath;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        StrictMode.VmPolicy.Builder builder = new StrictMode.VmPolicy.Builder();
        StrictMode.setVmPolicy(builder.build());
        setContentView(R.layout.activity_message_send);

        setupActionBar();

        setUpListOfUsers();
        sendGetRequest(MainActivity.endPoint + "operation=getlocations&searchid=" + MainActivity.searchid);
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
                EditText messageTextSend = (EditText) findViewById(R.id.messageTextSend);
                message = messageTextSend.getText().toString();
                try {
                    message = URLEncoder.encode(message, "UTF-8");
                    sendMessage();
                    //new SendMessageToURL().execute(R.string.pref_default_endpoint + "operation=insertmessage&searchid=" + MainActivity.searchid + "&from_id=" + MainActivity.sessionId + "&message=" + message + "&id=coordinator" + MainActivity.searchid);
                } catch (UnsupportedEncodingException e) {
                    e.printStackTrace();
                }
                return true;
            case R.id.send_message_attachment_action:
                showFileDialog();
                return true;
        }
        return false;
    }

    private void sendMessage() {
        String url = getString(R.string.pref_default_endpoint);
        RequestParams params = new RequestParams();
        String ids = "";
        for (int counter = 0; counter < users.size(); counter++) {
            User user = users.get(counter);
            if (user.isSelected()) {
                if (counter == 0) {
                    ids += user.getId();
                } else {
                    ids += ";" + user.getId();
                }
            }
        }

        if (ids.isEmpty()) {
            Toast toast = Toast.makeText(MessageSendActivity.this, "NO RECIPIENT", Toast.LENGTH_LONG);
            toast.show();
            return;
        }

        try {
            params.put("operation", "insertmessages");
            params.put("searchid", MainActivity.searchid);
            params.put("from_id", MainActivity.sessionId);
            params.put("message", message);
            params.put("ids", ids);
            if (fileToUploadPath != null) {
                File fileToUpload = new File(fileToUploadPath);
                params.put("fileToUpload", fileToUpload);
                Toast toast = Toast.makeText(MessageSendActivity.this, "OK", Toast.LENGTH_LONG);
                toast.show();
            }
        } catch (FileNotFoundException e) {
            Toast toast = Toast.makeText(MessageSendActivity.this, "ERROR", Toast.LENGTH_LONG);
            toast.show();
        }

        AsyncHttpClient client = new AsyncHttpClient();
        client.post(url, params, new AsyncHttpResponseHandler() {
            @Override
            public void onSuccess(int statusCode, Header[] headers, byte[] bytes) {
                // handle success response
                Toast toast = Toast.makeText(MessageSendActivity.this, getString(R.string.message) + " " + getString(R.string.message_was_sent), Toast.LENGTH_LONG);
                toast.show();
                fileToUploadPath = null;
            }

            @Override
            public void onFailure(int statusCode, Header[] headers, byte[] bytes, Throwable throwable) {
                // handle failure response
                Toast toast = Toast.makeText(MessageSendActivity.this, getString(R.string.message) + " " + getString(R.string.message_was_not_sent), Toast.LENGTH_LONG);
                toast.show();
            }
        });
    }

    private void showFileDialog() {
        DialogProperties properties = new DialogProperties();
        properties.selection_mode = DialogConfigs.SINGLE_MODE;
        properties.selection_type = DialogConfigs.FILE_SELECT;
        properties.root = new File(DialogConfigs.DEFAULT_DIR);
        properties.error_dir = new File(DialogConfigs.DEFAULT_DIR);
        properties.offset = new File(DialogConfigs.DEFAULT_DIR);
        properties.extensions = null;
        FilePickerDialog dialog = new FilePickerDialog(MessageSendActivity.this, properties);
        dialog.setTitle(getString(R.string.message_attachment_select));
        dialog.show();
        dialog.setDialogSelectionListener(new DialogSelectionListener() {
            @Override
            public void onSelectedFilePaths(String[] files) {
                fileToUploadPath = files[0];
                File file = new File(fileToUploadPath);
                long length = file.length();
                if (length == 0) {
                    Toast toast = Toast.makeText(MessageSendActivity.this, getString(R.string.message_attachment) + " " + getString(R.string.message_attachment_can_not_be_zero_length), Toast.LENGTH_LONG);
                    toast.show();
                    fileToUploadPath = null;
                    return;
                }
                if (length > 10_000_000L) {
                    Toast toast = Toast.makeText(MessageSendActivity.this, getString(R.string.message_attachment) + " " + getString(R.string.message_attachment_is_too_big), Toast.LENGTH_LONG);
                    toast.show();
                    fileToUploadPath = null;
                    return;
                }
                Toast toast = Toast.makeText(MessageSendActivity.this, getString(R.string.message_attachment) + " " + getString(R.string.message_attachment_was_append), Toast.LENGTH_LONG);
                toast.show();
            }
        });
    }

    private void setUpListOfUsers() {

        // TODO
        //https://medium.com/mindorks/custom-array-adapters-made-easy-b6c4930560dd

        users = new ArrayList<>();
        User u = new User("coordinator" + MainActivity.searchid, "Štáb", false);
        users.add(u);

        usersNamesList = new ArrayList<>();
        usersNamesList.add(getString(R.string.coordinator));

        // Create an ArrayAdapter from List
        //arrayAdapter = new ArrayAdapter<String>
        //        (this, android.R.layout.simple_list_item_1, usersNamesList);

        arrayAdapter = new UsersArrayAdapter(this, android.R.layout.simple_list_item_1, users, usersNamesList);

        // DataBind ListView with items from ArrayAdapter
        final ListView usersListView = (ListView) findViewById(R.id.usersListView);
        usersListView.setAdapter(arrayAdapter);

        usersListView.setChoiceMode(ListView.CHOICE_MODE_MULTIPLE);

        usersListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                if (users.get(position).isSelected()) {
                    users.get(position).setSelected(false);
                    usersListView.setItemChecked(position, false);
                    view.setBackgroundColor(Color.TRANSPARENT);
                } else {
                    users.get(position).setSelected(true);
                    usersListView.setItemChecked(position, true);
                    view.setBackgroundColor(Color.LTGRAY);
                }
                EditText messageTextSend = (EditText) findViewById(R.id.messageTextSend);
                InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                imm.hideSoftInputFromWindow(messageTextSend.getWindowToken(), 0);
                //messageTextSend.onEditorAction(EditorInfo.IME_ACTION_DONE);
            }
        });

        usersListView.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> adapterView, View view, int i, long l) {
                for (User user : users) {
                    user.setSelected(longClick);
                }
                longClick = !longClick;
                arrayAdapter.notifyDataSetChanged();
                return true;
            }
        });

    }

    /**
     * It is triggered when the download of the data from Download is finished.
     *
     * @param result response from the server
     */
    public void processResponse(String result) {
        if (result != null) {
            // List of users
            String[] lines = result.split("\n");
            for (int lineid = 0; lineid < lines.length; lineid++) {
                String[] items = lines[lineid].split(";");
                User user = new User(items[0], items[3], false);
                users.add(1, user);
                usersNamesList.add(1, items[3]);
                new AdapterHelper().update((ArrayAdapter) arrayAdapter, new ArrayList<Object>(usersNamesList));
                arrayAdapter.notifyDataSetChanged();
            }
        } else {
            // no info
        }
    }

    private void sendGetRequest(String url) {
        GetRequest getRequest = new GetRequest();
        getRequest.setActivity(this);
        getRequest.execute(url);
    }
}
