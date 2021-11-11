package cn.edu.lyun.kexin.text2code;

import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.*;

import cn.edu.lyun.util.Pair;
import cn.edu.lyun.util.StringHelper;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.ImportDeclaration;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.PackageDeclaration;
import com.github.javaparser.ast.body.BodyDeclaration;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.FieldDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.body.Parameter;
import com.github.javaparser.ast.type.Type;
import com.github.javaparser.ast.body.VariableDeclarator;
import com.github.javaparser.ast.expr.AssignExpr;
import com.github.javaparser.ast.expr.BinaryExpr;
import com.github.javaparser.ast.expr.BooleanLiteralExpr;
import com.github.javaparser.ast.expr.EnclosedExpr;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.ObjectCreationExpr;
import com.github.javaparser.ast.expr.VariableDeclarationExpr;
import com.github.javaparser.ast.stmt.BlockStmt;
import com.github.javaparser.ast.stmt.ExpressionStmt;
import com.github.javaparser.ast.stmt.ForStmt;
import com.github.javaparser.ast.stmt.IfStmt;
import com.github.javaparser.ast.stmt.ReturnStmt;
import com.github.javaparser.ast.stmt.Statement;
import com.github.javaparser.ast.stmt.SwitchEntry;
import com.github.javaparser.ast.stmt.SwitchStmt;
import com.github.javaparser.ast.stmt.WhileStmt;
import com.github.javaparser.printer.lexicalpreservation.LexicalPreservingPrinter;

import cn.edu.lyun.kexin.text2ast.ASTManager;
import cn.edu.lyun.kexin.text2ast.FieldAST;
import cn.edu.lyun.kexin.text2code.astskeleton.HoleAST;
import cn.edu.lyun.kexin.text2code.astskeleton.HoleNode;
import cn.edu.lyun.kexin.text2code.astskeleton.HoleType;
import cn.edu.lyun.kexin.text2code.astskeleton.TypeNameMap;
import cn.edu.lyun.kexin.text2pattern.nfa.RegexSet;
import cn.edu.lyun.kexin.text2pattern.pattern.Pattern;
import cn.edu.lyun.kexin.text2pattern.pattern.PatternSet;

import io.vavr.control.Either;

public class Text2CompilationUnit {

	private CompilationUnit compilationUnit;
	private HoleAST holeAST;
	private boolean isDebug;

	public Text2CompilationUnit() {
		this.compilationUnit = new CompilationUnit();
		this.holeAST = new HoleAST();
		this.isDebug = false;
	}

	public HoleAST getHoleAST() {
		return this.holeAST;
	}

	public Node getCompilationUnit() {
		return this.compilationUnit;
	}

	public void generatePNGofHoleAST() {
		this.holeAST.generateDotAndPNGOfHoleAST();
	}

