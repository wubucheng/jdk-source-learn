package com.sun.corba.se.spi.activation;


/**
* com/sun/corba/se/spi/activation/_ServerImplBase.java .
* Generated by the IDL-to-Java compiler (portable), version "3.2"
* from c:/re/workspace/8-2-build-windows-amd64-cygwin/jdk8u171/10807/corba/src/share/classes/com/sun/corba/se/spi/activation/activation.idl
* Wednesday, March 28, 2018 4:07:48 PM PDT
*/


/** Server callback API, passed to Activator in active method.
    */
public abstract class _ServerImplBase extends org.omg.CORBA.portable.ObjectImpl
                implements com.sun.corba.se.spi.activation.Server, org.omg.CORBA.portable.InvokeHandler
{

  // Constructors
  public _ServerImplBase ()
  {
  }

  private static java.util.Hashtable _methods = new java.util.Hashtable ();
  static
  {
    _methods.put ("shutdown", new java.lang.Integer (0));
    _methods.put ("install", new java.lang.Integer (1));
    _methods.put ("uninstall", new java.lang.Integer (2));
  }

  public org.omg.CORBA.portable.OutputStream _invoke (String $method,
                                org.omg.CORBA.portable.InputStream in,
                                org.omg.CORBA.portable.ResponseHandler $rh)
  {
    org.omg.CORBA.portable.OutputStream out = null;
    java.lang.Integer __method = (java.lang.Integer)_methods.get ($method);
    if (__method == null)
      throw new org.omg.CORBA.BAD_OPERATION (0, org.omg.CORBA.CompletionStatus.COMPLETED_MAYBE);

    switch (__method.intValue ())
    {

  /** Shutdown this server.  Returns after orb.shutdown() completes.
	*/
       case 0:  // activation/Server/shutdown
       {
         this.shutdown ();
         out = $rh.createReply();
         break;
       }


  /** Install the server.  Returns after the install hook completes
	* execution in the server.
	*/
       case 1:  // activation/Server/install
       {
         this.install ();
         out = $rh.createReply();
         break;
       }


  /** Uninstall the server.  Returns after the uninstall hook
	* completes execution.
	*/
       case 2:  // activation/Server/uninstall
       {
         this.uninstall ();
         out = $rh.createReply();
         break;
       }

       default:
         throw new org.omg.CORBA.BAD_OPERATION (0, org.omg.CORBA.CompletionStatus.COMPLETED_MAYBE);
    }

    return out;
  } // _invoke

  // Type-specific CORBA::Object operations
  private static String[] __ids = {
    "IDL:activation/Server:1.0"};

  public String[] _ids ()
  {
    return (String[])__ids.clone ();
  }


} // class _ServerImplBase
