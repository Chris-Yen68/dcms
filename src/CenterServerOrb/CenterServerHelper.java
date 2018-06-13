package CenterServerOrb;


/**
* CenterServerOrb/CenterServerHelper.java .
* 由IDL-to-Java 编译器 (可移植), 版本 "3.2"生成
* 从CenterServerOrb.idl
* 2018年6月12日 星期二 下午07时46分10秒 EDT
*/

abstract public class CenterServerHelper
{
  private static String  _id = "IDL:CenterServerOrb/CenterServer:1.0";

  public static void insert (org.omg.CORBA.Any a, CenterServerOrb.CenterServer that)
  {
    org.omg.CORBA.portable.OutputStream out = a.create_output_stream ();
    a.type (type ());
    write (out, that);
    a.read_value (out.create_input_stream (), type ());
  }

  public static CenterServerOrb.CenterServer extract (org.omg.CORBA.Any a)
  {
    return read (a.create_input_stream ());
  }

  private static org.omg.CORBA.TypeCode __typeCode = null;
  synchronized public static org.omg.CORBA.TypeCode type ()
  {
    if (__typeCode == null)
    {
      __typeCode = org.omg.CORBA.ORB.init ().create_interface_tc (CenterServerOrb.CenterServerHelper.id (), "CenterServer");
    }
    return __typeCode;
  }

  public static String id ()
  {
    return _id;
  }

  public static CenterServerOrb.CenterServer read (org.omg.CORBA.portable.InputStream istream)
  {
    return narrow (istream.read_Object (_CenterServerStub.class));
  }

  public static void write (org.omg.CORBA.portable.OutputStream ostream, CenterServerOrb.CenterServer value)
  {
    ostream.write_Object ((org.omg.CORBA.Object) value);
  }

  public static CenterServerOrb.CenterServer narrow (org.omg.CORBA.Object obj)
  {
    if (obj == null)
      return null;
    else if (obj instanceof CenterServerOrb.CenterServer)
      return (CenterServerOrb.CenterServer)obj;
    else if (!obj._is_a (id ()))
      throw new org.omg.CORBA.BAD_PARAM ();
    else
    {
      org.omg.CORBA.portable.Delegate delegate = ((org.omg.CORBA.portable.ObjectImpl)obj)._get_delegate ();
      CenterServerOrb._CenterServerStub stub = new CenterServerOrb._CenterServerStub ();
      stub._set_delegate(delegate);
      return stub;
    }
  }

  public static CenterServerOrb.CenterServer unchecked_narrow (org.omg.CORBA.Object obj)
  {
    if (obj == null)
      return null;
    else if (obj instanceof CenterServerOrb.CenterServer)
      return (CenterServerOrb.CenterServer)obj;
    else
    {
      org.omg.CORBA.portable.Delegate delegate = ((org.omg.CORBA.portable.ObjectImpl)obj)._get_delegate ();
      CenterServerOrb._CenterServerStub stub = new CenterServerOrb._CenterServerStub ();
      stub._set_delegate(delegate);
      return stub;
    }
  }

}
