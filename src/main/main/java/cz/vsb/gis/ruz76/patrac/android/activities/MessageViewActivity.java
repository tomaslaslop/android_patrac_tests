package cz.vsb.gis.ruz76.patrac.android.activities;

import android.app.ActionBar;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.app.Activity;
import android.os.Environment;
import android.os.StrictMode;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.TextView;
import android.widget.Toast;

import cz.vsb.gis.ruz76.patrac.android.R;

public class MessageViewActivity extends Activity {

    String message = "";
    String filename = "";

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
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        StrictMode.VmPolicy.Builder builder = new StrictMode.VmPolicy.Builder();
        StrictMode.setVmPolicy(builder.build());
        setContentView(R.layout.activity_message_view);
        Bundle bundle = getIntent().getExtras();
        message = bundle.getString("message");
        filename = bundle.getString("filename");
        TextView txtView = (TextView) findViewById(R.id.messageTextView);
        String path = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MOVIES).toString();
        path = path + "/" + filename;
        if (filename == null || filename.isEmpty()) {
            txtView.setText(message);
        } else {
            txtView.setText(message + "\n" + getString(R.string.message_attachment_name) + ": " + filename + "\n" + getString(R.string.message_attachment_placement) + ": " + path);
        }
        setupActionBar();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.message, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        String path = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MOVIES).toString();
        path = path + "/" + filename;
        String type = getType(filename);
        switch (item.getItemId()) {
            case android.R.id.home:
                finish();
                return true;
            case R.id.show_action:
                Intent i = new Intent();
                i.setAction(android.content.Intent.ACTION_VIEW);
                i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                i.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                i.setDataAndType(Uri.parse("file://"+path), type);
                try {
                    startActivity(i);
                } catch (ActivityNotFoundException exception) {
                    Toast toast = Toast.makeText(MessageViewActivity.this, R.string.can_not_open_activity, Toast.LENGTH_LONG);
                    toast.show();
                }
                return true;

            case R.id.send_action:
                Intent intent = new Intent();
                intent.setAction(Intent.ACTION_SEND);
                intent.setType(type);
                intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                intent.putExtra(Intent.EXTRA_STREAM, Uri.parse("file://"+path));
                try {
                    startActivity(intent);
                } catch (ActivityNotFoundException exception) {
                    Toast toast = Toast.makeText(MessageViewActivity.this, R.string.can_not_open_activity, Toast.LENGTH_LONG);
                    toast.show();
                }
                return true;

        }
        return false;
    }

    //TODO do it better, probably other types can be sent as well
    private String getType(String filename) {
        String parts[] = filename.split("\\.");
        String extension = parts[parts.length - 1];
        switch (extension) {
            case "png":
                return "image/png";
            case "jpg":
                return "image/jpeg";
            case "xml":
                return "text/xml";
            case "gpx":
                return "application/gpx+xml";
            default:
                return "image/png";
        }
    }
}
