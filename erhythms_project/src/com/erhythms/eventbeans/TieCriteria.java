package com.erhythms.eventbeans;

import java.io.Serializable;

/*
 * This is a bean used to deal with anything associated with a tie criteria, includes every details about a criteria
 * @author E-Rhythms Project
 * 
 */

public class TieCriteria implements Serializable{
	   
	   
	   private static final long serialVersionUID = 1L;

		//this is id for the criteria
	   private int id; 
	   
	   // to be used if this is a dynamic text tie criteria
	   private int textPosition;
	   
	   // to be used to specify the X most/least name 
	   private int place = 1;
	   
	   //Frequency: the most, the least, mean, mode, median, or absolute value [num,num]
	   private String frequency;
	   
	   //Duration: in Days
	   private int duration_from;
	   
	   private int duration_to;
	   
	   //Action: TEXT or CALL;
	   private String action;
	   
	   // Selection method: random, walk-through or QID
	   private String method;
	   
	//This constructor is used by TIE DISPLAY
	public TieCriteria(int cID, String frequency,int duration_from, int duration_to, String action) {
			super();
			
			this.id = cID;
			this.frequency = frequency;
			this.duration_from = duration_from;
			this.duration_to = duration_to;
			this.action = action;
		}
	
	   
	//This constructor is used by SURVEY QUESTIONS
	public TieCriteria(int cID, int position, String frequency,
			int duration_from, int duration_to, String action, String method) {
		super();
		
		this.id = cID;
		this.textPosition = position;
		this.frequency = frequency;
		this.duration_from = duration_from;
		this.duration_to = duration_to;
		this.action = action;
		this.method = method;
	}

	public int getId() {
		return id;
	}

	public void setId(int id) {
		this.id = id;
	}

	public String getFrequency() {
		return frequency;
	}

	public void setFrequency(String frequency) {
		this.frequency = frequency;
	}

	public int getDuration_to() {
		return duration_to;
	}

	public void setDuration_to(int duration_to) {
		this.duration_to = duration_to;
	}

	public int getDuration_from() {
		return duration_from;
	}

	public void setDuration_from(int duration_from) {
		this.duration_from = duration_from;
	}

	public String getAction() {
		return action;
	}

	public void setAction(String type) {
		this.action = type;
	}

	public int getTextPosition() {
		return textPosition;
	}

	public void setTextPosition(int position) {
		this.textPosition = position;
	}

	public String getMethod() {
		return method;
	}

	public void setMethod(String method) {
		this.method = method;
	}

	public int getPlace() {
		return place;
	}

	public void setPlace(int place) {
		this.place = place;
	}


	@Override
	public String toString() {
		
		//method position != null means it's used in a Survey Question type event
		if(method!=null){
			
			//return conforms to the API
			return textPosition+","+id+","+frequency.replace(",","to")+","+duration_from+","+duration_to+","+action+","+method;
			
			
		// if method is not set, it's used in TIE DISPLAY
		}else{
			
			return id+","+frequency+","+duration_from+","+duration_to+","+action;
			
		}
	}

	   
}
