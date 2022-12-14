package cn.edu.anonymous.kexin.text2code.astskeleton;

import java.util.*;

public class HoleNode {
	private HoleType[] holeTypeOptions;
	private HoleType holeType;
	private boolean isHole;
	private HoleNode parent;

	private List<HoleNode> childList;

	public HoleNode() {
		this.isHole = true;
		this.holeType = HoleType.Undefined;
		this.childList = new ArrayList<HoleNode>();
		this.parent = null;
	}

	public HoleNode(HoleType holeType, boolean isHole) {
		this.holeType = holeType;
		this.isHole = isHole;
		this.childList = new ArrayList<HoleNode>();

	}

	/**
	 * @param holeType
	 * @param isHole
	 * @param realHoleType
	 */
	public HoleNode(HoleType holeType, boolean isHole, HoleType realHoleType) {
		this.holeType = holeType;
		this.isHole = isHole;
		this.holeTypeOptions = new HoleType[] { realHoleType };
		this.childList = new ArrayList<HoleNode>();
	}

	/**
	 * @param possibleHoleType
	 */
	public HoleNode(HoleType possibleHoleType) {
		this.isHole = true;
		this.holeType = HoleType.Undefined;
		this.holeTypeOptions = new HoleType[] { possibleHoleType };
		this.childList = new ArrayList<HoleNode>();
		this.parent = null;
	}

	public void setHoleTypeOptions(HoleType[] holeTypeOptions) {
		this.holeTypeOptions = holeTypeOptions;
	}

	public HoleType[] getHoleTypeOptions() {
		return this.holeTypeOptions;
	}

	public HoleType getHoleTypeOfOptionsIfOnlyOne() {
		if (this.holeTypeOptions != null && this.holeTypeOptions.length == 1) {
			return this.holeTypeOptions[0];
		} else {
			return null;
		}
	}

	public void setHoleTypeOptionsOfOnlyOne(HoleType holeType) {
		this.holeTypeOptions = new HoleType[] { holeType };
	}

	public void setHoleType(HoleType holeType) {
		this.holeType = holeType;
	}

	public HoleType getHoleType() {
		return this.holeType;
	}

	public void setIsHole(boolean isHole) {
		this.isHole = isHole;
	}

	public boolean getIsHole() {
		return this.isHole;
	}

	/**
	 * @param holeType
	 * @param isHole
	 */
	public void set(HoleType holeType, boolean isHole) {
		this.holeType = holeType;
		this.isHole = isHole;
	}

	public void set(HoleType holeType, boolean isHole, HoleType realType) {
		this.holeType = holeType;
		this.isHole = isHole;
		this.holeTypeOptions = new HoleType[] { realType };
	}

	public List<HoleNode> getChildList() {
		return this.childList;
	}

	public void addChild(HoleNode holeNode) {
		holeNode.setParent(this);
		this.childList.add(holeNode);
	}

	public HoleNode getIthChild(int index) {
		return this.childList.get(index);
	}

	public int getChildListSize() {
		return this.childList.size();
	}

	public int getNonUndefinedChildListSize() {
		int count = 0;
		for (HoleNode holeNode : childList) {
			if (!holeNode.getHoleType().equals(HoleType.Undefined)) {
				count++;
			}
		}
		return count;
	}

	public void deleteHole(int index) {
		this.childList.remove(index);
	}

	public void setParent(HoleNode parent) {
		this.parent = parent;
	}

	public HoleNode getParent() {
		return this.parent;
	}
}
