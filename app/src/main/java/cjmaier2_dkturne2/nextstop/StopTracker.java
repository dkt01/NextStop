package cjmaier2_dkturne2.nextstop;

import android.content.Context;
import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;

public class StopTracker extends ActionBarActivity implements LocationListener, SensorEventListener {

    private List<BusStopData> busStops;
    private final short MAXDRAWCARDS = 10;
    private RecyclerView rv;

    private LocationManager lManager;
    private SensorManager mSensorManager;
    private Sensor mAccel;
    private Sensor mMag;
    private Sensor mGrav;

    private float[] mLastAccelerometer = new float[3];
    private float[] mLastMagnetometer = new float[3];
    private boolean mLastAccelerometerSet = false;
    private boolean mLastMagnetometerSet = false;
    private float[] mR = new float[9];
    private float[] mOrientation = new float[3];
    private float mBearing = 0f;
    private double lat = 0;
    private double lon = 0;
    private float a_x = 0;
    private float a_y = 0;
    private float a_z = 0;
    private double prev_dist = 0;

    private android.os.Handler guiHandler;
    private int guiInterval = 500; // 500 mS
    private boolean create = true;

    private DataBaseHelper_Routes routesDB;
    private DataBaseHelper_Shapes shapesDB;
    private DataBaseHelper_Stops stopsDB;
    private DataBaseHelper_StopTimes stoptimesDB;
    private DataBaseHelper_Trips tripsDB;
    private DataBaseHelper_StopRoutes stoproutesDB;

    private List<RoutePath> tripCandidates;
    private List<List<String>> upcomingStopCandidates;
    private List<String> upcomingStops;

    //dead reckoning variables
    private double time_prev = 0; //used for integration
    private float dt = 0;
    private LinkedList<Float> a_x_data = new LinkedList<Float>();
    private LinkedList<Float> a_y_data = new LinkedList<Float>();
    private LinkedList<Float> a_z_data = new LinkedList<Float>();
    private float a_x_off = 0, a_y_off = 0, a_z_off = 0; //offsets to reduce noise
    private float a_x_prev = 0, a_y_prev = 0, a_z_prev = 0;
    private float v_x = 0, v_y = 0, v_z = 0;
    private float v_x_prev = 0, v_y_prev = 0, v_z_prev = 0;
    private float p_x = 0, p_y = 0, p_z = 0, p_net = 0; //p_net is all 3 axes
    private float grav_x = 0, grav_y = 0, grav_z = 0;
    private int dedreck_itr = 0; //need 2 readings for velocity, 3 readings for position

    private File rootdir;
    private boolean writer_created = false; //to determine whether to write to csv
    private BufferedWriter writer;
    private StringBuffer csv_text;
    private String wfilename;
    File textFile = null;

    private boolean need_loc = true;


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

        tripCandidates = new ArrayList<>();
        upcomingStopCandidates = new ArrayList<>();
        upcomingStops = new ArrayList<>();

