/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package sonnode;

import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.net.*;
import java.nio.file.Paths;
import javax.swing.*;
import java.util.logging.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Queue;
import java.util.Scanner;
import java.util.Set;

/**
 *
 * @author Usman
 */
public class SONNode {
    
    //Constants
    private final String QUERY_CATEGORIES_FILE = "query_categories.txt";
    private final String NODE_CATEGORIES_FILE = "node_categories.txt";
    private final String SEARCH_SCRIPT = "python " + Paths.get(".", "search_engine.py").toString();
    private final String QUERY_CLASSIFIER_SCRIPT = "python " + Paths.get(".", "query_classifier.py").toString();
    private final String NODE_CLASSIFIER_SCRIPT = "python " + Paths.get(".", "node_classifier.py").toString();
    private final int REFRESH_INTERVAL = 30; //In seconds
    private static final Logger logger = Logger.getLogger(SONNode.class.getName());
    
    //data members
    private String peer_ip;
    private int peer_port;
    private int server_port;
    private String server_ip;
    private String shell;
    private String shell_param;
    private HashMap<String, NodeCategoryInfo> categories;
    private HashMap<String, HostAddress> super_peers;
    
    
    public static void main(String[] args) {
        logger.entering(SONNode.class.getName(), "main");
        
        SONNode node;
        if(args.length == 4){
            //@TODO get ip of machine
            node = new SONNode(args[0], args[1], args[2], args[3]);
        } else if(args.length == 2){
            //@TODO get ip of machine
            node = new SONNode(args[0], args[1], null, null);
        } else {
            System.out.println("Usage: \n\tSONNode.jar server_ip server_port [remote_ip] [remote_port]");
            return;
        }
        node.go();
        
        logger.exiting(SONNode.class.getName(), "main");
    }
    
    public SONNode(String _server_ip, String _server_port, String _peer_ip, String _peer_port){
        logger.entering(SONNode.class.getName(), "SONNode");
        server_ip = _server_ip;
        server_port = Integer.parseInt(_server_port);
        peer_ip = _peer_ip;
        super_peers = new HashMap<String, HostAddress>();
        
        if(_peer_port != null)
            peer_port = Integer.parseInt(_peer_port);
        else
            peer_port = -1;
        
        categories = new HashMap<String, NodeCategoryInfo>();
        logger.setLevel(Level.INFO);
        
        if(System.getProperty("os.name").toLowerCase().contains("win")){
            shell = "cmd";
            shell_param = "/C";
        } else {
            shell = "bash";
            shell_param = "-c";
        }
        logger.exiting(SONNode.class.getName(), "SONNode");
    }
    

    private String [] resolve_locally(String query){
        StringBuffer output = new StringBuffer();
        Process p;
        try {
            String current_directory =  System.getProperty("user.dir");
            query = query.replaceAll(" ", ",");
            String exec_string = SEARCH_SCRIPT +  " \"" + query + "\"";
            logger.info("Executing Script: " + exec_string);
            p = Runtime.getRuntime().exec(exec_string);
            p.waitFor();
            BufferedReader reader = new BufferedReader(new InputStreamReader(p.getInputStream()));
            String line = "";
            line = reader.readLine();
            output.append(line);
        } catch (Exception e) {
                e.printStackTrace();
        }
        
        logger.info("Script executed, result: " + output.toString());

        ArrayList<String> results = new ArrayList<String>();
        String [] result_array = output.toString().split("\\|");
        for(int i = 0; i < result_array.length; i++){
            if(!result_array[i].equals("")){
                results.add(result_array[i]);
            }
        }
        
        String [] return_array = new String[results.size()];
        return_array = results.toArray(return_array);
        return return_array;
    }
    
    String[] load_categories(String file){
        String content = "";
        try {
            content = new Scanner(new File(file)).useDelimiter("\\Z").next();
        } catch (FileNotFoundException ex) {
            Logger.getLogger(SONNode.class.getName()).log(Level.SEVERE, null, ex);
        }
        return content.split(",");
    }
    
