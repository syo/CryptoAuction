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

public class Server {
	private static final int portno=9091;
	private static final int numBidders=3;
	private static Socket auctioneer;
	private static ArrayList<Socket> bidders;
	private static ServerSocket serverSocket;
	private static PaillierPublicKey[] pk;
	private static PaillierPublicKey auctioneer_pk;
	
	//
	// Server helper methods
	//
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
	private static BigInteger[] getBids() {
		//This should get the four bids and return them.
		return null;
		
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
	public static void main(String[] args) {
		testServer();
		System.out.println("Server started up. Listening for connections on port "+portno);
		try {
			getClients();
		} catch (IOException e) {
			System.out.println("ERROR: IO Exception. Could not initialize bidders.");
			giveUp();
		}
		Scanner scanner=new Scanner(System.in);
		BigInteger[] bids=getBids();
		//TODO: Get starting price
		
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