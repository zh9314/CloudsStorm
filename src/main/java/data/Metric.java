package data;

import com.fasterxml.jackson.annotation.JsonIgnore;

public class Metric {
	
	
	public String AboveThreshold;
	
	public String BelowThreshold;
	
	public String Field;
	/**
	 * The basic time unit for measurements.
	 */
	public String TimeUnit;
	
	/**
	 * A sequential number of times that exceeds the 'Threshold' will trigger the operation. 
	 */
	public String SeqTimes;
	@JsonIgnore
	public int seqTimesCount = 0;
	
	/**
	 * A total number of times that exceeds the 'Threshold' will trigger the operation. 
	 */
	public String TotalTimes;
	@JsonIgnore
	public int totalTimesCount = 0;
	
	/**
	 * To indicate whether this metric has been triggered.
	 */
	@JsonIgnore
	public boolean triggered = false;
}
