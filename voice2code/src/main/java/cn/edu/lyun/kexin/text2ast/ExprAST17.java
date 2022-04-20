package cn.edu.lyun.kexin.text2ast;

import com.github.javaparser.ast.*;
import cn.edu.lyun.kexin.text2pattern.pattern.Pattern;
import com.github.javaparser.ast.expr.LambdaExpr;

// expression? lambda expression
public class ExprAST17 implements AST {
	public Node generate(Pattern pattern) {
    LambdaExpr lambdaExpr = new LambdaExpr();
    lambdaExpr.setEnclosingParameters(true);
    return lambdaExpr;
	}
}