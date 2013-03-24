/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package sjtu.SemanticAnalyse;

/**
 *
 * @author fish
 */
public class PrimitiveClass extends ClassDescription{
    public PrimitiveClass(String name, int theSize){
	super("",name);
	size = theSize;
    }
    
    @Override
    public boolean isPrimitive(){
	return true;
    }
    
    public boolean isInt(){
        if(name.equals("int") || name.equals("short") || name.equals("char") || name.equals("byte") || name.equals("boolean")){
            return true;
        } else {
            return false;
        }
    }
    
    public boolean isFloat(){
        if(name.equals("float"))
            return true;
        else 
            return false;
    }

}
