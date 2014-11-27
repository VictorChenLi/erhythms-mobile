package com.erhythms.eventbeans;

import java.io.Serializable;
import java.util.ArrayList;

/*
 * This event bean is used to initiate Text Display, Tie Display and Survey Question Events
 * @author E-Rhythms Project
 * 
 * 
 */

public class EventBean implements Serializable{
	   
	   /**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	//this is the event index
	   private int index; 
	   
	   //this is used to indicate event type:1.TEXT 2.TIE 3.SURVEY QUESTION
	   private String eventType;
	   
	   //this is the question id
	   private int qid;
	   
	   //this is the text body
	   private String textbody;
	   
	   /* 
	    * The following are used for Survey Questions
	    */
	   
	   // setting the question as single or multiple choices
	   private String questionType= "";
	   
	   //this is the question choices string
	   private String choicestring;
	   
	   // count the number of question choices given
	   private int choicecount;
	   
	   //used for setting participant response choices
	   private String choiceResponse = "";
	   
	   //used for accepting text response
	   private String textResponse = "";
	   
	   //used for accepting tie number responsed
	   private String tieResponse = "";
	   
	   //this boolean indicates FREE TEXTBOX RESPONSE
	   private boolean enterText;

	   //Booleans specifying: 1.choose from contacts,2.choose from calllog 3.enter manually
	   private boolean selectContacts;
	   private boolean selectCallLog;
	   private boolean enterManually;
	   
	   //this ArrayList is used to store the TieCriteria instances
	   private ArrayList<TieCriteria> tieCriteria;
	   
	   private String dynamicText = "";
	   
	   //initiating Text Display
	   public EventBean(int index, String textbody)
	   {
		    this.index = index;
		    this.textbody = textbody;
		    this.eventType = "TEXT_DISPLAY"; 
		}
	   
	   
	   //Initiating Tie Display
	   public EventBean(int index)
	   {	
		   this.tieCriteria = new ArrayList<TieCriteria>();
		   this.index = index;
		   this.eventType = "TIE_DISPLAY";
		}
	 
	   
	   // initiating Survey Questions
	   public EventBean(int index, int qid, String textbody,String questionType, String choices, int numOfAnswers, boolean contacts, boolean calllog, boolean enterM, boolean enterText)
	   {
		    this.tieCriteria = new ArrayList<TieCriteria>();
		    this.index = index;
		    this.qid = qid;
		    this.textbody = textbody;
		    this.questionType = questionType;
		    this.choicestring = choices;
		    this.choicecount = numOfAnswers;
		    this.eventType = "SURVEY_QUESTION"; 
		    this.selectContacts = contacts;
		    this.selectCallLog = calllog;
		    this.enterManually = enterM;
		    this.enterText = enterText;
		}
	   
	   
	   
	    //this is only valid if the event is a Survey Question
		public String getChoice(int num){
			
			String choice = "";
			
			if (num > choicecount-1||num < 0){
				
				// if entering a choice number out of range
				// returns "NONE"
				return "NONE";
			}
			
			else{
				
				//parsing the choice string to get the choice
				choice = choicestring.split("_")[num];
				
				return choice;
			}
		}
		
	public int getQid() {
		return qid;
	}

	public String getChoices() {
		return choicestring;
	}

	public String getTextbody() {
		return textbody;
	}

	public int getChoicecount() {
		return choicecount;
	}

	public String getChoiceResponse() {
		return choiceResponse;
	}

	// this is used to set the answers
	public void setChoiceResponse(String response) {
		this.choiceResponse = response;
	}

	public int getIndex() {
		return index;
	}

	public String getEventType() {
		return eventType;
	}


	@Override
	public String toString() {
		
		//based on the event type, return different strings
		
		if(eventType.equals("TEXT_DISPLAY")){

			return index+","+textbody;
			
		}
		
		else if(eventType.equals("TIE_DISPLAY")){
			

			return index+","+tieCriteria.toString();
			
		}
		
		else if(eventType.equals("SURVEY_QUESTION")){
			
			
			return index+","+qid+","+questionType+","+textbody+","+selectContacts+","+
			selectCallLog+","+enterManually+","+enterText+","+choicestring+","+tieCriteria.size()+","+tieCriteria;
			
		}else{
		
		return "invalid event";
		
		}
	}


	public boolean isSelectContacts() {
		return selectContacts;
	}


	public void setSelectContacts(boolean selectContacts) {
		this.selectContacts = selectContacts;
	}


	public boolean isSelectCallLog() {
		return selectCallLog;
	}


	public void setSelectCallLog(boolean selectCallLog) {
		this.selectCallLog = selectCallLog;
	}


	public boolean isEnterManually() {
		return enterManually;
	}


	public void setEnterManually(boolean enterManually) {
		this.enterManually = enterManually;
	}


	public ArrayList<TieCriteria> getTieCriteria() {
		return tieCriteria;
	}

	//method used to add new tie criteria
	public void addTieCriteria(TieCriteria newtc) {
		this.tieCriteria.add(newtc);
	}


	public boolean isEnterText() {
		return enterText;
	}


	public void setEnterText(boolean enterText) {
		this.enterText = enterText;
	}


	public String getQuestionType() {
		return questionType;
	}


	public void setQuestionType(String questionType) {
		this.questionType = questionType;
	}


	public String getDynamicText() {
		return dynamicText;
	}


	public void setDynamicText(String dynamicText) {
		this.dynamicText = dynamicText;
	}


	public String getTextResponse() {
		return textResponse;
	}


	public void setTextResponse(String response_text) {
		this.textResponse = response_text;
	}


	public String getTieResponse() {
		return tieResponse;
	}


	public void setTieResponse(String tieResponse) {
		this.tieResponse = tieResponse;
	}
}
