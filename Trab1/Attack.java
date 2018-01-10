package br.inf.ufes.pp2017_01.imp;

import java.util.Calendar;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Random;
import java.util.UUID;

import br.inf.ufes.pp2017_01.Guess;
import br.inf.ufes.pp2017_01.Master;
import br.inf.ufes.pp2017_01.Slave;

public class Attack {
	private HashMap<UUID,SubAttack> subAttacks;
	private Master masterThread;
	private int attackNumber;
	private HashMap<UUID, Slave> slaves;
	private Guess[] attackGuess;
	private int contGuess;
	
	private TimeCheck tC;
	
	public Attack( Master m, int attackNumber, HashMap<UUID, Slave> slaves)
	{
		subAttacks = new HashMap<>();
		this.masterThread = m;
		this.attackNumber = attackNumber;
		this.slaves = slaves;
		attackGuess  = new Guess[0];
		contGuess = 0;	
		
	}
	
	
	public int getAttackNumber() {
		return attackNumber;
	}


	public void setAttackNumber(int attackNumber) {
		this.attackNumber = attackNumber;
	}


	public int getContGuess() {
		return contGuess;
	}

	public void setContGuess(int contGuess) {
		this.contGuess = contGuess;
	}

	public Guess[] getAttackGuess() {
		return attackGuess;
	}
	
	public void addNewGuess(Guess guess)
	{
		Guess[] newGuessArray = new Guess[contGuess + 1];
		
		for(int i = 0;i<contGuess;i++)
			newGuessArray[i] = attackGuess[i];
		newGuessArray[contGuess] = guess;
		attackGuess = newGuessArray;
		contGuess++;		
	}
	public void setAttackGuess(Guess[] attackGuess) {	
		
		this.attackGuess = attackGuess;
	}

	public void startTimeCheck()
	{
		tC = new TimeCheck(masterThread, slaves, subAttacks,attackNumber);
		tC.start();
	}
	
	public void notifyFinished()
	{
		tC.notifyFinished();
	}
	
	public void addSubAttack(UUID slavekey, SubAttack sA)
	{
		subAttacks.put(slavekey, sA);
	}

	public HashMap<UUID, SubAttack> getSubAttacks() {
		return subAttacks;
	}


	public void setSubAttacks(HashMap<UUID, SubAttack> subAttacks) {
		this.subAttacks = subAttacks;
	}


	public SubAttack getSubAttack(UUID slavekey)
	{
		return subAttacks.get(slavekey);
	}
		

	
}

class TimeCheck extends Thread
{
	private HashMap<UUID, Slave> slaves;
	private HashMap<UUID,SubAttack> subAttacks;
	private HashSet<UUID> deletedSlaves; // Lista auxiliar para remover os escravos inativos
	private Master callBackMaster;
	private int attackNumber;
	private Integer finished;
	 //conta quantos subattacks acabaram
	
	public TimeCheck (Master m,HashMap<UUID, Slave> slaves, HashMap<UUID,SubAttack> subAttacks, int attackNumber)
	{
		this.subAttacks = subAttacks;
		this.deletedSlaves = new HashSet<>();
		this.slaves = slaves;		
		this.callBackMaster = m;
		finished = 0;
		this.attackNumber = attackNumber;
	}
	
	public void notifyFinished()
	{
		synchronized (finished) {
			finished++;
		}
		
	}
	public void run()
	{
		while(finished != subAttacks.size())
		{			
			try
			{		
						
				synchronized (subAttacks) 
				{
					for (Map.Entry<UUID, SubAttack> at : subAttacks.entrySet()) //Consultar a HashMap de ataques e perguntar se tem algum escravo que nao fez
					{														 //checkpoint a mais de 20s		
						if(Calendar.getInstance().getTime().getTime() - at.getValue().getTime() > 20*1e+3)	
						{
							deletedSlaves.add(at.getKey());		
							System.out.println("OSAOLSAL");
						}
						
					}
					
					for (UUID slavekey : deletedSlaves) 
					{							
						slaves.remove(slavekey);
						SubAttack sA  = subAttacks.get(slavekey);
						
						Random generator = new Random();
						Object[] values = slaves.values().toArray();
						Slave randomSlave = (Slave)  values[generator.nextInt(values.length)]; //Escolher um slave aleatoriamente
						
						
						randomSlave.startSubAttack(sA.getCiphertext(), sA.getKnowntext(), sA.getInitialindex(), //Redirecionar o trabalho dele						
								sA.getFinalindex() ,attackNumber, callBackMaster);
						
						
						subAttacks.remove(slavekey);
					}
					deletedSlaves.clear();
				}
				sleep(20000); 
			}catch (Exception e) {
				// TODO: handle exception
			}
		}
		
		subAttacks.clear();
		finished = 0;
		synchronized (callBackMaster) {
			callBackMaster.notify();
		}
		
	}
}