    public void go(){
        logger.entering(SONNode.class.getName(), "go");
        /*
        File cat_file = new File(CAT_FILENAME);
        if(!cat_file.exists()){
            logger.log(Level.SEVERE, "Could not find categories file, {0}", CAT_FILENAME);
            return;
        }*/
        
        
        //String current_directory =  System.getProperty("user.dir");
        //String data_directory = Paths.get(current_directory, "data").toString();
        String exec_string = NODE_CLASSIFIER_SCRIPT;
        logger.info("Executing script: " + exec_string);
        ArrayList<String> node_categories;
        /*Scanner scanner;
        try {
            scanner = new Scanner(cat_file);
            scanner.useDelimiter(",");
            while(scanner.hasNext()) {
                node_categories.add(scanner.next());
            }
            scanner.close();
        } catch (FileNotFoundException ex) {
            logger.log(Level.SEVERE, null, ex);
        }*/
        
        Process p;
        try {
            String [] commands = { shell, shell_param, exec_string};
            p = Runtime.getRuntime().exec(commands);
            p.waitFor();
        } catch (Exception e) {
                e.printStackTrace();
        }
        
        logger.info("Script executed");

        String[] ncategories = load_categories(NODE_CATEGORIES_FILE);
        String temp = "";
        for(int i = 0; i < ncategories.length; i++){
            temp += ncategories[i] + " ";
        }
        logger.info("Classified as: " + temp);
        node_categories = new ArrayList<String>();
        
        for(int i = 0; i < ncategories.length; i++)
            if(!ncategories[i].isEmpty())
                node_categories.add(ncategories[i]);
        
        if(peer_ip == null){
            logger.info("No SON node given, creating a new SON");
            for(int i = 0; i < node_categories.size(); i++){
                NodeCategoryInfo tempCatInfo = new NodeCategoryInfo(true);
                categories.put(node_categories.get(i), tempCatInfo);

                HostAddress sock_addr = new HostAddress(server_ip, server_port);
                super_peers.put(node_categories.get(i), sock_addr);
            }
            logger.info("New SON created");
        } else {
            logger.info("Joining an existing SON at " + peer_ip + ":" + String.valueOf(peer_port));
            this.joinSON(node_categories);
        }
        Thread nm = new Thread(new NetworkManager());
        Thread qm = new Thread(new QueryManager());
        qm.start();
        nm.start();
        startServer();
        
        logger.exiting(SONNode.class.getName(), "go");
    }
    
    private void joinSON(ArrayList<String> node_categories){
        logger.entering(SONNode.class.getName(), "joinSON");
        
        for(int i = 0; i < node_categories.size(); i++){
            String cat = node_categories.get(i);
            String status = "f";
            String [] components = null;
            HostAddress address = new HostAddress(peer_ip, peer_port);
            
            while(status.equals("f")){
                String req = "jq," + cat + "," + server_ip + ":" + String.valueOf(server_port);
                String response = sendRequest(address.getIP(), address.getPort(), req);
                if(response.charAt(0) == 'j' && response.charAt(1) == 'a'){
                    components = response.split(",");
                    //components[1] is status n(not found)/f(forward)/j(joined)
                    status = components[1];
                    if(status.equals("f")){
                        //return IP in case of forward
                        address = new HostAddress(components[2]);
                    }
                } else {
                    logger.warning("Invalid response: " + response);
                }
            }
            
            if(status.equals("n")){
                //If no super peer for this category exists
                categories.put(cat, new NodeCategoryInfo(true));
               
                super_peers.put(cat, new HostAddress(server_ip, server_port));
                notifyOtherSuperPeers(cat, components[2]);
            } else if(status.equals("j")){
                //If super peer has been joined
                categories.put(cat, new NodeCategoryInfo(false));
                categories.get(cat).add_peer(new HostAddress(components[2]));
            }
        }
        
        logger.exiting(SONNode.class.getName(), "joinSON");
    }
    
