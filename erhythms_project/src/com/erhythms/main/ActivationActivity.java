package com.erhythms.main;

import java.util.ArrayList;
import java.util.List;

import org.apache.http.NameValuePair;
import org.apache.http.message.BasicNameValuePair;

import com.erhythms.network.Encoder;
import com.erhythms.network.UploadDataTask;
import com.erhythmsproject.erhythmsapp.R;

import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;

public class ActivationActivity extends Activity {
	private EditText input1 = null;
	private EditText input2 = null;
	
	private String did = "";
	private String pid = null;
	private String returncode = "--";
	
	private List<NameValuePair> params = null;
	private String url = null;
	
	private UploadDataTask uploadtask = null;
	private ProgressDialog progressDialog;
	
	private SharedPreferences appinfo = null;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activation_activity);
		input1 = (EditText)findViewById(R.id.PID_1);
		input2 = (EditText)findViewById(R.id.PID_2);
		
		//find interface elements in this activity
		Button bactivate = (Button)findViewById(R.id.activationButton);
		bactivate.setOnClickListener(new ActivationClickListener());
		
		//move cursor to input2 when input1 is full
		input1.setOnKeyListener(new inputListener());
		
		//read preferences info to see if application is first startup
		appinfo = getSharedPreferences("appinfo", Context.MODE_PRIVATE);
		
		//Getting did from user's device and encode
		TelephonyManager TelephonyMgr = (TelephonyManager)getSystemService(TELEPHONY_SERVICE); 
		this.did = Encoder.MD5Encode(TelephonyMgr.getDeviceId());
		Log.v("did","DeviceID="+did);
		
		//getting the URL of web service
		url = getApplicationContext().getResources().getString(R.string.activate_url);
		
	}
	
	//input1 listener ====THIS HASN'T BEEN SUCCESSFULLY DEVELOPED YET====
	private class inputListener implements View.OnKeyListener
	{
		@Override
		public boolean onKey(View v, int keyCode, KeyEvent event) {
			// If the text box input1 is full, auto move to input2
			return false;
		}
		
	}
	
	
	//button listener
	private class ActivationClickListener implements View.OnClickListener
	{

		@Override
		public void onClick(View v) {

			//checking first if network is available
			if(isNetworkConnected(getApplicationContext())){
			// TODO Auto-generated method stub
			String inputID = input1.getText().toString()+"-"+input2.getText().toString();
				
				//deciding if the length of entered string is valid
				if (inputID.length()!=7){
					input1.setText("");
					input2.setText("");
					Toast.makeText(getApplicationContext(),"Invalid PID, please check again.",Toast.LENGTH_SHORT).show();
					}
						else{
							
							//pass input value to pid
							pid = inputID;
							
							//initiate and start the thread to verify data
							VerifyPID();
							
							//Setting and showing the progress bar
							progressDialog = new ProgressDialog(ActivationActivity.this);
							progressDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
							progressDialog.setMessage("Verifying PID, please wait....");
							progressDialog.setIndeterminate(false);
							progressDialog.setCancelable(false);
							progressDialog.show();
							
							//this thread puts a waiting time
							new Thread(new Runnable(){  
							@Override  
								public void run() { 
										try {
											Thread.sleep(2500);
										} catch (InterruptedException e) {
											// TODO Auto-generated catch block
											e.printStackTrace();
										}
										
										//after sleeping notify handler to continue
										activateHandler.sendEmptyMessage(0); 
								      }
							}).start(); 
							
						}//when input is right
				
				}
				//if network is not connected
				else{
					Toast.makeText(getApplicationContext(), "Network not available, please check network settings",Toast.LENGTH_SHORT).show();
			}
			
		}
	}

		Handler activateHandler = new Handler(){
			@Override
			public void handleMessage(Message msg) {
				
				//changing activation status to activated
				returncode = appinfo.getString("returncode", "99999");
				Log.v("ttt","return+"+returncode);
				
				if(returncode.equals("0")){
					
					//changing activation status to activated
					Editor editor = appinfo.edit();
					editor.putBoolean("activated", true);
					
					//saving the pid and did to app settings for future use
					editor.putString("pid", pid);
					editor.putString("did", did);
					
					editor.commit();
					
					//retrieving URL of agree to consent form
					String url_consent = getApplicationContext().getResources().getString(R.string.agree_systemconsent_url);
					
					//setting the parameters to upload
					ArrayList<NameValuePair> params = new ArrayList<NameValuePair>();
					
					Log.v("pid","pid="+pid);
					Log.v("did","did="+did);
					
					// Using a list of nameValuePairs to store POST data
					params.add(new BasicNameValuePair("participant_id",pid));
					params.add(new BasicNameValuePair("device_id",did));
					params.add(new BasicNameValuePair("AgreeToSystemConsentForm","Y"));
					
					UploadDataTask sendConsent = new UploadDataTask(url_consent, params, getApplicationContext());
					sendConsent.execute();
					
					Intent intent = new Intent();
					intent.setClass(getApplicationContext(),MainActivity.class); 
					startActivity(intent);
					
					ActivationActivity.this.finish(); 
					
				} 
				else{
					
					AlertDialog alertDialog;
		            alertDialog = new AlertDialog.Builder(ActivationActivity.this).create();
		            alertDialog.setTitle("Sorry,verification Failed");
		            alertDialog.setMessage("Invalid PID, please check again.");
		            alertDialog.setButton("Ok", new DialogInterface.OnClickListener() {

		                  public void onClick(DialogInterface dialog, int id) {
		                	  
		                	 input1.setText("");
		                	 input2.setText("");
		                	 
		                } }); 
		            
		            alertDialog.show();

					//move cursor to the first input box
					input1.requestFocus();
				}
				progressDialog.cancel(); 
			}
	    };
	    
	private void VerifyPID(){
		params = new ArrayList<NameValuePair>();
		
		// Using a list of nameValuePairs to store POST data
		params.add(new BasicNameValuePair("participant_id",pid));
		params.add(new BasicNameValuePair("device_id",did));
		
		Log.v("rqt", params.toString());

		//initiate the data upload task
		uploadtask = new UploadDataTask(url, params,this);
		
		//executing the AsyncTask of data uploading
		uploadtask.execute();
		
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
