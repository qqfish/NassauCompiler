/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package sjtu.SemanticAnalyse;

import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author fish
 */
public class SymbolTable {
    private List<ClassDescription> classTable;
    private List<ClassDescription> staticTable;

    public SymbolTable() {
	classTable = new ArrayList();
	staticTable = new ArrayList();
	
	//add primitive  class
	ClassDescription INT = new PrimitiveClass("int",4);
	//ClassDescription DOUBLE = new PrimitiveClass("double", 8);
	ClassDescription FLOAT = new PrimitiveClass("float", 4);
	ClassDescription BOOLEAN = new PrimitiveClass("boolean",1);
	ClassDescription BYTE = new PrimitiveClass("byte",1);
	ClassDescription CHAR = new PrimitiveClass("char",1);
	//ClassDescription LONG = new PrimitiveClass("long",8);
	ClassDescription SHORT = new PrimitiveClass("short",2);
	ClassDescription VOID = new PrimitiveClass("void",0);
	
	addClass(INT);
	//addClass(DOUBLE);
	addClass(FLOAT);
	addClass(BOOLEAN);
	addClass(BYTE);
	addClass(CHAR);
	//addClass(LONG);
	addClass(SHORT);
	addClass(VOID);
    }
    
    public ClassDescription getClassDescription(String name, String packageName){
	for(int i = 0; i < classTable.size(); i++){
	    if(classTable.get(i).getName().equals(name) && classTable.get(i).getPackageName().equals(packageName)){
		return classTable.get(i);
	    }
	}
	return null;
    }
    
    public ClassDescription getStaticDescription(String name, String packageName){
	for(int i = 0; i < staticTable.size(); i++){
	    if(staticTable.get(i).getName().equals(name) && staticTable.get(i).getPackageName().equals(packageName)){
		return staticTable.get(i);
	    }
	}
	return null;
    }
    
    public boolean addClass(ClassDescription c){
	if(classTable.contains(c)) return false;
	classTable.add(c);
	return true;
    }
    
    public boolean addStatic(ClassDescription c){
	if(staticTable.contains(c)) return false;
	staticTable.add(c);
	return true;
    }

    public List<ClassDescription> getClassTable() {
	return classTable;
    }

    public List<ClassDescription> getStaticTable() {
	return staticTable;
    }
    
    
}
