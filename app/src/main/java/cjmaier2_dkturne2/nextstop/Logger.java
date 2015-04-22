package cjmaier2_dkturne2.nextstop;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.media.MediaRecorder;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.net.wifi.WifiManager;
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
import java.util.Locale;

//below for CSV


public class Logger extends ActionBarActivity implements SensorEventListener{

    private final int UPDATE_RATE = SensorManager.SENSOR_DELAY_NORMAL;

    private boolean ACCEL_ENABLED = false;
    private boolean GYRO_ENABLED = false;
    private boolean MAG_ENABLED = false;
    private boolean LIGHT_ENABLED = false;

    private boolean DB_ENABLED = false;
    private SharedPreferences dbSettings;
    private AppKeyPair appKeys;
    private AndroidAuthSession session;
    private DropboxAPI<AndroidAuthSession> mDBApi;


    private SensorManager mSensorManager;
    private Sensor mAccel;
    private Sensor mGyro;
    private Sensor mMag;
    private Sensor mLight;

    private MediaRecorder mic;
    private WifiManager wifi;
    private BroadcastReceiver receiver;

    private double start_time; //reference for time_elapsed
    private boolean time_elapsed_started = false; //to determine starting timestamp
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
    private float light = 0;
    private int rssi = 0;
    private double micv = 0;
    File textFile = null;

    private File rootdir;

    private String wfilename;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_logger);
        TextView testText = (TextView)findViewById(R.id.testText);
        mSensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        PackageManager manager = getPackageManager();
        ACCEL_ENABLED = manager.hasSystemFeature(PackageManager.FEATURE_SENSOR_ACCELEROMETER);
        GYRO_ENABLED = manager.hasSystemFeature(PackageManager.FEATURE_SENSOR_GYROSCOPE);
        MAG_ENABLED = manager.hasSystemFeature(PackageManager.FEATURE_SENSOR_COMPASS);
        LIGHT_ENABLED = manager.hasSystemFeature(PackageManager.FEATURE_SENSOR_LIGHT);

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
        if(!LIGHT_ENABLED)
        {
            findViewById(R.id.alightlabel).setVisibility(View.INVISIBLE);
            findViewById(R.id.alight).setVisibility(View.INVISIBLE);
        }
        else
        {
            mLight = mSensorManager.getDefaultSensor(Sensor.TYPE_LIGHT);
        }

        wifi = (WifiManager)getSystemService(Context.WIFI_SERVICE);
        if(!wifi.isWifiEnabled()){
            if(wifi.getWifiState() != WifiManager.WIFI_STATE_ENABLING){
                wifi.setWifiEnabled(true);
            }
        }

        mic = new MediaRecorder();
        mic.setAudioSource(MediaRecorder.AudioSource.MIC);
        mic.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP);
        mic.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB);
        mic.setOutputFile("/dev/null");
        try {
            mic.prepare();
        } catch (IOException e) {
            e.printStackTrace();
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

    }

    @Override
    protected void onResume()
    {
        super.onResume();
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
        if(LIGHT_ENABLED)
        {
            mSensorManager.registerListener(this,
                    mLight,
                    UPDATE_RATE);
        }

        wifi.startScan();
        receiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                wifi.startScan();
                rssi = wifi.getConnectionInfo().getRssi();
                EditText wifival = (EditText)findViewById(R.id.wifi);
                wifival.setText(Float.toString(rssi));
            }
        };
        registerReceiver(receiver, new IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION));

        mic.start();
    }

    @Override
    protected void onPause() {
        // unregister listener
        super.onPause();
        mSensorManager.unregisterListener(this);
        unregisterReceiver(receiver);
        mic.stop();
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_logger, menu);
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
    public void onSensorChanged(SensorEvent event)
    {
        if (!time_elapsed_started) {
            time_elapsed_started = true;
            start_time = event.timestamp;
        }
        EditText time = (EditText)findViewById(R.id.time);
        time_elapsed = (event.timestamp - start_time)/1000000.0;
        time.setText(Double.toString(time_elapsed));

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
        if(LIGHT_ENABLED)
        {
            if(event.sensor.getType() == Sensor.TYPE_LIGHT)
            {
                getAmbientLight(event);
            }
        }
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
                rootdir = new File(externalDir.getAbsolutePath() + "/Trajectory-App-Tests");
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
                    wfilename = "cjmaier2_dkturne2_"+sdf.format(new Date())+"_PA2.csv";
                    textFile = new File(dir, wfilename);
                    try {
                        writer = new BufferedWriter(new FileWriter(textFile));
                        csv_text.append("Timestamp (ms),Accel_x,Accel_y,Accel_z,Gyro_x,Gyro_y,Gyro_z,Mag_x,Mag_y,Mag_z,Light intensity,WiFi,Sound\n");
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
                } finally {
                    writer_created = false;
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
        a_x = values[0];
        a_y = values[1];
        a_z = values[2];
        EditText xval = (EditText)findViewById(R.id.axisxaccel);
        EditText yval = (EditText)findViewById(R.id.axisyaccel);
        EditText zval = (EditText)findViewById(R.id.axiszaccel);

        xval.setText(Float.toString(a_x));
        yval.setText(Float.toString(a_y));
        zval.setText(Float.toString(a_z));

        micv = mic.getMaxAmplitude();
        EditText micval = (EditText)findViewById(R.id.mic);
        micval.setText(Double.toString(micv));

        if(writer_created) {
            String newentry = String.valueOf(time_elapsed) + "," + Float.toString(a_x) + "," + Float.toString(a_y) + "," + Float.toString(a_z) + ","
                    + Float.toString(g_x) + "," + Float.toString(g_y) + "," + Float.toString(g_z) + ","
                    + Float.toString(m_x) + "," + Float.toString(m_y) + "," + Float.toString(m_z) + "," + Float.toString(light) + ","
                    + Integer.toString(rssi) + "," + Double.toString(micv) + "\n";
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
        EditText xval = (EditText)findViewById(R.id.axisxgyro);
        EditText yval = (EditText)findViewById(R.id.axisygyro);
        EditText zval = (EditText)findViewById(R.id.axiszgyro);

        xval.setText(Float.toString(g_x));
        yval.setText(Float.toString(g_y));
        zval.setText(Float.toString(g_z));
    }

    private void getMagnetometer(SensorEvent event)
    {
        float[] values = event.values;
        m_x = values[0];
        m_y = values[1];
        m_z = values[2];
        EditText xval = (EditText)findViewById(R.id.axisxmag);
        EditText yval = (EditText)findViewById(R.id.axisymag);
        EditText zval = (EditText)findViewById(R.id.axiszmag);

        xval.setText(Float.toString(m_x));
        yval.setText(Float.toString(m_y));
        zval.setText(Float.toString(m_z));
    }

    private void getAmbientLight(SensorEvent event)
    {
        light = event.values[0];
        EditText lval = (EditText)findViewById(R.id.alight);

        lval.setText(Float.toString(light));
    }
}
