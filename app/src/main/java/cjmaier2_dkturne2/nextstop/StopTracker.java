package cjmaier2_dkturne2.nextstop;

import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.Menu;
import android.view.MenuItem;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import cjmaier2_dkturne2.nextstop.R;

public class StopTracker extends ActionBarActivity {

    private List<BusStopData> busStops;
    private RecyclerView rv;

    private android.os.Handler guiHandler;
    private int guiInterval = 250; // 1000 mS
    private boolean create = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_stop_tracker);

        rv=(RecyclerView)findViewById(R.id.rv);

        LinearLayoutManager llm = new LinearLayoutManager(this);
        rv.setLayoutManager(llm);
        rv.setHasFixedSize(true);
        rv.setItemAnimator(new DefaultItemAnimator());

        initializeData();
        initializeAdapter();

        guiHandler = new android.os.Handler();
    }

    @Override
    protected void onResume() {
        super.onResume();
        guiUpdate.run();
    }

    @Override
    protected void onPause() {
        super.onPause();
        guiHandler.removeCallbacks(guiUpdate);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_stop_tracker, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    private void initializeData(){
        busStops = new ArrayList<>();
        busStops.add(new BusStopData("Wright & Springfield", new ArrayList<Route>() , 50));
        busStops.add(new BusStopData("Wright & Healey", new ArrayList<Route>(), 70));
        busStops.add(new BusStopData("Transit Plaza", new ArrayList<Route>(), 90));
    }

    private void initializeAdapter(){
        RVAdapter adapter = new RVAdapter(busStops);
        rv.setAdapter(adapter);
    }

    public void addItem(BusStopData item) {
        addItem(item, rv.getAdapter().getItemCount());
    }

    public void addItem(BusStopData item, int position) {
        busStops.add(position, item);
        rv.getAdapter().notifyItemInserted(position);
    }

    public void removeItem(BusStopData item) {
        int position = busStops.indexOf(item);
        busStops.remove(position);
        rv.getAdapter().notifyItemRemoved(position);
    }

    public void updateDistance(int index, int distance) {
        try {
            busStops.get(index).distance = distance;
            rv.getAdapter().notifyItemChanged(index);
        } catch (IndexOutOfBoundsException e){
            // Do Nothing
        }
    }

    Runnable guiUpdate = new Runnable() {
        @Override
        public void run() {
            guiHandler.postDelayed(guiUpdate, guiInterval);
            if(create)
            {
                Random r = new Random();
                
                addItem(new BusStopData("Wright & Chalmers", new ArrayList<Route>(), r.nextInt(500)));
            }
            else
            {
                removeItem(busStops.get(0));
            }
            create = !create;
        }
    };
}
