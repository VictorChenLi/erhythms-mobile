package com.erhythms.network;

import java.security.MessageDigest;

import com.google.i18n.phonenumbers.NumberParseException;
import com.google.i18n.phonenumbers.PhoneNumberUtil;
import com.google.i18n.phonenumbers.Phonenumber.PhoneNumber;

public class Encoder {

     private final static String[] hexDigits = { "0", "1", "2", "3", "4", "5",
                        "6", "7", "8", "9", "a", "b", "c", "d", "e", "f" };
     
        public static String byteArrayToHexString(byte[] b) {
                StringBuffer resultSb = new StringBuffer();
                for (int i = 0; i < b.length; i++) {
                        resultSb.append(byteToHexString(b[i]));
                }
                return resultSb.toString();
        }
 

        private static String byteToHexString(byte b) {
                int n = b;
                if (n < 0)
                        n = 256 + n;
                int d1 = n / 16;
                int d2 = n % 16;
                return hexDigits[d1] + hexDigits[d2];
        }
 

        public static String MD5Encode(String origin) {
                String resultString = null;
                try {
                        resultString = new String(origin);
                        MessageDigest md = MessageDigest.getInstance("MD5");
                        resultString = byteArrayToHexString(md.digest(resultString.getBytes()));
 
                } catch (Exception ex) {
                }
                return resultString;
        }
        
        public static String hashPhoneNumber(String input_number){
        	
			String contact_hash = null;
			
			//check first by removing all the "-", "(", ")" and spaces"
			String contact_number = input_number.replace(" ", "").replace("-", "").replace("(", "").replace(")", "");
			
			if (contact_number.startsWith("+")){
				
				//check the country code first
				String countryCode = null;
				PhoneNumberUtil phoneUtil = PhoneNumberUtil.getInstance();
				try {
				    // phone must begin with '+'
				    PhoneNumber numberProto = phoneUtil.parse(contact_number, "");
				    countryCode = numberProto.getCountryCode()+"";
				} catch (NumberParseException e) {
				    System.err.println("NumberParseException was thrown: " + e.toString());
				}
				
				//if country code equals 1, meaning if it is a North America number
				if (countryCode.equals("1")){
					
					// if the number starts with +1 (in most cases) it is a North America
					// number, so the country code and area code are both kept.
					// BUT check first if the digits is less than 10 (if less than 10, probably a service number)
					if(contact_number.length()<10){contact_hash = "+1-"+Encoder.MD5Encode(contact_number);}
						else{
							contact_hash = "+1-("+contact_number.substring(2, 5)+
									")-"+Encoder.MD5Encode(contact_number.substring(5));
							}
					
				} else {
					
					// if the country code is not 1, it's most likely an international (at least not a North America number)
					// So only the country code is kept.
					// BUT check first if the digits is less than 10 (if less than 10, probably a service number)
					if(contact_number.length()<10){contact_hash = "+"+countryCode+"-"+Encoder.MD5Encode(contact_number);}
						else {
							contact_hash = "+"+countryCode+"-"+Encoder.MD5Encode(contact_number.substring(countryCode.length()+1));
							}
				}
			} 
			else {
				// if the number does not start with "+", it's most likely a local number
				// so add +1 and keep area code and hash the rest of contact_number
				// BUT check first if the digits is less than 10 (if less than 10, probably a service number)
				if(contact_number.length() < 10 || contact_number.length() > 10 ){contact_hash = "+1-"+Encoder.MD5Encode(contact_number);}
				else if(contact_number.length()==10){contact_hash = "+1-("+contact_number.substring(0, 3)+")-"+Encoder.MD5Encode(contact_number.substring(3));}
			}
		
        	
        	return contact_hash;
        	
        }
}
