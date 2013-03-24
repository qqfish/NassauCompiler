/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package sjtu.SemanticAnalyse;

import java.util.List;
import java.util.Objects;

/**
 *
 * @author fish
 */
public class VariableDescription {
    protected String name;
    protected ClassDescription type;
    protected int offset;

    public VariableDescription(String name, ClassDescription type) {
	this.name = name;
	this.type = type;
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

    public int getOffset() {
	return offset;
    }

    public void setOffset(int offset) {
	this.offset = offset;
    }
    
    public int getSize(){
	return type.getSize();
    }

    @Override
    public boolean equals(Object obj) {
	if (obj == null) {
	    return false;
	}
	if (getClass() != obj.getClass()) {
	    return false;
	}
	final VariableDescription other = (VariableDescription) obj;
	if (!Objects.equals(this.name, other.name)) {
	    return false;
	}
	return true;
    }

    @Override
    public int hashCode() {
	int hash = 7;
	return hash;
    }
    
    
}
