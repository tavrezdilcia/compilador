package com.utesa.compilador.traductores;

import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableMap;

import com.utesa.compilador.entities.ArrayAggregate;
import com.utesa.compilador.entities.AssignmentStatement;
import com.utesa.compilador.entities.Block;
import com.utesa.compilador.entities.BooleanLiteral;
import com.utesa.compilador.entities.BreakStatement;
import com.utesa.compilador.entities.CallExpression;
import com.utesa.compilador.entities.CallStatement;
import com.utesa.compilador.entities.Case;
import com.utesa.compilador.entities.CharLiteral;
import com.utesa.compilador.entities.ClassicForStatement;
import com.utesa.compilador.entities.Declaration;
import com.utesa.compilador.entities.DottedVariable;
import com.utesa.compilador.entities.EmptyArray;
import com.utesa.compilador.entities.Entity;
import com.utesa.compilador.entities.Expression;
import com.utesa.compilador.entities.Function;
import com.utesa.compilador.entities.IfStatement;
import com.utesa.compilador.entities.IncrementStatement;
import com.utesa.compilador.entities.InfixExpression;
import com.utesa.compilador.entities.IntegerLiteral;
import com.utesa.compilador.entities.NullLiteral;
import com.utesa.compilador.entities.PostfixExpression;
import com.utesa.compilador.entities.PrefixExpression;
import com.utesa.compilador.entities.PrintStatement;
import com.utesa.compilador.entities.Program;
import com.utesa.compilador.entities.RealLiteral;
import com.utesa.compilador.entities.ReturnStatement;
import com.utesa.compilador.entities.SimpleVariableReference;
import com.utesa.compilador.entities.Statement;
import com.utesa.compilador.entities.StringLiteral;
import com.utesa.compilador.entities.StructAggregate;
import com.utesa.compilador.entities.StructField;
import com.utesa.compilador.entities.StructType;
import com.utesa.compilador.entities.SubscriptedVariable;
import com.utesa.compilador.entities.Type;
import com.utesa.compilador.entities.Variable;
import com.utesa.compilador.entities.VariableExpression;
import com.utesa.compilador.entities.WhileStatement;
import java.util.HashMap;
import java.util.Map;
import javax.swing.JOptionPane;

/**
 * A translator from MyCode semantic graphs to JavaScript.
 */
public class MyCodeToJavaScriptTranslator {

    private PrintWriter writer;
    private int indentPadding = 4;
    private int indentLevel = 0;

//    private ImmutableMap<Function, String> builtIns = ImmutableMap.<Function, String>builder()
//        .put(Function.ATAN, "Math.atan2")
//        .put(Function.COS, "Math.cos")
//        .put(Function.LN, "Math.log")
//        .put(Function.SIN, "Math.sin")
//        .put(Function.SQRT, "Math.sqrt")
//        .build();
//
//    private ImmutableMap<Type, String> initialValues = ImmutableMap.<Type, String>builder()
//        .put(Type.BOOLEAN, "false")
//        .put(Type.CHAR, "'\\0'")
//        .put(Type.INT, "0")
//        .put(Type.REAL, "0.0")
//        .put(Type.STRING, "\"\"")
//        .build();
    
    private Map<Function, String> builtIns;
    private Map<Type, String> initialValues;
    
    public MyCodeToJavaScriptTranslator() {
        
        this.builtIns = new HashMap<Function, String>();
                this.builtIns.put(Function.ATAN, "Math.atan2");
                this.builtIns.put(Function.COS, "Math.cos");
                this.builtIns.put(Function.LN, "Math.log");
                this.builtIns.put(Function.SIN, "Math.sin");
                this.builtIns.put(Function.SQRT, "Math.sqrt");
                
        this.initialValues = new HashMap<Type, String>();
            this.initialValues.put(Type.BOOLEAN, "false");
            this.initialValues.put(Type.CHAR, "'\\0'");
            this.initialValues.put(Type.INT, "0");
            this.initialValues.put(Type.REAL, "0.0");
            this.initialValues.put(Type.STRING, "\"\"");
   
    }

