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
public class ClassDescription {

    protected String packageName;
    protected String name;
    protected int size;
    protected List<VariableDescription> privateYield;
    protected List<VariableDescription> publicYield;
    protected List<ConstructorDescription> constructor;
    protected List<FunctionDescription> privateFunction;
    protected List<FunctionDescription> publicFunction;

    public ClassDescription(String packageName, String name) {
	this.packageName = packageName;
	this.name = name;
	privateYield = new ArrayList();
	publicYield = new ArrayList();
	constructor = new ArrayList();
	privateFunction = new ArrayList();
	publicFunction = new ArrayList();
	size = 0;
    }
    
    
    public FunctionDescription confirmPrivateFunction(ClassDescription returnType, String funcName, List<ParameterDescription> para) {
	for (int i = 0; i < privateFunction.size(); i++) {
	    if (privateFunction.get(i).getName().equals(funcName)) {
		FunctionDescription result = privateFunction.get(i);
		if(!result.getReturnType().equals(returnType)){
		    continue;
		}
		if (result.getParameters().size() != para.size() + 1) {
		    continue;
		}
		boolean check = true;
		for (int j = 0; j < para.size(); j++) {
		    if (!result.getParameters().get(j + 1).equals(para.get(j))) {
			check = false;
			break;
		    }
		}
		if (check) {
		    return result;
		}
	    }
	}
	return null;
    }
    
    public FunctionDescription confirmPublicFunction(ClassDescription returnType, String funcName, List<ParameterDescription> para) {
	for (int i = 0; i < publicFunction.size(); i++) {
	    if (publicFunction.get(i).getName().equals(funcName)) {
		FunctionDescription result = publicFunction.get(i);
		if(!result.getReturnType().equals(returnType)){
		    continue;
		}
		if (result.getParameters().size() != para.size() + 1) {
		    continue;
		}
		boolean check = true;
		for (int j = 0; j < para.size(); j++) {
		    if (!result.getParameters().get(j+1).equals(para.get(j))) {
			check = false;
			break;
		    }
		}
		if (check) {
		    return result;
		}
	    }
	}
	return null;
    }
    
    public ConstructorDescription confirmConstructor(List<ParameterDescription> para){
	for (int i = 0; i < constructor.size(); i++) {
	    ConstructorDescription result = constructor.get(i);
	    if (result.getParameters().size() != para.size() + 1) {
		continue;
	    }
	    boolean check = true;
	    for (int j = 0; j < para.size(); j++) {
		if (!result.getParameters().get(j + 1).equals(para.get(j))) {
		    check = false;
		    break;
		}
	    }
	    if (check) {
		return result;
	    }
	}
	return null;	
    }

    public ConstructorDescription getConstructor(List<ClassDescription> para) {
	for (int i = 0; i < constructor.size(); i++) {
	    ConstructorDescription result = constructor.get(i);
	    if (result.getParameters().size() != para.size()) {
		continue;
	    }
	    boolean check = true;
	    for (int j = 0; j < result.getParameters().size(); j++) {
		if (!result.getParameters().get(j).getType().equals(para.get(j))) {
		    check = false;
		    continue;
		}
	    }
	    if (check) {
		return result;
	    }
	}
	return null;
    }

    public FunctionDescription getPublicFunction(String funcName, List<ClassDescription> para) {
	for (int i = 0; i < publicFunction.size(); i++) {
	    if (publicFunction.get(i).getName().equals(funcName)) {
		FunctionDescription result = publicFunction.get(i);
		if (result.getParameters().size() != para.size()) {
		    continue;
		}
		boolean check = true;
		for (int j = 0; j < result.getParameters().size(); j++) {
		    if (!result.getParameters().get(j).getType().equals(para.get(j))) {
			check = false;
			continue;
		    }
		}
		if (check) {
		    return result;
		}
	    }
	}
	return null;
    }

    public FunctionDescription getPrivateFunction(String funcName, List<ClassDescription> para) {
	for (int i = 0; i < privateFunction.size(); i++) {
	    if (privateFunction.get(i).getName().equals(funcName)) {
		FunctionDescription result = privateFunction.get(i);
		if (result.getParameters().size() != para.size()) {
		    continue;
		}
		boolean check = true;
		for (int j = 0; j < result.getParameters().size(); j++) {
		    if (!result.getParameters().get(j).getType().equals(para.get(j))) {
			check = false;
			continue;
		    }
		}
		if (check) {
		    return result;
		}
	    }
	}
	return null;
    }

    public boolean isPrimitive() {
	return false;
    }

    public boolean addPrivateYield(VariableDescription v) {
	if (privateYield.contains(v)) {
	    return false;
	}
	if (publicYield.contains(v)) {
	    return false;
	}
	privateYield.add(v);
	v.setOffset(size);
	size += v.getType().getSize();
	return true;
    }

    public boolean addPublicYield(VariableDescription v) {
	if (privateYield.contains(v)) {
	    return false;
	}
	if (publicYield.contains(v)) {
	    return false;
	}
	publicYield.add(v);
	v.setOffset(size);
	size += v.getType().getSize();
	return true;
    }
    
    public VariableDescription getOnePublicYield(String varName){
	for(int i = 0; i < publicYield.size(); i++){
	    if(publicYield.get(i).getName().equals(varName))
		return publicYield.get(i);
	}
	return null;
    }
    
    public VariableDescription getOnePrivateYield(String varName){
	for(int i = 0; i < privateYield.size(); i++){
	    if(privateYield.get(i).getName().equals(varName))
		return privateYield.get(i);
	}
	return null;
    }

    public boolean addConstructor(ConstructorDescription c) {
	if (constructor.contains(c)) {
	    return false;
	}
	constructor.add(c);
	return true;
    }

    public boolean addPrivateFunction(FunctionDescription f) {
	if (privateFunction.contains(f)) {
	    return false;
	}
	if (publicFunction.contains(f)) {
	    return false;
	}
	privateFunction.add(f);
	return true;
    }

    public boolean addPublicFunction(FunctionDescription f) {
	if (privateFunction.contains(f)) {
	    return false;
	}
	if (publicFunction.contains(f)) {
	    return false;
	}
	publicFunction.add(f);
	return true;
    }

    public List<ConstructorDescription> getConstructor() {
	return constructor;
    }

    public List<FunctionDescription> getPrivateFunction() {
	return privateFunction;
    }

    public List<VariableDescription> getPrivateYield() {
	return privateYield;
    }

    public List<FunctionDescription> getPublicFunction() {
	return publicFunction;
    }

    public List<VariableDescription> getPublicYield() {
	return publicYield;
    }

    public String getName() {
	return name;
    }

    public String getPackageName() {
	return packageName;
    }

    public int getSize() {
	return size;
    }

    @Override
    public boolean equals(Object obj) {
	if (obj == null) {
	    return false;
	}
	if (getClass() != obj.getClass()) {
	    return false;
	}
	final ClassDescription other = (ClassDescription) obj;
	if (!Objects.equals(this.packageName, other.packageName)) {
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
	hash = 97 * hash + Objects.hashCode(this.packageName);
	hash = 97 * hash + Objects.hashCode(this.name);
	return hash;
    }
}
