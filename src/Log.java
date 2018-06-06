import java.io.*;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

public class Log {

    public static String getCurrentTime() {
        Date date = new Date();
        DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
        String formattedDate = dateFormat.format(date);
        return formattedDate;
    }

    public static void log(String time, String managerId, String operation, String result) {
        File file = new File("./log");
        if (!file.exists()) {
            file.mkdir();
        }
        File inforClientFile = new File("./log/" + managerId + ".txt");
        File inforServerFile = new File("./log/" + managerId.substring(0, 3) + ".txt");

        BufferedWriter clientBufferedWriter = null;
        BufferedWriter serverBufferedWriter = null;
        String clientInformation = time + " | " + operation + " | " + managerId.substring(0, 3) + "Server" + " | " + result + "\n";
        String serverInformation = time + " | " + managerId + " | " + operation + " | " + result + "\n";
        if (inforServerFile.exists()) {
            try {
                clientBufferedWriter = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(inforServerFile,true)));
                clientBufferedWriter.write(serverInformation);
                clientBufferedWriter.flush();
                clientBufferedWriter.close();
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
        } else {
            try {
                inforServerFile.createNewFile();
                clientBufferedWriter = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(inforServerFile,true)));
                clientBufferedWriter.write("time | managerId | operation | result\n"+serverInformation);
                clientBufferedWriter.flush();
                clientBufferedWriter.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        if (inforClientFile.exists()) {
            try {
                clientBufferedWriter = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(inforClientFile,true)));
                clientBufferedWriter.write(clientInformation);
                clientBufferedWriter.flush();
                clientBufferedWriter.close();
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }

        } else {
            try {
                inforClientFile.createNewFile();
                clientBufferedWriter = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(inforClientFile,true)));
                clientBufferedWriter.write("time | operation | location | result\n"+clientInformation);
                clientBufferedWriter.flush();
                clientBufferedWriter.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

}
