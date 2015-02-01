package com.erhythms.main;

import java.io.Serializable;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;

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
import com.erhythms.widget.TutorialScreen;

import android.os.Build;
import android.os.Bundle;
import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.app.FragmentTransaction;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
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
import android.view.WindowManager;
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

	// Hash map with Integer being event index and map to event bean
	private HashMap <Integer,EventBean> eventBeanList;

	//Array List used to handle the fragments
	private ArrayList<EventFragment> eventFragmentList;
	
	// Array List used to store the question skip JSON Objects
	private ArrayList<JSONObject> questionSkipList;
	
	// a hashset storing all the branch enabled qids
	private HashSet<Integer> branchEnabledQid;
	
	// total number of survey questions
	private int totalTextNum = 0;
		
	// total number of survey questions
	private int totalTieNum = 0;
 	
	// total number of survey questions
	private int totalQuestionNum = 0;
	
	// total number of events
	private int totalEventNum = 0;
	
	//parameter to identify if downloading and parsing is ok
	private boolean readyUpload = false;
	
	private static final int LOADING_SCREEN_RESULT = 2002;
	
	/* TIE POOL is used in this application for multiple purposes
	 * The mapping is indexed by question id, so <Integer, Tie> will be assigned <QuestionID, Tie>
	 * In dealing with the selection methods: it is used  for the three selection methods: random, walk_through and select from previous
	 * For walk_through, it ensures no same name is selected by checking the namePool
	 * For use previous tie, it checks this pool to find a previously used tie
	 * 
	 */
	private ArrayList <Tie> tiePool = new ArrayList<Tie>();
	
	@SuppressLint("NewApi") @Override
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
			 
