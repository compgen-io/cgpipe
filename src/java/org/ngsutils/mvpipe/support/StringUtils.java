package org.ngsutils.mvpipe.support;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class StringUtils {
	public static String strip(String str) {
		return lstrip(rstrip(str));
	}
	
    public static String rstrip(String str) {
        Pattern pattern = Pattern.compile("^(.*?)\\s*$");        
        Matcher m = pattern.matcher(str);
        if (m.find()) {        
            return m.group(1);
        }
        return str;
    }
    
    public static String lstrip(String str) {
        Pattern pattern = Pattern.compile("^\\s*(.*?)$");
        Matcher m = pattern.matcher(str);
        if (m.find()) {        
            return m.group(1);
        }
        return str;
    }
    
    public static String join(String delim, String[] args) {
        String out = "";
        
        for (String arg: args) {
            if (out.equals("")) {
                out = arg;
            } else {
                out = out + delim + arg;
            }
        }
        
        return out;
    }

    public static String join(String delim, double[] args) {
        String out = "";
        
        for (Number arg: args) {
            if (out.equals("")) {
                out = ""+arg;
            } else {
                out = out + delim + arg;
            }
        }
        
        return out;
    }

    public static String join(String delim, int[] args) {
        String out = "";
        
        for (Number arg: args) {
            if (out.equals("")) {
                out = ""+arg;
            } else {
                out = out + delim + arg;
            }
        }
        
        return out;
    }
    
    public static String join(String delim, Iterable<? extends Object> args) {
        String out = "";
        if (args != null) {
	        for (Object arg: args) {
	            if (out.equals("")) {
	                out = arg.toString();
	            } else {
	                out = out + delim + arg.toString();
	            }
	        }
        }
        
        return out;
    }

    public static List<String> quotedSplit(String str, String delim) {
    	return quotedSplit(str, delim, false);
    }
    public static List<String> quotedSplit(String str, String delim, boolean includeDelim) {
		List<String> tokens = new ArrayList<String>();

		String buf="";
		boolean inquote = false;
		int i=0;
		
		while (i < str.length()) {			
			if (inquote) {
				if (str.charAt(i) == '"') {
					if (buf.endsWith("\\")) {
						buf = buf.substring(0, buf.length()-1) + "\"";
					} else {
						buf += "\"";
						inquote = false;
					}
				} else {
					buf += str.charAt(i);
				}
				i++;
				continue;
			}

			if (str.charAt(i) == '"') {
				buf += "\"";
				inquote = true;
				i++;
				continue;
			}

			if (str.substring(i, i+delim.length()).equals(delim)) {
				if (buf.length()>0) {
					tokens.add(buf);
					if (includeDelim) {
						tokens.add(delim);
					}
					buf = "";
				}
				i += delim.length();
				continue;
			}
			
			buf += str.charAt(i);
			i++;
		}
		
		if (!buf.equals("")) {
			tokens.add(buf);
		}
		
		return tokens;
    }

	public static String removeIndent(String line, int indentLevel) {
		int acc = 0;
		int i;
		for (i=0; i<line.length() && acc < indentLevel; i++) {
			if (line.charAt(i) == ' ') { 
				acc +=1;
			} else if (line.charAt(i) == '\t') { 
				acc += 4; 
			}
		}
		return line.substring(i);
	}

	public static int calcIndentLevel(String line) {
		int acc = 0;
		for (int i=0; i<line.length(); i++) {
			if (line.charAt(i) == ' ') { 
				acc +=1;
			} else if (line.charAt(i) == '\t') { 
				acc += 4; 
			} else {
				return acc;
			}
		}
		return acc;
	}
}
