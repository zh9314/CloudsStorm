package infrastructureCode.main;

import java.util.Map;

import com.fasterxml.jackson.annotation.JsonIgnore;

public class Operation {
	public String Operation;
	
	/**
	 * When the Operation is 'execute', this tells which command wants to execute on 
	 * the remote node.
	 */
	public String Command;
	
	/**
	 * This contains all the options that needed by this operation.
	 * It stores the key value pairs for these options.
	 */
	public Map<String, String> Options;
	
	/**
	 * It means whether to log the output of this command. Only valid 
	 * when the operation is 'execute'.
	 */
	public String Log;
	
	
	/**
	 * The type of the object of this operation.
	 * Can be: "VM", "SubTopology"
	 */
	public String ObjectType;
	
	/**
	 * To define the set of objects of this operation.
	 * The names are split by "||" as parallel lambda calculus.
	 */
	public String Objects;
	
	
	/**
	 * This is a counter to tell the position in the code type of 'LOOP'.
	 * In the infrastructure code, it is identified as '$counter'
	 */
	@JsonIgnore
	public int loopCounter;
}
