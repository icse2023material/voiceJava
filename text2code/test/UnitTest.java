package test;

import lyun.voice2code.*;

public class UnitTest {

	public static void main(String[] args) {
		Unit any = new Unit();
		System.out.println(any.toVoiceJavaPattern());
		Unit key = new Unit("define");
		System.out.println(key.toVoiceJavaPattern());
		Unit star = new Unit("asterisk", new Unit("dot"), new Unit());
		System.out.println(star.toVoiceJavaPattern());
		Unit starOr = new Unit("asterisk", new Unit("dot"), new Unit("or", new Unit(), new Unit("star")));
		System.out.println(starOr.toVoiceJavaPattern());
	}
}
