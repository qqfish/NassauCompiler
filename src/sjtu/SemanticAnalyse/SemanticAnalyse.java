/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package sjtu.SemanticAnalyse;

import java.io.BufferedWriter;
import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PipedOutputStream;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JTextArea;
import sjtu.ParseAnalyse.*;

/**
 *
 * @author fish
 */
public class SemanticAnalyse {

    private SymbolTable symbolTable;
    private ASTStart astTree;
    private String packageName;
    private List<String> packageScan;
    private OutputStream out;
    BufferedWriter buffer;
    OutputStreamWriter osw;

    public SemanticAnalyse(ASTStart astTree) {
        this.astTree = astTree;
        symbolTable = new SymbolTable();
        packageScan = new ArrayList();
        out = new ByteArrayOutputStream();
        osw = new OutputStreamWriter(out);
        buffer = new BufferedWriter(osw);
    }

    public String run(JTextArea area) throws SemanticException, IOException {
        String result = "sucess\n";
        entryPoint();
        area.setText("");
        area.append(out.toString());
        return result;

    }

    private void entryPoint() throws SemanticException, IOException {
        //header information
        buffer.write("TITLE MASM Template						(main.asm)\n");
        buffer.write("INCLUDE Irvine32.inc\n");
        buffer.write(".data\n");
        buffer.write("__PARAFUNC__0 DWORD 0d\n");
        buffer.write("__PARAFUNC__1 DWORD 0d\n");
        buffer.write("__PARAFUNC__2 DWORD 0d\n");
        buffer.write("__PARAFUNC__3 DWORD 0d\n");
        buffer.write("__PARAFUNC__4 DWORD 0d\n");
        buffer.write("__PARAFUNC__5 DWORD 0d\n");
        buffer.write("__PARAFUNC__6 DWORD 0d\n");
        buffer.write("__PARAFUNC__7 DWORD 0d\n");
        buffer.append(".code\n");
        buffer.flush();

        int child = 0;

        //pakage name
        packageName = "";
        SimpleNode pkg = (SimpleNode) astTree.jjtGetChild(child);
        pkg = (SimpleNode) pkg.jjtGetChild(0);
        while (pkg.jjtGetNumChildren() > 1) {
            ASTIdentifier pkgName = (ASTIdentifier) pkg.jjtGetChild(0);
            packageName += (pkgName.jjtGetToken().image + "_");
            pkg = (SimpleNode) pkg.jjtGetChild(1);
        }
        packageName += ((SimpleNode) pkg.jjtGetChild(0)).jjtGetToken().image;
        packageScan.add(packageName);
        packageScan.add("");
        child++;


        //add class
        for (int i = 1; i < astTree.jjtGetNumChildren(); i++) {
            SimpleNode currentNode = (SimpleNode) astTree.jjtGetChild(i);
            //System.out.println(currentNode.toString());
            if (currentNode.toString().equals("ClassDeclaration")) {
                classDeclarationScan((ASTClassDeclaration) currentNode, packageName);
            } else if (currentNode.toString().equals("ClassImplement")) {
                classImplementScan((ASTClassImplement) currentNode, packageName);
            }
        }
    }

    private void classDeclarationScan(ASTClassDeclaration node, String packageName) throws SemanticException, IOException {
        ASTIdentifier id = (ASTIdentifier) node.jjtGetChild(0);
        String name = id.jjtGetToken().image;
        ClassDescription c = new ClassDescription(packageName, name);
        ClassDescription s = new ClassDescription(packageName, name);
        if (!(symbolTable.addClass(c) && symbolTable.addStatic(s))) {
            throw new SemanticException("Same Class name", id.jjtGetToken());
        }

        //add field and function
        for (int k = 1; k < node.jjtGetNumChildren(); k++) {
            SimpleNode classNode = (SimpleNode) node.jjtGetChild(k);
            //System.out.println(classNode.toString());
            if (classNode.toString().equals("StaticInitializer")) {
                //leave blank
            } else if (classNode.toString().equals("ConstructorDeclaration")) {
                classConstructorScan((ASTConstructorDeclaration) classNode, c);
            } else if ((classNode.toString().equals("MethodDeclaration"))) {
                classMethodScan((ASTMethodDeclaration) classNode, c, s);
            } else if ((classNode.toString().equals("FieldDeclaration"))) {
                classFieldScan((ASTFieldDeclaration) classNode, c, s);
            }
        }
    }

    private void classConstructorScan(ASTConstructorDeclaration node, ClassDescription theClass) throws SemanticException, IOException {

        SimpleNode nameNode = (SimpleNode) node.jjtGetChild(0);
        String consName = nameNode.jjtGetToken().image;

        if (!consName.equals(theClass.getName())) {
            throw new SemanticException("error constructor name", nameNode.jjtGetToken());
        }

        ConstructorDescription consDes = new ConstructorDescription(theClass);

        for (int i = 1; i < node.jjtGetNumChildren(); i++) {
            SimpleNode tmp = (SimpleNode) node.jjtGetChild(i);

            //Parameter
            if (tmp.toString().equals("FormalParameter")) {
                SimpleNode paraType = (SimpleNode) tmp.jjtGetChild(0);
                ClassDescription paraClass = null;
                paraClass = getParameterType(paraType);

                SimpleNode variableName = (SimpleNode) tmp.jjtGetChild(1);
                ParameterDescription var = new ParameterDescription(paraClass, variableName.jjtGetToken().image);
                consDes.addParameter(var);
            } else if (tmp.toString().equals("Block")) {
                if (!theClass.addConstructor(consDes)) {
                    throw new SemanticException("duplicate function", nameNode.jjtGetToken());
                }
                consDes.impleIt();

                buffer.write(consDes.getAssemblyName() + " PROC");
                for (int j = 0; j < consDes.getParameters().size(); j++) {
                    buffer.write(",\n" + consDes.getParameters().get(j).getAssemblyName() + ":PTR BYTE");
                    buffer.write(",\n" + consDes.getParameters().get(j).getAssemblyName() + "_NUM:DWORD");
                }
                buffer.newLine();
                buffer.flush();
                blockScan((ASTBlock) tmp, consDes.getParameters(), symbolTable.getClassDescription("void", ""), null);

                buffer.write(consDes.getAssemblyName() + "ENDP");
                return;
            }
        }
        if (!theClass.addConstructor(consDes)) {
            throw new SemanticException("duplicate function", nameNode.jjtGetToken());
        }

        buffer.write(consDes.getAssemblyName() + " PROTO");
        for (int j = 0; j < consDes.getParameters().size(); j++) {
            buffer.write(",\n" + consDes.getParameters().get(j).getAssemblyName() + ":PTR BYTE");
            //buffer.write(",\n" + consDes.getParameters().get(j).getAssemblyName() + "_NUM:DWORD");
        }
        buffer.newLine();
        buffer.flush();
    }

