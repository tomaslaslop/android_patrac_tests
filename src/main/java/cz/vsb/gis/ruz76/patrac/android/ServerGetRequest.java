package cz.vsb.gis.ruz76.patrac.android;

import android.os.AsyncTask;
import android.os.StrictMode;
import android.util.Log;
import android.widget.TextView;

import java.io.BufferedInputStream;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;

/**
 * Background Async Task to download file.
 */
public class ServerGetRequest extends AsyncTask<String, String, String> {

    private TextView textStatus = null;
    private GetRequestUpdate activity = null;

    public void setTextStatus(TextView textStatus) {
        this.textStatus = textStatus;
    }

    public void setActivity(GetRequestUpdate activity) {
        this.activity = activity;
    }

    /**
     * Before starting background thread set VM policy.
     */
    @Override
    protected void onPreExecute() {
        super.onPreExecute();
        StrictMode.VmPolicy.Builder builder = new StrictMode.VmPolicy.Builder();
        StrictMode.setVmPolicy(builder.build());
    }

    /**
     * Downloading file in background thread.
     */
    @Override
    protected String doInBackground(String... f_url) {
        int count;
        try {
            URL url = new URL(f_url[0]);
            URLConnection conection = url.openConnection();
            conection.connect();
            InputStream input = new BufferedInputStream(url.openStream(),8192);
            StringBuilder stringBuilder = new StringBuilder();
            byte data[] = new byte[1024];
            long total = 0;
            while ((count = input.read(data)) != -1) {
                total += count;
                String currentBufferString = new String(data, 0, count);
                stringBuilder.append(currentBufferString);
            }
            input.close();
            return stringBuilder.toString();

        } catch (Exception e) {
            if (textStatus != null) {
                textStatus.setText(R.string.download_error);
            }
            MainActivity.StatusMessages = e.getMessage();
            Log.e("Error: ", e.getMessage());
            cancel(true);
            return null;
        }
    }

    /**
     * After completing background task show the data.
     * **/
    @Override
    protected void onPostExecute(String result) {
        activity.processResponse(result);
    }
}
