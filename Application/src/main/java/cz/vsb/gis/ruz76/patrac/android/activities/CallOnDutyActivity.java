package cz.vsb.gis.ruz76.patrac.android.activities;

import android.app.ActionBar;
import android.os.Bundle;
import android.app.Activity;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;

import cz.vsb.gis.ruz76.patrac.android.R;
import cz.vsb.gis.ruz76.patrac.android.domain.RequestMode;

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

    /**
     * Adds back button handling.
     * @param item
     * @return
     */
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
        listViewSearches = findViewById(R.id.listViewSearches);

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

        TextView textViewDutyDescription = findViewById(R.id.textViewDutyDescription);
        textViewDutyDescription.setText(getString(R.string.activity_call_on_duty_description));

        setupActionBar();
    }
}
