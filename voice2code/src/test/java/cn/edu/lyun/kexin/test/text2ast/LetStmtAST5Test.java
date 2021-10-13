package cn.edu.lyun.kexin.test.text2ast;

import cn.edu.lyun.kexin.text2ast.*;
import cn.edu.lyun.kexin.text2pattern.nfa.RegexSet;
import cn.edu.lyun.kexin.text2pattern.pattern.Pattern;
import cn.edu.lyun.kexin.text2pattern.pattern.PatternSet;
import com.github.javaparser.ast.*;

public class LetStmtAST5Test {

	public static void main(String[] args) {
		// let _ [dot _]? equal (int | byte | short | long | char | float | double |
		// boolean | String) _
		String[] textList = { "let world equal int 1", "let hello dot world equal string nice" };

		for (String text : textList) {
			Pattern pattern = RegexSet.compile(new PatternSet()).matchPattern(text);
			System.out.println(pattern.showInstance());

			AST stmt = new LetStmtAST5();
			Node node = stmt.generate(pattern);
			System.out.println(node);
		}
	}
}
