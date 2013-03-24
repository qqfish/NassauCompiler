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
public class FunctionDescription {
    private String name;
    private ClassDescription fartherClass;
    private ClassDescription returnType;
    private List<ParameterDescription> parameters;
    private boolean isImple;

    public FunctionDescription(String name, ClassDescription fartherClass, ClassDescription returnType) {
	this.name = name;
	this.fartherClass = fartherClass;
	this.returnType = returnType;
	this.parameters = new ArrayList();
        isImple = false;
	ParameterDescription p = new ParameterDescription(this.fartherClass,"this");
	addParameter(p);
    }
    
    public boolean addParameter(ParameterDescription p){
	if(parameters.contains(p)) return false;
	parameters.add(p);
	return true;
    }

    public ClassDescription getFartherClass() {
	return fartherClass;
    }

    public void setFartherClass(ClassDescription fartherClass) {
	this.fartherClass = fartherClass;
    }

    public String getName() {
	return name;
    }

    public void setName(String name) {
	this.name = name;
    }

    public void setParameters(List<ParameterDescription> parameters) {
	this.parameters = parameters;
    }

    public List<ParameterDescription> getParameters() {
	return parameters;
    }

    public ClassDescription getReturnType() {
	return returnType;
    }

    public void setReturnType(ClassDescription returnType) {
	this.returnType = returnType;
    }

    @Override
    public boolean equals(Object obj) {
	if (obj == null) {
	    return false;
	}
	if (getClass() != obj.getClass()) {
	    return false;
	}
	final FunctionDescription other = (FunctionDescription) obj;
	if (!Objects.equals(this.name, other.name)) {
	    return false;
	}
	if (!Objects.equals(this.fartherClass, other.fartherClass)) {
	    return false;
	}
	if(!Objects.equals(this.parameters.size(), other.parameters.size())){
	    return false;
	}
	for(int i = 0; i < parameters.size(); i++){
	    if(!Objects.equals(this.parameters.get(i).getType(), other.parameters.get(i).getType())){
		return false;
	    }
	}
	return true;
    }
    
    public void impleIt(){
        isImple = true;
    }
    
    public boolean getIsImple(){
        return isImple;
    }

    @Override
    public int hashCode() {
	int hash = 7;
	hash = 97 * hash + Objects.hashCode(this.name);
	hash = 97 * hash + Objects.hashCode(this.fartherClass);
	hash = 97 * hash + Objects.hashCode(this.parameters);
	return hash;
    }
    
    public String getAssemblyName(){
	String str = "__FUNC" + fartherClass.getPackageName() + "_" + fartherClass.getName() + "_" + name;
        for(int i = 0 ; i < parameters.size(); i++){
            str += parameters.get(i).getName();
        }
        return str;
    }
}
