package com.erhythms.network;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.Locale;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.erhythms.logdata.CallPhoneBean;
import com.erhythms.logdata.SMSMessageBean;
import com.erhythms.network.Encoder;

import android.net.Uri;
import android.provider.CallLog;
import android.provider.ContactsContract;
import android.provider.ContactsContract.PhoneLookup;
import android.util.Log;
import android.content.ContentResolver;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.database.Cursor;


public class GetLogData{
	
	//context is used to get application data
	private Context appcontext;
	
	//used to retrieve SMS
	public static final String SMS_URI_ALL = "content://sms/";
	public static final String CALL_URI_ALL = "content://call_log/calls";
	
	
	private Uri uri = null; //used for getting SMS data
	private Uri calluri = null; 
	
	//shared preference used to store data
	private SharedPreferences logdata = null;
	
	private HashSet<String> addressbook_names = null;
	private HashSet<String> log_names = null;
	
	public GetLogData(Context appcontext){
		this.appcontext = appcontext;
		uri = Uri.parse(SMS_URI_ALL);
		calluri = Uri.parse(CALL_URI_ALL);
	}
	
	// Getting all the log data for upload
	public JSONArray generateAllLogData(){
		
		JSONArray output_array = new JSONArray();
		
		//read preferences info to see if application is first startup
		logdata = appcontext.getSharedPreferences("logdata", Context.MODE_PRIVATE);
		
		//initiate the string set of contact names
		addressbook_names = new HashSet<String>();
		log_names = new HashSet<String>();
		
		//writing the log data to shared preference
		Editor editor = logdata.edit();
		
		ArrayList<CallPhoneBean> call_raw_log = GetCallLog();
		ArrayList<SMSMessageBean> text_raw_log = GetTextLog();
		
		//Read through the address book to get a name pool of contacts
		//FIRST, get a name pool of all the names from the address book
		Cursor cursor = appcontext.getContentResolver().query(ContactsContract.Contacts.CONTENT_URI, null, null, null, null); 

	    while (cursor.moveToNext()) { 
	        String name = cursor.getString(cursor.getColumnIndex(ContactsContract.Contacts.DISPLAY_NAME));
	        addressbook_names.add(name);
	        }
		
	    cursor.close();
		
	    //THEN, check if the name in the call log exists in the address book
	    //IF IT DOES, save to the name pool
	    
		for (CallPhoneBean cbean: call_raw_log){
			if (cbean.getPhonename()!=null){
				String contact_name = cbean.getPhonename();
				
				//HERE implicitly, duplication is reduced by using HashSet
				if(addressbook_names.contains(contact_name))log_names.add(contact_name);
				
			}
		}
		
		 //this is a string of contact names
	  	String contact_namepool = "";
	  	int addid = 1;
		
		//if the name in the log exists in address book
		for(String name : log_names){
			
				contact_namepool = contact_namepool + name + "=" + addid + "&";
				addid++;
			
		}
		
		Log.v("addid", "POOL="+contact_namepool);
		
		editor.putString("contact_pool", contact_namepool);
		
		editor.commit();
		
		JSONObject calllog = GetCallJSONData(call_raw_log);
		JSONObject textlog = GetTextJSONData(text_raw_log);
		
		output_array.put(calllog);
		output_array.put(textlog);
		
		return output_array;
	}
	
	
		//this method retrieves all phone data and keep them in an array
	public ArrayList<CallPhoneBean> GetCallLog(){
			
			ArrayList<CallPhoneBean> callLogList = new ArrayList<CallPhoneBean>();
			
			ContentResolver cr = appcontext.getContentResolver();
			Cursor cursor = cr.query(calluri,
					new String[] { CallLog.Calls.NUMBER, CallLog.Calls.CACHED_NAME,
							CallLog.Calls.TYPE, CallLog.Calls.DURATION,
							CallLog.Calls.DATE }, null, null, CallLog.Calls.DATE);
			while (cursor.moveToNext()) {
				CallPhoneBean bean = new CallPhoneBean();
				bean.setPhonenumber(cursor.getString(0));
				bean.setPhonename(cursor.getString(1));
				
				//parsing to get the call type
				String calltype = "";
				switch (cursor.getInt(2)) {
				case CallLog.Calls.INCOMING_TYPE:
					calltype = "incoming";
					break;
				case CallLog.Calls.OUTGOING_TYPE:
					calltype = "outgoing";
					break;
				case CallLog.Calls.MISSED_TYPE:
					calltype = "missed";
					break;
				default:
					break;
				}
				bean.setCalltype(calltype);
				
				bean.setCallsecondes(cursor.getInt(3));
				
				//formating the call date to yyyy-MM-d H:m:s
				SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-d H:m:s",Locale.CANADA);
				String calldate = sdf.format(new Date(Long.parseLong(cursor.getString(4))));
				bean.setCalldate(calldate);
				
				//parse the call time and set the previous number of days
				int days = GetDaysMethod(Long.parseLong(cursor.getString(4)));
				bean.setDays(days);
				
				callLogList.add(bean);
			}
			cursor.close();
			
			return callLogList;
		}
	
