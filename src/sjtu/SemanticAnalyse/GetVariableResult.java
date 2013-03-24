/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package sjtu.SemanticAnalyse;

/**
 *
 * @author fish
 */
public class GetVariableResult {
    private int offset;
    private ClassDescription type;
    private ParameterDescription para;

    public GetVariableResult() {
	offset = 0;
	type = null;
	para = null;
    }

    public int getOffset() {
	return offset;
    }

    public void setOffset(int offset) {
	this.offset = offset;
    }

    public ParameterDescription getPara() {
	return para;
    }

    public void setPara(ParameterDescription para) {
	this.para = para;
    }

    public ClassDescription getType() {
	return type;
    }

    public void setType(ClassDescription type) {
	this.type = type;
    }
    
    
}