    private void classMethodScan(ASTMethodDeclaration node, ClassDescription theClass, ClassDescription theStatic) throws SemanticException, IOException {
        SimpleNode tmp = (SimpleNode) node.jjtGetChild(0);
        String modifier = tmp.jjtGetToken().image;

        SimpleNode returnTypeNode = (SimpleNode) node.jjtGetChild(1);
        if (returnTypeNode.toString().equals("ArrayType")) {
            SimpleNode errorNode = returnTypeNode;
            while (errorNode.jjtGetToken() == null) {
                errorNode = (SimpleNode) errorNode.jjtGetChild(0);
            }
            throw new SemanticException("return array type is not supported", errorNode.jjtGetToken());
        }
        ClassDescription returnType;
        if (returnTypeNode.toString().equals("voidType")) {
            returnType = symbolTable.getClassDescription("void", "");
        } else {
            returnType = getParameterType((ASTType) returnTypeNode);
        }

        SimpleNode funcNameNode = (SimpleNode) node.jjtGetChild(2);

        FunctionDescription func;
        if (modifier.equals("static")) {
            func = new FunctionDescription(funcNameNode.jjtGetToken().image, theStatic, returnType);
        } else {
            func = new FunctionDescription(funcNameNode.jjtGetToken().image, theClass, returnType);
        }

        for (int i = 3; i < node.jjtGetNumChildren(); i++) {
            tmp = (SimpleNode) node.jjtGetChild(i);

            //Parameter
            if (tmp.toString().equals("FormalParameter")) {
                SimpleNode paraType = (SimpleNode) tmp.jjtGetChild(0);
                ClassDescription paraClass = null;
                paraClass = getParameterType(paraType);

                SimpleNode variableName = (SimpleNode) tmp.jjtGetChild(1);
                ParameterDescription var = new ParameterDescription(paraClass, variableName.jjtGetToken().image);
                func.addParameter(var);
            } else if (tmp.toString().equals("Block")) {
                boolean result = false;
                if (modifier.equals("static")) {
                    result = theStatic.addPublicFunction(func);
                } else if (modifier.equals("public")) {
                    result = theClass.addPublicFunction(func);
                } else if (modifier.equals("private")) {
                    result = theClass.addPrivateFunction(func);
                }
                if (!result) {
                    throw new SemanticException("duplicate function", funcNameNode.jjtGetToken());
                }
                func.impleIt();

                buffer.write(func.getAssemblyName() + " PROC");
                for (int j = 0; j < func.getParameters().size(); j++) {
                    buffer.write(",\n" + func.getParameters().get(j).getAssemblyName() + ":PTR BYTE");
                    buffer.write(",\n" + func.getParameters().get(j).getAssemblyName() + "_NUM:DWORD");
                }
                buffer.newLine();
                buffer.flush();

                blockScan((ASTBlock) tmp, func.getParameters(), func.getReturnType(), null);

                buffer.write(func.getAssemblyName() + " ENDP\n");
                buffer.flush();
                return;
            }
        }
        boolean result = false;
        if (modifier.equals("static")) {
            result = theStatic.addPublicFunction(func);
        } else if (modifier.equals("public")) {
            result = theClass.addPublicFunction(func);
        } else if (modifier.equals("private")) {
            result = theClass.addPrivateFunction(func);
        }
        if (!result) {
            throw new SemanticException("duplicate function", funcNameNode.jjtGetToken());
        }
        buffer.write(func.getAssemblyName() + " PROTO");
        for (int j = 0; j < func.getParameters().size(); j++) {
            buffer.write(",\n" + func.getParameters().get(j).getAssemblyName() + ":PTR BYTE");
            //buffer.write(",\n" + func.getParameters().get(j).getAssemblyName() + "_NUM:DWORD");
        }
        buffer.newLine();
        buffer.flush();
    }

    private void classFieldScan(ASTFieldDeclaration node, ClassDescription theClass, ClassDescription theStatic) throws SemanticException {
        SimpleNode tmp = (SimpleNode) node.jjtGetChild(0);
        String modifier = tmp.jjtGetToken().image;

        SimpleNode fieldTypeNode = (SimpleNode) node.jjtGetChild(1);
        if (fieldTypeNode.toString().equals("ArrayType")) {
            SimpleNode errorNode = fieldTypeNode;
            while (errorNode.jjtGetToken() == null) {
                errorNode = (SimpleNode) errorNode.jjtGetChild(0);
            }
            throw new SemanticException("array type is not supported", errorNode.jjtGetToken());
        }
        ClassDescription fieldType;
        fieldType = getParameterType((ASTType) fieldTypeNode);

        for (int i = 2; i < node.jjtGetNumChildren(); i++) {
            tmp = (SimpleNode) node.jjtGetChild(i);
            VariableDescription var;
            SimpleNode fieldIdNode;
            if (tmp.toString().equals("ArrayDeclarator")) {
                fieldIdNode = (SimpleNode) tmp.jjtGetChild(0);
                SimpleNode arrayNumNode = (SimpleNode) tmp.jjtGetChild(1);
                int num;
                try {
                    num = Integer.parseInt(arrayNumNode.jjtGetToken().image);
                } catch (NumberFormatException e) {
                    throw new SemanticException("number format exception", arrayNumNode.jjtGetToken());
                }
                var = new ArrayVariable(fieldIdNode.jjtGetToken().image, fieldType, num);
            } else {
                fieldIdNode = tmp;
                var = new VariableDescription(tmp.jjtGetToken().image, fieldType);
            }

            boolean result = false;
            if (modifier.equals("static")) {
                result = theStatic.addPublicYield(var);
            } else if (modifier.equals("public")) {
                result = theClass.addPublicYield(var);
            } else if (modifier.equals("private")) {
                result = theClass.addPrivateYield(var);
            }
            if (!result) {
                throw new SemanticException("duplicate field declaration", fieldIdNode.jjtGetToken());
            }
        }
    }

    private void classImplementScan(ASTClassImplement node, String packageName) throws SemanticException, IOException {
        ASTIdentifier id = (ASTIdentifier) node.jjtGetChild(0);
        String name = id.jjtGetToken().image;
        ClassDescription c = symbolTable.getClassDescription(name, packageName);
        ClassDescription s = symbolTable.getStaticDescription(name, packageName);
        if (!(c != null && s != null)) {
            throw new SemanticException("Class not found", id.jjtGetToken());
        }

        //add field and function
        for (int k = 1; k < node.jjtGetNumChildren(); k++) {
            SimpleNode classNode = (SimpleNode) node.jjtGetChild(k);
            if (classNode.toString().equals("StaticInitializer")) {
                //leave blank
            } else if (classNode.toString().equals("ConstructorImplement")) {
                constructorImplementScan((ASTConstructorImplement) classNode, c);
            } else if ((classNode.toString().equals("ClassMethodImplement"))) {
                classMethodImplementScan((ASTClassMethodImplement) classNode, c, s);
            }
        }
    }

    private void constructorImplementScan(ASTConstructorImplement node, ClassDescription theClass) throws SemanticException, IOException {

        SimpleNode nameNode = (SimpleNode) node.jjtGetChild(0);
        String consName = nameNode.jjtGetToken().image;

        if (!consName.equals(theClass.getName())) {
            throw new SemanticException("error constructor name", nameNode.jjtGetToken());
        }

        List<ParameterDescription> varList = new ArrayList();

        for (int i = 1; i < node.jjtGetNumChildren(); i++) {
            SimpleNode tmp = (SimpleNode) node.jjtGetChild(i);

            //Parameter
            if (tmp.toString().equals("FormalParameter")) {
                SimpleNode paraType = (SimpleNode) tmp.jjtGetChild(0);
                ClassDescription paraClass = null;
                paraClass = getParameterType(paraType);

                SimpleNode variableName = (SimpleNode) tmp.jjtGetChild(1);
                ParameterDescription var = new ParameterDescription(paraClass, variableName.jjtGetToken().image);
                varList.add(var);
            } else if (tmp.toString().equals("Block")) {
                ConstructorDescription consImp = theClass.confirmConstructor(varList);
                if (consImp == null) {
                    throw new SemanticException("constructor not found", nameNode.jjtGetToken());
                }
                if (consImp.getImpleIt()) {
                    throw new SemanticException("duplicate implement", nameNode.jjtGetToken());
                }
                consImp.impleIt();
                buffer.write(consImp.getAssemblyName() + " PROC");
                for (int j = 0; j < consImp.getParameters().size(); j++) {
                    buffer.write(",\n" + consImp.getParameters().get(j).getAssemblyName() + ":PTR BYTE");
                    //buffer.write(",\n" + consImp.getParameters().get(j).getAssemblyName() + "_NUM:DWORD");
                }
                buffer.newLine();
                buffer.flush();

                blockScan((ASTBlock) tmp, consImp.getParameters(), symbolTable.getClassDescription("void", ""), null);

                buffer.write(consImp.getAssemblyName() + " ENDP\n");
                buffer.flush();
                return;
            }
        }
    }

