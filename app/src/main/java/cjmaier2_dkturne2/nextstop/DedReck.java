package cjmaier2_dkturne2.nextstop;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.ToggleButton;

import com.dropbox.client2.DropboxAPI;
import com.dropbox.client2.android.AndroidAuthSession;
import com.dropbox.client2.session.AppKeyPair;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;

//below for CSV


public class DedReck extends ActionBarActivity implements SensorEventListener, LocationListener {

    private final int capture_speed = 0;
    //0: DELAY_FASTEST
    //1: DELAY_GAME
    //3: DELAY_NORMAL
    //2: DELAY_UI
    private final int UPDATE_RATE = 2;

    private boolean ACCEL_ENABLED = false;
    private boolean GYRO_ENABLED = false;
    private boolean MAG_ENABLED = false;
    private boolean LOC_ENABLED = false;

    private boolean DB_ENABLED = false;
    private SharedPreferences dbSettings;
    private AppKeyPair appKeys;
    private AndroidAuthSession session;
    private DropboxAPI<AndroidAuthSession> mDBApi;


    private SensorManager mSensorManager;
    private LocationManager lManager;
    private List<String> lProviders;
    private Sensor mAccel;
    private Sensor mGyro;
    private Sensor mMag;

    private double start_time; //reference for time_elapsed
    private boolean time_elapsed_started = false; //to determine starting timestamp
    private int dedreck_itr = 0; //need 2 readings for velocity, 3 readings for position
    private double time_elapsed = 0;
    private boolean writer_created = false; //to determine whether to write to csv
    private BufferedWriter writer;
    private StringBuffer csv_text;

    //temp values for writing to CSV
    private float a_x = 0;
    private float a_y = 0;
    private float a_z = 0;
    private float g_x = 0;
    private float g_y = 0;
    private float g_z = 0;
    private float m_x = 0;
    private float m_y = 0;
    private float m_z = 0;
    private double lat = 0;
    private double lon = 0;

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
    private float p_x = 0, p_y = 0, p_z = 0;

    File textFile = null;

    private File rootdir;

    private String wfilename;

    private android.os.Handler guiHandler;
    private int guiInterval = 100; // 1000 mS

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        guiHandler = new android.os.Handler();
        setContentView(R.layout.activity_ded_reck);
        TextView testText = (TextView)findViewById(R.id.testText);
        mSensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        lManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        PackageManager manager = getPackageManager();
        ACCEL_ENABLED = manager.hasSystemFeature(PackageManager.FEATURE_SENSOR_ACCELEROMETER);
        GYRO_ENABLED = manager.hasSystemFeature(PackageManager.FEATURE_SENSOR_GYROSCOPE);
        MAG_ENABLED = manager.hasSystemFeature(PackageManager.FEATURE_SENSOR_COMPASS);
        LOC_ENABLED = lManager.isProviderEnabled(lManager.GPS_PROVIDER);

