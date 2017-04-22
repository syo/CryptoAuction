package cryptoII;

import java.io.IOException;
import java.math.BigInteger;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.Scanner;

import de.henku.jpaillier.PaillierKeyPair;

public class Client {
	private static String serverIP="localhost";
	private static int port=9091;
	private static Socket s;
	private static PaillierKeyPair keys;
	private static void bid() {
		//TODO
	}
	private static void getResult() throws IOException {
		BigInteger result=cryptoMessaging.recvBigInteger(s.getInputStream());
		result=keys.decrypt(result);
		if (result.compareTo(BigInteger.ZERO)==0) {
			System.out.println("We lost the auction.");
		}
		else {
			System.out.println("We won the auction. The price of the item is "+result);
		}
	}
	public static void main(String[] args) throws UnknownHostException, IOException {
		Scanner scanner=new Scanner(System.in);
		if (serverIP==null) {
			System.out.println("Enter server IP. Do not enter port number yet.");
			serverIP=scanner.nextLine();
		}
		if (port==0) {
			System.out.println("Enter port number:");
			port=Integer.parseInt(scanner.nextLine());
		}
		s=new Socket(serverIP,port);
		bid();
		getResult();
	}
}
