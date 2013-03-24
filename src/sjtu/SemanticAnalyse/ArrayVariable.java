/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package sjtu.SemanticAnalyse;


/**
 *
 * @author fish
 */
public class ArrayVariable extends VariableDescription{
    protected int len;
    public ArrayVariable(String name, ClassDescription type, int len){
	super(name, type);
	this.len = len;
    }
    
    public ArrayVariable(String name, ClassDescription type){
        super(name,type);
        len = 65536*256;
    }

    public int getLen() {
	return len;
    }
    
    @Override
    public int getSize(){
	int result = type.getSize() * len;
	return result;
    }
    
}
