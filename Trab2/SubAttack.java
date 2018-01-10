import java.io.Serializable;

import javax.jms.*;

public class SubAttack implements Serializable{

	private byte[] ciphertext, knowntext;
	private int attackNumber;
	private long initialindex,finalindex;



	public SubAttack(byte[] ciphertext, byte[] knowntext,int attackNumber,long initialindex,long finalindex)
	{
		this.ciphertext = ciphertext;
		this.knowntext = knowntext;
		this.attackNumber = attackNumber;
		this.initialindex  = initialindex;
		this.finalindex = finalindex;
		
		
	}
	

	public int getAttackNumber() {
		return attackNumber;
	}



	public void setAttackNumber(int attackNumber) {
		this.attackNumber = attackNumber;
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
