package cjmaier2_dkturne2.nextstop;

import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.content.Intent;
import android.view.View;
import android.content.pm.PackageManager;
import android.widget.Button;


public class MainActivity extends ActionBarActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        PackageManager manager = getPackageManager();
        Button accelButton = (Button)findViewById(R.id.atbutton);
        Button gyroButton = (Button)findViewById(R.id.gtbutton);
        Button magButton = (Button)findViewById(R.id.mtbutton);
        if(!manager.hasSystemFeature(PackageManager.FEATURE_SENSOR_ACCELEROMETER))
        {
            accelButton.setEnabled(false);
            accelButton.setTextColor(0x66FFFFFF);
        }
        if(!manager.hasSystemFeature(PackageManager.FEATURE_SENSOR_GYROSCOPE))
        {
            gyroButton.setEnabled(false);
            gyroButton.setTextColor(0x66FFFFFF);
        }
        if(!manager.hasSystemFeature(PackageManager.FEATURE_SENSOR_COMPASS))
        {
            magButton.setEnabled(false);
            magButton.setTextColor(0x66FFFFFF);
        }
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
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

    public void launchGyroTest(View view)
    {
        Intent intent = new Intent(this, GyroTest.class);
        startActivity(intent);
    }

    public void launchAccelTest(View view)
    {
        Intent intent = new Intent(this, AccelTest.class);
        startActivity(intent);
    }

    public void launchMagTest(View view)
    {
        Intent intent = new Intent(this, MagTest.class);
        startActivity(intent);
    }

    public void launchLogger(View view)
    {
        Intent intent = new Intent(this, Logger.class);
        startActivity(intent);
    }

    public void launchTracker(View view)
    {
        Intent intent = new Intent(this, StopTracker.class);
        startActivity(intent);
    }

    public void launchDedReck(View view)
    {
        Intent intent = new Intent(this, DedReck.class);
        startActivity(intent);
    }

    public void launchDropboxSetup(View view)
    {
        Intent intent = new Intent(this, DropboxSetup.class);
        startActivity(intent);
    }
}
