package CenterServerOrb;


/**
* CenterServerOrb/CenterServerOperations.java .
* 由IDL-to-Java 编译器 (可移植), 版本 "3.2"生成
* 从CenterServerOrb.idl
* 2018年6月11日 星期一 下午05时57分17秒 EDT
*/

public interface CenterServerOperations 
{
  String createTRecord (String managerId, String firstName, String lastName, String address, String phone, String specialization, String location);
  String createSRecord (String managerId, String firstName, String lastName, String[] courseRegistered, String status, String statusDate);
  String getRecordCounts (String managerId);
  String editRecord (String managerId, String recordID, String fieldName, String newValue) throws CenterServerOrb.CenterServerPackage.except;
  String transferRecord (String managerId, String recordID, String remoteCenterServerName) throws CenterServerOrb.CenterServerPackage.except;
  void shutdown ();
} // interface CenterServerOperations
