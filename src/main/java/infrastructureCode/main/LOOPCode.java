package infrastructureCode.main;

import java.util.ArrayList;

import com.fasterxml.jackson.annotation.JsonTypeName;

@JsonTypeName("LOOP")
public class LOOPCode extends Code {
	///These are the end conditions for the loop.
	///If it is not set, it should be null. 
	public String Duration;
	public String Count;
	public String Deadline;
	
	public ArrayList<Operation> OpCodes;
	

    public LOOPCode() {  
        this.CodeType = "LOOP";  
    }  
}
