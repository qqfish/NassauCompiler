/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package sjtu.SemanticAnalyse;

import java.util.Objects;

/**
 *
 * @author fish
 */
public class ParameterDescription {
    private ClassDescription type;
    private String name;

    public ParameterDescription(ClassDescription type, String name) {
	this.type = type;
	this.name = name;
    }

    public String getName() {
	return name;
    }

    public void setName(String name) {
	this.name = name;
    }

    public ClassDescription getType() {
	return type;
    }

    public void setType(ClassDescription type) {
	this.type = type;
    }

    @Override
    public boolean equals(Object obj) {
	if (obj == null) {
	    return false;
	}
	if (getClass() != obj.getClass()) {
	    return false;
	}
	final ParameterDescription other = (ParameterDescription) obj;
	if (!Objects.equals(this.type, other.type)) {
	    return false;
	}
	if (!Objects.equals(this.name, other.name)) {
	    return false;
	}
	return true;
    }

    @Override
    public int hashCode() {
	int hash = 7;
	hash = 23 * hash + Objects.hashCode(this.type);
	hash = 23 * hash + Objects.hashCode(this.name);
	return hash;
    }


    
    public String getAssemblyName(){
	return "__Var" + name;
    }
    
    public boolean checkParameter(VariableDescription v){
	return v.getType().equals(type);
    }
}
