import java.awt.*;
import java.awt.event.*;
import java.io.IOException;
import java.net.*;
import java.util.*;


public class Server extends Frame implements Runnable {
	
	private static Thread t;
	private static int port = 55002;
	private ArrayList<ServerThread> serverThreads = new ArrayList<ServerThread>();
	private boolean kraj = false;
	private DatagramSocket listeningSocket;
	private DatagramSocket sendSocket;
	private Label prikazivac;
	
	public ArrayList<ServerThread> getServerThreads() {
		return serverThreads;
	}
	
	public Server()  {
		super("ServerSide");
		setSize(500,500);
		try {
			listeningSocket = new DatagramSocket(55000);
			sendSocket = new DatagramSocket(55001);
//			listeningSocket.setBroadcast(true);
		} catch (Exception e) {
			e.printStackTrace();
		}
		addWindowListener(new ProzorDogadjaji());
		popuniProzor();
		setVisible(true);
	}
	
	public boolean getKraj() {
		return kraj;
	}
	private class ProzorDogadjaji extends WindowAdapter {
		public void windowClosing(WindowEvent e) {
			System.out.println("Usao u WindowClosing!");
			for(ServerThread s: serverThreads) {
				s.zavrsi();
			}
			zaustaviServer();
			System.out.println("Zaustavio server!");
			
//			serverThread.zavrsi(); MORA DA PRODJE KROZ SVE NITI DA IH INTERRUPT-UJE I DA ZATVORI SOCKET-E !
			dispose();
		}
	}
	public Label getPrikazivac() {
		return prikazivac;
	}
	
/*	private void pokreni() {
		serverThread.start();
	}
*/
	
	private void zaustaviServer() {
		t.interrupt();
		listeningSocket.close();
		sendSocket.close();
		kraj = true;
	}
	
	private void popuniProzor() {
		setLayout(new GridLayout(2,1));
		add(new Label("Serverska strana kreirana",Label.CENTER),"North");
		prikazivac = new Label("",Label.CENTER);
		add(prikazivac,"South");
	}
	
	private void closeSocket() throws IOException {
		System.out.println("server zatvara svoje socket-e");
		listeningSocket.close();
		sendSocket.close();
	}
	
	public static void main(String[] args) {
		Server s = new Server();
		t = new Thread(s);
		t.start();
	//	t.stop();
		
	}
	
	public void sendMessageToAllClients() {
		byte[] sendMess = new byte [5000];
		for (ServerThread s : serverThreads) {
			sendMess = new String(prikazivac.getText()).getBytes();
			DatagramPacket sendPacket = new DatagramPacket(sendMess,sendMess.length,s.getClientAddress(),s.getClientPort());
			try {
				sendSocket.send(sendPacket);
			} catch (IOException e) {
				e.printStackTrace();
			}
			for (int i=0;i<sendMess.length;i++) {
				sendMess[i] = 0;
			}
		}
	}
	

	
	@Override
	public void run()  {
		
		System.out.println("Server je pokrenut!");
		byte[] recieveBuff = new byte[5000];
		byte[] sendBuff = new byte[5000];
		while (!Thread.currentThread().isInterrupted()) {
			DatagramPacket recievePacket = new DatagramPacket (recieveBuff,recieveBuff.length);
			for(ServerThread s : serverThreads) {
				System.out.println(s.getClientPort());
			}
			try {
				System.out.println("Server ceka broadcast paket!");
				listeningSocket.receive(recievePacket);
			} catch (IOException e) {}
			if (kraj) break;
			String message = new String(recievePacket.getData()).trim();
			
			if (message.equals("broadcast")) {
				System.out.println("Server primio broadcast paket!");
				sendBuff = "i_am_server".getBytes();
				InetAddress clientAddress = recievePacket.getAddress();
				int clientPort = recievePacket.getPort();
				DatagramPacket sendPacket = new DatagramPacket(sendBuff,sendBuff.length,clientAddress,clientPort);
				try {
					listeningSocket.send(sendPacket);
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
			
			if (message.equals("connect")) {
				System.out.println("Server primio connect zahtev!");
				int serverThreadPort = port++;
				InetAddress clientAddress = recievePacket.getAddress();
				int clientPort = recievePacket.getPort();
				ServerThread s = new ServerThread(this,serverThreadPort,clientAddress,clientPort);
				serverThreads.add(s);
				sendBuff = new String(String.valueOf(serverThreadPort)).getBytes();
				DatagramPacket sendPacket = new DatagramPacket(sendBuff,sendBuff.length,clientAddress,clientPort);
				try {
					listeningSocket.send(sendPacket);
				} catch (IOException e) {
					e.printStackTrace();
				}
				s.start();
			}
			
			if (message.equals("deleteme")) {
				System.out.println("Zaustavljam nit na serveru!");
				InetAddress address = recievePacket.getAddress();
				int port = recievePacket.getPort();
				ServerThread s = null;
				for (int i=0; i<serverThreads.size();i++) {
					s = serverThreads.get(i);
					if (s.getClientAddress().equals(address) && s.getClientPort()==port) {
						System.out.println("usao u if");
						serverThreads.remove(i);
						s.zavrsi();
					}
				}
			}
			
			for (int i=0;i<recieveBuff.length;i++) {
				recieveBuff[i] = 0;
			}
			for (int i=0;i<sendBuff.length;i++) {
				sendBuff[i] = 0;
			}
		}
		

	}
	
	protected void finalize() throws Throwable {
		super.finalize();
		closeSocket();
	}

	

}