    private void classMethodImplementScan(ASTClassMethodImplement node, ClassDescription theClass, ClassDescription theStatic) throws SemanticException, IOException {
        SimpleNode tmp = (SimpleNode) node.jjtGetChild(0);
        String modifier = tmp.jjtGetToken().image;

        SimpleNode returnTypeNode = (SimpleNode) node.jjtGetChild(1);
        if (returnTypeNode.toString().equals("ArrayType")) {
            SimpleNode errorNode = returnTypeNode;
            while (errorNode.jjtGetToken() == null) {
                errorNode = (SimpleNode) errorNode.jjtGetChild(0);
            }
            throw new SemanticException("return array type is not supported", errorNode.jjtGetToken());
        }
        ClassDescription returnType;
        if (returnTypeNode.toString().equals("voidType")) {
            returnType = symbolTable.getClassDescription("void", "");
        } else {
            returnType = getParameterType((ASTType) returnTypeNode);
        }

        SimpleNode funcNameNode = (SimpleNode) node.jjtGetChild(2);
        String funcName = funcNameNode.jjtGetToken().image;

        List<ParameterDescription> paraList = new ArrayList();

        for (int i = 2; i < node.jjtGetNumChildren(); i++) {
            tmp = (SimpleNode) node.jjtGetChild(i);

            //Parameter
            if (tmp.toString().equals("FormalParameter")) {
                SimpleNode paraType = (SimpleNode) tmp.jjtGetChild(0);
                ClassDescription paraClass = null;
                paraClass = getParameterType(paraType);

                SimpleNode variableName = (SimpleNode) tmp.jjtGetChild(1);
                ParameterDescription var = new ParameterDescription(paraClass, variableName.jjtGetToken().image);
                paraList.add(var);
            } else if (tmp.toString().equals("Block")) {
                FunctionDescription func = null;
                if (modifier.equals("static")) {
                    func = theStatic.confirmPublicFunction(returnType, funcName, paraList);
                } else if (modifier.equals("public")) {
                    func = theClass.confirmPublicFunction(returnType, funcName, paraList);
                } else if (modifier.equals("private")) {
                    func = theClass.confirmPrivateFunction(returnType, funcName, paraList);
                }
                if (func == null) {
                    throw new SemanticException("function not declare", funcNameNode.jjtGetToken());
                }
                if (func.getIsImple()) {
                    throw new SemanticException("duplicate implement", funcNameNode.jjtGetToken());
                }
                func.impleIt();

                buffer.write(func.getAssemblyName() + " PROC");
                for (int j = 0; j < func.getParameters().size(); j++) {
                    buffer.write(",\n" + func.getParameters().get(j).getAssemblyName() + ":PTR BYTE");
                    //buffer.write(",\n" + func.getParameters().get(j).getAssemblyName() + "_NUM:DWORD");
                }
                buffer.newLine();
                buffer.flush();

                blockScan((ASTBlock) tmp, func.getParameters(), func.getReturnType(), null);

                buffer.write(func.getAssemblyName() + " ENDP\n");
                buffer.flush();
                return;
            }
        }
    }

    private void blockScan(ASTBlock node, List<ParameterDescription> funcPara, ClassDescription returnType, BlockDescription lastBlock) throws SemanticException, IOException {
        BlockDescription currentBlock = new BlockDescription(lastBlock, funcPara, returnType);
        for (int i = 0; i < node.jjtGetNumChildren(); i++) {
            SimpleNode childNode = (SimpleNode) node.jjtGetChild(i);
            System.out.println(childNode.toString());
            if (childNode.toString().equals("LocalVariableDeclarationStatement")) {
                localVariableDeclarationStatementScan((ASTLocalVariableDeclarationStatement) childNode, currentBlock);
            } else if (childNode.toString().equals("CallFuntion")) {
                expressionScan(childNode, currentBlock);
                buffer.write("pop eax\n");
                buffer.flush();
            } else if (childNode.toString().equals("Assignment")) {
                expressionScan(childNode, currentBlock);
                buffer.write("pop eax\n");
                buffer.flush();
            } else if (childNode.toString().equals("IfStatement")) {
                ifStatementScan((ASTIfStatement) childNode, currentBlock);
            } else if (childNode.toString().equals("ForStatement")) {
            } else if (childNode.toString().equals("WhileStatement")) {
                whileStatementScan((ASTWhileStatement) childNode, currentBlock);
            } else if (childNode.toString().equals("DoStatement")) {
            } else if (childNode.toString().equals("BreakStatement")) {
                buffer.write(".BREAK\n");
                buffer.flush();
            } else if (childNode.toString().equals("ContinueStatement")) {
                buffer.write(".CONTINUE\n");
                buffer.flush();
            } else if (childNode.toString().equals("ReturnStatement")) {
                returnStatementScan((ASTReturnStatement) childNode, currentBlock);
            } else if (childNode.toString().equals("Block")) {
                blockScan((ASTBlock) childNode, currentBlock.getFuncParameters(), currentBlock.getReturnType(), currentBlock);
            } else if (childNode.toString().equals("PrintStatement")) {
                ClassDescription printType = expressionScan((SimpleNode) childNode.jjtGetChild(0), currentBlock);

                if (!printType.isPrimitive()) {
                    throw new SemanticException("print do not support this type", getToken(childNode));
                }
                if (((PrimitiveClass) printType).isInt()) {
                    buffer.write("pop eax\n");
                    buffer.write("call WriteInt\n");
                    buffer.flush();
                } else {
                    throw new SemanticException("print do not support this type", getToken(childNode));
                }
            }


        }
        //code delete the local variable
        buffer.write("add esp," + currentBlock.getAddedSize() + "d\n");
        if (lastBlock == null) {
            buffer.write("ret\n");
        }
        buffer.flush();
    }

    private void whileStatementScan(ASTWhileStatement node, BlockDescription currentBlock) throws SemanticException, IOException {
        SimpleNode expressionNode = (SimpleNode) node.jjtGetChild(0);
        ASTBlock blockNode = (ASTBlock) (SimpleNode) node.jjtGetChild(1);

        ClassDescription expType = expressionScan(expressionNode, currentBlock);
        if (!expType.isPrimitive()) {
            throw new SemanticException("error type of while", getToken(expressionNode));
        }
        buffer.write("pop eax\n");
        buffer.write(".WHILE eax != 0\n");
        buffer.flush();
        blockScan(blockNode, currentBlock.getFuncParameters(), currentBlock.getReturnType(), currentBlock);
        expType = expressionScan(expressionNode, currentBlock);
        if (!expType.isPrimitive()) {
            throw new SemanticException("error type of while", getToken(expressionNode));
        }
        buffer.write("pop eax\n");
        buffer.write(".ENDW\n");
    }

    private void returnStatementScan(ASTReturnStatement node, BlockDescription currentBlock) throws IOException, SemanticException {
        if (currentBlock.getReturnType().getSize() == 0 && node.jjtGetNumChildren() > 0) {
            throw new SemanticException("void return type", getToken(node));
        }

        if (node.jjtGetNumChildren() == 0) {
            buffer.write("and edx,0d\n");
            buffer.write("add esp," + currentBlock.getAddedSize() + "d\n");
            buffer.write("ret\n");
            buffer.flush();
        } else {
            ClassDescription expressionType = expressionScan((SimpleNode) node.jjtGetChild(0), currentBlock);

            if (!expressionType.isPrimitive()) {
                throw new SemanticException("return type not support", getToken(node));
            }

            if (((PrimitiveClass) expressionType).isInt()
                    && ((PrimitiveClass) currentBlock.getReturnType()).isInt()) {
                buffer.write("pop edx\n");
                buffer.write("add esp," + currentBlock.getAddedSize() + "d\n");
                buffer.write("ret\n");
                buffer.flush();
            } else {
                throw new SemanticException("return type not support", getToken(node));
            }
        }
    }