    private void notifyOtherSuperPeers(String category, String sp_info){
        logger.entering(SONNode.class.getName(), "notifyOtherSuperPeers");
        
        String [] peer_info = sp_info.split("\\|");
        for(int i = 0; i < peer_info.length; i++){
            if(peer_info[i] != ""){
                //format category=ip:port;
                String [] super_peer_info = peer_info[i].split("=");
                String [] peer_address = super_peer_info[1].split(":");
                String ip = peer_address[0];
                int port = Integer.parseInt(peer_address[1]);
                
                if(!(ip.equals(server_ip) && port == server_port)){
                    String response = sendRequest(ip, port, "aq," + category + "," +
                                                    server_ip + ":" + String.valueOf(server_port));
                    if(response.equals("aa")){
                        if(super_peers.get(super_peer_info[0]) == null){
                            HostAddress address = new HostAddress(ip, port);
                            super_peers.put(super_peer_info[0], address);
                        }
                        logger.log(Level.INFO, "Super peer added {0}", super_peer_info[1]);
                    } else {
                        logger.log(Level.WARNING, "Super peer response not aa {0}{1}",
                                  new Object [] {super_peer_info[1], response});
                    }
                }
            }
        }
        
        logger.exiting(SONNode.class.getName(), "notifyOtherSuperPeers");
    }
    
    private String sendRequest(String ip, int port, String req){
        logger.entering(SONNode.class.getName(), "sendRequest");
        
        logger.log(Level.INFO, "Sending request {2} to: {0}:{1}", new Object[]{ip, port, req});
        String response = "";
        Socket socket = null;
        try{
            socket = new Socket(ip, port);
            InputStream inStream = socket.getInputStream();
            InputStreamReader isr = new InputStreamReader(inStream);
            BufferedReader reader = new BufferedReader(isr);
            OutputStream outStream = socket.getOutputStream();
            PrintWriter writer = new PrintWriter(outStream);
            
            //Send request
            writer.write(req + "\n");
            writer.flush();
            
            
            if(req.charAt(0) == 'd'){
                String number = "";
                char c;
                do{
                    c = (char) reader.read();
                    if(c != ',')
                        number += c;
                } while(c != ',');
                
                int chars = Integer.parseInt(number);
                for(int i = 0; i < chars; i++){
                    response += (char) reader.read();
                }
            }else{
                //Receive response
                response = reader.readLine();
            }

            writer.close();
            reader.close();
            socket.close();
        } catch (Exception ex){
            logger.log(Level.SEVERE, null, ex);
        }
        
        logger.log(Level.INFO, "Response received: {0}", response);
        logger.exiting(SONNode.class.getName(), "sendRequest");
        return response;
    }
    
    private void startServer(){
        logger.entering(SONNode.class.getName(), "startServer");
        ServerSocket sock = null;
        try{
            sock = new ServerSocket(server_port);
            while(true){
                Socket client_sock = sock.accept();
                logger.log(Level.INFO, "Client connected: {0}", client_sock);
                Thread t = new Thread(new RequestHandler(client_sock));
                t.start();
            }
        } catch (Exception ex){
            logger.log(Level.SEVERE, null, ex);
        }
        
        logger.exiting(SONNode.class.getName(), "startServer");
    }
    
    public class RequestHandler implements Runnable{
        private Socket client_sock;
        
        public RequestHandler(Socket _client_sock){
            client_sock = _client_sock;
        }
        
        public String resolveJoinRequest(String category, HostAddress address){
            logger.entering(RequestHandler.class.getName(), "resolveJoinRequest");
            String response_prefix = "ja,";
            NodeCategoryInfo cat_info = categories.get(category);
            if(cat_info == null){
                if(!super_peers.isEmpty()){
                    HostAddress sp_address = super_peers.get(category);
                    if(sp_address == null){
                        //return no super peer found of this category
                        String sp_info = "";
                        for(String key : super_peers.keySet()){
                            sp_info += (key + "=" + super_peers.get(key) + "|");
                        }
                        return response_prefix + "n," + sp_info;

                    } else {
                        return response_prefix + "f," + sp_address;
                    }
                } else {
                    for(String key: categories.keySet()){
                        NodeCategoryInfo n_info = categories.get(key);
                        if(!n_info.isSuperPeer()){
                            return response_prefix + "f," + n_info.getPeer(0);
                        }
                    }
                }
            } else {
                if(cat_info.isSuperPeer()){
                    categories.get(category).add_peer(address);
                    return response_prefix + "j," + server_ip + ":" + server_port;
                } else {
                    HostAddress sp_address = cat_info.getPeer(0);
                    return response_prefix + "f," + sp_address;
                }
            }
            return "failed";
        }
        
