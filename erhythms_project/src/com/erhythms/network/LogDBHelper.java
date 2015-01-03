package com.erhythms.network;

import java.util.ArrayList;

import com.erhythms.logdata.CallPhoneBean;
import com.erhythms.logdata.SMSMessageBean;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

public class LogDBHelper extends SQLiteOpenHelper{
	
	//the instance of the helper and the database
	private static LogDBHelper dbInstance;
	
	//context is used to get application data
	private Context appcontext;
	
	// All Static variables
    // Database Version
    private static final int DATABASE_VERSION = 1;
 
    // Database Name
    private static final String DATABASE_NAME = "log_database";
 
    // Call and text log table name
    private static final String TABLE_CALL = "call_log";
    private static final String TABLE_TEXT = "text_log";
 
    // Call log Table Columns names
    private static final String CALL_ID = "call_id";
    private static final String CALL_NAME = "name";
    private static final String CALL_PH_NO = "phone_number";
    private static final String CALL_DURATION = "duration";
    private static final String CALL_TYPE = "type";
    private static final String CALL_DAY = "days"; //Number of previous days the call occurred
    
    // Text log Table Columns names
    private static final String TEXT_ID = "text_id";
    private static final String TEXT_NAME = "name";
    private static final String TEXT_PH_NO = "phone_number";
    private static final String TEXT_TYPE = "type";
    private static final String TEXT_DAY = "days"; //Number of previous days the text occurred
 
    private static final String CREATE_CALLLOG_TABLE = "CREATE TABLE " + TABLE_CALL + " ("
            + CALL_ID + " INTEGER PRIMARY KEY AUTOINCREMENT," + CALL_NAME + " TEXT,"
            + CALL_PH_NO + " TEXT," + CALL_DURATION + " INTEGER," + 
            CALL_TYPE + " TEXT," + CALL_DAY + " INTEGER" + ")";
    
    private static final String CREATE_TEXTLOG_TABLE = "CREATE TABLE " + TABLE_TEXT + " ("
            + TEXT_ID + " INTEGER PRIMARY KEY AUTOINCREMENT," + TEXT_NAME + " TEXT,"
            + TEXT_PH_NO + " TEXT," + TEXT_TYPE + " TEXT," + TEXT_DAY 
            + " INTEGER" + ")";
    
    
    private LogDBHelper(Context context) {
    	
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    	this.appcontext = context;
    }
 
    
    // Get the instance of the database
    public static LogDBHelper getInstance(Context context) {

        // Use the application context, which will ensure that you 
        // don't accidentally leak an Activity's context.
        if (dbInstance == null) {
        	
        	Log.v("dbquery","Generated new database");
        	
        	dbInstance = new LogDBHelper(context);
        }
        return dbInstance;
      }
    
    
    // Creating Tables and importing the real time call logs from phone
    @Override
    public void onCreate(SQLiteDatabase db) {

    	Log.v("dbquery","Creating new database");
    	
    	// Drop older table if existed
    	db.execSQL("DROP TABLE IF EXISTS " + TABLE_CALL);
    	db.execSQL("DROP TABLE IF EXISTS " + TABLE_TEXT);
        
    	db.execSQL(CREATE_CALLLOG_TABLE);
    	db.execSQL(CREATE_TEXTLOG_TABLE);
    	
    }
 
    // Upgrading database
    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        
    	Log.v("dbquery","Updating database");
    	
    	// Drop older table if existed
    	db.execSQL("DROP TABLE IF EXISTS " + TABLE_CALL);
    	db.execSQL("DROP TABLE IF EXISTS " + TABLE_TEXT);
 
        // Create tables again
        onCreate(db);
    }
    
    public void updateAllLogs(){
    	
    	SQLiteDatabase db = this.getWritableDatabase();
    	
    	// Drop older table if existed
    	db.execSQL("DROP TABLE IF EXISTS " + TABLE_CALL);
    	db.execSQL("DROP TABLE IF EXISTS " + TABLE_TEXT);
        
    	db.execSQL(CREATE_CALLLOG_TABLE);
    	db.execSQL(CREATE_TEXTLOG_TABLE);
    	
	    //Initiate a new GetLogData instance and pass in the context
	    GetLogData gtlog = new GetLogData(appcontext);
	    
	    //The following method insert all Call Log to the database
	    ArrayList<CallPhoneBean> calllogs = gtlog.GetCallLog();
	    
	    for (CallPhoneBean calllog : calllogs){
	    	
	    	ContentValues call_values = new ContentValues();
	        call_values.put(CALL_NAME, calllog.getPhonename()); 
	        call_values.put(CALL_PH_NO, calllog.getPhonenumber()); 
	        call_values.put(CALL_DURATION, (int)calllog.getCallsecondes()); 
	        call_values.put(CALL_TYPE, calllog.getCalltype()); 
	        call_values.put(CALL_DAY, calllog.getDays()); 
	    	
	        // Inserting Row
	        db.insert(TABLE_CALL, null, call_values);
	    }
	    
	    //The following method insert all Text Log to the database
	    ArrayList<SMSMessageBean> textlogs = gtlog.GetTextLog();
	    
	    for (SMSMessageBean smslog : textlogs){
	    	
	    	ContentValues sms_values = new ContentValues();
	        sms_values.put(TEXT_NAME, smslog.getName()); 
	        sms_values.put(TEXT_PH_NO, smslog.getPhoneNumber()); 
	        sms_values.put(TEXT_TYPE, smslog.getType()); 
	        sms_values.put(TEXT_DAY, smslog.getDays()); 
	        
	        // Inserting Row
	        db.insert(TABLE_TEXT, null, sms_values);
	    }
	    
    }
    
      // TESTING ONLY
	  public void printAllCalls(){
		  
		  SQLiteDatabase db = this.getReadableDatabase();
		  
		  // Select based on the most or the least criteria
		  String callQuery = "SELECT * " + "FROM "+TABLE_CALL;
	    		
		  Cursor cursor = db.rawQuery(callQuery, null);
		      
		  Log.v("dblist","================================================");
		  Log.v("dblist","CALL LOG DATABASE");
		  Log.v("dblist","================================================");
		      
		   // looping through all rows and adding to list
		      if (cursor.moveToFirst()) {
		            do {
		            	
		                String name = cursor.getString(1);
		                String phone_number = cursor.getString(2);
		                int duration = cursor.getInt(3);
		                String type = cursor.getString(4);
		                int num_of_days = cursor.getInt(5);
		               
		                Log.v("dblist",name+", "+phone_number+", "+duration+", "+type+", "+num_of_days);
		               
		            } while (cursor.moveToNext());
		            
		      } 
	
	    }
	  
	  // TESTING ONLY
	  public void printAllTexts(){
		  
		  SQLiteDatabase db = this.getReadableDatabase();
		  
		  // Select based on the most or the least criteria
		  String callQuery = "SELECT * " + "FROM "+TABLE_TEXT;
	    		
		  Cursor cursor = db.rawQuery(callQuery, null);
		      
		  Log.v("dblist","================================================");
		  Log.v("dblist","TEXT LOG DATABASE");
		  Log.v("dblist","================================================");
		      
		   // looping through all rows and adding to list
		      if (cursor.moveToFirst()) {
		            do {
		                String name = cursor.getString(1);
		                String phone_number = cursor.getString(2);
		                String type = cursor.getString(3);
		                int num_of_days = cursor.getInt(4);
		               
		                Log.v("dblist",name+", "+phone_number+", "+type+", "+num_of_days);
		               
		            } while (cursor.moveToNext());
		            
		      } 
	
	    }
    
}

