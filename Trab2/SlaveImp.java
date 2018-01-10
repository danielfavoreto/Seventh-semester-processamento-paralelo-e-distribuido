

import java.io.File;
import java.io.Serializable;
import java.util.HashMap;
import java.util.Scanner;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import javax.jms.JMSConsumer;
import javax.jms.JMSContext;
import javax.jms.JMSException;
import javax.jms.JMSProducer;
import javax.jms.Message;
import javax.jms.MessageListener;
import javax.jms.ObjectMessage;
import javax.jms.Queue;


import com.sun.messaging.ConnectionConfiguration;

import br.inf.ufes.pp2017_01.*;

public class SlaveImp implements Slave, Serializable {

	public String slaveName;
	private HashMap<Integer, String> dictionary;
	private UUID slavekey;
	private JMSProducer producerGuess;
	private JMSContext contextSubAttack, contextGuess;
	private Queue subAttackQueue, guessQueue;
	private JMSConsumer consumerSubAttack;
	

	public SlaveImp(String name, UUID slaveKey, String ip )
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
			connectGuessesQueue(ip);
			connectSubAttacksQueue(ip);		
		
			 
			


		}catch (Exception e) {
			System.out.println(e.getMessage());
		}
		//Fim Leitura Dicionario

	}
	public void connectSubAttacksQueue(String ipAddr)
	{
		
		
		try (Scanner s = new Scanner(System.in)) {
			Logger.getLogger("").setLevel(Level.SEVERE);

			System.out.println("obtaining connection factory...");
			com.sun.messaging.ConnectionFactory connectionFactory = new com.sun.messaging.ConnectionFactory();
			connectionFactory.setProperty(ConnectionConfiguration.imqAddressList,ipAddr+":7676");	
			System.out.println("obtained connection factory.");
			
			System.out.println("obtaining queue...");
			this.subAttackQueue = new com.sun.messaging.Queue("SubAttacksQueue");
			System.out.println("obtained queue.");

			this.contextSubAttack = connectionFactory.createContext();					
			this.consumerSubAttack = contextSubAttack.createConsumer(subAttackQueue);
			this.consumerSubAttack.setMessageListener(new MessageListener() {
			    public void onMessage(Message msg) {		
			    	SubAttack sA;
					if( msg instanceof ObjectMessage)
					{
						ObjectMessage obj = (ObjectMessage) msg;
						try {
							sA = (SubAttack) obj.getObject();
							startSubAttack(sA.getCiphertext(), sA.getKnowntext(), sA.getInitialindex(),
									sA.getFinalindex(), sA.getAttackNumber(), null);
						} catch (JMSException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
						
										
					}
			    }
			});
			
			
			
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	public void connectGuessesQueue(String ip)
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
			this.producerGuess = contextGuess.createProducer();	
			

			
		
		} catch (Exception e) {
			e.printStackTrace();
		}
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
		
		
		System.out.println("Starting Sub Attack with initialindex = !"+ initialwordindex + " and finalindex = " + finalwordindex);
		Guess guessWord;
		for(long i = initialwordindex;i<finalwordindex;i++)
		{
			
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
					guessWord = new Guess();
					guessWord.setKey(dicWord);
					guessWord.setMessage(encrypted);
					guessWord.setSlaveName(slaveName);					
					ObjectMessage message = contextGuess.createObjectMessage(); 
					message.setObject(guessWord);
					producerGuess.send(guessQueue,message );
					
				}
			} catch (Exception  e) {
				//e.printStackTrace();
			}
		}
		
		guessWord = new Guess(); // Criar um Guess vazio que ira sinalizar o fim do sub attack
		guessWord.setKey("");		
		guessWord.setSlaveName(slaveName);
		ObjectMessage message = contextGuess.createObjectMessage(); 
		try {
			message.setObject(guessWord);
		} catch (JMSException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		producerGuess.send(guessQueue,message );
		
				
		System.out.println("Slave: \""+slaveName+"\" finished");
	}
	/**
	 *
	 * @param args[0] nome descritivo do escravo
	 */
	public static void main(String[] args)
	{
		try
		{
			
			String slaveName = new String(args[0]);
			UUID slaveKey = java.util.UUID.randomUUID();
			Slave s = new SlaveImp(slaveName,slaveKey, args[1]);
			





		}
		catch(Exception e)
		{
			System.err.println("Client exception: " + e.toString());
			e.printStackTrace();
		}


	}

}


