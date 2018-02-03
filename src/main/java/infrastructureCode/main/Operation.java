package infrastructureCode.main;

public class Operation {
	public String Operation;
	
	/**
	 * When the Operation is 'execute', this tells which command wants to execute on 
	 * the remote node.
	 */
	public String Command;
	
	/**
	 * It means whether to log the output of this command. Only valid 
	 * when the operation is 'execute'.
	 */
	public String Log;
	
	
	/**
	 * The type of the subject of this operation.
	 * Can be: "VM", "SubTopology"
	 */
	public String SubjectType;
	
	/**
	 * To define the set of subjects of this operation.
	 * The names are split by "||" as parallel lambda calculus.
	 */
	public String Subjects;
}
