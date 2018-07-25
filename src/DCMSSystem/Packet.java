package DCMSSystem;

import java.io.Serializable;

public class Packet implements Serializable {
    private int checkSum;
    private int seqNUmber;
    private byte[] content;
    private String sender;

    public void setSeqNUmber(int seqNUmber) {
        this.seqNUmber = seqNUmber;
    }

    public void setContent(byte[] content) {
        this.content = content;
    }

    public String getSender() {
        return sender;
    }

    public void setSender(String sender) {
        this.sender = sender;
    }

    public Packet(int checkSum, int seqNUmber, byte[] content, String sender) {

        this.checkSum = checkSum;
        this.seqNUmber = seqNUmber;
        this.content = content;
        this.sender = sender;
    }

    public int getCheckSum() {
        return checkSum;
    }

    public void setCheckSum(int checkSum) {
        this.checkSum = checkSum;
    }

    public int getSeqNUmber() {
        return seqNUmber;
    }

    public byte[] getContent() {
        return content;
    }
}
