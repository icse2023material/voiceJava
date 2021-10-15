package cn.edu.lyun.kexin.text2ast;

import com.github.javaparser.ast.*;
import java.util.*;
import cn.edu.lyun.kexin.text2pattern.pattern.Pattern;
import cn.edu.lyun.kexin.text2pattern.pattern.Unit;
import cn.edu.lyun.util.*;
import com.github.javaparser.ast.expr.AssignExpr;
import com.github.javaparser.ast.expr.Expression;

// let _ [dot _]? equal (int | byte | short | long | char | float | double | boolean | String) _ 
public class LetStmtAST5 implements AST {
	public Node generate(Pattern pattern) {
		Unit[] units = pattern.getUnits();
		List<Unit> unitList = new ArrayList<Unit>(Arrays.asList(units));
		unitList.remove(0); // remove "let"
		Pair<List<Unit>, List<Unit>> pair = new ListHelper().splitList(unitList, "equal");
		List<Unit> first = pair.getFirst();
		AssignExpr assignExpr = new AssignExpr();

		Expression left = new FieldAccessAST().generate(first);
		assignExpr.setTarget(left);

		List<Unit> second = pair.getSecond();
		Expression right = PrimitiveTypeAST.generatePrimiviteExpr(second);
		assignExpr.setValue(right);
		return assignExpr;
	}

}