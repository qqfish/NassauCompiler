/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package sjtu.ParseAnalyse;

import java.io.InputStream;
import javax.swing.tree.DefaultMutableTreeNode;
import sjtu.SemanticAnalyse.SemanticAnalyse;
import sjtu.SemanticAnalyse.SemanticException;

/**
 *
 * @author yuxiao
 */
public class ParseAnalyse {

    static Nassau t;

    public ASTStart analyse(InputStream in, DefaultMutableTreeNode root) throws ParseException {
        if (t == null) {
            t = new Nassau(in);
        } else {
            t.ReInit(in);
        }
        ASTStart start = t.Start();
        start.dump(root);
        return start;
    }
}