        /*private String resolveDownloadRequest(String file_path){           
            String content = "";
            try {
                content = new Scanner(new File(file_path)).useDelimiter("\\Z").next();
            } catch (FileNotFoundException ex) {
                Logger.getLogger(SONNode.class.getName()).log(Level.SEVERE, null, ex);
            }
            return content;
        }*/
        
        private String resolveQueryRequest(String category, String query){
            logger.info("Resolving query: '" + query + "' in category: '"+ category +"'");
            NodeCategoryInfo node_info = categories.get(category);
            String response_prefix = "qa,";
            //If not a node of given category
            if(node_info == null){
                if(super_peers.get(category) != null){
                    return response_prefix + "," + super_peers.get(category);
                } else {
                    return response_prefix + ",";
                }
            } else {
                String [] results = resolve_locally(query);
                String result = "";
                for(int i = 0; i < results.length; i++){
                    if(results[i].length() > 2)
                        result += (results[i] + "|");
                }
                
                if(node_info.isSuperPeer()){
                    String child_list = "";
                    for(int i = 0; i < node_info.totalPeers(); i++){
                        child_list += (node_info.getPeer(i) + "|");
                    }
                    return response_prefix + result + "," + child_list;
                } else {
                    return response_prefix + result + ",";
                }
            }
        }
        
        private String resolveChangeRequest(String category, String new_sp, String child_list, String sp_list){
            if(categories.containsKey(category)){
                if(new_sp == (server_ip + ":" + server_port)){
                    ArrayList<HostAddress> child_array = new ArrayList<HostAddress>();
                    String[] c_list = child_list.split("\\|");
                    for(String child: c_list){
                        child_array.add(new HostAddress(child));
                    }
                    categories.get(category).makeSuperPeer(child_array);
                    
                    
                    if(super_peers.size() == 0){
                        String [] sup_peer_addr = sp_list.split("\\|");
                        for(String sp_info : sup_peer_addr){
                            //format category=ip:port;
                            String [] super_peer_info = sp_info.split("=");
                            String [] peer_address = super_peer_info[1].split(":");
                            String ip = peer_address[0];
                            int port = Integer.parseInt(peer_address[1]);
                            super_peers.put(super_peer_info[0], new HostAddress(ip, port));                            
                        }
                    }
                } else if(new_sp.length() > 0){
                    categories.get(category).setSuperPeer(new HostAddress(new_sp));
                }
            }
            
            if(super_peers.size() > 0){
                if(new_sp.length() > 0)
                    super_peers.put(category, new HostAddress(new_sp));
                else if(super_peers.containsKey(category))
                    super_peers.remove(category);
            }
            
            return "ca";
        }
        
        public void run(){
            int t_id = (int) Thread.currentThread().getId();
            
            OutputStream out = null;
            InputStream in = null;
            try {   
                in = client_sock.getInputStream();
                out = client_sock.getOutputStream();
                PrintWriter writer = new PrintWriter(out);
                InputStreamReader stream_reader = new InputStreamReader(in);
                BufferedReader reader = new BufferedReader(stream_reader);

                //Get Request
                String request = reader.readLine();
                logger.log(Level.INFO, "{0} => Request received: {1}",
                                    new Object[]{t_id, request});
                
                String response = "";
                
                //Check request type
                if(request.charAt(0) == 'j'){
                    logger.log(Level.INFO, "{0} => Join request", t_id);
                    if(request.charAt(1) == 'q'){
                        //format is jq,category,ip:port
                        String [] components = request.split(",");
                        response = resolveJoinRequest(components[1], new HostAddress(components[2]));
                        
                    } else {
                        logger.log(Level.WARNING, "{0} => Invalid request: {1}",
                                        new Object[]{t_id, request});
                    }
                } else if(request.charAt(0) == 'q'){
                    logger.log(Level.INFO, "{0} => Query request", t_id);
                    if(request.charAt(1) == 'q'){
                        //query format qq,category,query
                        String [] components = request.split(",");
                        response = resolveQueryRequest(components[1], components[2]);
                    } else {
                        logger.log(Level.WARNING, "{0} => Invalid request: {1}", new Object[]{t_id, request});
                    }
                } else if(request.charAt(0) == 'a'){
                    if(request.charAt(1) == 'q'){
                        String [] tokens = request.split(",");
                        HostAddress addr = new HostAddress(tokens[2]);
                        super_peers.put(tokens[1], addr);
                        response = "aa";
                    }
                } else if(request.charAt(0) == 'd'){
                    String [] components = request.split(",");
                    //response = resolveDownloadRequest(components[1]);
                    response = response.length() + "," + response;
                } else if(request.charAt(0) == 'r'){
                    String [] components = request.split(",");
                    String category = components[1];
                    String to_remove = components[2];
                   
                    NodeCategoryInfo info = categories.get(category);
                    for(int i = 0; i < info.totalPeers(); i++){
                        if(info.getPeer(i).toString().equals(to_remove)){
                            categories.get(category).removePeer(i);
                            break;
                        }
                    }
                    response = "ra";
                    
                } else if(request.charAt(0) == 'c'){
                    String [] components = request.split(",");
                    String category = components[1];
                    String new_super_peer = components[2];
                    String child_list = components[3];
                    String sp_list = components[4];
                    response = resolveChangeRequest(category, new_super_peer, child_list, sp_list);
                }
                
                logger.log(Level.INFO, "{0} => Writing response: {1}", new Object[]{t_id, response});
                writer.write(response + "\n");
                writer.flush();
            } catch (IOException ex) {
                logger.log(Level.SEVERE, null, ex);
            }
        }
    }
    