	public CompilationUnit generate(String text) {
		Pattern pattern = RegexSet.compile(new PatternSet()).matchPattern(text);
		Node node = ASTManager.generate(pattern);
		if (this.isDebug) {
			System.out.println("[log] matched pattern name: " + pattern.getName());
		}

		Pair<Pair<HoleNode, HoleNode>, List<Integer>> holePosition = this.holeAST.getCurrentHole();
		List<Integer> path = holePosition.getSecond();
		Pair<Either<Node, Either<List<?>, NodeList<?>>>, Integer> parentAndIndex = this.getParentOfHole(path);
		int holeIndex = parentAndIndex.getSecond();
		Pair<HoleNode, HoleNode> parentAndCurrentHole = holePosition.getFirst();
		HoleNode parentHole = parentAndCurrentHole.getFirst();
		HoleNode currentHole = parentAndCurrentHole.getSecond();
		HoleType parentHoleType = parentHole.getHoleType();
		Either<Node, Either<List<?>, NodeList<?>>> parent = parentAndIndex.getFirst();
		HoleNode parentOfParentHole = this.holeAST.getParentOfNode(path);
		HoleNode parentOfParentOfParentHole = this.holeAST.getParentOfParentOfParentNode(path);

		// the following code can be optimized.
		String parentNodeClassStr = null;
		if (parent.isLeft()) {
			parentNodeClassStr = StringHelper.getClassName(parent.getLeft().getClass().toString());
		}

		switch (pattern.getName()) {
		case "moveNext":
			HoleNode holeNode = new HoleNode(HoleType.Undefined, true);
			if (currentHole.getHoleTypeOfOptionsIfOnlyOne() != null) {
				HoleType holeType = currentHole.getHoleTypeOfOptionsIfOnlyOne();
				if (holeType.equals(HoleType.TypeVariables)) {
					currentHole.set(HoleType.TypeVariables, false);
					parentHole.addChild(holeNode);
				} else if (holeType.equals(HoleType.ForInitialization)) {
					currentHole.set(HoleType.ForInitialization, false);
					holeNode.setHoleTypeOptionsOfOnlyOne(HoleType.ForCompare);
					parentHole.addChild(holeNode);
				} else if (holeType.equals(HoleType.ForCompare)) {
					ForStmt forStmt = (ForStmt) parent.getLeft();
					forStmt.setCompare(new BooleanLiteralExpr(true));
					currentHole.set(HoleType.ForCompare, false);
					holeNode.setHoleTypeOptionsOfOnlyOne(HoleType.ForExpression);
					parentHole.addChild(holeNode);
				} else if (holeType.equals(HoleType.ForExpression)) {
					currentHole.set(HoleType.ForExpression, false);
					parentHole.addChild(holeNode);
				} else {
					// TODO: small step move. Not syntax-directed.
					parentHole.deleteHole(holeIndex);
					parentOfParentHole.addChild(holeNode);
				}
			} else if (parentHoleType.equals(HoleType.SwitchEntries)) {
				HoleNode elderBrother = parentHole.getIthChild(holeIndex - 1);
				// Not has default case yet, then add a default case
				if (elderBrother.getIthChild(0).getHoleType().equals(HoleType.Expression)) {
					// generate SwitchEntry Sketch
					NodeList<SwitchEntry> switchEntries = (NodeList<SwitchEntry>) parent.get().get();
					SwitchEntry switchEntry = new SwitchEntry();
					switchEntries.add(switchEntry);

					currentHole.set(HoleType.Wrapper, false);
					currentHole.setHoleTypeOptions(new HoleType[] { HoleType.SwitchEntry });

					holeNode = new HoleNode(HoleType.Wrapper, false);
					holeNode.setHoleTypeOptionsOfOnlyOne(HoleType.MoveNext);
					currentHole.addChild(holeNode);

					currentHole.addChild(new HoleNode());
				} else {
					parentHole.deleteHole(holeIndex);
					parentOfParentHole.addChild(holeNode);
				}
			} else if (parentHoleType.equals(HoleType.ElseStatement) || (parentHole.getHoleTypeOfOptionsIfOnlyOne() != null
					&& parentHole.getHoleTypeOfOptionsIfOnlyOne().equals(HoleType.IfStmt))) {
				// default else case
				BlockStmt blockStmt = new BlockStmt();
				IfStmt ifStmt = (IfStmt) parent.getLeft();
				ifStmt.setElseStmt(blockStmt);

				currentHole.set(HoleType.ElseStatement, false);
				HoleNode stmtsHole = new HoleNode();
				stmtsHole.set(HoleType.Statements, false);
				currentHole.addChild(stmtsHole);
				stmtsHole.addChild(new HoleNode());
			} else {
				// TODO: small step move. Not syntax-directed.
				parentHole.deleteHole(holeIndex);
				parentOfParentHole.addChild(holeNode);
			}
			break;
		case "package":
			CompilationUnit parentNode = null;
			if (parent.isLeft()) {
				parentNode = (CompilationUnit) parent.getLeft();
			} else {
				// TODO: shall not be other case.
			}
			parentNode.setPackageDeclaration((PackageDeclaration) node);
			// update current hole
			currentHole.set(HoleType.PackageDeclaration, false);
			holeNode = new HoleNode();
			holeNode.setHoleTypeOptions(new HoleType[] { HoleType.ImportDeclaration, HoleType.TypeDeclaration });
			parentHole.addChild(holeNode);
			break;
		case "import":
			parentNode = null;
			if (parent.isLeft()) {
				parentNode = (CompilationUnit) parent.getLeft();
				if (parentNode.getImports().size() == 0) {
					NodeList<ImportDeclaration> importNodeList = new NodeList<ImportDeclaration>();
					importNodeList.add((ImportDeclaration) node);
					parentNode.setImports(importNodeList);
					currentHole.set(HoleType.ImportDeclarations, false);
					holeNode = new HoleNode(HoleType.ImportDeclaration, false);
					holeNode.setHoleTypeOptions(new HoleType[] { HoleType.ImportDeclaration });
					currentHole.addChild(holeNode);
					holeNode = new HoleNode();
					holeNode.setHoleTypeOptions(new HoleType[] { HoleType.ImportDeclaration }); // under wrapper: should only be
					currentHole.addChild(holeNode);
				}
			} else {
				NodeList<ImportDeclaration> importDeclarations = (NodeList<ImportDeclaration>) parent.get().get();
				importDeclarations.add((ImportDeclaration) node);
				currentHole.set(HoleType.ImportDeclaration, false);
				holeNode = new HoleNode();
				holeNode.setHoleTypeOptions(new HoleType[] { HoleType.ImportDeclaration });
				parentHole.addChild(holeNode);
			}
			break;
		case "interface":
			parentNode = (CompilationUnit) parent.getLeft();
			parentNode.addType((ClassOrInterfaceDeclaration) node);
			currentHole.set(HoleType.TypeDeclarations, false);

			holeNode = new HoleNode(HoleType.Wrapper, false);
			holeNode.setHoleTypeOptions(new HoleType[] { HoleType.InterfaceDeclaration });
			currentHole.addChild(holeNode);

			HoleNode childHoleNode0 = new HoleNode();
			childHoleNode0.setHoleTypeOptions(new HoleType[] { HoleType.BodyDeclaration });
			holeNode.addChild(childHoleNode0);
			break;
		case "class": // Note: class and interface belongs to TypeDeclaration.
			parentNode = null;
			if (parent.isLeft()) {
				parentNode = (CompilationUnit) parent.getLeft();
				parentNode.addType((ClassOrInterfaceDeclaration) node);

				currentHole.set(HoleType.TypeDeclarations, false);

				holeNode = new HoleNode(HoleType.Wrapper, false);
				holeNode.setHoleTypeOptions(new HoleType[] { HoleType.ClassDeclaration });
				currentHole.addChild(holeNode);

				HoleNode childHoleNode = new HoleNode();
				childHoleNode.setHoleTypeOptions(new HoleType[] { HoleType.BodyDeclaration });
				holeNode.addChild(childHoleNode);
			} else {
				NodeList<ClassOrInterfaceDeclaration> classOrInterfaceDeclarations = (NodeList<ClassOrInterfaceDeclaration>) parent
						.get().get();
				classOrInterfaceDeclarations.add((ClassOrInterfaceDeclaration) node);

				currentHole.set(HoleType.Wrapper, false);
				currentHole.setHoleTypeOptions(new HoleType[] { HoleType.ClassDeclaration });

				HoleNode childHoleNode = new HoleNode();
				childHoleNode.setHoleTypeOptions(new HoleType[] { HoleType.BodyDeclaration });
				currentHole.addChild(childHoleNode);
			}
			break;
		case "constructor":
			break;
		case "method":
			if (parentHoleType.equals(HoleType.BodyDeclarations)) {
				NodeList<BodyDeclaration<?>> bodyDeclarations = (NodeList<BodyDeclaration<?>>) parent.get().get();
				// Interface method, no body.
				BodyDeclaration<?> bodyDeclaration0 = bodyDeclarations.get(0);
				String firstBodyDeclarationType = StringHelper.getClassName(bodyDeclaration0.getClass().toString());
				if (firstBodyDeclarationType.equals("MethodDeclaration")
						&& ((MethodDeclaration) bodyDeclaration0).getBody().isEmpty()) {
					((MethodDeclaration) node).removeBody();
				}
				bodyDeclarations.add((BodyDeclaration<?>) node);

				currentHole.set(HoleType.Wrapper, false);
				currentHole.setHoleTypeOptions(new HoleType[] { HoleType.MethodDeclaration });
				holeNode = new HoleNode();
				holeNode.setHoleTypeOptions(new HoleType[] { HoleType.TypeExtends });
				currentHole.addChild(holeNode);
			}
			if (parentNodeClassStr != null && parentNodeClassStr.equals("ClassOrInterfaceDeclaration")) {
				ClassOrInterfaceDeclaration classOrInterfaceDeclaration = (ClassOrInterfaceDeclaration) parent.getLeft();
				// If Interface, no body
				if (classOrInterfaceDeclaration.isInterface()) {
					((MethodDeclaration) node).removeBody();
				}
				classOrInterfaceDeclaration.addMember((BodyDeclaration<?>) node);

				currentHole.set(HoleType.BodyDeclarations, false);

				holeNode = new HoleNode(HoleType.Wrapper, false);
				holeNode.setHoleTypeOptions(new HoleType[] { HoleType.MethodDeclaration });
				currentHole.addChild(holeNode);

				HoleNode chilHoleNode = new HoleNode();
				chilHoleNode.setHoleTypeOptions(new HoleType[] { HoleType.TypeExtends });
				holeNode.addChild(chilHoleNode);
			}
			break;
		case "arrowFunction":
			break;
		case "field":
			if (parentNodeClassStr != null && parentNodeClassStr.equals("ClassOrInterfaceDeclaration")
					&& parentHoleType.equals(HoleType.Wrapper)) {
				// real field
				ClassOrInterfaceDeclaration pNode = (ClassOrInterfaceDeclaration) parent.getLeft();
				pNode.addMember((BodyDeclaration<?>) node);

				currentHole.set(HoleType.BodyDeclarations, false);

				holeNode = new HoleNode(HoleType.Wrapper, false);
				holeNode.setHoleTypeOptions(new HoleType[] { HoleType.FieldDeclaration });
				currentHole.addChild(holeNode);

				HoleNode chilHoleNode = new HoleNode();
				chilHoleNode.setHoleTypeOptions(new HoleType[] { HoleType.Expression });
				holeNode.addChild(chilHoleNode);
			} else if (parentHoleType.equals(HoleType.BodyDeclarations)) {
				NodeList<BodyDeclaration<?>> bodyDeclarations = (NodeList<BodyDeclaration<?>>) parent.get().get();
				bodyDeclarations.add((BodyDeclaration<?>) node);

				currentHole.set(HoleType.Wrapper, false);
				currentHole.setHoleTypeOptions(new HoleType[] { HoleType.FieldDeclaration });

				HoleNode chilHoleNode = new HoleNode();
				chilHoleNode.setHoleTypeOptions(new HoleType[] { HoleType.Expression });
				currentHole.addChild(chilHoleNode);

			} else if (parentNodeClassStr != null && parentNodeClassStr.equals("MethodDeclaration")) {
				// variable declaration inside body. i.e. VariableDeclarationExpr
				// Regenerate VariableDeclarationExpr.
				node = new FieldAST().generateVariableDeclarationExpr(pattern);
				MethodDeclaration mNode = (MethodDeclaration) parent.getLeft();
				Optional<BlockStmt> optionalBody = mNode.getBody();
				currentHole.set(HoleType.Body, false);

				HoleNode anotherCurrentHole = new HoleNode();
				currentHole.addChild(anotherCurrentHole);

				BlockStmt blockStmt = optionalBody.get();
				NodeList<Statement> statements = blockStmt.getStatements();

				if (statements.size() == 0) {
					statements.add((Statement) node);

					anotherCurrentHole.set(HoleType.Statements, false);

					holeNode = new HoleNode(HoleType.Wrapper, false);
					holeNode.setHoleTypeOptions(new HoleType[] { HoleType.Statement });
					anotherCurrentHole.addChild(holeNode);

					holeNode.addChild(new HoleNode());
				} else {
					System.out.println("Should not go to this branch");
				}
				// } else if (parentHoleType.equals(HoleType.Wrapper) &&
				// parentOfParentHoleType.equals(HoleType.Body)) {
			} else if (parentNodeClassStr != null && parentNodeClassStr.equals("BlockStmt")) {
				// variable declaration inside body. i.e. VariableDeclarationExpr
				// regenerate VariableDeclarationExpr.
				// TODO: later may according to index to insert to specific location.
				node = new FieldAST().generateVariableDeclarationExpr(pattern);
				BlockStmt blockStmt = (BlockStmt) parent.getLeft();
				NodeList<Statement> statements = blockStmt.getStatements();

				statements.add((Statement) node);

				currentHole.set(HoleType.Statement, false);
				HoleNode holeNodeChild = new HoleNode();
				holeNodeChild.setHoleTypeOptions(new HoleType[] { HoleType.Expression });
				currentHole.addChild(holeNodeChild);
			} else if (parentNodeClassStr != null && parentNodeClassStr.equals("ForStmt")) {
				ForStmt forStmt = (ForStmt) parent.getLeft();
				ExpressionStmt expressionStmt = (ExpressionStmt) new FieldAST().generateVariableDeclarationExpr(pattern);
				NodeList<Expression> initializationList = new NodeList<Expression>();
				initializationList.add(expressionStmt.getExpression());
				forStmt.setInitialization(initializationList);

				currentHole.set(HoleType.ForInitialization, false);
				currentHole.addChild(new HoleNode());
			} else if (parentHoleType.equals(HoleType.Statements)) {

				node = new FieldAST().generateVariableDeclarationExpr(pattern);
				NodeList<Statement> statements = (NodeList<Statement>) parent.get().get();
				statements.add((Statement) node);

				currentHole.set(HoleType.Wrapper, false);
				currentHole.setHoleTypeOptions(new HoleType[] { HoleType.Statement });

				currentHole.addChild(new HoleNode());
			}
			break;
		case "typeExtends":
			if (parentNodeClassStr != null && parentNodeClassStr.equals("MethodDeclaration")) {
				MethodDeclaration mNode = (MethodDeclaration) parent.getLeft();
				mNode.setType((Type) node);
				currentHole.set(HoleType.TypeExtends, false);
				HoleNode newHole = new HoleNode();
				newHole.setHoleTypeOptionsOfOnlyOne(HoleType.TypeVariables);
				parentHole.addChild(newHole);
			}
			break;
		case "typeVariable":
			if (parentNodeClassStr != null && parentNodeClassStr.equals("MethodDeclaration")) {
				MethodDeclaration mNode = (MethodDeclaration) parent.getLeft();
				NodeList<Parameter> nodeList = new NodeList<Parameter>();
				nodeList.add((Parameter) node);
				mNode.setParameters(nodeList);

				currentHole.set(HoleType.TypeVariables, false);

				holeNode = new HoleNode(HoleType.TypeVariable, false);
				holeNode.setHoleTypeOptions(new HoleType[] { HoleType.TypeVariable });
				currentHole.addChild(holeNode);

				currentHole.addChild(new HoleNode());
			} else if (parentHoleType.equals(HoleType.TypeVariables)) {
				NodeList<Parameter> nodeList = (NodeList<Parameter>) parent.get().get();
				nodeList.add((Parameter) node);
				currentHole.set(HoleType.TypeVariable, false);
				holeNode = new HoleNode();
				holeNode.setHoleTypeOptions(new HoleType[] { HoleType.TypeVariable });
				parentHole.addChild(holeNode);
			}
			break;
		case "for":
			HoleType holeTypeFor = HoleType.ForStmt;
			if (parentHoleType.equals(HoleType.Statements)) {
				NodeList<Statement> statements = (NodeList<Statement>) parent.get().get();
				statements.add((Statement) node);

				currentHole.set(HoleType.Wrapper, false);
				currentHole.setHoleTypeOptions(new HoleType[] { HoleType.ForStmt });
				currentHole.addChild(new HoleNode(HoleType.Undefined, true, HoleType.ForInitialization));
			} else if (parentNodeClassStr != null && parentNodeClassStr.equals("MethodDeclaration")) {
				MethodDeclaration mNode = (MethodDeclaration) parent.getLeft();
				Optional<BlockStmt> optionalBody = mNode.getBody();
				currentHole.set(HoleType.Body, false);
				HoleNode anotherCurrentHole = new HoleNode();
				currentHole.addChild(anotherCurrentHole);
				BlockStmt blockStmt = optionalBody.get();
				NodeList<Statement> statements = blockStmt.getStatements();
				if (statements.size() == 0) {
					statements.add((Statement) node);

					anotherCurrentHole.set(HoleType.Statements, false);
					holeNode = new HoleNode(HoleType.Wrapper, false, HoleType.ForStmt);
					anotherCurrentHole.addChild(holeNode);
					holeNode.addChild(new HoleNode(HoleType.Undefined, true, HoleType.ForInitialization));
				}
				// } else if (parentHoleType.equals(HoleType.Wrapper) &&
				// parentOfParentHoleType.equals(HoleType.Body)) {
			} else if (parentNodeClassStr != null && parentNodeClassStr.equals("BlockStmt")) {
				BlockStmt blockStmt = (BlockStmt) parent.getLeft();
				NodeList<Statement> statements = blockStmt.getStatements();
				statements.add((Statement) node);
				currentHole.set(HoleType.Statement, false);
				HoleNode holeNodeChild = new HoleNode(HoleType.Undefined, true, HoleType.ForInitialization);
				currentHole.addChild(holeNodeChild);
			} else if (parentNodeClassStr != null && parentNodeClassStr.equals("WhileStmt")) {
				WhileStmt whileStmt = (WhileStmt) parent.getLeft();
				Statement body = whileStmt.getBody();
				String bodyClassStr = StringHelper.getClassName(body.getClass().toString());
				if (bodyClassStr.equals("ReturnStmt")) {
					BlockStmt blockStmt = new BlockStmt();
					NodeList<Statement> statements = new NodeList<Statement>();
					statements.add((Statement) node);
					blockStmt.setStatements(statements);
					whileStmt.setBody(blockStmt);

					currentHole.set(HoleType.Body, false);
					HoleNode anotherCurrentHole = new HoleNode();
					anotherCurrentHole.set(HoleType.Statements, false);
					currentHole.addChild(anotherCurrentHole);

					HoleNode childNode = new HoleNode(HoleType.Wrapper, false);
					childNode.setHoleTypeOptionsOfOnlyOne(holeTypeFor);
					anotherCurrentHole.addChild(childNode);
					childNode.addChild(new HoleNode(HoleType.Undefined, true, HoleType.ForInitialization));
				} else {
					// TODO
				}
			} else if (parentNodeClassStr != null && parentNodeClassStr.equals("ForStmt")) {
				ForStmt stmt = (ForStmt) parent.getLeft();
				Statement body = stmt.getBody();
				String bodyClassStr = body.getClass().toString();
				bodyClassStr = StringHelper.getClassName(bodyClassStr);
				if (bodyClassStr.equals("ReturnStmt")) {
					BlockStmt blockStmt = new BlockStmt();
					NodeList<Statement> statements = new NodeList<Statement>();
					statements.add((Statement) node);
					blockStmt.setStatements(statements);
					stmt.setBody(blockStmt);

					currentHole.set(HoleType.Body, false);
					HoleNode anotherCurrentHole = new HoleNode(HoleType.Statements, false);
					currentHole.addChild(anotherCurrentHole);

					HoleNode childNode = new HoleNode(HoleType.Wrapper, false);
					childNode.setHoleTypeOptionsOfOnlyOne(holeTypeFor);
					anotherCurrentHole.addChild(childNode);
					childNode.addChild(new HoleNode(HoleType.Undefined, true, HoleType.ForInitialization));
				} else {
					// TODO
				}
			}
			break;
		case "while":
			if (parentHoleType.equals(HoleType.Statements)) {
				NodeList<Statement> statements = (NodeList<Statement>) parent.get().get();
				if (holeIndex < statements.size()) {
					// insert at holeIndex
					statements.add(holeIndex, (Statement) node);
				} else {
					// append
					statements.add((Statement) node);
					currentHole.set(HoleType.Wrapper, false);
					currentHole.setHoleTypeOptions(new HoleType[] { HoleType.WhileStmt });
					HoleNode holeNodeChild = new HoleNode();
					holeNodeChild.setHoleTypeOptions(new HoleType[] { HoleType.Expression });
					currentHole.addChild(holeNodeChild);
				}
			} else if (parentNodeClassStr != null && parentNodeClassStr.equals("MethodDeclaration")) {
				MethodDeclaration mNode = (MethodDeclaration) parent.getLeft();
				Optional<BlockStmt> optionalBody = mNode.getBody();
				currentHole.set(HoleType.Body, false);
				HoleNode anotherCurrentHole = new HoleNode();
				currentHole.addChild(anotherCurrentHole);
				BlockStmt blockStmt = optionalBody.get();
				NodeList<Statement> statements = blockStmt.getStatements();
				if (statements.size() == 0) {
					statements.add((Statement) node);
					anotherCurrentHole.set(HoleType.Statements, false);
					HoleNode childNode = new HoleNode(HoleType.Wrapper, false);
					childNode.setHoleTypeOptionsOfOnlyOne(HoleType.WhileStmt);
					anotherCurrentHole.addChild(childNode);
					childNode.addChild(new HoleNode());
				}
			} else if (parentNodeClassStr != null && parentNodeClassStr.equals("ForStmt")) {
				ForStmt forStmt = (ForStmt) parent.getLeft();
				Statement body = forStmt.getBody();
				String bodyClassStr = body.getClass().toString();
				bodyClassStr = StringHelper.getClassName(bodyClassStr);
				if (bodyClassStr.equals("ReturnStmt")) {
					BlockStmt blockStmt = new BlockStmt();
					NodeList<Statement> statements = new NodeList<Statement>();
					statements.add((Statement) node);
					blockStmt.setStatements(statements);
					forStmt.setBody(blockStmt);

					currentHole.set(HoleType.Body, false);

					HoleNode anotherCurrentHole = new HoleNode();
					anotherCurrentHole.set(HoleType.Statements, false);
					currentHole.addChild(anotherCurrentHole);

					HoleNode childNode = new HoleNode();
					childNode.set(HoleType.Wrapper, false);
					childNode.setHoleTypeOptionsOfOnlyOne(HoleType.WhileStmt);
					anotherCurrentHole.addChild(childNode);
					childNode.addChild(new HoleNode());
				} else if (bodyClassStr.equals("BlockStmt")) {

				} else {
					System.out.println("Should not go to this branch");
				}
			} else if (parentNodeClassStr != null && parentNodeClassStr.equals("WhileStmt")) {
				WhileStmt whileStmt = (WhileStmt) parent.getLeft();
				Statement body = whileStmt.getBody();
				String bodyClassStr = StringHelper.getClassName(body.getClass().toString());
				if (bodyClassStr.equals("ReturnStmt")) {
					BlockStmt blockStmt = new BlockStmt();
					NodeList<Statement> statements = new NodeList<Statement>();
					statements.add((Statement) node);
					blockStmt.setStatements(statements);
					whileStmt.setBody(blockStmt);

					currentHole.set(HoleType.Body, false);
					HoleNode anotherCurrentHole = new HoleNode(HoleType.Statements, false);
					currentHole.addChild(anotherCurrentHole);
					HoleNode childNode = new HoleNode(HoleType.Wrapper, false);
					childNode.setHoleTypeOptionsOfOnlyOne(HoleType.WhileStmt);
					anotherCurrentHole.addChild(childNode);
					childNode.addChild(new HoleNode());
				} else {
					// TODO
				}
			}
			break;
		case "if":
			if (parentHoleType.equals(HoleType.Statements)) {
				NodeList<Statement> statements = (NodeList<Statement>) parent.get().get();
				if (holeIndex < statements.size()) {
					// insert at holeIndex
					statements.add(holeIndex, (Statement) node);
				} else {
					// append
					statements.add((Statement) node);
					currentHole.set(HoleType.Wrapper, false);
					currentHole.setHoleTypeOptions(new HoleType[] { HoleType.IfStmt });
					HoleNode holeNodeChild = new HoleNode();
					holeNodeChild.setHoleTypeOptions(new HoleType[] { HoleType.Expression });
					currentHole.addChild(holeNodeChild);
				}
			} else if (parentNodeClassStr != null && parentNodeClassStr.equals("ForStmt")) {
				ForStmt forStmt = (ForStmt) parent.getLeft();
				Statement body = forStmt.getBody();
				String bodyClassStr = body.getClass().toString();
				bodyClassStr = StringHelper.getClassName(bodyClassStr);
				if (bodyClassStr.equals("ReturnStmt")) {
					BlockStmt blockStmt = new BlockStmt();
					NodeList<Statement> statements = new NodeList<Statement>();
					statements.add((Statement) node);
					blockStmt.setStatements(statements);
					forStmt.setBody(blockStmt);

					currentHole.set(HoleType.Body, false);

					HoleNode anotherCurrentHole = new HoleNode(HoleType.Statements, false);
					currentHole.addChild(anotherCurrentHole);

					HoleNode childNode = new HoleNode(HoleType.Wrapper, false);
					childNode.setHoleTypeOptionsOfOnlyOne(HoleType.IfStmt);
					anotherCurrentHole.addChild(childNode);

					childNode.addChild(new HoleNode());
				} else if (bodyClassStr.equals("BlockStmt")) {

				} else {
					System.out.println("Should not go to this branch");
				}
			} else if (parentNodeClassStr != null && parentNodeClassStr.equals("WhileStmt")) {
				WhileStmt whileStmt = (WhileStmt) parent.getLeft();
				Statement body = whileStmt.getBody();
				String bodyClassStr = StringHelper.getClassName(body.getClass().toString());
				if (bodyClassStr.equals("ReturnStmt")) {
					BlockStmt blockStmt = new BlockStmt();
					NodeList<Statement> statements = new NodeList<Statement>();
					statements.add((Statement) node);
					blockStmt.setStatements(statements);
					whileStmt.setBody(blockStmt);

					currentHole.set(HoleType.Body, false);

					HoleNode anotherCurrentHole = new HoleNode(HoleType.Statements, false);
					currentHole.addChild(anotherCurrentHole);

					HoleNode childNode = new HoleNode(HoleType.Wrapper, false);
					childNode.setHoleTypeOptionsOfOnlyOne(HoleType.IfStmt);
					anotherCurrentHole.addChild(childNode);
					childNode.addChild(new HoleNode());
				} else {
					// TODO
				}
			} else if (parentNodeClassStr != null && parentNodeClassStr.equals("MethodDeclaration")) {
				MethodDeclaration mNode = (MethodDeclaration) parent.getLeft();
				Optional<BlockStmt> optionalBody = mNode.getBody();
				currentHole.set(HoleType.Body, false);
				HoleNode anotherCurrentHole = new HoleNode();
				currentHole.addChild(anotherCurrentHole);
				BlockStmt blockStmt = optionalBody.get();
				NodeList<Statement> statements = blockStmt.getStatements();
				if (statements.size() == 0) {
					statements.add((Statement) node);

					anotherCurrentHole.set(HoleType.Statements, false);

					HoleNode childNode = new HoleNode(HoleType.Wrapper, false);
					childNode.setHoleTypeOptionsOfOnlyOne(HoleType.IfStmt);
					anotherCurrentHole.addChild(childNode);
					childNode.addChild(new HoleNode());
				}
			}
			break;
		case "switch":
			if (parentHoleType.equals(HoleType.Statements)) {
				NodeList<Statement> statements = (NodeList<Statement>) parent.get().get();
				if (holeIndex < statements.size()) {
					// insert at holeIndex
					statements.add(holeIndex, (Statement) node);
				} else {
					// append
					statements.add((Statement) node);
					currentHole.set(HoleType.Wrapper, false);
					currentHole.setHoleTypeOptions(new HoleType[] { HoleType.SwitchStmt });
					HoleNode holeNodeChild = new HoleNode();
					holeNodeChild.setHoleTypeOptions(new HoleType[] { HoleType.Expression });
					currentHole.addChild(holeNodeChild);
				}
			} else if (parentNodeClassStr != null && parentNodeClassStr.equals("ForStmt")) {
				ForStmt forStmt = (ForStmt) parent.getLeft();
				Statement body = forStmt.getBody();
				String bodyClassStr = body.getClass().toString();
				bodyClassStr = StringHelper.getClassName(bodyClassStr);
				if (bodyClassStr.equals("ReturnStmt")) {
					BlockStmt blockStmt = new BlockStmt();
					NodeList<Statement> statements = new NodeList<Statement>();
					statements.add((Statement) node);
					blockStmt.setStatements(statements);
					forStmt.setBody(blockStmt);

					currentHole.set(HoleType.Body, false);

					HoleNode anotherCurrentHole = new HoleNode(HoleType.Statements, false);
					currentHole.addChild(anotherCurrentHole);

					HoleNode childNode = new HoleNode(HoleType.Wrapper, false);
					childNode.setHoleTypeOptionsOfOnlyOne(HoleType.SwitchStmt);
					anotherCurrentHole.addChild(childNode);
					childNode.addChild(new HoleNode());
				} else if (bodyClassStr.equals("BlockStmt")) {

				} else {
					System.out.println("Should not go to this branch");
				}
			} else if (parentNodeClassStr != null && parentNodeClassStr.equals("WhileStmt")) {
				WhileStmt whileStmt = (WhileStmt) parent.getLeft();
				Statement body = whileStmt.getBody();
				String bodyClassStr = StringHelper.getClassName(body.getClass().toString());
				if (bodyClassStr.equals("ReturnStmt")) {
					BlockStmt blockStmt = new BlockStmt();
					NodeList<Statement> statements = new NodeList<Statement>();
					statements.add((Statement) node);
					blockStmt.setStatements(statements);
					whileStmt.setBody(blockStmt);

					currentHole.set(HoleType.Body, false);

					HoleNode anotherCurrentHole = new HoleNode();
					anotherCurrentHole.set(HoleType.Statements, false);
					currentHole.addChild(anotherCurrentHole);

					HoleNode childNode = new HoleNode(HoleType.Wrapper, false);
					childNode.setHoleTypeOptionsOfOnlyOne(HoleType.SwitchStmt);
					anotherCurrentHole.addChild(childNode);
					childNode.addChild(new HoleNode());
				} else if (bodyClassStr.equals("BlockStmt")) {

				} else {
					System.out.println("Should not go to this branch");
				}
			} else if (parentNodeClassStr != null && parentNodeClassStr.equals("MethodDeclaration")) {
				MethodDeclaration mNode = (MethodDeclaration) parent.getLeft();
				Optional<BlockStmt> optionalBody = mNode.getBody();
				currentHole.set(HoleType.Body, false);
				HoleNode anotherCurrentHole = new HoleNode();
				currentHole.addChild(anotherCurrentHole);
				BlockStmt blockStmt = optionalBody.get();
				NodeList<Statement> statements = blockStmt.getStatements();
				if (statements.size() == 0) {
					statements.add((Statement) node);

					anotherCurrentHole.set(HoleType.Statements, false);

					HoleNode childNode = new HoleNode(HoleType.Wrapper, false);
					childNode.setHoleTypeOptionsOfOnlyOne(HoleType.SwitchStmt);
					anotherCurrentHole.addChild(childNode);
					childNode.addChild(new HoleNode());
				}
			}
			break;
		case "tryCatch":
			break;
		case "override":
			break;
		case "subexpression":
			HoleType holeTypeExpr = HoleType.InnerExpr;
			if (parentNodeClassStr != null && parentNodeClassStr.equals("FieldDeclaration")) {
				NodeList<VariableDeclarator> variableDeclarators = ((FieldDeclaration) parent.getLeft()).getVariables();
				VariableDeclarator vNode = variableDeclarators.get(0);
				vNode.setInitializer((Expression) node);

				currentHole.set(HoleType.VariableDeclarator, false);

				HoleNode variableHole = new HoleNode(HoleType.Wrapper, false);
				variableHole.setHoleTypeOptionsOfOnlyOne(HoleType.VariableDeclarator);
				currentHole.addChild(variableHole);

				HoleNode initializerHole = new HoleNode(HoleType.VariableInitializer, false);
				variableHole.addChild(initializerHole);

				HoleNode innerHole = new HoleNode(HoleType.Wrapper, false);
				innerHole.setHoleTypeOptionsOfOnlyOne(holeTypeExpr);
				initializerHole.addChild(innerHole);
				innerHole.addChild(new HoleNode());
			} else if (parentNodeClassStr != null && parentNodeClassStr.equals("BinaryExpr")) {
				this.generateBinarExprInExpr(parent, holeIndex, node, currentHole, parentHole, parentOfParentHole,
						parentOfParentOfParentHole, holeTypeExpr);
			} else if (parentNodeClassStr != null && parentNodeClassStr.equals("AssignExpr")) {
				this.generateExprInAssignExprForExpr10AndExpr11(parent, node, currentHole, parentOfParentHole, holeTypeExpr);
			} else if (parentNodeClassStr != null && parentNodeClassStr.equals("ReturnStmt")) {
				this.generateReturn6ForExpr10AndExpr11(parent, node, currentHole, parentHole, parentOfParentHole, holeTypeExpr);
			}
			break;
		case "break":
			if (parentHoleType.equals(HoleType.Statements)) {
				NodeList<Statement> statements = (NodeList<Statement>) parent.get().get();
				if (holeIndex < statements.size()) {
					// TODO
				} else {
					statements.add((Statement) node);
					currentHole.set(HoleType.Break, false);
					parentOfParentHole.addChild(new HoleNode());
				}
			}
			break;
		case "continue":
			if (parentHoleType.equals(HoleType.Statements)) {
				NodeList<Statement> statements = (NodeList<Statement>) parent.get().get();
				if (holeIndex < statements.size()) {
					// TODO
				} else {
					statements.add((Statement) node);
					currentHole.set(HoleType.Continue, false);
					parentOfParentHole.addChild(new HoleNode());
				}
			}
			break;
		case "newInstance":
			if (parentNodeClassStr != null && parentNodeClassStr.equals("ExpressionStmt")) {
				ExpressionStmt expressionStmt = (ExpressionStmt) parent.getLeft();
				VariableDeclarationExpr expression = (VariableDeclarationExpr) expressionStmt.getExpression();
				NodeList<VariableDeclarator> variableDeclarators = expression.getVariables();
				variableDeclarators.get(0).setInitializer((Expression) node);

				currentHole.set(HoleType.Expression, false);
				holeNode = new HoleNode(HoleType.VariableDeclarator, false);
				currentHole.addChild(holeNode);

				HoleNode childHoleNode = new HoleNode(HoleType.Wrapper, false);
				childHoleNode.setHoleTypeOptionsOfOnlyOne(HoleType.VariableDeclarator);
				holeNode.addChild(childHoleNode);

				HoleNode childOfChildNode = new HoleNode(HoleType.VariableInitializer, false);
				childHoleNode.addChild(childOfChildNode);
				childOfChildNode.addChild(new HoleNode());
			}
			break;
		case "throw":
			holeTypeExpr = HoleType.Throw;
			if (parentHoleType.equals(HoleType.Statements)) {
				NodeList<Statement> statements = (NodeList<Statement>) parent.get().get();
				if (holeIndex < statements.size()) {
					// TODO
				} else {
					statements.add((Statement) node);
					currentHole.set(holeTypeExpr, false);
					parentHole.addChild(new HoleNode());
				}
			} else if (parentNodeClassStr != null && parentNodeClassStr.equals("MethodDeclaration")) {
				MethodDeclaration mNode = (MethodDeclaration) parent.getLeft();
				Optional<BlockStmt> optionalBody = mNode.getBody();
				currentHole.set(HoleType.Body, false);

				HoleNode anotherCurrentHole = new HoleNode();
				currentHole.addChild(anotherCurrentHole);

				BlockStmt blockStmt = optionalBody.get();
				NodeList<Statement> statements = blockStmt.getStatements();
				if (statements.size() == 0) {
					statements.add((Statement) node);

					anotherCurrentHole.set(HoleType.Statements, false);
					holeNode = new HoleNode(holeTypeExpr, false);
					anotherCurrentHole.addChild(holeNode);
					parentOfParentHole.addChild(new HoleNode());
				}
			} else if (parentNodeClassStr != null && parentNodeClassStr.equals("WhileStmt")) {
				WhileStmt whileStmt = (WhileStmt) parent.getLeft();
				Statement body = whileStmt.getBody();
				String bodyClassStr = StringHelper.getClassName(body.getClass().toString());
				if (bodyClassStr.equals("ReturnStmt")) {
					BlockStmt blockStmt = new BlockStmt();
					NodeList<Statement> statements = new NodeList<Statement>();
					statements.add((Statement) node);
					blockStmt.setStatements(statements);
					whileStmt.setBody(blockStmt);

					currentHole.set(HoleType.Body, false);

					HoleNode anotherCurrentHole = new HoleNode(HoleType.Statements, false);
					anotherCurrentHole.setHoleTypeOptions(new HoleType[] { HoleType.BlockStmt });
					currentHole.addChild(anotherCurrentHole);

					HoleNode childNode = new HoleNode(HoleType.Statement, false);
					anotherCurrentHole.addChild(childNode);

					HoleNode exprNode = new HoleNode(HoleType.Throw, false);
					childNode.addChild(exprNode);
					anotherCurrentHole.addChild(new HoleNode());
				} else {
					// TODO
				}
			} else if (parentNodeClassStr != null && parentNodeClassStr.equals("ForStmt")) {
				ForStmt forStmt = (ForStmt) parent.getLeft();
				Statement body = this.generateTotalStmt(forStmt.getBody(), node, currentHole, parentHole, holeTypeExpr);
				forStmt.setBody(body);
			} else if (parentNodeClassStr != null && parentNodeClassStr.equals("IfStmt")) {
				this.generateTotalStmtInIfThenStmt(parent, node, currentHole, parentHole, holeTypeExpr);
			} else if (parentNodeClassStr != null && parentNodeClassStr.equals("SwitchEntry")) {
				this.generateSwitchEntryForTotalReturnStmt(parent, node, holeIndex, currentHole, holeTypeExpr);
			}
			break;
		case "let1":
			holeTypeExpr = HoleType.Let1Expr;
			if (parentHoleType.equals(HoleType.Statements)) {
				this.generateExprStmtForLet1AndLet2(parent, node, holeIndex, currentHole, parentHole, holeTypeExpr);
			} else if (parentNodeClassStr != null && parentNodeClassStr.equals("WhileStmt")) {
				WhileStmt whileStmt = (WhileStmt) parent.getLeft();
				Statement body = whileStmt.getBody();
				String bodyClassStr = StringHelper.getClassName(body.getClass().toString());
				if (bodyClassStr.equals("ReturnStmt")) {
					BlockStmt blockStmt = new BlockStmt();
					NodeList<Statement> statements = new NodeList<Statement>();
					ExpressionStmt expressionStmt = new ExpressionStmt((Expression) node);
					statements.add(expressionStmt);
					blockStmt.setStatements(statements);
					whileStmt.setBody(blockStmt);

					currentHole.set(HoleType.Body, false);

					HoleNode anotherCurrentHole = new HoleNode(HoleType.Statements, false);
					anotherCurrentHole.setHoleTypeOptions(new HoleType[] { HoleType.BlockStmt });
					currentHole.addChild(anotherCurrentHole);

					anotherCurrentHole.addChild(this.constructHoleASTOfAssignStmtForLet1AndLet2(holeTypeExpr));
				} else {
					// TODO
				}
			} else if (parentNodeClassStr != null && parentNodeClassStr.equals("MethodDeclaration")) {
				MethodDeclaration mNode = (MethodDeclaration) parent.getLeft();
				Optional<BlockStmt> optionalBody = mNode.getBody();
				currentHole.set(HoleType.Body, false);

				HoleNode stmtsNode = new HoleNode();
				currentHole.addChild(stmtsNode);

				BlockStmt blockStmt = optionalBody.get();
				NodeList<Statement> statements = blockStmt.getStatements();
				if (statements.size() == 0) {
					ExpressionStmt expressionStmt = new ExpressionStmt((Expression) node);
					statements.add(expressionStmt);

					stmtsNode.set(HoleType.Statements, false);
					stmtsNode.addChild(this.constructHoleASTOfAssignStmtForLet1AndLet2(holeTypeExpr));
				}
			} else if (parentNodeClassStr != null && parentNodeClassStr.equals("ForStmt")) {
				this.generateExprStmtInForStmtForLet1AndLet2(parent, currentHole, node, holeTypeExpr);
			} else if (parentNodeClassStr != null && parentNodeClassStr.equals("IfStmt")) {
				this.generateThenStmtInIfStmtForLet1AndLet2(parent, node, currentHole, holeTypeExpr);
			} else if (parentNodeClassStr != null && parentNodeClassStr.equals("SwitchEntry")) {
				this.generateSwitchEntryForLet1AndLet2(parent, node, holeIndex, currentHole, holeTypeExpr);
			}
			break;
		case "let2":
			holeTypeExpr = HoleType.Let2Expr;
			if (parentHoleType.equals(HoleType.Statements)) {
				this.generateExprStmtForLet1AndLet2(parent, node, holeIndex, currentHole, parentHole, holeTypeExpr);
			} else if (parentNodeClassStr != null && parentNodeClassStr.equals("WhileStmt")) {
				WhileStmt whileStmt = (WhileStmt) parentAndIndex.getFirst();
				Statement body = whileStmt.getBody();
				String bodyClassStr = StringHelper.getClassName(body.getChildNodes().toString());
				if (bodyClassStr.equals("ReturnStmt")) {
					BlockStmt blockStmt = new BlockStmt();
					NodeList<Statement> statements = new NodeList<Statement>();
					ExpressionStmt expressionStmt = new ExpressionStmt((Expression) node);
					statements.add(expressionStmt);
					blockStmt.setStatements(statements);
					whileStmt.setBody(blockStmt);

					currentHole.set(HoleType.Body, false);

					HoleNode anotherCurrentHole = new HoleNode(HoleType.Wrapper, false);
					currentHole.addChild(anotherCurrentHole);

					HoleNode childNode = new HoleNode(HoleType.Statement, false);
					anotherCurrentHole.addChild(childNode);

					anotherCurrentHole.addChild(new HoleNode());
				}
			} else if (parentNodeClassStr != null && parentNodeClassStr.equals("BlockStmt")) {
				BlockStmt blockStmt = (BlockStmt) parent.getLeft();
				NodeList<Statement> statements = blockStmt.getStatements();
				ExpressionStmt expressionStmt = new ExpressionStmt((Expression) node);
				statements.add(expressionStmt);

				HoleNode exprHole = this.constructHoleASTOfAssignStmtForLet1AndLet2(holeTypeExpr).getIthChild(0);
				currentHole.set(HoleType.Wrapper, false, HoleType.Statement);
				currentHole.addChild(exprHole);
			} else if (parentNodeClassStr != null && parentNodeClassStr.equals("MethodDeclaration")) {
				MethodDeclaration mNode = (MethodDeclaration) parent.getLeft();
				Optional<BlockStmt> optionalBody = mNode.getBody();
				currentHole.set(HoleType.Body, false);

				HoleNode stmtsNode = new HoleNode();
				currentHole.addChild(stmtsNode);

				BlockStmt blockStmt = optionalBody.get();
				NodeList<Statement> statements = blockStmt.getStatements();
				if (statements.size() == 0) {
					ExpressionStmt expressionStmt = new ExpressionStmt((Expression) node);
					statements.add(expressionStmt);

					stmtsNode.set(HoleType.Statements, false);
					stmtsNode.set(HoleType.Statements, false);
					stmtsNode.addChild(this.constructHoleASTOfAssignStmtForLet1AndLet2(holeTypeExpr));
				} else {
					// TODO
				}
			} else if (parentNodeClassStr != null && parentNodeClassStr.equals("ForStmt")) {
				this.generateExprStmtInForStmtForLet1AndLet2(parent, currentHole, node, holeTypeExpr);
			} else if (parentNodeClassStr != null && parentNodeClassStr.equals("IfStmt")) {
				this.generateThenStmtInIfStmtForLet1AndLet2(parent, node, currentHole, holeTypeExpr);
			} else if (parentNodeClassStr != null && parentNodeClassStr.equals("SwitchEntry")) {
				this.generateSwitchEntryForLet1AndLet2(parent, node, holeIndex, currentHole, holeTypeExpr);
			}
			break;
		case "let3":
			holeTypeExpr = HoleType.Let3Expr;
			if (parentHoleType.equals(HoleType.Statements)) {
				this.generateExprForLetInStmts(parent, node, holeIndex, currentHole, parentHole, holeTypeExpr);
			} else if (parentNodeClassStr != null && parentNodeClassStr.equals("BlockStmt")) {
				BlockStmt blockStmt = (BlockStmt) parentAndIndex.getFirst();
				NodeList<Statement> statements = blockStmt.getStatements();
				ExpressionStmt expressionStmt = new ExpressionStmt((Expression) node);
				statements.add(expressionStmt);

				currentHole.set(HoleType.Statement, false);
				parentHole.addChild(new HoleNode());
			} else if (parentNodeClassStr != null && parentNodeClassStr.equals("MethodDeclaration")) {
				MethodDeclaration mNode = (MethodDeclaration) parent.getLeft();
				Optional<BlockStmt> optionalBody = mNode.getBody();
				currentHole.set(HoleType.Body, false);

				HoleNode anotherCurrentHole = new HoleNode();
				currentHole.addChild(anotherCurrentHole);

				BlockStmt blockStmt = optionalBody.get();
				NodeList<Statement> statements = blockStmt.getStatements();
				if (statements.size() == 0) {
					ExpressionStmt expressionStmt = new ExpressionStmt((Expression) node);
					statements.add(expressionStmt);

					anotherCurrentHole.set(HoleType.Statements, false);
					holeNode = new HoleNode(HoleType.Expression, false);
					anotherCurrentHole.addChild(holeNode);

					HoleNode holeNodeChild = new HoleNode();
					holeNodeChild.setHoleTypeOptions(new HoleType[] { HoleType.Expression });
					anotherCurrentHole.addChild(holeNodeChild);
				} else {
					// TODO
				}
			} else if (parentNodeClassStr != null && parentNodeClassStr.equals("ForStmt")) {
				this.generateExprStmtInForStmt(parent, currentHole, node, holeTypeExpr);
			} else if (parentNodeClassStr != null && parentNodeClassStr.equals("IfStmt")) {
				this.generateThenStmtInIfStmt(parent, node, currentHole, holeTypeExpr);
			} else if (parentNodeClassStr != null && parentNodeClassStr.equals("SwitchEntry")) {
				this.generateSwitchEntry(parent, node, holeIndex, currentHole, holeTypeExpr);
			}
			break;
		case "let4":
			holeTypeExpr = HoleType.Let4Expr;
			if (parentHoleType.equals(HoleType.Statements)) {
				this.generateExprForLetInStmts(parent, node, holeIndex, currentHole, parentHole, holeTypeExpr);
			} else if (parentNodeClassStr != null && parentNodeClassStr.equals("BlockStmt")) {
				BlockStmt blockStmt = (BlockStmt) parentAndIndex.getFirst();
				NodeList<Statement> statements = blockStmt.getStatements();
				ExpressionStmt expressionStmt = new ExpressionStmt((Expression) node);
				statements.add(expressionStmt);

				currentHole.set(HoleType.Statement, false);
				parentHole.addChild(new HoleNode());
			} else if (parentNodeClassStr != null && parentNodeClassStr.equals("MethodDeclaration")) {
				MethodDeclaration mNode = (MethodDeclaration) parent.getLeft();
				Optional<BlockStmt> optionalBody = mNode.getBody();
				currentHole.set(HoleType.Body, false);

				HoleNode anotherCurrentHole = new HoleNode();
				currentHole.addChild(anotherCurrentHole);

				BlockStmt blockStmt = optionalBody.get();
				NodeList<Statement> statements = blockStmt.getStatements();
				if (statements.size() == 0) {
					ExpressionStmt expressionStmt = new ExpressionStmt((Expression) node);
					statements.add(expressionStmt);

					anotherCurrentHole.set(HoleType.Statements, false);

					holeNode = new HoleNode(HoleType.Expression, false);
					anotherCurrentHole.addChild(holeNode);

					HoleNode exprNode = new HoleNode(HoleType.Wrapper, false);
					exprNode.setHoleTypeOptionsOfOnlyOne(holeTypeExpr);
					holeNode.addChild(exprNode);

					HoleNode holeNodeChild = new HoleNode();
					holeNodeChild.setHoleTypeOptions(new HoleType[] { HoleType.Expression });
					anotherCurrentHole.addChild(holeNodeChild);
				} else {
					// TODO
				}

			} else if (parentNodeClassStr != null && parentNodeClassStr.equals("ForStmt")) {
				this.generateExprStmtInForStmt(parent, currentHole, node, holeTypeExpr);
			} else if (parentNodeClassStr != null && parentNodeClassStr.equals("IfStmt")) {
				this.generateThenStmtInIfStmt(parent, node, currentHole, holeTypeExpr);
			} else if (parentNodeClassStr != null && parentNodeClassStr.equals("SwitchEntry")) {
				this.generateSwitchEntry(parent, node, holeIndex, currentHole, holeTypeExpr);
			}
			break;
		case "let5":
			holeTypeExpr = HoleType.Let5Expr;
			if (parentHoleType.equals(HoleType.Statements)) {
				this.generateExprForLetInStmts(parent, node, holeIndex, currentHole, parentHole, holeTypeExpr);
			} else if (parentNodeClassStr != null && parentNodeClassStr.equals("BlockStmt")) {
				BlockStmt blockStmt = (BlockStmt) parentAndIndex.getFirst();
				NodeList<Statement> statements = blockStmt.getStatements();
				ExpressionStmt expressionStmt = new ExpressionStmt((Expression) node);
				statements.add(expressionStmt);

				currentHole.set(HoleType.Statement, false);
				parentHole.addChild(new HoleNode());
			} else if (parentNodeClassStr != null && parentNodeClassStr.equals("MethodDeclaration")) {
				MethodDeclaration mNode = (MethodDeclaration) parent.getLeft();
				Optional<BlockStmt> optionalBody = mNode.getBody();
				currentHole.set(HoleType.Body, false);

				HoleNode anotherCurrentHole = new HoleNode();
				currentHole.addChild(anotherCurrentHole);

				BlockStmt blockStmt = optionalBody.get();
				NodeList<Statement> statements = blockStmt.getStatements();
				if (statements.size() == 0) {
					ExpressionStmt expressionStmt = new ExpressionStmt((Expression) node);
					statements.add(expressionStmt);

					anotherCurrentHole.set(HoleType.Statements, false);

					holeNode = new HoleNode(HoleType.Expression, false);
					anotherCurrentHole.addChild(holeNode);

					HoleNode holeNodeChild = new HoleNode();
					holeNodeChild.setHoleTypeOptions(new HoleType[] { HoleType.Expression });
					anotherCurrentHole.addChild(holeNodeChild);
				} else {
					// TODO
				}

			} else if (parentNodeClassStr != null && parentNodeClassStr.equals("ForStmt")) {
				if (holeIndex == 0) {
					ForStmt forStmt = (ForStmt) parent.getLeft();
					NodeList<Expression> initializationList = new NodeList<Expression>();
					initializationList.add((Expression) node);
					forStmt.setInitialization(initializationList);

					currentHole.set(HoleType.ForInitialization, false);
					currentHole.addChild(new HoleNode());
				} else {
					this.generateExprStmtInForStmt(parent, currentHole, node, holeTypeExpr);
				}
			} else if (parentNodeClassStr != null && parentNodeClassStr.equals("IfStmt")) {
				this.generateThenStmtInIfStmt(parent, node, currentHole, holeTypeExpr);
			} else if (parentNodeClassStr != null && parentNodeClassStr.equals("SwitchEntry")) {
				this.generateSwitchEntry(parent, node, holeIndex, currentHole, holeTypeExpr);
			}
			break;
		case "let6":
			holeTypeExpr = HoleType.Let6Expr;
			if (parentHoleType.equals(HoleType.Statements)) {
				NodeList<Statement> statements = (NodeList<Statement>) parent.get().get();
				ExpressionStmt expressionStmt = new ExpressionStmt((Expression) node);
				statements.add(expressionStmt);
				currentHole.set(HoleType.Wrapper, false);
				currentHole.setHoleTypeOptions(new HoleType[] { HoleType.Statement });

				holeNode = new HoleNode(HoleType.Expression, false);
				currentHole.addChild(holeNode);

				HoleNode exprHole = new HoleNode(HoleType.Wrapper, false);
				exprHole.setHoleTypeOptionsOfOnlyOne(holeTypeExpr);
				holeNode.addChild(exprHole);

				exprHole.addChild(new HoleNode());
			} else if (parentNodeClassStr != null && parentNodeClassStr.equals("ForStmt")) {
				// Note: we only support BlockStmt.
				// https://www.javadoc.io/static/com.github.javaparser/javaparser-core/3.23.1/com/github/javaparser/ast/stmt/ForStmt.html
				// sum = sum + i in for(; i < 10 ;){ ;sum = sum + i; }
				ForStmt forStmt = (ForStmt) parent.getLeft();
				Statement body = forStmt.getBody();
				String bodyClassStr = body.getClass().toString();
				bodyClassStr = StringHelper.getClassName(bodyClassStr);
				if (bodyClassStr.equals("ReturnStmt")) {
					BlockStmt blockStmt = new BlockStmt();
					NodeList<Statement> statements = new NodeList<Statement>();
					ExpressionStmt expressionStmt = new ExpressionStmt((Expression) node);
					statements.add(expressionStmt);
					blockStmt.setStatements(statements);
					forStmt.setBody(blockStmt);

					currentHole.set(HoleType.Body, false);

					HoleNode anotherCurrentHole = new HoleNode(HoleType.Statements, false);
					currentHole.addChild(anotherCurrentHole);

					HoleNode childNode = new HoleNode(HoleType.Wrapper, false);
					childNode.setHoleTypeOptionsOfOnlyOne(HoleType.Let6Expr);
					anotherCurrentHole.addChild(childNode);

					childNode.addChild(new HoleNode());
				} else if (bodyClassStr.equals("BlockStmt")) {

				} else {
					System.out.println("Should not go to this branch");
				}
			} else if (parentNodeClassStr != null && parentNodeClassStr.equals("BlockStmt")) {
				BlockStmt blockStmt = (BlockStmt) parent.getLeft();
				NodeList<Statement> statements = blockStmt.getStatements();
				ExpressionStmt expressionStmt = new ExpressionStmt((Expression) node);
				statements.add(expressionStmt);
				currentHole.set(HoleType.Statements, false);
				currentHole.addChild(new HoleNode());
			} else if (parentNodeClassStr != null && parentNodeClassStr.equals("MethodDeclaration")) {
				MethodDeclaration mNode = (MethodDeclaration) parent.getLeft();
				Optional<BlockStmt> optionalBody = mNode.getBody();
				currentHole.set(HoleType.Body, false);

				HoleNode anotherCurrentHole = new HoleNode();
				currentHole.addChild(anotherCurrentHole);

				BlockStmt blockStmt = optionalBody.get();
				NodeList<Statement> statements = blockStmt.getStatements();
				if (statements.size() == 0) {
					ExpressionStmt expressionStmt = new ExpressionStmt((Expression) node);
					statements.add(expressionStmt);

					anotherCurrentHole.set(HoleType.Statements, false);

					holeNode = new HoleNode(HoleType.Expression, false);
					holeNode.setHoleTypeOptions(new HoleType[] { HoleType.Let6Expr });
					anotherCurrentHole.addChild(holeNode);

					HoleNode holeNodeChild = new HoleNode();
					holeNodeChild.setHoleTypeOptions(new HoleType[] { HoleType.Expression });
					holeNode.addChild(holeNodeChild);
				} else {
					// TODO
				}
			} else if (parentNodeClassStr != null && parentNodeClassStr.equals("IfStmt")) {
				this.generateThenStmtInIfStmtForExpr10AndExpr11AndLet6(parent, node, currentHole, holeTypeExpr);
			} else if (parentNodeClassStr != null && parentNodeClassStr.equals("SwitchEntry")) {
				this.generateSwitchEntryForExpr10AndExpr11AndLet6(parent, node, holeIndex, currentHole, holeTypeExpr);
			}
			break;
		case "return1":
			holeTypeExpr = HoleType.Return1;
			if (parentHoleType.equals(HoleType.Statements)) {
				NodeList<Statement> statements = (NodeList<Statement>) parent.get().get();
				statements.add((Statement) node);

				HoleNode exprHole = this.constructHoleASTOfReturnStmtForReturn1AndReturn2(holeTypeExpr).getIthChild(0);
				currentHole.set(HoleType.Wrapper, false, holeTypeExpr);
				currentHole.addChild(exprHole);
			} else if (parentNodeClassStr != null && parentNodeClassStr.equals("MethodDeclaration")) {
				MethodDeclaration mNode = (MethodDeclaration) parent.getLeft();
				Optional<BlockStmt> optionalBody = mNode.getBody();
				currentHole.set(HoleType.Body, false);

				HoleNode anotherCurrentHole = new HoleNode();
				currentHole.addChild(anotherCurrentHole);

				BlockStmt blockStmt = optionalBody.get();
				NodeList<Statement> statements = blockStmt.getStatements();
				if (statements.size() == 0) {
					statements.add((Statement) node);

					anotherCurrentHole.set(HoleType.Statements, false);
					anotherCurrentHole.addChild(this.constructHoleASTOfReturnStmtForReturn1AndReturn2(holeTypeExpr));
				} else {
					// TODO
				}
			} else if (parentNodeClassStr != null && parentNodeClassStr.equals("ForStmt")) {
				this.generateReturnStmtInForStmtForReturn1AndReturn2(parent, node, currentHole, parentOfParentHole,
						holeTypeExpr);
			} else if (parentNodeClassStr != null && parentNodeClassStr.equals("WhileStmt")) {
				this.generateReturnStmtInWhiletmtForReturn1AndReturn2(parent, node, currentHole, parentHole, holeTypeExpr);
			} else if (parentNodeClassStr != null && parentNodeClassStr.equals("IfStmt")) {
				this.generateTotalStmtInIfThenStmtForReturn1AndReturn2(parent, node, currentHole, parentHole, holeTypeExpr);
			} else if (parentNodeClassStr != null && parentNodeClassStr.equals("SwitchEntry")) {
				this.generateSwitchEntryForReturn11AndReturn2(parent, node, holeIndex, currentHole, holeTypeExpr);
			}
			break;
		case "return2":
			holeTypeExpr = HoleType.Return2;
			if (parentHoleType.equals(HoleType.Statements)) {
				NodeList<Statement> statements = (NodeList<Statement>) parent.get().get();
				statements.add((Statement) node);
				HoleNode exprHole = this.constructHoleASTOfReturnStmtForReturn1AndReturn2(holeTypeExpr).getIthChild(0);
				currentHole.set(HoleType.Wrapper, false, holeTypeExpr);
				currentHole.addChild(exprHole);
			} else if (parentNodeClassStr != null && parentNodeClassStr.equals("MethodDeclaration")) {
				MethodDeclaration mNode = (MethodDeclaration) parent.getLeft();
				Optional<BlockStmt> optionalBody = mNode.getBody();
				currentHole.set(HoleType.Body, false);

				HoleNode anotherCurrentHole = new HoleNode();
				currentHole.addChild(anotherCurrentHole);

				BlockStmt blockStmt = optionalBody.get();
				NodeList<Statement> statements = blockStmt.getStatements();
				if (statements.size() == 0) {
					statements.add((Statement) node);

					anotherCurrentHole.set(HoleType.Statements, false);
					anotherCurrentHole.addChild(this.constructHoleASTOfReturnStmtForReturn1AndReturn2(holeTypeExpr));
				} else {
					// TODO
				}
			} else if (parentNodeClassStr != null && parentNodeClassStr.equals("ForStmt")) {
				this.generateReturnStmtInForStmtForReturn1AndReturn2(parent, node, currentHole, parentOfParentHole,
						holeTypeExpr);
			} else if (parentNodeClassStr != null && parentNodeClassStr.equals("WhileStmt")) {
				this.generateReturnStmtInWhiletmtForReturn1AndReturn2(parent, node, currentHole, parentHole, holeTypeExpr);
			} else if (parentNodeClassStr != null && parentNodeClassStr.equals("IfStmt")) {
				this.generateTotalStmtInIfThenStmtForReturn1AndReturn2(parent, node, currentHole, parentHole, holeTypeExpr);
			} else if (parentNodeClassStr != null && parentNodeClassStr.equals("SwitchEntry")) {
				this.generateSwitchEntryForReturn11AndReturn2(parent, node, holeIndex, currentHole, holeTypeExpr);
			}
			break;
		case "return3":
			holeTypeExpr = HoleType.Return3;
			if (parentHoleType.equals(HoleType.Statements)) {
				NodeList<Statement> statements = (NodeList<Statement>) parent.get().get();
				if (holeIndex < statements.size()) {
					// TODO
				} else {
					statements.add((Statement) node);
					currentHole.set(HoleType.Return3, false);
					parentHole.addChild(new HoleNode());
				}
			} else if (parentNodeClassStr != null && parentNodeClassStr.equals("MethodDeclaration")) {
				MethodDeclaration mNode = (MethodDeclaration) parent.getLeft();
				Optional<BlockStmt> optionalBody = mNode.getBody();
				currentHole.set(HoleType.Body, false);

				HoleNode anotherCurrentHole = new HoleNode();
				currentHole.addChild(anotherCurrentHole);

				BlockStmt blockStmt = optionalBody.get();
				NodeList<Statement> statements = blockStmt.getStatements();
				if (statements.size() == 0) {
					statements.add((Statement) node);

					anotherCurrentHole.set(HoleType.Statements, false);

					holeNode = new HoleNode(HoleType.Return3, false);
					anotherCurrentHole.addChild(holeNode);
					anotherCurrentHole.addChild(new HoleNode());
				} else {
					// TODO
				}
			} else if (parentNodeClassStr != null && parentNodeClassStr.equals("BlockStmt")) {
				BlockStmt blockStmt = (BlockStmt) parentAndIndex.getFirst();
				NodeList<Statement> statements = blockStmt.getStatements();
				statements.add((Statement) node);
				currentHole.set(HoleType.Statement, false);
				parentHole.addChild(new HoleNode());
			} else if (parentNodeClassStr != null && parentNodeClassStr.equals("ForStmt")) {
				this.generateReturnStmtInForStmt(parent, node, currentHole, parentOfParentHole, holeTypeExpr);
			} else if (parentNodeClassStr != null && parentNodeClassStr.equals("WhileStmt")) {
				this.generateReturnStmtInWhiletmt(parent, node, currentHole, parentHole, holeTypeExpr);
			} else if (parentNodeClassStr != null && parentNodeClassStr.equals("IfStmt")) {
				this.generateTotalStmtInIfThenStmt(parent, node, currentHole, parentHole, holeTypeExpr);
			} else if (parentNodeClassStr != null && parentNodeClassStr.equals("SwitchEntry")) {
				this.generateSwitchEntryForTotalReturnStmt(parent, node, holeIndex, currentHole, holeTypeExpr);
			}
			break;
		case "return4":
			holeTypeExpr = HoleType.Return4;
			if (parentHoleType.equals(HoleType.Statements)) {
				NodeList<Statement> statements = (NodeList<Statement>) parent.get().get();
				statements.add((Statement) node);
				currentHole.set(HoleType.Return4, false);
				parentOfParentHole.addChild(new HoleNode());
			} else if (parentNodeClassStr != null && parentNodeClassStr.equals("MethodDeclaration")) {
				MethodDeclaration mNode = (MethodDeclaration) parent.getLeft();
				Optional<BlockStmt> optionalBody = mNode.getBody();
				currentHole.set(HoleType.Body, false);

				HoleNode anotherCurrentHole = new HoleNode();
				currentHole.addChild(anotherCurrentHole);

				BlockStmt blockStmt = optionalBody.get();
				NodeList<Statement> statements = blockStmt.getStatements();
				if (statements.size() == 0) {
					statements.add((Statement) node);

					anotherCurrentHole.set(HoleType.Statements, false);

					holeNode = new HoleNode(HoleType.Return4, false);
					anotherCurrentHole.addChild(holeNode);
					parentOfParentHole.addChild(new HoleNode());
				} else {
					// TODO
				}
			} else if (parentNodeClassStr != null && parentNodeClassStr.equals("ForStmt")) {
				this.generateReturnStmtInForStmt(parent, node, currentHole, parentOfParentHole, holeTypeExpr);
			} else if (parentNodeClassStr != null && parentNodeClassStr.equals("WhileStmt")) {
				this.generateReturnStmtInWhiletmt(parent, node, currentHole, parentHole, holeTypeExpr);
			} else if (parentNodeClassStr != null && parentNodeClassStr.equals("IfStmt")) {
				this.generateTotalStmtInIfThenStmt(parent, node, currentHole, parentHole, holeTypeExpr);
			} else if (parentNodeClassStr != null && parentNodeClassStr.equals("SwitchEntry")) {
				this.generateSwitchEntryForTotalReturnStmt(parent, node, holeIndex, currentHole, holeTypeExpr);
			}
			break;
		case "return5":
			holeTypeExpr = HoleType.Return5;
			if (parentHoleType.equals(HoleType.Statements)) {
				NodeList<Statement> statements = (NodeList<Statement>) parent.get().get();
				statements.add((Statement) node);
				currentHole.set(HoleType.Return5, false);
				parentOfParentHole.addChild(new HoleNode());
			} else if (parentNodeClassStr != null && parentNodeClassStr.equals("MethodDeclaration")) {
				MethodDeclaration mNode = (MethodDeclaration) parent.getLeft();
				Optional<BlockStmt> optionalBody = mNode.getBody();
				currentHole.set(HoleType.Body, false);

				HoleNode anotherCurrentHole = new HoleNode();
				currentHole.addChild(anotherCurrentHole);

				BlockStmt blockStmt = optionalBody.get();
				NodeList<Statement> statements = blockStmt.getStatements();
				if (statements.size() == 0) {
					statements.add((Statement) node);

					anotherCurrentHole.set(HoleType.Statements, false);

					holeNode = new HoleNode(HoleType.Return5, false);
					anotherCurrentHole.addChild(holeNode);
					anotherCurrentHole.addChild(new HoleNode());
				} else {
					// TODO
				}
			} else if (parentNodeClassStr != null && parentNodeClassStr.equals("ForStmt")) {
				this.generateReturnStmtInForStmt(parent, node, currentHole, parentOfParentHole, holeTypeExpr);
			} else if (parentNodeClassStr != null && parentNodeClassStr.equals("WhileStmt")) {
				this.generateReturnStmtInWhiletmt(parent, node, currentHole, parentHole, holeTypeExpr);
			} else if (parentNodeClassStr != null && parentNodeClassStr.equals("IfStmt")) {
				this.generateTotalStmtInIfThenStmt(parent, node, currentHole, parentHole, holeTypeExpr);
			} else if (parentNodeClassStr != null && parentNodeClassStr.equals("SwitchEntry")) {
				this.generateSwitchEntryForTotalReturnStmt(parent, node, holeIndex, currentHole, holeTypeExpr);
			}
			break;
		case "return6":
			holeTypeExpr = HoleType.Return6;
			if (parentHoleType.equals(HoleType.Statements)) {
				NodeList<Statement> statements = (NodeList<Statement>) parent.get().get();
				statements.add((Statement) node);
				currentHole.set(HoleType.Wrapper, false);
				currentHole.setHoleTypeOptions(new HoleType[] { HoleType.Return6 });
				HoleNode holeNodeChild = new HoleNode();
				holeNodeChild.setHoleTypeOptions(new HoleType[] { HoleType.Expression });
				currentHole.addChild(holeNodeChild);
			} else if (parentNodeClassStr != null && parentNodeClassStr.equals("MethodDeclaration")) {
				MethodDeclaration mNode = (MethodDeclaration) parent.getLeft();
				Optional<BlockStmt> optionalBody = mNode.getBody();
				currentHole.set(HoleType.Body, false);

				HoleNode anotherCurrentHole = new HoleNode();
				currentHole.addChild(anotherCurrentHole);

				BlockStmt blockStmt = optionalBody.get();
				NodeList<Statement> statements = blockStmt.getStatements();
				if (statements.size() == 0) {
					statements.add((Statement) node);

					anotherCurrentHole.set(HoleType.Statements, false);
					holeNode = new HoleNode(HoleType.Wrapper, false);
					holeNode.setHoleTypeOptions(new HoleType[] { HoleType.Return6 });
					anotherCurrentHole.addChild(holeNode);

					HoleNode holeNodeChild = new HoleNode();
					holeNodeChild.setHoleTypeOptions(new HoleType[] { HoleType.Expression });
					holeNode.addChild(holeNodeChild);
				} else {
					// TODO
				}
			} else if (parentNodeClassStr != null && parentNodeClassStr.equals("ForStmt")) {
				ForStmt forStmt = (ForStmt) parent.getLeft();
				Statement body = forStmt.getBody();
				String bodyClassStr = body.getClass().toString();
				bodyClassStr = StringHelper.getClassName(bodyClassStr);
				if (bodyClassStr.equals("ReturnStmt")) {
					BlockStmt blockStmt = new BlockStmt();
					NodeList<Statement> statements = new NodeList<Statement>();
					statements.add((Statement) node);
					blockStmt.setStatements(statements);
					forStmt.setBody(blockStmt);

					currentHole.set(HoleType.Body, false);

					HoleNode anotherCurrentHole = new HoleNode();
					anotherCurrentHole.set(HoleType.Statements, false);
					currentHole.addChild(anotherCurrentHole);

					HoleNode childNode = new HoleNode(HoleType.Wrapper, false);
					childNode.setHoleTypeOptionsOfOnlyOne(holeTypeExpr);
					anotherCurrentHole.addChild(childNode);

					childNode.addChild(new HoleNode());
				} else if (bodyClassStr.equals("BlockStmt")) {

				} else {
					System.out.println("Should not go to this branch");
				}
			} else if (parentNodeClassStr != null && parentNodeClassStr.equals("WhileStmt")) {
				WhileStmt stmt = (WhileStmt) parent.getLeft();
				Statement body = this.generateReturnStmt6(stmt.getBody(), node, currentHole, parentHole, holeTypeExpr);
				stmt.setBody(body);
			} else if (parentNodeClassStr != null && parentNodeClassStr.equals("IfStmt")) {
				IfStmt ifStmt = (IfStmt) parent.getLeft();
				Statement stmt = ifStmt.getThenStmt();

				currentHole.set(HoleType.ThenStatement, false);

				String bodyClassStr = stmt.getClass().toString();
				bodyClassStr = StringHelper.getClassName(bodyClassStr);
				if (bodyClassStr.equals("ReturnStmt")) {
					ifStmt.setThenStmt((Statement) node);

					HoleNode anotherCurrentHole = new HoleNode(HoleType.Wrapper, false);
					anotherCurrentHole.setHoleTypeOptionsOfOnlyOne(holeTypeExpr);
					currentHole.addChild(anotherCurrentHole);

					HoleNode newHole = new HoleNode();
					newHole.setHoleTypeOptionsOfOnlyOne(HoleType.Expression);
					anotherCurrentHole.addChild(newHole);
				} else if (bodyClassStr.equals("BlockStmt")) {

				} else {
					System.out.println("Should not go to this branch");
				}
			} else if (parentNodeClassStr != null && parentNodeClassStr.equals("SwitchEntry")) {
				SwitchEntry switchEntry = (SwitchEntry) parent.getLeft();
				NodeList<Statement> statements = switchEntry.getStatements();
				if (holeIndex < statements.size()) {
					// TODO
				} else {
					statements.add((Statement) node);
					currentHole.set(HoleType.Statements, false);

					HoleNode childHoleNode = new HoleNode(HoleType.Wrapper, false);
					childHoleNode.setHoleTypeOptionsOfOnlyOne(holeTypeExpr);
					currentHole.addChild(childHoleNode);
					childHoleNode.addChild(new HoleNode());
				}
			}
			break;
		case "expr1":
			holeTypeExpr = HoleType.Expr1;
			this.generateCallFunctionExpr(parent, node, currentHole, parentHole, parentOfParentHole, parentHoleType,
					holeIndex, parentNodeClassStr, holeTypeExpr);
			break;
		case "expr2":
			holeTypeExpr = HoleType.Expr2;
			this.generateCallFunctionExpr(parent, node, currentHole, parentHole, parentOfParentHole, parentHoleType,
					holeIndex, parentNodeClassStr, holeTypeExpr);
			break;
		case "expr3":
			holeTypeExpr = HoleType.Expr3;
			if (parentHoleType.equals(HoleType.Statements)) {
				this.generateExpInStatements(parent, holeIndex, node, currentHole, parentHole, holeTypeExpr);
			} else if (parentNodeClassStr != null && parentNodeClassStr.equals("MethodDeclaration")) {
				this.generateExpInMethodBody(parent, currentHole, node, holeTypeExpr);
			} else if (parentNodeClassStr != null && parentNodeClassStr.equals("BinaryExpr")) {
				this.generateBinarExprInExpr(parent, holeIndex, node, currentHole, parentHole, parentOfParentHole,
						parentOfParentOfParentHole, holeTypeExpr);
			} else if (parentNodeClassStr != null && parentNodeClassStr.equals("ForStmt")) {
				this.generateExprStmtInForStmt(parent, currentHole, node, holeTypeExpr);
			} else if (parentNodeClassStr != null && parentNodeClassStr.equals("WhileStmt")) {
				this.generateExprStmtInWhileStmt(parent, currentHole, node, holeTypeExpr);
			} else if (parentNodeClassStr != null && parentNodeClassStr.equals("IfStmt")) {
				this.generateThenStmtInIfStmt(parent, node, currentHole, holeTypeExpr);
			} else if (parentNodeClassStr != null && parentNodeClassStr.equals("BlockStmt")) {
				this.generateBlockStmt(parent, node, currentHole, holeIndex, parentHole, holeTypeExpr);
			} else if (parentNodeClassStr != null && parentNodeClassStr.equals("SwitchEntry")) {
				this.generateSwitchEntry(parent, node, holeIndex, currentHole, holeTypeExpr);
			} else if (parentNodeClassStr != null && parentNodeClassStr.equals("ExpressionStmt")) {
				this.generateExpForExpressionStmt(parent, node, holeIndex, currentHole, parentOfParentHole, holeTypeExpr);
			} else if (parentNodeClassStr != null && parentNodeClassStr.equals("ReturnStmt")) {
				this.generateReturn6(parent, node, currentHole, parentHole, parentOfParentHole, holeTypeExpr);
			} else if (parentNodeClassStr != null && parentNodeClassStr.equals("AssignExpr")) {
				this.generateExprInAssignExpr(parent, node, currentHole, parentOfParentHole, holeTypeExpr);
			} else if (parentHoleType.equals(HoleType.Arguments)) {
				this.generateExprInArguments(parent, node, currentHole, parentHole, holeTypeExpr);
			} else if (parentNodeClassStr != null && parentNodeClassStr.equals("EnclosedExpr")) {
				this.generateExprForEnclosedExpr(parent, node, currentHole, parentOfParentHole, holeTypeExpr);
			}
			break;
		case "expr4":
			holeTypeExpr = HoleType.Expr4;
			if (parentHoleType.equals(HoleType.Statements)) {
				this.generateExpInStatements(parent, holeIndex, node, currentHole, parentHole, holeTypeExpr);
			} else if (parentNodeClassStr != null && parentNodeClassStr.equals("MethodDeclaration")) {
				this.generateExpInMethodBody(parent, currentHole, node, holeTypeExpr);
			} else if (parentNodeClassStr != null && parentNodeClassStr.equals("BinaryExpr")) {
				this.generateBinarExprInExpr(parent, holeIndex, node, currentHole, parentHole, parentOfParentHole,
						parentOfParentOfParentHole, holeTypeExpr);
			} else if (parentNodeClassStr != null && parentNodeClassStr.equals("ForStmt")) {
				this.generateExprStmtInForStmt(parent, currentHole, node, holeTypeExpr);
			} else if (parentNodeClassStr != null && parentNodeClassStr.equals("WhileStmt")) {
				this.generateExprStmtInWhileStmt(parent, currentHole, node, holeTypeExpr);
			} else if (parentNodeClassStr != null && parentNodeClassStr.equals("IfStmt")) {
				this.generateThenStmtInIfStmt(parent, node, currentHole, holeTypeExpr);
			} else if (parentNodeClassStr != null && parentNodeClassStr.equals("BlockStmt")) {
				this.generateBlockStmt(parent, node, currentHole, holeIndex, parentHole, holeTypeExpr);
			} else if (parentNodeClassStr != null && parentNodeClassStr.equals("SwitchStmt")) {
				SwitchStmt switchStmt = (SwitchStmt) parent.getLeft();
				switchStmt.setSelector((Expression) node);
				currentHole.set(HoleType.Expression, false);
				parentHole.addChild(new HoleNode());
			} else if (parentNodeClassStr != null && parentNodeClassStr.equals("SwitchEntry")) {
				this.generateSwitchEntry(parent, node, holeIndex, currentHole, holeTypeExpr);
			} else if (parentNodeClassStr != null && parentNodeClassStr.equals("ReturnStmt")) {
				this.generateReturn6(parent, node, currentHole, parentHole, parentOfParentHole, holeTypeExpr);
			} else if (parentNodeClassStr != null && parentNodeClassStr.equals("ExpressionStmt")) {
				this.generateExpForExpressionStmt(parent, node, holeIndex, currentHole, parentOfParentHole, holeTypeExpr);
			} else if (parentNodeClassStr != null && parentNodeClassStr.equals("AssignExpr")) {
				this.generateExprInAssignExpr(parent, node, currentHole, parentOfParentHole, holeTypeExpr);
			} else if (parentHoleType.equals(HoleType.Arguments)) {
				this.generateExprInArguments(parent, node, currentHole, parentHole, holeTypeExpr);
			} else if (parentNodeClassStr != null && parentNodeClassStr.equals("ExpressionStmt")) {
				// body part, expression
				this.generateExpForExpressionStmt(parent, node, holeIndex, currentHole, parentOfParentHole, holeTypeExpr);
			} else if (parentNodeClassStr != null && parentNodeClassStr.equals("EnclosedExpr")) {
				this.generateExprForEnclosedExpr(parent, node, currentHole, parentOfParentHole, holeTypeExpr);
			}
			break;
		case "expr5":
			holeTypeExpr = HoleType.Expr5;
			this.generateExprForExpr5AndExpr14(parent, node, parentNodeClassStr, holeIndex, currentHole, parentHole,
					parentOfParentHole, parentOfParentOfParentHole, parentHoleType, holeTypeExpr);
			break;
		case "expr6":
			holeTypeExpr = HoleType.Expr6;
			if (parentHoleType.equals(HoleType.Statements)) {
				this.generateExpInStatements(parent, holeIndex, node, currentHole, parentHole, holeTypeExpr);
			} else if (parentNodeClassStr != null && parentNodeClassStr.equals("MethodDeclaration")) {
				this.generateExpInMethodBody(parent, currentHole, node, holeTypeExpr);
			} else if (parentNodeClassStr != null && parentNodeClassStr.equals("BinaryExpr")) {
				this.generateBinarExprInExpr(parent, holeIndex, node, currentHole, parentHole, parentOfParentHole,
						parentOfParentOfParentHole, holeTypeExpr);
			} else if (parentNodeClassStr != null && parentNodeClassStr.equals("ForStmt")) {
				this.generateExprStmtInForStmtForPlusPLus(parent, currentHole, node, holeTypeExpr, holeIndex, parentHole);
			} else if (parentNodeClassStr != null && parentNodeClassStr.equals("WhileStmt")) {
				this.generateExprStmtInWhileStmt(parent, currentHole, node, holeTypeExpr);
			} else if (parentNodeClassStr != null && parentNodeClassStr.equals("IfStmt")) {
				this.generateThenStmtInIfStmt(parent, node, currentHole, holeTypeExpr);
			} else if (parentNodeClassStr != null && parentNodeClassStr.equals("BlockStmt")) {
				this.generateBlockStmt(parent, node, currentHole, holeIndex, parentHole, holeTypeExpr);
			} else if (parentNodeClassStr != null && parentNodeClassStr.equals("SwitchEntry")) {
				this.generateSwitchEntry(parent, node, holeIndex, currentHole, holeTypeExpr);
			} else if (parentNodeClassStr != null && parentNodeClassStr.equals("ReturnStmt")) {
				this.generateReturn6(parent, node, currentHole, parentHole, parentOfParentHole, holeTypeExpr);
			} else if (parentNodeClassStr != null && parentNodeClassStr.equals("AssignExpr")) {
				this.generateExprInAssignExpr(parent, node, currentHole, parentOfParentHole, holeTypeExpr);
			} else if (parentHoleType.equals(HoleType.Arguments)) {
				this.generateExprInArguments(parent, node, currentHole, parentHole, holeTypeExpr);
			} else if (parentNodeClassStr != null && parentNodeClassStr.equals("ExpressionStmt")) {
				// body part, expression
				this.generateExpForExpressionStmt(parent, node, holeIndex, currentHole, parentOfParentHole, holeTypeExpr);
			} else if (parentNodeClassStr != null && parentNodeClassStr.equals("EnclosedExpr")) {
				this.generateExprForEnclosedExpr(parent, node, currentHole, parentOfParentHole, holeTypeExpr);
			}
			break;
		case "expr7":
			holeTypeExpr = HoleType.Expr7;
			if (parentHoleType.equals(HoleType.Statements)) {
				this.generateExpInStatements(parent, holeIndex, node, currentHole, parentHole, holeTypeExpr);
			} else if (parentNodeClassStr != null && parentNodeClassStr.equals("MethodDeclaration")) {
				this.generateExpInMethodBody(parent, currentHole, node, holeTypeExpr);
			} else if (parentNodeClassStr != null && parentNodeClassStr.equals("BinaryExpr")) {
				this.generateBinarExprInExpr(parent, holeIndex, node, currentHole, parentHole, parentOfParentHole,
						parentOfParentOfParentHole, holeTypeExpr);
			} else if (parentNodeClassStr != null && parentNodeClassStr.equals("ForStmt")) {
				this.generateExprStmtInForStmtForPlusPLus(parent, currentHole, node, holeTypeExpr, holeIndex, parentHole);
			} else if (parentNodeClassStr != null && parentNodeClassStr.equals("WhileStmt")) {
				this.generateExprStmtInWhileStmt(parent, currentHole, node, holeTypeExpr);
			} else if (parentNodeClassStr != null && parentNodeClassStr.equals("IfStmt")) {
				this.generateThenStmtInIfStmt(parent, node, currentHole, holeTypeExpr);
			} else if (parentNodeClassStr != null && parentNodeClassStr.equals("BlockStmt")) {
				this.generateBlockStmt(parent, node, currentHole, holeIndex, parentHole, holeTypeExpr);
			} else if (parentNodeClassStr != null && parentNodeClassStr.equals("SwitchEntry")) {
				this.generateSwitchEntry(parent, node, holeIndex, currentHole, holeTypeExpr);
			} else if (parentNodeClassStr != null && parentNodeClassStr.equals("ReturnStmt")) {
				this.generateReturn6(parent, node, currentHole, parentHole, parentOfParentHole, holeTypeExpr);
			} else if (parentNodeClassStr != null && parentNodeClassStr.equals("AssignExpr")) {
				this.generateExprInAssignExpr(parent, node, currentHole, parentOfParentHole, holeTypeExpr);
			} else if (parentHoleType.equals(HoleType.Arguments)) {
				this.generateExprInArguments(parent, node, currentHole, parentHole, holeTypeExpr);
			} else if (parentNodeClassStr != null && parentNodeClassStr.equals("ExpressionStmt")) {
				// body part, expression
				this.generateExpForExpressionStmt(parent, node, holeIndex, currentHole, parentOfParentHole, holeTypeExpr);
			} else if (parentNodeClassStr != null && parentNodeClassStr.equals("EnclosedExpr")) {
				this.generateExprForEnclosedExpr(parent, node, currentHole, parentOfParentHole, holeTypeExpr);
			}
			break;
		case "expr8":
			holeTypeExpr = HoleType.Expr8;
			if (parentHoleType.equals(HoleType.Statements)) {
				this.generateExpInStatements(parent, holeIndex, node, currentHole, parentHole, holeTypeExpr);
			} else if (parentNodeClassStr != null && parentNodeClassStr.equals("MethodDeclaration")) {
				this.generateExpInMethodBody(parent, currentHole, node, holeTypeExpr);
			} else if (parentNodeClassStr != null && parentNodeClassStr.equals("BinaryExpr")) {
				this.generateBinarExprInExpr(parent, holeIndex, node, currentHole, parentHole, parentOfParentHole,
						parentOfParentOfParentHole, holeTypeExpr);
			} else if (parentNodeClassStr != null && parentNodeClassStr.equals("ForStmt")) {
				this.generateExprStmtInForStmtForPlusPLus(parent, currentHole, node, holeTypeExpr, holeIndex, parentHole);
			} else if (parentNodeClassStr != null && parentNodeClassStr.equals("WhileStmt")) {
				this.generateExprStmtInWhileStmt(parent, currentHole, node, holeTypeExpr);
			} else if (parentNodeClassStr != null && parentNodeClassStr.equals("IfStmt")) {
				this.generateThenStmtInIfStmt(parent, node, currentHole, holeTypeExpr);
			} else if (parentNodeClassStr != null && parentNodeClassStr.equals("BlockStmt")) {
				this.generateBlockStmt(parent, node, currentHole, holeIndex, parentHole, holeTypeExpr);
			} else if (parentNodeClassStr != null && parentNodeClassStr.equals("SwitchEntry")) {
				this.generateSwitchEntry(parent, node, holeIndex, currentHole, holeTypeExpr);
			} else if (parentNodeClassStr != null && parentNodeClassStr.equals("ReturnStmt")) {
				this.generateReturn6(parent, node, currentHole, parentHole, parentOfParentHole, holeTypeExpr);
			} else if (parentNodeClassStr != null && parentNodeClassStr.equals("AssignExpr")) {
				this.generateExprInAssignExpr(parent, node, currentHole, parentOfParentHole, holeTypeExpr);
			} else if (parentHoleType.equals(HoleType.Arguments)) {
				this.generateExprInArguments(parent, node, currentHole, parentHole, holeTypeExpr);
			} else if (parentNodeClassStr != null && parentNodeClassStr.equals("ExpressionStmt")) {
				// body part, expression
				this.generateExpForExpressionStmt(parent, node, holeIndex, currentHole, parentOfParentHole, holeTypeExpr);
			} else if (parentNodeClassStr != null && parentNodeClassStr.equals("EnclosedExpr")) {
				this.generateExprForEnclosedExpr(parent, node, currentHole, parentOfParentHole, holeTypeExpr);
			}
			break;
		case "expr9":
			holeTypeExpr = HoleType.Expr9;
			if (parentHoleType.equals(HoleType.Statements)) {
				this.generateExpInStatements(parent, holeIndex, node, currentHole, parentHole, holeTypeExpr);
			} else if (parentNodeClassStr != null && parentNodeClassStr.equals("MethodDeclaration")) {
				this.generateExpInMethodBody(parent, currentHole, node, holeTypeExpr);
			} else if (parentNodeClassStr != null && parentNodeClassStr.equals("BinaryExpr")) {
				this.generateBinarExprInExpr(parent, holeIndex, node, currentHole, parentHole, parentOfParentHole,
						parentOfParentOfParentHole, holeTypeExpr);
			} else if (parentNodeClassStr != null && parentNodeClassStr.equals("ForStmt")) {
				this.generateExprStmtInForStmtForPlusPLus(parent, currentHole, node, holeTypeExpr, holeIndex, parentHole);
			} else if (parentNodeClassStr != null && parentNodeClassStr.equals("WhileStmt")) {
				this.generateExprStmtInWhileStmt(parent, currentHole, node, holeTypeExpr);
			} else if (parentNodeClassStr != null && parentNodeClassStr.equals("IfStmt")) {
				this.generateThenStmtInIfStmt(parent, node, currentHole, holeTypeExpr);
			} else if (parentNodeClassStr != null && parentNodeClassStr.equals("BlockStmt")) {
				this.generateBlockStmt(parent, node, currentHole, holeIndex, parentHole, holeTypeExpr);
			} else if (parentNodeClassStr != null && parentNodeClassStr.equals("SwitchEntry")) {
				this.generateSwitchEntry(parent, node, holeIndex, currentHole, holeTypeExpr);
			} else if (parentNodeClassStr != null && parentNodeClassStr.equals("ReturnStmt")) {
				this.generateReturn6(parent, node, currentHole, parentHole, parentOfParentHole, holeTypeExpr);
			} else if (parentNodeClassStr != null && parentNodeClassStr.equals("AssignExpr")) {
				this.generateExprInAssignExpr(parent, node, currentHole, parentOfParentHole, holeTypeExpr);
			} else if (parentHoleType.equals(HoleType.Arguments)) {
				this.generateExprInArguments(parent, node, currentHole, parentHole, holeTypeExpr);
			} else if (parentNodeClassStr != null && parentNodeClassStr.equals("ExpressionStmt")) {
				// body part, expression
				this.generateExpForExpressionStmt(parent, node, holeIndex, currentHole, parentOfParentHole, holeTypeExpr);
			} else if (parentNodeClassStr != null && parentNodeClassStr.equals("EnclosedExpr")) {
				this.generateExprForEnclosedExpr(parent, node, currentHole, parentOfParentHole, holeTypeExpr);
			}
			break;
		case "expr10":
			holeTypeExpr = HoleType.Expr10;
			if (parentHoleType.equals(HoleType.Statements)) {
				NodeList<Statement> statements = (NodeList<Statement>) parent.get().get();
				if (holeIndex < statements.size()) {
					// TODO
				} else {
					statements.add(new ExpressionStmt((Expression) node));

					currentHole.set(HoleType.Expression, false);

					HoleNode holdeNodeChild0 = new HoleNode(HoleType.Wrapper, false);
					holdeNodeChild0.setHoleTypeOptionsOfOnlyOne(holeTypeExpr);
					currentHole.addChild(holdeNodeChild0);

					HoleNode holeNodeChild = new HoleNode();
					holeNodeChild.setHoleTypeOptionsOfOnlyOne(HoleType.Expression);
					holdeNodeChild0.addChild(holeNodeChild);
				}
			} else if (parentNodeClassStr != null && parentNodeClassStr.equals("MethodDeclaration")) {
				this.generateExpInMethodBody(parent, currentHole, node, HoleType.Expr10);
			} else if (parentNodeClassStr != null && parentNodeClassStr.equals("BinaryExpr")) {
				BinaryExpr binaryExpr = (BinaryExpr) parent.getLeft();
				if ((parentHole.getHoleTypeOfOptionsIfOnlyOne() != null
						&& parentHole.getHoleTypeOfOptionsIfOnlyOne().equals(HoleType.Expr11))
						|| parentOfParentHole.getHoleTypeOfOptionsIfOnlyOne().equals(HoleType.Expr11)) {
					binaryExpr.setRight((Expression) node);

					currentHole.set(HoleType.RightSubExpr, false);
					holeNode = new HoleNode(HoleType.Wrapper, false);
					holeNode.setHoleTypeOptionsOfOnlyOne(holeTypeExpr);
					currentHole.addChild(holeNode);
					holeNode.addChild(new HoleNode());
				} else if ((parentHole.getHoleTypeOfOptionsIfOnlyOne() != null
						&& parentHole.getHoleTypeOfOptionsIfOnlyOne().equals(HoleType.Expr10))
						|| parentOfParentHole.getHoleTypeOfOptionsIfOnlyOne().equals(HoleType.Expr10)) {
					if (holeIndex == 0) {
						// left
						binaryExpr.setLeft((Expression) node);
						currentHole.set(HoleType.LeftSubExpr, false);
						HoleNode anotherCurrentHole = new HoleNode(HoleType.Wrapper, false);
						anotherCurrentHole.setHoleTypeOptionsOfOnlyOne(holeTypeExpr);
						currentHole.addChild(anotherCurrentHole);
						anotherCurrentHole.addChild(new HoleNode());
					} else {
						// right
						binaryExpr.setRight((Expression) node);
						currentHole.set(HoleType.RightSubExpr, false);
						HoleNode anotherCurrentHole = new HoleNode(HoleType.Wrapper, false);
						anotherCurrentHole.setHoleTypeOptionsOfOnlyOne(holeTypeExpr);
						currentHole.addChild(anotherCurrentHole);
						anotherCurrentHole.addChild(new HoleNode());
					}
				}
			} else if (parentNodeClassStr != null && parentNodeClassStr.equals("ForStmt")) {
				// Note: we only support BlockStmt.
				// https://www.javadoc.io/static/com.github.javaparser/javaparser-core/3.23.1/com/github/javaparser/ast/stmt/ForStmt.html
				// _ op _ in for(; i < 10 ;){ ;_ op _; }
				ForStmt forStmt = (ForStmt) parent.getLeft();
				Statement body = forStmt.getBody();
				String bodyClassStr = body.getClass().toString();
				bodyClassStr = StringHelper.getClassName(bodyClassStr);
				if (bodyClassStr.equals("ReturnStmt")) {
					BlockStmt blockStmt = new BlockStmt();
					NodeList<Statement> statements = new NodeList<Statement>();
					ExpressionStmt expressionStmt = new ExpressionStmt((Expression) node);
					statements.add(expressionStmt);
					blockStmt.setStatements(statements);
					forStmt.setBody(blockStmt);

					currentHole.set(HoleType.Body, false);

					HoleNode anotherCurrentHole = new HoleNode();
					anotherCurrentHole.set(HoleType.Statements, false);
					currentHole.addChild(anotherCurrentHole);

					HoleNode childNode = new HoleNode(HoleType.Wrapper, false);
					childNode.setHoleTypeOptions(new HoleType[] { HoleType.Statement });
					anotherCurrentHole.addChild(childNode);

					HoleNode anotherChildNode = new HoleNode();
					anotherChildNode.set(HoleType.Expression, false);
					childNode.addChild(anotherChildNode);

					HoleNode childOfanotherChildNode = new HoleNode(HoleType.Wrapper, false);
					childOfanotherChildNode.setHoleTypeOptionsOfOnlyOne(holeTypeExpr);
					anotherChildNode.addChild(childOfanotherChildNode);

					HoleNode newHole = new HoleNode();
					newHole.setHoleTypeOptionsOfOnlyOne(HoleType.Expression);
					childOfanotherChildNode.addChild(newHole);
				} else if (bodyClassStr.equals("BlockStmt")) {

				} else {
					System.out.println("Should not go to this branch");
				}
			} else if (parentNodeClassStr != null && parentNodeClassStr.equals("WhileStmt")) {
				WhileStmt stmt = (WhileStmt) parent.getLeft();
				Statement body = stmt.getBody();
				String bodyClassStr = body.getClass().toString();
				bodyClassStr = StringHelper.getClassName(bodyClassStr);
				if (bodyClassStr.equals("ReturnStmt")) {
					BlockStmt blockStmt = new BlockStmt();
					NodeList<Statement> statements = new NodeList<Statement>();
					ExpressionStmt expressionStmt = new ExpressionStmt((Expression) node);
					statements.add(expressionStmt);
					blockStmt.setStatements(statements);
					stmt.setBody(blockStmt);

					currentHole.set(HoleType.Body, false);
					HoleNode anotherCurrentHole = new HoleNode(HoleType.Statements, false);
					currentHole.addChild(anotherCurrentHole);

					HoleNode childNode = new HoleNode(HoleType.Wrapper, false);
					childNode.setHoleTypeOptions(new HoleType[] { HoleType.Statement });
					anotherCurrentHole.addChild(childNode);

					HoleNode anotherChildNode = new HoleNode(HoleType.Expression, false);
					childNode.addChild(anotherChildNode);

					HoleNode childOfanotherChildNode = new HoleNode(HoleType.Wrapper, false);
					childOfanotherChildNode.setHoleTypeOptionsOfOnlyOne(holeTypeExpr);
					anotherChildNode.addChild(childOfanotherChildNode);

					HoleNode newHole = new HoleNode();
					newHole.setHoleTypeOptionsOfOnlyOne(HoleType.Expression);
					childOfanotherChildNode.addChild(newHole);
				} else if (bodyClassStr.equals("BlockStmt")) {

				} else {
					System.out.println("Should not go to this branch");
				}
			} else if (parentNodeClassStr != null && parentNodeClassStr.equals("IfStmt")) {
				IfStmt ifStmt = (IfStmt) parent.getLeft();
				if (parentHoleType.equals(HoleType.Wrapper) && parentHole.getChildList().size() == 1) {
					// parentHole.getChildList().size() == 1 means one child hole, it shall be
					// condition for the if.
					// If condition
					ifStmt.setCondition((Expression) node);
					currentHole.set(HoleType.Expression, false);
					currentHole.setHoleTypeOptions(new HoleType[] { HoleType.IfCondition });
					parentHole.addChild(new HoleNode());
				} else if (parentHole.getChildList().size() == 2) {
					// then branch
					this.generateThenStmtInIfStmtForExpr10AndExpr11AndLet6(parent, node, currentHole, holeTypeExpr);
				} else if (parentHole.getChildList().size() > 2) {
					// else branch
					IfStmt elseBranch = new IfStmt();
					elseBranch.setCondition((Expression) node);
					ifStmt.setElseStmt(elseBranch);

					currentHole.set(HoleType.ElseStatement, false);
					HoleNode holeNodeChild = new HoleNode(HoleType.Expression, false);
					currentHole.addChild(holeNodeChild);
					currentHole.addChild(new HoleNode());
				}
			} else if (parentNodeClassStr != null && parentNodeClassStr.equals("SwitchEntry")) {
				this.generateSwitchEntryForExpr10AndExpr11AndLet6(parent, node, holeIndex, currentHole, holeTypeExpr);
			} else if (parentNodeClassStr != null && parentNodeClassStr.equals("ReturnStmt")) {
				this.generateReturn6ForExpr10AndExpr11(parent, node, currentHole, parentHole, parentOfParentHole, holeTypeExpr);
			} else if (parentNodeClassStr != null && parentNodeClassStr.equals("AssignExpr")) {
				this.generateExprInAssignExprForExpr10AndExpr11(parent, node, currentHole, parentOfParentHole, holeTypeExpr);
			} else if (parentNodeClassStr != null && parentNodeClassStr.equals("EnclosedExpr")) {
				EnclosedExpr enclosedExpr = (EnclosedExpr) parent.getLeft();
				enclosedExpr.setInner((Expression) node);
				currentHole.set(HoleType.InnerExpr, false);

				HoleNode exprHole = new HoleNode(HoleType.Wrapper, false);
				exprHole.setHoleTypeOptionsOfOnlyOne(holeTypeExpr);
				currentHole.addChild(exprHole);
				exprHole.addChild(new HoleNode());
			}
			break;
		case "expr11":
			holeTypeExpr = HoleType.Expr11;
			if (parentHoleType.equals(HoleType.Statements)) {
				NodeList<Statement> statements = (NodeList<Statement>) parent.get().get();
				if (holeIndex < statements.size()) {
					// TODO
				} else {
					statements.add(new ExpressionStmt((Expression) node));

					currentHole.set(HoleType.Wrapper, false);
					currentHole.setHoleTypeOptionsOfOnlyOne(holeTypeExpr);
					HoleNode holdeNodeChild0 = new HoleNode(HoleType.Expression, false);
					currentHole.addChild(holdeNodeChild0);

					HoleNode holeNodeChild = new HoleNode();
					holeNodeChild.setHoleTypeOptionsOfOnlyOne(HoleType.Expression);
					holdeNodeChild0.addChild(holeNodeChild);
				}
			} else if (parentNodeClassStr != null && parentNodeClassStr.equals("MethodDeclaration")) {
				this.generateExpInMethodBody(parent, currentHole, node, holeTypeExpr);
			} else if (parentNodeClassStr != null && parentNodeClassStr.equals("BinaryExpr")) {
				BinaryExpr binaryExpr = (BinaryExpr) parent.getLeft();
				if (parentHole.getHoleType().equals(HoleType.LeftSubExpr)) {
					binaryExpr.setLeft((Expression) node);
					currentHole.set(holeTypeExpr, false);
					holeNode = new HoleNode();
					parentOfParentOfParentHole.addChild(holeNode);
				} else if (parentHole.getHoleType().equals(HoleType.RightSubExpr)) {
					binaryExpr.setRight((Expression) node);
					currentHole.set(HoleType.Expression, false);
					holeNode = new HoleNode();
					parentOfParentOfParentHole.addChild(holeNode);
				} else if ((parentHole.getHoleTypeOfOptionsIfOnlyOne() != null
						&& parentHole.getHoleTypeOfOptionsIfOnlyOne().equals(HoleType.Expr11))
						|| parentOfParentHole.getHoleTypeOfOptionsIfOnlyOne().equals(HoleType.Expr11)) {
					binaryExpr.setRight((Expression) node);
					currentHole.set(HoleType.Wrapper, false);
					currentHole.setHoleTypeOptionsOfOnlyOne(holeTypeExpr);

					HoleNode holeNodeChild = new HoleNode();
					currentHole.addChild(holeNodeChild);
				} else if ((parentHole.getHoleTypeOfOptionsIfOnlyOne() != null
						&& parentHole.getHoleTypeOfOptionsIfOnlyOne().equals(HoleType.Expr10))
						|| parentOfParentHole.getHoleTypeOfOptionsIfOnlyOne().equals(HoleType.Expr10)) {
					if (holeIndex == 0) {
						// left
						binaryExpr.setLeft((Expression) node);
						currentHole.set(HoleType.LeftSubExpr, false);
						HoleNode anotherCurrentHole = new HoleNode(HoleType.Wrapper, false);
						anotherCurrentHole.setHoleTypeOptionsOfOnlyOne(holeTypeExpr);
						currentHole.addChild(anotherCurrentHole);
						anotherCurrentHole.addChild(new HoleNode());
					} else {
						// right
						binaryExpr.setRight((Expression) node);
						currentHole.set(HoleType.RightSubExpr, false);
						HoleNode anotherCurrentHole = new HoleNode(HoleType.Wrapper, false);
						anotherCurrentHole.setHoleTypeOptionsOfOnlyOne(holeTypeExpr);
						currentHole.addChild(anotherCurrentHole);
						anotherCurrentHole.addChild(new HoleNode());
					}
				}
			} else if (parentNodeClassStr != null && parentNodeClassStr.equals("ForStmt")) {
				if (holeIndex == 1) {
					// i < subexpression in for(; i < subexpression ;){}
					ForStmt forStmt = (ForStmt) parent.getLeft();
					forStmt.setCompare((Expression) node);

					currentHole.set(HoleType.ForCompare, false);

					HoleNode anOtherCurrentHole = new HoleNode(HoleType.Wrapper, false);
					anOtherCurrentHole.setHoleTypeOptionsOfOnlyOne(holeTypeExpr);
					currentHole.addChild(anOtherCurrentHole);

					HoleNode holeNodeChild = new HoleNode();
					holeNodeChild.setHoleTypeOptionsOfOnlyOne(HoleType.Expression);
					anOtherCurrentHole.addChild(holeNodeChild);
				} else {
					// Note: we only support BlockStmt.
					// https://www.javadoc.io/static/com.github.javaparser/javaparser-core/3.23.1/com/github/javaparser/ast/stmt/ForStmt.html
					// _ op _ in for(; i < 10 ;){ ;_ op _; }
					ForStmt forStmt = (ForStmt) parent.getLeft();
					Statement body = forStmt.getBody();
					String bodyClassStr = body.getClass().toString();
					bodyClassStr = StringHelper.getClassName(bodyClassStr);
					if (bodyClassStr.equals("ReturnStmt")) {
						BlockStmt blockStmt = new BlockStmt();
						NodeList<Statement> statements = new NodeList<Statement>();
						ExpressionStmt expressionStmt = new ExpressionStmt((Expression) node);
						statements.add(expressionStmt);
						blockStmt.setStatements(statements);
						forStmt.setBody(blockStmt);

						currentHole.set(HoleType.Body, false);
						HoleNode anotherCurrentHole = new HoleNode(HoleType.Statements, false);
						currentHole.addChild(anotherCurrentHole);

						HoleNode childNode = new HoleNode(HoleType.Wrapper, false);
						childNode.setHoleTypeOptions(new HoleType[] { HoleType.Statement });
						anotherCurrentHole.addChild(childNode);

						HoleNode anotherChildNode = new HoleNode(HoleType.Expression, false);
						childNode.addChild(anotherChildNode);

						HoleNode childOfanotherChildNode = new HoleNode(HoleType.Wrapper, false);
						childOfanotherChildNode.setHoleTypeOptionsOfOnlyOne(holeTypeExpr);
						anotherChildNode.addChild(childOfanotherChildNode);

						HoleNode newHole = new HoleNode();
						newHole.setHoleTypeOptionsOfOnlyOne(HoleType.Expression);
						childOfanotherChildNode.addChild(newHole);
					} else if (bodyClassStr.equals("BlockStmt")) {

					} else {
						System.out.println("Should not go to this branch");
					}
				}
			} else if (parentNodeClassStr != null && parentNodeClassStr.equals("WhileStmt")) {
				WhileStmt stmt = (WhileStmt) parent.getLeft();
				Statement body = stmt.getBody();
				String bodyClassStr = body.getClass().toString();
				bodyClassStr = StringHelper.getClassName(bodyClassStr);
				if (bodyClassStr.equals("ReturnStmt")) {
					BlockStmt blockStmt = new BlockStmt();
					NodeList<Statement> statements = new NodeList<Statement>();
					ExpressionStmt expressionStmt = new ExpressionStmt((Expression) node);
					statements.add(expressionStmt);
					blockStmt.setStatements(statements);
					stmt.setBody(blockStmt);

					currentHole.set(HoleType.Body, false);
					HoleNode anotherCurrentHole = new HoleNode(HoleType.Statements, false);
					currentHole.addChild(anotherCurrentHole);

					HoleNode childNode = new HoleNode(HoleType.Wrapper, false);
					childNode.setHoleTypeOptions(new HoleType[] { HoleType.Statement });
					anotherCurrentHole.addChild(childNode);

					HoleNode anotherChildNode = new HoleNode(HoleType.Expression, false);
					childNode.addChild(anotherChildNode);

					HoleNode childOfanotherChildNode = new HoleNode(HoleType.Wrapper, false);
					childOfanotherChildNode.setHoleTypeOptionsOfOnlyOne(holeTypeExpr);
					anotherChildNode.addChild(childOfanotherChildNode);

					HoleNode newHole = new HoleNode();
					newHole.setHoleTypeOptionsOfOnlyOne(HoleType.Expression);
					childOfanotherChildNode.addChild(newHole);
				} else if (bodyClassStr.equals("BlockStmt")) {

				} else {
					System.out.println("Should not go to this branch");
				}
			} else if (parentNodeClassStr != null && parentNodeClassStr.equals("IfStmt")) {
				IfStmt ifStmt = (IfStmt) parent.getLeft();
				if (parentHoleType.equals(HoleType.Wrapper) && parentHole.getChildList().size() == 1) {
					// parentHole.getChildList().size() == 1 means one child hole, it shall be
					// condition for the if.
					// If condition
					ifStmt.setCondition((Expression) node);
					currentHole.set(HoleType.Expression, false);
					currentHole.setHoleTypeOptions(new HoleType[] { HoleType.IfCondition });
					parentHole.addChild(new HoleNode());
				} else if (parentHole.getChildList().size() == 2) {
					// then branch
					this.generateThenStmtInIfStmtForExpr10AndExpr11AndLet6(parent, node, currentHole, holeTypeExpr);
				} else if (parentHole.getChildList().size() > 2) {
					// else branch
					IfStmt elseBranch = new IfStmt();
					elseBranch.setCondition((Expression) node);
					ifStmt.setElseStmt(elseBranch);
					currentHole.set(HoleType.ElseStatement, false);

					HoleNode holeNodeChild = new HoleNode(HoleType.Expression, false);
					currentHole.addChild(holeNodeChild);
					currentHole.addChild(new HoleNode());
				}
			} else if (parentNodeClassStr != null && parentNodeClassStr.equals("SwitchEntry")) {
				this.generateSwitchEntryForExpr10AndExpr11AndLet6(parent, node, holeIndex, currentHole, holeTypeExpr);
			} else if (parentNodeClassStr != null && parentNodeClassStr.equals("ReturnStmt")) {
				this.generateReturn6ForExpr10AndExpr11(parent, node, currentHole, parentHole, parentOfParentHole, holeTypeExpr);
			} else if (parentNodeClassStr != null && parentNodeClassStr.equals("AssignExpr")) {
				this.generateExprInAssignExprForExpr10AndExpr11(parent, node, currentHole, parentOfParentHole, holeTypeExpr);
			} else if (parentNodeClassStr != null && parentNodeClassStr.equals("ExpressionStmt")) {
				ExpressionStmt expressionStmt = (ExpressionStmt) parent.getLeft();
				String expressionClassStr = StringHelper.getClassName(expressionStmt.getExpression().getClass().toString());
				if (expressionClassStr.equals("VariableDeclarationExpr")) {
					VariableDeclarationExpr variableDeclarationExpr = (VariableDeclarationExpr) expressionStmt.getExpression();
					NodeList<VariableDeclarator> variableDeclarators = variableDeclarationExpr.getVariables();
					variableDeclarators.get(0).setInitializer((Expression) node);

					currentHole.set(HoleType.Expression, false);

					HoleNode variablesHole = new HoleNode(HoleType.VariableDeclarator, false);
					currentHole.addChild(variablesHole);

					HoleNode variableHole = new HoleNode(HoleType.VariableInitializer, false);
					variablesHole.addChild(variableHole);

					HoleNode exprHole = new HoleNode(HoleType.Wrapper, false);
					exprHole.setHoleTypeOptionsOfOnlyOne(holeTypeExpr);
					variableHole.addChild(exprHole);
					exprHole.addChild(new HoleNode());
				} else if (expressionClassStr.equals("AssignExpr")) {
					AssignExpr assignExpr = (AssignExpr) expressionStmt.getExpression();
					assignExpr.setValue((Expression) node);

					currentHole.set(HoleType.Expression, false);
					HoleNode valueHole = new HoleNode(HoleType.AssignExprValue, false);
					currentHole.addChild(valueHole);

					HoleNode exprHole = new HoleNode(HoleType.Wrapper, false);
					exprHole.setHoleTypeOptionsOfOnlyOne(holeTypeExpr);
					valueHole.addChild(exprHole);
					exprHole.addChild(new HoleNode());
				}
			} else if (parentNodeClassStr != null && parentNodeClassStr.equals("EnclosedExpr")) {
				EnclosedExpr enclosedExpr = (EnclosedExpr) parent.getLeft();
				enclosedExpr.setInner((Expression) node);
				currentHole.set(HoleType.InnerExpr, false);
				HoleNode exprHole = new HoleNode(HoleType.Wrapper, false);
				exprHole.setHoleTypeOptionsOfOnlyOne(holeTypeExpr);
				currentHole.addChild(exprHole);
				exprHole.addChild(new HoleNode());
			}
			break;
		case "expr12":
			holeTypeExpr = HoleType.Expr12;
			if (parentHoleType.equals(HoleType.Statements)) {
				this.generateExpInStatements(parent, holeIndex, node, currentHole, parentHole, holeTypeExpr);
			} else if (parentNodeClassStr != null && parentNodeClassStr.equals("ForStmt")) {
				if (holeIndex == 1) {
					// i < 10 in for(; i < 10 ;){}
					ForStmt forStmt = (ForStmt) parent.getLeft();
					forStmt.setCompare((Expression) node);

					currentHole.set(HoleType.Wrapper, false, HoleType.ForCompare);
					HoleNode exprHole = new HoleNode(HoleType.Expression, false);
					currentHole.addChild(exprHole);
					HoleNode exprWrapper = new HoleNode(holeTypeExpr, false);
					exprHole.addChild(exprWrapper);
					parentHole.addChild(new HoleNode());
				} else {
					this.generateExprStmtInForStmt(parent, currentHole, node, holeTypeExpr);
				}
			} else if (parentNodeClassStr != null && parentNodeClassStr.equals("ExpressionStmt")) {
				// body part, expression
				this.generateExpForExpressionStmt(parent, node, holeIndex, currentHole, parentOfParentHole, holeTypeExpr);
			} else if (parentNodeClassStr != null && parentNodeClassStr.equals("WhileStmt")) {
				if (holeIndex == 0) {
					// i < 10 in while(i < 10){}
					WhileStmt whileStmt = (WhileStmt) parent.getLeft();
					whileStmt.setCondition((Expression) node);
					currentHole.set(HoleType.Expression, false);
					// holeNode = new HoleNode(HoleType.Body, true);
					parentHole.addChild(new HoleNode());
				} else {
					this.generateExprStmtInWhileStmt(parent, currentHole, node, holeTypeExpr);
				}
			} else if (parentNodeClassStr != null && parentNodeClassStr.equals("IfStmt")) {
				IfStmt ifStmt = (IfStmt) parent.getLeft();
				if (parentHoleType.equals(HoleType.Wrapper) && parentHole.getChildList().size() == 1) {
					// parentHole.getChildList().size() == 1 means one child hole, it shall be
					// condition for the if.
					// If condition
					ifStmt.setCondition((Expression) node);
					currentHole.set(HoleType.Expression, false);
					currentHole.setHoleTypeOptions(new HoleType[] { HoleType.IfCondition });
					parentHole.addChild(new HoleNode());
				} else if (parentHole.getChildList().size() == 2) {
					// then branch
					this.generateThenStmtInIfStmt(parent, node, currentHole, holeTypeExpr);
				} else if (parentHole.getChildList().size() > 2) {
					// else branch
					IfStmt elseBranch = new IfStmt();
					elseBranch.setCondition((Expression) node);
					ifStmt.setElseStmt(elseBranch);

					currentHole.set(HoleType.ElseStatement, false);
					HoleNode holeNodeChild = new HoleNode(HoleType.Expression, false);
					currentHole.addChild(holeNodeChild);
					currentHole.addChild(new HoleNode());
				}
			} else if (parentNodeClassStr != null && parentNodeClassStr.equals("BlockStmt")) {
				this.generateBlockStmt(parent, node, currentHole, holeIndex, parentHole, holeTypeExpr);
			} else if (parentNodeClassStr != null && parentNodeClassStr.equals("SwitchEntry")) {
				this.generateSwitchEntry(parent, node, holeIndex, currentHole, holeTypeExpr);
			} else if (parentNodeClassStr != null && parentNodeClassStr.equals("MethodDeclaration")) {
				this.generateExpInMethodBody(parent, currentHole, node, holeTypeExpr);
			} else if (parentNodeClassStr != null && parentNodeClassStr.equals("BinaryExpr")) {
				this.generateBinarExprInExpr(parent, holeIndex, node, currentHole, parentHole, parentOfParentHole,
						parentOfParentOfParentHole, holeTypeExpr);
			} else if (parentNodeClassStr != null && parentNodeClassStr.equals("ReturnStmt")) {
				this.generateReturn6(parent, node, currentHole, parentHole, parentOfParentHole, holeTypeExpr);
			} else if (parentNodeClassStr != null && parentNodeClassStr.equals("AssignExpr")) {
				this.generateExprInAssignExpr(parent, node, currentHole, parentOfParentHole, holeTypeExpr);
			} else if (parentNodeClassStr != null && parentNodeClassStr.equals("ExpressionStmt")) {
				// body part, expression
				this.generateExpForExpressionStmt(parent, node, holeIndex, currentHole, parentOfParentHole, holeTypeExpr);
			} else if (parentNodeClassStr != null && parentNodeClassStr.equals("EnclosedExpr")) {
				this.generateExprForEnclosedExpr(parent, node, currentHole, parentOfParentHole, holeTypeExpr);
			}
			break;
		case "expr13":
			holeTypeExpr = HoleType.Expr13;
			if (parentHoleType.equals(HoleType.Statements)) {
				this.generateExpInStatements(parent, holeIndex, node, currentHole, parentHole, holeTypeExpr);
			} else if (parentNodeClassStr != null && parentNodeClassStr.equals("MethodDeclaration")) {
				this.generateExpInMethodBody(parent, currentHole, node, holeTypeExpr);
			} else if (parentNodeClassStr != null && parentNodeClassStr.equals("BinaryExpr")) {
				this.generateBinarExprInExpr(parent, holeIndex, node, currentHole, parentHole, parentOfParentHole,
						parentOfParentOfParentHole, holeTypeExpr);
			} else if (parentNodeClassStr != null && parentNodeClassStr.equals("ForStmt")) {
				this.generateExprStmtInForStmt(parent, currentHole, node, holeTypeExpr);
			} else if (parentNodeClassStr != null && parentNodeClassStr.equals("WhileStmt")) {
				this.generateExprStmtInWhileStmt(parent, currentHole, node, holeTypeExpr);
			} else if (parentNodeClassStr != null && parentNodeClassStr.equals("IfStmt")) {
				this.generateThenStmtInIfStmt(parent, node, currentHole, holeTypeExpr);
			} else if (parentNodeClassStr != null && parentNodeClassStr.equals("BlockStmt")) {
				this.generateBlockStmt(parent, node, currentHole, holeIndex, parentHole, holeTypeExpr);
			} else if (parentNodeClassStr != null && parentNodeClassStr.equals("SwitchEntry")) {
				this.generateSwitchEntry(parent, node, holeIndex, currentHole, holeTypeExpr);
			} else if (parentNodeClassStr != null && parentNodeClassStr.equals("ExpressionStmt")) {
				this.generateExpForExpressionStmt(parent, node, holeIndex, currentHole, parentOfParentHole, holeTypeExpr);
			} else if (parentNodeClassStr != null && parentNodeClassStr.equals("ReturnStmt")) {
				this.generateReturn6(parent, node, currentHole, parentHole, parentOfParentHole, holeTypeExpr);
			} else if (parentNodeClassStr != null && parentNodeClassStr.equals("AssignExpr")) {
				this.generateExprInAssignExpr(parent, node, currentHole, parentOfParentHole, holeTypeExpr);
			} else if (parentHoleType.equals(HoleType.Arguments)) {
				this.generateExprInArguments(parent, node, currentHole, parentHole, holeTypeExpr);
			} else if (parentNodeClassStr != null && parentNodeClassStr.equals("EnclosedExpr")) {
				this.generateExprForEnclosedExpr(parent, node, currentHole, parentOfParentHole, holeTypeExpr);
			}
			break;
		case "expr14":
			holeTypeExpr = HoleType.Expr14;
			this.generateExprForExpr5AndExpr14(parent, node, parentNodeClassStr, holeIndex, currentHole, parentHole,
					parentOfParentHole, parentOfParentOfParentHole, parentHoleType, holeTypeExpr);
			break;
		}

		this.holeAST.cleverMove();

		if (this.isDebug) {
			System.out.println("[log] write to file");
		}
		this.lexicalPreseveToJavaFile();
		return this.compilationUnit;
	}

