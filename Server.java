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
	private static ArrayList<Socket> clients;
	private static ServerSocket serverSocket;
	private static PaillierPublicKey[] pk;
	
	//
	// Server helper methods
	//
	//Terminate the server, usually because of an error.
	private static void giveUp() {
		System.out.println("The server will now terminate.");
		throw(new RuntimeException());
	}
	//Attempt to receive a BigInteger from the specified client after encrypting it. 
	//Throws an error and terminates if not successful.
	private static BigInteger readFromClient(int clientNo) {
		BigInteger result=BigInteger.ZERO;
		try {
			InputStream stream=clients.get(clientNo).getInputStream();
			result=cryptoMessaging.recvBigInteger(stream);
		} catch (IOException io) {
			System.out.println("ERROR: IO Exception. Could read message from client "+clientNo);
			giveUp();
		}
		return result;
	}
	//Attempt to write a BigInteger into the client's socket. Throws an error and terminates if not successful.
	private static void sendToClient(int clientNo,BigInteger msg) {
		msg=pk[clientNo].encrypt(msg)[0];
		try {
			OutputStream stream=clients.get(clientNo).getOutputStream();
			cryptoMessaging.sendBigInteger(stream,msg);
		} catch (IOException io) {
			System.out.println("ERROR: IO Exception. Could not send message to client "+clientNo);
			giveUp();
		}
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
	//Get sockets to all clients. These clients should be verified before proceeding.
	private static void getClients() throws IOException {
		clients=new ArrayList<Socket>();
		serverSocket=new ServerSocket(portno);
		while (clients.size()<4) {
			Socket temp=serverSocket.accept();
			//if (verify(temp))
				clients.add(temp);
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
		System.out.println("Enter asking price:");
		System.out.println("Server started up. Listening for connections on port "+portno);
		try {
			getClients();
		} catch (IOException e) {
			System.out.println("ERROR: IO Exception. Could not initialize clients.");
			giveUp();
		}
		Scanner scanner=new Scanner(System.in);
		BigInteger askingPrice=BigInteger.valueOf(Integer.parseInt(scanner.nextLine()));
		BigInteger[] bids=getBids();
		BigInteger highest=askingPrice;
		BigInteger price=askingPrice;
		int winnerIndex=4;
		//Determine the winner and the price of the item
		for (int x=0;x<4;x++) {
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
		for (int x=0;x<4;x++) {
			if (winnerIndex==x) {
				sendToClient(x,price);
			}
			else {
				sendToClient(x,BigInteger.ZERO);
			}
		}
		System.out.println("Auction complete. The server will now terminate.");
	}
}
