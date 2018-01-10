package br.inf.ufes.pp2017_01.client;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Random;
import java.util.Scanner;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;

import br.inf.ufes.pp2017_01.Encrypt;
import br.inf.ufes.pp2017_01.Guess;
import br.inf.ufes.pp2017_01.Master;

public class Client {

	public static void main(String[] args) {
		
		try {
			
			ArrayList<String> dicionario = carregaDicionario("dictionary.txt");
			
			String nomeArquivoMensagemCriptografada = args[0];
					
			String palavraConhecida = args[1];
			
			String nomeMestre = "mestre";
			
			// Instncia da classe Encrypt
			Encrypt criptografado = new Encrypt();
			
			// Carrega mensagem conhecida em vetor
			byte[] mensagemConhecida = palavraConhecida.getBytes();
						
			byte[] mensagemCriptografada = null;
							
			Registry registry = LocateRegistry.getRegistry();
				
			Master mestre = (Master) registry.lookup(nomeMestre);
			
			try {
				
				// Carrega a mensagem criptografada em vetor
				mensagemCriptografada = criptografado.readFile(nomeArquivoMensagemCriptografada);
				
			} catch (Exception e){
				
				// Provavelmente nao achou o arquivo com a mensagem criptografada
				atacaExibeChutes(args, dicionario, mestre);
				
				return;
			}
			
			
			long tempoInicial = System.nanoTime();
			
			// realiza o ataque utilizando a mensagem criptografada e a mensagem conhecida
			Guess[] chutes = mestre.attack(mensagemCriptografada,mensagemConhecida);
			
			long tempoFinal = System.nanoTime();
			
			// calcula o tempo total relizado do ataque
			long tempoTotal = tempoFinal - tempoInicial;
			
			System.out.println(chutes.length + " chutes encontrados\n Tempo: " + tempoTotal*1e-9 + " s\n Chaves candidatas encontradas:");
			
			for (int i = 0; i < chutes.length; i++) {
				
				System.out.println(chutes[i].getKey());
				
				// salva o arquivo com as chaves candidatas e as respectivas mensagens candidatas
				FileOutputStream out = new FileOutputStream(chutes[i].getKey() + ".msg");
			    out.write(chutes[i].getMessage());
			    out.close();				
				
			}
			
		}
		catch (Exception e){
			
			e.printStackTrace();
			
		}
		
	}
	
	/**
	 * @param pathDicionario passa o caminho do arquivo de dicionrio
	 * @return o dicionrio populado como uma lista
	 * @throws Exception
	 */
	public static ArrayList<String> carregaDicionario (String pathDicionario) throws Exception {
		
		//try {
		
			// Dicionrio de chaves
			ArrayList<String> dicionario = new ArrayList<String>();
			
			// Arquivo com dicionario de chaves
			File arquivoDicionario = new File (pathDicionario);
			
			Scanner scanner = new Scanner (arquivoDicionario);
			
			// Carrega cada chave do arquivo de dicionario na lista do dicionario
			while (scanner.hasNext()){
				
				dicionario.add(scanner.nextLine());
				
			}
			
			scanner.close();
			
			return dicionario;
		//}
		
		/*catch (Exception e){
			
			throw new Exception("Erro ao carregar arquivo de dicionrio: " + e.getMessage());
		}*/
		
	}

	/** Faz o ataque e exibe os chutes das chaves encontradas com uma mensagem criptografada aleatria, 
	 * podendo ter um tamanho qualquer ou especificado via linha de argumento
	 * @param args argumentos da linha de comando main
	 * @param dicionario dicionario de chaves
	 * @param mestre referncia do mestre
	 * @throws IOException
	 */

	private static void atacaExibeChutes (String [] args, ArrayList <String> dicionario, Master mestre) throws Exception{
		
		//try {
			
			int tamanhoVetorBytes;
			
			Random aleatorio = new Random();
			
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
			
			Guess[] chutes = mestre.attack(mensagemCriptografada,mensagemConhecida);
			
			long tempoFinal = System.nanoTime();
			
			long tempoTotal = tempoFinal - tempoInicial;
			
			System.out.println(chutes.length + " chutes encontrados\n Tempo: " + tempoTotal*1e-9 + " s" + "\n Chaves candidatas encontradas:");

			for (int i = 0; i < chutes.length; i++) {				
				
				out = new FileOutputStream(chutes[i].getKey() + ".msg");
			    out.write(chutes[i].getMessage());
			    out.close();
				
			}
			
		/*} catch (Exception e1) {

			e1.printStackTrace();
		}*/
	}
}
