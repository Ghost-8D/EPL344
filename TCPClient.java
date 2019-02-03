import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.ConnectException;
import java.net.NoRouteToHostException;
import java.net.Socket;
import java.util.Date;
import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.CyclicBarrier;

public class TCPClient {
	
	private static double rttSum = 0;
	private static int rttCount = 0;
	private static CyclicBarrier barrier;
	
	private static class TCPuser implements Runnable {

		private int userID;
        private String serverIP;
        private int serverPort; 

        public TCPuser(String sIP, int sPort, int uID) {
            serverIP = sIP;
            serverPort = sPort;
            userID = uID;
        }
        
        @Override
        public void run() {
            try {
            	String message, response;
	            Socket socket = new Socket(serverIP, serverPort);
	
	            DataOutputStream output = new 
	            		DataOutputStream(socket.getOutputStream());
	            BufferedReader server = new BufferedReader(
	                    new InputStreamReader(socket.getInputStream())
	            );
	            long totalRTT = 0;
            	for (int j=0; j<300; j++) {     
		            message = "HELLO " + socket.getLocalAddress() + " " 
		            		+ socket.getLocalPort() +" "+ userID + System.lineSeparator();
		            long startRTT = System.currentTimeMillis();
		            output.writeBytes(message);
		            response = server.readLine();
		            long endRTT = System.currentTimeMillis();
		            long RTT = endRTT - startRTT;
		            totalRTT += RTT;
		            //System.out.println("RTT\t" + RTT);
		            //System.out.println("[" + new Date() + "] Received: " + response);
        		}
            	double avgRTT = totalRTT / 300.0;
            	rttSum += avgRTT;
            	rttCount += 1;
            	socket.close();
            	System.out.println("User " + (userID+1) + " has closed the connection!");
            	barrier.await();
            } 
            catch (NoRouteToHostException nre) {
            	System.out.println("The specified host is unreachable. Please "
            			+ "make sure you entered the correct server IP address"
            			+ " and that the server is operating.");
            	System.exit(1);
            }
            catch (ConnectException ce){
            	System.out.println("Connection timed out. Please make sure you entered" 
            			+ " the correct IP address / Port number and that the server "
            			+ "is running.");
            	System.exit(1);
            }
            catch (IOException ioe) {
                ioe.printStackTrace();
            }
            catch (BrokenBarrierException bbe){
            	bbe.printStackTrace();
            }
            catch (Exception e){
            	e.printStackTrace();
            }
        }
    }
	
	public static class MasterThread implements Runnable {
		 
        @Override
        public void run() {
        	if (rttCount < 1) {
                System.out.println("Error! RTT count is zero.");
                System.exit(1);
        	}
        	double latency = rttSum / (double) rttCount;
            System.out.println("Average Communication Latency = " + latency);
        }
    }

    public static void main(String args[]) {
        try {
        	if (args.length != 3) {
        		System.out.println("Missing or incorrect arguments given! "
        				+ "Please try again (arg: server_IP server_port total_users)");
        		System.exit(1);
        	}
        	String serverIP = args[0]; 	
        	int serverPort = Integer.parseInt(args[1]);
        	if (serverPort < 1024 || serverPort > 65535) {
        		System.out.println("The port number does not exists/is not available. "
        				+ "Please enter a port number between 1024-65535.");
        		System.exit(1);
        	}
        	int concurrentUsers = Integer.parseInt(args[2]);
        	if (concurrentUsers < 1) {
        		System.out.println("Error! The number for the users must be an "
        				+ "integer above 0.");
        		System.exit(1);
        	}
        	barrier = new CyclicBarrier(concurrentUsers, new MasterThread());
        	for (int i=0; i<concurrentUsers; i++) {
        		TCPuser user = new TCPuser(serverIP, serverPort, i);
	            Thread tUser = new Thread(user);
	            tUser.start();
        	}
        	
        } 
        catch (Exception e) {
            e.printStackTrace();
        }
    }

}
