import java.io.IOException;
import java.util.LinkedList;

public class Test1 {
    public static void main(String[] args) throws IOException, ClassNotFoundException {
        String request = "get data!";

        TeacherRecord teacherRecord = new TeacherRecord("Li","Zhen","1950 Lincoln","4389274666","cs","Montreal");
        Records record = (TeacherRecord)Records.deepCopy(teacherRecord);
        System.out.println(record.getLastName());
        byte [] infor = ByteUtility.toByteArray(request);
        byte [] infor1 = ByteUtility.toByteArray(teacherRecord);
        LinkedList<byte[]> linkedList = new LinkedList<>();
        linkedList.add(infor);
        linkedList.add(infor1);
        for (byte[] bytes : linkedList) {
            Object obj = ByteUtility.toObject(bytes);
            if (obj instanceof  String){
                System.out.println((String)ByteUtility.toObject(infor));
            }else if (ByteUtility.toObject(bytes) instanceof Records){
                System.out.println(((Records) ByteUtility.toObject(bytes)).getLastName());
            }
        }

    }
}
