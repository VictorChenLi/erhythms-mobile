package com.erhythms.eventbeans;

import java.io.Serializable;

/*
 * This is the bean for storing anything associated with a tie, e.g. name phone number
 * @author E-Rhythms Project
 * 
 */

public class Tie implements Serializable {
	   
	   /**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	//this is the question id where this tie appears
	   private int qid; 

	   // this is the name of this tie
	   private String name;
	   
	   // this is the phone number of this tie
	   private String phone_number;
	   
	   // this is the criteria that is used to generate this tie
	   private TieCriteria criteria;
	   
	   public Tie(int qid, String name, String phone_number,
			TieCriteria criteria) {
		super();
		this.qid = qid;
		this.name = name;
		this.phone_number = phone_number;
		this.criteria = criteria;
	}
	   
	   
	 // this is used only by the TieGenerator, do remember to set the QID
	 // if you use the Tie Generator
	   public Tie(String name, String phone_number, TieCriteria criteria) {
		super();
		this.name = name;
		this.phone_number = phone_number;
		this.criteria = criteria;
	}



	public int getQid() {
		return qid;
	}

	public void setQid(int qid) {
		this.qid = qid;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getPhone_number() {
		return phone_number;
	}

	public void setPhone_number(String phone_number) {
		this.phone_number = phone_number;
	}

	public TieCriteria getCriteria() {
		return criteria;
	}

	public void setCriteria(TieCriteria criteria) {
		this.criteria = criteria;
	}

	@Override
	public String toString() {
		return "Tie [qid=" + qid + ", criteria_id=" + criteria.getId() +", name=" + name
				+ ", phone_number=" + phone_number + "]";
	}
	
}
