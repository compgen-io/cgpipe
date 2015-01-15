package org.ngsutils.mvpipe.parser;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class Tokenizer {
	public static List<String> tokenize(String str) {
		List<String> tokens = extractQuotedStrings(str);
		
		tokens = correctNegNumbers(tokens);
		tokens = mergeOps(tokens);
		tokens = correctDollarExp(tokens);
		tokens = removeComments(tokens);
		
		return tokens;
	}
	private static List<String> removeComments(List<String> in) {
		List<String> out = new ArrayList<String>();
		
		for (String s: in) {
			if (s.startsWith("#")) {
				return out;
			} else {
				out.add(s);
			}
		}

		return out;
	}

	private static List<String> correctNegNumbers(List<String> in) {
		if (in.size() == 0) {
			return in;
		}
		List<String> out = new ArrayList<String>();
		
		// starts with a neg, so this must be a number...
		if (in.get(0).equals("-")) {
			in.remove(0);
			in.set(0,  "-" + in.get(0));
		}
		
		Set<String> valid = new HashSet<String>();
		valid.add("+");
		valid.add("-");
		valid.add("*");
		valid.add("/");
		valid.add("%");
		valid.add("**");
		valid.add("(");
		
		String last = null;
		boolean appendNeg = false;
		for (String s: in) {
			if (last == null) {
				out.add(s);
				last = s;
				continue;
			}
		
			if (appendNeg) {
				out.add("-" + s);
				appendNeg = false;
				continue;
			}
			
			if (s.equals("-")) {
				if (valid.contains(last)) {
					appendNeg = true;
					continue;
				}
			}
			
			out.add(s);
			last = s;			
		}

		return out;
	}

	
	private static List<String> mergeOps(List<String> in) {
		List<String> out = new ArrayList<String>();
		String last = null;
		
		String[] validOps = new String[] {
				"**", "==", "?=", "!=", ">=", "<="
		};
		
		for (String s: in) {
			if (last == null) {
				last = s;
				continue;
			}
			
			String pair = last + s;
			for (String op: validOps) {
				if (op.equals(pair)) {
					out.add(op);
					last = null;
					break;
				}
			}
			
			if (last != null) {
				out.add(last);
				last = s;
			}
		}
		
		if (last != null) {
			out.add(last);
		}
		return out;
	}

	private static List<String> correctDollarExp(List<String> in) {
		List<String> out = new ArrayList<String>();
		
		String buf = null;
		
		for (String s: in) {
			if (buf != null) {
				buf += s;
				if (s.endsWith("}")) {
					out.add(buf);
					buf = null;
				}
			} else if (s.startsWith("${") && !s.endsWith("}")) {
				buf = s;
			} else {
				out.add(s);
			}
		}

		return out;
	}

//	private static List<String> correctOps(List<String> in) {
//		List<String> out = new ArrayList<String>();
//		
//		String buf = null;
//		
//		for (String s: in) {
//			if (buf != null) {
//				buf += s;
//				if (s.endsWith("}")) {
//					out.add(buf);
//					buf = null;
//				}
//			} else if (s.startsWith("${") && !s.endsWith("}")) {
//				buf = s;
//			} else {
//				out.add(s);
//			}
//		}
//
//		return out;
//	}
private static List<String> extractQuotedStrings(String str) {
		List<String> tokens = new ArrayList<String>();

		String buf="";
		boolean inquote = false;
		
		for (int i=0; i<str.length(); i++) {
			String delim = "" + str.charAt(i);
			
			switch(str.charAt(i)) {
			case '"':

				if (!inquote) {
					// we aren't in a quoted block, so start one (naked \" isn't allowed)					
					buf += "\"";
					inquote = true;
				} else {
					if (buf.endsWith("\\")){
						// we are in a quoted block, but the preceding char was '\', so just ignore the quote
						buf += "\"";
					} else {
						// we are in a quoted block, so close it 
						buf += "\"";
						tokens.add(buf);
						buf = "";
						inquote = false;
					}
				}
				break;

			// operators
			case '+':
			case '-':
			case '*':
			case '/':
			case '%':
			case '?':
			case '!':
			case '=':
			case '|':
			case ':':
			case '&':
			case '(':
			case ')':
			case ' ':
			case '#':
				if (!inquote) {
					if (buf.length()>0) {
						tokens.add(buf);
						buf = "";
					}
					if (str.charAt(i) != ' ') {
						tokens.add(delim);
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
