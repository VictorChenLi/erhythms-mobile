package com.erhythms.network;

import java.util.List;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;
import org.apache.http.util.EntityUtils;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.os.AsyncTask;
import android.util.Log;

public class UploadDataTask extends AsyncTask<Void, Integer, String>{
	
	//url is the url of the web service
	private String url = null;
	
	//list is a list of parameters to upload
	private List<NameValuePair> params = null;
	
	//context is used to update the status code
	private Context appcontext;

	public UploadDataTask(String url, List<NameValuePair> params, Context appcontext){
		this.url = url;
		this.params = params;
		this.appcontext = appcontext;
	}
		
	@Override
	protected void onPreExecute() {
		super.onPreExecute();
		}
	    	
	@Override
	protected String doInBackground(Void... para) {
			String retCode = null;
		
			try {
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
				
				retCode = retSrc.substring(retSrc.indexOf("{"),retSrc.indexOf("}")).split(":")[1]; 
				
				Log.v("result",retSrc);
						
				} catch (Exception e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
					Log.v("exception",e.toString());
				}
			
				return retCode;
			}

			protected void onPostExecute(String result) {
				SharedPreferences pref = appcontext.getSharedPreferences("appinfo", Context.MODE_PRIVATE);
				
				//update the status code shared in the appinfo preference
				Editor editor = pref.edit();
				editor.putString("returncode",result);
				editor.commit();
			}
}