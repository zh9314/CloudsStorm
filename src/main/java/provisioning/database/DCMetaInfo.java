package provisioning.database;

public abstract class DCMetaInfo {
	public String domain;
	public String endpoint;
	
	///location info. It is the three digits code for country.
	///Satisfy the standard of ISO 3166.
	public String country;
	
	////double number with a direction number
	public String longitude;
	public String latitude;
	
	public String availability; 
}