        if(!ACCEL_ENABLED)
        {
            findViewById(R.id.accelxlabel).setVisibility(View.INVISIBLE);
            findViewById(R.id.accelylabel).setVisibility(View.INVISIBLE);
            findViewById(R.id.accelzlabel).setVisibility(View.INVISIBLE);
            findViewById(R.id.axisxaccel).setVisibility(View.INVISIBLE);
            findViewById(R.id.axisyaccel).setVisibility(View.INVISIBLE);
            findViewById(R.id.axiszaccel).setVisibility(View.INVISIBLE);
        }
        else
        {
            mAccel = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        }
        if(!GYRO_ENABLED)
        {
            findViewById(R.id.gyroxlabel).setVisibility(View.INVISIBLE);
            findViewById(R.id.gyroylabel).setVisibility(View.INVISIBLE);
            findViewById(R.id.gyrozlabel).setVisibility(View.INVISIBLE);
            findViewById(R.id.axisxgyro).setVisibility(View.INVISIBLE);
            findViewById(R.id.axisygyro).setVisibility(View.INVISIBLE);
            findViewById(R.id.axiszgyro).setVisibility(View.INVISIBLE);
        }
        else
        {
            mGyro = mSensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE);
        }
        if(!MAG_ENABLED)
        {
            findViewById(R.id.magxlabel).setVisibility(View.INVISIBLE);
            findViewById(R.id.magylabel).setVisibility(View.INVISIBLE);
            findViewById(R.id.magzlabel).setVisibility(View.INVISIBLE);
            findViewById(R.id.axisxmag).setVisibility(View.INVISIBLE);
            findViewById(R.id.axisymag).setVisibility(View.INVISIBLE);
            findViewById(R.id.axiszmag).setVisibility(View.INVISIBLE);
        }
        else
        {
            mMag = mSensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);
        }
        if(!LOC_ENABLED)
        {
            findViewById(R.id.latitude).setVisibility(View.INVISIBLE);
            findViewById(R.id.longitude).setVisibility(View.INVISIBLE);
            findViewById(R.id.latlabel).setVisibility(View.INVISIBLE);
            findViewById(R.id.lonlabel).setVisibility(View.INVISIBLE);
        }
        else
        {
            lProviders = lManager.getProviders(true);
        }

        dbSettings = getSharedPreferences("DBSETTINGS",MODE_PRIVATE);

        if(dbSettings.contains("DB_APP_KEY") &&
                dbSettings.contains("DB_APP_SECRET") &&
                dbSettings.contains("DB_ACCESS_TOKEN"))
        {
            try {
                appKeys = new AppKeyPair(dbSettings.getString("DB_APP_KEY",""), dbSettings.getString("DB_APP_SECRET",""));
                session = new AndroidAuthSession(appKeys);
                mDBApi = new DropboxAPI<AndroidAuthSession>(session);
                mDBApi.getSession().setOAuth2AccessToken(dbSettings.getString("DB_ACCESS_TOKEN", ""));
                DB_ENABLED = true;
                testText.setText("Dropbox Enabled!");
            } catch(Exception ex)
            {
                Log.i("DBsetup", "ErrorInitializing", ex);
            }
        }
        //display sensor delay speed (for debugging)
        TextView senSpeed = (TextView)findViewById(R.id.testText);
        switch(UPDATE_RATE) {
            case 0:
                senSpeed.setText("DELAY_FASTEST");
                break;
            case 1:
                senSpeed.setText("DELAY_GAME");
                break;
            case 3:
                senSpeed.setText("DELAY_NORMAL");
                break;
            case 2:
                senSpeed.setText("DELAY_UI");
                break;
        }
    }

    @Override
    protected void onResume()
    {
        super.onResume();
        guiUpdate.run();
        // register this class as a listener for the orientation and
        // accelerometer sensors
        if(ACCEL_ENABLED)
        {
            mSensorManager.registerListener(this,
                    mAccel,
                    UPDATE_RATE);
        }
        if(GYRO_ENABLED)
        {
            mSensorManager.registerListener(this,
                    mGyro,
                    UPDATE_RATE);
        }
        if(MAG_ENABLED)
        {
            mSensorManager.registerListener(this,
                    mMag,
                    UPDATE_RATE);
        }
        if(LOC_ENABLED)
        {
            // This seems to cause issues since different providers have
            // different locations at the same time.
//            for(String provider:lProviders)
//                lManager.requestLocationUpdates(provider,50,0,this);
//
//            Location loc = null;
//
//            for(String provider:lProviders)
//            {
//                loc = lManager.getLastKnownLocation(provider);
//                if(loc != null)
//                    break;
//            }
            lManager.requestLocationUpdates(lManager.GPS_PROVIDER,50,10,this);
            Location loc = lManager.getLastKnownLocation(lManager.GPS_PROVIDER);
            if(loc != null)
            {
                lat = loc.getLatitude();
                lon = loc.getLongitude();
                EditText latval = (EditText) findViewById(R.id.latitude);
                EditText lonval = (EditText) findViewById(R.id.longitude);
                latval.setText(Double.toString(lat));
                lonval.setText(Double.toString(lon));
            }
        }
    }

    @Override
    protected void onPause() {
        // unregister listener
        super.onPause();
        guiHandler.removeCallbacks(guiUpdate);
        mSensorManager.unregisterListener(this);
        lManager.removeUpdates(this);
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_ded_reck, menu);
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

    Runnable guiUpdate = new Runnable() {
        @Override
        public void run() {
            guiHandler.postDelayed(guiUpdate, guiInterval);

            EditText axval = (EditText)findViewById(R.id.axisxaccel);
            EditText ayval = (EditText)findViewById(R.id.axisyaccel);
            EditText azval = (EditText)findViewById(R.id.axiszaccel);
            EditText gxval = (EditText)findViewById(R.id.axisxgyro);
            EditText gyval = (EditText)findViewById(R.id.axisygyro);
            EditText gzval = (EditText)findViewById(R.id.axiszgyro);
            EditText vxval = (EditText)findViewById(R.id.axisxvel);
            EditText vyval = (EditText)findViewById(R.id.axisyvel);
            EditText vzval = (EditText)findViewById(R.id.axiszvel);
            EditText pxval = (EditText)findViewById(R.id.axisxpos);
            EditText pyval = (EditText)findViewById(R.id.axisypos);
            EditText pzval = (EditText)findViewById(R.id.axiszpos);
            EditText mxval = (EditText)findViewById(R.id.axisxmag);
            EditText myval = (EditText)findViewById(R.id.axisymag);
            EditText mzval = (EditText)findViewById(R.id.axiszmag);
            EditText latval = (EditText)findViewById(R.id.latitude);
            EditText lonval = (EditText)findViewById(R.id.longitude);
            EditText time = (EditText)findViewById(R.id.time);

            if(ACCEL_ENABLED) {
                axval.setText(Float.toString(a_x));
                ayval.setText(Float.toString(a_y));
                azval.setText(Float.toString(a_z));
                vxval.setText(Float.toString(v_x));
                vyval.setText(Float.toString(v_y));
                vzval.setText(Float.toString(v_z));
                pxval.setText(Float.toString(p_x));
                pyval.setText(Float.toString(p_y));
                pzval.setText(Float.toString(p_z));
            }
            if(GYRO_ENABLED) {
//                gxval.setText(Float.toString(g_x));
//                gyval.setText(Float.toString(g_y));
//                gzval.setText(Float.toString(g_z));
            }
            if(MAG_ENABLED) {
                mxval.setText(Float.toString(m_x));
                myval.setText(Float.toString(m_y));
                mzval.setText(Float.toString(m_z));
            }
            if(LOC_ENABLED) {
                latval.setText(Double.toString(lat));
                lonval.setText(Double.toString(lon));
            }
            time.setText(Double.toString(time_elapsed));
        }
    };

    @Override
    public void onSensorChanged(SensorEvent event)
    {
        if (!time_elapsed_started) {
            time_elapsed_started = true;
            start_time = event.timestamp;
        }

        time_elapsed = (event.timestamp - start_time)/1000000.0;

        if(ACCEL_ENABLED)
        {
            if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER)
            {
                getAccelerometer(event);
                return;
            }
        }
        if(GYRO_ENABLED)
        {
            if (event.sensor.getType() == Sensor.TYPE_GYROSCOPE)
            {
                getGyroscope(event);
                return;
            }
        }
        if(MAG_ENABLED)
        {
            if (event.sensor.getType() == Sensor.TYPE_MAGNETIC_FIELD)
            {
                getMagnetometer(event);
                return;
            }
        }
    }

    @Override
    public void onLocationChanged(Location location) {
        lat = location.getLatitude();
        lon = location.getLongitude();
    }

    @Override
    public void onProviderEnabled(String provider) {}

    @Override
    public void onProviderDisabled(String provider) {}

    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy)
    {
        /* Unused */
    }

    public void toggleRecord(View view)
    {
        boolean recording = ((ToggleButton) view).isChecked();

        TextView testText = (TextView)findViewById(R.id.testText);

        //set this to !recording just to test
        if(recording) { //toggle to recording
            testText.setText("");
            csv_text = new StringBuffer();
            String state = Environment.getExternalStorageState();
            if(!state.equals(Environment.MEDIA_MOUNTED)) {
                testText.setText("No external storage mounted");
            }
            else
            {
                File externalDir = Environment.getExternalStorageDirectory();
                rootdir = new File(externalDir.getAbsolutePath() + "/NextStop-Tests");
                if(!rootdir.exists())
                {
                    try{
                        rootdir.mkdir();
                    } catch(SecurityException ex){
                        testText.setText("Something went wrong!" + ex.getMessage());
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
                        testText.setText("Something went wrong!?" + ex.getMessage());
                        return;
                    }
                }

                if(!writer_created)
                {
                    SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd_HHmmss", Locale.US);
                    wfilename = "cjmaier2_dkturne2_"+sdf.format(new Date())+".csv";
                    textFile = new File(dir, wfilename);
                    try {
                        writer = new BufferedWriter(new FileWriter(textFile));
                        csv_text.append("Timestamp (ms),Accel_x,Accel_y,Accel_z,Gyro_x,Gyro_y,Gyro_z,Mag_x,Mag_y,Mag_z,Lat,Lon\n");
                        writer_created = true;
                    } catch (IOException ex) {
                        testText.setText("Something went wrong!" + ex.getMessage());
                    }
                }
            }
        }

        else { //toggled to not recording
            if(writer_created)
            {
                File dir;
                writer_created = false;
                try {
                    writer.close();
                    // Write to Dropbox
                    if(DB_ENABLED)
                    {
                        dir = new File(rootdir.getAbsolutePath());
                        File file = new File(dir,wfilename);
                        UploadDropbox upload = new UploadDropbox(this, mDBApi, "/" , file);
                        upload.execute();
                    }
                } catch (IOException ex) {
                    testText.setText("Something went wrong!(IO)" + ex.getMessage());
                }
            }
        }
    }

    public void makeFileDiscoverable(File file, Context context){
        MediaScannerConnection.scanFile(context, new String[]{file.getPath()}, null, null);
        context.sendBroadcast(new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE,
                Uri.fromFile(file)));
    }

    private void getAccelerometer(SensorEvent event)
    {
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

        a_x = values[0]-a_x_off;
        a_y = values[1]-a_y_off;
        a_z = values[2]-a_z_off;

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
        }
        else if (dedreck_itr == 2){ //enough readings to get velocity and position
            dedreck_itr = 3;
            if(Math.abs(a_x - a_x_prev) > 0.1) v_x = (a_x - a_x_prev)*dt;
            if(Math.abs(a_y - a_y_prev) > 0.1) v_y = (a_y - a_y_prev)*dt;
            if(Math.abs(a_z - a_z_prev) > 0.1) v_z = (a_z - a_z_prev)*dt;
            if(Math.abs(a_x - a_x_prev) > 0.1) p_x += (v_x - v_x_prev)*dt;
            if(Math.abs(a_y - a_y_prev) > 0.1) p_y += (v_y - v_y_prev)*dt;
            if(Math.abs(a_z - a_z_prev) > 0.1) p_z += (v_z - v_z_prev)*dt;
        }
        else {
            if(Math.abs(a_x - a_x_prev) > 0.1) v_x += a_x*dt;
            if(Math.abs(a_y - a_y_prev) > 0.1) v_y += a_y*dt;
            if(Math.abs(a_z - a_z_prev) > 0.1) v_z += a_z*dt;
            if(Math.abs(a_x - a_x_prev) > 0.1) p_x += v_x*dt + 0.5f*a_x*dt*dt;
            if(Math.abs(a_y - a_y_prev) > 0.1) p_y += v_y*dt + 0.5f*a_y*dt*dt;
            if(Math.abs(a_z - a_z_prev) > 0.1) p_z += v_z*dt + 0.5f*a_z*dt*dt;
        }

        time_prev = event.timestamp;
        a_x_prev = a_x;
        a_y_prev = a_y;
        a_z_prev = a_z;
        v_x_prev = v_x;
        v_y_prev = v_y;
        v_z_prev = v_z;

        //writing to CSV
        if(writer_created) {
            String newentry = String.valueOf(time_elapsed) + "," + Float.toString(a_x) + "," + Float.toString(a_y) + "," + Float.toString(a_z) + ","
                    + Float.toString(g_x) + "," + Float.toString(g_y) + "," + Float.toString(g_z) + ","
                    + Float.toString(m_x) + "," + Float.toString(m_y) + "," + Float.toString(m_z) + ","
                    + Double.toString(lat) + "," + Double.toString(lon) + "\n";
            csv_text.append(newentry);
            try {
                writer.write(csv_text.toString());
                makeFileDiscoverable(textFile, this);
                csv_text.delete(0,csv_text.length()); //prevent writing same thing over and over and over and over and over and over and over
                // and over and over and over and over and over and over and over and over and over and
                // over and over and over and over and over and over and over and over and over again
            } catch (IOException ex){
                //do something?
            }
        }
    }

    private void getGyroscope(SensorEvent event)
    {
        float[] values = event.values;
        g_x = values[0];
        g_y = values[1];
        g_z = values[2];
    }

    private void getMagnetometer(SensorEvent event)
    {
        float[] values = event.values;
        m_x = values[0];
        m_y = values[1];
        m_z = values[2];
    }
}
