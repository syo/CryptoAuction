package cryptoII;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Scanner;

import de.henku.jpaillier.PaillierPublicKey;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.PublicKey;
import java.security.Signature;
import java.security.spec.InvalidKeySpecException;

public class Server {
	private static final int portno=9091;
	private static final int numBidders=3;
	private static Socket auctioneer;
	private static ArrayList<Socket> bidders;
	private static ServerSocket serverSocket;
	private static PaillierPublicKey[] pk;
	private static PaillierPublicKey auctioneer_pk;
	private static PublicKey[] biddersigs;
	private static BigInteger askingPrice;
	
	//
	// Server helper methods
	//

	//verify a signature is correct
	private static boolean verify(BigInteger number, PublicKey pubKey, byte[] sigbytes) throws Exception {
		Signature sig = Signature.getInstance("SHAwithDSA");
		sig.initVerify(pubKey);
		byte[] data = bigIntToByteArray(number);
		sig.update(data);
		return sig.verify(sigbytes);
  	}
	
	private static byte[] bigIntToByteArray(BigInteger bigInteger) {
		byte[] array = bigInteger.toByteArray();
		if (array[0] == 0) {
			byte[] tmp = new byte[array.length - 1];
			System.arraycopy(array, 1, tmp, 0, tmp.length);
			array = tmp;
		}
		return array;
	}
	
	//Terminate the server, usually because of an error.
	private static void giveUp() {
		System.out.println("The server will now terminate.");
		throw(new RuntimeException());
	}

	//Attempt to send a BigInteger to the specified socket. 
	//Throws an error and terminates if not successful.
	private static BigInteger readFromClient(Socket socket) {
		BigInteger result=BigInteger.ZERO;
		try {
			InputStream stream=socket.getInputStream();
			result=cryptoMessaging.recvBigInteger(stream);
		} catch (IOException io) {
			System.out.println("ERROR: IO Exception. Could read message from client.");
			giveUp();
		}
		return result;
	}

	//Attempt receive a BigInteger from the specified socket.
	private static void sendToClient(Socket socket,BigInteger msg) {
		try {
			OutputStream stream=socket.getOutputStream();
			cryptoMessaging.sendBigInteger(stream,msg);
		} catch (IOException io) {
			System.out.println("ERROR: IO Exception. Could not send message to client.");
			giveUp();
		}
	}

	// Encrypt a message and send it to the specified bidder.
	private static void sendToBidder(int clientNo, BigInteger msg) {
		msg=pk[clientNo].encrypt(msg)[0];
		sendToClient(bidders.get(clientNo),msg);
	}

	// Send a message to the auctioneer
	private static void SendToAuctioneer(BigInteger msg) {
		msg=auctioneer_pk.encrypt(msg)[0];
		sendToClient(auctioneer,msg);
	}

	// Receive a message from the specified bidder.
	private static BigInteger recvFromBidder(int bidderNo) {
		return readFromClient(bidders.get(bidderNo));
	}
	
	//Return true if this client is valid. Return false if not.
	private static boolean verify(Socket s) {
		//TODO: Implement this.
		return true;
	}

	private static void informAuctioneer(int winner) throws IOException {
		cryptoMessaging.sendInt(auctioneer.getOutputStream(), winner);
	}
	
	private static void informBidders(int winner, BigInteger price) throws IOException {
		for (int i=0; i<3; i++) {
			if (i == winner) {
				cryptoMessaging.sendInt((bidders.get(i)).getOutputStream(), 1);
				cryptoMessaging.sendBigInteger((bidders.get(i)).getOutputStream(), price);
			}
			else {
				cryptoMessaging.sendInt((bidders.get(i)).getOutputStream(), 0);
			}
		}
	}
	
	private static void evaluateBids(BigInteger[] bids, boolean[] bidAccepted) throws IOException {
		BigInteger max = askingPrice;
		BigInteger second = askingPrice;
		
		int cur_max = -1;
		
		for (int i=0; i < 3; i++) {
			if (bidAccepted[i]) {
				if (bids[i].compareTo(max) > 0) {
					second = max;
					max = bids[i];
					cur_max = i;
				}
			}
		}
		
		informAuctioneer(cur_max);
		informBidders(cur_max, second);
	}
	
	private static BigInteger[] getBids() throws Exception {
		//This should get the 3 bids and return them.
		InputStream stream;
		BigInteger bids[] = new BigInteger[3];
		byte[][] signatures = new byte[3][];
		byte[] signature;
		boolean[] bidAccepted = new boolean[3];
		//for each bidder
		for (int i=0; i < bidders.size(); i++) {
			stream=(bidders.get(i)).getInputStream();
			//get their bid
			bids[i] = cryptoMessaging.recvBigInteger(stream);
			//get their signature
			signature = cryptoMessaging.recvByteArr(stream);
			
			bidAccepted[i] = false;
			// if bid is from the person
			if (verify(bids[i], biddersigs[i], signature)) {
				// and the bid is higher than the asking price...
				if (bids[i].compareTo(askingPrice) >= 0) {
					bidAccepted[i] = true;
				}
				else {
					System.out.println("Bid below the asking price");
				}
			}
			else {
				System.out.println("Bid signature did not match source");
			}
		}
		
		// evaluateBids(bids, bidAccepted); //possible better framework for bid eval than what we got, I was on autopilot and made it
		
		return bids;	
	}