    public void translateProgram(Program program, PrintWriter writer) {
        
        this.writer = writer;
        
        emit("(function () {");
        
        translateBlock(program);
        
        emit("}());");
    }
//
    private void translateBlock(Block block) {
        
        indentLevel++;
        for (Statement s: block.getStatements()) {
            
            translateStatement(s);
        }
        indentLevel--;
    }
//
    private void translateStatement(Statement s) {
        
        

        if (s instanceof Declaration) {
            translateDeclaration(Declaration.class.cast(s));

        } else if (s instanceof AssignmentStatement) {
            translateAssignmentStatement(AssignmentStatement.class.cast(s));

        } else if (s instanceof IncrementStatement) {
            translateIncrementStatement(IncrementStatement.class.cast(s));

        } else if (s instanceof CallStatement) {
            translateCallStatement(CallStatement.class.cast(s));

        } else if (s instanceof BreakStatement) {
            emit("break;");

        } else if (s instanceof ReturnStatement) {
            translateReturnStatement(ReturnStatement.class.cast(s));

        } else if (s instanceof PrintStatement) {
            translatePrintStatement(PrintStatement.class.cast(s));

        } else if (s instanceof IfStatement) {
            translateIfStatement(IfStatement.class.cast(s));

        } else if (s instanceof WhileStatement) {
            translateWhileStatement(WhileStatement.class.cast(s));

        } else if (s instanceof ClassicForStatement) {
            translateClassicForStatement(ClassicForStatement.class.cast(s));

        } else {
            throw new RuntimeException("Unknown statement class: " + s.getClass().getName());
        }
    }

    private void translateDeclaration(Declaration s) {
        
        
        if (s.getDeclarable() instanceof Variable) {
            
            translateVariableDeclaration(Variable.class.cast(s.getDeclarable()));
        } else if (s.getDeclarable() instanceof Function) {
            
            translateFunctionDeclaration(Function.class.cast(s.getDeclarable()));
        } else if (s.getDeclarable() instanceof Type) {
            
            // Intentionally empty; type declarations do not get translated in JavaScript
        } else {
            
            throw new RuntimeException("Unknown declaration: " + s.getClass().getName());
        }
    }

    private void translateVariableDeclaration(Variable v) {
        String initializer;
        if (v.getInitializer() == null) {
            initializer = initialValues.get(v.getType());
            if (initializer == null) {
                initializer = "null";
            }
        } else {
            initializer = translateExpression(v.getInitializer());
        }
        emit ("var %s = %s;", v.getName(), initializer);
    }

    private void translateFunctionDeclaration(Function f) {
        
        emit("function %s(%s) {", f.getName(), translateParameters(f.getParameters()));
        
        translateBlock(f.getBody());
        
        emit("}");
    }

    private void translateAssignmentStatement(AssignmentStatement s) {
        emit("%s = %s;", translateExpression(s.getLeft()), translateExpression(s.getRight()));
    }

    private void translateIncrementStatement(IncrementStatement s) {
        emit("%s%s;", translateExpression(s.getTarget()), s.getOp());
    }

    private void translateCallStatement(CallStatement s) {
        emit("%s(%s);", s.getFunctionName(), translateExpressionList(s.getArgs()));
    }

    private void translateReturnStatement(ReturnStatement s) {
        if (s.getReturnExpression() == null) {
            emit("return;");
        } else {
            emit("return %s;", translateExpression(s.getReturnExpression()));
        }
    }

    private void translatePrintStatement(PrintStatement s) {
        for (Expression e: s.getArgs()) {
            emit("console.log(%s);", translateExpression(e));
        }
    }

    private void translateIfStatement(IfStatement s) {
        String lead = "if";
        for (Case c: s.getCases()) {
            emit("%s (%s) {", lead, translateExpression(c.getCondition()));
            translateBlock(c.getBody());
            lead = "} else if";
        }
        if (s.getElsePart() != null) {
            if (s.getCases().isEmpty()) {
                // If and else-ifs were all optimized away!  Just do the else and get out.
                for (Statement statement: s.getElsePart().getStatements()) {
                    translateStatement(statement);
                }
                return;
            } else {
                emit("} else {");
                translateBlock(s.getElsePart());
            }
        }
        emit("}");
    }

    private void translateWhileStatement(WhileStatement s) {
        emit("while (%s) {", translateExpression(s.getCondition()));
        translateBlock(s.getBody());
        emit("}");
    }

