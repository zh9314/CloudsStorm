package commonTool;

import java.io.IOException;

import org.apache.log4j.ConsoleAppender;
import org.apache.log4j.FileAppender;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.log4j.PatternLayout;

public class Log4JUtils {
	
	//This is must be invoked at the beginning of the program, if you want to change the output file for the error messages.
	//The path can be absolute or relative.
	public static boolean setErrorLogFile(String outputPath){
		Logger root = Logger.getRootLogger();
		try {
			//FileAppender appender = new FileAppender(new SimpleLayout(), outputPath, false);
			PatternLayout patternLayout = new PatternLayout();
			patternLayout.setConversionPattern("%d{yyyy-MM-dd HH:mm:ss,SSS} [%-5p] [%c.%M]	%m%n");
			FileAppender appender = new FileAppender(patternLayout, outputPath, true);
			
			appender.setThreshold(Level.ERROR);
			
			root.addAppender(appender);

		} catch (IOException e) {
			e.printStackTrace();
			return false;
		}    
		return true;
	}
	
	public static boolean setWarnLogFile(String outputPath){
		Logger root = Logger.getRootLogger();
		try {
			//FileAppender appender = new FileAppender(new SimpleLayout(), outputPath, false);
			PatternLayout patternLayout = new PatternLayout();
			patternLayout.setConversionPattern("%d{yyyy-MM-dd HH:mm:ss,SSS} [%-5p] [%c.%M]	%m%n");
			FileAppender appender = new FileAppender(patternLayout, outputPath, true);
			
			appender.setThreshold(Level.WARN);
			
			root.addAppender(appender);

		} catch (IOException e) {
			e.printStackTrace();
			return false;
		}    
		return true;
	}
	
	//This is set the information log files to some outputPath
	public static boolean setInfoLogFile(String outputPath){
		Logger root = Logger.getRootLogger();
		try {
			PatternLayout patternLayout = new PatternLayout();
			patternLayout.setConversionPattern("%d{yyyy-MM-dd HH:mm:ss,SSS} [%-5p] [%c.%M]	%m%n");
			FileAppender appender = new FileAppender(patternLayout, outputPath, true);
			
			appender.setThreshold(Level.INFO);
			
			root.addAppender(appender);

		} catch (IOException e) {
			e.printStackTrace();
			return false;
		}    
		return true;
	}
	
	public static boolean setSystemOutputLogFile(Level logLevel){
		Logger root = Logger.getRootLogger();
		
		PatternLayout patternLayout = new PatternLayout();
		patternLayout.setConversionPattern("%d{yyyy-MM-dd HH:mm:ss,SSS} [%-5p] [%c.%M]	%m%n");
		ConsoleAppender appender = new ConsoleAppender(patternLayout);
		
		appender.setThreshold(logLevel);
		
		root.addAppender(appender);
		
		return true;
	}

}
