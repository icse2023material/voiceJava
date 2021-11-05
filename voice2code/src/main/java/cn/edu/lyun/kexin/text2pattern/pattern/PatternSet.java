package cn.edu.lyun.kexin.text2pattern.pattern;

import java.util.ArrayList;

public class PatternSet {
	private ArrayList<Pattern> patSet;

	public PatternSet() {
		patSet = new ArrayList<Pattern>();
		// Pattern packagePat = new Pattern("define package _ [dot _]*", new Unit[] {
		// new Unit("define"), new Unit("package"),
		// new Unit(), new Unit("asterisk", new Unit("dot"), new Unit()) });
		Pattern packagePat = new Pattern("package", "define package [_]+ [dot [_]+]*",
				new Unit[] { new Unit("define"), new Unit("package"), new Unit("plus", new Unit()),
						new Unit("asterisk", new Unit("dot"), new Unit("plus", new Unit())) });

		patSet.add(packagePat);
		Pattern importPat = new Pattern("import", "import static? _ [dot [_|star]]*",
				new Unit[] { new Unit("import"), new Unit("question", new Unit("static")), new Unit(),
						new Unit("asterisk", new Unit("dot"), new Unit("or", new Unit(), new Unit("star"))) });
		patSet.add(importPat);

		Unit classModifier = new Unit("or", new Unit("annotation"),
				new Unit("or", new Unit("public"),
						new Unit("or", new Unit("protected"),
								new Unit("or", new Unit("private"), new Unit("or", new Unit("abstract"),
										new Unit("or", new Unit("static"), new Unit("or", new Unit("final"), new Unit("strictfp"))))))));

		Pattern interfacePat = new Pattern("interface", "define [public|private] interface _",
				new Unit[] { new Unit("define"), classModifier, new Unit("interface"), new Unit() });
		patSet.add(interfacePat);
		Pattern classPat = new Pattern("class",
				"define [Annotation|public|protected|private|abstract|static|final|strictfp]* class _ [extends _]? [implements _]?",
				new Unit[] { new Unit("define"), new Unit("asterisk", classModifier), new Unit("class"), new Unit(),
						new Unit("question", new Unit("extends"), new Unit()),
						new Unit("question", new Unit("implements"), new Unit()) });
		patSet.add(classPat);
		Pattern constructorPat = new Pattern("constructor", "define constructor",
				new Unit[] { new Unit("define"), new Unit("constructor") });
		patSet.add(constructorPat);
		Unit methodModifier = new Unit("or", new Unit("annotation"),
				new Unit("or", new Unit("public"),
						new Unit("or", new Unit("protected"),
								new Unit("or", new Unit("private"),
										new Unit("or", new Unit("abstract"),
												new Unit("or", new Unit("static"), new Unit("or", new Unit("final"), new Unit("or",
														new Unit("synchronized"), new Unit("or", new Unit("native"), new Unit("strictfp"))))))))));
		Pattern methodPat = new Pattern("method",
				"define [Annotation|public|protected|private|abstract|static|final|synchronized|native|strictfp]* function _ [throws Exception]?",
				new Unit[] { new Unit("define"), new Unit("asterisk", methodModifier), new Unit("function"), new Unit(),
						new Unit("question", new Unit("throws"), new Unit("exception")) });
		patSet.add(methodPat);
		Pattern arrowFunctionPat = new Pattern("arrowFunction", "define arrow function",
				new Unit[] { new Unit("define"), new Unit("arrow"), new Unit("function") });
		patSet.add(arrowFunctionPat);
		// shoule be put before fieldPat
		Pattern forPat = new Pattern("for", "define [enhanced] for",
				new Unit[] { new Unit("define"), new Unit("question", new Unit("enhanced")), new Unit("for") });
		patSet.add(forPat);
		Pattern whilePat = new Pattern("while", "define [do]? while",
				new Unit[] { new Unit("define"), new Unit("question", new Unit("do")), new Unit("while") });
		patSet.add(whilePat);
		Pattern ifPat = new Pattern("if", "define if", new Unit[] { new Unit("define"), new Unit("if") });
		patSet.add(ifPat);
		Pattern switchPat = new Pattern("switch", "define switch", new Unit[] { new Unit("define"), new Unit("switch") });
		patSet.add(switchPat);
		Pattern tryCatchPat = new Pattern("tryCatch", "define try catch",
				new Unit[] { new Unit("define"), new Unit("try"), new Unit("catch") });
		patSet.add(tryCatchPat);
		Pattern atOverridePat = new Pattern("override", "define at override",
				new Unit[] { new Unit("define"), new Unit("at"), new Unit("override") });
		patSet.add(atOverridePat);

		Unit fieldModifier = new Unit("or", new Unit("annotation"),
				new Unit("or", new Unit("public"),
						new Unit("or", new Unit("protected"), new Unit("or", new Unit("private"), new Unit("or", new Unit("static"),
								new Unit("or", new Unit("final"), new Unit("or", new Unit("transient"), new Unit("volatile"))))))));
		Pattern fieldPat = new Pattern("field",
				"define [Annotation|public|protected|private|static|final|transient|volatile]* (_ list | _ [dot _]? [with _+]?) variable _ ",
				new Unit[] { new Unit("define"), new Unit("asterisk", fieldModifier),
						new Unit("or", new Unit("normal", new Unit(), new Unit("list")),
								new Unit(new Unit[] { new Unit(), new Unit("question", new Unit("dot"), new Unit()),
										new Unit("question", new Unit("with"), new Unit("plus", new Unit())) })),
						new Unit("variable"), new Unit() });
		patSet.add(fieldPat);
		Pattern typePat = new Pattern("typeExtends", "type _ [extends _]?",
				new Unit[] { new Unit("type"), new Unit(), new Unit("question", new Unit("extends"), new Unit()) });
		patSet.add(typePat);
		Pattern typePat2 = new Pattern("typeVariable", "type (_ list | _ [dot _]? [with _+]?) variable _",
				new Unit[] { new Unit("type"),
						new Unit("or", new Unit("normal", new Unit(), new Unit("list")),
								new Unit(new Unit[] { new Unit(), new Unit("question", new Unit("dot"), new Unit()),
										new Unit("question", new Unit("with"), new Unit("plus", new Unit())) })),
						new Unit("variable"), new Unit() });
		patSet.add(typePat2);
		Pattern subExpressionPat = new Pattern("subexpression", "subexpression", new Unit[] { new Unit("subexpression") });
		patSet.add(subExpressionPat);
		Pattern breakPat = new Pattern("break", "break", new Unit[] { new Unit("break") });
		patSet.add(breakPat);
		Pattern continuePat = new Pattern("continue", "continue", new Unit[] { new Unit("continue") });
		patSet.add(continuePat);
		Pattern newInstancePat = new Pattern("newInstance", "new instance _ [dot _]*", new Unit[] { new Unit("new"),
				new Unit("instance"), new Unit(), new Unit("asterisk", new Unit("dot"), new Unit()) });
		patSet.add(newInstancePat);
		Pattern throwPat = new Pattern("throw", "throw new _",
				new Unit[] { new Unit("throw"), new Unit("new"), new Unit() });
		patSet.add(throwPat);
		Pattern moveNextPat = new Pattern("moveNext", "move next", new Unit[] { new Unit("move"), new Unit("next") });
		patSet.add(moveNextPat);
		Pattern jumpOutPat = new Pattern("jumpOut", "jump out", new Unit[] { new Unit("jump"), new Unit("out") });
		patSet.add(jumpOutPat);
		Pattern jumpBeforePat = new Pattern("jumpBefore", "jump before _",
				new Unit[] { new Unit("jump"), new Unit("before"), new Unit() });
		patSet.add(jumpBeforePat);
		Pattern jumpAfterPat = new Pattern("jumpAfter", "jump after _",
				new Unit[] { new Unit("jump"), new Unit("after"), new Unit() });
		patSet.add(jumpAfterPat);
		Pattern jumpToPat = new Pattern("jumpToLine", "jump to line [_]? [start | end]?",
				new Unit[] { new Unit("jump"), new Unit("to"), new Unit("line"), new Unit("question", new Unit()),
						new Unit("question", new Unit("or", new Unit("start"), new Unit("end"))) });
		patSet.add(jumpToPat);
		Pattern upPat = new Pattern("up", "up [_ lines]?",
				new Unit[] { new Unit("up"), new Unit("question", new Unit("normal", new Unit(), new Unit("lines"))) });
		patSet.add(upPat);
		Pattern downPat = new Pattern("down", "down [_ lines]?",
				new Unit[] { new Unit("down"), new Unit("question", new Unit("normal", new Unit(), new Unit("lines"))) });
		patSet.add(downPat);
		Pattern leftPat = new Pattern("left", "left", new Unit[] { new Unit("left") });
		patSet.add(leftPat);
		Pattern rightPat = new Pattern("right", "right", new Unit[] { new Unit("right") });
		patSet.add(rightPat);
		Pattern selectLinePat = new Pattern("selectLine", "select line",
				new Unit[] { new Unit("select"), new Unit("line") });
		patSet.add(selectLinePat);
		Pattern selectBodyPat = new Pattern("selectBody", "select body",
				new Unit[] { new Unit("select"), new Unit("body") });
		patSet.add(selectBodyPat);
		Pattern selectNamePat = new Pattern("selectName", "select _", new Unit[] { new Unit("select"), new Unit() });
		patSet.add(selectNamePat);
		Pattern selectFunctionPat = new Pattern("selectFunction", "select function [_]?",
				new Unit[] { new Unit("select"), new Unit("function"), new Unit("question", new Unit()) });
		patSet.add(selectFunctionPat);
		Pattern replacePat = new Pattern("replace", "replace _ to _",
				new Unit[] { new Unit("replace"), new Unit(), new Unit("to"), new Unit() });
		patSet.add(replacePat);
		Pattern deletePat = new Pattern("delete", "delete", new Unit[] { new Unit("delete") });
		patSet.add(deletePat);
		Pattern let1Pat = new Pattern("let1", "let _ [dot _]? equal call _ ", new Unit[] { new Unit("let"), new Unit(),
				new Unit("question", new Unit("dot"), new Unit()), new Unit("equal"), new Unit("call"), new Unit() });
		patSet.add(let1Pat);
		Pattern let2Pat = new Pattern("let2", "let _ [dot _]? equal _ [call _]+",
				new Unit[] { new Unit("let"), new Unit(), new Unit("question", new Unit("dot"), new Unit()), new Unit("equal"),
						new Unit(), new Unit("plus", new Unit("call"), new Unit()) });
		patSet.add(let2Pat);
		// let6 must be put before let3
		Pattern let6Pat = new Pattern("let6", "let _ [dot _]? equal [expression]? ",
				new Unit[] { new Unit("let"), new Unit(), new Unit("question", new Unit("dot"), new Unit()), new Unit("equal"),
						new Unit("question", new Unit("expression")) });
		patSet.add(let6Pat);
		Pattern let3Pat = new Pattern("let3", "let _ [dot _]? equal _ [dot _]* ",
				new Unit[] { new Unit("let"), new Unit(), new Unit("question", new Unit("dot"), new Unit()), new Unit("equal"),
						new Unit(), new Unit("asterisk", new Unit("dot"), new Unit()) });
		patSet.add(let3Pat);
		Pattern let4Pat = new Pattern("let4", "let _ [dot _]? equal [variable]? _",
				new Unit[] { new Unit("let"), new Unit(), new Unit("question", new Unit("dot"), new Unit()), new Unit("equal"),
						new Unit("question", new Unit("variable")), new Unit() });
		patSet.add(let4Pat);
		Unit typeUnit = new Unit("or", new Unit("int"),
				new Unit("or", new Unit("byte"),
						new Unit("or", new Unit("short"),
								new Unit("or", new Unit("long"), new Unit("or", new Unit("char"), new Unit("or", new Unit("float"),
										new Unit("or", new Unit("double"), new Unit("or", new Unit("boolean"), new Unit("string")))))))));
		Pattern let5Pat = new Pattern("let5",
				"let _ [dot _]? equal (int | byte | short | long | char | float | double | boolean | String) _ ",
				new Unit[] { new Unit("let"), new Unit(), new Unit("question", new Unit("dot"), new Unit()), new Unit("equal"),
						typeUnit, new Unit() });
		patSet.add(let5Pat);
		Pattern return1Pat = new Pattern("return1", "return call _",
				new Unit[] { new Unit("return"), new Unit("call"), new Unit() });
		patSet.add(return1Pat);
		Pattern return2Pat = new Pattern("return2", "return _ [dot _]* [call _]+",
				new Unit[] { new Unit("return"), new Unit(), new Unit("asterisk", new Unit("dot"), new Unit()),
						new Unit("plus", new Unit("call"), new Unit()) });
		patSet.add(return2Pat);
		Pattern return6Pat = new Pattern("return6", "return [expression]? ",
				new Unit[] { new Unit("return"), new Unit("question", new Unit("expression")) });
		patSet.add(return6Pat);
		Pattern return3Pat = new Pattern("return3", "return _ [dot _]*",
				new Unit[] { new Unit("return"), new Unit(), new Unit("asterisk", new Unit("dot"), new Unit()) });
		patSet.add(return3Pat);
		Pattern return4Pat = new Pattern("return4", "return [variable]? _",
				new Unit[] { new Unit("return"), new Unit("question", new Unit("variable")), new Unit() });
		patSet.add(return4Pat);
		Pattern return5Pat = new Pattern("return5",
				"return (int | byte | short | long | char | float | double | boolean | String) _ ",
				new Unit[] { new Unit("return"), typeUnit, new Unit() });
		patSet.add(return5Pat);
		Pattern expr1Pat = new Pattern("expr1", "[expression]? call _",
				new Unit[] { new Unit("question", new Unit("expression")), new Unit("call"), new Unit() });
		patSet.add(expr1Pat);
		Pattern expr2Pat = new Pattern("expr2", "[expression]? _ [dot _]* [call _]+",
				new Unit[] { new Unit("question", new Unit("expression")), new Unit(),
						new Unit("asterisk", new Unit("dot"), new Unit()), new Unit("plus", new Unit("call"), new Unit()) });
		patSet.add(expr2Pat);
		Pattern expr3Pat = new Pattern("expr3", "[expression]? _ [dot _]*", new Unit[] {
				new Unit("question", new Unit("expression")), new Unit(), new Unit("asterisk", new Unit("dot"), new Unit()) });
		patSet.add(expr3Pat);
		Pattern expr4Pat = new Pattern("expr4", "[expression]? [variable]? _ ", new Unit[] {
				new Unit("question", new Unit("expression")), new Unit("question", new Unit("variable")), new Unit() });
		patSet.add(expr4Pat);
		Pattern expr5Pat = new Pattern("expr5",
				"[expression]? (int | byte | short | long | char | float | double | boolean | String) _ ",
				new Unit[] { new Unit("question", new Unit("expression")), typeUnit, new Unit() });
		patSet.add(expr5Pat);
		Pattern expr6Pat = new Pattern("expr6", "[expression]? _ plus plus",
				new Unit[] { new Unit("question", new Unit("expression")), new Unit(), new Unit("plus"), new Unit("plus") });
		patSet.add(expr6Pat);
		Pattern expr7Pat = new Pattern("expr7", "[expression]? _ minus minus",
				new Unit[] { new Unit("question", new Unit("expression")), new Unit(), new Unit("minus"), new Unit("minus") });
		patSet.add(expr7Pat);

		Pattern expr8Pat = new Pattern("expr8", "[expression]? plus plus _",
				new Unit[] { new Unit("question", new Unit("expression")), new Unit("plus"), new Unit("plus"), new Unit() });
		patSet.add(expr8Pat);
		Pattern expr9Pat = new Pattern("expr9", "[expression]? minus minus _",
				new Unit[] { new Unit("question", new Unit("expression")), new Unit("minus"), new Unit("minus"), new Unit() });
		patSet.add(expr9Pat);

		Unit opUnit = new Unit("or", new Unit("plus"), new Unit("or", new Unit("minus"),
				new Unit("or", new Unit("times"), new Unit("or", new Unit("divide"), new Unit("mod")))));
		Unit compareUnit = new Unit("or", new Unit("normal", new Unit("less"), new Unit("than")),
				new Unit("or", new Unit("normal", new Unit("less"), new Unit("equal")),
						new Unit("or", new Unit("normal", new Unit("greater"), new Unit("than")),
								new Unit("or", new Unit("normal", new Unit("greater"), new Unit("equal")),
										new Unit("or", new Unit("normal", new Unit("double"), new Unit("equal")),
												new Unit("or", new Unit("and"), new Unit("normal", new Unit("double"), new Unit("and"))))))));

		Pattern expr10Pat = new Pattern("expr10", "[expression]? expression (op | compare) expression",
				new Unit[] { new Unit("question", new Unit("expression")), new Unit("expression"),
						new Unit("or", opUnit, compareUnit), new Unit("expression") });
		patSet.add(expr10Pat);
		Pattern expr11Pat = new Pattern("expr11", "[expression]? _ (op | compare) expression",
				new Unit[] { new Unit("question", new Unit("expression")), new Unit(), new Unit("or", opUnit, compareUnit),
						new Unit("expression") });
		patSet.add(expr11Pat);
		Pattern expr12Pat = new Pattern("expr12", "[expression]? _ (op | compare) _", new Unit[] {
				new Unit("question", new Unit("expression")), new Unit(), new Unit("or", opUnit, compareUnit), new Unit() });
		patSet.add(expr12Pat);

		Pattern expr13Pat = new Pattern("expr13", "[expression]? variable _ index _",
				new Unit[] { new Unit("question", new Unit("expression")), new Unit("variable"), new Unit(), new Unit("index"),
						new Unit() });
		patSet.add(expr13Pat);

		Pattern subExpr1Pat = new Pattern("subexpr1", "subexpression call _",
				new Unit[] { new Unit("subexpression"), new Unit("call"), new Unit() });
		patSet.add(subExpr1Pat);
		Pattern subExpr2Pat = new Pattern("subexpr2", "subexpression _ [call _]+",
				new Unit[] { new Unit("subexpression"), new Unit(), new Unit("plus", new Unit("call"), new Unit()) });
		patSet.add(subExpr2Pat);
		Pattern subExpr3Pat = new Pattern("subexpr3", "subexpression _ [dot _]?",
				new Unit[] { new Unit("subexpression"), new Unit(), new Unit("question", new Unit("dot"), new Unit()) });
		patSet.add(subExpr3Pat);
		Pattern subExpr4Pat = new Pattern("subexpr4", "subexpression [variable]? _ ",
				new Unit[] { new Unit("subexpression"), new Unit("question", new Unit("variable")), new Unit() });
		patSet.add(subExpr4Pat);
		Pattern subExpr5Pat = new Pattern("subexpr5",
				"subexpression (int | byte | short | long | char | float | double | boolean | String) _ ",
				new Unit[] { new Unit("subexpression"), typeUnit, new Unit() });
		patSet.add(subExpr5Pat);
		Pattern subExpr6Pat = new Pattern("subexpr6", "subexpression _ plus plus",
				new Unit[] { new Unit("subexpression"), new Unit(), new Unit("plus"), new Unit("plus") });
		patSet.add(subExpr6Pat);
		Pattern subExpr7Pat = new Pattern("subexpr7", "expression _ minus minus",
				new Unit[] { new Unit("subexpression"), new Unit(), new Unit("minus"), new Unit("minus") });
		patSet.add(subExpr7Pat);

		Pattern subExpr8Pat = new Pattern("subexpr8", "subexpression plus plus _",
				new Unit[] { new Unit("subexpression"), new Unit("plus"), new Unit("plus"), new Unit() });
		patSet.add(subExpr8Pat);
		Pattern subExpr9Pat = new Pattern("subexpr9", "subexpression minus minus _",
				new Unit[] { new Unit("subexpression"), new Unit("minus"), new Unit("minus"), new Unit() });
		patSet.add(subExpr9Pat);

		Pattern subExpr10Pat = new Pattern("subexpr10", "subexpression subexpression (op | compare) subexpression",
				new Unit[] { new Unit("subexpression"), new Unit("subexpression"), new Unit("or", opUnit, compareUnit),
						new Unit("subexpression") });
		patSet.add(subExpr10Pat);
		Pattern subExpr11Pat = new Pattern("subexpr11", "subexpression _ (op | compare) subexpression", new Unit[] {
				new Unit("subexpression"), new Unit(), new Unit("or", opUnit, compareUnit), new Unit("subexpression") });
		patSet.add(subExpr11Pat);
		Pattern subExpr12Pat = new Pattern("subexpr12", "subexpression _ (op | compare) _",
				new Unit[] { new Unit("subexpression"), new Unit(), new Unit("or", opUnit, compareUnit), new Unit() });
		patSet.add(subExpr12Pat);

		Pattern subExpr13Pat = new Pattern("subexpr13", "[subexpression]? variable _ index _",
				new Unit[] { new Unit("question", new Unit("subexpression")), new Unit("variable"), new Unit(),
						new Unit("index"), new Unit() });
		patSet.add(subExpr13Pat);
	}

	public void addToSet(Pattern pat) {
		patSet.add(pat);
	}

	public int size() {
		return patSet.size();
	}

	public ArrayList<Pattern> getPatternSet() {
		return this.patSet;
	}

	@Override
	public String toString() {
		String str = "";
		for (Pattern pattern : patSet) {
			str = str != "" ? str + "\n" + pattern.toVoiceJavaPattern() : str + pattern.toVoiceJavaPattern();
		}

		return str;
	}

}
