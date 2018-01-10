package br.inf.ufes.pp2017_01.client;

import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Random;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;

import br.inf.ufes.pp2017_01.Encrypt;
import br.inf.ufes.pp2017_01.Guess;

public class ClientSequencial {

	public static void main(String[] args) {
		
		try {
		
			ArrayList<String> dicionario = Client.carregaDicionario("dictionary.txt");
			
			ArrayList<Guess> guesses = new ArrayList<Guess>();
			
			String nomeArquivoMensagemCriptografada = args[0];
					
			String palavraConhecida = args[1];
			
			// Instncia da classe Encrypt
			Encrypt criptografado = new Encrypt();
			
			// Carrega mensagem conhecida em vetor
			byte[] mensagemConhecida = palavraConhecida.getBytes();
						
			byte[] mensagemCriptografada = null;
			
			try{
			
				// Carrega a mensagem criptografada em vetor
				mensagemCriptografada = criptografado.readFile(nomeArquivoMensagemCriptografada);
			}
			catch (Exception e ){
				
				// Provavelmente nao achou o arquivo com a mensagem criptografada
				atacaExibeChutes(args,dicionario);
				
			}
			
			long tempoInicial = System.nanoTime();
			
			for (long i = 0; i < dicionario.size(); i++)
			{	
				
				String dicWord = new String(dicionario.get( (int) i)) ;	
				
				byte[] word = dicWord.getBytes();
				
				SecretKeySpec	keySpec	=	new	SecretKeySpec(word,	"Blowfish");	
				
		 	 	Cipher cipher;
		 	 	
				try 
				{
					
					cipher = Cipher.getInstance("Blowfish");
					
					cipher.init(Cipher.DECRYPT_MODE,	keySpec); // Decriptografar o ciphertext com uma palavra do dicionario
					
					byte[] encrypted = cipher.doFinal(mensagemCriptografada);
					
					String plainText = new String(encrypted);				
					
					if (plainText.contains(new String(mensagemConhecida)))				
					{
						
						Guess guessWord = new Guess();
						guessWord.setKey(dicWord);
						guessWord.setMessage(encrypted);	
						guesses.add(guessWord);
					}			
				} catch (Exception  e) { 

				}
			}
			
			System.out.println(guesses.size() + " chutes encontrados\n Chaves candidatas encontradas:");
			
			Guess[] g = new Guess[guesses.size()];
			guesses.toArray(g);
			
			for (int i = 0; i < guesses.size(); i++) {
				
				System.out.println(g[i].getKey());
				
				try {
				
					// salva o arquivo com as chaves candidatas e as respectivas mensagens candidatas
					FileOutputStream out = new FileOutputStream(g[i].getKey() + ".msg");
				    out.write(g[i].getMessage());
				    out.close();				
				}
				catch (Exception e){
					e.printStackTrace();
				}
			}
			
			long tempoFinal = System.nanoTime();
			
			long tempoTotal = tempoFinal - tempoInicial;
			
			System.out.println("Tempo: " + tempoTotal*1e-9 + " s");
			
		}
		catch (Exception e){
			
		}

	}	
	
	private static void atacaExibeChutes(String[] args, ArrayList<String> dicionario) throws Exception{
	
		int tamanhoVetorBytes;
		
		Random aleatorio = new Random();
		
		ArrayList<Guess> guesses = new ArrayList<Guess>();
						
		if (args.length < 3) { // nao passou o tamanho do vetor de bytes
		
			tamanhoVetorBytes = new Random().nextInt(99001) + 1000;
			
		}
		else { // caso tenha passado o tamanho do vetor de bytes
			
			tamanhoVetorBytes = Integer.parseInt(args[2]);
			
		}
		
		byte[] mensagemCriptografada = new byte[tamanhoVetorBytes];
		
		new Random().nextBytes(mensagemCriptografada);
		
		int lower = new Random().nextInt(mensagemCriptografada.length);
		
		byte[] mensagemConhecida = Arrays.copyOfRange(mensagemCriptografada, lower, Math.min(lower + args[1].length() - 1, mensagemCriptografada.length - 1));

		byte[] chave = dicionario.get(aleatorio.nextInt(dicionario.size())).getBytes();
		
		SecretKeySpec keySpec = new SecretKeySpec(chave, "Blowfish");
		
		Cipher cipher = Cipher.getInstance("Blowfish");
		
		cipher.init(Cipher.ENCRYPT_MODE, keySpec);
		
		mensagemCriptografada = cipher.doFinal(mensagemConhecida);
		
		FileOutputStream out;
		out = new FileOutputStream(args[0] + ".cipher");
	    out.write(mensagemCriptografada);
	    out.close();
		
		long tempoInicial = System.nanoTime();
		
		for (long i = 0; i < dicionario.size(); i++)
		{	
			
			String dicWord = new String(dicionario.get( (int) i)) ;	
			
			byte[] word = dicWord.getBytes();
			
			SecretKeySpec	keySpecDecrip	=	new	SecretKeySpec(word,	"Blowfish");	
			
	 	 	Cipher cipherDecrip;
	 	 	
			try 
			{
				
				cipherDecrip = Cipher.getInstance("Blowfish");
				
				cipherDecrip.init(Cipher.DECRYPT_MODE,	keySpecDecrip); // Decriptografar o ciphertext com uma palavra do dicionario
				
				byte[] encrypted = cipherDecrip.doFinal(mensagemCriptografada);
				
				String plainText = new String(encrypted);				
				
				if (plainText.contains(new String(mensagemConhecida)))				
				{
					
					Guess guessWord = new Guess();
					guessWord.setKey(dicWord);
					guessWord.setMessage(encrypted);	
					guesses.add(guessWord);
				}			
			} catch (Exception  e) { 

			}
		}
		
		System.out.println(guesses.size() + " chutes encontrados\n Chaves candidatas encontradas:");
		
		Guess[] g = new Guess[guesses.size()];
		guesses.toArray(g);
		
		for (int i = 0; i < guesses.size(); i++) {
			
			System.out.println(g[i].getKey());
			
			try {
			
				// salva o arquivo com as chaves candidatas e as respectivas mensagens candidatas
				FileOutputStream outFile = new FileOutputStream(g[i].getKey() + ".msg");
			    outFile.write(g[i].getMessage());
			    outFile.close();				
			}
			catch (Exception e){
				e.printStackTrace();
			}
		}
		
		long tempoFinal = System.nanoTime();
		
		long tempoTotal = tempoFinal - tempoInicial;
		
		System.out.println("Tempo: " + tempoTotal*1e-9 + " s");
	    
	}
}