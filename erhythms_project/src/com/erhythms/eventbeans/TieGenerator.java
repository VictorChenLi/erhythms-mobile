package com.erhythms.eventbeans;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

import com.erhythms.network.LogDBHelper;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

public class TieGenerator{
	
	/*The following is the RELATIVE NAME GENERTER METHOD
	* database: the database reference passed in
	* place: integer, the 1st or 2nd or 3rd name on the list
	* criteria: either "most" or "least"
	* type: either "call" or "text"
	* prev_days: integer, within number of days
	*/
	
	private SQLiteOpenHelper dbhelper;
	private SQLiteDatabase database;
	
	
	public TieGenerator(Context context) {
		super();
		this.dbhelper = LogDBHelper.getInstance(context);
		this.database = dbhelper.getReadableDatabase();
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
		
		// checking the type to decide which database to query
		if(type.equals("call"))select_database = "call_log";
		else if(type.equals("text"))select_database = "text_log";
		
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
		
		// used in "mean" criteria to calculate mean
		float averageCount = 0;
		
		// checking the type to decide which database to query
		if(type.equals("call"))select_database = "call_log";
		else if(type.equals("text"))select_database = "text_log";
		
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
		
//		Log.v("debugtag", "query="+selectQuery);
		
        Cursor cursor = database.rawQuery(selectQuery, null);
        
        Log.v("namelist","================================================");
        Log.v("namelist","Criteria_id="+tc.getId()+": "+criteria+" "+type+"ed within "+durationFrom+" to "+durationTo+" day(s)");
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
