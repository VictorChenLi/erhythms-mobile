package com.erhythms.main;

import java.io.Serializable;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import org.apache.http.NameValuePair;
import org.apache.http.message.BasicNameValuePair;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.erhythmsproject.erhythmsapp.R;
import com.erhythms.eventbeans.EventBean;
import com.erhythms.eventbeans.Tie;
import com.erhythms.eventbeans.TieCriteria;
import com.erhythms.network.Encoder;
import com.erhythms.network.UploadDataTask;

import android.net.Uri;
import android.os.Bundle;
import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.app.FragmentTransaction;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.provider.ContactsContract;
import android.provider.ContactsContract.CommonDataKinds.Phone;
import android.provider.ContactsContract.Data;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.view.ViewPager;
import android.text.SpannableString;
import android.text.method.LinkMovementMethod;
import android.text.util.Linkify;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

@SuppressLint("DefaultLocale") 
public class MainActivity extends FragmentActivity implements EventFragment.OnEventRespondedListener,
	EventFragment.OnTieGeneratedListener,HorizontalScrollInViewGroup.OnScreenChangedListener{
	
	//initiate the buttons
	private MenuItem btnBack = null;
	private MenuItem btnNext = null;
	
	private ProgressBar eventProgressBar;

	private HorizontalScrollInViewGroup viewGroup;
	
	// shared preference used to retrieve app status
	private SharedPreferences appinfo = null;
	
	// pid did retrieved from what is saved during activation
	private String pid = null;
	private String did = null;

	// ArrayList for storing ArrayList
	private HashMap <Integer,EventBean> eventBeanList;

	//Array List used to handle the fragments
	private ArrayList<EventFragment> eventFragmentList;
	
	// total number of survey questions
	private int totalTextNum = 0;
		
	// total number of survey questions
	private int totalTieNum = 0;
 	
	// total number of survey questions
	private int totalQuestionNum = 0;
	
	// total number of events
	private int totalEventNum = 0;
	
	//parameter to identify if downloading and parsing is ok
	private boolean isReady = false;
	
	//Time out for the "choose from address book" button
	private static final int LOADING_SCREEN_RESULT = 2002;
	
	/* TIE POOL is used in this application for multiple purposes
	 * The mapping is indexed by question id, so <Integer, Tie> will be assigned <QuestionID, Tie>
	 * In dealing with the selection methods: it is used  for the three selection methods: random, walk_through and select from previous
	 * For walk_through, it ensures no same name is selected by checking the namePool
	 * For use previous tie, it checks this pool to find a previously used tie
	 * 
	 */
	private ArrayList <Tie> tiePool = new ArrayList<Tie>();
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		//the following decided if the app runs for the first time and show consent form
		appinfo = getSharedPreferences("appinfo", Context.MODE_PRIVATE);
		
 		//User hasn't agreed to System Consent Form
		if (!appinfo.getBoolean("system_consent", false)) {
			Intent intent = new Intent();
			intent.setClass(getApplicationContext(),SystemConsentActivity.class); 
			startActivity(intent);
			this.finish();
		}
		
		//User has agreed to System Consent Form, BUT hasn't activate
		else if (!appinfo.getBoolean("activated", false)) {
			Intent intent = new Intent();
			intent.setClass(getApplicationContext(),ActivationActivity.class); 
			startActivity(intent);
			this.finish();
		
		//User has agreed to System Consent Form, activated, BUT hasn't agree to Participant Consent Form
		}else if (!appinfo.getBoolean("participant_consent", false)) {
			
			Intent intent = new Intent();
			intent.setClass(getApplicationContext(),ParticipantConsentActivity.class); 
			startActivity(intent);
			this.finish();
		
		// User has agreed to System Consent Form, activated, and agreed to Participant Consent Form
		}else{
			setContentView(R.layout.main_activity);
			
			// read preferences and retrieve PID and DID
			pid = appinfo.getString("pid", "--");
			did = appinfo.getString("did", "--");
			 
			Intent i = new Intent(MainActivity.this, LoadingScreen.class);
			startActivityForResult(i, LOADING_SCREEN_RESULT);
			
			// THESE CODE FORCES THE SHOWING OF THE MENU BUTTON
		    try {
		    	  ViewConfiguration config = ViewConfiguration.get(this);
		    	  Field menuKeyField = ViewConfiguration.class.getDeclaredField("sHasPermanentMenuKey");

		    	  if (menuKeyField != null) {
		    	    menuKeyField.setAccessible(true);
		    	    menuKeyField.setBoolean(config, false);
		    	  }
		    	}
		    	catch (Exception e) {
		    	  // presumably, not relevant
		    	}
		    
			eventProgressBar = (ProgressBar)findViewById(R.id.eventProgress);
			
			viewGroup = (HorizontalScrollInViewGroup)findViewById(R.id.event_frame);
					
		}
	}		
			
			
		//This method is used by the "Contact Picker" to retrieve user picks from contacts
		@Override
		protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		    
			super.onActivityResult(requestCode, resultCode, data);
	
			if (requestCode == LOADING_SCREEN_RESULT) {
		        // Make sure the request was successful
		        if (resultCode == RESULT_OK && data!=null) {
		        	 
		        	/* THE FOLLOWING CODE INITIALIZE THE APP
		        	 */
		        	
					 // read the string data from the loading activity
		        	 isReady = data.getBooleanExtra("isReady", false);
		        	 String eventString = data.getStringExtra("eventString");
		        	 
		        	 parseEventString(eventString);
		        	 
		        	 // get total number of events
		        	 totalEventNum = eventBeanList.size();
	
			 	 	 eventFragmentList = new ArrayList<EventFragment>();
			 		
		 	 	     for(int i = 1; i <= eventBeanList.size(); i++)
		 	         {
		 	         	
		 	 	    	Log.v("ebstring", eventBeanList.get(i).toString());
		 	 	    	
		 	 	    	/* initiate new fragment, passing current tie pool
		 	 	    	 * the current tie pool will be used in walk-through and UsePreviousTie methods
		 	 	    	 */
		 	 	    	
		 	 	    	Bundle nBundle = new Bundle();
		 	           	nBundle.putSerializable("tiePool",(Serializable) tiePool);
		 	           	EventFragment eventFragment = EventFragment.getInstance(nBundle);
		 	    		eventFragment.setEventbean(eventBeanList.get(i));
		 	    		
		 	    		// add to the fragment list
		 	    		eventFragmentList.add(eventFragment);
		 	    		
		 	    		FragmentTransaction ft = getFragmentManager().beginTransaction();
		 	    		ft.add(R.id.event_frame, eventFragmentList.get(i-1),i+"");
		 	    		ft.commit();
		 	    		
		 	         }
		 	 	     
		 	 	     // setting the max value for the progress bar
		 	 	     eventProgressBar.setMax(eventFragmentList.size());
		 	 	     eventProgressBar.setProgress(1);
		 	 	     
		 	 	     
		 	 	     /* CHECK FOR THE FIRST EVENT TO SEE IF IT REQURIES RESPONSE
		 	 	      * IF IT DOES NOT, WILL ENABLE THE NEXT BUTTON
		 	 	      */
		 	 	     
		 	 	     if(!eventFragmentList.get(0).getEventbean().isSelectCallLog() && 
							!eventFragmentList.get(0).getEventbean().isSelectContacts() && 
							!eventFragmentList.get(0).getEventbean().isEnterManually() && 
							!eventFragmentList.get(0).getEventbean().isEnterText()
							&& eventFragmentList.get(0).getEventbean().getChoicecount() == 0){
							
							setButtonEnable(btnNext);
							viewGroup.setAllowNext(true);
					}else{
						
						// else disable the next (because no response need to be checked)
						viewGroup.setAllowNext(false);}
			 	 	 
		        	 
		             }else{Log.w("debugtag", "Warning: activity result not ok");}
			} else {
		        Log.w("debugtag", "Warning: no activity result found");
		    }
		}

	private void uploadResponse(){
		
		// change the text of the button to indicate interview is over
		setButtonDisable(btnNext);
		viewGroup.setAllowNext(false);
        
        // THE FOLLOWING CODE UPLOAD THE USER RESPONSE
        
        //form a JSON file of user response and upload all user response now to the server
        ArrayList<NameValuePair> responseUpload = new ArrayList<NameValuePair>();
		
		Log.v("pid","pid="+pid);
		Log.v("did","did="+did);
		
		// Using a list of nameValuePairs to store POST data
		responseUpload.add(new BasicNameValuePair("participant_id",pid));
		responseUpload.add(new BasicNameValuePair("device_id",did));
       
		// Putting every question response into a JSON array
		
		JSONArray survey_response = new JSONArray(); // Creating an array of survey response
		
		// iterate through the event beans and elicit all survey question id and responses
		for (int i = 0; i < eventBeanList.size(); i++)
		{
			// if the event is survey question, save it to the JSON file
			if(eventBeanList.get(i).getEventType().equals("SURVEY_QUESTION")){

				int qid = eventBeanList.get(i).getQid();
				String encodedText = Encoder.MD5Encode(eventBeanList.get(i).getTextResponse());
				String response = eventBeanList.get(i).getChoiceResponse() + " || " + encodedText;
				
				JSONObject question_answer = new JSONObject();  // Creating individual response_record
					
				// the following add fields to the JSON object
				try {

					question_answer.put("answer",response);
					question_answer.put("question_id",qid);
					
				} catch (JSONException e) {
					e.printStackTrace();
				}
				
				survey_response.put(question_answer);
			}

		}
        
		//putting the array of JSON object survey responses to the name and value pairs
		responseUpload.add(new BasicNameValuePair("survey_response",survey_response.toString()));
		
		//uploading the name and value pairs
		String response_url = getResources().getString(R.string.response_url);
		
		Log.v("rqt",responseUpload.toString());
		
		UploadDataTask upload_survey = new UploadDataTask(response_url, responseUpload, MainActivity.this);
		
		try {
			
			String retc = upload_survey.execute().get();
			Log.v("statuscode","upload_survey_return_code="+retc);
			
		} catch (Exception e){
			e.printStackTrace();
		}
		
		
		// THE FOLLOWING CODE UPLOAD ALL THE GENERATED TIE FOR EACH QUESTION
		
		//form a JSON file of user response and upload all user response now to the server
        ArrayList<NameValuePair> criteriaResultUpload = new ArrayList<NameValuePair>();
		
		Log.v("pid","pid="+pid);
		Log.v("did","did="+did);
		
		// Using a list of nameValuePairs to store POST data
		criteriaResultUpload.add(new BasicNameValuePair("participant_id",pid));
		criteriaResultUpload.add(new BasicNameValuePair("device_id",did));
		
		// Putting every question response into a JSON array
		
		JSONArray criteria_resultArray = new JSONArray(); // Creating an array of survey response
		
		// iterate through the event beans and elicit all survey question id and responses
//		for (Map.Entry<Integer, Tie> entry : tiePool.entrySet())
//		{
//			
//			Tie tie = entry.getValue();
//			String qid = tie.getQid()+"";
//			String criteria_id = tie.getCriteria().getId()+"";
//			String hashed_tie = Encoder.hashPhoneNumber(tie.getPhone_number());
//			
//			JSONObject criteria_result = new JSONObject();  // Creating individual criteria_result_record
//			
//				// the following add fields to the JSON object
//				try {
//
//					criteria_result.put("question_id",qid);
//					criteria_result.put("criteria_id", criteria_id);
//					criteria_result.put("tie_hash", hashed_tie);
//					
//				} catch (JSONException e) {
//					e.printStackTrace();
//				}
//				
//				criteria_resultArray.put(criteria_result);
//			}
		
			//putting the array of JSON object survey responses to the name and value pairs
			criteriaResultUpload.add(new BasicNameValuePair("criteria_result",criteria_resultArray.toString()));
			
			//uploading the name and value pairs
			String criteria_result_url = getResources().getString(R.string.criteria_result_url);
			
			Log.v("rqt",criteriaResultUpload.toString());
			
			UploadDataTask uploadCriteriaResult = new UploadDataTask(criteria_result_url, criteriaResultUpload, MainActivity.this);
			
			try {
				
				String retc = uploadCriteriaResult.execute().get();
				Log.v("statuscode","uploadCriteriaResult_code="+retc);
				
			} catch (Exception e){
				e.printStackTrace();
			}
		
	}
	
	// this method parses all the events from the event string
	private void parseEventString(String eventString){
		
		//initiate the list
		eventBeanList = new HashMap<Integer,EventBean>();
		
		//Retrieve the three sub strings first
		String textString = eventString.substring(eventString.indexOf("\"TEXT_DISPLAY\""),
				eventString.indexOf("\"TIE_DISPLAY\"")-1).replace("\"TEXT_DISPLAY\",","");
		
		String tieString = eventString.substring(eventString.indexOf("\"TIE_DISPLAY\""),
				eventString.indexOf("\"SURVEY_QUESTION\"")-1).replace("\"TIE_DISPLAY\",","");
		
		String questionString = eventString.substring(eventString.indexOf("\"SURVEY_QUESTION\""),
				eventString.indexOf("]")).replace("\"SURVEY_QUESTION\",","");
		
		Log.v("estring","TEXT="+textString);
		Log.v("estring","TIE="+tieString);
		Log.v("estring","QS="+questionString); 
		
		// THE FOLLOWING PARSES THE TEXT DISPLAY STRING
		String[] txtArray = textString.split(",(?=([^\"]*\"[^\"]*\")*[^\"]*$)");
		totalTextNum = Integer.parseInt(txtArray[0].replace("\"", ""));
		
		//  only parsing when there exists Text Display Event
		if (totalTextNum != 0){
			
			int cursor = 1; //set cursor to 1 because 0 has been read (totalNumber)
			
			while (cursor < txtArray.length){
				int eventIndex = 0;
				String textbody = "";
				
				eventIndex = Integer.parseInt(txtArray[cursor].replace("\"", "")); //reading event index
				cursor++; // move to the next
				
				textbody = txtArray[cursor]; //reading text body
				cursor++; // move to the next
				
				// for every event read, a new bean is created
				EventBean ebean = new EventBean(eventIndex,textbody);
				
				eventBeanList.put(eventIndex,ebean);
			}
		}
		
		
		// THE FOLLOWING PARSES THE TIE DISPLAY STRING
		String[] tieArray = tieString.split(",(?=([^\"]*\"[^\"]*\")*[^\"]*$)");
		totalTieNum = Integer.parseInt(tieArray[0].replace("\"", ""));
		
		//only parsing when there exists Tie Display Event
		if (totalTieNum != 0){
			
			int cursor = 1; //set cursor to 1 because 0 has been read (totalNumber)
			
			while (cursor < tieArray.length){
				int eventIndex = 0;
				int criteria_id = 0;
				String frequency = "";
				int from = 0;
				int duration = 0;
				String action = "";
				
				eventIndex = Integer.parseInt(tieArray[cursor].replace("\"", "")); //reading event index
				cursor++; // move to the next
				
				criteria_id = Integer.parseInt(tieArray[cursor].replace("\"", "")); //reading criteria id
				cursor++; // move to the next
				
				frequency = tieArray[cursor].replace("\"", "").toLowerCase(); //reading frequency
				cursor++; // move to the next
				
				from = Integer.parseInt(tieArray[cursor].replace("\"", "")); //reading from days integer
				cursor++; // move to the next
				
				duration = Integer.parseInt(tieArray[cursor].replace("\"", "")); //reading duration days integer
				cursor++; // move to the next
					
				action = tieArray[cursor].replace("\"", "").replace("]", "").toLowerCase(); //reading frequency
				cursor++; // move to the next
				
				// for every event read, a new bean is created
				EventBean ebean = new EventBean(eventIndex);
				
				// for every criteria read, initiate a new criteria to add to the event bean
				TieCriteria tieCriteria = new TieCriteria(criteria_id, frequency, from, duration, action);

				ebean.addTieCriteria(tieCriteria);
				
				eventBeanList.put(eventIndex,ebean);
			}
		}
		
		
		// THE FOLLOWING PARSES THE SURVEY QUESTION STRING
		String[] qArray = questionString.split(",(?=([^\"]*\"[^\"]*\")*[^\"]*$)");
		totalQuestionNum = Integer.parseInt(qArray[0].replace("\"", ""));
		
		
		//  Only parsing when there exists survey question event
		if (totalQuestionNum != 0){
			
			int cursor = 1; // initially set cursor to 1 because question strings start from the second place
		
			// the loop is used to parse the entire string
			while(cursor < qArray.length){
				
				int eventIndex = 0;
				int qid = 0;
				String qbody = "";
				String qanswer = "";
				String qType = "single";
				int dynamic_count = 0;
				
				boolean selectContacts = false;
				boolean selectCallLog = false;
				boolean enterManually = false;
				boolean enterText = false;
				
				int numofans = 0; //number of answers
				
				// parsing the event index id
				eventIndex = Integer.parseInt(qArray[cursor].replace("\"", "")); //reading question body
				cursor++; // move to the next
				
				// parsing the question id
				qid = Integer.parseInt(qArray[cursor].replace("\"", "")); //reading question id
				cursor++;
				
				// parsing the question type
				qType = qArray[cursor].replace("\"", "").toLowerCase();
				cursor++;
				
				// parsing the question body
				qbody = qArray[cursor]; //reading question body
				cursor++; // move to the next
				
				if(qArray[cursor].replace("\"", "").toLowerCase().equals("yes"))selectContacts = true; //reading whether select contacts
				cursor++; // move to the next
				
				if(qArray[cursor].replace("\"", "").toLowerCase().equals("yes"))selectCallLog = true; //reading whether select call log
				cursor++; // move to the next
				
				if(qArray[cursor].replace("\"", "").toLowerCase().equals("yes"))enterManually = true; //reading whether enter manually
				cursor++; // move to the next
				
				if(qArray[cursor].replace("\"", "").toLowerCase().equals("yes"))enterText = true; //reading whether enter textbox is enabled
				cursor++; // move to the next
				
				numofans = Integer.parseInt(qArray[cursor].replace("\"", "").replace("]", ""));
				cursor++;

					//inner loop to read the answers
					for (int i=0;i < numofans; i++){
						
						// concatenate answers together
						// if not reading the last answer
						if (i != numofans - 1){
							qanswer = qanswer + qArray[cursor] + "_";
						}// if reading the last answer
						else{
							qanswer = qanswer + qArray[cursor];
						}
						cursor++; // move to next
					}
				
				dynamic_count = Integer.parseInt(qArray[cursor].replace("\"", "").replace("]", ""));
				cursor ++; // moved to the next

				
				//initiate a new event bean to save events
			
				// for every survey question read, a new bean is created
				EventBean ebean = new EventBean(eventIndex,qid,qbody,qType,qanswer,numofans,selectContacts,selectCallLog,enterManually,enterText);
				
				// inner loop to read the dynamic text parameters
				// the multiplied number is the number of fields for a single dynamic text
				// for every criteria (dynamic text) the loop goes once
				for(int i=0;i < dynamic_count; i++){

						//reading the dynamic position
						int position = Integer.parseInt(qArray[cursor].replace("\"", ""));
						cursor ++; // moved to the next
						
						//reading the criteria id
						int criteriaID = Integer.parseInt(qArray[cursor].replace("\"", ""));
						cursor ++; // moved to the next
						
						//reading the frequency
						String frequency = qArray[cursor].replace("\"", "").toLowerCase();
						cursor ++; // moved to the next
						
						//reading the duration from
						int durationFrom = Integer.parseInt(qArray[cursor].replace("\"", ""));
						cursor ++; // moved to the next
						
						//reading the duration to
						int durationTo = Integer.parseInt(qArray[cursor].replace("\"", ""));
						cursor ++; // moved to the next
						
						//reading the type
						String type = qArray[cursor].replace("\"", "").toLowerCase();
						cursor ++; // moved to the next
						
						//reading the selection method
						String method = qArray[cursor].replace("\"", "").replace("]", "").toLowerCase();
						cursor ++; // moved to the next
						
						TieCriteria newTc = new TieCriteria(criteriaID, position, frequency, durationFrom, durationTo, type, method);
						
						ebean.addTieCriteria(newTc);
					}
				
				// adding the bean to the array list
				eventBeanList.put(eventIndex,ebean);
				
				}
			
			}
	}
	
	//this method sets the ImageButton to ENABLE state
	private void setButtonEnable(MenuItem button){
		
		//setting the state of the button to enable
		button.setEnabled(true);
		button.getIcon().setAlpha(255);
		
	}
	
	
	
	
	//this method sets the ImageButton to DISABLE state
	private void setButtonDisable(MenuItem button){
		
		//setting the state of the button to enable
		button.setEnabled(false);
		button.getIcon().setAlpha(10);
		
	}
	
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		
		super.onCreateOptionsMenu(menu);
		
		// Inflate the menu; this adds items to the action bar if it is present.
		MenuInflater inflater = getMenuInflater();
	    inflater.inflate(R.menu.main, menu);

		btnBack = menu.findItem(R.id.button_back);
		btnNext = menu.findItem(R.id.button_next);
		
		//disable button back at start
		setButtonDisable(btnBack);
		setButtonDisable(btnNext);
		viewGroup.setAllowNext(false);
		
	    return true;
	}
	
	
	

	@Override
	public void onAttachFragment(Fragment fragment) {
		// TODO Auto-generated method stub
		super.onAttachFragment(fragment);
	}


	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
	    // Handle presses on the action bar items
	    switch (item.getItemId()) {
	    
	    	// code for the back button here
	    	case R.id.button_back:
				
	    		//Error control to avoid going out of bound
	    		if(viewGroup.getCurScreen() > 0){
	    			
	    			// switch to the next screen
	    			viewGroup.snapToScreen(viewGroup.getCurScreen()-1);
	    		
	    		}
	    		
	    		return true;
	    		
	    	// code for the next button here	
	    	case R.id.button_next:
				
	    		//Error control to avoid going out of bound
	    		if(viewGroup.getCurScreen() < eventFragmentList.size()-1){
	    			
	    			//switch to the next screen
	    			viewGroup.snapToScreen(viewGroup.getCurScreen()+1);
	    		
	    		}
	    		
	    		return true;
	    	
	        case R.id.action_help:
	        	
	        	AlertDialog alertDialog;
	            alertDialog = new AlertDialog.Builder(MainActivity.this).create();
	            alertDialog.setTitle("Help");
	            alertDialog.setMessage(getResources().getString(R.string.help_dialog));
	            alertDialog.setButton("Ok", new DialogInterface.OnClickListener() {

	                  public void onClick(DialogInterface dialog, int id) {
	                	  
	                } }); 
	            
	            alertDialog.show();
	            
	            TextView textView_help = (TextView) alertDialog.findViewById(android.R.id.message);
	            textView_help.setTextSize(15);
		        
	            return true;
	            
	        case R.id.action_about:
	        	
	        	AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
	        	
	        	final SpannableString about_dialog = new SpannableString(getResources().getString(R.string.about_dialog));
	            Linkify.addLinks(about_dialog, Linkify.WEB_URLS);
	            
	        	builder.setTitle("About E-Rhythms");
	        	builder.setMessage(about_dialog);
	        	
	        	// Add the buttons
	        	builder.setPositiveButton("Back", new DialogInterface.OnClickListener() {
	        	           public void onClick(DialogInterface dialog, int id) {
	        	               // User clicked back
	        	           }
	        	       });
	        
	        	// Create the AlertDialog
	        	AlertDialog dialog = builder.create();
		        
		        dialog.show();
		        
		        TextView textView_about = (TextView) dialog.findViewById(android.R.id.message);
		        textView_about.setTextSize(13);
		        textView_about.setMovementMethod(LinkMovementMethod.getInstance());
		        
	            return true;
	            
	        case R.id.exit:
	        	
	        	promptExit();
	            
	        	return true;
	           
	        default:
	            return super.onOptionsItemSelected(item);
	    }
	}
	
	@Override
	public void onBackPressed() {
	    
		promptExit();
		
	}
	
		private void promptExit(){
			// do something on Exit.
			
			  	LayoutInflater inflater = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
			    View exitprompt_layout = inflater.inflate((R.layout.exit_prompt), (ViewGroup) findViewById(R.id.exitlayout_root));
			   
				AlertDialog.Builder abortinterview = new AlertDialog.Builder(MainActivity.this).setView(exitprompt_layout);
		    	
		    	abortinterview.setTitle("Exit");
		    	abortinterview.setMessage(R.string.abort_message);
		    	
		    	// Create the AlertDialog
		    	final AlertDialog abort_dialog = abortinterview.create();
		    	abort_dialog.show();
		    	
		    	
		    	exitprompt_layout.findViewById(R.id.exit_discard).setOnClickListener(new View.OnClickListener() {

	                @Override
	                public void onClick(View view) {
	                	
	                	   setButtonDisable(btnBack);
	    	        	   setButtonDisable(btnNext);
	    	        	   
	    	        	   // User clicked back then return
	    	        	   AlertDialog alertDialog;
	    		           alertDialog = new AlertDialog.Builder(MainActivity.this).create();
	    		           alertDialog.setTitle("Thank you");
	    		           alertDialog.setCancelable(false);
	    		           alertDialog.setMessage(getResources().getString(R.string.finish_message));
	    		           alertDialog.setButton("Ok", new DialogInterface.OnClickListener() {

	    		                  public void onClick(DialogInterface dialog, int id) {
	    		                	  
	    		                	  //exit the application
	    		                	  finish();
	    		                	  
	    		                } }); 
	    		            
	    		            alertDialog.show();
	                }
		    	});
		    	
		    	
		    	exitprompt_layout.findViewById(R.id.exit_save).setOnClickListener(new View.OnClickListener() {

	                @Override
	                public void onClick(View view) {
	                	
		                	setButtonDisable(btnBack);
	    					setButtonDisable(btnNext);
							
	    					// upload survey response
			            	uploadResponse();
							
							// User clicked back then return
	    	        	    AlertDialog alertDialog;
	    		            alertDialog = new AlertDialog.Builder(MainActivity.this).create();
	    		            alertDialog.setTitle("Thank you");
	    		            alertDialog.setCancelable(false);
	    		            alertDialog.setMessage(getResources().getString(R.string.finish_message_withdata));
	    		            alertDialog.setButton("Ok", new DialogInterface.OnClickListener() {
	
	    		                  public void onClick(DialogInterface dialog, int id) {
	    		                	  
	    		                	  //exit the application
	    		                	  finish();
	    		                	  
	    		                } }); 
	    		            
	    		            alertDialog.show();
	                }
		    	});
		    	
		    	
		    	
		    	
		    	exitprompt_layout.findViewById(R.id.exit_cancel).setOnClickListener(new View.OnClickListener() {

	                @Override
	                public void onClick(View view) {
	                	//do nothing, just return
	                	abort_dialog.dismiss();
	                }
		    	});
		    	
		    	
		    	
		    	
		    
	}

		
		// this handles the event triggered in the fragment
		// which is that: the use has provided response for that event
		
		@Override
		public void onEventResponded(String code) {
			
			// TODO Auto-generated method stub
			Log.v("debugtag",code);
			
			// Based on the yes/no from the fragment event
			// enable or disable the next button
			
			String ifEnableNext = code.split(",")[1];
			
			if(ifEnableNext.equals("yes")){setButtonEnable(btnNext);viewGroup.setAllowNext(true);}
			else if (ifEnableNext.equals("no")){setButtonDisable(btnNext);viewGroup.setAllowNext(false);}
			
			
		}


		@Override
		public void onScreenChanged(int currentScreen) {
			// TODO Auto-generated method stub
			
			EventFragment currentFrag = eventFragmentList.get(currentScreen);
			
			// setting the progress bar
			eventProgressBar.setProgress(currentScreen+1);
			
			//CHECK IF AT END OR BEGINNNING TO DISABLE OR ENABLE BACK/NEXT BUTTONS
			if(currentScreen==0){
				
				//disable at the start
				setButtonDisable(btnBack);
				
			}else if(currentScreen==eventFragmentList.size()-1){
				
				//disable next at the end
				setButtonDisable(btnNext);
				viewGroup.setAllowNext(false);
				
			}else{

				//okay to always enable back button in the middle
				setButtonEnable(btnBack);
				
				/* Depending on the situation, check if the event is responded or no response required
				 * If it is already responded (a previous event), next is enabled. 
				 */
			}
				
				//disable next button initially then enable based on situation
				setButtonDisable(btnNext);
				viewGroup.setAllowNext(false);
			
				/* If there is any type of response, text or choice
				 * The Next button will be enabled, because this question is been responded
				 */
				
				if(currentFrag.getEventbean().getTextResponse().length() > 1 || 
						currentFrag.getEventbean().getChoiceResponse().length() > 1){
					
						setButtonEnable(btnNext);
						viewGroup.setAllowNext(true);
						
						Log.v("debugtag",currentScreen+":enabled Next cause already responded");
				}
				/* If no response is expected: no question choices, no enter buttons
				 * Then the "next" button will allow the user to continue
				 */
				
				else if(!currentFrag.getEventbean().isSelectCallLog() && 
						!currentFrag.getEventbean().isSelectContacts() && 
						!currentFrag.getEventbean().isEnterManually() && 
						!currentFrag.getEventbean().isEnterText()
						&& currentFrag.getEventbean().getChoicecount() == 0){
					
						Log.v("debugtag",currentScreen+":enabled Next cause no response needed");
					
						setButtonEnable(btnNext);
						viewGroup.setAllowNext(true);
				}
			
			Log.v("debugtag",currentScreen+":choice="+eventFragmentList.get(currentScreen).getEventbean().getChoiceResponse());
			Log.v("debugtag",currentScreen+":text="+eventFragmentList.get(currentScreen).getEventbean().getChoiceResponse());
				
		}


		@Override
		public void onTieGenerated(Tie tie) {
			// TODO Auto-generated method stub
			
			// Once the activity knows that a tie is generated, it will register it in the tie pool
			if(tie!=null)
			{
				tiePool.add(tie);
				Log.v("debugtag","registered:"+tie.toString());
			
			}
			
		}
	
}
