package cjmaier2_dkturne2.nextstop;

import android.media.MediaRecorder;
import android.os.Bundle;
import android.os.Handler;
import android.support.v7.app.ActionBarActivity;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.EditText;

import java.io.IOException;

public class MicTest extends ActionBarActivity {
    private int updateTime = 100; // 100ms
    private MediaRecorder mic;
    private Handler sampler;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_mic_test);
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
        sampler = new Handler();
    }

    @Override
    protected void onResume()
    {
        super.onResume();
        // register this class as a listener for the orientation and
        // accelerometer sensors
        mic.start();
        micSampler.run();
    }

    @Override
    protected void onPause() {
        // unregister listener
        super.onPause();
        mic.stop();
        sampler.removeCallbacks(micSampler);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_mic_test, menu);
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

    private Runnable micSampler = new Runnable() {
        @Override
        public void run() {
            double value = getAmplitude();
            EditText mval = (EditText)findViewById(R.id.mout);
            mval.setText(Double.toString(value));
            sampler.postDelayed(micSampler, updateTime);
        }
    };

    private double getAmplitude() {
        if (mic != null)
            return  mic.getMaxAmplitude();
        else
            return 0;
    }
}
