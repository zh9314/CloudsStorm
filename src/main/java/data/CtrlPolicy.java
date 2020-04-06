package data;

import java.util.ArrayList;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonIgnore;

import lambdaExprs.infrasCode.main.Operation;

public class CtrlPolicy {
	
	public String ObjectType;
	
	public String Objects;
	///transfered from 'Objects' to get the real node names
	@JsonIgnore
	public ArrayList<String> nodes = new ArrayList<String>();
	
	public Map<String, Metric> Metrics;
	
	public ArrayList<Operation> OpCodes;
	
	

}
