package Record;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Random;

public class StudentRecord extends Records implements Serializable {

    private ArrayList<String> coursesRegistered=new ArrayList<>();
    private String status;
    private String statusDate;

    public ArrayList<String> getCoursesRegistered() {
        return coursesRegistered;
    }

    public synchronized void setCoursesRegistered(ArrayList<String> coursesRegistered) {
        this.coursesRegistered = coursesRegistered;
    }

    public String getStatus() {
        return status;
    }

    @Override
    public String genRecordID(){
        String recordId = "SR";
        String chars = "1234567890";
        while (recordId.length()<7){
            recordId+=new Random().nextInt(9);
        }
        return recordId;
    }

    @Override
    public void regenRecordID() {
        this.recordID = genRecordID();
    }

    public synchronized void setStatus(String status) {
        this.status = status;
    }

    public String getStatusDate() {
        return statusDate;
    }

    public synchronized void setStatusDate(String statusDate) {
        this.statusDate = statusDate;
    }

    public StudentRecord(String firstName, String lastName, String[] coursesRegistered, String status, String statusDate) {
        super(firstName, lastName);
        this.recordID = genRecordID();
        for (String s :
                coursesRegistered) {
            this.coursesRegistered.add(s);
        }
        this.status = status;
        this.statusDate = statusDate;
    }
}
