/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package sjtu.LexicalAnalyse;

import java.io.InputStream;

/**
 *
 * @author yuxiao
 */
public class LexicalUsage {

    static NassauToken t;

    public String analyse(InputStream in) throws ParseException {
        String result = "";
        if (t == null) {
            t = new NassauToken(in);
        } else {
            t.ReInit(in);
        }
        ASTStart n = t.Start();
        result = n.dump();
        return result;
    }
}
