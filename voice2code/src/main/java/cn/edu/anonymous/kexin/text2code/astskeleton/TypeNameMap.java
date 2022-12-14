package cn.edu.anonymous.kexin.text2code.astskeleton;

import java.util.HashMap;

public class TypeNameMap {

	public static HashMap<HoleType, String> map = createMap();

	private static HashMap<HoleType, String> createMap() {
		HashMap<HoleType, String> map = new HashMap<HoleType, String>();
		map.put(HoleType.PackageDeclaration, "getPackageDeclaration");
		map.put(HoleType.ImportDeclarations, "getImports"); // get all imports
		map.put(HoleType.TypeDeclarations, "getTypes");
		map.put(HoleType.BodyDeclarations, "getMembers");
		map.put(HoleType.FieldDeclarations, "getFields");
		map.put(HoleType.MethodDeclaration, "getMethods");
		map.put(HoleType.Parameters, "getParameters");
		map.put(HoleType.Parameter, "getParameter");
		map.put(HoleType.VariableDeclarators, "getVariables");
		map.put(HoleType.Type, "getType");
		map.put(HoleType.Body, "getBody");
		map.put(HoleType.Statements, "getStatements");
		map.put(HoleType.ForInitialization, "getInitialization");
		map.put(HoleType.ThenStatement, "getThenStmt");
		map.put(HoleType.ElseStatement, "getElseStmt");
		map.put(HoleType.SwitchEntries, "getEntries");
		map.put(HoleType.Expression, "getExpression");
		map.put(HoleType.LeftSubExpr, "getLeft");
		map.put(HoleType.RightSubExpr, "getRight");
		map.put(HoleType.AssignExprTarget, "getTarget");
		map.put(HoleType.AssignExprValue, "getValue");
		map.put(HoleType.VariableInitializer, "getInitializer");
		map.put(HoleType.Arguments, "getArguments");
		map.put(HoleType.ForCompare, "getCompare");
		map.put(HoleType.InnerExpr, "getInner");
		map.put(HoleType.WhileCondition, "getCondition");
		map.put(HoleType.IfCondition, "getCondition");
    map.put(HoleType.ConditionalExprCondition, "getCondition");
    map.put(HoleType.ConditionalExprThen, "getThenExpr");
    map.put(HoleType.ConditionalExprElse, "getElseExpr");
		map.put(HoleType.SwitchSelector, "getSelector");
		map.put(HoleType.SwitchEntryLabels, "getLabels");
    map.put(HoleType.TypeParameters, "getTypeParameters");
    map.put(HoleType.ExtendedTypes, "getExtendedTypes");
    map.put(HoleType.ImplementedTypes, "getImplementedTypes");
    map.put(HoleType.ThrownExceptions, "getThrownExceptions");
    map.put(HoleType.TryBlock, "getTryBlock");
    map.put(HoleType.CatchClauses, "getCatchClauses");
    map.put(HoleType.TypeArguments, "getTypeArguments");
		return map;
	}
}
