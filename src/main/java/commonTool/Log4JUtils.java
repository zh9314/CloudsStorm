package commonTool;

import java.io.IOException;

import org.apache.log4j.FileAppender;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
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

}
