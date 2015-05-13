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
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

public class DataBaseHelper_Routes extends SQLiteOpenHelper {

    private static String DB_NAME = "routes.sqlite";
    private String DB_PATH;

    private final String TABLE_NAME = "routes";

    private SQLiteDatabase myDataBase;

    private final Context myContext;

    private static DataBaseHelper_Routes mInstance = null;

    /**
     * Constructor
     * Takes and keeps a reference of the passed context in order to access to the application assets and resources.
     */
    private DataBaseHelper_Routes(Context context) {

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

    public static DataBaseHelper_Routes getInstance(Context ctx) {

        // Use the application context, which will ensure that you
        // don't accidentally leak an Activity's context.
        // See this article for more information: http://bit.ly/6LRzfx
        if (mInstance == null) {
            mInstance = new DataBaseHelper_Routes(ctx.getApplicationContext());
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

    public List<Route> getRouteColors(List<String> routeIDs) {
        HashSet<Route> retval = new HashSet<>();
        int routeName;
        for(String routeID:routeIDs) {
            Cursor c = myDataBase.rawQuery("select * from "+TABLE_NAME+" where route_id='"+routeID+"'",null);
            if(c.moveToFirst())
            {
                routeName = c.getInt(8);
                switch (routeName) {
                    case 1:
                    case 100:
                        retval.add(Route.YELLOW);
                        break;
                    case 2:
                    case 20:
                        retval.add(Route.RED);
                        break;
                    case 3:
                    case 30:
                        retval.add(Route.LAVENDER);
                        break;
                    case 4:
                        retval.add(Route.BLUE);
                        break;
                    case 5:
                    case 50:
                        retval.add(Route.GREEN);
                        break;
                    case 6:
                        retval.add(Route.ORANGE);
                        break;
                    case 7:
                    case 70:
                        retval.add(Route.GREY);
                        break;
                    case 8:
                        retval.add(Route.BRONZE);
                        break;
                    case 9:
                        retval.add(Route.BROWN);
                        break;
                    case 10:
                        retval.add(Route.GOLD);
                        break;
                    case 11:
                    case 110:
                        retval.add(Route.RUBY);
                        break;
                    case 12:
                    case 120:
                        retval.add(Route.TEAL);
                        break;
                    case 13:
                    case 130:
                        retval.add(Route.SILVER);
                        break;
                    case 14:
                        retval.add(Route.NAVY);
                        break;
                    case 16:
                        retval.add(Route.PINK);
                        break;
                    case 22:
                    case 220:
                        retval.add(Route.ILLINI);
                        break;
                    case 27:
                    case 270:
                        retval.add(Route.AIRBUS);
                        break;
                    case 180:
                        retval.add(Route.LIME);
                        break;
                    default:
                        break;
                }
            }
            c.close();
        }
        return new ArrayList<>(retval);
    }
}