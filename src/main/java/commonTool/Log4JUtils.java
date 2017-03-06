package commonTool;

import java.io.IOException;

import org.apache.log4j.FileAppender;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.log4j.PatternLayout;
import org.apache.log4j.SimpleLayout;

public class Log4JUtils {
	
	//This is must be invoked at the beginning of the program, if you want to change the output file for the error messages.
	//The path can be absolute or relative.
	public static boolean setErrorLogFile(String outputPath){
		Logger root = Logger.getRootLogger();
		try {
			FileAppender appender = new FileAppender(new SimpleLayout(), outputPath, false);
			
			appender.setThreshold(Level.ERROR);
			
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

}
