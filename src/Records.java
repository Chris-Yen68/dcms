import java.io.*;
import java.util.ArrayList;
import java.util.Random;

public  class  Records implements Serializable {
    private String firstName;
    private String lastName;
    protected String recordID;

    public Records(String firstName, String lastName) {
        this.firstName = firstName;
        this.lastName = lastName;

    }

    public String genRecordID(){
        String recordId = "";
        String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZ1234567890";
        while (recordId.length()<6){
            recordId+=chars.charAt(new Random().nextInt(chars.length()));
        }
        return recordId;
    }

    public void regenRecordID() {
        this.recordID = genRecordID();
    }
    public static Object deepCopy(Object o) throws IOException, ClassNotFoundException {
        ByteArrayOutputStream bo = new ByteArrayOutputStream();
        ObjectOutputStream oo = new ObjectOutputStream(bo);
        oo.writeObject(o);
        ByteArrayInputStream bi = new ByteArrayInputStream(bo.toByteArray());
        ObjectInputStream oi = new ObjectInputStream(bi);
        return oi.readObject();
    }

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    public String getRecordID() {
        return recordID;
    }



}
