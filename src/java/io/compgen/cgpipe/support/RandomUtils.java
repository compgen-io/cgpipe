package io.compgen.cgpipe.support;

import java.security.SecureRandom;

public class RandomUtils {

	public static final String UPPER="ABCDEFGHIJKLMNOPQRSTUVWXYZ";
	public static final String LOWER="abcdefghijklmnopqrstuvwxyz";
	public static final String NUM="0123456789";
	
	public static final String generateRandomString() {
		return generateRandomString(24, UPPER+LOWER+NUM);
	}
	
	public static final String generateRandomString(int length, String pool) {
		
	    SecureRandom rand = new SecureRandom();
	
	    String s = "";
	    
	    while (s.length() < length) {
	    	int next = rand.nextInt(pool.length());
	    	s += pool.charAt(next);
	    }
	    
		return s;
	}


}
