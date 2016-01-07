package sr.akarbarc;

import sr.akarbarc.node.Node;

import java.util.Scanner;

public class Main {

    public static void main(String[] args) {
        if (args.length < 3) {
            System.err.println("Usage: Main tracker_host tracker_port node_port");
            System.exit(1);
        }
        String trackerHost = args[0];
        int trackerPort = Integer.parseInt(args[1]);
        int nodePort = Integer.parseInt(args[2]);

        Node node = new Node(trackerHost, trackerPort, nodePort);
        try {
            if(!node.start()) {
                System.out.println("Failed to start node.");
                node.stop();
                return;
            }

            Scanner reader = new Scanner(System.in);
            while (true) {
                System.out.println("Press ENTER to get resource or type \"exit\" to exit.");
                if (reader.nextLine().equalsIgnoreCase("exit"))
                    break;

                System.out.println("Trying to get resource...");
                try {
                    node.getResource();
                    System.out.println("Success to get resource, press ENTER to release it.");
                    reader.nextLine();
                    node.releaseResource();
                    System.out.println("Resource released.");
                } catch (Exception e) {
                    System.err.println("Unable to get resource.");
                }
            }
            node.stop();
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }
}
