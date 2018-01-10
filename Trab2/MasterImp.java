
import java.io.File;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.HashMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.Scanner;
import java.util.UUID;

import javax.jms.*;

import com.sun.messaging.ConnectionConfiguration;

import br.inf.ufes.pp2017_01.*;


public class MasterImp implements Master {

	private String name;
	private HashMap<UUID,Slave> slaves;
	private HashMap<UUID, String> nameSlaves;
	private HashMap<Integer, String> dictionary;	
	private Integer attackNumber;
	private int m;
	private JMSProducer producerSubAttack;
	private JMSContext contextSubAttack, contextGuess;
	private Queue subAttackQueue, guessQueue;
	private JMSConsumer consumerGuess;
	private int contGuess;
	private HashMap<Integer, Guess[]> guesses;
	private String ip;

	public MasterImp(String name, String ip, int m)
	{
		this.name = name;
		slaves = new HashMap<>();
		dictionary = new HashMap<>();
		this.nameSlaves = new HashMap<>();
		this.m = m;
		guesses = new HashMap<>();
		this.ip = ip;
		attackNumber = 0;
		//Leitura do Dicionï¿½rio
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
		//Fim Leitura Dicionï¿½rio
		createSubAttacksQueue();
		createGuessesQueue();
		

	}
	public int getM() {
		return m;
	}
	public void setM(int m) {
		this.m = m;
	}
	@Override
	public void addSlave(Slave s, String slaveName, UUID slavekey) throws RemoteException {
		synchronized (slaves) {
			slaves.put(slavekey, s);
			nameSlaves.put(slavekey,slaveName);
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

		System.out.println("\nWord \"" + currentguess.getKey() + "\" found!\n" + "Slave Name: " +  nameSlaves.get(slaveKey)
				+ " Slave Key = "+ slaveKey
				+ "\nAttack Number = " + attackNumber + "\n" );

			

	}

	@Override
	public void checkpoint(UUID slaveKey, int attackNumber, long currentindex) throws RemoteException {
		

	}
	public void addNewGuess(Guess guess, int attackNum)
	{
		Guess[] newGuessArray;
		if(guesses.containsKey(attackNum))
			newGuessArray = new Guess[guesses.get(attackNum).length + 1];
		else
		{
			newGuessArray = new Guess[1];
			newGuessArray[0] = guess;
			guesses.put(attackNum, newGuessArray);
			return;
		}

		for(int i = 0;i<guesses.get(attackNum).length;i++)
			newGuessArray[i] = guesses.get(attackNum)[i];
		newGuessArray[guesses.get(attackNum).length] = guess;
		guesses.remove(attackNum);
		guesses.put(attackNum,newGuessArray);
		
	}

	@Override
	public Guess[] attack(byte[] ciphertext, byte[] knowntext) throws RemoteException {	
		
		int sizeDicti = dictionary.size();
		int wordsEach = (sizeDicti - (sizeDicti%m))/m;		
		long initialI, finalI;
		initialI = 1;
		finalI = wordsEach;
		int contFinished = 0;		
		int attackNum;
		
		synchronized (attackNumber) {
			attackNumber++;
			attackNum = attackNumber;
			
		}
		SubAttack sA;
		ObjectMessage message = null;
		
		try {
			for(int i = 0;i<m;i++)  //Coloca os sub attacks na fila
			{
				if(i == (m-1))
				{
					sA = new SubAttack(ciphertext, knowntext, attackNum, initialI, finalI);
					message = contextSubAttack.createObjectMessage();
					message.setObject(sA);
					producerSubAttack.send(subAttackQueue, sA);
					System.out.println("Attack: "+ attackNum +" initialindex: " + initialI + " finalindex: "+ sizeDicti);
				}
				else
				{
					sA = new SubAttack(ciphertext, knowntext, attackNum, initialI, finalI);
					message = contextSubAttack.createObjectMessage();
					message.setObject(sA);
					producerSubAttack.send(subAttackQueue, sA);
					System.out.println("Attack: "+ attackNum +" initialindex: " + initialI + " finalindex: "+ finalI);
					initialI= finalI + 1;
					finalI = wordsEach + finalI;
					
				}	
				
			}			
			
			
		
			while(true)
			{
				Message msg = consumerGuess.receive();				
				if(msg instanceof ObjectMessage)
				{
					ObjectMessage obj = (ObjectMessage) msg;
					Guess guessWord = (Guess) obj.getObject();			
					
					
					if(guessWord.getKey().equals("")) // Se message for vazia é pq acabou um sub attack
					{
						contFinished++;
						System.out.println("Slave \"" + guessWord.getSlaveName() + "\" finished.");						
						if(contFinished == m) // Saida do sub attack
							break;
					}
					else
					{
						System.out.println("\nWord \"" + guessWord.getKey() + "\" found!\n" + "Slave Name: " +  guessWord.getSlaveName()						
						+ "\nAttack Number = " + attackNum + "\n" );
						
						addNewGuess(guessWord,attackNum);
					}
					
					
				}
				
			}
		}catch (Exception e) {
			e.printStackTrace();
		}	
	
		return guesses.get(attackNum) ;
		
	}



	public void createSubAttacksQueue()
	{
		
		
		try (Scanner s = new Scanner(System.in)) {
			Logger.getLogger("").setLevel(Level.SEVERE);

			System.out.println("obtaining connection factory...");
			com.sun.messaging.ConnectionFactory connectionFactory = new com.sun.messaging.ConnectionFactory();
			connectionFactory.setProperty(ConnectionConfiguration.imqAddressList,ip+":7676");	
			System.out.println("obtained connection factory.");
			
			System.out.println("obtaining queue...");
			this.subAttackQueue = new com.sun.messaging.Queue("SubAttacksQueue");
			System.out.println("obtained queue.");

			this.contextSubAttack = connectionFactory.createContext();
			this.producerSubAttack = contextSubAttack.createProducer();						
			
			
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	public void createGuessesQueue()
	{
		try {
			Logger.getLogger("").setLevel(Level.SEVERE);
			
			System.out.println("obtaining connection factory...");
			com.sun.messaging.ConnectionFactory connectionFactory = new com.sun.messaging.ConnectionFactory();
			connectionFactory.setProperty(ConnectionConfiguration.imqAddressList,ip+":7676");	
			System.out.println("obtained connection factory.");
			
			System.out.println("obtaining queue...");
			guessQueue = new com.sun.messaging.Queue("GuessesQueue");
			System.out.println("obtained queue.");			
	
			this.contextGuess = connectionFactory.createContext();
			
			this.consumerGuess = contextGuess.createConsumer(guessQueue);

			
		
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	public static void main(String[] args)
	{
		try
		{

			Master obj = new MasterImp("master", args[1],Integer.valueOf(args[0]));
			

			Master objref = (Master) UnicastRemoteObject.exportObject( obj, 0);

			Registry registry = LocateRegistry.getRegistry(args[1]);
			registry.rebind("mestre", objref);
			
			
			


		}
		catch(Exception e)
		{
			System.err.println("Server exception: " + e.toString());
			e.printStackTrace();
		}


	}


}
