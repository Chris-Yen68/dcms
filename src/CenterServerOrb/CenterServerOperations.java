package CenterServerOrb;


import org.omg.CosNaming.NamingContextPackage.CannotProceed;
import org.omg.CosNaming.NamingContextPackage.InvalidName;

/**
* CenterServerOrb/CenterServerOperations.java .
* Generated by the IDL-to-Java compiler (portable), version "3.2"
* from CenterServerOrb.idl
* Saturday, June 9, 2018 12:05:53 o'clock PM EDT
*/

public interface CenterServerOperations 
{
  String createTRecord (String managerId, String firstName, String lastName, String address, String phone, String specialization, String location);
  String createSRecord (String managerId, String firstName, String lastName, String[] courseRegistered, String status, String statusDate);
  String getRecordCounts (String managerId);
  String editRecord (String managerId, String recordID, String fieldName, String newValue) throws CenterServerOrb.CenterServerPackage.except;
  String transferRecord (String managerId, String recordID, String remoteCenterServerName) throws CenterServerOrb.CenterServerPackage.except, CannotProceed, InvalidName;
  void shutdown ();
} // interface CenterServerOperations
