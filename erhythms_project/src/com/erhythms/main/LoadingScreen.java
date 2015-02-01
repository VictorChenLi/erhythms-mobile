package com.erhythms.main;

import java.util.ArrayList;
import java.util.Calendar;

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
import org.json.JSONObject;

import com.erhythms.network.LogDBHelper;
import com.erhythmsproject.erhythmsapp.R;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.graphics.Color;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

@SuppressLint("NewApi") public class LoadingScreen extends Activity {

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
	
	private PrefetchData downloader;
	
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
		
		/* This code changes the status bar color
		 */
		
	    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
		        getWindow().addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
			    getWindow().setStatusBarColor(Color.parseColor("#77c2d7"));
		}	
		
		/*
		 * Showing splash screen while making network calls to download necessary
		 * data before launching the app Will use AsyncTask to make http call
		 */
		downloader = new PrefetchData();
		downloader.execute();
		
		// HANDLES RECONNECT WHEN CONNECT FAILS
		
		reconnect_button.setOnClickListener(new View.OnClickListener() {
			
			@Override
			public void onClick(View v) {
				// TODO Auto-generated method stub
				
				downloader = new PrefetchData();
				downloader.execute();
				
				reconnect_button.setVisibility(View.GONE);
				loadingBar.setVisibility(View.VISIBLE);
				loadingText.setVisibility(View.VISIBLE);
				
			}
		});
		
		
		
	}

	/*
	 * AsyncTask to make http call
	 */
	private class PrefetchData extends AsyncTask<String, String, String> {

		@Override
		protected void onPreExecute() {
			super.onPreExecute();
			
			loadingBar.setProgress(1);
			// read preferences and retrieve information
			appinfo = getSharedPreferences("appinfo", Context.MODE_PRIVATE);
			pid = appinfo.getString("pid", "--");
			did = appinfo.getString("did", "--");
			
		}

		@Override
		protected String doInBackground(String... arg0) {
			
			// CHECK FIRST IF NETWORK IS AVAILABLE
			if(!isNetworkAvailable()){
				
				Log.v("debugtag","network="+isNetworkAvailable());
				
				return "badnetwork";
				
			}else {
			
				/*
				 * GENERATE DATABASE OF ALL USER LOGS
				 * The following code will generate a database of logs
				 * And will be used by the name generator to query names
				 * 
				 */
			
				try {
				
				/* TimeStamp Mechanism for the database update
				 * Every specific date will have a unique time stamp
				 * App running on the same day will not generate database again
				 */
				
				long DB_TimeStamp = appinfo.getLong("DBtimestamp", 0);
				Calendar cal = Calendar.getInstance();
				long currentTimeStamp = Long.parseLong(cal.get(Calendar.YEAR)+""+cal.get(Calendar.MONTH)+""+
						cal.get(Calendar.DAY_OF_MONTH));
				
				
				if (DB_TimeStamp < currentTimeStamp){
					
					// create the database and insert all call and text logs to it
					dbhelper = LogDBHelper.getInstance(getApplicationContext());
					dbhelper.updateAllLogs();
					
					//record the time stamp of the updated database
					Editor appinfoEdit = appinfo.edit();
					appinfoEdit.putLong("DBtimestamp", currentTimeStamp);
					
					appinfoEdit.commit();
				}
					
					Thread.sleep(200);
				
					loadingBar.setProgress(1);
					
					Thread.sleep(200);
					
					loadingBar.setProgress(4);
				
				if(appinfo.getBoolean("activated", false)){
					
					/*
					 * RETRIEVE ALL EVENT FROM THE WEB PORTAL
					 * The following code will use a download data task to download all event data from the web portal
					 * It will then use the first event (expected to be a welcome text set by the researcher) to update UI
					 * 
					 */
					
					String event_url = getResources().getString(R.string.event_url);
					
					ArrayList<NameValuePair> requestparams = new ArrayList<NameValuePair>();
					
					// Using a list of nameValuePairs to store POST data
					requestparams.add(new BasicNameValuePair("participant_id",pid));
					requestparams.add(new BasicNameValuePair("device_id",did));
				
					
					/* THE FOLLOWING CODE DOWNLOAD DATA FROM THE WEB PORTAL
					 */
					
					// initiating HttpPost object using URL
					HttpPost request = new HttpPost(event_url);
							
					// Binding Request to String Entity
					request.setEntity(new UrlEncodedFormEntity(requestparams));
					
					HttpParams httpParameters = new BasicHttpParams();
					
					// Set the timeout in milliseconds until a connection is established.
					// The default value is zero, that means the timeout is not used. 
					int timeoutConnection = 10000;
					HttpConnectionParams.setConnectionTimeout(httpParameters, timeoutConnection);
	
					// Set the default socket timeout (SO_TIMEOUT) 
					// in milliseconds which is the timeout for waiting for data.
					int timeoutSocket = 10000;
					HttpConnectionParams.setSoTimeout(httpParameters, timeoutSocket);
							
					DefaultHttpClient httpClient = new DefaultHttpClient(httpParameters);
					
					Log.v("rqt","requestingEvents"+requestparams.toString());
					
					// Sending Request to Server
					HttpResponse response = httpClient.execute(request);
					
					// Get Response stored in a Json file 
					eventString = EntityUtils.toString(response.getEntity()); 
					
					// parsing to get the returned data
					eventString = eventString.substring(eventString.indexOf("{"),eventString.length());
					
					Log.v("result",eventString);
					
					//GETTING THE RETURN CODE
					JSONObject retObj = new JSONObject(eventString);
					String retCode = retObj.getString("ReturnStatus");
					
					if(!retCode.equals("64891")){return "no";}
					else{return "ok";}
		
			}
				
				loadingBar.setProgress(8);
				
				Thread.sleep(200);
				
				loadingBar.setProgress(10);
				
			return "ok";
				
				
				
		}catch (Exception e){
				
				// Code used to handle error with network access or parsing string
				Log.v("exceptionmessage", "exception", e);
				
				return "no";
				
				}
			
			}

		}

		@Override
		protected void onPostExecute(String result) {
			super.onPostExecute(result);
			
			if(result.equals("ok")){
			
				// After completing http call
				// will close this activity and lauch main activity
				Intent resultIntent = new Intent(LoadingScreen.this, MainActivity.class);
				if(eventString!=null)resultIntent.putExtra("eventString", eventString);			
				
				// starting the main activity
				startActivity(resultIntent);
				
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

	}
	
	private boolean isNetworkAvailable() {
	    ConnectivityManager connectivityManager 
	    = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
	    NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
	    return activeNetworkInfo != null;
	}
	
}