    private void translateClassicForStatement(ClassicForStatement s) {
        String init = "", test = "", each = "";
        if (s.getInit() != null) {
            init = String.format("var %s = %s", s.getIndex(), s.getInit());
        }
        if (s.getTest() != null) {
            test = translateExpression(s.getTest());
        }
        if (s.getEach() instanceof AssignmentStatement) {
            AssignmentStatement e = AssignmentStatement.class.cast(s.getEach());
            String left = translateExpression(e.getLeft());
            String right = translateExpression(e.getRight());
            each = String.format("%s = %s", left, right);
        } else if (s.getEach() instanceof IncrementStatement) {
            IncrementStatement e = IncrementStatement.class.cast(s.getEach());
            each = String.format("%s%s", e.getOp(), e.getOp());
        }
        emit("for (%s; %s; %s) {", init, test, each);
        translateBlock(s.getBody());
        emit("}");
    }

    private String translateExpression(Expression e) {
        if (e instanceof IntegerLiteral) {
            return IntegerLiteral.class.cast(e).getValue().toString();
        } else if (e instanceof CharLiteral) {
            return CharLiteral.class.cast(e).getValue().toString();
        } else if (e instanceof RealLiteral) {
            return RealLiteral.class.cast(e).getValue().toString();
        } else if (e instanceof NullLiteral) {
            return "null";
        } else if (e == BooleanLiteral.TRUE) {
            return "true";
        } else if (e == BooleanLiteral.FALSE) {
            return "false";
        } else if (e instanceof StringLiteral) {
            return translateStringLiteral(StringLiteral.class.cast(e));
        } else if (e instanceof ArrayAggregate) {
            return translateArrayAggregate(ArrayAggregate.class.cast(e));
        } else if (e instanceof StructAggregate) {
            return translateStructAggregate(StructAggregate.class.cast(e));
        } else if (e instanceof EmptyArray) {
            return translateEmptyArray(EmptyArray.class.cast(e));
        } else if (e instanceof PrefixExpression) {
            return translatePrefixExpression(PrefixExpression.class.cast(e));
        } else if (e instanceof PostfixExpression) {
            return translatePostfixExpression(PostfixExpression.class.cast(e));
        } else if (e instanceof InfixExpression) {
            return translateInfixExpression(InfixExpression.class.cast(e));
        } else if (e instanceof VariableExpression) {
            return translateVariableExpression(VariableExpression.class.cast(e));
        } else {
            throw new RuntimeException("Unknown entity class: " + e.getClass().getName());
        }
    }

    private String translateStringLiteral(StringLiteral s) {
        StringBuilder result = new StringBuilder("\"");
        for (int codepoint: s.getValues()) {
            if (isDisplayable(codepoint)) {
                result.append((char)codepoint);
            } else {
                for (char c: Character.toChars(codepoint)) {
                    result.append(String.format("\\u%04x", (int)c));
                }
            }
        }
        result.append("\"");
        return result.toString();
    }

    private String translatePrefixExpression(PrefixExpression e) {
        String op = e.getOp();
        String operand = translateExpression(e.getOperand());
        if ("!~-".indexOf(op) >= 0 || "++".equals(op) || "--".equals(op)) {
            return String.format("%s%s", op, operand);
        } else if ("string".equals(e.getOp())) {
            return String.format("JSON.stringify(%s)", operand);
        } else if ("length".equals(op)) {
            return String.format("(%s).length", operand);
        } else if ("int".equals(op) || "char".equals(op)) {
            return operand;
        } else {
            throw new RuntimeException("Unknown prefix operator: " + e.getOp());
        }
    }

    private String translatePostfixExpression(PostfixExpression e) {
        String op = e.getOp();
        String operand = translateExpression(e.getOperand());
        if ("++".equals(op) || "--".equals(op)) {
            return String.format("%s%s", operand, op);
        } else {
            throw new RuntimeException("Unknown postfix operator: " + e.getOp());
        }
    }

    private String translateInfixExpression(InfixExpression e) {
        // All Carlos binary operators look exactly the same as their JavaScript counterparts!
        String left = translateExpression(e.getLeft());
        String right = translateExpression(e.getRight());
        return String.format("(%s %s %s)", left, e.getOp(), right);
    }

    private String translateEmptyArray(EmptyArray e) {
        return String.format("Array()", translateExpression(e.getBound()));
    }

    private String translateArrayAggregate(ArrayAggregate e) {
        List<String> expressions = new ArrayList<String>();
        String join="";
        for (Expression arg : e.getArgs()) {
            join=join+translateExpression(arg)+", ";
            expressions.add(translateExpression(arg));
        }
        return "["+join+"]"; //"[" + Joiner.on(", ").join(expressions) + "]";
    }

