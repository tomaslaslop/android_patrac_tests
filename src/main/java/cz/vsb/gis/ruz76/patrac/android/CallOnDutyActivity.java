package cz.vsb.gis.ruz76.patrac.android;

import android.app.ActionBar;
import android.content.Intent;
import android.os.Bundle;
import android.app.Activity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class CallOnDutyActivity extends Activity {

    private String[] searches;
    private List<String> searchesList;
    // Create an ArrayAdapter from List
    private ArrayAdapter<String> arrayAdapter;
    private ListView listViewSearches;


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
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == android.R.id.home) {
            finish();
        }
        return true;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_call_on_duty);
        Bundle bundle = getIntent().getExtras();
        searches = bundle.getStringArray("searches");
        searchesList = new ArrayList<>();
        for (int i = 0; i<searches.length; i++) {
            searchesList.add(searches[i].split(";")[1]);
        }
        arrayAdapter = new ArrayAdapter<String>
                (this, android.R.layout.simple_list_item_1, searchesList);
        listViewSearches = (ListView) findViewById(R.id.listViewSearches);

        // DataBind ListView with items from ArrayAdapter
        listViewSearches.setAdapter(arrayAdapter);

        listViewSearches.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                MainActivity.searchid = searches[position].split(";")[0];
                MainActivity.mode = RequestMode.SELECTED;
                Toast toast = Toast.makeText(CallOnDutyActivity.this, R.string.call_on_duty_selected, Toast.LENGTH_LONG);
                toast.show();
            }
        });

        TextView textViewDutyDescription = (TextView) findViewById(R.id.textViewDutyDescription);
        textViewDutyDescription.setText(getString(R.string.activity_call_on_duty_description));

        setupActionBar();
    }

}