	public Pair<Node, Integer> getParentNodeOfHole(List<Integer> path) {
		int index;
		Node parent = this.compilationUnit;
		HoleNode parentHole = this.holeAST.getRoot();
		HoleNode parentOfParentHole = parentHole;
		for (index = 0; index < path.size() - 1; index++) {
			HoleNode temp = parentHole.getIthChild(path.get(index));
			parentOfParentHole = parentHole;
			parentHole = temp;

			HoleType holeType = parentHole.getHoleType();
			if (holeType.equals(HoleType.Wrapper)) {
				continue;
			}

			String name = TypeNameMap.map.get(holeType);

			Class parentClass = parent.getClass();
			Method method;
			try {
				int indexWithSameType = this.computeASTIndex(parentOfParentHole, parentHole, path.get(index));
				method = parentClass.getMethod(name);
				// Optional<NodeList> optional = (Optional<NodeList>) method.invoke(parent);
				try {
					NodeList nodeList = (NodeList) method.invoke(parent);
					parent = nodeList.get(indexWithSameType);

				} catch (Exception e) {
					try {
						List<?> nodeList = (List<?>) method.invoke(parent);
						parent = (Node) nodeList.get(indexWithSameType);
					} catch (Exception e2) {
						try {
							Optional<?> optionalData = (Optional<?>) method.invoke(parent);
							parent = (Node) optionalData.get();
						} catch (Exception e3) {
							parent = (Node) method.invoke(parent);
						}
					}
				}
				// if (optional.isEmpty()) {
				// System.out.println("something is wrong");
				// } else {
				// parent = optional.get().get(index);
				// }
			} catch (NoSuchMethodException e) {
				e.printStackTrace();
			} catch (SecurityException e) {
				e.printStackTrace();
			} catch (IllegalAccessException e) {
				e.printStackTrace();
			} catch (IllegalArgumentException e) {
				e.printStackTrace();
			} catch (InvocationTargetException e) {
				e.printStackTrace();
			}
		}
		return new Pair<Node, Integer>(parent, path.get(index));
	}

