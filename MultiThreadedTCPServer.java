import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.BindException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.util.Date;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.lang.management.ManagementFactory;
import com.sun.management.OperatingSystemMXBean;

public class MultiThreadedTCPServer {
	private static double throughputSum = 0;
	private static int userCount = 0;
	private static double cpuLoadSum = 0;
	private static double ramUsageSum = 0;

    private static class TCPWorker implements Runnable {

        private Socket client;
        private String clientbuffer;

        public TCPWorker(Socket client) {
            this.client = client;
            this.clientbuffer = "";
        }
        
        private String generatePayload() {
        	String payload = new String("");
        	String sample = "Lorem ipsum dolor sit amet, consectetur adipiscing"
        			+ " elit. Vivamus vehicula dignissim mauris, sit amet "
        			+ "aliquam eros gravida a. Suspendisse vitae fermentum "
        			+ "enim, sed ultricies ligula. Nullam lacinia, lacus eget "
        			+ "interdum tempor, ex enim pellentesque arcu, ut faucibus "
        			+ "sapien nunc eu diam. Aenean mollis ut erat et scelerisque. "
        			+ "Etiam quis sodales ipsum. Sed in purus sagittis, imperdiet "
        			+ "arcu id, mattis elit. Donec faucibus cursus mi, sed commodo "
        			+ "tortor laoreet ut. Morbi ut neque sollicitudin, interdum "
        			+ "mauris sed, euismod quam. Nulla sed aliquam purus.\n" + 
        			"\n" + 
        			"Interdum et malesuada fames ac ante ipsum primis in "
        			+ "faucibus. Pellentesque egestas nulla ut leo laoreet "
        			+ "facilisis. Praesent a velit erat. Nunc tempus tincidunt "
        			+ "nulla eu pretium. Vestibulum vel diam ligula. Maecenas "
        			+ "tincidunt magna feugiat aliquam fringilla. Praesent blandit "
        			+ "purus sit amet justo commodo hendrerit. Sed ultricies a "
        			+ "neque a interdum. Nullam efficitur diam eget tortor volutpat, "
        			+ "et scelerisque ligula faucibus. Aliquam venenatis enim "
        			+ "dapibus, pulvinar volutpat.\n";
        	int payloadSize = 30 + (int)(Math.random() * 271);
        	for (int i=0; i<payloadSize; i++) {
        		payload.concat(sample);
        	}
        	return payload;
        }

        @Override
        public void run() {

            try {
                //System.out.println("Client connected with: " + 
                //		this.client.getInetAddress());
            	long connectionStart = System.currentTimeMillis();
            	DataOutputStream output = new 
                		DataOutputStream(client.getOutputStream());
                BufferedReader reader = new BufferedReader(
                        new InputStreamReader(this.client.getInputStream())
                );
                
                OperatingSystemMXBean bean = (com.sun.management.OperatingSystemMXBean) 
                		ManagementFactory.getOperatingSystemMXBean();
                double cpuSum = 0;
                double ramSum = 0;
                int requestCount = 0;
                while ((this.clientbuffer = reader.readLine())!=null){
	                cpuSum += bean.getSystemCpuLoad();
	                double totalRAM = bean.getTotalPhysicalMemorySize();
	                double freeRAM = bean.getFreePhysicalMemorySize();
	                ramSum += (totalRAM - freeRAM) / totalRAM;
	                //System.out.println("[" + new Date() + "] Received: " 
	                //		+ this.clientbuffer);
	                requestCount++;
	                String msg[] = this.clientbuffer.split(" ", 5);
	                int userID = Integer.parseInt(msg[3]);
	                String response = "WELCOME " + userID + " " + generatePayload();
	                
	                output.writeBytes(response + System.lineSeparator());
                }
                long connectionEnd = System.currentTimeMillis();
                long conDuration = (connectionEnd - connectionStart)/1000;
                double throughput = requestCount / (double) conDuration;
                
                throughputSum += throughput;
                userCount += 1;
                
                double avgCPU = cpuSum / (double) conDuration;
                cpuLoadSum += avgCPU;
                
                double avgRAM = ramSum / (double) conDuration;
                ramUsageSum += avgRAM;
                
                System.out.println(throughput + "\t" + avgCPU + "\t" + avgRAM);
                
            }
            catch (SocketException se) {
            	System.out.println("Connection reset.");
            }
            catch (IOException e) {
                e.printStackTrace();
            }

        }

    }

    public static ExecutorService TCP_WORKER_SERVICE = 
    		Executors.newFixedThreadPool(20);

    public static void main(String args[]) {
        try {
        	if (args.length != 1 ) {
        		System.out.println("Error! You must enter a port number for "
        				+ "the server (0-65535)");
        		System.exit(1);
        	}
        	int port = Integer.parseInt(args[0]);
        	if (port < 0 || port > 65535) {
        		System.out.println("The port you entered can not be used. "
        				+ "Please enter a port between 0-65535");
        		System.exit(1);
        	}
            ServerSocket socket = new ServerSocket(port);

            System.out.println("Server listening to: " + socket.getInetAddress()
            	+ ":" + socket.getLocalPort());

            while (true) {
                Socket client = socket.accept();

                TCP_WORKER_SERVICE.submit(
                        new TCPWorker(client)
                );
                
            }
          
        } catch (BindException be) {
        	System.out.println("Address already in use (Bind failed).");
        }
        catch (IOException e) {
            e.printStackTrace();
        }
        finally {
        	if (userCount == 0) {
        		System.out.println("Error! User count is zero.");
        		System.exit(1);
        	}
        	double avgThroughput = throughputSum / userCount;
            System.out.println("Average Server Throughput = " + avgThroughput);
            
            double avgCPULoad = cpuLoadSum / userCount;
            System.out.println("Average Server CPU Load = " + avgCPULoad);
            
            double avgRamUsage = ramUsageSum / userCount;
            System.out.println("Average Server RAM Usage = " + avgRamUsage);
           
        }
    }

}

