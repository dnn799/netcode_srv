import java.awt.*;
import java.io.IOException;
import java.net.*;
import java.util.ArrayList;

public class ServerThread extends Thread {
	
	private Server server;
	private InetAddress clientAddress;
	private int clientPort;
	
	
	private DatagramSocket socketCommunication;
	
	public ServerThread(Server serv,int portOfServerThread, InetAddress cliAddr, int cliPort) {
		server = serv;
		try {
			socketCommunication = new DatagramSocket(portOfServerThread);
		} catch (SocketException e) {
			e.printStackTrace();
		}
		clientAddress = cliAddr;
		clientPort = cliPort;
		
	}
	
	public InetAddress getClientAddress() {
		return clientAddress;
	}
	
	public int getClientPort() {
		return clientPort;
	}
	
	
	
	public void run() {
		try {
			
		
		System.out.println(socketCommunication.getLocalPort());
		byte[] recieveData = new byte [10000];
		
		while(!interrupted()){
			synchronized (this) {
				System.out.println("Nit na portu "+socketCommunication.getLocalPort()+" ceka paket!");
				DatagramPacket recievePacket = new DatagramPacket(recieveData,recieveData.length);
				try {
					socketCommunication.receive(recievePacket); // ovde se blokira dok ne primi paket
				} catch (Exception e) {}
				
				
				if(server.getKraj() || interrupted()) break;
				
				String message = new String( recievePacket.getData()).trim();
				System.out.println("Nit na portu "+socketCommunication.getLocalPort()+" je primila paket!");
				server.getPrikazivac().setText(message); // postavlja message u prikazivac na serveru
				
				server.sendMessageToAllClients();
				
				for (int i=0;i<recieveData.length;i++) {
					recieveData[i] = 0;
				}
				
			}
			
		}
		} catch (Exception e) {}
	}
	
	protected void finalize() throws Throwable {
		super.finalize();
		socketCommunication.close();
	}
	
	public void zavrsi() {
		System.out.println("zatvara socket klijenta!");
		socketCommunication.close();
		interrupt();
		
	}
	
}