    public class NetworkManager implements Runnable{
        private void removeCategory(String cat){
            logger.info("Removing category: " + cat);
            if(categories.containsKey(cat)){
                if(categories.get(cat).isSuperPeer()){
                    logger.info("Super peer");
                    ArrayList<HostAddress> children = new ArrayList<HostAddress>();
                    String descendent = "";
                    for(int i = 0; i < categories.get(cat).totalPeers(); i++){
                        if(i == 0){
                            descendent = categories.get(cat).getPeer(i).toString();
                        } else {
                            children.add(categories.get(cat).getPeer(i));
                        }
                    }
                    
                    String request = "cq";
                    request += "," + cat;
                    request += "," + descendent;
                    request += ",";
                    for(HostAddress child: children){
                        request += child.toString() + "|";
                    }
                    
                    request += ",";
                    for(String category: super_peers.keySet()){
                        request += category;
                        request += "=";
                        request += super_peers.get(category);
                        request += "|";
                    }
                    
                    logger.info("Notifying other super peers");
                    
                    HashSet<String> keys = new HashSet<String>(super_peers.keySet());
                    
                    //Notify other super peers
                    for(String _cat: keys){
                        if(!_cat.equals(cat)){
                            logger.info("Informing super peer of: " + _cat);
                            HostAddress addr = super_peers.get(_cat);
                            String response = sendRequest(addr.getIP(), addr.getPort(), request);
                            if(response.equals("ca")){
                                logger.info("Successful");
                            } else {
                                logger.info("Error");
                            }
                        }
                    }
                    
                    //Notify Children
                    for(int i = 0; i < categories.get(cat).totalPeers(); i++){
                        HostAddress child = categories.get(cat).getPeer(i);
                        logger.info("Informing children: " + child);
                        String response = sendRequest(child.getIP(), child.getPort(), request);
                        if(response.equals("ca")){
                            logger.info("Successful");
                        } else {
                            logger.info("Error");
                        }
                    }
                } else {
                    logger.info("Not super peer");
                    String request = "rq";
                    request += "," + cat;
                    request += "," + server_ip + ":" + server_port;
                    HostAddress super_peer = categories.get(cat).getPeer(0);
                    String response = sendRequest(super_peer.getIP(), super_peer.getPort(), request);
                    if(response.equals("ra")){
                        logger.info("Success");
                    } else {
                        logger.info("Failed");
                    }
                }
            }
        }
        
