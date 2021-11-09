package cn.edu.lyun.kexin.text2ast;

import com.github.javaparser.ast.*;
import com.github.javaparser.ast.expr.StringLiteralExpr;

import java.util.*;
import cn.edu.lyun.kexin.text2pattern.pattern.Pattern;
import cn.edu.lyun.kexin.text2pattern.pattern.Unit;

// [expression]? string hello world
public class ExprAST14 implements AST {
	public Node generate(Pattern pattern) {
		Unit[] units = pattern.getUnits();
		List<Unit> unitList = new ArrayList<Unit>(Arrays.asList(units));
		if (unitList.get(0).getKeyword().equals("expression")) {
			unitList.remove(0); // remove "expression"
		}
		if (unitList.get(0).getKeyword().equals("string")) {
			unitList.remove(0); // remove "string"
		}
		String value = "";
		for (Unit unit : unitList) {
			value += " " + unit.getKeyword();
		}
		return new StringLiteralExpr(value.strip());
	}
}