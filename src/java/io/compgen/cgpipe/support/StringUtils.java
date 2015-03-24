package io.compgen.cgpipe.support;

import io.compgen.cgpipe.support.IterUtils.MapFunc;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
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
    	return join(delim, args, null);
    }
    public static <T> String join(String delim, Iterable<T> args, MapFunc<T, String> mapper) {
        String out = "";
        if (args != null) {
	        for (T arg: args) {
	        	String val = null;
	        	if (mapper == null) {
	        		val = arg.toString();
	        	} else {
	        		val = mapper.map(arg);
	        	}
	        	
	        	if (val == null) {
	        		continue;
	        	}
	        	
	            if (out.equals("")) {
	                out = val;
	            } else {
	                out = out + delim + val;
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

	public static String stripIndent(String line, int indent) {
		int acc = 0;
		int i=0;
		for (i=0; i<line.length() && acc < indent; i++) {
			if (line.charAt(0) == ' ') { 
				acc +=1;
			} else if (line.charAt(0) == '\t') { 
				acc += 4;
			} else {
				break;
			}
		}
		return line.substring(i);
	}

	public static String slurp(InputStream is) throws IOException {
		return slurp(is, Charset.defaultCharset());
	}

	public static String slurp(InputStream is, Charset cs) throws IOException {
		String s="";

		int read;
		byte[] buf = new byte[16*1024];
		
		while ((read = is.read(buf)) > -1) {
			s += new String(buf, 0, read, cs);
		}
		
		return s;
	}

	public static String repeat(String str, int indent) {
		String s = "";
		for (int i=0; i< indent; i++) {
			s += str;
		}
		return s;
	}
	
	public static String readFile(String filename) throws IOException {
		InputStream is;
		if (filename.equals("-")) {
			is = System.in;
		} else {
			is = new FileInputStream(filename);
		}
		String s = readStream(is);
		if (is != System.in) {
			is.close();
		}
		
		return s;
	}

	public static String readFile(File file) throws IOException {
		InputStream is = new FileInputStream(file);
		String s = readStream(is);
		is.close();
		return s;
	}

	private static String readStream(InputStream is) throws IOException {
		String s = null;
		byte[] buf = new byte[4096];
		int read = 0;
		while ((read = is.read(buf, 0, buf.length)) > -1) {
			if (s == null) {
				s = "";
			}
			s += new String(buf,0,read);
		}
		return s;
		
	}
	
	public static void writeFile(String filename, String val) throws IOException {
		writeFile(new File(filename), val, false);
	}
	public static void writeFile(File file, String val) throws IOException {
		writeFile(file, val, false);
	}
	public static void writeFile(String filename, String val, boolean append) throws IOException {
		writeFile(new File(filename), val, append);
	}
	public static void writeFile(File filename, String val, boolean append) throws IOException {
		OutputStream os;
		if (filename.equals("-")) {
			os = System.out;
		} else {
			os = new BufferedOutputStream(new FileOutputStream(filename, append));
		}
		
		os.write(val.getBytes());
		os.flush();

		if (os != System.out) {
			os.close();
		}
		
	}

	final private static String sym = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
	final private static Random rand = new Random();
	
	public static String randomString(int len) {
		String s="";
		while (s.length()<len) {
			s += sym.charAt(rand.nextInt(sym.length()));
		}
		return s;
	}


}
