package com.erhythms.widget;

import com.erhythmsproject.erhythmsapp.R;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.view.WindowManager;
import android.widget.RelativeLayout;

@SuppressLint("NewApi") 
public class TutorialScreen extends Activity{

		private RelativeLayout rootView;
	
	
		@Override
		protected void onCreate(Bundle savedInstanceState) {
			// TODO Auto-generated method stub
			super.onCreate(savedInstanceState);
			setContentView(R.layout.tutorial_screen);
			
			//find all view elements by id
			rootView = (RelativeLayout)findViewById(R.id.tutorial_screen_root);
			
			// add onClickListener to the layout, which is the entire screen
			rootView.setOnClickListener(new View.OnClickListener() {
				
				@Override
				public void onClick(View v) {
					// TODO Auto-generated method stub
					
					TutorialScreen.this.finish();
				}
			});
			
			/* This code changes the status bar color
			 */
			
		    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
			        getWindow().addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
				    getWindow().setStatusBarColor(Color.parseColor("#3B616B"));
			}
			
		}
	
		@Override
		public void onBackPressed() {
			// TODO Auto-generated method stub
			super.onBackPressed();
			
			
		}

	
	}
