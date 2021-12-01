package cn.edu.lyun.kexin.text2ast;

import com.github.javaparser.ast.Node;
import cn.edu.lyun.kexin.text2pattern.pattern.Pattern;

public class ASTManager {

	public static Node generate(Pattern pattern) {
		switch (pattern.getName()) {
			case "package":
				return new PackageAST().generate(pattern);
			case "import":
				return new ImportAST().generate(pattern);
			case "interface":
				return new InterfaceAST().generate(pattern);
			case "class":
				return new ClassAST().generate(pattern);
			case "constructor":
				return new ConstructorAST().generate(pattern);
			case "method":
				return new MethodAST().generate(pattern);
			case "arrowFunction":
				return null;
			case "field":
				return new FieldAST().generate(pattern);
			case "typeExtends":
				return new TypeExtendAST().generate(pattern);
			case "typeVariable":
				return new TypeVariableAST().generate(pattern);
			case "for":
				return new ForAST().generate(pattern);
			case "while":
				return new WhileAST().generate(pattern);
			case "if":
				return new IfAST().generate(pattern);
			case "switch":
				return new SwitchAST().generate(pattern);
			case "tryCatch":
				return new TryAST().generate(pattern);
			case "override":
				return null;
			case "subexpression":
				return new SubExpressionAST().generate(pattern);
			case "break":
				return new BreakAST().generate(pattern);
			case "continue":
				return new ContinueAST().generate(pattern);
			case "newInstance":
				return new NewInstanceAST().generate(pattern);
			case "throw":
				return new ThrowAST().generate(pattern);
			case "let1":
				return new LetStmtAST().generate(pattern);
			case "let2":
				return new LetStmtAST2().generate(pattern);
			case "let3":
				return new LetStmtAST3().generate(pattern);
			case "let4":
				return new LetStmtAST4().generate(pattern);
			case "let5":
				return new LetStmtAST5().generate(pattern);
			case "let6":
				return new LetStmtAST6().generate(pattern);
			case "return1":
				return new ReturnStmtAST1().generate(pattern);
			case "return2":
				return new ReturnStmtAST2().generate(pattern);
			case "return3":
				return new ReturnStmtAST3().generate(pattern);
			case "return4":
				return new ReturnStmtAST4().generate(pattern);
			case "return5":
				return new ReturnStmtAST5().generate(pattern);
			case "return6":
				return new ReturnStmtAST6().generate(pattern);
			case "expr1":
				return new ExprAST().generate(pattern);
			case "expr2":
				return new ExprAST2().generate(pattern);
			case "expr3":
				return new ExprAST3().generate(pattern);
			case "expr4":
				return new ExprAST4().generate(pattern);
			case "expr5":
				return new ExprAST5().generate(pattern);
			case "expr6":
				return new ExprAST6().generate(pattern);
			case "expr7":
				return new ExprAST7().generate(pattern);
			case "expr8":
				return new ExprAST8().generate(pattern);
			case "expr9":
				return new ExprAST9().generate(pattern);
			case "expr10":
				return new ExprAST10().generate(pattern);
			case "expr11":
				return new ExprAST11().generate(pattern);
			case "expr12":
				return new ExprAST12().generate(pattern);
			case "expr13":
				return new ExprAST13().generate(pattern);
			case "expr14":
				return new ExprAST14().generate(pattern);
			case "expr15":
				return new ExprAST15().generate(pattern);
		}

		return null;
	}
}