		//this method retrieves all phone data and keep them in an array
	public ArrayList<SMSMessageBean> GetTextLog(){
			ArrayList<SMSMessageBean> smslist = new ArrayList<SMSMessageBean>();
			
			String[] projection = new String[] { "_id", "address", "person", "body", "date", "type" };
			
			ContentResolver cr = appcontext.getContentResolver();
			Cursor cusor = cr.query(uri, projection, null, null,"date desc");
			int phoneNumberColumn = cusor.getColumnIndex("address");
			int smsbodyColumn = cusor.getColumnIndex("body");
			int dateColumn = cusor.getColumnIndex("date");
			int typeColumn = cusor.getColumnIndex("type");
			
			if (cusor != null) {
				while (cusor.moveToNext()) {
					SMSMessageBean smsinfo = new SMSMessageBean();
					
					SimpleDateFormat sdformat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
					String LgTime = sdformat.format(Long.parseLong(cusor.getString(dateColumn)));  
					
					//parse the text time and set the previous number of days
					int days = GetDaysMethod(Long.parseLong(cusor.getString(dateColumn)));
					smsinfo.setDays(days);
					
					smsinfo.setDate(LgTime);
					
					smsinfo.setPhoneNumber(cusor.getString(phoneNumberColumn));
					
					String contact_address = cusor.getString(phoneNumberColumn);
					
                    Uri uri = Uri.withAppendedPath(PhoneLookup.CONTENT_FILTER_URI, Uri.encode(contact_address));  
                    Cursor cs= appcontext.getContentResolver().query(uri, new String[]{PhoneLookup.DISPLAY_NAME},PhoneLookup.NUMBER+"='"+contact_address+"'",null,null);

                    if(cs.getCount()>0)
                    	
                     {
                      cs.moveToFirst();
                      String contact_name =cs.getString(cs.getColumnIndex(PhoneLookup.DISPLAY_NAME));
                      smsinfo.setName(contact_name);
                     } 
					
					smsinfo.setSmsbody(cusor.getString(smsbodyColumn));
					
					//deciding on type value to see if it's incoming or outgoing
					if(Integer.parseInt(cusor.getString(typeColumn))==1){
						
						smsinfo.setType("incoming");	
						
					}else if(Integer.parseInt(cusor.getString(typeColumn))==2){
						
						smsinfo.setType("outgoing");
					}
					
					smslist.add(smsinfo);
				}
				
			}
			cusor.close();
			
			return smslist;
		}
		
		// return a JSONObject of the call log info bean
		public JSONObject GetCallJSONData(ArrayList<CallPhoneBean> array){
			
			JSONObject calllog_data = null;
			
			/* Creating a json file as the following
			   json file for storing call log data
			   
			   json file : calllog_data
			   {  
			      "call_records" : [
			      			{
			      			"contact_hash" : "asw3xe",
			      			"duration" : long (in seconds), "date" : "YYYYMMDDHHMMSS" // object
			      			}
						]
			    }
			*/
			
			// The following implements the calllog_data json file 
			try {
				
				calllog_data = new JSONObject(); // Initiate the top level json file  
				
				// Putting every call log entry into an array
				JSONArray call_records = new JSONArray(); // Creating an array of call_records
				
				//read preferences for accessing contact name pool
				logdata = appcontext.getSharedPreferences("logdata", Context.MODE_PRIVATE);
				
				for(int i = 0 ; i < array.size() ; i++ ){
					
					JSONObject call_record = new JSONObject();  // Creating individual call_record
					
					String contact_number = array.get(i).getPhonenumber();
					String date = array.get(i).getCalldate();
					String duration = array.get(i).getCallsecondes()+"";
					int address_id = 0;
					
					// check the contact name pool, if the name exists, assigning a number id to it
					
					if (array.get(i).getPhonename()!=null){
					
						String contact_name = array.get(i).getPhonename();
						
						String namepool = logdata.getString("contact_pool", "");
						
						String poolstrings[] = namepool.split("&");
						
						// the following checks if the contact name in the record exists in the name pool
						if (namepool.toLowerCase().contains(contact_name.toLowerCase())){
							
							// if the name exists in the name pool, assign the Address id in the name pool to this log record
							for (String substring: poolstrings){
								
								// find the contact name in the pool name string array
								if (substring.toLowerCase().contains(contact_name.toLowerCase())){
									
									// if the name is located, get the address id 
									address_id = Integer.parseInt(substring.split("=")[1]);
								}
							}
						}
						
					}
					
					String calltype = array.get(i).getCalltype();
					
					// the following add fields to the JSON object
					call_record.put("duration",duration);
					call_record.put("date",date);
					call_record.put("contact_hash",HashContact(contact_number));
					
					if(address_id != 0)call_record.put("address_book_id",address_id);
					
					call_record.put("direction",calltype);
					
					call_records.put(call_record);
				}
				
				// Putting call records array in json file
				calllog_data.put("call_records",call_records); 
				
			} catch (JSONException e) {
				e.printStackTrace();
			}
			return calllog_data;
		}

