package CenterServerOrb;


/**
* CenterServerOrb/listHolder.java .
* 由IDL-to-Java 编译器 (可移植), 版本 "3.2"生成
* 从CenterServerOrb.idl
* 2018年6月11日 星期一 下午05时57分17秒 EDT
*/

public final class listHolder implements org.omg.CORBA.portable.Streamable
{
  public String value[] = null;

  public listHolder ()
  {
  }

  public listHolder (String[] initialValue)
  {
    value = initialValue;
  }

  public void _read (org.omg.CORBA.portable.InputStream i)
  {
    value = CenterServerOrb.listHelper.read (i);
  }

  public void _write (org.omg.CORBA.portable.OutputStream o)
  {
    CenterServerOrb.listHelper.write (o, value);
  }

  public org.omg.CORBA.TypeCode _type ()
  {
    return CenterServerOrb.listHelper.type ();
  }

}
