package cjmaier2_dkturne2.nextstop;

import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.database.sqlite.SQLiteOpenHelper;
import android.location.Location;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.PriorityQueue;

public class DataBaseHelper_Stops extends SQLiteOpenHelper {

    private static String DB_NAME = "stops.sqlite";
    private String DB_PATH;

    private final String TABLE_NAME = "stops";

    private SQLiteDatabase myDataBase;

    private final Context myContext;

    /**
     * Constructor
     * Takes and keeps a reference of the passed context in order to access to the application assets and resources.
     */
    public DataBaseHelper_Stops(Context context) {

        super(context, DB_NAME, null, 1);
        this.myContext = context;
        DB_PATH = "/data/data/"
                + myContext.getApplicationContext().getPackageName()
                + "/databases/";
        try {
            createDataBase();
            openDataBase();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Creates a empty database on the system and rewrites it with your own database.
     * */
    public void createDataBase() throws IOException{

        boolean dbExist = checkDataBase();

        if(dbExist){
            //do nothing - database already exist
        }else{

            //By calling this method and empty database will be created into the default system path
            //of your application so we are gonna be able to overwrite that database with our database.
            this.getReadableDatabase();

            try {

                copyDataBase();

            } catch (IOException e) {

                throw new Error("Error copying database");

            }
        }

    }

    /**
     * Check if the database already exist to avoid re-copying the file each time you open the application.
     * @return true if it exists, false if it doesn't
     */
    private boolean checkDataBase(){

        SQLiteDatabase checkDB = null;

        try{
            String myPath = DB_PATH + DB_NAME;
            checkDB = SQLiteDatabase.openDatabase(myPath, null, SQLiteDatabase.OPEN_READONLY);

        }catch(SQLiteException e){

            //database doesn't exist yet.

        }

        if(checkDB != null){

            checkDB.close();

        }

        return checkDB != null;
    }

    /**
     * Copies your database from your local assets-folder to the just created empty database in the
     * system folder, from where it can be accessed and handled.
     * This is done by transfering bytestream.
     * */
    private void copyDataBase() throws IOException {

        //Open your local db as the input stream
        InputStream myInput = myContext.getAssets().open(DB_NAME);

        // Path to the just created empty db
        String outFileName = DB_PATH + DB_NAME;

        //Open the empty db as the output stream
        OutputStream myOutput = new FileOutputStream(outFileName);

        //transfer bytes from the inputfile to the outputfile
        byte[] buffer = new byte[1024];
        int length;
        while ((length = myInput.read(buffer))>0){
            myOutput.write(buffer, 0, length);
        }

        //Close the streams
        myOutput.flush();
        myOutput.close();
        myInput.close();

    }

    public void openDataBase() throws SQLException {

        //Open the database
        String myPath = DB_PATH + DB_NAME;
        myDataBase = SQLiteDatabase.openDatabase(myPath, null, SQLiteDatabase.OPEN_READONLY);

    }

    @Override
    public synchronized void close() {

        if(myDataBase != null)
            myDataBase.close();

        super.close();

    }

    @Override
    public void onCreate(SQLiteDatabase db) {

    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {

    }

    public List<String> nearestStop(double lat, double lon) {
        float MAX_DIST = 50; // Only return stops under 50 meters away
        ArrayList<String> retval = new ArrayList();
        PriorityQueue<pqEntry> pq = new PriorityQueue();
        Cursor c = myDataBase.rawQuery("select * from "+TABLE_NAME,null);
        while (c.moveToNext())
        {
            float[] dist = {0};
            Location.distanceBetween(c.getDouble(4), c.getDouble(5), lat, lon, dist);
            pq.add(new pqEntry(dist[0],c.getString(0)));
        }
        c.close();
        retval.add(pq.remove().name);
        while(pq.peek().distance <= MAX_DIST)
            retval.add(pq.remove().name);
        return retval;
    }

    class pqEntry implements Comparable<pqEntry> {
        final float distance;
        final String name;

        public pqEntry(float d, String n) {
            distance = d;
            name = n;
        }

        public int compareTo(pqEntry other) {
            return distance < other.distance ? -1 : distance > other.distance ? 1 : 0;
        }
    }

    public String getStopName(String stop_id) {
        String retval = null;
        Cursor c = myDataBase.rawQuery("select * from "+TABLE_NAME+" where stop_id='"+stop_id+"'",null);
        if(c.moveToFirst())
        {
            retval = c.getString(2);
        }
        c.close();
        return retval;
    }

    public float getDist2Stop(String stop_id, double lat, double lon) {
        float[] retval = {-1};
        Cursor c = myDataBase.rawQuery("select * from "+TABLE_NAME+" where stop_id='"+stop_id+"'",null);
        if(c.moveToFirst())
        {
            Location.distanceBetween(c.getDouble(4), c.getDouble(5), lat, lon, retval);
        }
        c.close();
        return retval[0];
    }

    public Location getStopLocation(String stop_id) {
        Location retval = null;
        Cursor c = myDataBase.rawQuery("select * from "+TABLE_NAME+" where stop_id='"+stop_id+"'",null);
        if(c.moveToFirst())
        {
            retval = new Location("");
            retval.setLatitude(c.getDouble(4));
            retval.setLongitude(c.getDouble(5));
        }
        return retval;
    }
}