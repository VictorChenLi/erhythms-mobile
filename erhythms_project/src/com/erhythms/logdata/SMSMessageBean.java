package com.erhythms.logdata;

import java.io.Serializable;

/**
 * This class implements a JavaBean for storing Call Logs
 * 
 * @author E-Rhythm Project
 */
public class SMSMessageBean implements Serializable {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	/**
	 * Message body
	 */
	private String smsbody;
	/**
	 * Sender's phone number
	 */
	private String phoneNumber;
	/**
	 * Date and time of message
	 */
	private String date;
	/**
	 * Sender's name
	 */
	private String name;
	/**
	 * Message type 1 = received, 2 = sent
	 */
	private String type;
	
	//number of previous days when this message is received
	private int days;

	public String getSmsbody() {
		return smsbody;
	}

	public void setSmsbody(String smsbody) {
		this.smsbody = smsbody;
	}

	public String getPhoneNumber() {
		return phoneNumber;
	}

	public void setPhoneNumber(String phoneNumber) {
		this.phoneNumber = phoneNumber;
	}

	public String getDate() {
		return date;
	}

	public void setDate(String date) {
		this.date = date;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
	}

	@Override
	public String toString() {
		// TODO Auto-generated method stub
		return "number="+phoneNumber+"name="+name+"date="+date;
	}

	public int getDays() {
		return days;
	}

	public void setDays(int days) {
		this.days = days;
	}
	
}