    private String translateStructAggregate(StructAggregate e) {
        Iterator<StructField> fields = StructType.class.cast(e.getType()).getFields().iterator();
        Iterator<Expression> values = e.getArgs().iterator();
        List<String> pairs = new ArrayList<String>();
        String join="";
        while (fields.hasNext() && values.hasNext()) {
            join=join+property(fields.next().getName()) + ": " + translateExpression(values.next())+", ";
            pairs.add(property(fields.next().getName()) + ": " + translateExpression(values.next()));
        }
        return "{"+join+"}";//"{" + Joiner.on(", ").join(pairs) + "}";
    }

    private String translateVariableExpression(VariableExpression v) {
        if (v instanceof SimpleVariableReference) {
            return SimpleVariableReference.class.cast(v).getReferent().getName();
        } else if (v instanceof SubscriptedVariable) {
            return translateSubscriptedVariable(SubscriptedVariable.class.cast(v));
        } else if (v instanceof DottedVariable) {
            return translateDottedVariable(DottedVariable.class.cast(v));
        } else if (v instanceof CallExpression) {
            return translateCallExpression(CallExpression.class.cast(v));
        } else {
            throw new RuntimeException("Unknown variable expression class: " + v.getClass().getName());
        }
    }

    private String translateSubscriptedVariable(SubscriptedVariable v) {
        String sequence = translateVariableExpression(v.getSequence());
        String index = translateExpression(v.getIndex());
        return String.format("%s[%s]", sequence, index);
    }

    private String translateDottedVariable(DottedVariable v) {
        String struct = translateVariableExpression(v.getStruct());
        String fieldName = property(v.getFieldName());
        return String.format("%s[%s]", struct, fieldName);
    }

    private String translateCallExpression(CallExpression e) {

        if (Function.PI.equals(e.getFunction())) {
            return "Math.PI";
        } else if (Function.SUBSTRING.equals(e.getFunction())) {
            return String.format("(%s).substring(%s, %s)",
                translateExpression(e.getArgs().get(0)),
                translateExpression(e.getArgs().get(1)),
                translateExpression(e.getArgs().get(2)));
        } else if (Function.GET_STRING.equals(e.getFunction())) {
            return "fs.readFileSync('/dev/stdin')";
        }

        String function = e.getFunctionName();
        String args = translateExpressionList(e.getArgs());
        if (builtIns.containsKey(e.getFunction())) {
            function = builtIns.get(e.getFunction());
        }
        return String.format("%s(%s)", function, args);
    }

    private String translateExpressionList(List<Expression> list) {
        List<String> expressions = new ArrayList<String>();
        String join="";
        for (Expression e : list) {
            join=join+translateExpression(e)+", ";
            expressions.add(translateExpression(e));
        }
        return join;//Joiner.on(", ").join(expressions);
    }

    private String translateParameters(List<Variable> list) {
        
        List<String> names = new ArrayList<String>();
        String parametros="";
        
        for (Variable v : list) {
            
            parametros = parametros + v.getName()+", ";
            names.add(v.getName());
        }
        
        
        return parametros;//Joiner.on(", ").join(names);
    }

    private String property(String s) {
        StringBuilder result = new StringBuilder("\"");

        // Both Java and JavaScript use UTF-16, so this is pretty easy
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (isDisplayable(c)) {
                result.append(c);
            } else {
                result.append(String.format("\\u%04x", (int)c));
            }
        }
        result.append("\"");
        return result.toString();
    }

    private String variable(Entity e) {
        return String.format("_v%d", e.getId());
    }

    /**
     * Returns whether or not we should show a particular character in the JavaScript output.
     * We only show characters we are guaranteed to see, that is, the non-control ASCII
     * characters, except the double quote character itself, since we are always going to
     * render string literals and property names inside double quotes.
     */
    private boolean isDisplayable(int c) {
        return 20 <= c && c <= 126 && c != '"';
    }

    private void emit(String line, Object... args) {
        
        int pad = indentPadding * indentLevel;

        if (args.length != 0) {
            line = String.format(line, args);
        }

        // printf does not allow "%0s" as a format specifier, darn it.
        if (pad == 0) {
            
            writer.println(line);
            
        } else {
            
            writer.printf("%" + pad + "s%s\n", "", line);
            
        }
    }
}
