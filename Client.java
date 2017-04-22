package cryptoII;

import java.io.IOException;
import java.math.BigInteger;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.Scanner;

import de.henku.jpaillier.KeyPairBuilder;
import de.henku.jpaillier.PaillierKeyPair;

public abstract class Client {
	private static String serverIP="localhost";
	private static int port=9091;
	private static Socket s;
	private static PaillierKeyPair keys;
	private static void getBiddingResult() throws IOException {
		BigInteger result=cryptoMessaging.recvBigInteger(s.getInputStream());
		result=keys.decrypt(result);
		if (result.compareTo(BigInteger.ZERO)==0) {
			System.out.println("We lost the auction.");
		}
		else {
			System.out.println("We won the auction. The price of the item is "+result);
		}
	}
	private static void getAuctioneerResult() throws IOException {
		BigInteger winner=cryptoMessaging.recvBigInteger(s.getInputStream());
		BigInteger price=cryptoMessaging.recvBigInteger(s.getInputStream());
		price=keys.decrypt(price);
		winner=keys.decrypt(winner);
		if (price.compareTo(BigInteger.ZERO)==0) {
			System.out.println("It seems nobody wished to bid.");
		}
		else {
			System.out.println(winner+" won the auction. The price of the item is "+price);
		}
	}
	private static void runBidderClient() throws IOException {
		//TODO: Authenticate and send bid
		getBiddingResult();
	}
	private static void runAuctioneerClient() throws IOException {
		//TODO: Authenticate and send starting price
		getAuctioneerResult();
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
		boolean bidder=false;
		System.out.println("Is this the auctioneer? Enter yes or no.");
		if (scanner.nextLine().charAt(0)!='y') bidder=false;
		port=Integer.parseInt(scanner.nextLine());
		PaillierKeyPair pk=new KeyPairBuilder().generateKeyPair();
		s=new Socket(serverIP,port);
		if (bidder) runBidderClient();
		else runAuctioneerClient();
	}
}
