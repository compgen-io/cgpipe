package org.ngsutils.mvpipe.parser;

import java.util.ArrayList;
import java.util.List;

public class Tokenizer {
	public static List<String> tokenize(String str) {
		List<String> tokens = new ArrayList<String>();
		
		String buf="";
		boolean inquote = false;
		
		for (int i=0; i<str.length(); i++) {
			switch(str.charAt(i)) {
			case '"':
				if (!inquote) {
					inquote = true;
				} else {
					tokens.add(buf);
					buf = "";
					inquote = false;
				}
				break;
			case '=':
			case '+':
			case '-':
			case '*':
			case '/':
			case '%':
			case '?':
			case '!':
			case ' ':
				if (!inquote) {
					if (buf.length()>0) {
						tokens.add(buf);
						buf = "";
					}
					if (str.charAt(i) != ' ') {
						tokens.add(""+str.charAt(i));
					}
					break;
				}
			default:
				buf += str.charAt(i);
				break;
			}
		}
		
		if (buf.length() > 0) {
			tokens.add(buf);
		}
		
		return tokens;
	}

}