	public Pair<Either<Node, Either<List<?>, NodeList<?>>>, Integer> getParentOfHole(List<Integer> path,
			HoleType holeTypeOfHole) {
		int index;
		Node parent = this.compilationUnit;
		HoleNode parentHole = this.holeAST.getRoot();
		for (index = 0; index < path.size() - 1; index++) {
			HoleNode temp = parentHole.getIthChild(path.get(index));
			parentHole = temp;

			HoleType holeType = parentHole.getHoleType();
			if (holeType.equals(HoleType.Wrapper)) {
				continue;
			}

			String name = TypeNameMap.map.get(holeType);

			Class parentClass = parent.getClass();
			Method method;
			try {
				int indexWithSameType = this.computeASTIndex(parentHole, holeTypeOfHole, path.get(index));
				method = parentClass.getMethod(name);
				try {
					NodeList nodeList = (NodeList) method.invoke(parent);
					if (indexWithSameType < nodeList.size()) {
						parent = nodeList.get(indexWithSameType);
					} else {
						Either<List<?>, NodeList<?>> either = Either.right(nodeList);
						return new Pair<Either<Node, Either<List<?>, NodeList<?>>>, Integer>(Either.right(either), path.get(index));
					}

				} catch (Exception e) {
					try {
						List<?> nodeList = (List<?>) method.invoke(parent);
						if (indexWithSameType < nodeList.size()) {
							parent = (Node) nodeList.get(indexWithSameType);
						} else {
							Either<List<?>, NodeList<?>> either = Either.left(nodeList);
							return new Pair<Either<Node, Either<List<?>, NodeList<?>>>, Integer>(Either.right(either),
									path.get(index));
						}
					} catch (Exception e2) {
						try {
							Optional<?> optionalData = (Optional<?>) method.invoke(parent);
							parent = (Node) optionalData.get();
						} catch (Exception e3) {
							parent = (Node) method.invoke(parent);
						}
					}
				}
			} catch (NoSuchMethodException e) {
				e.printStackTrace();
			} catch (SecurityException e) {
				e.printStackTrace();
			} catch (IllegalAccessException e) {
				e.printStackTrace();
			} catch (IllegalArgumentException e) {
				e.printStackTrace();
			} catch (InvocationTargetException e) {
				e.printStackTrace();
			}
		}
		Either<Node, Either<List<?>, NodeList<?>>> either = Either.left(parent);
		return new Pair<Either<Node, Either<List<?>, NodeList<?>>>, Integer>(either, path.get(index));
	}

