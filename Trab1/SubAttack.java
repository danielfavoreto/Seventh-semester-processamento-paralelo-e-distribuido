package br.inf.ufes.pp2017_01.imp;

import java.util.UUID;

public class SubAttack {

	private byte[] ciphertext, knowntext;
	private long time;
	private long initialindex,finalindex;
	private UUID slaveKey;
	
	public UUID getSlaveKey() {
		return slaveKey;
	}
	public void setSlaveKey(UUID slaveKey) {
		this.slaveKey = slaveKey;
	}
	public SubAttack(byte[] ciphertext, byte[] knowntext, UUID slaveKey, long time, long initialindex,long finalindex)
	{
		this.ciphertext = ciphertext;
		this.knowntext = knowntext;
		this.time = time;
		this.initialindex  = initialindex;
		this.finalindex = finalindex;
		this.slaveKey = slaveKey;
	}

	public long getTime() {
		return time;
	}
	public void setTime(long time) {
		this.time = time;
	}
	public long getInitialindex() {
		return initialindex;
	}
	public void setInitialindex(long initialindex) {
		this.initialindex = initialindex;
	}
	public long getFinalindex() {
		return finalindex;
	}
	public void setFinalindex(long finalindex) {
		this.finalindex = finalindex;
	}
	public byte[] getCiphertext() {
		return ciphertext;
	}
	public void setCiphertext(byte[] ciphertext) {
		this.ciphertext = ciphertext;
	}
	public byte[] getKnowntext() {
		return knowntext;
	}
	public void setKnowntext(byte[] knowntext) {
		this.knowntext = knowntext;
	}
	
}
