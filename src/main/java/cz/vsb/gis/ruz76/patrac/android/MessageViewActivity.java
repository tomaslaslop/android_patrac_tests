package cz.vsb.gis.ruz76.patrac.android;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.app.Activity;
import android.os.Environment;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.TextView;

public class MessageViewActivity extends Activity {

    String message = "";
    String filename = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_message_view);
        Bundle bundle = getIntent().getExtras();
        message = bundle.getString("message");
        filename = bundle.getString("filename");
        TextView txtView = (TextView) findViewById(R.id.messageTextView);
        txtView.setText(message + "\n" + filename);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.message, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        String path = Environment.getExternalStorageDirectory().toString() + "/" + filename;
        String type = getType(filename);
        switch (item.getItemId()) {
            case R.id.show_action:
                Intent i = new Intent();
                i.setAction(android.content.Intent.ACTION_VIEW);
                i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                i.setDataAndType(Uri.parse("file://"+path), type);
                startActivity(i);
                return true;

            case R.id.send_action:
                //http://www.egr.msu.edu/classes/ece480/capstone/spring14/group01/docs/appnote/Wirsing-SendingAndReceivingDataViaBluetoothWithAnAndroidDevice.pdf
                //https://tsicilian.wordpress.com/2012/11/06/bluetooth-data-transfer-with-android/
                //BluetoothAdapter btAdapter = BluetoothAdapter.getDefaultAdapter();
                //if (btAdapter != null) {
                    Intent intent = new Intent();
                    intent.setAction(Intent.ACTION_SEND);
                    intent.setType(type);
                    intent.putExtra(Intent.EXTRA_STREAM, Uri.parse("file://"+path));
                    startActivity(intent);
                //}
                return true;

        }
        return false;
    }

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
