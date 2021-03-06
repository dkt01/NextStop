package cjmaier2_dkturne2.nextstop;

import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.database.sqlite.SQLiteOpenHelper;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.HashSet;
import java.util.List;

public class DataBaseHelper_StopTimes extends SQLiteOpenHelper {

    private static String DB_NAME = "stop_times.sqlite";
    private String DB_PATH;

    private final String TABLE_NAME = "stop_times";

    private SQLiteDatabase myDataBase;

    private final Context myContext;

    private static DataBaseHelper_StopTimes mInstance = null;

    /**
     * Constructor
     * Takes and keeps a reference of the passed context in order to access to the application assets and resources.
     */
    private DataBaseHelper_StopTimes(Context context) {

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

    public static DataBaseHelper_StopTimes getInstance(Context ctx) {

        // Use the application context, which will ensure that you
        // don't accidentally leak an Activity's context.
        // See this article for more information: http://bit.ly/6LRzfx
        if (mInstance == null) {
            mInstance = new DataBaseHelper_StopTimes(ctx.getApplicationContext());
        }
        return mInstance;
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

    public List<String> getTrips(String stopID) {
        List<String> retval = new ArrayList<>();
        Cursor c = myDataBase.rawQuery("select distinct trip_id from "+TABLE_NAME+" where stop_id='"+stopID+"'",null);
        while (c.moveToNext())
        {
            retval.add(c.getString(0));
        }
        c.close();
        return retval;
    }

    public List<String> getTripCandidates(String stopID, GregorianCalendar now) {
        SimpleDateFormat outFormat = new SimpleDateFormat("HH:mm:ss");
        ArrayList<String> retval = new ArrayList();
        now.add(Calendar.MINUTE,-5);
        String preVal = outFormat.format(now.getTime());
        now.add(Calendar.MINUTE,10);
        String postVal = outFormat.format(now.getTime());
        Cursor c = myDataBase.rawQuery("SELECT * FROM "+TABLE_NAME+" WHERE stop_id='"+stopID+"' " +
                                       "AND strftime( '%H:%M:%S', departure_time ) >= strftime( '%H:%M:%S', '"+preVal+"' ) " +
                                       "AND strftime( '%H:%M:%S', departure_time ) <= strftime( '%H:%M:%S', '"+postVal+"' ) ",null);
        while (c.moveToNext())
        {
            retval.add(c.getString(0));
        }
        c.close();
        return retval;
    }

    public List<String> getUpcomingStops(String stopID, String tripID) {
        ArrayList<String> retval = new ArrayList();
        // Get trip sequence number first
        Cursor c = myDataBase.rawQuery("SELECT * FROM "+TABLE_NAME+" WHERE stop_id='"+stopID+"' " +
                                       "AND trip_id='"+tripID+"'",null);
        if(c.moveToFirst())
        {
            int seqnum = c.getInt(4);
            Cursor d = myDataBase.rawQuery("SELECT * FROM "+TABLE_NAME+" WHERE trip_id='"+tripID+"' " +
                    "AND stop_sequence>"+Integer.toString(seqnum)+" ORDER BY stop_sequence ASC",null);
            while (d.moveToNext())
            {
                retval.add(d.getString(3));
            }
            d.close();
        }
        c.close();
        return retval;
    }
}