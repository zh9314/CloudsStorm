package infrastructureCode.log;

import infrastructureCode.main.Operation;

public class Log {
	/**
	 * The time of the event happens which is a Long value.
	 */
	public String Time;
	
	/**
	 * This is the operation overhead, which record the time that this operation takes.
	 * The unit is in second. 
	 */
	public String Overhead;
	
	public Operation Event;
	
	/**
	 * The detailed content of the log.
	 */
	public String LOG;
	
}
