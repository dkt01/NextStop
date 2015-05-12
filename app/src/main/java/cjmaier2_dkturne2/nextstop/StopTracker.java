package cjmaier2_dkturne2.nextstop;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.Menu;
import android.view.MenuItem;

import java.util.ArrayList;
import java.util.GregorianCalendar;
import java.util.List;

public class StopTracker extends ActionBarActivity implements LocationListener, SensorEventListener {

    private List<BusStopData> busStops;
    private RecyclerView rv;

    private LocationManager lManager;
    private SensorManager mSensorManager;
    private Sensor mAccel;
    private Sensor mMag;

    private float[] mLastAccelerometer = new float[3];
    private float[] mLastMagnetometer = new float[3];
    private boolean mLastAccelerometerSet = false;
    private boolean mLastMagnetometerSet = false;
    private float[] mR = new float[9];
    private float[] mOrientation = new float[3];
    private float mBearing = 0f;
    private double lat = 0;
    private double lon = 0;

    private android.os.Handler guiHandler;
    private int guiInterval = 1000; // 1000 mS
    private boolean create = true;

    private DataBaseHelper_Routes routesDB;
    private DataBaseHelper_Shapes shapesDB;
    private DataBaseHelper_Stops stopsDB;
    private DataBaseHelper_StopTimes stoptimesDB;
    private DataBaseHelper_Trips tripsDB;

    private List<RoutePath> tripCandidates;


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
        lManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);

        routesDB = new DataBaseHelper_Routes(this.getApplicationContext());
        shapesDB = new DataBaseHelper_Shapes(this.getApplicationContext());
        stopsDB = new DataBaseHelper_Stops(this.getApplicationContext());
        stoptimesDB = new DataBaseHelper_StopTimes(this.getApplicationContext());
        tripsDB = new DataBaseHelper_Trips(this.getApplicationContext());

        mSensorManager = (SensorManager)getSystemService(SENSOR_SERVICE);
        mAccel = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        mMag = mSensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);
    }

    @Override
    protected void onResume() {
        super.onResume();
        guiUpdate.run();

        lManager.requestLocationUpdates(lManager.GPS_PROVIDER, 500, 1, this);
        Location loc = lManager.getLastKnownLocation(lManager.GPS_PROVIDER);
        if(loc != null)
        {
            lat = loc.getLatitude();
            lon = loc.getLongitude();
            List<String> stops = stopsDB.nearestStop(lat,lon);
            for(String stop:stops) {
                addStop newstop = new addStop(stop,loc);
                newstop.execute();
            }
        }

        mSensorManager.registerListener(this, mAccel, 50000);
        mSensorManager.registerListener(this, mMag, 200000);
    }

    @Override
    protected void onPause() {
        super.onPause();
        guiHandler.removeCallbacks(guiUpdate);
        lManager.removeUpdates(this);
        mSensorManager.unregisterListener(this);
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

    @Override
    public void onLocationChanged(Location location) {
        lat = location.getLatitude();
        lon = location.getLongitude();
        List<String> stops = stopsDB.nearestStop(lat,lon);
        for(String stop:stops) {
            addStop newstop = new addStop(stop,location);
            newstop.execute();
        }
    }

    @Override
    public void onProviderEnabled(String provider) {}

    @Override
    public void onProviderDisabled(String provider) {}

    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        if (event.sensor == mAccel) {
            System.arraycopy(event.values, 0, mLastAccelerometer, 0, event.values.length);
            mLastAccelerometerSet = true;
        } else if (event.sensor == mMag) {
            System.arraycopy(event.values, 0, mLastMagnetometer, 0, event.values.length);
            mLastMagnetometerSet = true;
        }
        if (mLastAccelerometerSet && mLastMagnetometerSet) {
            SensorManager.getRotationMatrix(mR, null, mLastAccelerometer, mLastMagnetometer);
            SensorManager.getOrientation(mR, mOrientation);
            float azimuthInRadians = mOrientation[0];
            mBearing = (float)(Math.toDegrees(azimuthInRadians)+360)%360;
            mLastAccelerometerSet = false;
            mLastMagnetometerSet = false;
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
    }

    private void initializeData(){
        busStops = new ArrayList<>();
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
//            if(create)
//            {
//                Random r = new Random();
//                ArrayList<Route> routes = new ArrayList<Route>();
//                for(int i = 0; i <= r.nextInt(9); i++)
//                {
//                    routes.add(Route.values()[r.nextInt(Route.values().length)]);
//                }
//                addItem(new BusStopData("Wright & Chalmers", (ArrayList<Route>) routes.clone(), r.nextInt(500)));
//            }
//            else
//            {
//                removeItem(busStops.get(0));
//            }
//            create = !create;
        }
    };

    // Returns list of route colors for a given stop
    public List<Route> getRoutesByStop(String stopID) {
        List<String> trips = stoptimesDB.getTrips(stopID);
        List<String> routeIDs = tripsDB.getRoutes(trips);
        return routesDB.getRouteColors(routeIDs);
    }

    public void getCandidates(Location loc) {
        tripCandidates.clear();
        List<String> stopCandidates = stopsDB.nearestStop(loc.getLatitude(), loc.getLongitude());
        for(String stopCandidate:stopCandidates) {
            List<String> tCandidates = stoptimesDB.getTripCandidates(stopCandidate,new GregorianCalendar());
            for(String tCandidate:tCandidates) {
                List<Location> waypoints = shapesDB.getWaypoints(tCandidate);
                tripCandidates.add(new RoutePath(waypoints,stopsDB.getStopLocation(stopCandidate),
                                                 tCandidate,tripsDB.getRoutes(tCandidate),stopCandidate));
            }
        }
        initialPruneCandidates(mBearing);
    }

    public void initialPruneCandidates(float bearing) {
        for(RoutePath candidate:tripCandidates) {
            if(!candidate.bearingInMargin(bearing))
                tripCandidates.remove(candidate);
        }
    }

    private class addStop extends AsyncTask<Void, Void, Void> {
        private final String stop;
        private final Location loc;

        public addStop(String stop, Location loc) {
            this.stop = stop;
            this.loc = loc;
        }

        protected Void doInBackground(Void... params) {
            addItem(new BusStopData(stopsDB.getStopName(stop), stop, getRoutesByStop(stop),
                                    (int)stopsDB.getDist2Stop(stop,loc.getLatitude(),loc.getLongitude())));
            return null;
        }
    }
}