        guiHandler = new android.os.Handler();
        lManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);

        routesDB = DataBaseHelper_Routes.getInstance(this.getApplicationContext());
        shapesDB = DataBaseHelper_Shapes.getInstance(this.getApplicationContext());
        stopsDB = DataBaseHelper_Stops.getInstance(this.getApplicationContext());
        stoptimesDB = DataBaseHelper_StopTimes.getInstance(this.getApplicationContext());
        tripsDB = DataBaseHelper_Trips.getInstance(this.getApplicationContext());
        stoproutesDB = DataBaseHelper_StopRoutes.getInstance(this.getApplicationContext());

        mSensorManager = (SensorManager)getSystemService(SENSOR_SERVICE);
        mAccel = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        mMag = mSensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);
        mGrav = mSensorManager.getDefaultSensor(Sensor.TYPE_GRAVITY);
    }

    @Override
    protected void onResume() {
        super.onResume();

        mSensorManager.registerListener(this, mAccel, 0);
        mSensorManager.registerListener(this, mGrav, 0);
        mSensorManager.registerListener(this, mMag, 100000);

        lManager.requestLocationUpdates(lManager.GPS_PROVIDER, 1000, 5, this);
        Location loc = lManager.getLastKnownLocation(lManager.GPS_PROVIDER);
        if(loc != null)
        {
            lat = loc.getLatitude();
            lon = loc.getLongitude();
            getCandidates(loc);
            updateUpcomingStops();
            updateCards(loc);
            need_loc = tripCandidates.size() == 0;
        }
        guiUpdate.run();
    }

    @Override
    protected void onPause() {
        super.onPause();
        guiHandler.removeCallbacks(guiUpdate);
        lManager.removeUpdates(this);
        mSensorManager.unregisterListener(this);
        writer_created = false;
        try {
            writer.close();
        } catch (IOException ex) {
            // Do nothing
        }
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
        if(need_loc) {
            getCandidates(location);
            updateUpcomingStops();
            updateCards(location);
            need_loc = tripCandidates.size() == 0;
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
            float[] values = event.values;

            //keep track of first 5 accelerometer readings and average them for offset values
            if(a_x_data.size() < 5) {
                a_x_data.add(values[0]);
                a_y_data.add(values[1]);
                a_z_data.add(values[2]);
                return;
            }
            else if(a_x_data.size() == 5) {
                a_x_off = (a_x_data.get(0) + a_x_data.get(1) + a_x_data.get(2) + a_x_data.get(3) + a_x_data.get(4)) / 5.0f;
                a_y_off = (a_y_data.get(0) + a_y_data.get(1) + a_y_data.get(2) + a_y_data.get(3) + a_y_data.get(4)) / 5.0f;
                a_z_off = (a_z_data.get(0) + a_z_data.get(1) + a_z_data.get(2) + a_z_data.get(3) + a_z_data.get(4)) / 5.0f;
                a_x_data.add(0.0f);
            }

            a_x = values[0]-grav_x;
            a_y = values[1]-grav_y;
            a_z = values[2]-grav_z;
            float w = 0.05f;
            if(a_x > -1*w && a_x < w && a_y > -1*w && a_y < w) {
                v_x = 0;
                v_y = 0;
            }

            //dead reckoning, with time in seconds
            dt = (new Double((event.timestamp - time_prev)/1000000000.0)).floatValue(); //double to float

            if (dedreck_itr == 0) {
                dedreck_itr = 1;
            }
            else if (dedreck_itr == 1) { //enough readings to get velocity
                dedreck_itr = 2;
                v_x = (a_x - a_x_prev)*dt;
                v_y = (a_y - a_y_prev)*dt;
                v_z = (a_z - a_z_prev)*dt;
                v_y = v_y < 0 ? 0 : v_y;
            }
            else if (dedreck_itr == 2){ //enough readings to get velocity and position
                dedreck_itr = 3;
                v_x = (a_x - a_x_prev)*dt;
                v_y = (a_y - a_y_prev)*dt;
                v_z = (a_z - a_z_prev)*dt;
                v_y = v_y < 0 ? 0 : v_y;
                p_x += (v_x - v_x_prev)*dt;
                p_y += (v_y - v_y_prev)*dt;
                p_z += (v_z - v_z_prev)*dt;
            }
            else {
                if(Math.abs(a_x-a_x_prev) > 0.1) v_x += a_x*dt;
                if(Math.abs(a_y-a_y_prev) > 0.1) v_y += a_y*dt;
                if(Math.abs(a_z-a_z_prev) > 0.1) v_z += a_z*dt;
                v_y = v_y < 0 ? 0 : v_y;
                p_x += v_x*dt;
                p_y += v_y*dt;
                p_z += v_z*dt;
            }

            p_net = (float) Math.sqrt(p_x*p_x+p_y*p_y); //not accounting for z direction

            time_prev = event.timestamp;
            a_x_prev = a_x;
            a_y_prev = a_y;
            a_z_prev = a_z;
            v_x_prev = v_x;
            v_y_prev = v_y;
            v_z_prev = v_z;
            mLastAccelerometerSet = true;
        } else if (event.sensor == mMag) {
            System.arraycopy(event.values, 0, mLastMagnetometer, 0, event.values.length);
            mLastMagnetometerSet = true;
        } else if (event.sensor == mGrav) {
            float[] values = event.values;
            grav_x = values[0];
            grav_y = values[1];
            grav_z = values[2];
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
        removeItem(busStops.indexOf(item));
    }

    public void removeItem(int i) {
        busStops.remove(i);
        rv.getAdapter().notifyItemRemoved(i);
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
            double cur_dist = p_net;
            moveAlongPath((float)(cur_dist-prev_dist),mBearing);
            updateDistances();
            prev_dist = cur_dist;
            StringBuffer csv_text = new StringBuffer();
            String state = Environment.getExternalStorageState();
            if(state.equals(Environment.MEDIA_MOUNTED)) {
                File externalDir = Environment.getExternalStorageDirectory();
                rootdir = new File(externalDir.getAbsolutePath() + "/NextStop-Tests");
                if(!rootdir.exists())
                {
                    try{
                        rootdir.mkdir();
                    } catch(SecurityException ex){
                        return;
                    }
                }
                File dir;
                dir = new File(rootdir.getAbsolutePath());
//                testText.setText(externalDir.getAbsolutePath()); to show directory where file is
                if(!dir.exists()) {
                    try
                    {
                        dir.mkdir();
                    }
                    catch(SecurityException ex)
                    {
                        return;
                    }
                }

                if(!writer_created)
                {
                    SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd_HHmmss", Locale.US);
                    wfilename = "cjmaier2_dkturne2_nextstop_"+sdf.format(new Date())+".csv";
                    textFile = new File(dir, wfilename);
                    try {
                        writer = new BufferedWriter(new FileWriter(textFile));
                        csv_text.append("real lat,real lon,estimate lat,estimate lon\n");
                        writer_created = true;
                    } catch (IOException ex) {
                        Log.w("NEXTSTOP","Error writing");
                    }
                }
            } else
                Log.w("NEXTSTOP","Error starting writer");
            if(writer_created) {
                Location loc;
                try {
                    loc = tripCandidates.get(0).getLocation();
                } catch(Exception e) {
                    loc = new Location("");
                    loc.setLatitude(0);
                    loc.setLongitude(0);
                }
                String newentry = Double.toString(lat) + "," + Double.toString(lon) + "," +
                                  Double.toString(loc.getLatitude()) + "," +
                                  Double.toString(loc.getLongitude()) + "\n";
                csv_text.append(newentry);
                try {
                    writer.write(csv_text.toString());
                    makeFileDiscoverable(textFile, getApplicationContext());
                    csv_text.delete(0,csv_text.length()); //prevent writing same thing over and over and over and over and over and over and over
                    // and over and over and over and over and over and over and over and over and over and
                    // over and over and over and over and over and over and over and over and over again
                } catch (IOException ex){
                    Log.w("NEXTSTOP","Error writing to file");
                }
            }
        }
    };

    public void makeFileDiscoverable(File file, Context context){
        MediaScannerConnection.scanFile(context, new String[]{file.getPath()}, null, null);
        context.sendBroadcast(new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE,
                Uri.fromFile(file)));
    }

    // Returns list of route colors for a given stop
    public List<Route> getRoutesByStop(String stopID) {
        List<String> routeIDs = stoproutesDB.getRoutes(stopID);
        return routesDB.getRouteColors(routeIDs);
    }

    public void getCandidates(Location loc) {
        tripCandidates.clear();
        List<String> stopCandidates = stopsDB.nearestStop(loc.getLatitude(), loc.getLongitude());
        for(String stopCandidate:stopCandidates) {
            List<String> tCandidates = stoptimesDB.getTripCandidates(stopCandidate,new GregorianCalendar());
            for(String tCandidate:tCandidates) {
                String shapeID = tripsDB.getShapeID(tCandidate);
                List<Location> waypoints = shapesDB.getWaypoints(shapeID);
                tripCandidates.add(new RoutePath(waypoints,stopsDB.getStopLocation(stopCandidate),
                                                 tCandidate,tripsDB.getRoutes(tCandidate),stopCandidate));
                upcomingStopCandidates.add(stoptimesDB.getUpcomingStops(stopCandidate,tCandidate));
                Log.i("NEXTSTOP", "Added Candidate: " + tCandidate);
            }
        }
        initialPruneCandidates(mBearing);
    }

    public void initialPruneCandidates(float bearing) {
        List<Integer> deleteIndices = new ArrayList<>();
        for(RoutePath candidate:tripCandidates) {
            if (!candidate.bearingInMargin(bearing))
                Log.i("NEXTSTOP", "Pruned Candidate: " + candidate.getRouteID() + " b_mine: " +
                        Float.toString(bearing) + " b_exp: " + Float.toString(candidate.getBearing()));
        }
        if(deleteIndices.size() > 0) {
            Collections.sort(deleteIndices, Collections.reverseOrder());
            for(Integer i: deleteIndices) {
                tripCandidates.remove(i.intValue());
                upcomingStopCandidates.remove(i.intValue());
            }
        }
    }

    public void moveAlongPath(float dist, float bearing) {
        boolean changed = false;
        List<Integer> deleteIndices = new ArrayList<>();
        for(RoutePath candidate:tripCandidates) {
            if(candidate.moveAlongPath(dist,bearing) == null) {
                changed = true;
                int idx = tripCandidates.indexOf(candidate);
                deleteIndices.add(idx);
            }
        }
        if(deleteIndices.size() > 0) {
            Collections.sort(deleteIndices, Collections.reverseOrder());
            for(Integer i: deleteIndices) {
                Log.i("NEXTSTOP","Removed candidate "+tripCandidates.get(i).getRouteID());
                tripCandidates.remove(i.intValue());
                upcomingStopCandidates.remove(i.intValue());
            }
        }
        if(changed) {
            updateUpcomingStops();
            if(tripCandidates.size() > 0)
                updateCards(tripCandidates.get(0).getLocation());
            updateDistances();
        }
    }

    public void updateUpcomingStops() {
        upcomingStops.clear();
        if(upcomingStopCandidates.size() == 0)
            {}// Do Nothing
        else if(upcomingStopCandidates.size() == 1) {
            upcomingStops = upcomingStopCandidates.get(0);
        }
        else{
            boolean areSame = true;
            String testVal = null;
            for(int i=0; i < upcomingStopCandidates.get(0).size(); i ++) {
                testVal = null;
                for(List<String> list:upcomingStopCandidates) {
                    if(!areSame || list.size() <= i) {
                        areSame = false;
                        break;
                    }
                    if(testVal == null)
                        testVal = list.get(i);
                    else
                        areSame = list.get(i).equals(testVal);
                }
                if(areSame)
                    upcomingStops.add(upcomingStopCandidates.get(0).get(i));
                else
                    break;
            }
        }
        if(upcomingStops.size() == 0)
            Log.w("NEXTSTOP","No shared upcoming stops");
        else
            Log.i("NEXTSTOP", Integer.toString(upcomingStops.size()) + " shared stops");
    }

    public void updateCards(Location loc) {
        if(busStops.size() < MAXDRAWCARDS && busStops.size() < upcomingStops.size())
            for(int i = busStops.size(); i < MAXDRAWCARDS && i < upcomingStops.size(); i++) {
                addStop newStop = new addStop(upcomingStops.get(i),tripCandidates.get(0));
                newStop.execute();
            }
        if(busStops.size() == 0 && tripCandidates.size() > 0)
        {
            addStop newStop = new addStop(tripCandidates.get(0).getRecentStop(),tripCandidates.get(0));
            newStop.execute();
        }
    }

    public void updateDistances() {
        List<Integer> deleteIndices = new ArrayList<>();
        for(BusStopData stop:busStops) {
            int dist = (int)tripCandidates.get(0).distanceAlongRoute(stopsDB.getStopLocation(stop.stopID));
            int index = busStops.indexOf(stop);
            if(dist == -1)
                deleteIndices.add(index);
            else
                updateDistance(index,dist);
        }
        if(deleteIndices.size() > 0) {
            Collections.sort(deleteIndices, Collections.reverseOrder());
            for(Integer i: deleteIndices) {
                removeStop(i.intValue());
            }
        }
    }

    public void removeStop(int i) {
        for(List l:upcomingStopCandidates)
            l.remove(i);
        upcomingStops.remove(i);
        removeItem(i);
    }

    private class addStop extends AsyncTask<Void, Void, Void> {
        private final String stop;
        private Location loc = null;
        private RoutePath route = null;

        public addStop(String stop, Location loc) {
            this.stop = stop;
            this.loc = loc;
        }

        public addStop(String stop, RoutePath route) {
            this.stop = stop;
            this.route = route;
        }

        protected Void doInBackground(Void... params) {
            if(route == null)
                addItem(new BusStopData(stopsDB.getStopName(stop), stop, getRoutesByStop(stop),
                                        (int)stopsDB.getDist2Stop(stop,loc.getLatitude(),loc.getLongitude())));
            else {
                Location endpoint = stopsDB.getStopLocation(stop);
                int dist = (int)route.distanceAlongRoute(endpoint);
                addItem(new BusStopData(stopsDB.getStopName(stop), stop, getRoutesByStop(stop), dist));
            }
            return null;
        }
    }
}
