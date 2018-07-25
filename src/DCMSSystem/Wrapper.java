package DCMSSystem;

import DCMSSystem.Record.Records;

import java.io.Serializable;
import java.util.HashMap;

public class Wrapper implements Serializable {
    private String commandName;
    private Records records;
    private String managerID;
    private HashMap<String,String> editInfor;

    public String getCommandName() {
        return commandName;
    }

    public void setCommandName(String commandName) {
        this.commandName = commandName;
    }

    public Records getRecords() {
        return records;
    }

    public void setRecords(Records records) {
        this.records = records;
    }

    public Wrapper(String commandName, Records records) {

        this.commandName = commandName;
        this.records = records;
    }

    public Wrapper(String commandName, HashMap<String, String> editInfor) {
        this.commandName = commandName;
        this.editInfor = editInfor;
    }

    public HashMap<String, String> getEditInfor() {
        return editInfor;
    }

    public void setEditInfor(HashMap<String, String> editInfor) {
        this.editInfor = editInfor;
    }

    public Wrapper(String commandName, String managerID,Records records) {
        this.commandName = commandName;
        this.records = records;
        this.managerID = managerID;
    }

    public String getManagerID() {

        return managerID;
    }

    public void setManagerID(String managerID) {
        this.managerID = managerID;
    }
}
