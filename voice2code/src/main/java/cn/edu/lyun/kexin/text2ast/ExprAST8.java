package cn.edu.lyun.kexin.text2ast;

import com.github.javaparser.ast.*;
import com.github.javaparser.ast.expr.NameExpr;
import com.github.javaparser.ast.expr.SimpleName;
import com.github.javaparser.ast.expr.UnaryExpr;

import java.util.*;
import cn.edu.lyun.kexin.text2pattern.pattern.Pattern;
import cn.edu.lyun.kexin.text2pattern.pattern.Unit;

// [expression]? plus plus _
public class ExprAST8 implements AST {
	public Node generate(Pattern pattern) {
		Unit[] units = pattern.getUnits();
		List<Unit> unitList = new ArrayList<Unit>(Arrays.asList(units));
		if (unitList.get(0).getKeyword().equals("expression")) {
			unitList.remove(0); // remove "expression"
		}
		unitList.remove(0);
		unitList.remove(0);

		return new UnaryExpr(new NameExpr(new SimpleName(unitList.get(0).getKeyword())),
				UnaryExpr.Operator.PREFIX_INCREMENT);
	}
}
