package infrastructureCode.main;

import com.fasterxml.jackson.annotation.JsonSubTypes;  
import com.fasterxml.jackson.annotation.JsonTypeInfo;  
import com.fasterxml.jackson.annotation.JsonTypeInfo.As;  

@JsonTypeInfo(use=JsonTypeInfo.Id.NAME,include=As.PROPERTY,property="CodeType")  
@JsonSubTypes({@JsonSubTypes.Type(value=SEQCode.class, name="SEQ"),@JsonSubTypes.Type(value=LOOPCode.class, name="LOOP")})
public abstract class Code {
	public String CodeType;
}
