package cn.edu.anonymous.kexin.text2pattern.nfa.strategy;

public class EpsilonMatchStrategy extends MatchStrategy {
	@Override
	public boolean isMatch(char c, String edge) {
		return true;
	}

	public boolean isMatch(String str, String edge) {
		return true;
	}

}