//			Intent i = new Intent(MainActivity.this, LoadingScreen.class);
//			startActivityForResult(i, LOADING_SCREEN_RESULT);
			
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
			
			
			/* This code changes the status bar color
			 */
			
		    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
			        getWindow().addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
				    getWindow().setStatusBarColor(Color.parseColor("#77c2d7"));
			}	 
		    
		    
			// initialize all the events
			initialize();
		    
		    //TUTORIAL SCREEN
		    //Launch a tutorial screen if launched for the first time
		    Intent tutorialScreenIntent = new Intent(MainActivity.this, TutorialScreen.class);
		    startActivity(tutorialScreenIntent);
		}
	}		
			
			
		//This method is used by the "Contact Picker" to retrieve user picks from contacts
		private void initialize(){
		    
	        	/* THE FOLLOWING CODE INITIALIZE THE APP
	        	 */
	        	
				// read the string data from the loading activity
				String eventString = getIntent().getExtras().getString("eventString");
				if (eventString != null) {
				 
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
	 	    		ft.add(R.id.event_frame, eventFragmentList.get(i-1),String.valueOf(i));
	 	    		ft.commit();
	 	    		
	 	         }
	 	 	     
	 	 	     // setting the max value for the progress bar
	 	 	     eventProgressBar.setMax(eventFragmentList.size());
	 	 	     eventProgressBar.setProgress(1);
	 	 	     
	 	 	     
	 	 	     /* CHECK FOR THE FIRST EVENT TO SEE IF IT REQURIES RESPONSE
	 	 	      * IF IT DOES NOT, WILL ENABLE THE NEXT BUTTON
	 	 	      */
	 	 	     
	 	 	     if(!eventBeanList.get(1).isResponseNeeded()){
						
						viewGroup.setAllowNext(true);
				}else{
					
					// else disable the next (because no response need to be checked)
					viewGroup.setAllowNext(false);}
			}
		}

	private void uploadResponse(){
        
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
		for (int i = 0; i < eventFragmentList.size(); i++)
		{
			// if the event is survey question, save it to the JSON file
			if(eventFragmentList.get(i).getEventbean().getEventType().equals("SURVEY_QUESTION")){

				int qid = eventFragmentList.get(i).getEventbean().getQid();
				
				// check first if there exists choice, text or tie response
				String text_response = eventFragmentList.get(i).getEventbean().getTextResponse();
				String choice_response = eventFragmentList.get(i).getEventbean().getChoiceResponse();
				String tie_response = eventFragmentList.get(i).getEventbean().getTieResponse();
				
				if(text_response.length()>1 || choice_response.length()>1 || tie_response.length()>1){
				
					JSONObject question_answer = new JSONObject();  // Creating individual response_record
					
					String total_response = "";
					
					//append the choice response
					if(choice_response.length()>1)total_response = choice_response;
					
					//append the text response
					if(text_response.length()>1)
						
						{
							if(choice_response.length() > 1)total_response = total_response +"||" + text_response;
							else total_response = text_response;
						}
					
//					//append the tie response
					
					if(tie_response.length()>1)
						
					{
						if(choice_response.length() > 1 || text_response.length()>1)
							total_response = total_response +"||" + tie_response;
						else total_response = tie_response;
					}
							
					
					// the following add fields to the JSON object
					try {
	
						question_answer.put("answer",total_response);
						question_answer.put("question_id",qid);
						
					} catch (JSONException e) {
						e.printStackTrace();
					}
					
					survey_response.put(question_answer);
					
				}
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
		
		//form a JSON file upload all generated ties now to the server
        ArrayList<NameValuePair> criteriaResultUpload = new ArrayList<NameValuePair>();
		
		Log.v("pid","pid="+pid);
		Log.v("did","did="+did);
		
		// Using a list of nameValuePairs to store POST data
		criteriaResultUpload.add(new BasicNameValuePair("participant_id",pid));
		criteriaResultUpload.add(new BasicNameValuePair("device_id",did));
		
		// Putting ties into a JSON array
		
		JSONArray criteria_resultArray = new JSONArray(); // Creating an array to store tie generated
		
		// iterate through the tie pool to get ties
		for (int i=0; i < tiePool.size(); i++)
			
		{
			Tie tie = tiePool.get(i);
			
			// do a check first to make sure it is not NONAME
			if(tie.getName()!="NONAME"){
				
				//read everything from the tie
				String qid = tie.getQid()+"";
				String criteria_id = tie.getCriteria().getId()+"";
				String hashed_tie = Encoder.hashPhoneNumber(tie.getPhone_number());
				
				JSONObject criteria_result = new JSONObject();  // Creating individual criteria_result_record
				
					// the following add fields to the JSON object
					try {
	
						criteria_result.put("question_id",qid);
						criteria_result.put("criteria_id", criteria_id);
						criteria_result.put("tie_hash", hashed_tie);
						
					} catch (JSONException e) {
						e.printStackTrace();
					}
					
					criteria_resultArray.put(criteria_result);
				
			}
		}
		
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
			
			//initiate the questionskip list;
			questionSkipList = new ArrayList<JSONObject>();
			
			//initiate the branchenabledqid set
			branchEnabledQid = new HashSet<Integer>();
			
			String eventDataString = "";
			String questionSkipString = "";
			
			//IMPROVE THIS TO PARSE JSON OBJECTS
			try {
				
				JSONObject eventJSONObj = new JSONObject(eventString);
				
				// retrieve the event data string
				eventDataString = eventJSONObj.getString("event_data");
				
				// retrieve the question skipping data string
				questionSkipString = eventJSONObj.getString("question_skip");
				
				JSONArray qskipJSONArray = new JSONArray(questionSkipString);
				
				// iterate through the question skipp JSON Array List and add to list
				for (int i = 0; i < qskipJSONArray.length(); i++) {
					
					JSONObject qskipJSONObj = qskipJSONArray.getJSONObject(i);
					
					questionSkipList.add(qskipJSONObj);
					
					//register this qid in the list that indicates all branch enabled questions
					branchEnabledQid.add(qskipJSONObj.getInt("question_id"));
					
					Log.v("debugtag",qskipJSONObj.toString());
				}
				
				
			} catch (JSONException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				
			} 
			
			
			//Retrieve the three sub strings first
			String textString = eventDataString.substring(eventDataString.indexOf("\"TEXT_DISPLAY\""),
					eventDataString.indexOf("\"TIE_DISPLAY\"")-1).replace("\"TEXT_DISPLAY\",","");
			
			String tieString = eventDataString.substring(eventDataString.indexOf("\"TIE_DISPLAY\""),
					eventDataString.indexOf("\"SURVEY_QUESTION\"")-1).replace("\"TIE_DISPLAY\",","");
			
			String questionString = eventDataString.substring(eventDataString.indexOf("\"SURVEY_QUESTION\""),
					eventDataString.indexOf("]")).replace("\"SURVEY_QUESTION\",","");
			
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
					
					//SETTING Branch Enabled/Disabled for the event
					if(branchEnabledQid.contains(qid)){
						
						ebean.setBranchEnabled(true);
						
					}else {ebean.setBranchEnabled(false);}
					
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
		
		//if first event is OK to continue, then enable
		if(!eventBeanList.get(1).isResponseNeeded())setButtonEnable(btnNext);
			
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
	    		if(viewGroup.getCurScreen() < viewGroup.getChildCount()-1){
	    			
	    			Log.v("debugtag`", "VGCurrent="+viewGroup.getCurScreen()+"/"+"VGChildCount="+viewGroup.getChildCount());
	    			
	    			//switch to the next screen
	    			viewGroup.snapToScreen(viewGroup.getCurScreen()+1);
	    		
	    		}else if(readyUpload){
	    			
	    			// THE FOLLOWING CODE UPLOADS THE RESPONSES
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
	    		
	    		return true;
	    	
	        case R.id.action_help:
	        	
	        	//TUTORIAL SCREEN
			    //Launch a tutorial screen if launched for the first time
			    Intent tutorialScreenIntent = new Intent(MainActivity.this, TutorialScreen.class);
			    startActivity(tutorialScreenIntent);
		        
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
	    		                	  MainActivity.this.finish();
	    		                	  
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
	    		                	  MainActivity.this.finish();
	    		                	  
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
		
		private void toggleReadyUploadState(boolean isReady){
			
			if(isReady){
				
				btnNext.setIcon(R.drawable.ic_action_accept);
				readyUpload = true; //set ready upload state to true
			 	Toast.makeText(
	           		MainActivity.this,
	                   "       === THANK YOU ===\n" +
	                   "Please press the ¡Ì button to end.",
	                   Toast.LENGTH_SHORT).show();
			 	
			}else{
				
				btnNext.setIcon(R.drawable.ic_action_next);
				readyUpload = false; //set ready upload state to true
				
			}
			
			
		}

		
		// this handles the event triggered in the fragment
		// which is that: the use has provided response for that event
		
		@Override
		public void onEventResponded(int event_id, int question_id, String qtype, String responseString) {
			
			// TODO Auto-generated method stub
			
			//check if there's response string then enable going next
			if(!responseString.equals("-1")){setButtonEnable(btnNext);viewGroup.setAllowNext(true);}
			else {setButtonDisable(btnNext);viewGroup.setAllowNext(false);}
			
			// HERE THE APP CHECKS THE "QUESTION SKIPPING" BASED ON USER CHOICE
			// THE MECHANISM IS: FOR every question responded, check the question skip list for matches
			// first get the selected question id and question choice index
			 
			// CODE: CONTAINS A STRING OF FOUR VALUES
	        // QID(int), QUESTION_TYPE("S"(single) or "M"(multiple)),
	        // CHOICE_INDEX(int for Single, String for multiple)
			
			
			//Prerequisite: this question has enabled branching
			if (branchEnabledQid.contains(question_id)){
			
				// DEPENDING ON IF THE EVENT IS SINGLE/MULTIPLE CHOICE
				// Compare the question index to determine whether to skip
				if (qtype.equals("S")){
					
					int qid = 0;
					int option_index = 0;
					int qid_toskip = 0;
					
					// Find the corresponding JSONObject in the list
					for (JSONObject qskipObj:questionSkipList){
						
						try {
							
							qid = qskipObj.getInt("question_id");
							
							option_index = qskipObj.getInt("option_index");
							
							qid_toskip =  qskipObj.getInt("question_id_to_skip");
							
						} catch (JSONException e) {
		
							// TODO Auto-generated catch block
							e.printStackTrace();
							continue;
						}
						
						// check if question id and choice index matches
						if(question_id == qid && option_index == Integer.parseInt(responseString)){
								
								for (int j = 0; j < eventFragmentList.size(); j++){
									
									int event_qid = eventFragmentList.get(j).getEventbean().getQid();
									 
									if (qid_toskip == event_qid){
										
										// if finds match, check the fr agments to hide(skip) the intended question
										// This will remove it from the VIEWGROUP but NOT from the eventFragmentList
										
										FragmentTransaction removeFragmentTran = getFragmentManager().beginTransaction();
										removeFragmentTran.remove(getFragmentManager().findFragmentByTag(String.valueOf(j+1))); //j+1 because tag started with 1
										
										//remove fragment from the view group and the list
										eventFragmentList.remove(j);
							
										removeFragmentTran.commit();
										
										Log.v("debugtag","SkippedEvent#="+j+", currentEvent#="+event_id);
												
										//if already found the one to remove, no need to continue search
										//ONLY BECAUSE this is a single choice questions
										break;
										
										}
									
									}
								
						}
					}
						
					}else if (qtype.equals("M")){
						
						// IF THIS IS A MULTIPLE CHOICE, THEN RETRIEVE THE SELECTED BOXES
						// AND SKIP THE CORRESPONDING QUESTIONS
						
					};
			}
			
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
				
			}else if(currentScreen == viewGroup.getChildCount()-1){
				
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
						currentFrag.getEventbean().getChoiceResponse().length() > 1 ||
							currentFrag.getEventbean().getTieResponse().length() > 1){
					
						setButtonEnable(btnNext);
						viewGroup.setAllowNext(true);
						
				}
				/* If no response is expected: no question choices, no enter buttons
				 * Then the "next" button will allow the user to continue
				 */
				
				else if(!currentFrag.getEventbean().isSelectCallLog() && 
						!currentFrag.getEventbean().isSelectContacts() && 
						!currentFrag.getEventbean().isEnterManually() && 
						!currentFrag.getEventbean().isEnterText()
						&& currentFrag.getEventbean().getChoicecount() == 0){
					
						setButtonEnable(btnNext);
						viewGroup.setAllowNext(true);
				}
		
			
			//DO A CHECK IF IT IS THE FINAL SCREEN, IF IT IS (AND NO RESPONSE REQUIRED), CHANGE NEXT ICON
			if(currentScreen == viewGroup.getChildCount()-1){
				
				//IF NO RESPONSE REQUIRED
				if(!currentFrag.getEventbean().isSelectCallLog() && 
						!currentFrag.getEventbean().isSelectContacts() && 
						!currentFrag.getEventbean().isEnterManually() && 
						!currentFrag.getEventbean().isEnterText()
						&& currentFrag.getEventbean().getChoicecount() == 0){
				
						// means ready to upload
						toggleReadyUploadState(true);
					
				//OR IF RESPONSE REQUIRED, BUT ALREADY ANSWERED
				}else if(currentFrag.getEventbean().getChoiceResponse().length()>1 ||
						currentFrag.getEventbean().getTextResponse().length()>1 ||
						currentFrag.getEventbean().getTieResponse().length()>1)
					{
					
						// means ready to upload
						toggleReadyUploadState(true);
					
					}
				
			// else IF NOT AT THE FINAL SCREEN, NEVER END AND UPLOAD
			}else {toggleReadyUploadState(false);}	
		}


		@Override
		public void onTieGenerated(Tie tie) {
			// TODO Auto-generated method stub
			
			// Once the activity knows that a tie is generated, it will register it in the tie pool
			if(tie!=null)
			{
				tiePool.add(tie);
//				Log.v("debugtag","registered:"+tie.toString());
				
			}
			
		}
		
}
