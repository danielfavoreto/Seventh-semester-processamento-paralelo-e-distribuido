package br.inf.ufes.pp2017_01.imp;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.Serializable;
import java.lang.reflect.Array;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.Scanner;
import java.util.UUID;

import javax.crypto.Cipher;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.SecretKeySpec;

import br.inf.ufes.pp2017_01.*;

public class SlaveImp implements Slave, Serializable {
	 
	public String slaveName;
	private HashMap<Integer, String> dictionary;
	private UUID slavekey;	
	
	
	public SlaveImp(String name, UUID slaveKey )
	{
		dictionary = new HashMap<>();		
		this.slaveName = name;
		this.slavekey = slaveKey;
		
		//Leitura do Dicionario
		Integer index = 1;
		try{
			Scanner scanner = new Scanner(new File("dictionary.txt"));
			
			while(scanner.hasNextLine())
			{
				dictionary.put( index, scanner.nextLine());
				index++;
			}		
			
			scanner.close();
			
			
		}catch (Exception e) {
			System.out.println(e.getMessage());
		}
		//Fim Leitura Dicionario
	
	}	
	

	public String toString() {
		return slaveName;
	}


	public void setSlaveName(String slaveName) {
		this.slaveName = slaveName;
	}


	public int getDicSize()
	{
		return dictionary.size();
	}

	public void startSubAttack(
			byte[] ciphertext,
			byte[] knowntext,
			long initialwordindex,
			long finalwordindex,
			int attackNumber,
			SlaveManager callbackinterface)
	{
		
	
		(new SubAttackThread(ciphertext, knowntext, 
				initialwordindex, finalwordindex, 
				attackNumber, callbackinterface, dictionary,this.slavekey,slaveName)).start();
		
		
		
		
	}
	/**
	 * 
	 * @param args[0] nome descritivo do escravo
	 */
	public static void main(String[] args) 
	{
		try
		{			
			Registry registry = LocateRegistry.getRegistry();
			
			Master m = (Master) registry.lookup("mestre");	
			String slaveName = new String(args[0]);
			UUID slaveKey = java.util.UUID.randomUUID();
			Slave s = new SlaveImp(slaveName,slaveKey);
			m.addSlave(s, slaveName, slaveKey);
			
			(new Register(s,m,slaveName,slaveKey)).start(); //Thread de re-registro
			
		
			
			
			
		}
		catch(Exception e)
		{
			System.err.println("Client exception: " + e.toString());
			e.printStackTrace();
		}
		
		
	}

}
class CheckPoint extends Thread // Thread que chama o metodo checkpoint, eh sempre iniciada na Thread SubAttack, quando o SubAttack acaba
{ 								//esta thread eh iterrompida
	private UUID slaveKey;
	private int attackNumber;
	private long currentindex;
	private SlaveManager m;
	private boolean running;
	public CheckPoint(UUID slaveKey, int attackNumber, long currentindex, SlaveManager m)
	{
		this.attackNumber = attackNumber;
		this.slaveKey = slaveKey;
		this.currentindex = currentindex;
		this.m = m;
		running = true;
	}
	public void setCurrentIndex(long currentindex)
	{
		this.currentindex = currentindex;
	}
	
	public void stopThread()
	{
		running = false;
	}
	
	public void run()
	{		
		while(running)
		{
			try
			{
				
				m.checkpoint(slaveKey, attackNumber, currentindex);
				sleep(10000);
			}
			catch(Exception e) // Thread iterrompida
			{
				//e.printStackTrace();
				
			}
			
			
		}
	}
}

class SubAttackThread extends Thread
{
	private byte[] ciphertext, knowntext;
	private long initialwordindex, finalwordindex;
	private int attackNumber;
	private SlaveManager callbackinterface;
	private HashMap<Integer,String> dictionary;
	private UUID slavekey;
	private String slaveName;
	
	public SubAttackThread(byte[] ciphertext,
			byte[] knowntext,
			long initialwordindex,
			long finalwordindex,
			int attackNumber,
			SlaveManager callbackinterface, HashMap<Integer,String> dictionary, UUID slavekey, String slaveName)
	{
		this.ciphertext = ciphertext;
		this.knowntext = knowntext;
		this.initialwordindex = initialwordindex;
		this.finalwordindex = finalwordindex;
		this.attackNumber = attackNumber;
		this.callbackinterface = callbackinterface;
		this.dictionary = dictionary;
		this.slavekey = slavekey;
		this.slaveName = slaveName;
		
	}
	
	public void run()
	{
		
		CheckPoint checkPointThread = new CheckPoint(slavekey, attackNumber, initialwordindex,callbackinterface);
		checkPointThread.start();
		for(long i = initialwordindex;i<finalwordindex;i++)
		{				
			checkPointThread.setCurrentIndex(i);
			String dicWord = new String(dictionary.get( (int) i)) ;			
			byte[] word = dicWord.getBytes();
			SecretKeySpec	keySpec	=	new	SecretKeySpec(word,	"Blowfish");	
			
			
 	 	 	
	 	 	Cipher cipher;
			try 
			{
				cipher = Cipher.getInstance("Blowfish");
				cipher.init(Cipher.DECRYPT_MODE,	keySpec); // Decriptografar o ciphertext com uma palavra do dicionario
				byte[] encrypted = cipher.doFinal(ciphertext);
				
				String plainText = new String(encrypted);				
				
				if(plainText.contains(new String(knowntext)))				
				{
					Guess guessWord = new Guess();
					guessWord.setKey(dicWord);
					guessWord.setMessage(encrypted);					
					callbackinterface.foundGuess(slavekey, attackNumber, i, guessWord);
				}			
			} catch (Exception  e) { 
				//e.printStackTrace();
			}
		}
		
		try {			
			callbackinterface.checkpoint(slavekey, attackNumber, finalwordindex);
			checkPointThread.stopThread();
			System.out.println("Slave: \""+slaveName+"\" finished");			
			callbackinterface.notify();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			//e.printStackTrace();
		}
		
	}
}

class Register extends Thread
{
		
	private String slaveName;
	private UUID slavekey;	
	private Master m;
	private Slave s;
	public Register(Slave s,Master m, String slaveName, UUID slavekey)
	{		
		this.m = m;
		this.slaveName = slaveName;
		this.slavekey = slavekey;
		this.s = s;
		
		
	}
	
	public void run()
	{		
		while(true)
		{
			try
			{				
				sleep(30000);
				Registry registry = LocateRegistry.getRegistry();
				
				m = (Master) registry.lookup("mestre");
				m.addSlave(s, slaveName, slavekey);
				//System.out.println("RE-REGISTER");
				
				
			}				
			catch (Exception e) {
				System.out.println("Problemas no Mestre!");
				//e.printStackTrace();
				
				
			}
		}
	}
}