	public Pair<Either<Node, Either<List<?>, NodeList<?>>>, Integer> getParentOfHole(List<Integer> path) {
		int index;
		Node parent = this.compilationUnit;
		HoleNode parentHole = this.holeAST.getRoot();
		for (index = 0; index < path.size() - 1; index++) {
			HoleNode temp = parentHole.getIthChild(path.get(index));
			parentHole = temp;

			HoleType holeType = parentHole.getHoleType();
			if (holeType.equals(HoleType.Wrapper)) {
				continue;
			}

			int indexOfHole = path.get(index + 1);
			String name = TypeNameMap.map.get(holeType);
			Class parentClass = parent.getClass();
			Method method;
			try {
				method = parentClass.getMethod(name);
				try {

					NodeList nodeList = (NodeList) method.invoke(parent);
					if (indexOfHole < nodeList.size()) {
						parent = nodeList.get(indexOfHole);
					} else {
						Either<List<?>, NodeList<?>> either = Either.right(nodeList);
						return new Pair<Either<Node, Either<List<?>, NodeList<?>>>, Integer>(Either.right(either), indexOfHole);
					}

				} catch (Exception e) {
					try {
						List<?> nodeList = (List<?>) method.invoke(parent);
						if (indexOfHole < nodeList.size()) {
							parent = (Node) nodeList.get(indexOfHole);
						} else {
							Either<List<?>, NodeList<?>> either = Either.left(nodeList);
							return new Pair<Either<Node, Either<List<?>, NodeList<?>>>, Integer>(Either.right(either), indexOfHole);
						}
					} catch (Exception e2) {
						try {
							Optional<?> optionalData = (Optional<?>) method.invoke(parent);
							parent = (Node) optionalData.get();
						} catch (Exception e3) {
							parent = (Node) method.invoke(parent);
						}
					}
				}
			} catch (NoSuchMethodException e) {
				e.printStackTrace();
			} catch (SecurityException e) {
				e.printStackTrace();
			} catch (IllegalAccessException e) {
				e.printStackTrace();
			} catch (IllegalArgumentException e) {
				e.printStackTrace();
			} catch (InvocationTargetException e) {
				e.printStackTrace();
			}
		}
		Either<Node, Either<List<?>, NodeList<?>>> either = Either.left(parent);
		return new Pair<Either<Node, Either<List<?>, NodeList<?>>>, Integer>(either, path.get(index));
	}

