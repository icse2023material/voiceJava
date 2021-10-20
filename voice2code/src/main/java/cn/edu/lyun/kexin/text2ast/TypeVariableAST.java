package cn.edu.lyun.kexin.text2ast;

import cn.edu.lyun.kexin.text2pattern.pattern.Pattern;
import cn.edu.lyun.kexin.text2pattern.pattern.Unit;
import cn.edu.lyun.util.Pair;
import cn.edu.lyun.util.ListHelper;
import java.util.*;
import com.github.javaparser.ast.*;
import com.github.javaparser.ast.body.Parameter;
import com.github.javaparser.ast.expr.SimpleName;
import com.github.javaparser.ast.type.Type;

public class TypeVariableAST implements AST {

	public Node generate(Pattern pattern) {
		Unit[] units = pattern.getUnits();
		List<Unit> unitList = new ArrayList<Unit>(Arrays.asList(units));
		unitList.remove(0); // remove "type"

		Pair<List<Unit>, List<Unit>> pair = new ListHelper().splitList(unitList, "variable");
		List<Unit> typeList = pair.getFirst();
		List<Unit> variableNameList = pair.getSecond();

		Type type = new TypeAST().generateType(typeList);

		Parameter parameter = new Parameter();
		parameter.setName(new SimpleName(variableNameList.get(0).getKeyword()));
		parameter.setType(type);

		return parameter;
	}
}
