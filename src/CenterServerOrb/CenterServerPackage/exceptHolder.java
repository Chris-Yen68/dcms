package CenterServerOrb.CenterServerPackage;

/**
* CenterServerOrb/CenterServerPackage/exceptHolder.java .
* 由IDL-to-Java 编译器 (可移植), 版本 "3.2"生成
* 从CenterServerOrb.idl
* 2018年6月11日 星期一 下午05时57分17秒 EDT
*/

public final class exceptHolder implements org.omg.CORBA.portable.Streamable
{
  public CenterServerOrb.CenterServerPackage.except value = null;

  public exceptHolder ()
  {
  }

  public exceptHolder (CenterServerOrb.CenterServerPackage.except initialValue)
  {
    value = initialValue;
  }

  public void _read (org.omg.CORBA.portable.InputStream i)
  {
    value = CenterServerOrb.CenterServerPackage.exceptHelper.read (i);
  }

  public void _write (org.omg.CORBA.portable.OutputStream o)
  {
    CenterServerOrb.CenterServerPackage.exceptHelper.write (o, value);
  }

  public org.omg.CORBA.TypeCode _type ()
  {
    return CenterServerOrb.CenterServerPackage.exceptHelper.type ();
  }

}
