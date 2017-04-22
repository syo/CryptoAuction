package cryptoII;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.math.BigInteger;
import java.nio.ByteBuffer;

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

}