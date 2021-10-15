package cn.edu.lyun.kexin.test.text2ast;

import cn.edu.lyun.kexin.text2pattern.nfa.RegexSet;
import cn.edu.lyun.kexin.text2pattern.pattern.Pattern;
import cn.edu.lyun.kexin.text2pattern.pattern.PatternSet;
import cn.edu.lyun.kexin.text2ast.*;
import com.github.javaparser.ast.*;

public class InterfaceASTTest {
	public static void main(String[] args) {
		String[] textList = { "define public interface hello" };
		for (String text : textList) {
			Pattern pattern = RegexSet.compile(new PatternSet()).matchPattern(text);
			// System.out.println(pattern.getPattern());
			// System.out.println(pattern.toVoiceJavaPattern());
			// System.out.println(pattern.showInstance());

			InterfaceAST interfaceAST = new InterfaceAST();
			Node node = interfaceAST.generate(pattern);
			System.out.println(node);

			// Unit[] units = new Unit[] { new Unit("public"), new Unit("interface"), new
			// Unit(), new Unit() };
			// List<Unit> unitList = new ArrayList<>(Arrays.asList(units));
			// Pair<List<Unit>, List<Unit>> pair = interfaceAST.splitList(unitList,
			// "interface");
			// System.out.println(pair.getFirst());
			// System.out.println(pair.getSecond());
		}
	}
}
