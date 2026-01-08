import java.io.*;
import java.nio.file.*;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.time.format.DateTimeParseException;


public class BlockDaemon {

    private static final DateTimeFormatter LOG_DATE_FORMAT = DateTimeFormatter.ISO_DATE_TIME;
    private static final String BLOCKLIST_FILE = "/etc/blocklist.conf";
    private static final String LOG_FILE = "/var/log/blockdaemon.log";

    public static void main(String[] args) {
	String block = "sudo dbt -b";
	String unblock = "sudo dbt -u";
	ProcessBuilder blockCommand = new ProcessBuilder();
        blockCommand.command("bash", "-c", block);
	ProcessBuilder unblockCommand = new ProcessBuilder();
        unblockCommand.command("bash", "-c", unblock);

        try {
            Map<Integer, String> blockDays = readBlocklistConfig();

            List<LocalDateTime> ignoredTimes = readLogFile();

            LocalDateTime currentTime = LocalDateTime.now();

            for (LocalDateTime ignoreTime : ignoredTimes) {
                if (Duration.between(ignoreTime, currentTime).toHours() < 1) {
                    System.out.println("Unblock for the remainder of the time (until " +
                            ignoreTime.plusHours(1).format(DateTimeFormatter.ISO_LOCAL_TIME) + ")");
                    System.out.println("Running: sudo dbt -u");
		    Process unblockProcess = unblockCommand.start();
		    unblockProcess.waitFor();
                    return;
                }
            }

            int dayOfWeek = currentTime.getDayOfWeek().getValue();
            String blockSetting = blockDays.getOrDefault(dayOfWeek, "0000-0000");

            if (blockSetting.equals("all") || blockSetting.equals("0000-0000")) {
                System.out.println("Running: sudo dbt -b");
		Process blockProcess = blockCommand.start();
		blockProcess.waitFor();
            } else {
                System.out.println("No blocking required.");
            }

        } catch (Throwable t) {
            System.err.println("Error reading files: " + t.getMessage());
        }
    }

    private static Map<Integer, String> readBlocklistConfig() throws IOException {
        Map<Integer, String> blockDays = new HashMap<>();
        List<String> lines = Files.readAllLines(Paths.get(BLOCKLIST_FILE));

        boolean inDaysSection = false;
        for (String line : lines) {
            line = line.trim();
            if (line.equals("[Days]")) {
                inDaysSection = true;
                continue;
            }

            if (inDaysSection) {
                if (line.equals("[Domains]")) {
                    break;
                }

                if (!line.startsWith("#") && line.contains("=")) {
                    String[] parts = line.split("=");
                    try {
                        int day = Integer.parseInt(parts[0].trim());
                        String timeBlock = parts[1].trim();
                        blockDays.put(day, timeBlock);
                    } catch (NumberFormatException e) {
                        System.err.println("Invalid day format in config: " + parts[0]);
                    }
                }
            }
        }

        return blockDays;
    }

    private static List<LocalDateTime> readLogFile() throws IOException {
        List<LocalDateTime> ignoredTimes = new ArrayList<>();
        List<String> lines = Files.readAllLines(Paths.get(LOG_FILE));

        for (String line : lines) {
            if (line.startsWith("Ignoring @")) {
                String timestamp = line.split("Ignoring @")[1].trim();
                try {
                    LocalDateTime ignoreTime = LocalDateTime.parse(timestamp, LOG_DATE_FORMAT);
                    ignoredTimes.add(ignoreTime);
                } catch (DateTimeParseException e) {
                    System.err.println("Invalid timestamp format in log: " + timestamp);
                }
            }
        }

        return ignoredTimes;
    }
}

