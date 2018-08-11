/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * Copyright © Huan Zhou (SNE, University of Amsterdam) and contributors
 */
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
	
	public static void removeAllLogAppender(){
		Logger.getRootLogger().removeAllAppenders();
		
	}

}
