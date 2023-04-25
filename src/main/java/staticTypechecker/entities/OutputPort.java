package staticTypechecker.entities;

import java.util.List;

/**
 * Represents an output port in Jolie
 * 
 * @author Kasper Bergstedt (kberg18@student.sdu.dk)
 */
public class OutputPort implements Symbol {
	private String name; 				// the name of the port
	private String location; 			// the location of the port
	private String protocol; 			// the protocol of the port
	private List<String> interfaces; 	// a list of the interfaces this output port uses

	public OutputPort(String name, String location, String protocol, List<String> interfaces){
		this.name = name;
		this.location = location;
		this.protocol = protocol;
		this.interfaces = interfaces;
	}

	public String name(){
		return this.name;
	}

	public String location(){
		return this.location;
	}

	public String protocol(){
		return this.protocol;
	}

	public List<String> interfaces(){
		return this.interfaces;
	}

	public void setName(String name){
		this.name = name;
	}

	public void setLocation(String location){
		this.location = location;
	}

	public void setProtocol(String protocol){
		this.protocol = protocol;
	}

	public void setInterfaces(List<String> interfaces){
		this.interfaces = interfaces;
	}

	public boolean equals(Object other){
		if(this == other){
			return true;
		}

		if(!(other instanceof OutputPort)){
			return false;
		}

		OutputPort parsedOther = (OutputPort)other;

		return 	this.name.equals(parsedOther.name) && 
				this.location.equals(parsedOther.location) && 
				this.protocol.equals(parsedOther.protocol) && 
				this.interfaces.equals(parsedOther.interfaces);
	}

	public String prettyString(){
		if(this.name == null){
			return "null";
		}
		
		return this.name + " at " + this.location + " via " + this.protocol + " using " + this.interfaces;
	}
}
