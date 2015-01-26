package org.ngsutils.mvpipe.support;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.io.Serializable;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

import org.apache.commons.logging.Log;

public class SimpleFileLoggerImpl implements Log, Serializable {
	/**
	 * 
	 */
	private static final long serialVersionUID = -5168800526017037191L;

	public enum Level {
		ALL,
		TRACE,
		DEBUG,
		INFO,
		WARN,
		ERROR,
		FATAL
	}
	
	private static Level level = Level.INFO;
	private static PrintStream out = null;
	public static void setLevel(Level level) {
		SimpleFileLoggerImpl.level = level;
	}

	public static void setFilename(String logFilename) throws FileNotFoundException {
		if (SimpleFileLoggerImpl.out != null) {
			SimpleFileLoggerImpl.out.close();
		}
		SimpleFileLoggerImpl.out  = new PrintStream(new FileOutputStream(logFilename, true));
	}

	public static void close() {
		if (SimpleFileLoggerImpl.out != null) {
			SimpleFileLoggerImpl.out.close();
		}
	}
	
	static {
		Runtime.getRuntime().addShutdownHook(new Thread() {
			@Override
			public void run() {
				SimpleFileLoggerImpl.close();
			}
		});
	}
	
	private String name;
	private DateFormat dateFormater=new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
	
	public SimpleFileLoggerImpl(String name) {
		this.name = name;
	}
	
	private void log(Level level, Object arg0, Throwable arg1) {
		if (SimpleFileLoggerImpl.level.compareTo(level) <= 0) {
			if (out != null) {
				String date = dateFormater.format(new Date());
				out.println(date+" "+level.name()+" "+name+" "+arg0);
				if (arg1 != null) {
					arg1.printStackTrace(out);
				}
			}
		}
		if (level == Level.FATAL) {
			System.err.println(arg0+": " + (arg1 == null ? "Unknown error" : arg1.getMessage()));
		}
	}

	@Override
	public void debug(Object arg0) {
		log(Level.DEBUG, arg0, null);
	}

	@Override
	public void debug(Object arg0, Throwable arg1) {
		log(Level.DEBUG, arg0, arg1);
	}

	@Override
	public void error(Object arg0) {
		log(Level.ERROR, arg0, null);
	}

	@Override
	public void error(Object arg0, Throwable arg1) {
		log(Level.ERROR, arg0, arg1);
	}

	@Override
	public void fatal(Object arg0) {
		log(Level.FATAL, arg0, null);
	}

	@Override
	public void fatal(Object arg0, Throwable arg1) {
		log(Level.FATAL, arg0, arg1);
	}

	@Override
	public void info(Object arg0) {
		log(Level.INFO, arg0, null);
	}

	@Override
	public void info(Object arg0, Throwable arg1) {
		log(Level.INFO, arg0, arg1);
	}

	@Override
	public boolean isDebugEnabled() {
		return SimpleFileLoggerImpl.level.compareTo(Level.DEBUG) <= 0;
	}

	@Override
	public boolean isErrorEnabled() {
		return SimpleFileLoggerImpl.level.compareTo(Level.ERROR) <= 0;
	}

	@Override
	public boolean isFatalEnabled() {
		return SimpleFileLoggerImpl.level.compareTo(Level.FATAL) <= 0;
	}

	@Override
	public boolean isInfoEnabled() {
		return SimpleFileLoggerImpl.level.compareTo(Level.INFO) <= 0;
	}

	@Override
	public boolean isTraceEnabled() {
		return SimpleFileLoggerImpl.level.compareTo(Level.TRACE) <= 0;
	}

	@Override
	public boolean isWarnEnabled() {
		return SimpleFileLoggerImpl.level.compareTo(Level.WARN) <= 0;
	}

	@Override
	public void trace(Object arg0) {
		log(Level.TRACE, arg0, null);
	}

	@Override
	public void trace(Object arg0, Throwable arg1) {
		log(Level.TRACE, arg0, arg1);
	}

	@Override
	public void warn(Object arg0) {
		log(Level.WARN, arg0, null);
	}

	@Override
	public void warn(Object arg0, Throwable arg1) {
		log(Level.WARN, arg0, arg1);
	}

}
