package cryptoII;

import java.io.IOException;
import java.math.BigInteger;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.Scanner;

import de.henku.jpaillier.KeyPairBuilder;
import de.henku.jpaillier.PaillierKeyPair;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.security.Signature;

public abstract class Client {
	private static String serverIP="localhost";
	private static int port=9091;
	private static Socket s;
	private static PaillierKeyPair keys;
	private static KeyPairGenerator keyGen;
	private static SecureRandom random;
	private static KeyPair pair;

	private static byte[] bigIntToByteArray(BigInteger bigInteger) {
		byte[] array = bigInteger.toByteArray();
		if (array[0] == 0) {
			byte[] tmp = new byte[array.length - 1];
			System.arraycopy(array, 1, tmp, 0, tmp.length);
			array = tmp;
		}
		return array;
	}

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

	private static byte[] sign(BigInteger number, PrivateKey prvKey) throws Exception {
    	Signature sig = Signature.getInstance("SHAwithDSA");
    	sig.initSign(prvKey);
    	byte[] data = bigIntToByteArray(number);
		sig.update(data);

    	return sig.sign();
  	}

	private static boolean verify(BigInteger number, PublicKey pubKey, byte[] sigbytes) throws Exception {
		Signature sig = Signature.getInstance("SHAwithDSA");
		sig.initVerify(pubKey);
		byte[] data = bigIntToByteArray(number);
		sig.update(data);
		return sig.verify(sigbytes);
  	}

	private static void runBidderClient() throws Exception {
		// set up digital signature keys
		PublicKey serverSig;
		keyGen.initialize(1024, random);
		pair = keyGen.generateKeyPair();
		// send sig for client
		cryptoMessaging.sendPublicKey(s.getOutputStream(), pair.getPublic());
		// receive sig key for server
		serverSig = cryptoMessaging.recvPublicKey(s.getInputStream());
		//receive starting price and verify it
		BigInteger starting = cryptoMessaging.recvBigInteger(s.getInputStream());
		byte[] signature = cryptoMessaging.recvByteArr(s.getInputStream());
		if (!(verify(starting, serverSig, signature))) {
			System.out.println("Auctioneer may not be who he says he is");
			return;
		}
		
		// send bid, authenticated
		Scanner keyboard = new Scanner(System.in);
		System.out.println("enter your bid: ");
		BigInteger bid = BigInteger.valueOf(keyboard.nextInt());
		byte[] sigbytes = sign(bid, pair.getPrivate());
		cryptoMessaging.sendBigInteger(s.getOutputStream(), bid);
		cryptoMessaging.sendByteArr(s.getOutputStream(), sigbytes);
		getBiddingResult();
	}

	private static void runAuctioneerClient() throws Exception {
		//set up digital signature keys
		keyGen.initialize(1024, random);
		pair = keyGen.generateKeyPair();
		PublicKey[] clientSigs = new PublicKey[3];

		//send signature key for auctioneer
		cryptoMessaging.sendPublicKey(s.getOutputStream(), pair.getPublic()); 
		// receive signature keys for clients
		clientSigs[0] = cryptoMessaging.recvPublicKey(s.getInputStream());
		clientSigs[1] = cryptoMessaging.recvPublicKey(s.getInputStream());
		clientSigs[2] = cryptoMessaging.recvPublicKey(s.getInputStream());
		// sign starting price
		BigInteger startingprice = BigInteger.valueOf(20); //arbitrary for now
		byte[] sigbytes = sign(startingprice, pair.getPrivate());
		// send starting price with signature
		cryptoMessaging.sendBigInteger(s.getOutputStream(), startingprice);
		cryptoMessaging.sendByteArr(s.getOutputStream(), sigbytes);

		getAuctioneerResult();
	}

	public static void main(String[] args) throws Exception {
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
		
		keyGen = KeyPairGenerator.getInstance("DSA", "SUN");
		random = SecureRandom.getInstance("SHA1PRNG", "SUN");
		
		if (bidder) runBidderClient();
		else runAuctioneerClient();
	}
}
