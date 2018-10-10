package cz.vsb.gis.ruz76.patrac.android.helpers;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Environment;
import android.os.StrictMode;
import android.util.Log;
import android.widget.TextView;
import android.widget.Toast;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.net.URLConnection;
import java.util.Arrays;

import cz.vsb.gis.ruz76.patrac.android.R;
import cz.vsb.gis.ruz76.patrac.android.activities.MainActivity;

/**
 * Background Async Task to download file.
 */
public class DownloadFileFromUrl extends AsyncTask<String, String, String> {

    private TextView textStatus = null;
    private boolean openFile = false;
    private Activity activity = null;
    private String fileName = null;

    public void setTextStatus(TextView textStatus) {
        this.textStatus = textStatus;
    }

    public void setOpenFile(boolean openFile) {
        this.openFile = openFile;
    }

    public void setActivity(Activity activity) {
        this.activity = activity;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
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
        if (fileName == null) {
            fileName = f_url[1];
        }
        try {
            URL url = new URL(f_url[0]);
            URLConnection conection = url.openConnection();
            conection.connect();
            int lenghtOfFile = conection.getContentLength();
            InputStream input = new BufferedInputStream(url.openStream(),8192);
            // this was the only safe place that should exists on all devices
            String path = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MOVIES).toString();
            OutputStream output = new FileOutputStream(path + "/" + fileName);
            byte data[] = new byte[1024];
            long total = 0;
            while ((count = input.read(data)) != -1) {
                total += count;
                // shows the progress (runs the onProgressUpdate)
                publishProgress("" + (int) ((total * 100) / lenghtOfFile));
                output.write(data, 0, count);
            }
            output.flush();
            output.close();
            input.close();
        } catch (Exception e) {
            if (textStatus != null) {
                textStatus.setText(R.string.download_error);
            }
            MainActivity.StatusMessages = e.getMessage();
            Log.e("Error: ", e.getMessage());
            cancel(true);
        }
        return null;
    }

    /**
     * Updating progress info.
     */
    protected void onProgressUpdate(String... progress) {
        if (textStatus != null) {
            textStatus.setText(R.string.downloaded + " " + Arrays.toString(progress) + " %");
        }
    }

    /**
     * After completing background task show the data.
     * **/
    @Override
    protected void onPostExecute(String file_url) {
        if (openFile) {
            if (textStatus != null) {
                textStatus.setText(R.string.preparing_data_open);
            }
            Intent i = new Intent();
            i.setAction(android.content.Intent.ACTION_VIEW);
            i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            i.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            String path = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MOVIES).toString() + "/" + fileName;
            i.setDataAndType(Uri.parse("file://" + path), "application/gpx+xml");
            File file = new File(path);
            if (file.exists()) {
                if (activity != null) {
                    try {
                        activity.startActivity(i);
                    } catch (ActivityNotFoundException exception) {
                        Toast toast = Toast.makeText(activity, R.string.can_not_open_activity, Toast.LENGTH_LONG);
                        toast.show();
                    }
                }
                if (textStatus != null) {
                    textStatus.setText(R.string.ready_for_download);
                }
            } else {
                if (textStatus != null) {
                    textStatus.setText(R.string.error_data_download);
                }
            }
        }
    }

}
