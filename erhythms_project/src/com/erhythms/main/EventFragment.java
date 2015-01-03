/**
 * 
 */
package com.erhythms.main;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

import com.erhythms.eventbeans.EventBean;
import com.erhythms.eventbeans.Tie;
import com.erhythms.eventbeans.TieCriteria;
import com.erhythms.eventbeans.TieGenerator;
import com.erhythms.network.Encoder;
import com.erhythmsproject.erhythmsapp.R;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Fragment;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.provider.ContactsContract.Contacts;
import android.provider.ContactsContract.Data;
import android.provider.ContactsContract.CommonDataKinds.Phone;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.RadioGroup.OnCheckedChangeListener;

public class EventFragment extends Fragment {

	//this is the event bean that this fragment
    //will use to update its user interface
    private EventBean eventbean;
    
	private Button btnContacts = null;
	private Button btnCallLog = null;
	private Button btnEnterNumManually = null;
	private Button btnEnterText = null;

	//initiate the text view for showing text
	private TextView title_tv = null;
	private TextView textbody_tv = null;
	private TextView response_tv = null;
	private TextView hint_tv = null;
	
	//initializing all UI elements
	private RadioGroup radioGroup = null;
	
	// this is the input layout for check boxes
	private LinearLayout checkboxLayout = null;
	
    // this is the call back for communication with activity
	private OnEventRespondedListener responseCallBack;
	
	// this is the call back for communication with activity
	private OnTieGeneratedListener tieGenerateCallBack;
	
	//Use for identified by the OnActivityResult to recognize result from contact picker
	private static final int CONTACT_PICKER_RESULT = 1001;
	