    private void localVariableDeclarationStatementScan(ASTLocalVariableDeclarationStatement node, BlockDescription currentBlock) throws SemanticException, IOException {
        ClassDescription paraClass = getParameterType((SimpleNode) node.jjtGetChild(0));
        for (int j = 1; j < node.jjtGetNumChildren(); j++) {
            SimpleNode variableNode = (SimpleNode) node.jjtGetChild(j);
            if (variableNode.toString().equals("AssignmentDeclarator")) {
                SimpleNode variableName = (SimpleNode) variableNode.jjtGetChild(0);
                if (variableName.toString().equals("ArrayDeclarator")) {
                    throw new SemanticException("not support array assignment declarator", getToken(variableName));
                }
                VariableDescription var = new VariableDescription(variableName.jjtGetToken().image, paraClass);
                if (!currentBlock.addVariable(var)) {
                    throw new SemanticException("duplicate variable declaration", variableName.jjtGetToken());
                }
                ClassDescription type = expressionScan((SimpleNode) variableNode.jjtGetChild(1), currentBlock);
                if (type == null || !type.isPrimitive() || !paraClass.isPrimitive()) {
                    SimpleNode errorNode = (SimpleNode) node.jjtGetChild(0);
                    if (errorNode.jjtGetToken() == null) {
                        errorNode = (SimpleNode) errorNode.jjtGetChild(0);
                    }
                    throw new SemanticException("type not match", errorNode.jjtGetToken());
                }
                if (((PrimitiveClass) type).isInt() && ((PrimitiveClass) paraClass).isInt()) {
                    buffer.write("pop eax\n");
                    if (paraClass.getSize() == 1) {
                        buffer.write("sub esp,1\n");
                        buffer.write("mov BYTE ptr[esp],al\n");
                    } else if (paraClass.getSize() == 2) {
                        buffer.write("sub esp,2\n");
                        buffer.write("mov WORD ptr[esp],ax\n");
                    } else {
                        buffer.write("push eax\n");
                    }
                    buffer.flush();
                } else {
                    throw new SemanticException("float not support yet", getToken(node));
                }
            } else {
                if (variableNode.toString().equals("ArrayDeclarator")) {
                    SimpleNode nameNode = (SimpleNode) variableNode.jjtGetChild(0);
                    int len = Integer.parseInt(((SimpleNode) variableNode.jjtGetChild(1)).jjtGetToken().image);
                    ArrayVariable var = new ArrayVariable(nameNode.jjtGetToken().image, paraClass, len);
                    if (!currentBlock.addVariable(var)) {
                        throw new SemanticException("duplicate variable declaration", variableNode.jjtGetToken());
                    }
                    buffer.write("sub esp," + var.getSize() + "d\n");
                    buffer.flush();
                } else {
                    VariableDescription var = new VariableDescription(variableNode.jjtGetToken().image, paraClass);
                    if (!currentBlock.addVariable(var)) {
                        throw new SemanticException("duplicate variable declaration", variableNode.jjtGetToken());
                    }

                    //code
                    buffer.write("sub esp," + paraClass.getSize() + "d\n");
                    buffer.flush();
                }
            }
        }
    }

    private void ifStatementScan(ASTIfStatement node, BlockDescription currentBlock) throws SemanticException, IOException {
        SimpleNode childNode = (SimpleNode) node.jjtGetChild(0);
        ASTBlock bb = (ASTBlock) node.jjtGetChild(1);
        ClassDescription type = expressionScan(childNode, currentBlock);

        if (type == null || !type.isPrimitive()) {
            throw new SemanticException("type not support as a boolean", getToken(childNode));
        }

        buffer.write("sub eax,eax\n");
        buffer.write("pop eax\n");
        if (type.getSize() == 1) {
            buffer.write(".IF al != 0\n");
        } else if (type.getSize() == 2) {
            buffer.write(".IF ax != 0\n");
        } else if (type.getSize() == 4) {
            buffer.write(".IF eax != 0\n");
        } else {
            throw new SemanticException("type not support as a boolean", getToken(childNode));
        }
        buffer.flush();

        blockScan(bb, currentBlock.getFuncParameters(), currentBlock.getReturnType(), currentBlock);

        for (int i = 2; i < node.jjtGetNumChildren(); i++) {
            childNode = (SimpleNode) node.jjtGetChild(i);
            if (!childNode.toString().equals("Block")) {
                i++;
                bb = (ASTBlock) node.jjtGetChild(i);
                type = expressionScan(childNode, currentBlock);

                if (type == null || !type.isPrimitive()) {
                    SimpleNode errorNode = childNode;
                    while (errorNode.jjtGetToken() == null) {
                        errorNode = (SimpleNode) errorNode.jjtGetChild(0);
                    }
                    throw new SemanticException("type not support as a boolean", errorNode.jjtGetToken());
                }

                buffer.write("sub eax,eax\n");
                buffer.write("pop eax\n");
                if (type.getSize() == 1) {
                    buffer.write(".ELSEIF al != 0\n");
                } else if (type.getSize() == 2) {
                    buffer.write(".ELSEIF ax != 0\n");
                } else if (type.getSize() == 4) {
                    buffer.write(".ELSEIF eax != 0\n");
                } else {
                    SimpleNode errorNode = childNode;
                    while (errorNode.jjtGetToken() == null) {
                        errorNode = (SimpleNode) errorNode.jjtGetChild(0);
                    }
                    throw new SemanticException("type not support as a boolean", errorNode.jjtGetToken());
                }
                buffer.flush();

                blockScan(bb, currentBlock.getFuncParameters(), currentBlock.getReturnType(), currentBlock);
            } else {
                buffer.append(".ELSE\n");
                buffer.flush();

                blockScan((ASTBlock) childNode, currentBlock.getFuncParameters(), currentBlock.getReturnType(), currentBlock);
            }
        }

        buffer.write(".ENDIF\n");
        buffer.flush();
    }

