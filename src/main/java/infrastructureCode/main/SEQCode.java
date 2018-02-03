package infrastructureCode.main;

import com.fasterxml.jackson.annotation.JsonTypeName;

@JsonTypeName("SEQ")
public class SEQCode extends Code {
	
	public Operation OpCode;
	
	public SEQCode(){
		this.CodeType = "SEQ";
	}
}