	//Get sockets to all bidders and auctioneer. These should be verified before proceeding.
	private static void getClients() throws IOException {
		bidders=new ArrayList<Socket>();
		serverSocket=new ServerSocket(portno);
		while ((bidders.size()<numBidders)&&(auctioneer==null)) {
			Socket temp=serverSocket.accept();
			//if (this client is a bidder)
				bidders.add(temp);
			//if (this client is the auctioneer)
				auctioneer=temp;
		}
	}
	
	//For testing the server. Should not be called otherwise.
	private static void testServer() {
		try {
			serverSocket=new ServerSocket(portno);
			Socket temp=serverSocket.accept();
			int x=cryptoMessaging.recvInt(temp.getInputStream());
			System.out.println("Got this int: "+x);
			x++;
			cryptoMessaging.sendInt(temp.getOutputStream(), x);
			x=cryptoMessaging.recvInt(temp.getInputStream());
			System.out.println("Got this int: "+x);
		} catch (IOException e) {
			System.out.println("IO Error");
			giveUp();
		}
		System.out.println("Test complete.");
		giveUp();
	}

	// Exchange public digital signature keys between server and clients 
	private static void keyExchange() throws IOException, NoSuchAlgorithmException, NoSuchProviderException, InvalidKeySpecException {
		//receive from server
		PublicKey keyserver = cryptoMessaging.recvPublicKey(auctioneer.getInputStream());
		//receive from clients
		PublicKey keyclients[] = new PublicKey[3];
		biddersigs = new PublicKey[3];
		InputStream stream;
		for (int i=0; i < bidders.size(); i++) {
			stream=(bidders.get(i)).getInputStream();
			keyclients[i] = cryptoMessaging.recvPublicKey(stream);
			biddersigs[i] = keyclients[i];
		}
		//send to auctioneer
		cryptoMessaging.sendPublicKey(auctioneer.getOutputStream(), keyclients[0]); 
		cryptoMessaging.sendPublicKey(auctioneer.getOutputStream(), keyclients[1]); 
		cryptoMessaging.sendPublicKey(auctioneer.getOutputStream(), keyclients[2]); 
		//send to clients
		cryptoMessaging.sendPublicKey((bidders.get(0)).getOutputStream(), keyserver); 
		cryptoMessaging.sendPublicKey((bidders.get(1)).getOutputStream(), keyserver); 
		cryptoMessaging.sendPublicKey((bidders.get(2)).getOutputStream(), keyserver); 
		
		
		// now receive paillier public keys from bidders and auctioneer
		auctioneer_pk = cryptoMessaging.recvPaillier(auctioneer.getInputStream());
		pk[0] = cryptoMessaging.recvPaillier((bidders.get(0)).getInputStream());
		pk[1] = cryptoMessaging.recvPaillier((bidders.get(1)).getInputStream());
		pk[2] = cryptoMessaging.recvPaillier((bidders.get(2)).getInputStream());
	}

	//receive and distribute starting price
	private static BigInteger startingPrice() throws IOException {
		// get starting price and signature
		BigInteger startingprice = cryptoMessaging.recvBigInteger(auctioneer.getInputStream());
		byte[] signature = cryptoMessaging.recvByteArr(auctioneer.getInputStream());
		
		// send them to clients
		cryptoMessaging.sendBigInteger((bidders.get(0)).getOutputStream(), startingprice); 
		cryptoMessaging.sendByteArr((bidders.get(0)).getOutputStream(), signature); 
		cryptoMessaging.sendBigInteger((bidders.get(1)).getOutputStream(), startingprice); 
		cryptoMessaging.sendByteArr((bidders.get(1)).getOutputStream(), signature); 
		cryptoMessaging.sendBigInteger((bidders.get(2)).getOutputStream(), startingprice); 
		cryptoMessaging.sendByteArr((bidders.get(2)).getOutputStream(), signature); 
		
		return startingprice;
	}

	public static void main(String[] args) throws Exception {
		testServer();
		System.out.println("Server started up. Listening for connections on port "+portno);
		try {
			getClients();
		} catch (IOException e) {
			System.out.println("ERROR: IO Exception. Could not initialize bidders.");
			giveUp();
		}
		Scanner scanner=new Scanner(System.in);

		keyExchange();
		askingPrice = startingPrice();
		BigInteger[] bids=getBids();
		
		BigInteger highest=askingPrice;
		BigInteger price=askingPrice;
		int winnerIndex=numBidders;
		//Determine the winner and the price of the item
		for (int x=0;x<numBidders;x++) {
			if (bids[x].compareTo(highest)>0) {
				price=highest;
				winnerIndex=x;
				highest=bids[x];
			} //End of if block
			else if (bids[x].compareTo(price)>0) {
				price=bids[x];
			} //End of else block
		
		}
		System.out.println("The auction has been decided.");
		System.out.println("The winner is client # "+winnerIndex+" who bid "+highest);
		System.out.println("The price of the item shall be "+price);
		//Alert the winner that they won (if a winner exists) and notify them of the price.
		for (int x=0;x<numBidders;x++) {
			if (winnerIndex==x) {
				sendToBidder(x,price);
				SendToAuctioneer(BigInteger.valueOf(x));
			}
			else {
				sendToBidder(x,BigInteger.ZERO);
			}
		}
		if (winnerIndex==numBidders) {
			SendToAuctioneer(BigInteger.valueOf(numBidders));
			SendToAuctioneer(BigInteger.ZERO);
		}
		else {
			SendToAuctioneer(price);
		}
		System.out.println("Auction complete. The server will now terminate.");
	}
}