package cryptoII;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.PublicKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;

import de.henku.jpaillier.PaillierPublicKey;

public class cryptoMessaging {
	//
	// INPUT HANDLERS
	//

	//Read the specified number of bytes from the stream. Do not end until all bytes are read.
	private static byte[] demandBytes(InputStream i,int numBytes) throws IOException {
		byte[] me=new byte[numBytes];
		int remainingBytes=numBytes;
		while (remainingBytes>0) {
			remainingBytes-=i.read(me,numBytes-remainingBytes,remainingBytes);
		}
		return me;
	}

	//Read an int from the stream.
	public static int recvInt(InputStream i) throws IOException {
		byte[] st=demandBytes(i,4);
		ByteBuffer bf=ByteBuffer.wrap(st);
		return bf.getInt();
	}

	//Write a BigInteger to the stream.
	public static BigInteger recvBigInteger(InputStream stream) throws IOException {
		int size=recvInt(stream);
		byte[] me=demandBytes(stream,size);
		return new BigInteger(me);
	}

	//Read a PublicKey from the stream.
	public static PublicKey recvPublicKey(InputStream stream) throws IOException, NoSuchAlgorithmException, NoSuchProviderException, InvalidKeySpecException {
		byte[] byteKey = recvByteArr(stream);
        X509EncodedKeySpec X509publicKey = new X509EncodedKeySpec(byteKey);
        KeyFactory kf = KeyFactory.getInstance("DSA", "SUN");

        return kf.generatePublic(X509publicKey);
		/*int size=recvInt(stream);
		byte[] me=demandBytes(stream,size);
		return new PublicKey(me);*/
	}
	
	// Read a PaillerPublicKey
	public static PaillierPublicKey recvPaillier(InputStream stream) throws IOException {
		int bits=recvInt(stream);
		BigInteger n = recvBigInteger(stream);
		BigInteger nSquared = recvBigInteger(stream);
		BigInteger g = recvBigInteger(stream);
		return new PaillierPublicKey(n, nSquared, g, bits);
	}

	//Read a byte[] from the stream.
	public static byte[] recvByteArr(InputStream stream) throws IOException {
		int size=recvInt(stream);
		byte[] me=demandBytes(stream,size);
		return me;
	}
	//
	// OUTPUT HANDLERS
	//

	//Write an int to the stream.
	public static void sendInt(OutputStream o,int x) throws IOException {
		ByteBuffer b = ByteBuffer.allocate(4);
		b.putInt(x);
		o.write(b.array());
		o.flush();
	}
	
	//Write a big integer to the stream.
	public static void sendBigInteger(OutputStream o, BigInteger msg) throws IOException {
		byte[] me=msg.toByteArray();
		int msgSize=me.length;
		sendInt(o,msgSize);
		o.write(me);
		o.flush();
	}

	//Write a PublicKey to the stream.
	public static void sendPublicKey(OutputStream o, PublicKey msg) throws IOException {
        byte[] byteKey = msg.getEncoded();
        sendByteArr(o, byteKey);
		/*byte[] me= msg.toByteArray();
		int msgSize=me.length;
		sendInt(o,msgSize);
		o.write(me);
		o.flush();*/
	}
	
	//Write a byte[] to the stream.
	public static void sendByteArr(OutputStream o, byte[] msg) throws IOException {
		int msgSize=msg.length;
		sendInt(o,msgSize);
		o.write(msg);
		o.flush();
	}
	
	// Write a PaillerPublicKey
	public static void sendPaillier(OutputStream stream, PaillierPublicKey pk) throws IOException {
		sendInt(stream, pk.getBits());
		sendBigInteger(stream, pk.getN());
		sendBigInteger(stream, pk.getnSquared());
		sendBigInteger(stream, pk.getG());
	}
}
