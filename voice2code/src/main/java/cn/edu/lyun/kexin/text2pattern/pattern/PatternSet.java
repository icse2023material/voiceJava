package cn.edu.lyun.kexin.text2pattern.pattern;

import java.util.ArrayList;
import org.apache.commons.lang3.ArrayUtils;

public class PatternSet {
	private ArrayList<Pattern> patSet;

	public PatternSet() {
		patSet = new ArrayList<Pattern>();

		Unit Name = new Unit("plus", new Unit());

		Pattern packagePat = new Pattern("package", "define package [_]+ [dot [_]+]*",
				new Unit[] { new Unit("define"), new Unit("package"), Name, new Unit("asterisk", new Unit("dot"), Name) });
		patSet.add(packagePat);

		Pattern importPat = new Pattern("import", "import static? [_]+ [dot [[_]+|star]]*",
				new Unit[] { new Unit("import"), new Unit("question", new Unit("static")), Name,
						new Unit("asterisk", new Unit("dot"), new Unit("or", Name, new Unit("star"))) });
		patSet.add(importPat);

		Unit classModifier = new Unit("or", new Unit("annotation"),
				new Unit("or", new Unit("public"),
						new Unit("or", new Unit("protected"),
								new Unit("or", new Unit("private"), new Unit("or", new Unit("abstract"),
										new Unit("or", new Unit("static"), new Unit("or", new Unit("final"), new Unit("strictfp"))))))));

		Pattern interfacePat = new Pattern("interface", "define [public|private] interface [_]+",
				new Unit[] { new Unit("define"), classModifier, new Unit("interface"), Name });
		patSet.add(interfacePat);

		Pattern classPat = new Pattern("class",
				"define [Annotation|public|protected|private|abstract|static|final|strictfp]* class [_]+ [extends [_]+]? [implements [_]+]?",
				new Unit[] { new Unit("define"), new Unit("asterisk", classModifier), new Unit("class"), Name,
						new Unit("question", new Unit("extends"), Name), new Unit("question", new Unit("implements"), Name) });
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
				"define [Annotation|public|protected|private|abstract|static|final|synchronized|native|strictfp]* function [_]+ [throws Exception]?",
				new Unit[] { new Unit("define"), new Unit("asterisk", methodModifier), new Unit("function"), Name,
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
				"define [Annotation|public|protected|private|static|final|transient|volatile]* ([_]+ list | [_]+ [dot [_]+]? [with [_]+ [and [_]+]*]?) variable [_]+ ",
				new Unit[] { new Unit("define"), new Unit("asterisk", fieldModifier),
						new Unit("or", new Unit("normal", Name, new Unit("list")),
								new Unit(new Unit[] { Name, new Unit("question", new Unit("dot"), Name),
										new Unit("question", new Unit("with"),
												new Unit("normal", Name, new Unit("asterisk", new Unit("and"), Name))) })),
						new Unit("variable"), new Unit() });
		patSet.add(fieldPat);

		// 定义参数
		Pattern typePat2 = new Pattern("typeVariable",
				"type ([_]+ list | [_]+ [dot [_]+]? [with [_]+ [and [_]+]*]?) variable [_]+",
				new Unit[] { new Unit("type"),
						new Unit("or", new Unit("normal", Name, new Unit("list")),
								new Unit(new Unit[] { Name, new Unit("question", new Unit("dot"), Name),
										new Unit("question", new Unit("with"),
												new Unit("normal", Name, new Unit("asterisk", new Unit("and"), Name))) })),
						new Unit("variable"), Name });
		patSet.add(typePat2);

		// 定义类型
		Pattern typePat = new Pattern("typeExtends",
				"type ([_]+ list | [_]+ [dot [_]+]? [with [_]+ [and [_]+]*]?) [extends [_]+]?",
				new Unit[] { new Unit("type"),
						new Unit("or", new Unit("normal", Name, new Unit("list")),
								new Unit(new Unit[] { Name, new Unit("question", new Unit("dot"), Name),
										new Unit("question", new Unit("with"),
												new Unit("normal", Name, new Unit("asterisk", new Unit("and"), Name))) })),
						new Unit("question", new Unit("extends"), Name) });
		patSet.add(typePat);

		Pattern subExpressionPat = new Pattern("subexpression", "subexpression", new Unit[] { new Unit("subexpression") });
		patSet.add(subExpressionPat);

		Pattern breakPat = new Pattern("break", "break", new Unit[] { new Unit("break") });
		patSet.add(breakPat);

		Pattern continuePat = new Pattern("continue", "continue", new Unit[] { new Unit("continue") });
		patSet.add(continuePat);

		Pattern newInstancePat = new Pattern("newInstance", "new instance [_]+ [dot [_]+]*",
				new Unit[] { new Unit("new"), new Unit("instance"), Name, new Unit("asterisk", new Unit("dot"), Name) });
		patSet.add(newInstancePat);

		Pattern throwPat = new Pattern("throw", "throw new [_]+", new Unit[] { new Unit("throw"), new Unit("new"), Name });
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

		Unit[] letUnits = new Unit[] { new Unit("let"), Name, new Unit("question", new Unit("dot"), Name),
				new Unit("equal") };
		Pattern let1Pat = new Pattern("let1", "let [_]+ [dot [_]+]? equal call [_]+ ",
				ArrayUtils.addAll(letUnits, new Unit[] { new Unit("call"), Name }));
		patSet.add(let1Pat);

		Pattern let2Pat = new Pattern("let2", "let [_]+ [dot [_]+]? equal [_]+ [call [_]+]+",
				ArrayUtils.addAll(letUnits, new Unit[] { Name, new Unit("plus", new Unit("call"), Name) }));
		patSet.add(let2Pat);

		// let6 must be put before let3
		Pattern let6Pat = new Pattern("let6", "let [_]+ [dot [_]+]? equal [expression]? ",
				ArrayUtils.addAll(letUnits, new Unit[] { new Unit("question", new Unit("expression")) }));
		patSet.add(let6Pat);

		Pattern let3Pat = new Pattern("let3", "let [_]+ [dot [_]+]? equal [_]+ [dot [_]+]* ",
				ArrayUtils.addAll(letUnits, new Unit[] { Name, new Unit("asterisk", new Unit("dot"), Name) }));
		patSet.add(let3Pat);

		Pattern let4Pat = new Pattern("let4", "let [_]+ [dot [_]+]? equal [variable]? [_]+",
				ArrayUtils.addAll(letUnits, new Unit[] { new Unit("question", new Unit("variable")), Name }));
		patSet.add(let4Pat);

		Unit typeUnit = new Unit("or", new Unit("int"),
				new Unit("or", new Unit("byte"),
						new Unit("or", new Unit("short"),
								new Unit("or", new Unit("long"), new Unit("or", new Unit("char"), new Unit("or", new Unit("float"),
										new Unit("or", new Unit("double"), new Unit("or", new Unit("boolean"), new Unit("string")))))))));
		Pattern let5Pat = new Pattern("let5",
				"let [_]+ [dot [_]+]? equal (int | byte | short | long | char | float | double | boolean | string) [_]+ ",
				ArrayUtils.addAll(letUnits, new Unit[] { typeUnit, Name }));
		patSet.add(let5Pat);

		Pattern return1Pat = new Pattern("return1", "return call [_]+",
				new Unit[] { new Unit("return"), new Unit("call"), Name });
		patSet.add(return1Pat);

		Pattern return2Pat = new Pattern("return2", "return [_]+ [dot [_]+]* [call [_]+]+", new Unit[] { new Unit("return"),
				Name, new Unit("asterisk", new Unit("dot"), Name), new Unit("plus", new Unit("call"), Name) });
		patSet.add(return2Pat);

		Pattern return6Pat = new Pattern("return6", "return [expression]? ",
				new Unit[] { new Unit("return"), new Unit("question", new Unit("expression")) });
		patSet.add(return6Pat);

		Pattern return3Pat = new Pattern("return3", "return [_]+ [dot [_]+]*",
				new Unit[] { new Unit("return"), Name, new Unit("asterisk", new Unit("dot"), Name) });
		patSet.add(return3Pat);

		Pattern return4Pat = new Pattern("return4", "return [variable]? [_]+",
				new Unit[] { new Unit("return"), new Unit("question", new Unit("variable")), Name });
		patSet.add(return4Pat);

		Pattern return5Pat = new Pattern("return5",
				"return (int | byte | short | long | char | float | double | boolean | String) []+ ",
				new Unit[] { new Unit("return"), typeUnit, Name });
		patSet.add(return5Pat);

		Pattern expr1Pat = new Pattern("expr1", "[expression]? call [_]+",
				new Unit[] { new Unit("question", new Unit("expression")), new Unit("call"), Name });
		patSet.add(expr1Pat);

		Pattern expr2Pat = new Pattern("expr2", "[expression]? [_]+ [dot [_]+]* [call [_]+]+",
				new Unit[] { new Unit("question", new Unit("expression")), Name, new Unit("asterisk", new Unit("dot"), Name),
						new Unit("plus", new Unit("call"), Name) });
		patSet.add(expr2Pat);

		Pattern expr3Pat = new Pattern("expr3", "[expression]? [_]+ [dot [_]+]*",
				new Unit[] { new Unit("question", new Unit("expression")), Name, new Unit("asterisk", new Unit("dot"), Name) });
		patSet.add(expr3Pat);

		Pattern expr4Pat = new Pattern("expr4", "[expression]? [variable]? [_]+ ",
				new Unit[] { new Unit("question", new Unit("expression")), new Unit("question", new Unit("variable")), Name });
		patSet.add(expr4Pat);

		Pattern expr5Pat = new Pattern("expr5",
				"[expression]? (int | byte | short | long | char | float | double | boolean | String) [_]+",
				new Unit[] { new Unit("question", new Unit("expression")), typeUnit, Name });
		patSet.add(expr5Pat);

		Pattern expr6Pat = new Pattern("expr6", "[expression]? [_]+ plus plus",
				new Unit[] { new Unit("question", new Unit("expression")), Name, new Unit("plus"), new Unit("plus") });
		patSet.add(expr6Pat);

		Pattern expr7Pat = new Pattern("expr7", "[expression]? [_]+ minus minus",
				new Unit[] { new Unit("question", new Unit("expression")), Name, new Unit("minus"), new Unit("minus") });
		patSet.add(expr7Pat);

		Pattern expr8Pat = new Pattern("expr8", "[expression]? plus plus [_]+",
				new Unit[] { new Unit("question", new Unit("expression")), new Unit("plus"), new Unit("plus"), Name });
		patSet.add(expr8Pat);

		Pattern expr9Pat = new Pattern("expr9", "[expression]? minus minus [_]+",
				new Unit[] { new Unit("question", new Unit("expression")), new Unit("minus"), new Unit("minus"), Name });
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

		Pattern expr11Pat = new Pattern("expr11", "[expression]? [_]+ (op | compare) expression",
				new Unit[] { new Unit("question", new Unit("expression")), Name, new Unit("or", opUnit, compareUnit),
						new Unit("expression") });
		patSet.add(expr11Pat);

		Pattern expr12Pat = new Pattern("expr12", "[expression]? [_]+ (op | compare) [_]+",
				new Unit[] { new Unit("question", new Unit("expression")), Name, new Unit("or", opUnit, compareUnit), Name });
		patSet.add(expr12Pat);

		Pattern expr13Pat = new Pattern("expr13", "[expression]? variable [_]+ index [_]+", new Unit[] {
				new Unit("question", new Unit("expression")), new Unit("variable"), Name, new Unit("index"), Name });
		patSet.add(expr13Pat);

		Pattern expr14Pat = new Pattern("expr14", "[expression]? string [_]+",
				new Unit[] { new Unit("question", new Unit("expression")), new Unit("string"), Name });
		patSet.add(expr14Pat);
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
