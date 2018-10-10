package cz.vsb.gis.ruz76.patrac.android.listeners;

import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.net.Uri;
import android.os.Environment;
import android.view.View;
import android.widget.AdapterView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.text.SimpleDateFormat;
import java.util.TimeZone;

import cz.vsb.gis.ruz76.patrac.android.R;
import cz.vsb.gis.ruz76.patrac.android.activities.MainActivity;
import cz.vsb.gis.ruz76.patrac.android.activities.MapsActivity;
import cz.vsb.gis.ruz76.patrac.android.domain.Waypoint;
import cz.vsb.gis.ruz76.patrac.android.helpers.DownloadFileFromUrl;

/**
 * Created by jencek on 10.10.18.
 */

public class GpxListener implements AdapterView.OnItemClickListener {

    private TextView mTextStatus;
    private MapsActivity mapsActivity;

    public MapsActivity getMapsActivity() {
        return mapsActivity;
    }

    public void setMapsActivity(MapsActivity mapsActivity) {
        this.mapsActivity = mapsActivity;
    }

    public TextView getmTextStatus() {
        return mTextStatus;
    }

    public void setmTextStatus(TextView mTextStatus) {
        this.mTextStatus = mTextStatus;
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        if (position == 0) {
            mTextStatus.setText(mapsActivity.getString(R.string.preparing_data_wait));
            int log = createLocalGpx();
            if (log == 0) {
                showLocalGpx();
            }
            if (log == 1) {
                mTextStatus.setText(R.string.less_than_two_positions);
            }
            if (log == 2) {
                mTextStatus.setText(R.string.problem_to_create_log);
            }
        }

        if (position == 1) {
            mTextStatus.setText(R.string.downloading_wait);
            downloadFromUrl(MainActivity.endPoint + "operation=getgpx_last&searchid=" + MainActivity.searchid, "server_last.gpx");
        }

        if (position == 2) {
            mTextStatus.setText(R.string.downloading_wait);
            downloadFromUrl(MainActivity.endPoint + "operation=getgpx&searchid=" + MainActivity.searchid, "server.gpx");
        }

    }

    private void showLocalGpx() {
        mTextStatus.setText(R.string.preparing_data_open);
        Intent i = new Intent();
        i.setAction(android.content.Intent.ACTION_VIEW);
        i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        i.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        String fileName = "local.gpx";
        String path = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MOVIES).toString() + "/" + fileName;
        i.setDataAndType(Uri.parse("file://" + path), "application/gpx+xml");
        File file = new File(path);
        if (file.exists()) {
            try {
                mapsActivity.startActivity(i);
            } catch (ActivityNotFoundException exception) {
                Toast toast = Toast.makeText(mapsActivity, R.string.can_not_open_activity, Toast.LENGTH_LONG);
                toast.show();
            }
            mTextStatus.setText(R.string.ready_for_download);
        } else {
            mTextStatus.setText(R.string.error_data_download);
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
                local_gpx.println("<trkpt lat=\"" + wp.lat + "\" lon=\"" + wp.lon + "\"><name>" + R.string.this_device + "</name><time>" + timeutc + "</time></trkpt>");
            }
            local_gpx.println("</trkseg></trk></gpx>");
            local_gpx.close();
            return 0;
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            return 2;
        }
    }

    private void downloadFromUrl(String url, String fileName) {
        DownloadFileFromUrl downloadFileFromUrl = new DownloadFileFromUrl();
        downloadFileFromUrl.setActivity(mapsActivity);
        downloadFileFromUrl.setOpenFile(true);
        downloadFileFromUrl.setTextStatus(mTextStatus);
        downloadFileFromUrl.setFileName(fileName);
        downloadFileFromUrl.execute(url);
    }
}
