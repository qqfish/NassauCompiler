/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package sjtu.SemanticAnalyse;

import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author hp
 */
public class BlockDescription {

    private BlockDescription lastBlock;	//it can be null
    private ClassDescription returnType;
    private List<ParameterDescription> funcParameters;	//it must be set as a whole
    private List<VariableDescription> varList;
    private int currentSize;
    private int inSize;

    public BlockDescription(BlockDescription lastBlock, List<ParameterDescription> para, ClassDescription returnType) {
	this.lastBlock = lastBlock;
	funcParameters = para;
	varList = new ArrayList();
	if (this.lastBlock == null) {
	    currentSize = 0;
            inSize = 0;
	} else {
	    currentSize = this.lastBlock.getCurrentSize();
            inSize = currentSize;
	}
        this.returnType = returnType;
    }

    public int getCurrentSize() {
        return currentSize;
    }

    public ClassDescription getReturnType() {
        return returnType;
    }

    public void setReturnType(ClassDescription returnType) {
        this.returnType = returnType;
    }

    public List<ParameterDescription> getFuncParameters() {
	return funcParameters;
    }

    public void setFuncParameters(List<ParameterDescription> funcParameters) {
	this.funcParameters = funcParameters;
    }

    public List<VariableDescription> getVarList() {
	return varList;
    }

    public int getAddedSize() {
	return currentSize - inSize;
    }

    public BlockDescription getLastBlock() {
	return lastBlock;
    }

    public void setLastBlock(BlockDescription lastBlock) {
	this.lastBlock = lastBlock;
    }

    public boolean addVariable(VariableDescription p) {
	if (varList.contains(p)) {
	    return false;
	}
	p.setOffset(currentSize);
	currentSize += p.getSize();
	varList.add(p);
	return true;
    }

    public VariableDescription getVariable(String varName) {
	BlockDescription currentBlock = this;
	while (currentBlock != null) {
	    for (int i = 0; i < currentBlock.getVarList().size(); i++) {
		if (currentBlock.getVarList().get(i).getName().equals(varName)) {
		    return currentBlock.getVarList().get(i);
		}
	    }
	    currentBlock = currentBlock.getLastBlock();
	}
	return null;
    }
    
    public ParameterDescription getParameter(String paraName) {
	for(int i = 0; i < funcParameters.size(); i++){
	    if(funcParameters.get(i).getName().equals(paraName))
		return funcParameters.get(i);
	}
	return null;
    }
    
    public GetVariableResult getVariableWithName(List<String> name){
	GetVariableResult result = new GetVariableResult();
	if(name.size() == 0) {
            ParameterDescription thisPara = getParameter("this");
            result.setPara(thisPara);
            result.setOffset(0);
            result.setType(thisPara.getType());
            return result;
        }
	int offset = 0;
	
	String firstName = name.get(0);
	VariableDescription firstPara = getVariable(firstName);
	if(firstPara == null){
	    ParameterDescription theP = getParameter(firstName);
            int i = 1;
	    if(theP == null){ 
                theP = getParameter("this");
                i = 0;
            }
	    result.setPara(theP);
	    ClassDescription currentClass = theP.getType();
	    for(; i < name.size(); i++){
		String currentName = name.get(i);
		VariableDescription nextPara = currentClass.getOnePublicYield(currentName);
		if(nextPara == null && theP.getName().equals("this")){
		    nextPara = currentClass.getOnePrivateYield(currentName);
		}
		
		if(nextPara == null) return null;
		
		currentClass = nextPara.getType();
		offset += nextPara.getOffset();
	    }
	    result.setOffset(offset);
	    result.setType(currentClass);
	    return result;
	} else {
	    ClassDescription currentClass = firstPara.getType();
	    offset += firstPara.getOffset();
            offset += firstPara.getSize();
	    for(int i = 1; i < name.size(); i++){
		String currentName = name.get(i);
		VariableDescription nextPara = currentClass.getOnePublicYield(firstName);
		if(nextPara == null) return null;
		
		offset -= nextPara.getOffset();
		currentClass = nextPara.getType();
	    }
	    result.setOffset(offset);
	    result.setType(currentClass);
	    return result;
	}
    }
}
