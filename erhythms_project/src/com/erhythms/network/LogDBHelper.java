package com.erhythms.network;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

import com.erhythms.eventbeans.Tie;
import com.erhythms.eventbeans.TieCriteria;
import com.erhythms.logdata.CallPhoneBean;
import com.erhythms.logdata.SMSMessageBean;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.net.Uri;
import android.util.Log;

public class LogDBHelper extends SQLiteOpenHelper{
	
	//the instance of the helper and the database
	private static LogDBHelper dbInstance;
	
	//the instance of the helper and the database
	private SQLiteDatabase logDatabase;
	
	//context is used to get application data
	private Context appcontext;
	
	//used to retrieve SMS
	public static final String SMS_URI_ALL = "content://sms/";
	private Uri uri = null; //used for getting SMS data
	
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

    	uri = Uri.parse(SMS_URI_ALL);
    }
 
    
    // Get the instance of the database
    public static LogDBHelper getInstance(Context context) {

        // Use the application context, which will ensure that you 
        // don't accidentally leak an Activity's context.
        if (dbInstance == null) {
          dbInstance = new LogDBHelper(context.getApplicationContext());
        }
        return dbInstance;
      }
    
    
    // Creating Tables and importing the real time call logs from phone
    @Override
    public void onCreate(SQLiteDatabase db) {

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
	    	
	    	String name = calllog.getPhonename();
	        String number = calllog.getPhonenumber();
	        int durtation = (int)calllog.getCallsecondes();
	        String type = calllog.getCalltype();
	        int days = calllog.getDays();
	        
	        String insert_query = "INSERT INTO "+TABLE_CALL+" ("+CALL_NAME+", "+CALL_PH_NO+", "+CALL_DURATION+", "+CALL_TYPE+", "+CALL_DAY+") VALUES ('"+
	        					   name+"','"+number+"',"+durtation+",'"+type+"',"+days+")";
	      
	        //insert the row
	        db.execSQL(insert_query);
	    }
	    
	    //The following method insert all Text Log to the database
	    ArrayList<SMSMessageBean> textlogs = gtlog.GetTextLog();
	    
	    for (SMSMessageBean smslog : textlogs){
	    	
	    	ContentValues values = new ContentValues();
	        values.put(TEXT_NAME, smslog.getName()); 
	        values.put(TEXT_PH_NO, smslog.getPhoneNumber()); 
	        values.put(TEXT_TYPE, smslog.getType()); 
	        values.put(TEXT_DAY, smslog.getDays()); 
	        
	        // Inserting Row
	        db.insert(TABLE_TEXT, null, values);
	    }
    	
    }
 
    // Upgrading database
    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        
    	// Drop older table if existed
    	db.execSQL("DROP TABLE IF EXISTS " + TABLE_CALL);
    	db.execSQL("DROP TABLE IF EXISTS " + TABLE_TEXT);
 
        // Create tables again
        onCreate(logDatabase);
    }
    
    
    public void generateDatabase(){
    	
    	
    	
    }
    
    
    public int countTies(TieCriteria tc){

    	
		// reset the name count
		int nameCount = 0;
		//RELATIVE NAME GENERATER
		String select_database = null;
		String list_order = null;
		
		String criteria = tc.getFrequency();
		String type = tc.getAction();
		int durationFrom = tc.getDuration_from();
		int durationTo = tc.getDuration_to();
		
		//Query to be used to query the database
		String selectQuery = null;
		
		// Get the database
		SQLiteDatabase database = this.getReadableDatabase();
		
		// checking the type to decide which database to query
		if(type.equals("call"))select_database = "call_log";
		else if(type.equals("text"))select_database = "text_log";

		Log.v("tiecriteria","criteria="+criteria);
		
		// checking the criteria
		// 1. Relative Criteria: the most or the lease
		if(criteria.equals("the most")||criteria.equals("the least")){
		
			if(criteria.equals("the most"))list_order = "DESC";
			else if(criteria.equals("the least"))list_order = "ASC";
					
					// Select based on the most or the least criteria
					selectQuery = "SELECT name, COUNT(name),days " +
				    					 "FROM "+select_database+" WHERE name IS NOT 'null' AND days <= " + durationFrom +" AND days >= " + durationTo + " " +
				    					 "GROUP BY name ORDER BY COUNT(name) "+list_order;
				    
				}
				
				
				// 2. Absolute Criteria: between X and Y calls/texts
				else if(criteria.split(",").length==2){
					
					String fromNum = criteria.split(",")[0];
					String toNum = criteria.split(",")[1];

					// Select based on the absolute criteria
					selectQuery =   "SELECT name, COUNT(name), days " +
			    					"FROM "+select_database+" " +
			    					"WHERE name IS NOT 'null' " +
			    					//constraint on the time period of selection
				    				"AND days BETWEEN " + durationTo + " AND " + durationFrom + " " + 
				    				"GROUP BY name " +
				    				"HAVING COUNT(name) BETWEEN " + fromNum + " AND "+ toNum + " " +
				    				"ORDER BY COUNT(name) DESC";
				}
				
				// 3. Mode: the number that appears most
				else if(criteria.equals("mode")){
					
					// Select based on the mode criteria
					selectQuery = 	"SELECT A.name, COUNT(A.action_count), A.days "+
									"FROM " +
									"(SELECT name, COUNT(name) AS action_count,days " +
									"FROM "+select_database+" WHERE name IS NOT 'null' AND days <= " + durationFrom +" AND days >= " + durationTo + " " +
									"GROUP BY name ORDER BY COUNT(name)) A " +
									"GROUP BY A.name ORDER BY COUNT(A.action_count) DESC";
					
				}
				
				
				// 4. Median: the median number of calls
				else if(criteria.equals("median")){
					
					// first thing to do is still to select all the names
					selectQuery =  "SELECT name, COUNT(name),days " +
			    				   "FROM "+select_database+" WHERE name IS NOT 'null' AND days <= " + durationFrom +" AND days >= " + durationTo + " " +
			    				   "GROUP BY name ORDER BY COUNT(name) ASC"; //Must order in ASC or DESC to find median
					
				}
				
				
				// 5. Mean: the mean number of calls
				else if(criteria.equals("mean")){
					
					// first thing to do is to select all the names
					selectQuery =   "SELECT name, COUNT(name),days " +
		    					    "FROM "+select_database+" WHERE name IS NOT 'null' AND days <= " + durationFrom +" AND days >= " + durationTo + " " +
		    					    "GROUP BY name ORDER BY COUNT(name) DESC";
					
					// select based on the mean criteria
					String countAvgQuery = 	"SELECT AVG(A.action_count)"+
											"FROM " +
											"(SELECT name, COUNT(name) AS action_count,days " +
											"FROM "+select_database+" " +
											"WHERE name IS NOT 'null' AND days <= " + durationFrom +" AND days >= " + durationTo + " " +
											"GROUP BY name ORDER BY COUNT(name)) A";
					
					// get the average of action counts
					Cursor avgCursor = database.rawQuery(countAvgQuery, null);
					avgCursor.moveToFirst();
					
				}
		
	    Cursor cursor = database.rawQuery(selectQuery, null);
	    
	    // looping through all rows and adding to list
	    if (cursor.moveToFirst()) {
	          do {
	        	  
	        	  // increase the name counts
	              nameCount++;
	              
	          } while (cursor.moveToNext());
	          
	    } 
	    
	    database.close();
	    //returning the desired name
	    return nameCount;
	}
	
	public Tie getTieByCriteria(TieCriteria tc){
		
		// RELATIVE NAME GENERATER
		String select_database = null;
		String list_order = null;
		ArrayList<Tie> tie_list = new ArrayList<Tie>();
		
		// used in mean criteria to calculate nearest name to mean 
		Map <Tie,Integer> tieAndCounts = new HashMap <Tie,Integer>();
		
		String criteria = tc.getFrequency();
		String type = tc.getAction();
		int durationFrom = tc.getDuration_from();
		int durationTo = tc.getDuration_to();
		int place = tc.getPlace();
		
		//Query to be used to query the database
		String selectQuery = null;

		// Get the database
		SQLiteDatabase database = this.getReadableDatabase();
		
		// used in "mean" criteria to calculate mean
		float averageCount = 0;
		
		// checking the type to decide which database to query
		if(type.equals("call"))select_database = "call_log";
		else if(type.equals("text"))select_database = "text_log";

		Log.v("tiecriteria","criteria="+criteria);
		
		// checking the criteria
		// 1. Relative Criteria: the most or the lease
		if(criteria.equals("the most")||criteria.equals("the least")){
		
			if(criteria.equals("the most"))list_order = "DESC";
			else if(criteria.equals("the least"))list_order = "ASC";
					
					// Select based on the most or the least criteria
					selectQuery = "SELECT name, COUNT(name), days, phone_number " +
				    					 "FROM "+select_database+" WHERE name IS NOT null AND name IS NOT 'null' " +
				    					 "AND days <= " + durationFrom +" AND days >= " + durationTo + " " +
				    					 "GROUP BY name ORDER BY COUNT(name) "+list_order;
				    
				}
				
				
				// 2. Absolute Criteria: between X and Y calls/texts
				else if(criteria.split(",").length==2){
					
					String fromNum = criteria.split(",")[0];
					String toNum = criteria.split(",")[1];

					// Select based on the absolute criteria
					selectQuery =   "SELECT name, COUNT(name), days, phone_number " +
			    					"FROM "+select_database+" " +
			    					"WHERE name IS NOT null AND name IS NOT 'null' " +
			    					//constraint on the time period of selection
				    				"AND days BETWEEN " + durationTo + " AND " + durationFrom + " " + 
				    				"GROUP BY name " +
				    				"HAVING COUNT(name) BETWEEN " + fromNum + " AND "+ toNum + " " +
				    				"ORDER BY COUNT(name) DESC";
				}
				
				// 3. Mode: the number that appears most
				else if(criteria.equals("mode")){
					
					// Select based on the mode criteria
					selectQuery = 	"SELECT A.name, COUNT(A.action_count), A.days, A.phone_number "+
									"FROM " +
									"(SELECT name, COUNT(name), phone_number AS action_count, days, phone_number " +
									"FROM "+select_database+" WHERE name IS NOT null AND name IS NOT 'null' " +
									"AND days <= " + durationFrom +" AND days >= " + durationTo + " " +
									"GROUP BY name ORDER BY COUNT(name)) A " +
									"GROUP BY A.name ORDER BY COUNT(A.action_count) DESC";
					
				}
				
				
				// 4. Median: the median number of calls
				else if(criteria.equals("median")){
					
					// first thing to do is still to select all the names
					selectQuery =  "SELECT name, COUNT(name), days, phone_number " +
			    				   "FROM "+select_database+" WHERE name IS NOT null AND name IS NOT 'null' " +
			    				   	"AND days <= " + durationFrom +" AND days >= " + durationTo + " " +
			    				   "GROUP BY name ORDER BY COUNT(name) ASC"; //Must order in ASC or DESC to find median
					
				}
				
				
				// 5. Mean: the mean number of calls
				else if(criteria.equals("mean")){
					
					// first thing to do is to select all the names
					selectQuery =   "SELECT name, COUNT(name), days, phone_number " +
		    					    "FROM "+select_database+" WHERE name IS NOT null AND name IS NOT 'null' " +
		    					    "AND days <= " + durationFrom +" AND days >= " + durationTo + " " +
		    					    "GROUP BY name ORDER BY COUNT(name) DESC";
					
					// select based on the mean criteria
					String countAvgQuery = 	"SELECT AVG(A.action_count)"+
											"FROM " +
											"(SELECT name, COUNT(name) AS action_count,days " +
											"FROM "+select_database+" " +
											"WHERE name IS NOT null AND name IS NOT 'null' " +
											"AND days <= " + durationFrom +" AND days >= " + durationTo + " " +
											"GROUP BY name ORDER BY COUNT(name)) A";
					
					// get the average of action counts
					Cursor avgCursor = database.rawQuery(countAvgQuery, null);
					avgCursor.moveToFirst();
					
					averageCount = avgCursor.getFloat(0);
				}
				
		database.close();
		
        Cursor cursor = database.rawQuery(selectQuery, null);
        
        Log.v("namelist","================================================");
        Log.v("namelist","List of the "+criteria+" "+type+"ed within "+durationFrom+" to "+durationTo+" day(s)");
        Log.v("namelist","================================================");
        
        // looping through all rows and adding to list
        if (cursor.moveToFirst()) {
              do {
                  String name = cursor.getString(0);
                  int occurrence = cursor.getInt(1);
                  int num_of_days = cursor.getInt(2);
                  String phone_number = cursor.getString(3);
                  
                  // use these values to create a tie
                  Tie tie = new Tie(name,phone_number,tc);
                  
                  tie_list.add(tie); //add the tie to the list
                  
                  // if it is "mean" criteria, the action count will also be stored (to be used to calculate closest to mean)
                  if (criteria.equals("mean"))tieAndCounts.put(tie, occurrence);
                  
                  Log.v("namelist",name+", "+occurrence+" "+type+"s"+", most recent within "+num_of_days+" day(s)");
                 
              } while (cursor.moveToNext());
              
        } 
        
        // if criteria is median will return the median name
        if (criteria.equals("median")){
        	
        	int i = tie_list.size(); // get the total number of name counts
        	
        	if (i%2==0){	//even numbers of names
        		
        		// then, randomly choose between the two medians
        		Random rand = new Random();
			    place = i/2 + rand.nextInt(2);
        		
        		
        	}else if(i%2!=0){	// odd number of names
        		
        		// then, retrieve the name in the middle
        		place = (i+1)/2;
        		
        	}
        	
        	return tie_list.get(place-1);
        }
        
        // if criteria is mode it will return a random name (if multiple mode names exist)
        else if (criteria.equals("mode")){
        	
        	int i = tie_list.size();
        	
        	Random rand = new Random();
			place = rand.nextInt(i)+1;
        	
        	return tie_list.get(place-1);
        	
        }
        
        // if criteria is mean, then calculate the nearest name with calls/texts to that
        else if (criteria.equals("mean")){
        	
    		Tie output_tie = null; // the closest name to mean to be out put
    		
    		float diff = 999999; // used to calculate the nearest name
    	
    		// store multiple ties if many fits the criteria
    		ArrayList<Tie> tielist = new ArrayList<Tie>(); 
    		
        	// iterate through the action counts and find nearest to mean
    		for (Map.Entry<Tie, Integer> entry : tieAndCounts.entrySet()) {
    			
    			
        		// calculate the difference (distance from the value to mean)
        		float cal_float = Math.abs(entry.getValue() - averageCount);

        			// if find a nearer value
        			if (cal_float < diff){
        				
        				// clears the local tie list because a nearer tie is found
        				tielist.clear();
        				
        				diff = cal_float; // update the difference value 
        				
        				output_tie = entry.getKey(); // update the output name
        				
        				tielist.add(entry.getKey());
        				
        			// if equals, means multiple ties fit, will select one randomly
	        		}else if(cal_float==diff)tielist.add(entry.getKey());
	        		
        	}
	        
    		// look at the local tie list, if it has many them randomly choose one
    		if(tielist.size()>1){
    			
    			//randomly choose one from the list
    			Random rand = new Random();
    			int tieIndex = rand.nextInt(tielist.size());
    			return tielist.get(tieIndex);
    			
    		}
    		
    		else{ return output_tie; }
        }	
        else{
        	
	        //returning the desired name if it uses any criteria other than median
	        return tie_list.get(place-1);
        
        }
	}
    
}
