/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package sjtu.SemanticAnalyse;

import sjtu.ParseAnalyse.Token;

/**
 *
 * @author fish
 */
public class SemanticException extends Exception {

    private String error;
    private Token t;

    public SemanticException(String error, Token token) {
        super();
        this.error = error;
        t = token;
    }

    @Override
    public String getMessage() {
        String result;
        result = "\n error: " + error + ".  at line " + t.beginLine + " column " + t.beginColumn + "\n";
        return result;
    }

    public int getBeginColumn() {
        return t.beginColumn;
    }
    
    public int getEndColumn(){
        return t.endColumn;
    }

    public int getBeginLine() {
        return t.beginLine;
    }
    
    public int getEndLine(){
        return t.endLine;
    }
}
