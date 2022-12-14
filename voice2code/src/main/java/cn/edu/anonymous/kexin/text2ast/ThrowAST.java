package cn.edu.anonymous.kexin.text2ast;

import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.*;
import com.github.javaparser.ast.stmt.ThrowStmt;
import com.github.javaparser.ast.expr.ObjectCreationExpr;

import java.util.*;
import com.github.javaparser.ast.type.ClassOrInterfaceType;

import cn.edu.anonymous.kexin.text2pattern.pattern.Pattern;
import cn.edu.anonymous.kexin.text2pattern.pattern.Unit;

public class ThrowAST implements AST {
	public Node generate(Pattern pattern) {
		Unit[] units = pattern.getUnits();
		List<Unit> unitList = new ArrayList<Unit>(Arrays.asList(units));
		unitList.remove(0);
		unitList.remove(0);
		ThrowStmt stmt = new ThrowStmt();
		// ClassExpr expr = new ClassExpr();
		ClassOrInterfaceType type = StaticJavaParser.parseClassOrInterfaceType(unitList.get(0).getKeyword());
		// NameExpr expr = new NameExpr(unitList.get(0).getKeyword());
		ObjectCreationExpr expr = new ObjectCreationExpr();
		expr.setType(type);
		stmt.setExpression(expr);
		return stmt;
	}

}