        public void run(){
            logger.info("Running Netowrk Manager");
            
            if(peer_ip == null){
                peer_ip = server_ip;
                peer_port = server_port;
            }
            while(true){
                try {
                    Thread.sleep(REFRESH_INTERVAL * 1000);
                } catch (InterruptedException ex) {
                    Logger.getLogger(SONNode.class.getName()).log(Level.SEVERE, null, ex);
                }

                logger.info("Re-adjusting Network");
                Set<String> current_categories;
                current_categories = categories.keySet();
                logger.info("Current categories: " + current_categories.toString());
                //String current_directory =  System.getProperty("user.dir");
                //String data_directory = Paths.get(current_directory, "data").toString();
                String exec_string = NODE_CLASSIFIER_SCRIPT;
                logger.info("Executing script: " + exec_string);
                Set<String> node_categories;
                
                Process p;
                try {
                    String [] commands = { shell, shell_param, exec_string};
                    p = Runtime.getRuntime().exec(commands);
                    p.waitFor();
                } catch (Exception e) {
                        e.printStackTrace();
                }

                logger.info("Script executed");

                String[] ncategories = load_categories(NODE_CATEGORIES_FILE);
                node_categories = new HashSet<String>();
                for(int i = 0; i < ncategories.length; i++)
                    if(!ncategories[i].isEmpty())
                        node_categories.add(ncategories[i]);
                
                logger.info("Classified as: " + node_categories.toString());
                
                logger.info("Removing old categories");
                Set<String> difference = new HashSet<String>(current_categories);
                difference.removeAll(node_categories);
                for(String cat: difference){
                    this.removeCategory(cat);
                }
                
                logger.info("Joining new categories");
                difference = new HashSet<String>(node_categories);
                difference.removeAll(current_categories);
                joinSON(new ArrayList<String>(difference));
                logger.info("SON Joined");
                
            }
        }        
    }
    
    public class QueryManager implements Runnable{
        //JTextField query_field;
        //JScrollPane  scroller;
        //JPanel result_outer_panel;
        //JFrame frame;
        ArrayList<String> results;
        boolean result_printed = false;
        
        private void showResults(String [] results){
            //logger.info("----------Printing results------------");
            for(int i = 0; i < results.length; i++){
                if(results[i].length() > 0){
                    logger.info(results[i]);
                    this.results.add(results[i]);
                    //JPanel result_panel = new JPanel();
                    //result_panel.setLayout(new BorderLayout());
                    //JButton down = new JButton("Download");
                    //down.addActionListener(new DownloadButtonListener(results[i]));
                    //result_panel.add(new Label(results[i]), BorderLayout.WEST);
                    //result_panel.add(down, BorderLayout.EAST);
                    //result_panel.setPreferredSize(new Dimension(500, 50));
                    //result_outer_panel.add(result_panel);
                    result_printed = true;
                }
            }
            //logger.info("---------------------------------------");
        }
        
        /*private void set_to_not_found(){
            result_outer_panel.removeAll();
            JPanel result_panel = new JPanel(new BorderLayout());
            result_panel.setPreferredSize(new Dimension(500, 40));
            result_panel.add(new Label("No results found"), BorderLayout.WEST);
            result_outer_panel.add(result_panel);
        }*/
        