    private ClassDescription expressionScan(SimpleNode node, BlockDescription currentBlock) throws SemanticException, IOException {
        if (node.toString().equals("Assignment")) {
            SimpleNode nameNode = (SimpleNode) node.jjtGetChild(0);

            List<String> nameList;
            SimpleNode currentNode = nameNode;
            ASTArrayPrimarySuffix ArrayNode = null;

            if (currentNode.toString().equals("ArrayPrimarySuffix")) {
                ArrayNode = (ASTArrayPrimarySuffix) currentNode;
                currentNode = (SimpleNode) currentNode.jjtGetChild(0);
            }

            nameList = getNameList((SimpleNode) currentNode);

            GetVariableResult resultVar = currentBlock.getVariableWithName(nameList);


            ClassDescription childClass = expressionScan((SimpleNode) node.jjtGetChild(2), currentBlock);

            if (resultVar == null) {
                throw new SemanticException("error name", getToken(currentNode));
            }

            if (!childClass.isPrimitive()) {
                throw new SemanticException("assignment not support this variable", getToken(currentNode));
            }
            SimpleNode operatorNode = (SimpleNode) node.jjtGetChild(1);
            if (ArrayNode != null) {
                ClassDescription arrayExp = expressionScan((SimpleNode) ArrayNode.jjtGetChild(1), currentBlock);
                if (!arrayExp.isPrimitive() || !((PrimitiveClass) arrayExp).isInt()) {
                    throw new SemanticException("array error", getToken(currentNode));
                }
                if (!resultVar.getType().isPrimitive()) {
                    throw new SemanticException("assignment not support this variable", ((SimpleNode) currentNode.jjtGetChild(0)).jjtGetToken());
                }
                if (((PrimitiveClass) childClass).isInt() && ((PrimitiveClass) resultVar.getType()).isInt()) {
                    //code
                    buffer.write("pop eax\n");
                    if (arrayExp.getSize() == 1) {
                        buffer.write("and eax,00000000ffh\n");
                    } else if (arrayExp.getSize() == 2) {
                        buffer.write("and eax,0000ffffh\n");
                    }
                    buffer.write("mov ebx," + resultVar.getType().getSize() + "d\n");
                    buffer.write("mul ebx\n");
                    buffer.write("mov edx,eax\n");
                    if (resultVar.getPara() != null) {
                        buffer.write("add edx," + resultVar.getOffset() + "d\n");
                        buffer.write("mov ecx," + resultVar.getPara().getAssemblyName() + "\n");
                        buffer.write("add ecx,edx\n");
                        buffer.write("mov edx,ecx\n");
                    } else {
                        buffer.write("mov eax," + resultVar.getOffset() + "d\n");
                        buffer.write("sub eax,edx\n");
                        buffer.write("mov edx,eax\n");
                        buffer.write("mov ecx,ebp\n");
                        buffer.write("sub ecx,edx\n");
                        buffer.write("mov edx,ecx\n");
                    }
                    buffer.write("pop eax\n");
                    if (childClass.getSize() == 1) {
                        buffer.write("and eax,000000ffh\n");
                    } else if (childClass.getSize() == 2) {
                        buffer.write("and eax,0000ffffh\n");
                    }
                    if (resultVar.getType().getSize() == 1) {
                        buffer.write("mov BYTE ptr[edx],al\n");
                    } else if (resultVar.getType().getSize() == 2) {
                        buffer.write("mov WORD ptr[edx],ax\n");
                    } else {
                        buffer.write("mov DWORD ptr [edx],eax\n");
                    }
                    buffer.write("and eax,0h\n");
                    buffer.write("add eax,1d\n");
                    buffer.write("push eax\n");
                    buffer.flush();

                } else {
                    throw new SemanticException("type not match", getToken(currentNode));
                }
            } else {
                if (!resultVar.getType().isPrimitive()) {
                    throw new SemanticException("assignment not support this variable", ((SimpleNode) currentNode.jjtGetChild(0)).jjtGetToken());
                }
                PrimitiveClass baseClass = (PrimitiveClass) resultVar.getType();
                if (((PrimitiveClass) childClass).isInt() && baseClass.isInt()) {
                    buffer.write("mov edx," + resultVar.getOffset() + "d\n");
                    if (resultVar.getPara() != null) {
                        buffer.write("mov ecx," + resultVar.getPara().getAssemblyName() + "\n");
                        buffer.write("add ecx,edx\n");
                        buffer.write("mov edx,ecx\n");
                    } else {
                        buffer.write("mov ecx,ebp\n");
                        buffer.write("sub ecx,edx\n");
                        buffer.write("mov edx,ecx\n");
                    }
                    buffer.write("pop eax\n");
                    if (childClass.getSize() == 1) {
                        buffer.write("and eax,000000ffh\n");
                    } else if (childClass.getSize() == 2) {
                        buffer.write("and eax,0000ffffh\n");
                    }
                    if (baseClass.getSize() == 1) {
                        buffer.write("mov BYTE ptr[edx],al\n");
                    } else if (baseClass.getSize() == 2) {
                        buffer.write("mov WORD ptr[edx],ax\n");
                    } else {
                        buffer.write("mov DWORD ptr [edx],eax\n");
                    }
                    buffer.write("and eax,0h\n");
                    buffer.write("add eax,1d\n");
                    buffer.write("push eax\n");
                    buffer.flush();
                } else if (((PrimitiveClass) childClass).isFloat() && baseClass.isFloat()) {
                    //leave blank now
                } else {
                    throw new SemanticException("type not match", getToken(currentNode));
                }
            }

            return symbolTable.getClassDescription("boolean", "");
        } else if (node.toString().equals("ConditionalOrExpression")) {
            ClassDescription firstClass = expressionScan((SimpleNode) node.jjtGetChild(0), currentBlock);
            ClassDescription secondClass = expressionScan((SimpleNode) node.jjtGetChild(1), currentBlock);
            ClassDescription boo = symbolTable.getClassDescription("boolean", "");
            if (!firstClass.isPrimitive() || !secondClass.isPrimitive()) {
                throw new SemanticException("error type", getToken(node));
            }
            buffer.write("pop edx\n");
            buffer.write("pop ecx\n");
            buffer.write("or edx,ecd\n");
            buffer.write("and eax,0d\n");
            buffer.write(".IF edx != 0\n");
            buffer.write("add eax,1d\n");
            buffer.write(".ENDIF\n");
            buffer.write("push eax\n");

            return boo;
        } else if (node.toString().equals("ConditionalAndExpression")) {
            ClassDescription firstClass = expressionScan((SimpleNode) node.jjtGetChild(0), currentBlock);
            ClassDescription secondClass = expressionScan((SimpleNode) node.jjtGetChild(1), currentBlock);
            ClassDescription boo = symbolTable.getClassDescription("boolean", "");
            if (!firstClass.isPrimitive()
                    || !secondClass.isPrimitive()) {
                throw new SemanticException("error type", getToken(node));
            }
            buffer.write("pop edx\n");
            buffer.write("pop ecx\n");
            buffer.write("and edx,ecd\n");
            buffer.write("and eax,0d\n");
            buffer.write(".IF edx != 0\n");
            buffer.write("add eax,1d\n");
            buffer.write(".ENDIF\n");
            buffer.write("push eax\n");

            return boo;
        } else if (node.toString().equals("InclusiveOrExpression")) {
            ClassDescription firstClass = expressionScan((SimpleNode) node.jjtGetChild(0), currentBlock);
            ClassDescription secondClass = expressionScan((SimpleNode) node.jjtGetChild(1), currentBlock);
            SimpleNode tmp = node;
            ClassDescription boo = symbolTable.getClassDescription("boolean", "");
            ClassDescription integer = symbolTable.getClassDescription("int", "");
            while (tmp.jjtGetToken() == null) {
                tmp = (SimpleNode) tmp.jjtGetChild(0);
            }
            if (!firstClass.equals(secondClass)
                    || (!firstClass.equals(boo) && !firstClass.equals(integer))) {
                throw new SemanticException("error type", tmp.jjtGetToken());
            }

            //code


        } else if (node.toString().equals("ExclusiveOrExpression")) {
            ClassDescription firstClass = expressionScan((SimpleNode) node.jjtGetChild(0), currentBlock);
            ClassDescription secondClass = expressionScan((SimpleNode) node.jjtGetChild(1), currentBlock);
            SimpleNode tmp = node;
            ClassDescription boo = symbolTable.getClassDescription("boolean", "");
            ClassDescription integer = symbolTable.getClassDescription("int", "");
            while (tmp.jjtGetToken() == null) {
                tmp = (SimpleNode) tmp.jjtGetChild(0);
            }
            if (!firstClass.equals(secondClass)
                    || (!firstClass.equals(boo) && !firstClass.equals(integer))) {
                throw new SemanticException("error type", tmp.jjtGetToken());
            }

            //code


        } else if (node.toString().equals("AndExpression")) {
            ClassDescription firstClass = expressionScan((SimpleNode) node.jjtGetChild(0), currentBlock);
            ClassDescription secondClass = expressionScan((SimpleNode) node.jjtGetChild(1), currentBlock);
            SimpleNode tmp = node;
            ClassDescription boo = symbolTable.getClassDescription("boolean", "");
            ClassDescription integer = symbolTable.getClassDescription("int", "");
            while (tmp.jjtGetToken() == null) {
                tmp = (SimpleNode) tmp.jjtGetChild(0);
            }
            if (!firstClass.equals(secondClass)
                    || (!firstClass.equals(boo) && !firstClass.equals(integer))) {
                throw new SemanticException("error type", tmp.jjtGetToken());
            }

            //code


        } else if (node.toString().equals("EqualityExpression")) {
            ClassDescription boo = symbolTable.getClassDescription("boolean", "");
            ClassDescription secondClass = expressionScan((SimpleNode) node.jjtGetChild(1), currentBlock);
            ClassDescription firstClass = expressionScan((SimpleNode) node.jjtGetChild(0), currentBlock);
            if (!firstClass.isPrimitive()
                    || !secondClass.isPrimitive()) {
                throw new SemanticException("error type", getToken(node));
            }

            if (((PrimitiveClass) firstClass).isInt() && ((PrimitiveClass) secondClass).isInt()) {

                buffer.write("pop eax\n");
                buffer.write("pop ebx\n");
                buffer.write(".IF eax == ebx\n");
                buffer.write("push 1d\n");
                buffer.write(".ELSE\n");
                buffer.write("push 0d\n");
                buffer.write(".ENDIF\n");
                buffer.flush();
            } else {
                throw new SemanticException("error type", getToken(node));
            }

            return boo;
        } else if (node.toString().equals("NotEqualityExpression")) {
            ClassDescription boo = symbolTable.getClassDescription("boolean", "");
            ClassDescription secondClass = expressionScan((SimpleNode) node.jjtGetChild(1), currentBlock);
            ClassDescription firstClass = expressionScan((SimpleNode) node.jjtGetChild(0), currentBlock);
            if (!firstClass.isPrimitive()
                    || !secondClass.isPrimitive()) {
                throw new SemanticException("error type", getToken(node));
            }

            if (((PrimitiveClass) firstClass).isInt() && ((PrimitiveClass) secondClass).isInt()) {

                buffer.write("pop eax\n");
                buffer.write("pop ebx\n");
                buffer.write(".IF eax != ebx\n");
                buffer.write("push 1d\n");
                buffer.write(".ELSE\n");
                buffer.write("push 0d\n");
                buffer.write(".ENDIF\n");
                buffer.flush();
            } else {
                throw new SemanticException("error type", getToken(node));
            }

            return boo;
        } else if (node.toString().equals("LessRelationalExpression")) {
            ClassDescription boo = symbolTable.getClassDescription("boolean", "");
            ClassDescription secondClass = expressionScan((SimpleNode) node.jjtGetChild(1), currentBlock);
            ClassDescription firstClass = expressionScan((SimpleNode) node.jjtGetChild(0), currentBlock);
            if (!firstClass.isPrimitive()
                    || !secondClass.isPrimitive()) {
                throw new SemanticException("error type", getToken(node));
            }

            if (((PrimitiveClass) firstClass).isInt() && ((PrimitiveClass) secondClass).isInt()) {

                buffer.write("pop eax\n");
                buffer.write("pop ebx\n");
                buffer.write(".IF eax < ebx\n");
                buffer.write("push 1d\n");
                buffer.write(".ELSE\n");
                buffer.write("push 0d\n");
                buffer.write(".ENDIF\n");
                buffer.flush();
            } else {
                throw new SemanticException("error type", getToken(node));
            }

            return boo;
        } else if (node.toString().equals("BiggerRelationalExpression")) {
            ClassDescription boo = symbolTable.getClassDescription("boolean", "");
            ClassDescription secondClass = expressionScan((SimpleNode) node.jjtGetChild(1), currentBlock);
            ClassDescription firstClass = expressionScan((SimpleNode) node.jjtGetChild(0), currentBlock);
            if (!firstClass.isPrimitive()
                    || !secondClass.isPrimitive()) {
                throw new SemanticException("error type", getToken(node));
            }

            if (((PrimitiveClass) firstClass).isInt() && ((PrimitiveClass) secondClass).isInt()) {

                buffer.write("pop eax\n");
                buffer.write("pop ebx\n");
                buffer.write(".IF eax > ebx\n");
                buffer.write("push 1d\n");
                buffer.write(".ELSE\n");
                buffer.write("push 0d\n");
                buffer.write(".ENDIF\n");
                buffer.flush();
            } else {
                throw new SemanticException("error type", getToken(node));
            }

            return boo;
        } else if (node.toString().equals("LessEqRelationalExpression")) {
            ClassDescription boo = symbolTable.getClassDescription("boolean", "");
            ClassDescription secondClass = expressionScan((SimpleNode) node.jjtGetChild(1), currentBlock);
            ClassDescription firstClass = expressionScan((SimpleNode) node.jjtGetChild(0), currentBlock);
            if (!firstClass.isPrimitive()
                    || !secondClass.isPrimitive()) {
                throw new SemanticException("error type", getToken(node));
            }

            if (((PrimitiveClass) firstClass).isInt() && ((PrimitiveClass) secondClass).isInt()) {

                buffer.write("pop eax\n");
                buffer.write("pop ebx\n");
                buffer.write(".IF eax <= ebx\n");
                buffer.write("push 1d\n");
                buffer.write(".ELSE\n");
                buffer.write("push 0d\n");
                buffer.write(".ENDIF\n");
                buffer.flush();
            } else {
                throw new SemanticException("error type", getToken(node));
            }

            return boo;
        } else if (node.toString().equals("BiggerEqRelationalExpression")) {
            ClassDescription boo = symbolTable.getClassDescription("boolean", "");
            ClassDescription secondClass = expressionScan((SimpleNode) node.jjtGetChild(1), currentBlock);
            ClassDescription firstClass = expressionScan((SimpleNode) node.jjtGetChild(0), currentBlock);
            if (!firstClass.isPrimitive()
                    || !secondClass.isPrimitive()) {
                throw new SemanticException("error type", getToken(node));
            }

            if (((PrimitiveClass) firstClass).isInt() && ((PrimitiveClass) secondClass).isInt()) {

                buffer.write("pop eax\n");
                buffer.write("pop ebx\n");
                buffer.write(".IF eax >= ebx\n");
                buffer.write("push 1d\n");
                buffer.write(".ELSE\n");
                buffer.write("push 0d\n");
                buffer.write(".ENDIF\n");
                buffer.flush();
            } else {
                throw new SemanticException("error type", getToken(node));
            }

            return boo;
        } else if (node.toString().equals("LeftShiftExpression")) {
            ClassDescription firstClass = expressionScan((SimpleNode) node.jjtGetChild(0), currentBlock);
            ClassDescription secondClass = expressionScan((SimpleNode) node.jjtGetChild(1), currentBlock);
            SimpleNode tmp = node;
            ClassDescription integer = symbolTable.getClassDescription("boolean", "");
            while (tmp.jjtGetToken() == null) {
                tmp = (SimpleNode) tmp.jjtGetChild(0);
            }
            if (!firstClass.equals(integer)
                    || !secondClass.equals(integer)) {
                throw new SemanticException("error type", tmp.jjtGetToken());
            }

            //code

        } else if (node.toString().equals("RightShiftExpression")) {
            ClassDescription firstClass = expressionScan((SimpleNode) node.jjtGetChild(0), currentBlock);
            ClassDescription secondClass = expressionScan((SimpleNode) node.jjtGetChild(1), currentBlock);
            SimpleNode tmp = node;
            ClassDescription integer = symbolTable.getClassDescription("boolean", "");
            while (tmp.jjtGetToken() == null) {
                tmp = (SimpleNode) tmp.jjtGetChild(0);
            }
            if (!firstClass.equals(integer)
                    || !secondClass.equals(integer)) {
                throw new SemanticException("error type", tmp.jjtGetToken());
            }

            //code

        } else if (node.toString().equals("LeftThreeShiftExpression")) {
            ClassDescription firstClass = expressionScan((SimpleNode) node.jjtGetChild(0), currentBlock);
            ClassDescription secondClass = expressionScan((SimpleNode) node.jjtGetChild(1), currentBlock);
            SimpleNode tmp = node;
            ClassDescription integer = symbolTable.getClassDescription("boolean", "");
            while (tmp.jjtGetToken() == null) {
                tmp = (SimpleNode) tmp.jjtGetChild(0);
            }
            if (!firstClass.equals(integer)
                    || !secondClass.equals(integer)) {
                throw new SemanticException("error type", tmp.jjtGetToken());
            }

            //code

        } else if (node.toString().equals("PlusAdditiveExpression")) {
            ClassDescription secondClass = expressionScan((SimpleNode) node.jjtGetChild(1), currentBlock);
            ClassDescription firstClass = expressionScan((SimpleNode) node.jjtGetChild(0), currentBlock);
            if (!firstClass.isPrimitive()
                    || !secondClass.isPrimitive()) {
                throw new SemanticException("error type", getToken(node));
            }

            if (((PrimitiveClass) firstClass).isInt() && ((PrimitiveClass) secondClass).isInt()) {

                buffer.write("pop eax\n");
                buffer.write("pop ebx\n");
                buffer.write("add eax,ebx\n");
                if (firstClass.getSize() == 1) {
                    buffer.write("and eax,000000ffh\n");
                } else if (firstClass.getSize() == 2) {
                    buffer.write("and eax,0000ffffh\n");
                }
                buffer.write("push eax\n");
                buffer.flush();
            } else {
                throw new SemanticException("error type", getToken(node));
            }

            return firstClass;
        } else if (node.toString().equals("MinusAdditiveExpression")) {
            ClassDescription secondClass = expressionScan((SimpleNode) node.jjtGetChild(1), currentBlock);
            ClassDescription firstClass = expressionScan((SimpleNode) node.jjtGetChild(0), currentBlock);
            if (!firstClass.isPrimitive()
                    || !secondClass.isPrimitive()) {
                throw new SemanticException("error type", getToken(node));
            }

            if (((PrimitiveClass) firstClass).isInt() && ((PrimitiveClass) secondClass).isInt()) {

                buffer.write("pop eax\n");
                buffer.write("pop ebx\n");
                buffer.write("sub eax,ebx\n");
                if (firstClass.getSize() == 1) {
                    buffer.write("and eax,000000ffh\n");
                } else if (firstClass.getSize() == 2) {
                    buffer.write("and eax,0000ffffh\n");
                }
                buffer.write("push eax\n");
                buffer.flush();
            } else {
                throw new SemanticException("error type", getToken(node));
            }

            return firstClass;
        } else if (node.toString().equals("MultipleMultiplicativeExpression")) {
            ClassDescription secondClass = expressionScan((SimpleNode) node.jjtGetChild(1), currentBlock);
            ClassDescription firstClass = expressionScan((SimpleNode) node.jjtGetChild(0), currentBlock);

            if (!firstClass.isPrimitive()
                    || !secondClass.isPrimitive()) {
                throw new SemanticException("error type", getToken(node));
            }

            if (((PrimitiveClass) firstClass).isInt() && ((PrimitiveClass) secondClass).isInt()) {

                buffer.write("pop eax\n");
                buffer.write("pop ebx\n");
                buffer.write("mov edx,0d\n");
                buffer.write("mul ebx\n");
                if (firstClass.getSize() == 1) {
                    buffer.write("and eax,000000ffh\n");
                } else if (firstClass.getSize() == 2) {
                    buffer.write("and eax,0000ffffh\n");
                }
                buffer.write("push eax\n");
                buffer.flush();
            } else {
                throw new SemanticException("error type", getToken(node));
            }

            return firstClass;
        } else if (node.toString().equals("DivideMultiplicativeExpression")) {
            ClassDescription secondClass = expressionScan((SimpleNode) node.jjtGetChild(1), currentBlock);
            ClassDescription firstClass = expressionScan((SimpleNode) node.jjtGetChild(0), currentBlock);
            if (!firstClass.isPrimitive()
                    && !secondClass.isPrimitive()) {
                throw new SemanticException("error type", getToken(node));
            }
            if (((PrimitiveClass) firstClass).isInt() && ((PrimitiveClass) secondClass).isInt()) {

                buffer.write("pop eax\n");
                buffer.write("pop ebx\n");
                buffer.write("mov edx,0d\n");
                buffer.write("div ebx\n");
                if (firstClass.getSize() == 1) {
                    buffer.write("and eax,000000ffh\n");
                } else if (firstClass.getSize() == 2) {
                    buffer.write("and eax,0000ffffh\n");
                }
                buffer.write("push eax\n");
                buffer.flush();
            } else {
                throw new SemanticException("error type", getToken(node));
            }
            return firstClass;
        } else if (node.toString().equals("RemainMultiplicativeExpression")) {
            ClassDescription secondClass = expressionScan((SimpleNode) node.jjtGetChild(1), currentBlock);
            ClassDescription firstClass = expressionScan((SimpleNode) node.jjtGetChild(0), currentBlock);

            if (!firstClass.isPrimitive()
                    && !secondClass.isPrimitive()) {
                throw new SemanticException("error type", getToken(node));
            }
            if (((PrimitiveClass) firstClass).isInt()
                    && ((PrimitiveClass) secondClass).isInt()) {
                buffer.write("pop eax\n");
                buffer.write("pop ebx\n");
                buffer.write("mov edx,0d\n");
                buffer.write("div ebx\n");
                if (firstClass.getSize() == 1) {
                    buffer.write("and edx,000000ffh\n");
                } else if (firstClass.getSize() == 2) {
                    buffer.write("and edx,0000ffffh\n");
                }
                buffer.write("push edx\n");
                buffer.flush();
            } else {
                throw new SemanticException("error type", getToken(node));
            }
            return firstClass;
        } else if (node.toString().equals("PredecrementExpression")) {
            ClassDescription firstClass = expressionScan((SimpleNode) node.jjtGetChild(0), currentBlock);

            if (!firstClass.isPrimitive()) {
                throw new SemanticException("error type", getToken(node));
            }

            //code

        } else if (node.toString().equals("PreincrementExpression")) {
            ClassDescription firstClass = expressionScan((SimpleNode) node.jjtGetChild(0), currentBlock);
            SimpleNode tmp = node;
            ClassDescription integer = symbolTable.getClassDescription("int", "");
            while (tmp.jjtGetToken() == null) {
                tmp = (SimpleNode) tmp.jjtGetChild(0);
            }
            if (!firstClass.equals(integer)) {
                throw new SemanticException("error type", tmp.jjtGetToken());
            }

            //code


        } else if (node.toString().equals("OppositeExpression")) {
            ClassDescription firstClass = expressionScan((SimpleNode) node.jjtGetChild(0), currentBlock);
            ClassDescription voi = symbolTable.getClassDescription("void", "");
            if (firstClass.equals(voi)
                    || !firstClass.isPrimitive()) {
                throw new SemanticException("error type", getToken(node));
            }

            buffer.write("pop eax\n");
            buffer.write("not eax\n");
            buffer.write("push eax\n");
            buffer.flush();

            return firstClass;
        } else if (node.toString().equals("NotExpression")) {
            ClassDescription firstClass = expressionScan((SimpleNode) node.jjtGetChild(0), currentBlock);
            ClassDescription boo = symbolTable.getClassDescription("boolean", "");
            ClassDescription voi = symbolTable.getClassDescription("void", "");

            if (!firstClass.isPrimitive() || voi.equals(firstClass)) {
                throw new SemanticException("error type", getToken(node));
            }

            buffer.write("pop eax\n");
            buffer.write(".IF eax != 0\n");
            buffer.write("mov eax,1d\n");
            buffer.write(".ENDIF\n");
            buffer.write("push eax\n");

            return boo;
        } else if (node.toString().equals("PostdecrementExpression")) {
            ClassDescription firstClass = expressionScan((SimpleNode) node.jjtGetChild(0), currentBlock);
            SimpleNode tmp = node;
            ClassDescription integer = symbolTable.getClassDescription("int", "");
            while (tmp.jjtGetToken() == null) {
                tmp = (SimpleNode) tmp.jjtGetChild(0);
            }
            if (!firstClass.equals(integer)) {
                throw new SemanticException("error type", tmp.jjtGetToken());
            }

            //code


        } else if (node.toString().equals("PostincrementExpression")) {
            ClassDescription firstClass = expressionScan((SimpleNode) node.jjtGetChild(0), currentBlock);
            SimpleNode tmp = node;
            ClassDescription integer = symbolTable.getClassDescription("int", "");
            while (tmp.jjtGetToken() == null) {
                tmp = (SimpleNode) tmp.jjtGetChild(0);
            }
            if (!firstClass.equals(integer)) {
                throw new SemanticException("error type", tmp.jjtGetToken());
            }

            //code
        } else {
            //primary
            System.out.println(node.toString());
            if (node.toString().equals("IntegerLiteral")) {
                ClassDescription integer = symbolTable.getClassDescription("int", "");
                buffer.write("mov eax," + node.jjtGetToken().image + "d\n");
                buffer.write("push eax\n");
                buffer.flush();
                return integer;
            } else if (node.toString().equals("BooleanLiteral")) {
                ClassDescription boo = symbolTable.getClassDescription("boolean", "");
                buffer.write("and eax,0h\n");
                if (node.jjtGetToken().image.equals("true")) {
                    buffer.write("add eax,1d\n");
                }
                buffer.write("push eax\n");
                buffer.flush();
                return boo;
            } else if (node.toString().equals("FloatingPointLiteral")) {
                ClassDescription dou = symbolTable.getClassDescription("double", "");
                throw new SemanticException("float not suppor yet", getToken(node));
                //return dou;
            } else if (node.toString().equals("CharacterLiteral")) {
                ClassDescription cha = symbolTable.getClassDescription("char", "");
                String str = node.jjtGetToken().image;
                int c = str.charAt(0);
                buffer.write("mov eax," + c + "d\n");
                buffer.write("push eax\n");
                buffer.flush();
                return cha;
            } else if (node.toString().equals("StringLiteral")) {
                throw new SemanticException("not support yet", node.jjtGetToken());
            } else if (node.toString().equals("NullLiteral")) {
                buffer.write("push 0d\n");
                buffer.flush();
                return null;
            } else if (node.toString().equals("ExpressionPrimaryPrefix")) {
                throw new SemanticException("not support yet", node.jjtGetToken());
                //return expressionScan(node, currentBlock);
            } else if (node.toString().equals("Name")) {
                List<String> nameList = getNameList((SimpleNode) node);
                GetVariableResult varResult = currentBlock.getVariableWithName(nameList);
                if (varResult == null) {
                    throw new SemanticException("variable not found", getToken(node));
                }
                if (varResult == null || !varResult.getType().isPrimitive()) {
                    throw new SemanticException("only primitive can do this", getToken(node));
                }


                buffer.write("mov edx," + varResult.getOffset() + "d\n");
                if (varResult.getPara() != null) {
                    buffer.write("mov ecx," + varResult.getPara().getAssemblyName() + "\n");
                    buffer.write("add ecx,edx\n");
                    buffer.write("mov edx,ecx\n");
                } else {
                    buffer.write("mov ecx,ebp\n");
                    buffer.write("sub ecx,edx\n");
                    buffer.write("mov edx,ecx\n");
                }
                buffer.write("and eax,0d\n");
                if (varResult.getType().getSize() == 1) {
                    buffer.write("mov al,BYTE ptr[edx]\n");
                } else if (varResult.getType().getSize() == 2) {
                    buffer.write("mov ax,WORD ptr[edx]\n");
                } else {
                    buffer.write("mov eax,DWORD ptr [edx]\n");
                }
                buffer.write("push eax\n");
                buffer.flush();

                return varResult.getType();
            } else if (node.toString()
                    .equals("ClassInstanceCreationExpression")) {
                //
            } else if (node.toString()
                    .equals("ArrayPrimarySuffix")) {
                SimpleNode nameNode = (SimpleNode) node.jjtGetChild(0);
                SimpleNode expNode = (SimpleNode) node.jjtGetChild(1);
                List<String> nameList = getNameList((SimpleNode) nameNode);
                GetVariableResult varResult = currentBlock.getVariableWithName(nameList);

                ClassDescription expType = expressionScan(expNode, currentBlock);
                if (varResult == null) {
                    throw new SemanticException("variable not found", getToken(node));
                }
                if (!varResult.getType().isPrimitive()) {
                    throw new SemanticException("only primitive can do this", getToken(node));
                }
                if (!expType.isPrimitive() || !((PrimitiveClass) expType).isInt()) {
                    throw new SemanticException("error type for array", getToken(expNode));
                }


                buffer.write("pop eax\n");
                if (expType.getSize() == 1) {
                    buffer.write("and eax,00000000ffh\n");
                } else if (expType.getSize() == 2) {
                    buffer.write("and eax,0000ffffh\n");
                }
                buffer.write("mov ebx," + varResult.getType().getSize() + "d\n");
                buffer.write("mul ebx\n");
                buffer.write("mov edx,eax\n");
                if (varResult.getPara() != null) {
                    buffer.write("add edx," + varResult.getOffset() + "d\n");
                    buffer.write("mov ecx," + varResult.getPara().getAssemblyName() + "\n");
                    buffer.write("add ecx,edx\n");
                    buffer.write("mov edx,ecx\n");
                } else {
                    buffer.write("mov eax," + varResult.getOffset() + "d\n");
                    buffer.write("sub eax,edx\n");
                    buffer.write("mov edx,eax\n");
                    buffer.write("mov ecx,ebp\n");
                    buffer.write("sub ecx,edx\n");
                    buffer.write("mov edx,ecx\n");
                }
                buffer.write("and eax,0d\n");
                if (varResult.getType().getSize() == 1) {
                    buffer.write("mov al,BYTE ptr[edx]\n");
                } else if (varResult.getType().getSize() == 2) {
                    buffer.write("mov ax,WORD ptr[edx]\n");
                } else {
                    buffer.write("mov eax,DWORD ptr [edx]\n");
                }
                buffer.write("push eax\n");
                buffer.flush();

                return varResult.getType();
            } else if (node.toString()
                    .equals("CallFuntion")) {
                boolean isPrivate = false;
                List<String> nameList = getNameList((SimpleNode) node.jjtGetChild(0));
                List<ClassDescription> varList = new ArrayList();
                String funcName = nameList.get(nameList.size() - 1);
                nameList.remove(nameList.size() - 1);
                if (nameList.size() == 0 || (nameList.size() == 1 && nameList.get(0).equals("this"))) {
                    isPrivate = true;
                }
                GetVariableResult callClassResult = currentBlock.getVariableWithName(nameList);
                varList.add(callClassResult.getType());

                //put this in place
                buffer.write("mov edx," + callClassResult.getOffset() + "d\n");
                if (callClassResult.getPara() != null) {
                    buffer.write("mov ecx," + callClassResult.getPara().getAssemblyName() + "\n");
                    buffer.write("add ecx,edx\n");
                    buffer.write("mov edx,ecx\n");
                } else {
                    buffer.write("mov ecx,ebp\n");
                    buffer.write("sub ecx,edx\n");
                    buffer.write("mov edx,ecx\n");
                }
                buffer.write("mov __PARAFUNC__0,edx\n");
                buffer.flush();

                if (node.jjtGetChild(1).jjtGetNumChildren() > 7) {
                    throw new SemanticException("too many argument", getToken(node));
                }
                int i = 0;
                for (; i < node.jjtGetChild(1).jjtGetNumChildren(); i++) {
                    SimpleNode currentArg = (SimpleNode) node.jjtGetChild(1).jjtGetChild(i);
                    List<String> currentName = getNameList((SimpleNode) currentArg);
                    GetVariableResult currentResult = currentBlock.getVariableWithName(currentName);
                    System.out.println(currentName);
                    if (currentResult == null) {
                        throw new SemanticException("variable not found", getToken(currentArg));
                    }

                    varList.add(currentResult.getType());

                    //set argument in place
                    buffer.write("mov edx," + currentResult.getOffset() + "d\n");
                    if (currentResult.getPara() != null) {
                        buffer.write("mov ecx," + currentResult.getPara().getAssemblyName() + "\n");
                        buffer.write("add ecx,edx\n");
                        buffer.write("mov edx,ecx\n");
                    } else {
                        buffer.write("mov ecx,ebp\n");
                        buffer.write("sub ecx,edx\n");
                        buffer.write("mov edx,ecx\n");
                    }
                    buffer.write("mov __PARAFUNC__" + (i + 1) + ",edx\n");
                    buffer.flush();
                }
                FunctionDescription func = callClassResult.getType().getPublicFunction(funcName, varList);
                if (func == null && isPrivate) {
                    func = callClassResult.getType().getPrivateFunction(funcName, varList);
                }
                if (func == null) {
                    throw new SemanticException("func not found", getToken(node));
                }

                buffer.write("INVOKE " + func.getAssemblyName());
                for (int k = 0; k < i + 1; k++) {
                    buffer.write(",\n __PARAFUNC__" + k);
                }
                buffer.newLine();
                //save the return of func
                buffer.write("push edx\n");
                buffer.flush();

                return func.getReturnType();
            }
        }
        throw new SemanticException("not support yet", getToken(node));
    }