	/* TIE POOL is used in this application for multiple purposes
	 * The mapping is indexed by question id, so <Integer, Tie> will be assigned <QuestionID, Tie>
	 * In dealing with the selection methods: it is used  for the three selection methods: random, walk_through and select from previous
	 * For walk_through, it ensures no same name is selected by checking the namePool
	 * For use previous tie, it checks this pool to find a previously used tie
	 * 
	 * THIS TIE POOL IS A LOCAL COPY OF THE TIE POOL PASSED FRMO THE ACTIVITY
	 * It's like a snap shot of the ties that has been generated from the previous ties
	 * 
	 */
	private ArrayList<Tie> local_tiePool = new ArrayList<Tie>();
	
	
	public static EventFragment getInstance(Bundle bundle) {
		EventFragment contentFragment = new EventFragment();
		contentFragment.setArguments(bundle);
		return contentFragment;
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		// TODO Auto-generated method stub
		View fragmentView = inflater.inflate(R.layout.fragment_activity, null);
		
        // initializing all UI elements
		radioGroup = (RadioGroup)fragmentView.findViewById(R.id.choice_group);
		
		// initiate the linear layout for check boxes
		checkboxLayout = (LinearLayout)fragmentView.findViewById(R.id.checkboxsLayout);
	
		btnContacts = (Button)fragmentView.findViewById(R.id.choose_addressbook);
		btnCallLog = (Button)fragmentView.findViewById(R.id.choose_calllog);
		btnEnterNumManually = (Button)fragmentView.findViewById(R.id.enter_manually);
		btnEnterText = (Button)fragmentView.findViewById(R.id.enterText);
		
		title_tv = (TextView)fragmentView.findViewById(R.id.question_title);
		textbody_tv = (TextView)fragmentView.findViewById(R.id.question_body);
		response_tv = (TextView)fragmentView.findViewById(R.id.response_text);
		hint_tv = (TextView)fragmentView.findViewById(R.id.hint_text);
		
		radioGroup.setVisibility(View.GONE);
		btnContacts.setVisibility(View.GONE);
		btnCallLog.setVisibility(View.GONE);
		btnEnterNumManually.setVisibility(View.GONE);
		btnEnterText.setVisibility(View.GONE);
		response_tv.setVisibility(View.GONE);
		
		// setting listener for the pick contacts button
		btnContacts.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
			
				Intent contactPickerIntent = new Intent(Intent.ACTION_PICK,Contacts.CONTENT_URI);
			    startActivityForResult(contactPickerIntent, CONTACT_PICKER_RESULT);
			}
		});
		
		
		// setting listener for the select from call log button
		btnCallLog.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
			
				String[] strFields = {android.provider.CallLog.Calls._ID,
                        android.provider.CallLog.Calls.NUMBER,
                        android.provider.CallLog.Calls.CACHED_NAME};
				
                String strOrder = android.provider.CallLog.Calls.DATE + " DESC";
                
                final Cursor cursorCall = getActivity().getContentResolver().query(
                        android.provider.CallLog.Calls.CONTENT_URI, strFields,
                        null, null, strOrder);

                AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
                
                builder.setTitle("Select from Call History");
                
                android.content.DialogInterface.OnClickListener listener = new DialogInterface.OnClickListener() {
                 
					public void onClick(DialogInterface dialogInterface, int item) {
                    	
                        cursorCall.moveToPosition(item);
                        
                        String selected_number = cursorCall.getString(cursorCall
                                .getColumnIndex(android.provider.CallLog.Calls.NUMBER));
                        
                        cursorCall.close();
                        
                        Toast.makeText(
                        		getActivity(),
                                "You've selected:  "+selected_number+"\nas your response.\nPress next button to continue",
                                Toast.LENGTH_SHORT).show();
                        
                        String reponseNum = Encoder.hashPhoneNumber(selected_number);
                        
                        // setting the response for that survey question
                        eventbean.setTieResponse(reponseNum);
	    				
						// remove the three selection buttons
//									btnContacts.setVisibility(View.GONE);
//									btnCallLog.setVisibility(View.GONE);
//									btnEnterManually.setVisibility(View.GONE);

						// show user input as question answer
						response_tv.setVisibility(View.VISIBLE);
						response_tv.setText(selected_number);
						response_tv.setTextAppearance(getActivity(), R.style.responseText);
						
						// notify the activity to enable going next
						responseCallBack.onEventResponded(eventbean.getIndex(),eventbean.getQid(),"X","0");
						
                        return;
                    }
                };
                
                builder.setCursor(cursorCall, listener, android.provider.CallLog.Calls.NUMBER);
                
                builder.create().show();
                
			}
		});
			
		btnEnterNumManually.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				
				// get prompts.xml view
				LayoutInflater li = LayoutInflater.from(getActivity());
				View promptsView = li.inflate(R.layout.number_prompts, null);
 
				AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(getActivity());
 
				// set prompts.xml to alert dialog builder
				alertDialogBuilder.setView(promptsView);
 
				final EditText userInput = (EditText) promptsView
						.findViewById(R.id.editTextDialogUserInput);
 
				// set dialog message
				alertDialogBuilder
					.setCancelable(false)
					.setPositiveButton("OK",
					  new DialogInterface.OnClickListener() {
					    public void onClick(DialogInterface dialog,int id) {
						// get user input and set it to result
						// edit text
						
					    String input_text = userInput.getText().toString();
					    
					    if (!input_text.isEmpty()){
					    	
								Toast.makeText(
										getActivity(),
		                                "You've entered:  "+userInput.getText()+"\nas your response.\nPress next button to continue.",
		                                Toast.LENGTH_SHORT).show();
								
								//hash the number before set as response
								input_text = Encoder.hashPhoneNumber(input_text);
								
								//setting the response for that survey question
								eventbean.setTieResponse(input_text);
					
								response_tv.setVisibility(View.VISIBLE);
								
								// show user input as question answer
								String response = userInput.getText().toString();
								response_tv.setText(response);
								response_tv.setTextAppearance(getActivity(), R.style.responseText);
								
								// notify the activity to enable going next
								responseCallBack.onEventResponded(eventbean.getIndex(),eventbean.getQid(),"X","0");
								
								
					    	}else{
					    		
					    		Toast.makeText(
					    				getActivity(),
		                                "You have NOT entered your response.",
		                                Toast.LENGTH_SHORT).show();
					    		
					    	}
						
					    }
					  })
					.setNegativeButton("Cancel",
					  new DialogInterface.OnClickListener() {
					    public void onClick(DialogInterface dialog,int id) {
						dialog.cancel();
					    }
					  });
 
				// create alert dialog
				AlertDialog alertDialog = alertDialogBuilder.create();
			
				// show it
				alertDialog.show();
			}
		});
			
			
		btnEnterText.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				
				// get prompts.xml view
				LayoutInflater li = LayoutInflater.from(getActivity());
				View promptsView = li.inflate(R.layout.text_prompts, null);
 				
				AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(getActivity());
 
				// set prompts.xml to alert dialog builder
				alertDialogBuilder.setView(promptsView);
 
				final EditText userInput = (EditText) promptsView
						.findViewById(R.id.editTextDialogUserInput);
 
				// set dialog message
				alertDialogBuilder
					.setCancelable(false)
					.setPositiveButton("OK",
					  new DialogInterface.OnClickListener() {
					    public void onClick(DialogInterface dialog,int id) {
						// get user input and set it to result
						// edit text
						
					    String input_text = userInput.getText().toString(); 
					    
					    if (!input_text.isEmpty()){
					    	
								Toast.makeText(
										getActivity(),
		                                "You've entered:  "+userInput.getText()+"\nas your response.\nPress next button to continue.",
		                                Toast.LENGTH_SHORT).show();
								
								// setting the response for that survey question
								eventbean.setTextResponse(input_text);
			    				
								response_tv.setVisibility(View.VISIBLE);
								
								// show user input as question answer
								String response = userInput.getText().toString();
								response_tv.setText(response);
								response_tv.setTextAppearance(getActivity(), R.style.responseText);
								
								// notify the activity to enable going next
								responseCallBack.onEventResponded(eventbean.getIndex(),eventbean.getQid(),"X","0");
																
					    	}else{
					    		
					    		Toast.makeText(
					    				getActivity(),
		                                "You have NOT entered your response.",
		                                Toast.LENGTH_SHORT).show();
					    		
					    	}
					    }
					  })
					.setNegativeButton("Cancel",
					  new DialogInterface.OnClickListener() {
					    public void onClick(DialogInterface dialog,int id) {
						dialog.cancel();
					    }
					  });
 
				// create alert dialog
				AlertDialog alertDialog = alertDialogBuilder.create();
 
				// show it
				alertDialog.show();
			}
		});
		
		
		return fragmentView;
	}

	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent data) {
		// TODO Auto-generated method stub
		super.onActivityResult(requestCode, resultCode, data);
		
		if (requestCode == CONTACT_PICKER_RESULT) {
			
			if (resultCode == Activity.RESULT_OK) {
	        	 
	             Cursor cursor = null;

	             try {
	                 Uri result = data.getData();
	                 
	                 String contact_name = "";
	                 String contact_num = "";
	                 
	                 //Get Name
	                 cursor = getActivity().getContentResolver().query(result, null, null, null, null);
	                 if (cursor.moveToFirst()) {
	                    
	                	// this is the contact name picked by the user
	                	contact_name = cursor.getString(cursor.getColumnIndex(ContactsContract.Contacts.DISPLAY_NAME));
	                	
	                	/* THE FOLLOWING CODE QUERIES AGAIN WITH THE ID TO GET THE PHONE NUMBER
	                	 */
	                	
	                	String contactId = cursor.getString(cursor.getColumnIndex(ContactsContract.Contacts._ID));
	                	
	                	Cursor c1 = getActivity().getContentResolver().query(Data.CONTENT_URI,
	                		    new String[] {Data._ID, Phone.NUMBER, Phone.TYPE, Phone.LABEL},
	                		    Data.CONTACT_ID + "=?" + " AND "
	                		    + Data.MIMETYPE + "='" + Phone.CONTENT_ITEM_TYPE + "'",
	                		    new String[] {String.valueOf(contactId)}, null);
	                		c1.moveToFirst();
	                	
	                	contact_num = c1.getString(1);
	                	
	                 	Toast.makeText(
                                getActivity(),
                                "You've picked:  "+contact_name+"\nas your response.\nPress next button to continue",
                                        Toast.LENGTH_SHORT).show();
	                 	
	                 	//hash the contact number as response
	                 	contact_num = Encoder.hashPhoneNumber(contact_num);
	                 	
	                 	//setting the response for that survey question
	    				eventbean.setTieResponse(contact_num);
	                 	
						response_tv.setText(contact_name);
						response_tv.setVisibility(View.VISIBLE);
						
						// notify the activity to enable going next
						responseCallBack.onEventResponded(eventbean.getIndex(),eventbean.getQid(),"X","0");
						
						
	                 	} 
	                 }catch (Exception e) {Log.w("debugtag", "Warning: contact picker error");}
	             
				}else{Log.w("debugtag", "Warning: activity result not ok");}
			}
		
	}

	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		
		super.onActivityCreated(savedInstanceState);
		
		// get arguments from the bundle and set them in the fragment
		if (getArguments() != null) {
			
			try{
				
				local_tiePool = (ArrayList<Tie>) getArguments().getSerializable("tiePool");
				
				
			}catch(Exception e){
				
				Log.v("debugtag",e.toString());
			}
		
		}
		
		// update the UI of the fragment with provided parameters
		updateUI();
		
	}

	    // Container Activity must implement this interface
	    public interface OnEventRespondedListener {
	    	
	    	// boolean value to decide if let go next
	        public void onEventResponded(int eventID, int qid, String qtype, String responseString);
	        
	        // CODE: CONTAINS A STRING OF FOUR VALUES
	        // Event ID(int), QID(int), QUESTION_TYPE("S"(single) or "M"(multiple)),
	        // CHOICE_INDEX(int for Single, String for multiple)
	        
	    }
	    
	    
	    // Container Activity must implement this interface
	    public interface OnTieGeneratedListener {
	        public void onTieGenerated(Tie tie);
	    }
	    

	    @Override
	    public void onAttach(Activity activity) {
	        super.onAttach(activity);
	        
	        // This makes sure that the container activity has implemented
	        // the callback interface. If not, it throws an exception
	        try {
	            responseCallBack = (OnEventRespondedListener) activity;
	            tieGenerateCallBack = (OnTieGeneratedListener) activity;
	            
	        } catch (ClassCastException e) {
	            throw new ClassCastException(activity.toString()
	                    + " must implement OnHeadlineSelectedListener");
	        }
	    }
	    
	
	
    private void updateUI(){
		
		// identify the event type
		String eventType = eventbean.getEventType();
	
		//THE FOLLOWING MAKE EVERYTHING DISAPPEAR BEFORE UPDATING THE UI
		
		// set invisible text views not used and enable based on request
		response_tv.setVisibility(View.GONE);
		btnContacts.setVisibility(View.GONE);
		btnCallLog.setVisibility(View.GONE);
		btnEnterNumManually.setVisibility(View.GONE);
		btnEnterText.setVisibility(View.GONE);
		hint_tv.setVisibility(View.GONE);
		
		// IF THERE ARE ALREADY INPUT TEXT REPONSE, THEN SHOW
		if (eventbean.getTextResponse().length() > 1){
			
			response_tv.setVisibility(View.VISIBLE);
			response_tv.setText(eventbean.getTextResponse());
			
			}
		
		// Clean up all the items in the radio group and check box layout
		radioGroup.removeAllViews();
		checkboxLayout.removeAllViews();
		
		// based on the event type, dynamically change the user interface
		if (eventType.equals("TEXT_DISPLAY")){
			
			// get text value from bean
			String textbody = eventbean.getTextbody().replace("\"", "");
			
			//update user interface
			title_tv.setVisibility(View.GONE);
			textbody_tv.setText(textbody);
			textbody_tv.setTextAppearance(getActivity(), R.style.normalText);
			radioGroup.setVisibility(View.GONE);
			
		}
		
		else if(eventType.equals("TIE_DISPLAY")){
			
			// getting the list of tie criteria associated with this event
			TieCriteria tieCriteria = eventbean.getTieCriteria().get(0);
			
			// initiate a String and a ArrayList to store names
			Tie tie = null;
			
			TieGenerator tieGT = new TieGenerator(getActivity());
			
			if(eventbean.getDynamicText().length()<1){
			
				/* The following applies SELECTION METHOD = Random
				 * As only random selection is used in TIE DISPLAY event
				 */
				
				try{
					int nameCount = tieGT.countTies(tieCriteria);
					
					// randomize to get a random name
					Random rand = new Random();
					int place = rand.nextInt(nameCount)+1;
					tieCriteria.setPlace(place);
					
					// get a name based on the tie criteria
					tie = tieGT.getTieByCriteria(tieCriteria);
				
				}catch(Exception e){
					
					Toast.makeText(
                            getActivity(),"No name in your address book fits the criteria.",
                                    Toast.LENGTH_SHORT).show();
					
					tie = new Tie(eventbean.getQid(),"NONAME", "NONAME", tieCriteria);
					
				}
			
				eventbean.setDynamicText(tie.getName());
				
				//show in text view
				textbody_tv.setText(tie.getName());
			}
			
				else{
				
				//show in text view
				textbody_tv.setText(eventbean.getDynamicText());
				
			}
			
			
			//update user interface
//			title_tv.setText(eventIndex+".Tie Display");
			title_tv.setVisibility(View.GONE);
			textbody_tv.setTextAppearance(getActivity(), R.style.tieText);
			radioGroup.setVisibility(View.GONE);
		}
		
		else if(eventType.equals("SURVEY_QUESTION")){
			
			//IF EVENT IS SURVEY QUESTIONS 
			//disable confirm button every time to DISALLOW skipping questions
			
			// setting the text for the question title text view
//			String qtitle = eventIndex+".Survey Question";
			
			// setting the text for the question body text view
			String qbody = eventbean.getTextbody().replace("\"", "");
			
			int choiceTotal = eventbean.getChoicecount();
			
			// setting the current question title
//			title_tv.setText(qtitle);
			title_tv.setVisibility(View.GONE);
			
			// BRANCH ENABLED ONLY
			// if enabled branch, the hint will show that response can't be changed
			if (eventbean.isBranchEnabled()){
				
				hint_tv.setVisibility(View.VISIBLE);
				hint_tv.setText(R.string.branchenabled_hint);
				
			}
			
			/* THE FOLLOWING APPLIES TO: DYNAMIC QUESTIONS
			 * If the survey question requires dynamic text, will based on it
			 * to modify the text body
			 */
			
			// getting the list of tie criteria associated with this event
			ArrayList<TieCriteria> criteriaList = eventbean.getTieCriteria();
			
			// if it is NOT empty, then there's dynamic text to show
			if(!criteriaList.isEmpty()){
				
				// rearrange the array list in order of text position
				// this is necessary for later inserting the text into question body
				Comparator <TieCriteria> compareTextPosition = new Comparator<TieCriteria>(){

					@Override
					public int compare(TieCriteria tc1, TieCriteria tc2) {
						
						if (tc1.getTextPosition()!=tc2.getTextPosition()){
							return tc1.getTextPosition() - tc2.getTextPosition(); 
						}
						
						return 0;
					}
				};
				
				// rearrange the list in order of text position
				Collections.sort(criteriaList,compareTextPosition);
				
				// this array list is used locally, to make sure multiple tie criteria
				// in a single question does not generate the same name
				ArrayList<String> event_local_namepool = new ArrayList<String>();

				TieGenerator tieGT = new TieGenerator(getActivity());
				
				
				//IF THERE ARE NO EXISTING GENERATED TIES(NEW EVENT)
				/* THE if(event.getDynamicText().length()<1) IS USED FOR THE "BACK" 
				 * CHECK FIRST IF THE TIE HAS ALREADY BEEN GENERATED
				 * IF IT DOES, THEN USE PREVIOUSLY GENERATED TIES
				 */
				
				if(eventbean.getDynamicText().length()<1){
				
					// this value is added up as tie is inserted into the question
					int differPos = 0;
				
				for (TieCriteria tieCriteria:criteriaList){
					
					/* The following applies to when
					 * SELECTION METHOD = Random
					 */
					Tie tie = null;
					
					if (tieCriteria.getMethod().equals("random")){
						
						try {
							
							int nameCount = tieGT.countTies(tieCriteria);
							
							do{
							// randomize to get a random name
							Random rand = new Random();
							int place = rand.nextInt(nameCount)+1;
							tieCriteria.setPlace(place);
							
							// get a name based on the tie criteria
							tie = tieGT.getTieByCriteria(tieCriteria);
							
							}while(event_local_namepool.indexOf(tie.getName())!=-1);
							
							// assigning question id and event index to the tie
							tie.setQid(eventbean.getQid());
							
							} catch (Exception e) {
							
							tie = new Tie(eventbean.getQid(),"NONAME", "NONAME", tieCriteria);

						}
						
						// register the name in the name pool
						tieGenerateCallBack.onTieGenerated(tie);
						
					}else if (tieCriteria.getMethod().equals("walk_through")){
					
						/* the extra step in WALK THROUGH is just to check the name Pool
						 * and make sure the name does not appear again.
						 */
						
						try {
							
							int nameCount = tieGT.countTies(tieCriteria);
							
							do{
							// randomize to get a random name
							Random rand = new Random();
							int place = rand.nextInt(nameCount)+1;
							tieCriteria.setPlace(place);
							
							// get a name based on the tie criteria
							tie = tieGT.getTieByCriteria(tieCriteria);
							
							}while(local_tiePool.contains(tie) || //PAY ATTENTION IF IT WORKS
									event_local_namepool.indexOf(tie.getName())!=-1);
							
							// assigning question id to the tie
							tie.setQid(eventbean.getQid());
							
						} catch (Exception e) {
							
							tie = new Tie(eventbean.getQid(),"NONAME", "NONAME", tieCriteria);

							
						}
						
							// register the name in the name pool
							tieGenerateCallBack.onTieGenerated(tie);
							
					}else if (tieCriteria.getMethod().contains("use")){
						
						/* the extra step in USE PREVIOUS TIE is just to check the name Pool
						 * and pick up the name used in a previous question.
						 */
						
						// first of all, parsing the USE PREVIOUS TIE STRING to get the question id and tie id
						String methodStr = tieCriteria.getMethod();
						int qid = Integer.parseInt(methodStr.substring(methodStr.indexOf("q")+1, methodStr.indexOf("t")));
						int criteria_id = Integer.parseInt(methodStr.substring(methodStr.indexOf("t")+1, methodStr.length()));
						
						try {
							
							// iterate through the tie pool to find a previous tie that fits the question id and criteria id
							for (int i = 0; i < local_tiePool.size(); i++){
							
								int q_id = local_tiePool.get(i).getQid();
								
								int c_id = local_tiePool.get(i).getCriteria().getId();

								// if match is found, get this tie!
								if(qid == q_id && c_id == criteria_id){
									
									// get this tie
									tie = local_tiePool.get(i);
									break;
								}
								
							}
							
							// NO NEED TO REGISTER, BECAUSE USED PREVIOUS TIE
							
							// error control: if not found previous tie, then assign NONAME
							if(tie == null)tie = new Tie(eventbean.getQid(),"NONAME", "NONAME", tieCriteria);
							
							
						} catch (Exception e) {
							
							//this should not happen, if it does there's a big warning
							Log.v("debugtag","Warning: Can't find previous tie.");
							
							tie = new Tie("NONAME", "NONAME", tieCriteria);
							
						}
						
						
					}
					
					// add to a name list to later avoid having two same names
					if(!tie.getName().equals("NONAME"))event_local_namepool.add(tie.getName());
			
					// save this name to the event bean
					eventbean.setDynamicText(eventbean.getDynamicText() + tie.getName() + "," );
					
					// get the position that this tie wants to be placed
					int position = tieCriteria.getTextPosition();
					
					// use a String Builder to insert the name into the text body
					StringBuilder inserted_qbody = new StringBuilder(qbody);
				
					int destination_position = (int)position + (int)differPos;
					
					if(destination_position > qbody.length())destination_position = qbody.length()-1;
			
					inserted_qbody.insert(destination_position,tie.getName());
					
					qbody = inserted_qbody+"";
					
					differPos = differPos + tie.getName().length();
					
				}
					
					// ELSE IF the Dynamic Text is not empty
					// meaning ties have already been generated
				
			}else{
				
					// this value is added up as tie is inserted into the question
					int differPos = 0;
					
					for (int i=0 ; i < criteriaList.size();i++){
							
							// retrieve the name from the already generated list
							String name = eventbean.getDynamicText().split(",")[i];
							
							// get the position that this tie wants to be placed
							int position = criteriaList.get(i).getTextPosition();
							
							// use a String Builder to insert the name into the text body
							StringBuilder inserted_qbody = new StringBuilder(qbody);
						
							int destination_position = (int)position + (int)differPos;
							
							if(destination_position > qbody.length())destination_position = qbody.length()-1;
					
							inserted_qbody.insert(destination_position,name);
							
							qbody = inserted_qbody+"";
							
							differPos = differPos + name.length();
							
						}
						
				}//correspond to the else
				
			}

			// setting the question body
			textbody_tv.setText(qbody);
			
			textbody_tv.setTextAppearance(getActivity(), R.style.normalText);
			
			// identify the question type
			
			String questionType = eventbean.getQuestionType();
			
			/* Based on the question type (single or multiple)
			 * Different UI updates code will be initiated
			 * 1. Single Choice: RadioGroup and RadioButtons will be used
			 * 2. Multiple Choice: Check boxes will be used
			 */
			
			
			if (questionType.equals("single")){
				
				/*1. Single Choice Questions: update radio button for every choice
				 * 
				 * The following code has been changed from its previous version
				 * Now, it allows dynamically adding the radio buttons so there are no limit of choices
				 * 
				 */
				
				radioGroup.setVisibility(View.VISIBLE);
				
				for (int i = 0 ; i < choiceTotal ; i++)
					{
						
						// getting the text from the bean and update radio button text
						String choice_text = eventbean.getChoice(i);
						
						// instantiate new radio buttons
						RadioButton choiceRadioButton = new RadioButton(getActivity());
						
						// setting the text and tag(used later to read choice text) of the radio button
						choiceRadioButton.setText(choice_text.replace("\"", ""));
						choiceRadioButton.setTag((char) ('A' + i)); // this is to set characters as tags
						
						// setting the layout and style for the radio button
						LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
				                LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
				        
				        choiceRadioButton.setLayoutParams(params);
						choiceRadioButton.setTextSize(21);
						
						radioGroup.addView(choiceRadioButton,i);
						
					}
				
				/*
				 * Setting a checked change lister for the radio Group
				 * So when it is selected, will release the "next" button
				 * And user can go to the next event
				 */
				
				radioGroup.setOnCheckedChangeListener(new OnCheckedChangeListener() {

					@Override
					public void onCheckedChanged(RadioGroup group, int checkedId) {

		            	/* Error Control
		            	 * Every time a radio button is checked/unchecked,
		            	 * it will check if there are radio buttons checked
		            	 * if not, it will disable the "next" button
		            	 */
		            	
						// if there exists radio button checked, then enable the next button
						if (checkedId != -1){
		    				
		    				RadioButton checkedButton = (RadioButton) group.findViewById(checkedId);
		    				int checkedIndex = group.indexOfChild(checkedButton) + 1;
		    				
		    				responseCallBack.onEventResponded(eventbean.getIndex(),eventbean.getQid(),"S",checkedIndex+"");
							
		    			}else {responseCallBack.onEventResponded(eventbean.getIndex(),eventbean.getQid(),"S","-1");}
		    			
						// FOR BRANCH ENABLED EVENTS ONLY
						// if branching is enabled, the response will NOT be allowed to change 
						
						if(eventbean.isBranchEnabled())disableResponse();
						
			    		//record response
			    		checkSingleChoice();
						
					}
		        }
		        ); 
				
				String responseString = eventbean.getChoiceResponse();
				
				// make sure selected tag is not null first
				if(responseString!=null){
				
					// READ FROM RESPONSE: IF HAVE PREVIOUS RESPONSE, THEN RECALL
					String selectedTag = responseString.split("\\.")[0];
					
					for(int i = 0 ; i < radioGroup.getChildCount(); i++ ){
						
						RadioButton choice =  (RadioButton)radioGroup.getChildAt(i);

						String choiceTag = choice.getTag()+"";
						
						if (choiceTag.equals(selectedTag)){((RadioButton) radioGroup.getChildAt(i)).setChecked(true);}

					}
				}
				
				
			}else if (questionType.equals("multiple")){
				/*2. Multiple Choice Questions: update check boxes for every choice
				 *Since there's no thing as "check box group" the code here just
				 *add the check boxes to the input linear layout
				 */
				
				radioGroup.setVisibility(View.GONE);
				
				// used to store checked choices, accessed by later code 
				ArrayList<String> checkedChoices = new ArrayList<String>();
				
				// if there has already been response, check the boxes
				if(eventbean.getChoiceResponse().length()>1){
					
					String responseStr = eventbean.getChoiceResponse();
					
						// parsing every choice from the response string and add them to the list
						for (int i = 0; i < responseStr.split(",").length ; i++ ){
							
							String choice = responseStr.split(",")[i].split("\\.")[0];
							checkedChoices.add(choice);
						}
					
					
				}
				
				for (int i = 0 ; i < choiceTotal ; i++)
					
					{
					
					// getting the text from the bean and update radio button text
					String choice_text = eventbean.getChoice(i);
					
					// instantiate new radio buttons
					CheckBox choiceCheckBox = new CheckBox(getActivity());
					
					// setting the text and tag(used later to read choice text) of the radio button
					choiceCheckBox.setText(choice_text.replace("\"", ""));
					choiceCheckBox.setTag((char) ('A' + i)); // this is to set characters as tags
					
					// setting the layout and style for the radio button
					LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
			                LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
			        
			        choiceCheckBox.setLayoutParams(params);
			        choiceCheckBox.setTextSize(21);
					
			        // setting the listener that will release the "next" button when checked
			        choiceCheckBox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {

			            @Override
			            public void onCheckedChanged(CompoundButton buttonView,boolean isChecked) {
							
			            	/* = Error Control =
			            	 * Every time a box is checked/unchecked,
			            	 * it will check if there are still boxes checked by the user
			            	 * if not, it will disable the "next" button
			            	 */
			            	
			            	String checkedBoxIndexString = "";
			            	
			            	for (int i =0; i < checkboxLayout.getChildCount();i++){
			            			
			            			CheckBox choiceBox = (CheckBox) checkboxLayout.getChildAt(i);
			            			
			            			// if there exists checked box, then continue and enable "next" button
			            			if(choiceBox.isChecked()){
			              				
			            				// set the choice index string format
			            				checkedBoxIndexString = checkedBoxIndexString + (i + 1) + "&";
			            				
			            			}
			            	}
			            	
			            	// if there exists checked box, then continue and enable "next" button
			            	if(!checkedBoxIndexString.equals("")){
			            		
			            		// remove the & sign at end if exists
				            		if(checkedBoxIndexString.endsWith("&")){
				            			
				            			// remove the & at the end
				            			checkedBoxIndexString = checkedBoxIndexString.substring(0, checkedBoxIndexString.length()-1);
				            			
				            		}
				            		
			            		responseCallBack.onEventResponded(eventbean.getIndex(),eventbean.getQid(),"M",checkedBoxIndexString);
			            		
			            		}
			            	
			            	else {
			            	
		            			// if no checked box is found, then disable the button
		            			// THIS IS BECAUSE, we don't want to continue without at least a box checked
			            		responseCallBack.onEventResponded(eventbean.getIndex(),eventbean.getQid(),"M","-1");
	            			
			            	}
			            	
			            	//record the response
			            	checkMultipleChoice();
			            	
			            }
			        }
			        );  
					
			        // looking up saved choices and check the selected boxes
			        if (!checkedChoices.isEmpty()){
			        	
			        	// tag of the current check box
			        	String box_tag = choiceCheckBox.getTag()+"";
			        	
			        	// compared with all checked choices in the list
			        	if (checkedChoices.contains(box_tag))choiceCheckBox.setChecked(true);// if has, then check
			        	
			        }
			        
					checkboxLayout.addView(choiceCheckBox,i);
					
				}

			}
			
			// if the question asks user to select from contacts/call log/enter manually
			// the corresponding function button will show
			if(eventbean.isSelectContacts())btnContacts.setVisibility(View.VISIBLE);
			if(eventbean.isSelectCallLog())btnCallLog.setVisibility(View.VISIBLE);
			if(eventbean.isEnterManually())btnEnterNumManually.setVisibility(View.VISIBLE);
			if(eventbean.isEnterText())btnEnterText.setVisibility(View.VISIBLE);
			
		}
		
		
	}
    
        
	   // this method gets the selected RadioButton and its answer 
	   private void checkSingleChoice(){
 			
 			// get selected radio button from radioGroup
 			int selectedId = radioGroup.getCheckedRadioButtonId();
 			
 			RadioButton selectedButton = (RadioButton)getView().findViewById(selectedId);
 			
 			if(selectedButton!=null){
 				
 				String choice = selectedButton.getTag().toString();
 				String choiceText = selectedButton.getText().toString();
 				
 				// TESTING ONLY
 				int qid = eventbean.getQid();
 				Log.v("schoice","QID="+qid+"/Choice="+choice);
 				
 				//setting the response for that survey question
 				eventbean.setChoiceResponse(choice+"."+choiceText);
 				
 			}
 			
 		}
 	
 	
	 	// this method gets all the selected Check Boxes and their answers into a string 
	 	private void checkMultipleChoice(){
 			
 			// this is the answer string containing all the checked answers
 			String answer = "";
 				
 			if(checkboxLayout.getChildCount()>=1){
 			
 				// iterate through the child check box elements of the linear view
 				for (int i = 0; i < checkboxLayout.getChildCount();i++){
 					
 					CheckBox choiceCheckbox = (CheckBox) checkboxLayout.getChildAt(i);
 					
 					// if that check box is checked, its answer will be stored
 					if (choiceCheckbox.isChecked()){
 						
 						String choiceNum = choiceCheckbox.getTag().toString();
 						String choiceText = choiceCheckbox.getText().toString();
 						
 						// append the answer to the answer text string
 						if (i == checkboxLayout.getChildCount()-1)answer = answer + choiceNum +"." + choiceText;
 						else{ answer = answer + choiceNum +"."+ choiceText + ","; }
 						
 					}
 				}
 				
 			//setting the response for that survey question
 			eventbean.setChoiceResponse(answer);
 			
 			}
 		}
    

	public EventBean getEventbean() {
		return eventbean;
	}


	public void setEventbean(EventBean eventbean) {
		this.eventbean = eventbean;
	}
	
	public void disableResponse(){
		
		for (int i = 0; i < radioGroup.getChildCount(); i++) {
			radioGroup.getChildAt(i).setEnabled(false);
		}
		
		for (int i = 0; i < checkboxLayout.getChildCount(); i++) {
			checkboxLayout.getChildAt(i).setEnabled(false);
		}
		
		btnCallLog.setEnabled(false);
		btnContacts.setEnabled(false);
		btnEnterNumManually.setEnabled(false);
		btnEnterText.setEnabled(false);
		
	}
	
}
