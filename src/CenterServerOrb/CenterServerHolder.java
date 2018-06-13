package CenterServerOrb;

/**
* CenterServerOrb/CenterServerHolder.java .
* 由IDL-to-Java 编译器 (可移植), 版本 "3.2"生成
* 从CenterServerOrb.idl
* 2018年6月12日 星期二 下午07时46分10秒 EDT
*/

public final class CenterServerHolder implements org.omg.CORBA.portable.Streamable
{
  public CenterServerOrb.CenterServer value = null;

  public CenterServerHolder ()
  {
  }

  public CenterServerHolder (CenterServerOrb.CenterServer initialValue)
  {
    value = initialValue;
  }

  public void _read (org.omg.CORBA.portable.InputStream i)
  {
    value = CenterServerOrb.CenterServerHelper.read (i);
  }

  public void _write (org.omg.CORBA.portable.OutputStream o)
  {
    CenterServerOrb.CenterServerHelper.write (o, value);
  }

  public org.omg.CORBA.TypeCode _type ()
  {
    return CenterServerOrb.CenterServerHelper.type ();
  }

}
