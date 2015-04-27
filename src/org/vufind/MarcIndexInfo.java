package org.vufind;

public class MarcIndexInfo {
	private long checksum;
	private boolean EContent;

	public long getChecksum() {
		return checksum;
	}
	public void setChecksum(long checksum) {
		this.checksum = checksum;
	}
	public boolean isEContent() {
		return EContent;
	}
	public void setEContent(boolean eContent) {
		this.EContent = eContent;
	}
	
}
