/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package sjtu.SemanticAnalyse;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 *
 * @author fish
 */
public class ConstructorDescription {
    private List<ParameterDescription> parameters;
    private ClassDescription fartherClass;
    private boolean isImple;

    public ConstructorDescription(ClassDescription fartherClass) {
	this.fartherClass = fartherClass;
	this.parameters = new ArrayList();
        isImple = false;
        ParameterDescription p = new ParameterDescription(this.fartherClass,"this");
	addParameter(p);
    }

    public boolean addParameter(ParameterDescription p){
	if(parameters.contains(p))
	    return false;
	parameters.add(p);
	return true;
    }

    public List<ParameterDescription> getParameters() {
	return parameters;
    }

    public ClassDescription getFartherClass() {
	return fartherClass;
    }

    public void setFartherClass(ClassDescription fartherClass) {
	this.fartherClass = fartherClass;
    }

    public void setParameters(List<ParameterDescription> parameters) {
	this.parameters = parameters;
    }
    
    public void impleIt(){
        isImple = true;
    }
    
    public boolean getImpleIt(){
        return isImple;
    }

    @Override
    public boolean equals(Object obj) {
	if (obj == null) {
	    return false;
	}
	if (getClass() != obj.getClass()) {
	    return false;
	}
	final ConstructorDescription other = (ConstructorDescription) obj;
	if (!Objects.equals(this.parameters.size(), other.parameters.size())) {
	    return false;
	}
	for(int i = 0; i < this.parameters.size(); i++){
	    if(!Objects.equals(this.parameters.get(i).getType(), other.parameters.get(i).getType())){
		return false;
	    }
	}
	if (!Objects.equals(this.fartherClass, other.fartherClass)) {
	    return false;
	}
	return true;
    }

    @Override
    public int hashCode() {
	int hash = 5;
	hash = 67 * hash + Objects.hashCode(this.parameters);
	hash = 67 * hash + Objects.hashCode(this.fartherClass);
	return hash;
    }
    
    public String getAssemblyName(){
        String str = "__CONS" + fartherClass.getPackageName() + "_" + fartherClass.getName();
        for(int i = 0 ; i < parameters.size(); i++){
            str += parameters.get(i).getName();
        }
        return str;
    }
    
}