        public void run(){
            logger.info("Running query manager");
            while(true){
                ServerSocket serverSocket = null; 
                try { 
                    serverSocket = new ServerSocket(60000); 
                } catch (IOException e) { 
                 System.err.println("Could not listen on port: 60000."); 
                 System.exit(1);
                } 

                Socket clientSocket = null; 
                logger.info("Waiting for connection.....");

                try { 
                     clientSocket = serverSocket.accept(); 
                    } 
                  catch (IOException e) 
                { 
                    System.err.println("Accept failed."); 
                    System.exit(1); 
                } 
                
                try {
                    PrintWriter out = new PrintWriter(clientSocket.getOutputStream(), true);
                    BufferedReader in = new BufferedReader(new InputStreamReader( clientSocket.getInputStream())); 

                    String query = ""; 
                    try {
                        query = in.readLine();
                    } catch (IOException ex) {
                        Logger.getLogger(SONNode.class.getName()).log(Level.SEVERE, null, ex);
                    }
                    String exec_string = QUERY_CLASSIFIER_SCRIPT + " \"" + query + "\"";
                    String [] commands = { shell, shell_param, exec_string};
                    logger.info("Executing script: " + exec_string);

                    Process p;
                    try {
                        p = Runtime.getRuntime().exec(commands);
                        p.waitFor();
                    } catch (Exception e) {
                            e.printStackTrace();
                    }

                    logger.info("Script executed");

                    String[] categories = load_categories(QUERY_CATEGORIES_FILE);
                    String temp = "";
                    for(String cat: categories){
                        temp += cat + " ";
                    }
                    logger.info("Classified as: " + temp);

                    this.results = new ArrayList<String>();
                    int count = 0;
                    for(int i = 0; i < categories.length; i++){
                        if(!categories[i].isEmpty()){
                            result_printed = false;
                            resolve_query(query, categories[i]);
                            if(result_printed == false){
                                count++;
                            }
                        }
                    }

                    String response = "";
                    for(int i = 0; i < results.size(); i++){
                        response += results.get(i) + "|";
                    }

                    response += "\n";


                    out.println(response);
                    out.close(); 
                    in.close();
                    clientSocket.close();
                    serverSocket.close();

                } catch (IOException ex) {
                    Logger.getLogger(SONNode.class.getName()).log(Level.SEVERE, null, ex);
                }
                
            }
            /*frame = new JFrame("SON Search(" + server_ip + ":" + String.valueOf(server_port) + ")");
            JPanel search_panel = new JPanel();
            result_outer_panel = new JPanel();
            result_outer_panel.setLayout(new BoxLayout(result_outer_panel, BoxLayout.Y_AXIS));
            set_to_not_found();

            scroller = new JScrollPane(result_outer_panel);
            scroller.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS);
            scroller.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
            scroller.setPreferredSize(new Dimension(550, 300));

            query_field = new JTextField(40);
            JButton sendButton = new JButton("Search");
            sendButton.addActionListener(new SendButtonListener());
            search_panel.setLayout(new FlowLayout());
            search_panel.add(query_field);
            search_panel.add(sendButton);
        

            frame.getContentPane().add(BorderLayout.NORTH, search_panel);
            frame.getContentPane().add(BorderLayout.SOUTH, scroller);
            frame.pack();
            frame.setVisible(true);*/
        }
        
        private void resolve_query(String query, String category){
            NodeCategoryInfo node_info = categories.get(category);
            String request = "qq,"+category+","+query;
            ArrayList<HostAddress> next_hop_addr = new ArrayList<HostAddress>();
                
            //if node does not belong to this category
            if(node_info == null){
                //if super peer of category is stored locally
                if(super_peers.get(category) != null){
                    next_hop_addr.add(super_peers.get(category));
                } else if(super_peers.isEmpty()){
                    //if not super peer of any category
                    next_hop_addr.add(categories.entrySet().iterator().next().getValue().getPeer(0));
                } else {
                    logger.info("No node found of category " + category);
                }
            } else {
                //locally resolve query here
                String [] results = resolve_locally(query);
                showResults(results);

                if(node_info.isSuperPeer()){
                    //send to all chlidren
                    for(int i = 0; i < node_info.totalPeers(); i++)
                        next_hop_addr.add(node_info.getPeer(i));
                } else {
                    //send to super peer of category
                    next_hop_addr.add(node_info.getPeer(0));
                }
            }
            
            Queue<HostAddress> ip_queue = new LinkedList<HostAddress>(next_hop_addr);
            while(!ip_queue.isEmpty()){
                HostAddress address = ip_queue.remove();
                if(!(address.getIP().equals(server_ip) && address.getPort() == server_port)){
                    String response = sendRequest(address.getIP(), address.getPort(), request);
                    if(response.charAt(0) == 'q' && response.charAt(1) == 'a'){
                        String [] res_components = response.split(",");
                        if(res_components.length > 1){
                            String results = res_components[1];
                            showResults(results.split("\\|"));
                            if(res_components.length > 2){
                                String [] forward_ip = res_components[2].split("\\|");
                                for(int i = 0; i < forward_ip.length; i++){
                                    ip_queue.add(new HostAddress(forward_ip[i]));
                                }
                            }
                        }
                    } else {
                        logger.warning("Invalid response received for request: " + request);
                    }
                }
            }
        }
        
