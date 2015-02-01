package com.erhythms.main;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ExecutionException;

import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;
import org.apache.http.util.EntityUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.erhythms.eventbeans.Tie;
import com.erhythms.eventbeans.TieCriteria;
import com.erhythms.eventbeans.TieGenerator;
import com.erhythms.network.GetLogData;
import com.erhythms.network.UploadDataTask;
import com.erhythmsproject.erhythmsapp.R;

import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;

public class ParticipantConsentActivity extends Activity {
	
	//shared preference used to retrieve app status
	private SharedPreferences appinfo = null;
	private Editor editor = null;
	
	//pid did retrieved from what is saved during activation
	private String pid = null;
	private String did = null;
	
	// Determine if participant passed the prerequisite
	private boolean eligiable;
	
	//the text view of consent form is global
	TextView formtext = null;
	
	// return code to decide whether to allow continue
	String retCode = "";
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.consent_form);
		
		formtext=(TextView)findViewById(R.id.consentForm);
		formtext.setMovementMethod(ScrollingMovementMethod.getInstance());
		
		//get agree and disagree button from layout
		Button bagree = (Button)findViewById(R.id.bagree);
		Button bdisagree = (Button)findViewById(R.id.bdisagree);
		
		bagree.setOnClickListener(new ConsentClickListener());
		bdisagree.setOnClickListener(new ConsentClickListener());
		
		//read preferences and retrieve information
		appinfo = getSharedPreferences("appinfo", Context.MODE_PRIVATE);
		pid = appinfo.getString("pid", "--");
		did = appinfo.getString("did", "--");
		
		//SETTING ELIGIBILITY TO TRUE BY DEFAULT
		eligiable = true;
		
		//checking first if network is available
		if(isNetworkConnected(getApplicationContext())){
		
		GetConsentForm getcf = new GetConsentForm();
		try {
			getcf.execute().get();
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (ExecutionException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			}
		}else{
			formtext.setText("NETWORK NOT AVAILABLE, \nPLEASE CHECK YOUR NETWORK SETTINGS");
		}
		
	}
	
	private class GetConsentForm extends AsyncTask<Void, Integer, String>{
		
		//url is the url of the web service
		String url = getApplicationContext().getResources().getString(R.string.get_participantconsent_url);
		
		@Override
		protected void onPreExecute() {
			super.onPreExecute();
			}
		    	
		@Override
		protected String doInBackground(Void... para) {

				String formContent = null;
			
				try {

					//list is a list of parameters to upload
					List<NameValuePair> params = new ArrayList<NameValuePair>();
					
					// Using a list of nameValuePairs to store POST data
					params.add(new BasicNameValuePair("participant_id",pid));
					params.add(new BasicNameValuePair("device_id",did));
					
					Log.v("rqt", params.toString());
					
					// initiating HttpPost object using URL
					HttpPost request = new HttpPost(url);
							
					// Binding Request to String Entity
					request.setEntity(new UrlEncodedFormEntity(params));
					
					HttpParams httpParameters = new BasicHttpParams();

					// Set the timeout in milliseconds until a connection is established.
					// The default value is zero, that means the timeout is not used. 
					int timeoutConnection = 8000;
					HttpConnectionParams.setConnectionTimeout(httpParameters, timeoutConnection);

					// Set the default socket timeout (SO_TIMEOUT) 
					// in milliseconds which is the timeout for waiting for data.
					int timeoutSocket = 10000;
					HttpConnectionParams.setSoTimeout(httpParameters, timeoutSocket);
							
					DefaultHttpClient httpClient = new DefaultHttpClient(httpParameters);
							
					// Sending Request to Server
					HttpResponse response = httpClient.execute(request);
					
					// Get Response stored in a Json file 
					String retSrc = EntityUtils.toString(response.getEntity()); 
					
					String resultString = retSrc.substring(retSrc.indexOf("{"),retSrc.length());
					
					// convert the string to a JSON object
					JSONObject resultJson = new JSONObject(resultString);

					Log.v("result","ParticipantFormString="+resultString);
					
					//the following parsed and retrieve the contents from the consent form
					formContent = resultJson.getString("ParticipantConsentForm");
					
					retCode = resultJson.getString("ReturnStatus");
					
					JSONObject filterJson = resultJson.getJSONObject("ParticipantFilter");
					
					int min_first_text = Integer.parseInt(filterJson.getString("min_first_text"));
					int min_first_call = Integer.parseInt(filterJson.getString("min_first_call"));
					
					/* THE FOLLOWING CODE CHECK TO SEE IF THE PARTICIPANT IS ELIGIBALE, 
					 * BASED ON THE PRE-REQUISITE REQUIREMENTS
					 * 
					 * This is achieved by simply setting a tie criteria that checks if
					 * any tie exists before the required minimum days
					 */
					
					TieCriteria checkTextTC = new TieCriteria(0, "the most", 99999, min_first_text, "text");
					TieCriteria checkCallTC = new TieCriteria(0, "the most", 99999, min_first_call, "call");
					
					// initiate a tie generator
					TieGenerator tieGT = new TieGenerator(ParticipantConsentActivity.this);
					
					Tie text_tie = null;
					Tie call_tie = null;
					
					Log.v("debugtag","checking with Criteria:"+checkTextTC.toString());
					Log.v("debugtag","checking with Criteria:"+checkCallTC.toString());
					
					try {
						text_tie = tieGT.getTieByCriteria(checkTextTC);
						
						} catch (Exception e) {
						
							text_tie = new Tie(0,"NONAME", "NONAME", checkTextTC);

					}
					
					try {
						call_tie = tieGT.getTieByCriteria(checkCallTC);
						
						} catch (Exception e) {
						
							call_tie = new Tie(0,"NONAME", "NONAME", checkCallTC);

					}
					
					// determine eligibility based on the tie generated
					if(text_tie.getName().equals("NONAME")||call_tie.getName().equals("NONAME"))eligiable = false;
					
					Log.v("debugtag","For text, get:"+text_tie.getName()+", for call, get:"+call_tie.getName()+", eligibility="+eligiable);
					
					} catch (Exception e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
						Log.v("exception",e.toString());
					}
				
					return formContent;
				}

				protected void onPostExecute(String result) {
					formtext.setText(result);
				}
	}

	class ConsentClickListener implements View.OnClickListener
	{
		// handles event when user clicks on one of the buttons
		@Override
		public void onClick(View v) {
			// TODO Auto-generated method stub
			// decide on which button is clicked
			
			if (v.getId()==R.id.bdisagree){
				//user disagree with consent form then quit
				exitNotice();
			}
			else if (v.getId()==R.id.bagree){
				
				
				if(retCode.equals("1231")){
					
					// User clicked back then return
	        	    AlertDialog alertDialog;
		            alertDialog = new AlertDialog.Builder(ParticipantConsentActivity.this).create();
		            alertDialog.setTitle("Fatal Error");
		            alertDialog.setCancelable(false);
		            alertDialog.setMessage(getResources().getString(R.string.fatal_error_message));
		            alertDialog.setButton("Ok", new DialogInterface.OnClickListener() {

		                  public void onClick(DialogInterface dialog, int id) {
		                	  
		                	  //exit the application
		                	  finish();
		                	  
		                } }); 
		            
		            alertDialog.show();
		            
					
				}else if(!eligiable){
					
					//retrieving URL of agree to consent form
					String url_consent = getApplicationContext().getResources().getString(R.string.agree_participantconsent_url);
					
					//setting the parameters to upload
					ArrayList<NameValuePair> params = new ArrayList<NameValuePair>();
					
					Log.v("pid","pid="+pid);
					Log.v("did","did="+did);
					
					// Using a list of nameValuePairs to store POST data
					params.add(new BasicNameValuePair("participant_id",pid));
					params.add(new BasicNameValuePair("device_id",did));
					params.add(new BasicNameValuePair("AgreeToParticipantConsentForm","Y"));
					params.add(new BasicNameValuePair("PassedFilter","N"));
					
					UploadDataTask sendConsent = new UploadDataTask(url_consent, params, getApplicationContext());
					
					sendConsent.execute();
					
					// User clicked back then return
	        	    AlertDialog alertDialog;
		            alertDialog = new AlertDialog.Builder(ParticipantConsentActivity.this).create();
		            alertDialog.setTitle("Warning");
		            alertDialog.setCancelable(false);
		            alertDialog.setMessage(getResources().getString(R.string.not_eligible_message));
		            alertDialog.setButton("Ok", new DialogInterface.OnClickListener() {

		                  public void onClick(DialogInterface dialog, int id) {
		                	  
		                	  //exit the application
		                	  finish();
		                	  
		                } });
		            
		            alertDialog.show();
		            
				}
				else{
				
				
				//retrieving URL of agree to consent form
				String url_consent = getApplicationContext().getResources().getString(R.string.agree_participantconsent_url);
				
				//setting the parameters to upload
				ArrayList<NameValuePair> params = new ArrayList<NameValuePair>();
				
				Log.v("pid","pid="+pid);
				Log.v("did","did="+did);
				
				// Using a list of nameValuePairs to store POST data
				params.add(new BasicNameValuePair("participant_id",pid));
				params.add(new BasicNameValuePair("device_id",did));
				params.add(new BasicNameValuePair("AgreeToParticipantConsentForm","Y"));
				params.add(new BasicNameValuePair("PassedFilter","Y"));
				
				UploadDataTask sendConsent = new UploadDataTask(url_consent, params, getApplicationContext());
				
				sendConsent.execute();
					
				/* The following code gets call log and text log data from the user's phone
				 * And upload them to the server database.
				 */
				
				UploadLogTask sendLogs = new UploadLogTask();
				sendLogs.execute();
				
				editor = appinfo.edit();
				editor.putBoolean("participant_consent", true);
				editor.commit();
				
				Intent intent = new Intent();
				intent.setClass(getApplicationContext(),LoadingScreen.class); 
				startActivity(intent);
				
				ParticipantConsentActivity.this.finish();
				
				}
			}
		}
		
	}
	
	
	
	  private class UploadLogTask extends AsyncTask<String, Integer, String> {
	    	//onPreExecute used to do some UI handling before task
	    	@Override
	    	protected void onPreExecute() {
	    		
	    	}
	    	
	    	//doInBackground, UI can't be changed in this
			@Override
			protected String doInBackground(String... params) {

					//the following get all log data from user's phone
					GetLogData gtlog = new GetLogData(ParticipantConsentActivity.this);
					JSONArray logdata = gtlog.generateAllLogData();

					Log.v("logdata","call&text_log="+logdata.toString());
					
					String call_log = null;
					String text_log = null;
					try {
						call_log = logdata.getJSONObject(0).get("call_records").toString();
						text_log = logdata.getJSONObject(1).get("text_records").toString();
					} catch (JSONException e1) {
						// TODO Auto-generated catch block
						e1.printStackTrace();
					}
					
					try {
						JSONArray calllog = new JSONArray(logdata.getJSONObject(0).get("call_records").toString());
						JSONArray textlog = new JSONArray(logdata.getJSONObject(1).get("text_records").toString());
						
						for (int i=0;i<calllog.length();i++){
							String singlecall = calllog.get(i).toString();
							Log.v("logdata","=====================CALLDATA=======================");
							Log.v("logdata",singlecall);
						}
						
						for (int i=0;i<textlog.length();i++){
							String singletext = textlog.get(i).toString();
							Log.v("logdata","=====================TEXTDATA=======================");
							Log.v("logdata",singletext);
						}
						
					} catch (JSONException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				
					
					ArrayList<NameValuePair> uploaddata = new ArrayList<NameValuePair>();
					
					// Using a list of nameValuePairs to store POST data
					uploaddata.add(new BasicNameValuePair("participant_id",pid));
					uploaddata.add(new BasicNameValuePair("device_id",did));
					uploaddata.add(new BasicNameValuePair("call_records",call_log));
					uploaddata.add(new BasicNameValuePair("text_records",text_log));
					
					Log.v("rqt", uploaddata.toString());
					
					String upload_url = getResources().getString(R.string.upload_url);
					
					//this code upload the phone logs to the database
					UploadDataTask uploadlog = new UploadDataTask(upload_url, uploaddata, ParticipantConsentActivity.this);
					uploadlog.execute();
					return "";
					
			}
			
			@Override
			protected void onPostExecute(String result) {
				
			}
			
			@Override
			protected void onCancelled() {
			
			}
	    }
	
	
	protected void exitNotice() {
		AlertDialog.Builder builder = new Builder(this);
		builder.setMessage(R.string.disagree_notice);
		builder.setTitle("Warning");
		builder.setPositiveButton("OK",
		new android.content.DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				dialog.dismiss();
				//AccoutList.this.finish();
				//System.exit(1);
				android.os.Process.killProcess(android.os.Process.myPid());
			}
		});
		builder.create().show();
	}
	
	
	//this method checks if network is available
		public boolean isNetworkConnected(Context context) { 
			if (context != null) { 
				ConnectivityManager mConnectivityManager = (ConnectivityManager)context.getSystemService(Context.CONNECTIVITY_SERVICE); 
				NetworkInfo mNetworkInfo = mConnectivityManager.getActiveNetworkInfo(); 
				if (mNetworkInfo != null) { 
					return mNetworkInfo.isAvailable(); 
				} 
			} 
			return false; 
	}

}