	public void lexicalPreseveToJavaFile() {
		LexicalPreservingPrinter.setup(this.compilationUnit);
		FileWriter filewriter;
		try {
			filewriter = new FileWriter("Test.java");
			filewriter.write(compilationUnit.toString());
			filewriter.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private int computeASTIndex(HoleNode parentOfParent, HoleNode parent, int currentIndex) {
		List<HoleNode> childList = parentOfParent.getChildList();
		int count = 0;
		for (int i = 0; i <= currentIndex; i++) {
			if (childList.get(i).getHoleType().equals(parent.getHoleType())) {
				count++;
			}
		}

		return --count;
	}

	private int computeASTIndex(HoleNode parent, HoleType holeTypeOfHole, int currentIndex) {
		List<HoleNode> childList = parent.getChildList();
		int count = 0;
		for (int i = 0; i <= currentIndex; i++) {
			if (childList.get(i).getHoleType().equals(holeTypeOfHole)) {
				count++;
			}
		}

		return --count;
	}

	private void generateExpInMethodBody(Either<Node, Either<List<?>, NodeList<?>>> parent, HoleNode currentHole,
			Node node, HoleType exprHoleType) {
		MethodDeclaration mNode = (MethodDeclaration) parent.getLeft();
		Optional<BlockStmt> optionalBody = mNode.getBody();
		currentHole.set(HoleType.Body, false);

		HoleNode anotherCurrentHole = new HoleNode();
		currentHole.addChild(anotherCurrentHole);

		BlockStmt blockStmt = optionalBody.get();
		NodeList<Statement> statements = blockStmt.getStatements();
		if (statements.size() == 0) {
			ExpressionStmt expressionStmt = new ExpressionStmt((Expression) node);
			statements.add(expressionStmt);

			anotherCurrentHole.set(HoleType.Statements, false);

			HoleNode holeNode = new HoleNode(HoleType.Wrapper, false);
			holeNode.setHoleTypeOptions(new HoleType[] { exprHoleType });
			anotherCurrentHole.addChild(holeNode);

			HoleNode holdeNodeChild0 = new HoleNode(HoleType.Expression, false);
			holeNode.addChild(holdeNodeChild0);

			HoleNode holeNodeChild = new HoleNode();
			holeNodeChild.setHoleTypeOptions(new HoleType[] { HoleType.Expression });
			anotherCurrentHole.addChild(holeNodeChild);
		} else {
			// TODO
		}
	}

	private void generateExpInStatements(Either<Node, Either<List<?>, NodeList<?>>> parent, int holeIndex, Node node,
			HoleNode currentHole, HoleNode parentHole, HoleType exprHoleType) {
		NodeList<Statement> statements = (NodeList<Statement>) parent.get().get();
		if (holeIndex < statements.size()) {
			// TODO
		} else {
			statements.add(new ExpressionStmt((Expression) node));

			currentHole.set(HoleType.Expression, false);
			HoleNode holdeNodeChild0 = new HoleNode(HoleType.Wrapper, false);
			holdeNodeChild0.setHoleTypeOptionsOfOnlyOne(exprHoleType);
			currentHole.addChild(holdeNodeChild0);
			parentHole.addChild(new HoleNode());
		}
	}

	private void generateBinarExprInExpr(Either<Node, Either<List<?>, NodeList<?>>> parent, int holeIndex, Node node,
			HoleNode currentHole, HoleNode parentHole, HoleNode parentOfParentHole, HoleNode parentOfParentOfParentHole,
			HoleType exprHoleType) {
		BinaryExpr binaryExpr = (BinaryExpr) parent.getLeft();
		if (parentHole.getHoleType().equals(HoleType.LeftSubExpr)) {
			binaryExpr.setLeft((Expression) node);
			currentHole.set(exprHoleType, false);
			HoleNode holeNode = new HoleNode();
			parentOfParentOfParentHole.addChild(holeNode);
		} else if (parentHole.getHoleType().equals(HoleType.RightSubExpr)) {
			binaryExpr.setRight((Expression) node);
			currentHole.set(HoleType.Expression, false);
			HoleNode holeNode = new HoleNode();
			parentOfParentOfParentHole.addChild(holeNode);
		} else if ((parentHole.getHoleTypeOfOptionsIfOnlyOne() != null
				&& parentHole.getHoleTypeOfOptionsIfOnlyOne().equals(HoleType.Expr11))
				|| (parentOfParentHole.getHoleTypeOfOptionsIfOnlyOne() != null
						&& parentOfParentHole.getHoleTypeOfOptionsIfOnlyOne().equals(HoleType.Expr11))) {
			binaryExpr.setRight((Expression) node);
			currentHole.set(exprHoleType, false);
			HoleNode holeNode = new HoleNode();
			parentOfParentOfParentHole.addChild(holeNode);
		} else if ((parentHole.getHoleTypeOfOptionsIfOnlyOne() != null
				&& parentHole.getHoleTypeOfOptionsIfOnlyOne().equals(HoleType.Expr10))
				|| (parentOfParentHole.getHoleTypeOfOptionsIfOnlyOne() != null
						&& parentOfParentHole.getHoleTypeOfOptionsIfOnlyOne().equals(HoleType.Expr10))) {
			if (holeIndex == 0) {
				// left
				binaryExpr.setLeft((Expression) node);

				currentHole.set(HoleType.LeftSubExpr, false);
				HoleNode anotherCurrentHole = new HoleNode(exprHoleType, false);
				currentHole.addChild(anotherCurrentHole);
				HoleNode holeNode = new HoleNode();
				parentHole.addChild(holeNode);
			} else {
				// right
				binaryExpr.setRight((Expression) node);

				currentHole.set(HoleType.RightSubExpr, false);
				HoleNode anotherCurrentHole = new HoleNode(exprHoleType, false);
				currentHole.addChild(anotherCurrentHole);
				parentOfParentOfParentHole.addChild(new HoleNode());
			}
		}
	}

	private void generateExprStmtInForStmt(Either<Node, Either<List<?>, NodeList<?>>> parent, HoleNode currentHole,
			Node node, HoleType exprHoleType) {
		// Note: we only support BlockStmt.
		// https://www.javadoc.io/static/com.github.javaparser/javaparser-core/3.23.1/com/github/javaparser/ast/stmt/ForStmt.html
		// notify() in for(; i < 10 ;){ ;notify(); }
		ForStmt forStmt = (ForStmt) parent.getLeft();
		Statement body = this.generateExprStmtBodyForStmts(forStmt.getBody(), currentHole, node, exprHoleType);
		forStmt.setBody(body);
	}

	private void generateExprStmtInForStmtForLet1AndLet2(Either<Node, Either<List<?>, NodeList<?>>> parent,
			HoleNode currentHole, Node node, HoleType exprHoleType) {
		ForStmt forStmt = (ForStmt) parent.getLeft();
		Statement body = this.generateExprStmtBodyForStmtsForLet1AndLet2(forStmt.getBody(), currentHole, node,
				exprHoleType);
		forStmt.setBody(body);
	}

	private void generateExprStmtInForStmtForPlusPLus(Either<Node, Either<List<?>, NodeList<?>>> parent,
			HoleNode currentHole, Node node, HoleType exprHoleType, int holeIndex, HoleNode parentHole) {
		if (holeIndex == 2) {
			ForStmt forStmt = (ForStmt) parent.getLeft();
			NodeList<Expression> expressions = new NodeList<Expression>();
			expressions.add((Expression) node);
			forStmt.setUpdate(expressions);

			currentHole.set(HoleType.Expression, false);
			parentHole.addChild(new HoleNode());
		} else {
			// case i++ in for(){; i++; }
			this.generateExprStmtInForStmt(parent, currentHole, node, exprHoleType);
		}
	}

	private void generateExprStmtInWhileStmt(Either<Node, Either<List<?>, NodeList<?>>> parent, HoleNode currentHole,
			Node node, HoleType exprHoleType) {
		WhileStmt stmt = (WhileStmt) parent.getLeft();
		Statement body = this.generateExprStmtBodyForStmts(stmt.getBody(), currentHole, node, exprHoleType);
		stmt.setBody(body);
	}

	private Statement generateExprStmtBodyForStmts(Statement body, HoleNode currentHole, Node node,
			HoleType exprHoleType) {
		// Note: we only support BlockStmt.
		// https://www.javadoc.io/static/com.github.javaparser/javaparser-core/3.23.1/com/github/javaparser/ast/stmt/ForStmt.html
		String bodyClassStr = body.getClass().toString();
		bodyClassStr = StringHelper.getClassName(bodyClassStr);
		if (bodyClassStr.equals("ReturnStmt")) {
			BlockStmt blockStmt = new BlockStmt();
			NodeList<Statement> statements = new NodeList<Statement>();
			ExpressionStmt expressionStmt = new ExpressionStmt((Expression) node);
			statements.add(expressionStmt);
			blockStmt.setStatements(statements);

			currentHole.set(HoleType.Body, false);

			HoleNode anotherCurrentHole = new HoleNode(HoleType.Statements, false);
			currentHole.addChild(anotherCurrentHole);

			HoleNode childNode = new HoleNode(HoleType.Wrapper, false);
			childNode.setHoleTypeOptions(new HoleType[] { HoleType.Statement });
			anotherCurrentHole.addChild(childNode);

			HoleNode anotherChildNode = new HoleNode(HoleType.Expression, false);
			childNode.addChild(anotherChildNode);

			HoleNode childOfanotherChildNode = new HoleNode(HoleType.Wrapper, false);
			childOfanotherChildNode.setHoleTypeOptionsOfOnlyOne(exprHoleType);
			anotherChildNode.addChild(childOfanotherChildNode);
			anotherCurrentHole.addChild(new HoleNode());
			return blockStmt;
		} else if (bodyClassStr.equals("BlockStmt")) {

		} else {
			System.out.println("Should not go to this branch");
		}

		return body;
	}

	private Statement generateExprStmtBodyForStmtsForLet1AndLet2(Statement body, HoleNode currentHole, Node node,
			HoleType exprHoleType) {
		// Note: we only support BlockStmt.
		// https://www.javadoc.io/static/com.github.javaparser/javaparser-core/3.23.1/com/github/javaparser/ast/stmt/ForStmt.html
		String bodyClassStr = body.getClass().toString();
		bodyClassStr = StringHelper.getClassName(bodyClassStr);
		if (bodyClassStr.equals("ReturnStmt")) {
			BlockStmt blockStmt = new BlockStmt();
			NodeList<Statement> statements = new NodeList<Statement>();
			ExpressionStmt expressionStmt = new ExpressionStmt((Expression) node);
			statements.add(expressionStmt);
			blockStmt.setStatements(statements);

			currentHole.set(HoleType.Body, false);
			HoleNode anotherCurrentHole = new HoleNode(HoleType.Statements, false);
			currentHole.addChild(anotherCurrentHole);
			anotherCurrentHole.addChild(this.constructHoleASTOfAssignStmtForLet1AndLet2(exprHoleType));
			return blockStmt;
		} else if (bodyClassStr.equals("BlockStmt")) {

		} else {
			System.out.println("Should not go to this branch");
		}

		return body;
	}

	private void generateThenStmtInIfStmt(Either<Node, Either<List<?>, NodeList<?>>> parent, Node node,
			HoleNode currentHole, HoleType holeTypeExpr) {
		IfStmt ifStmt = (IfStmt) parent.getLeft();
		Statement thenStmt = ifStmt.getThenStmt();
		String thenStmtStr = StringHelper.getClassName(thenStmt.getClass().toString());
		if (thenStmtStr.equals("ReturnStmt")) {
			BlockStmt blockStmt = new BlockStmt();
			NodeList<Statement> statements = new NodeList<Statement>();
			ExpressionStmt expressionStmt = new ExpressionStmt((Expression) node);
			statements.add(expressionStmt);
			blockStmt.setStatements(statements);
			ifStmt.setThenStmt(blockStmt);

			// [condition,then, else, else]
			currentHole.set(HoleType.ThenStatement, false);

			HoleNode stmtsHole = new HoleNode(HoleType.Statements, false);
			currentHole.addChild(stmtsHole);

			HoleNode holeNodeChild = new HoleNode(HoleType.Expression, false);
			stmtsHole.addChild(holeNodeChild);

			HoleNode childOfHoleNodeChild = new HoleNode(holeTypeExpr, false);
			holeNodeChild.addChild(childOfHoleNodeChild);
			stmtsHole.addChild(new HoleNode());
		} else if (thenStmtStr.equals("BlockStmt")) {
			// Jump out of current if statement.

		}
	}

	private void generateThenStmtInIfStmtForLet1AndLet2(Either<Node, Either<List<?>, NodeList<?>>> parent, Node node,
			HoleNode currentHole, HoleType holeTypeExpr) {
		IfStmt ifStmt = (IfStmt) parent.getLeft();
		Statement thenStmt = ifStmt.getThenStmt();
		String thenStmtStr = StringHelper.getClassName(thenStmt.getClass().toString());
		if (thenStmtStr.equals("ReturnStmt")) {
			BlockStmt blockStmt = new BlockStmt();
			NodeList<Statement> statements = new NodeList<Statement>();
			ExpressionStmt expressionStmt = new ExpressionStmt((Expression) node);
			statements.add(expressionStmt);
			blockStmt.setStatements(statements);
			ifStmt.setThenStmt(blockStmt);

			// [condition,then, else, else]
			currentHole.set(HoleType.ThenStatement, false);
			HoleNode stmtsHole = new HoleNode(HoleType.Statements, false);
			currentHole.addChild(stmtsHole);
			stmtsHole.addChild(this.constructHoleASTOfAssignStmtForLet1AndLet2(holeTypeExpr));
		} else if (thenStmtStr.equals("BlockStmt")) {
			// Jump out of current if statement.

		}
	}

	private void generateBlockStmt(Either<Node, Either<List<?>, NodeList<?>>> parent, Node node, HoleNode currentHole,
			int holeIndex, HoleNode parentHole, HoleType holeTypeExpr) {
		BlockStmt blockStmt = (BlockStmt) parent.getLeft();
		NodeList<Statement> statements = blockStmt.getStatements();
		if (holeIndex < statements.size()) {
			// TODO
		} else {
			ExpressionStmt expressionStmt = new ExpressionStmt((Expression) node);
			statements.add(expressionStmt);
			currentHole.setIsHole(false);
			currentHole.setHoleType(HoleType.Expression);

			HoleNode exprHole = new HoleNode(holeTypeExpr, false);
			currentHole.addChild(exprHole);

			// HoleNode holeNode = new HoleNode(HoleType.Statement, true);
			parentHole.addChild(new HoleNode());
		}
	}

	private void generateThenStmtInIfStmtForExpr10AndExpr11AndLet6(Either<Node, Either<List<?>, NodeList<?>>> parent,
			Node node, HoleNode currentHole, HoleType holeTypeExpr) {
		IfStmt ifStmt = (IfStmt) parent.getLeft();
		Statement thenStmt = ifStmt.getThenStmt();
		String thenStmtStr = StringHelper.getClassName(thenStmt.getClass().toString());
		if (thenStmtStr.equals("ReturnStmt")) {
			BlockStmt blockStmt = new BlockStmt();
			NodeList<Statement> statements = new NodeList<Statement>();
			ExpressionStmt expressionStmt = new ExpressionStmt((Expression) node);
			statements.add(expressionStmt);
			blockStmt.setStatements(statements);
			ifStmt.setThenStmt(blockStmt);

			// [condition,then, else, else]
			currentHole.set(HoleType.ThenStatement, false);

			HoleNode stmtsNode = new HoleNode(HoleType.Statements, false);
			currentHole.addChild(stmtsNode);

			HoleNode holeNodeChild = new HoleNode(HoleType.Expression, false);
			stmtsNode.addChild(holeNodeChild);

			HoleNode childOfanotherChildNode = new HoleNode(HoleType.Wrapper, false);
			childOfanotherChildNode.setHoleTypeOptionsOfOnlyOne(holeTypeExpr);
			holeNodeChild.addChild(childOfanotherChildNode);

			HoleNode newHole = new HoleNode();
			newHole.setHoleTypeOptionsOfOnlyOne(HoleType.Expression);
			childOfanotherChildNode.addChild(newHole);
		} else if (thenStmtStr.equals("BlockStmt")) {
		}
	}

	private void generateSwitchEntry(Either<Node, Either<List<?>, NodeList<?>>> parent, Node node, int holeIndex,
			HoleNode currentHole, HoleType holeTypeExpr) {
		SwitchEntry switchEntry = (SwitchEntry) parent.getLeft();
		NodeList<Statement> statements = switchEntry.getStatements();
		if (holeIndex < statements.size()) {
			// TODO
		} else {
			ExpressionStmt expressionStmt = new ExpressionStmt((Expression) node);
			statements.add((Statement) expressionStmt);

			currentHole.set(HoleType.Statements, false);
			HoleNode holeNode = new HoleNode(HoleType.Expression, false);
			currentHole.addChild(holeNode);
			HoleNode childHoleNode = new HoleNode(holeTypeExpr, false);
			holeNode.addChild(childHoleNode);
			currentHole.addChild(new HoleNode());
		}
	}

	private void generateSwitchEntryForLet1AndLet2(Either<Node, Either<List<?>, NodeList<?>>> parent, Node node,
			int holeIndex, HoleNode currentHole, HoleType holeTypeExpr) {
		SwitchEntry switchEntry = (SwitchEntry) parent.getLeft();
		NodeList<Statement> statements = switchEntry.getStatements();
		if (holeIndex < statements.size()) {
			// TODO
		} else {
			ExpressionStmt expressionStmt = new ExpressionStmt((Expression) node);
			statements.add((Statement) expressionStmt);

			currentHole.set(HoleType.Statements, false);
			currentHole.addChild(this.constructHoleASTOfAssignStmtForLet1AndLet2(holeTypeExpr));
		}
	}

	private void generateSwitchEntryForExpr10AndExpr11AndLet6(Either<Node, Either<List<?>, NodeList<?>>> parent,
			Node node, int holeIndex, HoleNode currentHole, HoleType holeTypeExpr) {
		SwitchEntry switchEntry = (SwitchEntry) parent.getLeft();
		NodeList<Statement> statements = switchEntry.getStatements();
		if (holeIndex < statements.size()) {
			// TODO
		} else {
			ExpressionStmt expressionStmt = new ExpressionStmt((Expression) node);
			statements.add((Statement) expressionStmt);
			currentHole.set(HoleType.Statements, false);

			HoleNode holeNode = new HoleNode(HoleType.Expression, false);
			currentHole.addChild(holeNode);

			HoleNode childHoleNode = new HoleNode(HoleType.Wrapper, false);
			childHoleNode.setHoleTypeOptionsOfOnlyOne(holeTypeExpr);
			holeNode.addChild(childHoleNode);
			childHoleNode.addChild(new HoleNode());
		}
	}

	private void generateSwitchEntryForTotalReturnStmt(Either<Node, Either<List<?>, NodeList<?>>> parent, Node node,
			int holeIndex, HoleNode currentHole, HoleType holeTypeExpr) {
		SwitchEntry switchEntry = (SwitchEntry) parent.getLeft();
		NodeList<Statement> statements = switchEntry.getStatements();
		if (holeIndex < statements.size()) {
			// TODO
		} else {
			statements.add((Statement) node);
			currentHole.set(HoleType.Statements, false);

			HoleNode holeNode = new HoleNode(HoleType.Expression, false);
			currentHole.addChild(holeNode);

			HoleNode childHoleNode = new HoleNode(holeTypeExpr, false);
			holeNode.addChild(childHoleNode);
			currentHole.addChild(new HoleNode());
		}
	}

	private void generateSwitchEntryForReturn11AndReturn2(Either<Node, Either<List<?>, NodeList<?>>> parent, Node node,
			int holeIndex, HoleNode currentHole, HoleType holeTypeExpr) {
		SwitchEntry switchEntry = (SwitchEntry) parent.getLeft();
		NodeList<Statement> statements = switchEntry.getStatements();
		if (holeIndex < statements.size()) {
			// TODO
		} else {
			statements.add((Statement) node);
			currentHole.set(HoleType.Statements, false);

			HoleNode holeNode = new HoleNode(HoleType.Expression, false);
			currentHole.addChild(holeNode);

			HoleNode childHoleNode = new HoleNode(HoleType.Wrapper, false);
			childHoleNode.setHoleTypeOptionsOfOnlyOne(holeTypeExpr);
			holeNode.addChild(childHoleNode);

			HoleNode argsNode = new HoleNode(HoleType.Arguments, false);
			childHoleNode.addChild(argsNode);
			argsNode.addChild(new HoleNode());
		}
	}

	private void generateExpForExpressionStmt(Either<Node, Either<List<?>, NodeList<?>>> parent, Node node, int holeIndex,
			HoleNode currentHole, HoleNode parentOfParentHole, HoleType holeTypeExpr) {
		ExpressionStmt expressionStmt = (ExpressionStmt) parent.getLeft();
		String expressionClassStr = StringHelper.getClassName(expressionStmt.getExpression().getClass().toString());
		if (expressionClassStr.equals("VariableDeclarationExpr")) {
			VariableDeclarationExpr variableDeclarationExpr = (VariableDeclarationExpr) expressionStmt.getExpression();
			NodeList<VariableDeclarator> variableDeclarators = variableDeclarationExpr.getVariables();
			variableDeclarators.get(0).setInitializer((Expression) node);

			currentHole.set(holeTypeExpr, false);
			currentHole.setHoleTypeOptionsOfOnlyOne(HoleType.Expression);

			HoleNode holeNode = new HoleNode();
			holeNode.setHoleTypeOptions(new HoleType[] { HoleType.Statement });
			parentOfParentHole.addChild(holeNode);
		} else if (expressionClassStr.equals("AssignExpr")) {
			AssignExpr assignExpr = (AssignExpr) expressionStmt.getExpression();
			assignExpr.setValue((Expression) node);
			currentHole.set(holeTypeExpr, false);
			currentHole.setHoleTypeOptionsOfOnlyOne(HoleType.Expression);
			parentOfParentHole.addChild(new HoleNode());
		}
	}

	private void generateReturnStmtInForStmt(Either<Node, Either<List<?>, NodeList<?>>> parent, Node node,
			HoleNode currentHole, HoleNode parentHole, HoleType holeType) {
		ForStmt forStmt = (ForStmt) parent.getLeft();
		Statement body = this.generateTotalStmt(forStmt.getBody(), node, currentHole, parentHole, holeType);
		forStmt.setBody(body);
	}

	private void generateReturnStmtInForStmtForReturn1AndReturn2(Either<Node, Either<List<?>, NodeList<?>>> parent,
			Node node, HoleNode currentHole, HoleNode parentHole, HoleType holeType) {
		ForStmt forStmt = (ForStmt) parent.getLeft();
		Statement body = this.generateTotalStmtForReturn1AndReturn2(forStmt.getBody(), node, currentHole, parentHole,
				holeType);
		forStmt.setBody(body);
	}

	private void generateReturnStmtInWhiletmt(Either<Node, Either<List<?>, NodeList<?>>> parent, Node node,
			HoleNode currentHole, HoleNode parentHole, HoleType holeType) {
		WhileStmt stmt = (WhileStmt) parent.getLeft();
		Statement body = this.generateTotalStmt(stmt.getBody(), node, currentHole, parentHole, holeType);
		stmt.setBody(body);
	}

	private void generateReturnStmtInWhiletmtForReturn1AndReturn2(Either<Node, Either<List<?>, NodeList<?>>> parent,
			Node node, HoleNode currentHole, HoleNode parentHole, HoleType holeType) {
		WhileStmt stmt = (WhileStmt) parent.getLeft();
		Statement body = this.generateTotalStmtForReturn1AndReturn2(stmt.getBody(), node, currentHole, parentHole,
				holeType);
		stmt.setBody(body);
	}

	private Statement generateTotalStmt(Statement body, Node node, HoleNode currentHole, HoleNode parentHole,
			HoleType holeType) {
		String bodyClassStr = body.getClass().toString();
		bodyClassStr = StringHelper.getClassName(bodyClassStr);
		if (bodyClassStr.equals("ReturnStmt")) {
			BlockStmt blockStmt = new BlockStmt();
			NodeList<Statement> statements = new NodeList<Statement>();
			statements.add((Statement) node);
			blockStmt.setStatements(statements);

			currentHole.set(HoleType.Body, false);
			HoleNode anotherCurrentHole = new HoleNode(HoleType.Statements, false);
			currentHole.addChild(anotherCurrentHole);
			HoleNode childNode = new HoleNode(HoleType.Wrapper, false);
			childNode.setHoleTypeOptionsOfOnlyOne(holeType);
			anotherCurrentHole.addChild(childNode);
			parentHole.addChild(new HoleNode());
			return blockStmt;
		} else if (bodyClassStr.equals("BlockStmt")) {

		} else {
			System.out.println("Should not go to this branch");
		}
		return body;
	}

	private Statement generateTotalStmtForReturn1AndReturn2(Statement body, Node node, HoleNode currentHole,
			HoleNode parentHole, HoleType holeType) {
		String bodyClassStr = body.getClass().toString();
		bodyClassStr = StringHelper.getClassName(bodyClassStr);
		if (bodyClassStr.equals("ReturnStmt")) {
			BlockStmt blockStmt = new BlockStmt();
			NodeList<Statement> statements = new NodeList<Statement>();
			statements.add((Statement) node);
			blockStmt.setStatements(statements);

			currentHole.set(HoleType.Body, false);
			HoleNode anotherCurrentHole = new HoleNode(HoleType.Statements, false);
			currentHole.addChild(anotherCurrentHole);
			anotherCurrentHole.addChild(this.constructHoleASTOfReturnStmtForReturn1AndReturn2(holeType));
			return blockStmt;
		} else if (bodyClassStr.equals("BlockStmt")) {

		} else {
			System.out.println("Should not go to this branch");
		}
		return body;
	}

	private Statement generateReturnStmt6(Statement body, Node node, HoleNode currentHole, HoleNode parentHole,
			HoleType holeType) {
		String bodyClassStr = body.getClass().toString();
		bodyClassStr = StringHelper.getClassName(bodyClassStr);
		if (bodyClassStr.equals("ReturnStmt")) {
			BlockStmt blockStmt = new BlockStmt();
			NodeList<Statement> statements = new NodeList<Statement>();
			statements.add((Statement) node);
			blockStmt.setStatements(statements);

			currentHole.set(HoleType.Body, false);

			HoleNode anotherCurrentHole = new HoleNode(HoleType.Statements, false);
			currentHole.addChild(anotherCurrentHole);

			HoleNode childNode = new HoleNode(HoleType.Wrapper, false);
			childNode.setHoleTypeOptionsOfOnlyOne(holeType);
			anotherCurrentHole.addChild(childNode);

			HoleNode anotherChildNode = new HoleNode(HoleType.Expression, false);
			childNode.addChild(anotherChildNode);
			anotherChildNode.addChild(new HoleNode());
			return body;
		} else if (bodyClassStr.equals("BlockStmt")) {

		} else {
			System.out.println("Should not go to this branch");
		}
		return body;

	}

	private void generateTotalStmtInIfThenStmt(Either<Node, Either<List<?>, NodeList<?>>> parent, Node node,
			HoleNode currentHole, HoleNode parentHole, HoleType holeType) {
		IfStmt ifStmt = (IfStmt) parent.getLeft();
		Statement thenStmt = ifStmt.getThenStmt();
		String thenStmtStr = StringHelper.getClassName(thenStmt.getClass().toString());
		if (thenStmtStr.equals("ReturnStmt")) {
			BlockStmt blockStmt = new BlockStmt();
			NodeList<Statement> statements = new NodeList<Statement>();
			statements.add((Statement) node);
			blockStmt.setStatements(statements);
			ifStmt.setThenStmt(blockStmt);

			currentHole.set(HoleType.ThenStatement, false);
			HoleNode stmtsNode = new HoleNode(HoleType.Statements, false);
			currentHole.addChild(stmtsNode);
			HoleNode holeNodeChild = new HoleNode(HoleType.Expression, false);
			stmtsNode.addChild(holeNodeChild);
			HoleNode childOfanotherChildNode = new HoleNode(holeType, false);
			holeNodeChild.addChild(childOfanotherChildNode);
			parentHole.addChild(new HoleNode());
		} else if (thenStmtStr.equals("BlockStmt")) {
		}
	}

	private void generateTotalStmtInIfThenStmtForReturn1AndReturn2(Either<Node, Either<List<?>, NodeList<?>>> parent,
			Node node, HoleNode currentHole, HoleNode parentHole, HoleType holeType) {
		IfStmt ifStmt = (IfStmt) parent.getLeft();
		Statement thenStmt = ifStmt.getThenStmt();
		String thenStmtStr = StringHelper.getClassName(thenStmt.getClass().toString());
		if (thenStmtStr.equals("ReturnStmt")) {
			BlockStmt blockStmt = new BlockStmt();
			NodeList<Statement> statements = new NodeList<Statement>();
			statements.add((Statement) node);
			blockStmt.setStatements(statements);
			ifStmt.setThenStmt(blockStmt);

			currentHole.set(HoleType.ThenStatement, false);
			HoleNode stmtsNode = new HoleNode(HoleType.Statements, false);
			currentHole.addChild(stmtsNode);
			stmtsNode.addChild(this.constructHoleASTOfReturnStmtForReturn1AndReturn2(holeType));
		} else if (thenStmtStr.equals("BlockStmt")) {
		}
	}

	private void generateReturn6(Either<Node, Either<List<?>, NodeList<?>>> parent, Node node, HoleNode currentHole,
			HoleNode parentHole, HoleNode parentOfParentHole, HoleType holeType) {
		ReturnStmt stmt = (ReturnStmt) parent.getLeft();
		stmt.setExpression((Expression) node);
		currentHole.set(holeType, false);
		parentOfParentHole.addChild(new HoleNode());
	}

	private void generateReturn6ForExpr10AndExpr11(Either<Node, Either<List<?>, NodeList<?>>> parent, Node node,
			HoleNode currentHole, HoleNode parentHole, HoleNode parentOfParentHole, HoleType holeType) {
		ReturnStmt stmt = (ReturnStmt) parent.getLeft();
		stmt.setExpression((Expression) node);
		currentHole.set(HoleType.Expression, false);
		HoleNode anotherHole = new HoleNode(HoleType.Wrapper, false);
		anotherHole.setHoleTypeOptionsOfOnlyOne(holeType);
		currentHole.addChild(anotherHole);
		anotherHole.addChild(new HoleNode());
	}

	private void generateExprInAssignExpr(Either<Node, Either<List<?>, NodeList<?>>> parent, Node node,
			HoleNode currentHole, HoleNode parentOfParentHole, HoleType holeType) {
		AssignExpr assignExpr = (AssignExpr) parent.getLeft();
		assignExpr.setValue((Expression) node);
		currentHole.set(holeType, false);
		parentOfParentHole.addChild(new HoleNode());
	}

	private void generateExprInAssignExprForExpr10AndExpr11(Either<Node, Either<List<?>, NodeList<?>>> parent, Node node,
			HoleNode currentHole, HoleNode parentOfParentHole, HoleType holeType) {
		AssignExpr assignExpr = (AssignExpr) parent.getLeft();
		assignExpr.setValue((Expression) node);

		currentHole.set(HoleType.AssignExprValue, false);
		HoleNode anotherHole = new HoleNode(HoleType.Wrapper, false);
		anotherHole.setHoleTypeOptionsOfOnlyOne(holeType);
		currentHole.addChild(anotherHole);
		anotherHole.addChild(new HoleNode());
	}

	private void generateExprInArguments(Either<Node, Either<List<?>, NodeList<?>>> parent, Node node,
			HoleNode currentHole, HoleNode parentHole, HoleType holeTypeExpr) {
		NodeList<Expression> arguments = (NodeList<Expression>) parent.get().get();
		arguments.add((Expression) node);

		currentHole.set(HoleType.Wrapper, false);
		currentHole.setHoleTypeOptionsOfOnlyOne(HoleType.Argument);
		HoleNode holeNode = new HoleNode(holeTypeExpr, false);
		currentHole.addChild(holeNode);
		parentHole.addChild(new HoleNode());
	}

	private void generateCallFunctionExpr(Either<Node, Either<List<?>, NodeList<?>>> parent, Node node,
			HoleNode currentHole, HoleNode parentHole, HoleNode parentOfParentHole, HoleType parentHoleType, int holeIndex,
			String parentNodeClassStr, HoleType holeTypeExpr) {
		String nodeClassStr = StringHelper.getClassName(node.getClass().toString());
		if (parentHoleType.equals(HoleType.Statements)) {
			NodeList<Statement> statements = (NodeList<Statement>) parent.get().get();
			if (holeIndex < statements.size()) {
				// TODO
			} else {
				statements.add(new ExpressionStmt((Expression) node));

				currentHole.set(HoleType.Wrapper, false);
				currentHole.setHoleTypeOptionsOfOnlyOne(HoleType.Statement);
				HoleNode exprHole = new HoleNode(HoleType.Expression, false);
				currentHole.addChild(exprHole);
				HoleNode holeNode = new HoleNode(HoleType.Wrapper, false);
				holeNode.setHoleTypeOptionsOfOnlyOne(holeTypeExpr);
				exprHole.addChild(holeNode);

				HoleNode holdeNodeChild0 = new HoleNode(HoleType.Arguments, false);
				holeNode.addChild(holdeNodeChild0);
				holdeNodeChild0.addChild(new HoleNode());
			}

		} else if (parentNodeClassStr != null && parentNodeClassStr.equals("MethodDeclaration")) {
			MethodDeclaration mNode = (MethodDeclaration) parent.getLeft();
			Optional<BlockStmt> optionalBody = mNode.getBody();
			currentHole.set(HoleType.Body, false);

			HoleNode anotherCurrentHole = new HoleNode();
			currentHole.addChild(anotherCurrentHole);

			BlockStmt blockStmt = optionalBody.get();
			NodeList<Statement> statements = blockStmt.getStatements();
			if (statements.size() == 0) {
				ExpressionStmt expressionStmt = new ExpressionStmt((Expression) node);
				statements.add(expressionStmt);

				anotherCurrentHole.set(HoleType.Statements, false);

				HoleNode holdeNodeChild0 = new HoleNode(HoleType.Expression, false);
				anotherCurrentHole.addChild(holdeNodeChild0);

				HoleNode holeNode = new HoleNode(HoleType.Wrapper, false);
				holeNode.setHoleTypeOptions(new HoleType[] { holeTypeExpr });
				holdeNodeChild0.addChild(holeNode);

				HoleNode holeNodeChild = new HoleNode(HoleType.Arguments, false);
				holeNode.addChild(holeNodeChild);
				holeNodeChild.addChild(new HoleNode());
			} else {
				// TODO
			}
		} else if (parentNodeClassStr != null && parentNodeClassStr.equals("BinaryExpr")) {
			BinaryExpr binaryExpr = (BinaryExpr) parent.getLeft();
			if (parentHole.getHoleType().equals(HoleType.LeftSubExpr)) {
				System.out.println("Shall not go this branch");
			} else if (parentHole.getHoleType().equals(HoleType.RightSubExpr)) {
				binaryExpr.setRight((Expression) node);
				currentHole.set(HoleType.Expression, false);
				HoleNode holeNode = new HoleNode(HoleType.Wrapper, false);
				holeNode.setHoleTypeOptionsOfOnlyOne(holeTypeExpr);
				currentHole.addChild(holeNode);
				HoleNode anotherHoleNode = new HoleNode(HoleType.Arguments, false);
				holeNode.addChild(anotherHoleNode);
				anotherHoleNode.addChild(new HoleNode());
			} else if ((parentHole.getHoleTypeOfOptionsIfOnlyOne() != null
					&& parentHole.getHoleTypeOfOptionsIfOnlyOne().equals(HoleType.Expr11))
					|| (parentOfParentHole.getHoleTypeOfOptionsIfOnlyOne() != null
							&& parentOfParentHole.getHoleTypeOfOptionsIfOnlyOne().equals(HoleType.Expr11))) {
				binaryExpr.setRight((Expression) node);
				currentHole.set(HoleType.Expression, false);
				HoleNode holeNode = new HoleNode(HoleType.Wrapper, false);
				holeNode.setHoleTypeOptionsOfOnlyOne(holeTypeExpr);
				currentHole.addChild(holeNode);
				HoleNode anotherHoleNode = new HoleNode(HoleType.Arguments, false);
				holeNode.addChild(anotherHoleNode);
				anotherHoleNode.addChild(new HoleNode());
			} else if ((parentHole.getHoleTypeOfOptionsIfOnlyOne() != null
					&& parentHole.getHoleTypeOfOptionsIfOnlyOne().equals(HoleType.Expr10))
					|| (parentOfParentHole.getHoleTypeOfOptionsIfOnlyOne() != null
							&& parentOfParentHole.getHoleTypeOfOptionsIfOnlyOne().equals(HoleType.Expr10))) {
				if (holeIndex == 0) {
					// left
					System.out.println("Shall not go to this branch");
				} else {
					// right
					binaryExpr.setRight((Expression) node);
					currentHole.set(HoleType.Expression, false);
					HoleNode holeNode = new HoleNode(HoleType.Wrapper, false);
					holeNode.setHoleTypeOptionsOfOnlyOne(holeTypeExpr);
					currentHole.addChild(holeNode);
					HoleNode anotherHoleNode = new HoleNode(HoleType.Arguments, false);
					holeNode.addChild(anotherHoleNode);
					anotherHoleNode.addChild(new HoleNode());
				}
			}
		} else if (parentNodeClassStr != null && parentNodeClassStr.equals("ForStmt")) {
			ForStmt forStmt = (ForStmt) parent.getLeft();
			String bodyClassStr = forStmt.getBody().getClass().toString();
			bodyClassStr = StringHelper.getClassName(bodyClassStr);
			if (bodyClassStr.equals("ReturnStmt")) {
				BlockStmt blockStmt = new BlockStmt();
				NodeList<Statement> statements = new NodeList<Statement>();
				ExpressionStmt expressionStmt = new ExpressionStmt((Expression) node);
				statements.add(expressionStmt);
				blockStmt.setStatements(statements);
				forStmt.setBody(blockStmt);

				currentHole.set(HoleType.Body, false);
				HoleNode anotherCurrentHole = new HoleNode();
				anotherCurrentHole.set(HoleType.Statements, false);
				currentHole.addChild(anotherCurrentHole);

				HoleNode childNode = new HoleNode(HoleType.Wrapper, false);
				childNode.setHoleTypeOptions(new HoleType[] { HoleType.Statement });
				anotherCurrentHole.addChild(childNode);

				HoleNode anotherChildNode = new HoleNode(HoleType.Expression, false);
				childNode.addChild(anotherChildNode);

				HoleNode childOfanotherChildNode = new HoleNode(HoleType.Wrapper, false);
				childOfanotherChildNode.setHoleTypeOptionsOfOnlyOne(holeTypeExpr);
				anotherChildNode.addChild(childOfanotherChildNode);

				HoleNode argsWrapper = new HoleNode(HoleType.Arguments, false);
				childOfanotherChildNode.addChild(argsWrapper);
				argsWrapper.addChild(new HoleNode());
			} else {
				System.out.println("Should not go to this branch");
			}
		} else if (parentNodeClassStr != null && parentNodeClassStr.equals("WhileStmt")) {
			WhileStmt stmt = (WhileStmt) parent.getLeft();
			String bodyClassStr = stmt.getBody().getClass().toString();
			bodyClassStr = StringHelper.getClassName(bodyClassStr);
			if (bodyClassStr.equals("ReturnStmt")) {
				BlockStmt blockStmt = new BlockStmt();
				NodeList<Statement> statements = new NodeList<Statement>();
				ExpressionStmt expressionStmt = new ExpressionStmt((Expression) node);
				statements.add(expressionStmt);
				blockStmt.setStatements(statements);
				stmt.setBody(blockStmt);

				currentHole.set(HoleType.Body, false);

				HoleNode anotherCurrentHole = new HoleNode(HoleType.Statements, false);
				currentHole.addChild(anotherCurrentHole);

				HoleNode childNode = new HoleNode(HoleType.Wrapper, false);
				childNode.setHoleTypeOptions(new HoleType[] { HoleType.Statement });
				anotherCurrentHole.addChild(childNode);

				HoleNode anotherChildNode = new HoleNode(HoleType.Expression, false);
				childNode.addChild(anotherChildNode);

				HoleNode childOfanotherChildNode = new HoleNode(HoleType.Wrapper, false);
				childOfanotherChildNode.setHoleTypeOptionsOfOnlyOne(holeTypeExpr);
				anotherChildNode.addChild(childOfanotherChildNode);

				HoleNode argsWrapper = new HoleNode(HoleType.Arguments, false);
				childOfanotherChildNode.addChild(argsWrapper);
				argsWrapper.addChild(new HoleNode());
			} else {
				System.out.println("Should not go to this branch");
			}
		} else if (parentNodeClassStr != null && parentNodeClassStr.equals("IfStmt")) {
			IfStmt ifStmt = (IfStmt) parent.getLeft();
			Statement thenStmt = ifStmt.getThenStmt();
			String thenStmtStr = StringHelper.getClassName(thenStmt.getClass().toString());
			if (thenStmtStr.equals("ReturnStmt")) {
				BlockStmt blockStmt = new BlockStmt();
				NodeList<Statement> statements = new NodeList<Statement>();
				ExpressionStmt expressionStmt = new ExpressionStmt((Expression) node);
				statements.add(expressionStmt);
				blockStmt.setStatements(statements);
				ifStmt.setThenStmt(blockStmt);

				// [condition,then, else, else]
				currentHole.set(HoleType.ThenStatement, false);

				HoleNode statementsHole = new HoleNode(HoleType.Statements, false);
				currentHole.addChild(statementsHole);

				HoleNode statementHole = new HoleNode(HoleType.Wrapper, false);
				statementHole.setHoleTypeOptionsOfOnlyOne(HoleType.Statement);
				statementsHole.addChild(statementHole);

				HoleNode holeNodeChild = new HoleNode(HoleType.Expression, false);
				statementHole.addChild(holeNodeChild);

				HoleNode childOfHoleNodeChild = new HoleNode(HoleType.Wrapper, false);
				childOfHoleNodeChild.setHoleTypeOptionsOfOnlyOne(holeTypeExpr);
				holeNodeChild.addChild(childOfHoleNodeChild);

				HoleNode holeNodeChildChild = new HoleNode(HoleType.Arguments, false);
				childOfHoleNodeChild.addChild(holeNodeChildChild);
				holeNodeChildChild.addChild(new HoleNode());
			} else {
				System.out.println("Should not go to this branch");
			}
		} else if (parentNodeClassStr != null && parentNodeClassStr.equals("BlockStmt")) {
			BlockStmt blockStmt = (BlockStmt) parent.getLeft();
			NodeList<Statement> statements = blockStmt.getStatements();
			if (holeIndex < statements.size()) {
				// TODO
			} else {
				ExpressionStmt expressionStmt = new ExpressionStmt((Expression) node);
				statements.add(expressionStmt);
				currentHole.setIsHole(false);
				currentHole.setHoleType(HoleType.Expression);

				HoleNode exprHole = new HoleNode(HoleType.Wrapper, false);
				exprHole.setHoleTypeOptionsOfOnlyOne(holeTypeExpr);
				currentHole.addChild(exprHole);

				HoleNode holeNode = new HoleNode(HoleType.Arguments, false);
				parentHole.addChild(holeNode);
				holeNode.addChild(new HoleNode());
			}
		} else if (parentNodeClassStr != null && parentNodeClassStr.equals("SwitchEntry")) {
			SwitchEntry switchEntry = (SwitchEntry) parent.getLeft();
			NodeList<Statement> statements = switchEntry.getStatements();
			if (holeIndex < statements.size()) {
				// TODO
			} else {
				ExpressionStmt expressionStmt = new ExpressionStmt((Expression) node);
				statements.add((Statement) expressionStmt);
				currentHole.set(HoleType.Statements, false);

				HoleNode holeNode = new HoleNode(HoleType.Expression, false);
				currentHole.addChild(holeNode);

				HoleNode childHoleNode = new HoleNode(HoleType.Wrapper, false);
				childHoleNode.setHoleTypeOptionsOfOnlyOne(holeTypeExpr);
				holeNode.addChild(childHoleNode);

				HoleNode argsNode = new HoleNode(HoleType.Arguments, false);
				childHoleNode.addChild(argsNode);
				argsNode.addChild(new HoleNode());
			}
		} else if (parentNodeClassStr != null && parentNodeClassStr.equals("ExpressionStmt")) {
			ExpressionStmt expressionStmt = (ExpressionStmt) parent.getLeft();
			String expressionClassStr = StringHelper.getClassName(expressionStmt.getExpression().getClass().toString());
			if (expressionClassStr.equals("VariableDeclarationExpr")) {
				VariableDeclarationExpr variableDeclarationExpr = (VariableDeclarationExpr) expressionStmt.getExpression();
				NodeList<VariableDeclarator> variableDeclarators = variableDeclarationExpr.getVariables();
				variableDeclarators.get(0).setInitializer((Expression) node);

				currentHole.set(HoleType.Expression, false);

				HoleNode variables = new HoleNode(HoleType.VariableDeclarator, false);
				currentHole.addChild(variables);

				HoleNode variable = new HoleNode(HoleType.Wrapper, false);
				variable.setHoleTypeOptionsOfOnlyOne(holeTypeExpr);
				variables.addChild(variable);

				HoleNode args = new HoleNode(HoleType.Arguments, false);
				variable.addChild(args);

				args.addChild(new HoleNode());
			} else if (expressionClassStr.equals("AssignExpr")) {
				AssignExpr assignExpr = (AssignExpr) expressionStmt.getExpression();
				assignExpr.setValue((Expression) node);
				currentHole.set(HoleType.Expression, false);

				HoleNode exprHole = new HoleNode(HoleType.Wrapper, false);
				exprHole.setHoleTypeOptionsOfOnlyOne(holeTypeExpr);
				currentHole.addChild(exprHole);

				HoleNode argsHole = new HoleNode(HoleType.Arguments, false);
				exprHole.addChild(argsHole);

				argsHole.addChild(new HoleNode());
			}
		} else if (parentNodeClassStr != null && parentNodeClassStr.equals("ReturnStmt")) {
			ReturnStmt stmt = (ReturnStmt) parent.getLeft();
			stmt.setExpression((Expression) node);
			currentHole.set(HoleType.Expression, false);

			HoleNode exprHole = new HoleNode(HoleType.Wrapper, false);
			exprHole.setHoleTypeOptionsOfOnlyOne(holeTypeExpr);
			currentHole.addChild(exprHole);

			HoleNode argsHole = new HoleNode(HoleType.Arguments, false);
			exprHole.addChild(argsHole);
		} else if (parentNodeClassStr != null && parentNodeClassStr.equals("AssignExpr")) {
			AssignExpr assignExpr = (AssignExpr) parent.getLeft();
			assignExpr.setValue((Expression) node);
			currentHole.set(HoleType.AssignExprValue, false);

			HoleNode exprHole = new HoleNode(HoleType.Wrapper, false);
			exprHole.setHoleTypeOptionsOfOnlyOne(holeTypeExpr);
			currentHole.addChild(exprHole);

			HoleNode argsHole = new HoleNode(HoleType.Arguments, false);
			exprHole.addChild(argsHole);
		} else if (parentHoleType.equals(HoleType.Arguments)) {
			this.generateExprInArguments(parent, node, currentHole, parentHole, holeTypeExpr);
		} else if (parentNodeClassStr != null && parentNodeClassStr.equals("EnclosedExpr")) {
			EnclosedExpr enclosedExpr = (EnclosedExpr) parent.getLeft();
			enclosedExpr.setInner((Expression) node);
			currentHole.set(HoleType.InnerExpr, false);

			HoleNode exprHole = new HoleNode(HoleType.Wrapper, false);
			exprHole.setHoleTypeOptionsOfOnlyOne(holeTypeExpr);
			currentHole.addChild(exprHole);

			HoleNode argsHole = new HoleNode(HoleType.Arguments, false);
			exprHole.addChild(argsHole);
		}
	}

	private void generateExprForEnclosedExpr(Either<Node, Either<List<?>, NodeList<?>>> parent, Node node,
			HoleNode currentHole, HoleNode parentOfParentHole, HoleType holeTypeExpr) {
		EnclosedExpr enclosedExpr = (EnclosedExpr) parent.getLeft();
		enclosedExpr.setInner((Expression) node);
		currentHole.set(HoleType.InnerExpr, false);

		HoleNode exprHole = new HoleNode(holeTypeExpr, false);
		currentHole.addChild(exprHole);
		parentOfParentHole.addChild(new HoleNode());
	}

	private void generateExprForExpr5AndExpr14(Either<Node, Either<List<?>, NodeList<?>>> parent, Node node,
			String parentNodeClassStr, int holeIndex, HoleNode currentHole, HoleNode parentHole, HoleNode parentOfParentHole,
			HoleNode parentOfParentOfParentHole, HoleType parentHoleType, HoleType holeTypeExpr) {
		if (parentHoleType.equals(HoleType.Statements)) {
			this.generateExpInStatements(parent, holeIndex, node, currentHole, parentHole, holeTypeExpr);
		} else if (parentNodeClassStr != null && parentNodeClassStr.equals("MethodDeclaration")) {
			this.generateExpInMethodBody(parent, currentHole, node, holeTypeExpr);
		} else if (parentNodeClassStr != null && parentNodeClassStr.equals("BinaryExpr")) {
			this.generateBinarExprInExpr(parent, holeIndex, node, currentHole, parentHole, parentOfParentHole,
					parentOfParentOfParentHole, holeTypeExpr);
		} else if (parentNodeClassStr != null && parentNodeClassStr.equals("ForStmt")) {
			this.generateExprStmtInForStmt(parent, currentHole, node, holeTypeExpr);
		} else if (parentNodeClassStr != null && parentNodeClassStr.equals("WhileStmt")) {
			if (holeIndex == 0) {
				// i < 10 in while(i < 10){}
				WhileStmt whileStmt = (WhileStmt) parent.getLeft();
				whileStmt.setCondition((Expression) node);
				currentHole.set(HoleType.Expression, false);
				parentHole.addChild(new HoleNode());
			} else {
				this.generateExprStmtInWhileStmt(parent, currentHole, node, holeTypeExpr);
			}
		} else if (parentNodeClassStr != null && parentNodeClassStr.equals("IfStmt")) {
			this.generateThenStmtInIfStmt(parent, node, currentHole, holeTypeExpr);
		} else if (parentNodeClassStr != null && parentNodeClassStr.equals("BlockStmt")) {
			this.generateBlockStmt(parent, node, currentHole, holeIndex, parentHole, holeTypeExpr);
		} else if (parentHoleType.equals(HoleType.SwitchEntries)) {
			NodeList<SwitchEntry> switchEntries = (NodeList<SwitchEntry>) parent.get().get();
			if (holeIndex < switchEntries.size()) {
				// TODO
			} else {
				SwitchEntry switchEntry = new SwitchEntry();
				NodeList<Expression> labels = new NodeList<Expression>();
				labels.add((Expression) node);
				switchEntry.setLabels(labels);
				switchEntries.add(switchEntry);

				currentHole.set(HoleType.Wrapper, false);
				currentHole.setHoleTypeOptions(new HoleType[] { HoleType.SwitchEntry });

				HoleNode holeNode = new HoleNode(HoleType.Expression, false);
				holeNode.setHoleTypeOptionsOfOnlyOne(HoleType.Expr5);
				currentHole.addChild(holeNode);

				currentHole.addChild(new HoleNode());
			}
		} else if (parentNodeClassStr != null && parentNodeClassStr.equals("SwitchEntry")) {
			this.generateSwitchEntry(parent, node, holeIndex, currentHole, holeTypeExpr);
		} else if (parentNodeClassStr != null && parentNodeClassStr.equals("FieldDeclaration")) {
			NodeList<VariableDeclarator> variableDeclarators = ((FieldDeclaration) parent.getLeft()).getVariables();
			VariableDeclarator vNode = variableDeclarators.get(0);
			vNode.setInitializer((Expression) node);

			currentHole.set(HoleType.VariableDeclarator, false);
			HoleNode holeNode = new HoleNode();
			holeNode.setHoleTypeOptions(new HoleType[] { HoleType.BodyDeclaration });
			parentOfParentHole.addChild(holeNode);
		} else if (parentNodeClassStr != null && parentNodeClassStr.equals("ExpressionStmt")) {
			this.generateExpForExpressionStmt(parent, node, holeIndex, currentHole, parentOfParentHole, holeTypeExpr);
		} else if (parentNodeClassStr != null && parentNodeClassStr.equals("VariableDeclarationExpr")) {
			VariableDeclarationExpr variableDeclarationExpr = (VariableDeclarationExpr) parent.getLeft();
			NodeList<VariableDeclarator> variableDeclarators = variableDeclarationExpr.getVariables();
			variableDeclarators.get(0).setInitializer((Expression) node);
			currentHole.set(HoleType.Expression, false);

			HoleNode holeNode = new HoleNode();
			holeNode.setHoleTypeOptions(new HoleType[] { HoleType.Expression });
			parentOfParentHole.addChild(holeNode);
		} else if (parentNodeClassStr != null && parentNodeClassStr.equals("SwitchStmt")) {
			SwitchStmt switchStmt = (SwitchStmt) parent.getLeft();
			NodeList<SwitchEntry> switchEntries = switchStmt.getEntries();
			if (switchEntries.size() == 0) {
				SwitchEntry switchEntry = new SwitchEntry();
				NodeList<Expression> labels = new NodeList<Expression>();
				labels.add((Expression) node);
				switchEntry.setLabels(labels);
				switchEntries.add(switchEntry);

				currentHole.set(HoleType.SwitchEntries, false);

				HoleNode wrapperNode = new HoleNode(HoleType.Wrapper, false);
				wrapperNode.setHoleTypeOptions(new HoleType[] { HoleType.SwitchEntry });
				currentHole.addChild(wrapperNode);

				HoleNode holeNode = new HoleNode(HoleType.Expression, false);
				wrapperNode.addChild(holeNode);
				wrapperNode.addChild(new HoleNode());
			} else {

			}
		} else if (parentNodeClassStr != null && parentNodeClassStr.equals("ReturnStmt")) {
			this.generateReturn6(parent, node, currentHole, parentHole, parentOfParentHole, holeTypeExpr);
		} else if (parentNodeClassStr != null && parentNodeClassStr.equals("AssignExpr")) {
			this.generateExprInAssignExpr(parent, node, currentHole, parentOfParentHole, holeTypeExpr);
		} else if (parentNodeClassStr != null && parentNodeClassStr.equals("ObjectCreationExpr")) {
			ObjectCreationExpr objectCreationExpr = (ObjectCreationExpr) parent.getLeft();
			NodeList<Expression> arguments = objectCreationExpr.getArguments();
			arguments.add((Expression) node);

			currentHole.set(HoleType.Arguments, false);
			HoleNode holeNode = new HoleNode(HoleType.Wrapper, false);
			holeNode.setHoleTypeOptionsOfOnlyOne(HoleType.Argument);
			currentHole.addChild(holeNode);

			HoleNode childHoleNode = new HoleNode(holeTypeExpr, false);
			holeNode.addChild(childHoleNode);

			HoleNode newHole = new HoleNode();
			currentHole.addChild(newHole);
		} else if (parentHoleType.equals(HoleType.Arguments)) {
			this.generateExprInArguments(parent, node, currentHole, parentHole, holeTypeExpr);
		} else if (parentNodeClassStr != null && parentNodeClassStr.equals("ExpressionStmt")) {
			// body part, expression
			this.generateExpForExpressionStmt(parent, node, holeIndex, currentHole, parentOfParentHole, holeTypeExpr);
		} else if (parentNodeClassStr != null && parentNodeClassStr.equals("EnclosedExpr")) {
			this.generateExprForEnclosedExpr(parent, node, currentHole, parentOfParentHole, holeTypeExpr);
		}
	}

	private void generateExprForLetInStmts(Either<Node, Either<List<?>, NodeList<?>>> parent, Node node, int holeIndex,
			HoleNode currentHole, HoleNode parentHole, HoleType holeTypeExpr) {
		NodeList<Statement> statements = (NodeList<Statement>) parent.get().get();
		if (holeIndex < statements.size()) {
			// TODO
		} else {
			ExpressionStmt expressionStmt = new ExpressionStmt((Expression) node);
			statements.add(expressionStmt);
			currentHole.set(HoleType.Statement, false);
			HoleNode exprHole = new HoleNode(HoleType.Expression, false);
			currentHole.addChild(exprHole);
			HoleNode let3Hole = new HoleNode(holeTypeExpr, false);
			exprHole.addChild(let3Hole);
			parentHole.addChild(new HoleNode());
		}
	}

	private void generateExprStmtForLet1AndLet2(Either<Node, Either<List<?>, NodeList<?>>> parent, Node node,
			int holeIndex, HoleNode currentHole, HoleNode parentHole, HoleType holeTypeExpr) {
		NodeList<Statement> statements = (NodeList<Statement>) parent.get().get();
		if (holeIndex < statements.size()) {
			// TODO
		} else {
			ExpressionStmt expressionStmt = new ExpressionStmt((Expression) node);
			statements.add(expressionStmt);
			HoleNode exprHole = this.constructHoleASTOfAssignStmtForLet1AndLet2(holeTypeExpr).getIthChild(0);
			currentHole.set(HoleType.Wrapper, false, HoleType.Statement);
			currentHole.addChild(exprHole);
		}
	}

	private HoleNode constructHoleASTOfAssignStmtForLet1AndLet2(HoleType holeTypeExpr) {
		HoleNode stmtNode = new HoleNode(HoleType.Wrapper, false, HoleType.Statement);

		HoleNode exprNode = new HoleNode(HoleType.Expression, false);
		stmtNode.addChild(exprNode);

		HoleNode exprWrapper = new HoleNode(HoleType.Wrapper, false);
		exprWrapper.setHoleTypeOptionsOfOnlyOne(holeTypeExpr);
		exprNode.addChild(exprWrapper);

		HoleNode targetNode = new HoleNode(HoleType.AssignExprTarget, false);
		exprWrapper.addChild(targetNode);

		HoleNode assignValueHole = new HoleNode(HoleType.AssignExprValue, false);
		exprWrapper.addChild(assignValueHole);

		HoleNode methodCallHole = new HoleNode(HoleType.Wrapper, false, HoleType.MethodCallExpr);
		assignValueHole.addChild(methodCallHole);

		HoleNode argsHole = new HoleNode(HoleType.Arguments, false);
		methodCallHole.addChild(argsHole);
		argsHole.addChild(new HoleNode());

		return stmtNode;
	}

	private HoleNode constructHoleASTOfReturnStmtForReturn1AndReturn2(HoleType holeTypeExpr) {
		HoleNode stmtNode = new HoleNode(HoleType.Wrapper, false, holeTypeExpr);

		HoleNode exprNode = new HoleNode(HoleType.Expression, false);
		stmtNode.addChild(exprNode);

		HoleNode methodCallHole = new HoleNode(HoleType.Wrapper, false, HoleType.MethodCallExpr);
		exprNode.addChild(methodCallHole);

		HoleNode argsHole = new HoleNode(HoleType.Arguments, false);
		methodCallHole.addChild(argsHole);
		argsHole.addChild(new HoleNode());

		return stmtNode;
	}
}
