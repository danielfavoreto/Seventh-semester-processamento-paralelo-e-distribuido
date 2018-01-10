package br.inf.ufes.pp2017_01.imp;

import java.io.File;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.sql.Time;
import java.util.Calendar;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Scanner;
import java.util.UUID;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;

import br.inf.ufes.pp2017_01.*;

public class MasterImp implements Master {
	
	private String name;
	private HashMap<UUID,Slave> slaves;
	private HashMap<Integer, String> dictionary;	
	
	private Integer attackNumber;
	private HashMap<Integer,Attack> attacks; //Integer eh attack Number, guarda todos os ataques que estao acontecendo, a classe Attack auxilia na remocao 
											 //de um escravo se ele demorar mais de 20s para dar um checkpoint
	
	public MasterImp(String name) 
	{
		this.name = name;
		slaves = new HashMap<>();
		dictionary = new HashMap<>();
		
		attacks = new HashMap<>();		
		attackNumber = 0;
		//Leitura do Dicion�rio
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
		//Fim Leitura Dicion�rio
		
	}
	@Override
	public void addSlave(Slave s, String slaveName, UUID slavekey) throws RemoteException {
		synchronized (slaves) {
			slaves.put(slavekey, s);
		}
		
		//System.out.println("Slave : "+ slavekey + " registred.");
	}

	@Override
	public void removeSlave(UUID slaveKey) throws RemoteException {
		synchronized (slaves) {
			slaves.remove(slaveKey);
		}
		
		
	}

	@Override
	public void foundGuess(UUID slaveKey, int attackNumber, long currentindex, Guess currentguess)
			throws RemoteException {
		
		System.out.println("\nWord \"" + currentguess.getKey() + "\" found!\n" + "Slave Name: " +  slaves.get(slaveKey).toString()
				+ " Slave Key = "+ slaveKey 
				+ "\nAttack Number = " + attackNumber + "\n" );
		
			attacks.get(attackNumber).addNewGuess(currentguess);		
		
	}

	@Override
	public void checkpoint(UUID slaveKey, int attackNumber, long currentindex) throws RemoteException {
		System.out.println("Slave Name: " + slaves.get(slaveKey).toString() + " currentindex: "+currentindex + " his finalindex is "
				+ attacks.get(attackNumber).getSubAttack(slaveKey).getFinalindex());
		
		attacks.get(attackNumber).getSubAttack(slaveKey).setInitialindex(currentindex); // Usado para saber quando um escravo termina seu subAttack
		attacks.get(attackNumber).getSubAttack(slaveKey).setTime(Calendar.getInstance().getTime().getTime());
		
		if(attacks.get(attackNumber).getSubAttack(slaveKey).getFinalindex() == attacks.get(attackNumber).getSubAttack(slaveKey).getInitialindex())		
			attacks.get(attackNumber).notifyFinished();
			
		
	}

	@Override
	public Guess[] attack(byte[] ciphertext, byte[] knowntext) throws RemoteException {
				
		HashMap<UUID,Slave> copySlaves;
		int nSlaves = slaves.size();
		int sizeDicti = dictionary.size();
		int wordsEach = (sizeDicti - (sizeDicti%nSlaves))/nSlaves;
		int slavesCont = 0;
		long initialI, finalI;
		initialI = 1;
		finalI = wordsEach;		
		
		Attack at;
		synchronized (attackNumber) {
			attackNumber++;
			at = new Attack(this, attackNumber,slaves); //Criar classe de controle do ataque
			attacks.put(attackNumber, at);
		}
		synchronized (slaves) {
			copySlaves = new HashMap<>(slaves);		// Copiar lista de escravos para nao bloquear a lista original	
		}
		
		
		for (Map.Entry<UUID,Slave> s : copySlaves.entrySet() ) 
		{
			
			slavesCont++;			
			if(slavesCont == nSlaves)
			{
				
				SubAttack sA = new SubAttack(ciphertext, knowntext,s.getKey() , Calendar.getInstance().getTime().getTime(), initialI, sizeDicti);
				at.addSubAttack(s.getKey(), sA);
				s.getValue().startSubAttack(ciphertext, knowntext, initialI, sizeDicti, attackNumber, this);	
			}
			else
			{				
				SubAttack sA = new SubAttack(ciphertext, knowntext,s.getKey() , Calendar.getInstance().getTime().getTime(), initialI, finalI);
				at.addSubAttack(s.getKey(), sA);
				s.getValue().startSubAttack(ciphertext, knowntext, initialI, finalI, attackNumber, this);				
				initialI= finalI + 1;
				finalI = wordsEach + finalI;
				
			}
			
		}
		at.startTimeCheck();
		
		try {
			synchronized (this) {
				this.wait();
			}
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		};
		return attacks.get(at.getAttackNumber()).getAttackGuess();
	}
	
	
	
	
	public static void main(String[] args) 
	{
		try
		{						
			
			Master obj = new MasterImp("master");
			
			Master objref = (Master) UnicastRemoteObject.exportObject( obj, 2000);
			
			Registry registry = LocateRegistry.getRegistry("localhost"); 
			registry.rebind("mestre", objref);
		
			
		}
		catch(Exception e)
		{
			System.err.println("Server exception: " + e.toString());
			e.printStackTrace();
		}
		
		
	}
    

}

