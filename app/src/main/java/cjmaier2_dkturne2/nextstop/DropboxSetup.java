package cjmaier2_dkturne2.nextstop;

import android.content.SharedPreferences;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;

import com.dropbox.client2.DropboxAPI;
import com.dropbox.client2.android.AndroidAuthSession;
import com.dropbox.client2.session.AppKeyPair;


public class DropboxSetup extends ActionBarActivity {

    private DropboxAPI<AndroidAuthSession> mDBApi;
    private static final String APP_KEY = "ff31c20j0ebfp8q";
    private static final String APP_SECRET = "2yfptvmy8cppbt4";

    private AppKeyPair appKeys;
    private AndroidAuthSession session;

    private SharedPreferences dbSettings;
    private SharedPreferences.Editor dbSettingsEditor;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_dropbox_setup);

        dbSettings = getSharedPreferences("DBSETTINGS",MODE_PRIVATE);
        dbSettingsEditor = dbSettings.edit();

        appKeys = new AppKeyPair(APP_KEY, APP_SECRET);

        if(!dbSettings.contains("DB_APP_KEY"))
        {
            dbSettingsEditor.putString("DB_APP_KEY", APP_KEY);
            dbSettingsEditor.commit();
        }
        if(!dbSettings.contains("DB_APP_SECRET"))
        {
            dbSettingsEditor.putString("DB_APP_SECRET", APP_SECRET);
            dbSettingsEditor.commit();
        }

        session = new AndroidAuthSession(appKeys);
        mDBApi = new DropboxAPI<AndroidAuthSession>(session);
    }

    @Override
    protected void onResume() {
        super.onResume();
        TextView debugText = (TextView)findViewById(R.id.message);

        if(dbSettings.contains("DB_ACCESS_TOKEN"))
            debugText.setText("Currently registered!");
        if (mDBApi.getSession().authenticationSuccessful()) {
            try {
                // Required to complete auth, sets the access token on the session
                mDBApi.getSession().finishAuthentication();

                String accessToken = mDBApi.getSession().getOAuth2AccessToken();

                dbSettingsEditor.putString("DB_ACCESS_TOKEN",accessToken);

                debugText.setText("Successfully set up!");

                dbSettingsEditor.commit();

            } catch (IllegalStateException e) {
                Log.i("DbAuthLog", "Error authenticating", e);
                debugText.setText("Authentication error: " + e.getMessage());
            }
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_dropbox_setup, menu);
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

    public void runAuthentication(View view)
    {
        mDBApi.getSession().startOAuth2Authentication(DropboxSetup.this);
    }

    public void cheatAuthentication(View view)
    {
        TextView debugText = (TextView)findViewById(R.id.message);
        dbSettingsEditor.putString("DB_ACCESS_TOKEN","L4shDiI9TkMAAAAAAAByb7wBunQONXRfNylL3qgywb-gmF8eaXqyTp-m3uY7IWOi");
        debugText.setText("Successfully set up!");
        dbSettingsEditor.commit();
    }
}
