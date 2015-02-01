package com.erhythms.main;

import java.util.ArrayList;
import java.util.List;
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
import android.graphics.Typeface;

public class SystemConsentActivity extends Activity {
	
	//shared preference used to retrieve app status
	private SharedPreferences appinfo = null;
	private Editor editor = null;
	
	//pid did retrieved from what is saved during activation
	private String pid = null;
	private String did = null;
	
	//the text view of consent form is global
	TextView formtext = null;
		
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
		
		Typeface droid_font = Typeface.createFromAsset(getApplicationContext().getAssets(),"DroidSans.ttf");
		formtext.setTypeface(droid_font);
		
		
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
		String url = getApplicationContext().getResources().getString(R.string.get_systemconsent_url);
		
		@Override
		protected void onPreExecute() {
			super.onPreExecute();
			}
		    	
		@Override
		protected String doInBackground(Void... para) {
				String retCode = null;
				String consentForm = null;
				try {
					
					//list is a list of parameters to upload
					List<NameValuePair> params = new ArrayList<NameValuePair>();
					
					// Using a list of nameValuePairs to store POST data
					params.add(new BasicNameValuePair("device_id",did));
					
					Log.v("rqt", params.toString());
					
					// initiating HttpPost object using URL
					HttpPost request = new HttpPost(url);
							
					// Binding Request to String Entity
					request.setEntity(new UrlEncodedFormEntity(params));
					
					HttpParams httpParameters = new BasicHttpParams();

					// Set the timeout in milliseconds until a connection is established.
					// The default value is zero, that means the timeout is not used. 
					int timeoutConnection = 3000;
					HttpConnectionParams.setConnectionTimeout(httpParameters, timeoutConnection);

					// Set the default socket timeout (SO_TIMEOUT) 
					// in milliseconds which is the timeout for waiting for data.
					int timeoutSocket = 5000;
					HttpConnectionParams.setSoTimeout(httpParameters, timeoutSocket);
							
					DefaultHttpClient httpClient = new DefaultHttpClient(httpParameters);
							
					// Sending Request to Server
					HttpResponse response = httpClient.execute(request);
					
					// Get Response stored in a Json file 
					String retSrc = EntityUtils.toString(response.getEntity()); 
					
					retCode = retSrc.substring(retSrc.indexOf("{")+1,retSrc.indexOf("}"))
							.split(",")[0].split(":")[1]; 
					
					//the following parsed and retrieve the contents from the consent form
					consentForm = retSrc.substring(retSrc.indexOf("{")+1,retSrc.indexOf("}"))
							.split("\"SystemConsentForm\":")[1];
					
					consentForm = consentForm.substring(1,consentForm.length()-1);
					
					Log.v("result","returncode="+retSrc);
					Log.v("ttt","cf="+consentForm);
							
					} catch (Exception e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
						Log.v("exception",e.toString());
					}
				
					return consentForm;
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

				editor = appinfo.edit();
				editor.putBoolean("system_consent", true);
				editor.commit();
				
				Intent intent = new Intent();
				intent.setClass(getApplicationContext(),MainActivity.class); 
				startActivity(intent);
				
				SystemConsentActivity.this.finish();
			}
		}
		
	}
	
	protected void exitNotice() {
		AlertDialog.Builder builder = new Builder(this);
		builder.setMessage(R.string.disagree_notice);
		builder.setTitle("Notice");
		builder.setPositiveButton("Confirm",
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