	// return a JSONObject of the SMS Message info bean
	private JSONObject GetTextJSONData(ArrayList<SMSMessageBean> infos){
			
			JSONObject textlog_data = null;
			
			/* Creating a json file as the following
			   json file for storing call log data
			   
			   json file : calllog_data
			   {  
			      "text_records" : [
			      			{
			      			"contact_hash" : "asw3xe",
			      			"date" : "YYYYMMDDHHMMSS"
			      			}
			      		]
			    }
			*/
			
			// The following implements the textlog_data json file 
			try {
				
				textlog_data = new JSONObject(); // Initiate the top level json file  
				
				// Putting every call log entry into an array
				JSONArray text_records = new JSONArray(); // Creating an array of text_records
				
				//read preferences for accessing contact name pool
				logdata = appcontext.getSharedPreferences("logdata", Context.MODE_PRIVATE);
				
				for(int i = 0 ; i < infos.size() ; i++ ){
					
					JSONObject text_record = new JSONObject();  // Creating individual text_record
					
					String contact_number = infos.get(i).getPhoneNumber();
					String date = infos.get(i).getDate();
					String name = infos.get(i).getName();
					String texttype = infos.get(i).getType();
					String message_body = infos.get(i).getSmsbody();
					int address_id = 0;
					
					text_record.put("date",date);

					// check the contact name pool, if the name exists, assigning a number id to it
					
					if (infos.get(i).getName()!=null){
					
						String contact_name = name;
						
						String namepool = logdata.getString("contact_pool", "");
						
						String poolstrings[] = namepool.split("&");
						
						// the following checks if the contact name in the record exists in the name pool
						if (namepool.toLowerCase().contains(contact_name.toLowerCase())){
							
							// if the name exists in the name pool, assign the Address id in the name pool to this log record
							for (String substring: poolstrings){
								
								// find the contact name in the pool name string array
								if (substring.toLowerCase().contains(contact_name.toLowerCase())){
									
									// if the name is located, get the address id 
									address_id = Integer.parseInt(substring.split("=")[1]);
								}
							}
						}
						
					}
					
					text_record.put("contact_hash",HashContact(contact_number));
					
					// if the address_book_id equals 0, meaning it does not exist
					// simply will not include the address_id in the upload
					
					if(address_id != 0)text_record.put("address_book_id",address_id+"");
					
					text_record.put("direction",texttype);
					
					text_record.put("message_length",message_body.length());
					
					text_records.put(text_record);
				}
				
				// Putting call records array in json file
				textlog_data.put("text_records",text_records); 
				
			} catch (JSONException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			return textlog_data;
		}
	
		//the following method encodes contact number to a hash while leaving the area code
		public String HashContact(String input_number){
			
			String contact_hash = "";
			
			contact_hash = Encoder.hashPhoneNumber(input_number);
			
		return contact_hash;
	}
	
	//this method converts call/text time to number of days before
	private int GetDaysMethod(long calltime) {
		long nowtime = new Date().getTime();
		long duration = (nowtime - calltime) / (1000 * 60);
		if (duration < 1440) {
			return 0;
		} else {
			return (int)duration / 1440;
		}
	}
	

}