    private ClassDescription getParameterType(SimpleNode e) throws SemanticException {
        ClassDescription result = null;
        if (e.toString().equals("ArrayType")) {
            SimpleNode next = (SimpleNode) e.jjtGetChild(0);
            return getParameterType(next);
        } else if (e.toString().equals("Type")) {
            result = getParameterTypeWithoutArray((ASTType) e);
            if (result == null) {
                throw new SemanticException("type error", e.jjtGetToken());
            }
            return result;
        } else {
            return null;
        }
    }

    private ClassDescription getParameterTypeWithoutArray(ASTType node) throws SemanticException {
        ClassDescription result = null;
        String typeName = node.jjtGetToken().image;
        for (int i = 0; i < packageScan.size(); i++) {
            result = symbolTable.getClassDescription(typeName, packageScan.get(i));
            if (result != null) {
                return result;
            }
        }
        if (result == null) {
            throw new SemanticException("type not found", node.jjtGetToken());
        }

        return result;
    }

    private Token getToken(SimpleNode node) {
        SimpleNode currentNode = node;
        while (currentNode.jjtGetToken() == null) {
            currentNode = (SimpleNode) currentNode.jjtGetChild(0);
        }
        return currentNode.jjtGetToken();
    }

    List<String> getNameList(SimpleNode node) {
        List<String> nameList = new ArrayList();
        if (node.toString().equals("ThisPrimaryPrefix")) {
            nameList.add("this");
            if (node.jjtGetNumChildren() > 0) {
                node = (ASTName) (SimpleNode) node.jjtGetChild(0);
            }
        }
        while (node.jjtGetNumChildren() > 0) {
            nameList.add(((SimpleNode) node.jjtGetChild(0)).jjtGetToken().image);
            if (node.jjtGetNumChildren() > 1) {
                node = (ASTName) (SimpleNode) node.jjtGetChild(1);
            } else {
                break;
            }
        }
        return nameList;
    }
}