        /*public class DownloadButtonListener implements ActionListener{
            private String link;
            
            DownloadButtonListener(String _link){
                link = _link;
            }
            public void actionPerformed(ActionEvent ev){
                FileDownloader downloader = new FileDownloader();
                String response = downloader.getFile(link);
                String file_name = link.substring(link.lastIndexOf('/'));
                String path = "downloads/" + file_name;
                PrintWriter out;
                try {
                    out = new PrintWriter(path);
                    out.write(response);
                    out.close();
                } catch (FileNotFoundException ex) {
                    Logger.getLogger(SONNode.class.getName()).log(Level.SEVERE, null, ex);
                }
                JOptionPane.showMessageDialog(frame, "Download Complete");
            }
        }
        
        public class FileDownloader{
            public String getFile(String link){
                logger.info("Link: " + link);
                int index = link.indexOf('/');
                HostAddress address = new HostAddress(link.substring(0, index));
                logger.info("Address: " + address);
                String file_path = link.substring(index + 1);
                String request = "dq,"+file_path;
                String response = sendRequest(address.getIP(), address.getPort(), request);
                return response;
            }
        }
        
        public class SendButtonListener implements Runnable{
            public void actionPerformed(ActionEvent ev){
                String input = query_field.getText();
                
                String query = input;
                String exec_string = QUERY_CLASSIFIER_SCRIPT + "\"" + query + "\"";
                String [] commands = { shell, shell_param, exec_string};
                logger.info("Executing script: " + exec_string);
                
                Process p;
                try {
                    p = Runtime.getRuntime().exec(commands);
                    p.waitFor();
                } catch (Exception e) {
                        e.printStackTrace();
                }

                logger.info("Script executed");
               
                String[] categories = load_categories(QUERY_CATEGORIES_FILE);
                String temp = "";
                for(String cat: categories){
                    temp += cat + " ";
                }
                logger.info("Classified as: " + temp);
                
                result_outer_panel.removeAll();
                int count = 0;
                for(int i = 0; i < categories.length; i++){
                    if(!categories[i].isEmpty()){
                        result_printed = false;
                        Label cat_label = new Label("------------ Category: " + categories[i] + " -------------\n");
                        result_outer_panel.add(cat_label);
                        resolve_query(query, categories[i]);

                        if(result_printed == false){
                            result_outer_panel.remove(cat_label);
                            count++;
                        }
                    }
                }
                
                if(count == categories.length){
                    set_to_not_found();
                }
                
                result_outer_panel.revalidate();
                result_outer_panel.repaint();
                frame.revalidate();
                frame.repaint();
            }
	}*/
    }
    
    public class NodeCategoryInfo{
        private boolean is_super_peer;
        private ArrayList<HostAddress> peer_list;
        
        public NodeCategoryInfo(boolean _is_super_peer){
            is_super_peer = _is_super_peer;
            peer_list = new ArrayList<HostAddress>();
        }
        
        public boolean isSuperPeer(){
            return is_super_peer;
        }
        
        public void makeSuperPeer(ArrayList<HostAddress> child_list){
            is_super_peer = true;
            peer_list = child_list;
        }
        
        public boolean removePeer(int index){
            if(index >= 0 && index < peer_list.size()){
                peer_list.remove(index);
                return true;
            }
            return false;
        }
        
        public boolean setSuperPeer(HostAddress addr){
            if(!is_super_peer){
                peer_list.clear();
                peer_list.add(addr);
                return true;
            } else {
                return false;
            }
        }
        
        public HostAddress getPeer(int index){
            return peer_list.get(index);
        }
        
        public int totalPeers(){
            return peer_list.size();
        }
        
        public boolean add_peer(String ip, int port){
            return peer_list.add(new HostAddress(ip, port));
        }
        
        public boolean add_peer(HostAddress address){
            return peer_list.add(address);
        }
    }
    
    public class HostAddress{
        private String ip;
        private int port;
        
        public HostAddress(String _ip, int _port){
            ip = _ip;
            port = _port;
        }
        
        public HostAddress(String addr){
            String [] components = addr.split(":");
            ip = components[0];
            port = Integer.parseInt(components[1]);
        }
        
        @Override
        public String toString(){
            return ip + ":" + String.valueOf(port);
        }
        
        public String getIP(){
            return ip;
        }
        
        public int getPort(){
            return port;
        }
    }
}
