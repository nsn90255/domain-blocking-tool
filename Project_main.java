/*
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <https://www.gnu.org/licenses/>.
 */

import java.util.Scanner;
import java.io.File;
import java.io.FileWriter;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.BufferedReader;
import java.io.IOException;
import java.time.format.DateTimeParseException;
import java.time.*;
import java.nio.file.*;
import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class Project_main {
    
	public static void main(String[] args) {
		File blocklist = new File("/etc/blocklist.conf");
		// check if blocklist exists
		if (!blocklist.exists()){
			createBlocklist();
		}
		Scanner sc = new Scanner(System.in);
		if (args.length == 0){
			help();
			return;
		} else if ((args[0].equals("-u")) && (args.length < 2)) {
			unblock(blocklist);
		} else if ((args[0].equals("-b")) && (args.length < 2)) {
			block(blocklist);
		} else if (args[0].equals("-a")) {
			addBannedDomain(blocklist, sc, args);
		} else if (args[0].equals("-r")) {
			removeBannedDomain(blocklist, sc, args);
		} else if (args[0].equals("-s")) {
			checkStatus();
		} else if (args[0].equals("-v")) {
			version();
		} else if (args[0].equals("-i")) {
			ignore(blocklist);
		} else if (args[0].equals("-d")) {
			copyDefaults(blocklist);
		} else if (args[0].equals("-g")) {
			graphical(blocklist);
		} else {
			help();
		}
	}
	public static void unblock(File blocklist){
		// if not blocking do nothing
		if (!checkBlocking()) {
			System.out.println("Not blocking right now. Nothing to do.");
			return;
		}
		File hosts = new File("/etc/hosts");
		File backup = new File("/etc/hosts.bkp");
		// replace hosts file with backup of the original one
		hosts.delete();
		backup.renameTo(hosts);
		System.out.println("The blocklist is down.");
	}
	public static void block(File blocklist){
		// if already blocking stop
		if (checkBlocking()) {
			System.out.println("Already blocking.");
			return;
		}
		File backup = new File("/etc/hosts.bkp");
		try {
			// create new backup file
			backup.createNewFile();
			System.out.println("backup of hosts file created");
		} catch (IOException e) {
			System.out.println("Error : " + e.getMessage());
		}
		// check if hosts exists
		File doesHostsExist = new File("/etc/hosts");
		if (!doesHostsExist.exists()){
			// if not, tell user
			System.out.println("The hosts file doesn't exist, you should fix that.");
			return;
		}
		// hosts file
		File hosts = new File("/etc/hosts");
		// write the hosts file to a backup
		try {
			// Create writer & reader
			BufferedWriter wr = new BufferedWriter(new FileWriter(backup));
			BufferedReader rd = new BufferedReader(new FileReader(hosts));
			// var for the current line
			String currentLine;
			while((currentLine = rd.readLine()) !=null) {
				wr.write(currentLine + "\n");
			}
		wr.close();
		rd.close();
		} catch (IOException e) {
			System.out.println("Error : " + e.getMessage());
		}
		// copy /etc/blocklist.conf to /etc/hosts
		try {
			//create reader & writer
			BufferedWriter wr = new BufferedWriter(new FileWriter(hosts, true));
			BufferedReader rd = new BufferedReader(new FileReader(blocklist));
			// var for the current line
			String currentLine;
			wr.write("127.0.0.1 ");
			while((currentLine = rd.readLine()) !=null) {
				char notWrite[] = { '1', '2', '3', '4', '5', '6', '7', '[', '#' };
				boolean doWeWrite = true;
				for (int j = 0; j < 9; j++) {
					if (currentLine.charAt(0) == notWrite[j]) {
						doWeWrite = false;
					}
				}
				if (doWeWrite) {
					wr.write(currentLine + " ");
				}
			}
		wr.close();
		rd.close();
		} catch (IOException e) {
			System.out.println("Error : " + e.getMessage());
		}
		System.out.println("The blocklist is up.");
	}
	public static void addBannedDomain(File blocklist, Scanner sc, String[] args){
		// if blocking prevent user from editing config
		if (checkBlocking()) {
			System.out.println("Blocking right now. Unblock and try again.");
			return;
		}
		// if no domain is specified print help and return to main
		if (args.length < 2) {
			help();
			return;
		}
		try{
			BufferedWriter wr = new BufferedWriter(new FileWriter(blocklist, true));
			BufferedReader rd = new BufferedReader(new FileReader(blocklist));
			// loop through every argument starting from the second one
			for (int i = 1; i < args.length; i++){
				// assign the argument to toBlock
				String toBlock = args[i];
				// controls whether to write or not
				boolean write = true;
				// variable to store the line read by the buffered reader
				String currentLine;
				while ((currentLine = rd.readLine()) != null) {
					// remove whitespaces
					String trimmedLine = currentLine.trim();
					if (trimmedLine.equals(args[i])) {
						write = false;
						break;
					}
				}
				if (write) {
					wr.write(toBlock + "\n");
					System.out.println(toBlock + " has been added to the blocklist.");
				} else {
					System.out.println(toBlock + " is already in the blocklist.");
				}
			}
			wr.close();
			rd.close();
		} catch (IOException e){
			System.out.println("Error : " + e.getMessage());
		}
	}
	public static void removeBannedDomain(File blocklist, Scanner sc, String[] args){
		// if blocking prevent user from editing config
		if (checkBlocking()) {
			System.out.println("Blocking right now. Unblock and try again.");
			return;
		}
		// if no arguments stop here
		if (args.length < 2) {
			help();
			return;
		}
		try{
			File tempFile = new File("/tmp/blocklist.tmp");
			BufferedReader rd = new BufferedReader(new FileReader(blocklist));
			BufferedWriter wr = new BufferedWriter(new FileWriter(tempFile));
			boolean domainRemoved = false;
			// create the string for the current line that the br is reading
			String currentLine;
			// for each line
			while((currentLine = rd.readLine()) !=null) {
				// remove whitespaces
				String trimmedLine = currentLine.trim();
				// boolean value to allow writing gets reset for each line	
				boolean shouldWrite = true;
				// loop through every argument and check if they match the current line
				for (int i = 1;	i < args.length; i++) {
					// give the value of the argument to the string toUnblock
					String toUnblock = args[i].trim();// ;
					// if the line without the whitespace is the same as the argument
					if (trimmedLine.equals(toUnblock)) {
						System.out.println(toUnblock + " has been removed from the blocklist.");
						// do not write it
						shouldWrite = false;
						// make sure the blocklist is swapped out by the temp one if one domain is written
						domainRemoved = true;
						break;
					}
				}	
				// write that line to the temp file unless the line matches an argument
				if (shouldWrite) {
					wr.write(currentLine + "\n");
				}
			}
			wr.close();
			rd.close();
			// rename the temp file to blocklist if one domain or more is written
			Path blocklistPath = Paths.get("/etc/blocklist.conf");
			Path tempFilePath = Paths.get("/tmp/blocklist.tmp");
			if (domainRemoved) {
				Files.copy(tempFilePath, blocklistPath, StandardCopyOption.REPLACE_EXISTING);
				System.out.println("Blocklist updated");
			} else {
			    // If no domain was removed, delete the temp file
			    tempFile.delete();
			    System.out.println("No domain was removed from the blocklist");
			}
		} catch (IOException e) {
			System.out.println("Error : " + e.getMessage());
		}
	}
	public static boolean checkBlocking() {
		// check if a backup of the original hosts file exists (this should only be the case if it's blocking)
		File doesBackupExist = new File("/etc/hosts.bkp");
		if (doesBackupExist.exists()) {
			return true;
		} else {
			return false;
		}
	}
	public static void help(){
		System.out.println("Usage : project [command] \033[3mdomain\033[0m");
		System.out.println("  -u\t\t\t\tUnblock domains.");
		System.out.println("  -b\t\t\t\tBlock domains.");
		System.out.println("  -a\t\t\t\tAdd a domain to block.");
		System.out.println("  -r\t\t\t\tRemove a blocked domain.");
		System.out.println("  -s\t\t\t\tCheck if running or not.");
		System.out.println("  -v\t\t\t\tPrint version information.");
		System.out.println("  -i\t\t\t\tIgnore blocking.");
	}
	public static void createBlocklist() {
		// create the blocklist
		try {
			File blocklist = new File("/etc/blocklist.conf");
			BufferedWriter wr = new BufferedWriter(new FileWriter(blocklist));
			wr.write("# write your days to block here\n");
			wr.write("[Days]\n");
			wr.write("1=0000-0000\n");
			wr.write("2=0000-0000\n");
			wr.write("3=0000-0000\n");
			wr.write("4=0000-0000\n");
			wr.write("5=0000-0000\n");
			wr.write("6=0000-0000\n");
			wr.write("7=0000-0000\n");
			wr.write("# write your websites to block here\n");
			wr.write("[Domains]\n");
			wr.write("www.youtube.com\n");
			wr.write("# www.instagram.com\n");
			wr.write("# www.tiktok.com\n");
			wr.write("# www.facebook.com\n");
			wr.close();
		} catch (IOException e) {
			System.out.println("Error : " + e.getMessage());
		}
		System.out.println("Blocklist created with default blocked domains.");
		return;
	}
	public static void checkStatus() {
		// print the status
		String not = "";
		if (!checkBlocking()) {
			not = "Not ";
		}	
		System.out.println("Status : [" + not + "Blocking]");
	}
	public static void version() {
		// print the version
		System.out.println("dbt 1.0.0");
	}
	public static boolean checkIfCanIgnore() {
		// get last line from log; store in lastLine
		String lastLine = null;
		try {
			File log = new File("/var/log/blockdaemon.log");
			BufferedReader br = new BufferedReader(new FileReader(log));
			String lineRead = null, tmp;
			while ((tmp = br.readLine()) != null) {
				lineRead = tmp;
			}
			lastLine = lineRead;
			br.close();
		} catch (IOException e) {
			System.out.println("Error : " + e.getMessage());
		}
		// if the log is empty, allow
		if (lastLine == null) {
			return true;
		}
		// extract time string from last line
		String separateDate[] = lastLine.split(" @ ");
		boolean returnTrue = false;
		// check if more than 24h
		try {
			LocalDateTime lastLog = LocalDateTime.parse(separateDate[1]);
			LocalDateTime timeNow = LocalDateTime.now();
			Duration duration = Duration.between(lastLog, timeNow);
			if (duration.toMinutes() > 1440) {
				returnTrue = true;
			}
		} catch (DateTimeParseException e) {
			System.out.println("Error : " + e.getMessage());
			return false;
		}
		if (returnTrue) {
			return true;
		} else {
			return false;
		}
	}
	public static boolean writeLog() {
		LocalDateTime dateRightNow = LocalDateTime.now();
		boolean returnTrue = false;
		try {
			File log = new File("/var/log/blockdaemon.log");
			BufferedWriter wr = new BufferedWriter(new FileWriter(log, true));
			wr.write("Ignoring @ " + dateRightNow + "\n");
			wr.close();
			returnTrue = true;
		} catch (IOException e) {
			System.out.println("Error : " + e.getMessage());
		}
		if (returnTrue) {
			return true;
		} else {
			return false;
		}
	}
	public static void ignore(File blocklist) {
		if (!checkBlocking()) {
			System.out.println("Not blocking right now. Nothing to do.");
			return;
		}
		if (checkIfCanIgnore() && writeLog()) {
			unblock(blocklist);
			System.out.println("Successfully written to log and unblocked");
		} else {
			System.out.println("you cannot do that right now");
		}
	}
	public static void copyDefaults(File blocklist) {
		// file with defaults to be copied over
		File defaults = new File("/var/lib/dbt/defaults.conf");
		try{
			BufferedWriter wr = new BufferedWriter(new FileWriter(blocklist, true));
			BufferedReader rd = new BufferedReader(new FileReader(blocklist));
			BufferedReader rddef = new BufferedReader(new FileReader(defaults));
			// number of line in defaults to be used to create array
			int numberOfLinesInDefaults = 0;
			String defaultsLine;
			while ((defaultsLine = rddef.readLine()) != null) {
				numberOfLinesInDefaults++;
			}
			// reset reader
			rddef.close();
			BufferedReader rddef2 = new BufferedReader(new FileReader(defaults));
			// array to store the extracted lines
			String[] defaultDomainsToAdd = new String[numberOfLinesInDefaults];
			// line read from defaults to be stored in the array
			int lineToAdd = 0;
			while ((defaultsLine = rddef2.readLine()) != null) {
				// remove whitespaces
				String trimmedDefaults = defaultsLine.trim();
				// create a new array element with each new line
				defaultDomainsToAdd[lineToAdd] = trimmedDefaults;
				lineToAdd++;
			}
			// loop through every array item
			for (int i = 0; i < defaultDomainsToAdd.length; i++){
				// assign the argument to toBlock
				String toBlock = defaultDomainsToAdd[i];
				// controls whether to write or not
				boolean write = true;
				String currentLine;
				// check if each line in the blocklist equals the array item
				while ((currentLine = rd.readLine()) != null) {
					// remove whitespaces
					String trimmedLine = currentLine.trim();
					// if it does equal the item, flag it and stop the loop there
					if (trimmedLine.equals(defaultDomainsToAdd[i])) {
						write = false;
						break;
					}
				}
				// if we can write the array item, we do so
				if (write) {
					// add code here
					wr.write(toBlock + "\n");
					System.out.println(toBlock + " has been added to the blocklist.");
				} else {
					System.out.println(toBlock + " is already in the blocklist.");
				}
			}
			wr.close();
			rd.close();
			rddef2.close();
		} catch (IOException e){
			System.out.println("Error : " + e.getMessage());
		}
	}
	public static void graphical(File blocklist) {
        // Create a JFrame (the main window)
        JFrame frame = new JFrame("Pause button");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(600, 400);

        // Create a JButton
        JButton button = new JButton("Click To Pause Website Blockling for 1 hour.(Once per day.)");

        // Add an ActionListener to the button
        button.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                // Call the ignore() method when the button is clicked
                ignore(blocklist);
            }
        });

        // Add the button to the frame
        frame.getContentPane().add(button);

        // Set the frame visible
        frame.setVisible(true);
    }


}
