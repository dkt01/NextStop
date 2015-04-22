package cjmaier2_dkturne2.nextstop;

import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;

public class StepCount extends ActionBarActivity implements SensorEventListener{

    public static final float STEP_SIZE = 2.5f; //human's average step size

    private int steps = 0;

    private SensorManager mSensorManager;
    private Sensor mAccel;
    private Sensor mRotVec;

    private float curx = 0;
    private float cury = 0;
    private float curz = 0;
    private float prevx = 0;
    private float prevy = 0;
    private float prevz = 0;

    private double base_time = -1;

    private final double STEP_THRESH = 0.8;
    private final int DEBOUNCE_MS = 500;

    private int timer = 0;
    private int set_init_angle = 1; //set init angle on first run through
    private float init_angle = 0.0f; //reference for rotation angle
    private float prev_degrees = 0.0f; //previous "current degrees" for determining total degrees
    private float tot_degrees = 0.0f; //total degrees from origin

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_step_count);
        mSensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        mAccel = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        mRotVec = mSensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR);
    }

    @Override
    protected void onResume()
    {
        super.onResume();
        // register this class as a listener for the orientation and
        // accelerometer sensors
        mSensorManager.registerListener(this,
                mAccel,
                SensorManager.SENSOR_DELAY_FASTEST);
        mSensorManager.registerListener(this,
                mRotVec,
                SensorManager.SENSOR_DELAY_FASTEST);
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_step_count, menu);
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
        if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER)
        {
            getAccelerometer(event);
        }
        if (event.sensor.getType() == Sensor.TYPE_ROTATION_VECTOR)
        {
            getRotVec(event);
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }

    private void getAccelerometer(SensorEvent event)
    {
        EditText stepcount = (EditText)findViewById(R.id.numSteps);
        EditText num_feet = (EditText)findViewById(R.id.numFeet);

        prevx = curx;
        prevy = cury;
        prevz = curz;
        curx = event.values[0];
        cury = event.values[1];
        curz = event.values[2];

        if(base_time == -1)
        {
            base_time = (event.timestamp/1000000.0);
        }

        double timediff = (event.timestamp/1000000.0) - base_time;

        if(Math.abs(curz-prevz) > STEP_THRESH)
        {
            if(timediff > DEBOUNCE_MS)
            {
                steps++;
                stepcount.setText(Integer.toString(steps));
                base_time = (event.timestamp/1000000.0);
                num_feet.setText(Float.toString(steps*STEP_SIZE));
            }
        }
    }

    private void getRotVec(SensorEvent event)
    {
        float scalar = 180.0f*event.values[3];
        float cur_degrees; //current degrees from reference

        float[] rotMat = new float[9];
        float[] vals = new float[3];
        SensorManager.getRotationMatrixFromVector(rotMat,
                event.values);
        SensorManager
                .remapCoordinateSystem(rotMat,
                        SensorManager.AXIS_X, SensorManager.AXIS_Y,
                        rotMat);
        SensorManager.getOrientation(rotMat, vals);
        //from http://stackoverflow.com/questions/14740808/android-problems-calculating-the-orientation-of-the-device
        float azimuth = (float) Math.toDegrees(vals[0]); // in degrees [-180, +180]
        float pitch = (float) Math.toDegrees(vals[1]);
        float roll = (float) Math.toDegrees(vals[2]);

        if(set_init_angle == 1){
            set_init_angle = 0;
            init_angle = azimuth;
            tot_degrees = 0.0f;
            prev_degrees = 0.0f;
        }

        if(timer++ == 10) { //timer for more visible values
            timer = 0;

            EditText tmpText = (EditText)findViewById(R.id.tmp);
            tmpText.setText(Float.toString(azimuth));
            EditText tmpText1 = (EditText)findViewById(R.id.tmp1);
            tmpText1.setText(Float.toString(pitch));
            EditText tmpText2 = (EditText)findViewById(R.id.tmp2);
            tmpText2.setText(Float.toString(roll));
            EditText tmpText3 = (EditText)findViewById(R.id.tmp3);
            tmpText3.setText(Float.toString(scalar));

            EditText degrees_text = (EditText)findViewById(R.id.numDegrees);

            //adjust "current degrees" such that it ranges from -180 (left) to 180 (right)
            cur_degrees = azimuth-init_angle;
            if (cur_degrees > 180.0) {
                cur_degrees -= 360.0f;
            }
            else if (cur_degrees < -180.0) {
                cur_degrees += 360.0f;
            }
            degrees_text.setText(Float.toString(cur_degrees));

            float degree_change;
            if (cur_degrees > 90 && prev_degrees < -90) { //crossing from -180 to 180
                degree_change = Math.abs((cur_degrees - 360) - prev_degrees);
            }
            else if (prev_degrees > 90 && cur_degrees < -90) { //crossing from 180 to -180
                degree_change = Math.abs(cur_degrees - (prev_degrees - 360));
            }
            else {
                degree_change = Math.abs(cur_degrees - prev_degrees);
            }
            if (degree_change > 0.5f) { //only update if experience this degree change
                tot_degrees += degree_change;
            }
            prev_degrees = cur_degrees;

            EditText tot_deg_text = (EditText)findViewById(R.id.totalNumDegrees);
            tot_deg_text.setText(Float.toString(tot_degrees));
        }
    }

    public void resetCount(View view)
    {
        steps = 0;
        set_init_angle = 1; //reset init angle
        EditText stepcount = (EditText)findViewById(R.id.numSteps);
        stepcount.setText(Integer.toString(steps));
        EditText num_feet = (EditText)findViewById(R.id.numFeet);
        num_feet.setText(Float.toString(steps*STEP_SIZE));
    }
}
