package cn.edu.anonymous.kexin.test.text2ast;

import com.github.javaparser.ast.*;

import cn.edu.anonymous.kexin.text2ast.PackageAST;
import cn.edu.anonymous.kexin.text2pattern.nfa.RegexSet;
import cn.edu.anonymous.kexin.text2pattern.pattern.Pattern;
import cn.edu.anonymous.kexin.text2pattern.pattern.PatternSet;

public class PackageASTTest {
	public static void main(String[] args) {
		String[] textList = { "define package hello", "define package hello dot world",
				"define package hello dot world dot star" };

		PackageAST packageAST = new PackageAST();

		for (String text : textList) {
			Pattern pattern = RegexSet.compile(new PatternSet()).matchPattern(text);
			Node n = packageAST.generate(pattern);
			System.out.println(n.toString());
		}
	}

}
