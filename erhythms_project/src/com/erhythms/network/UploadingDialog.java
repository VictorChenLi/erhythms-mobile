package com.erhythms.network;

import java.util.ArrayList;
import java.util.List;

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

import com.erhythms.main.LoadingScreen;
import com.erhythms.main.MainActivity;
import com.erhythms.main.ParticipantConsentActivity;
import com.erhythms.network.LogDBHelper;
import com.erhythmsproject.erhythmsapp.R;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

public class UploadingDialog extends Activity {

	//database used to query logs
	private LogDBHelper dbhelper;
	
	//stores the event string
	private String eventString = null;
	
	// shared preference used to retrieve app status
	private SharedPreferences appinfo = null;
	
	// pid did retrieved from what is saved during activation
	private String pid = null;
	private String did = null;
	
	private Button reconnect_button;
	private ProgressBar loadingBar;
	private TextView loadingText;
	
	private UploadLogTask datauploader;
	
	@Override
	public void onBackPressed() {
		
		//Simply disable back button
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_splash);

		reconnect_button = (Button)findViewById(R.id.reconnect_button);
		loadingBar = (ProgressBar)findViewById(R.id.loadingProgress);
		loadingText = (TextView)findViewById(R.id.loadingtext);
		
		reconnect_button.setVisibility(View.GONE);
		/*
		 * Showing splash screen while making network calls to download necessary
		 * data before launching the app Will use AsyncTask to make http call
		 */
		datauploader = new UploadLogTask();
		datauploader.execute();
		
		// HANDLES RECONNECT WHEN CONNECT FAILS
		
		reconnect_button.setOnClickListener(new View.OnClickListener() {
			
			@Override
			public void onClick(View v) {
				// TODO Auto-generated method stub
				
				datauploader = new UploadLogTask();
				datauploader.execute();
				
				reconnect_button.setVisibility(View.GONE);
				loadingBar.setVisibility(View.VISIBLE);
				loadingText.setVisibility(View.VISIBLE);
				
			}
		});
		
		
		
	}

	/*
	 * AsyncTask Class for data upload
	 */
	  private class UploadLogTask extends AsyncTask<String, Integer, String> {
	    	//onPreExecute used to do some UI handling before task
	    	@Override
	    	protected void onPreExecute() {
	    		
	    	}
	    	
	    	//doInBackground, UI can't be changed in this
			@Override
			protected String doInBackground(String... params) {

					//the following get all log data from user's phone
					GetLogData gtlog = new GetLogData(UploadingDialog.this);
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
					UploadDataTask uploadlog = new UploadDataTask(upload_url, uploaddata, UploadingDialog.this);
					uploadlog.execute();
					return "ok";
					
			}
			
			@Override
			protected void onPostExecute(String result) {
				
				if(result.equals("ok")){
				
					// After completing http call
					// will close this activity and lauch main activity
					Intent resultIntent = new Intent(UploadingDialog.this, ParticipantConsentActivity.class);
					
					resultIntent.putExtra("eventString", eventString);			
					resultIntent.putExtra("isReady", true);
					setResult(Activity.RESULT_OK, resultIntent);
					
					// close this activity
					finish();
				
				}else if(result.equals("badnetwork")){
					
					//error message 
					Toast.makeText(getApplicationContext(),"Sorry, Your network is not available, \nplease check your settings.", Toast.LENGTH_SHORT).show();
					
					// make visible the retry button
					reconnect_button.setVisibility(View.VISIBLE);
					loadingBar.setVisibility(View.GONE);
					loadingText.setVisibility(View.GONE);
					
					
				}else if(result.equals("no")){

					//error message 
					Toast.makeText(getApplicationContext(),"Error downloading data, please try again", Toast.LENGTH_SHORT).show();
					
					// make visible the retry button
					reconnect_button.setVisibility(View.VISIBLE);
					loadingBar.setVisibility(View.GONE);
					loadingText.setVisibility(View.GONE);
					
				}
			}
			
			@Override
			protected void onCancelled() {
			
			}
	    }
	
	private boolean isNetworkAvailable() {
	    ConnectivityManager connectivityManager 
	    = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
	    NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
	    return activeNetworkInfo != null;
	}
	
}
