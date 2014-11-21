package com.erhythms.logdata;

import java.io.Serializable;

/**
 * This class implements a JavaBean for storing Call Logs
 * 
 * @author E-Rhythm Project
 */
public class CallPhoneBean implements Serializable {
	private static final long serialVersionUID = 1L;
	private String calldate;
	private String calltype;
	private String phonename;
	private String phonenumber;
	private String calltimes;
	private long callsecondes;
	private int days;

	public String getCalldate() {
		return calldate;
	}

	public void setCalldate(String calldate) {
		this.calldate = calldate;
	}

	public String getCalltype() {
		return calltype;
	}

	public void setCalltype(String calltype) {
		this.calltype = calltype;
	}

	public String getPhonename() {
		return phonename;
	}

	public void setPhonename(String phonename) {
		this.phonename = phonename;
	}

	public String getPhonenumber() {
		return phonenumber;
	}

	public void setPhonenumber(String phonenumber) {
		this.phonenumber = phonenumber;
	}

	public String getCalltimes() {
		return calltimes;
	}

	public void setCalltimes(String calltimes) {
		this.calltimes = calltimes;
	}

	public long getCallsecondes() {
		return callsecondes;
	}

	public void setCallsecondes(long callsecondes) {
		this.callsecondes = callsecondes;
	}

	public int getDays() {
		return days;
	}

	public void setDays(int days) {
		this.days = days;
	}

}
