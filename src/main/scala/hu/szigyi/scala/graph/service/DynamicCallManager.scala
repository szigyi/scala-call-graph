package hu.szigyi.scala.graph.service

import org.apache.bcel.classfile.*

import java.util.regex.Pattern
import scala.collection.mutable

/**
 * [[DynamicCallManager]] provides facilities to retrieve information about
 * dynamic calls statically.
 * <p>
 * Most of the time, call relationships are explicit, which allows to properly
 * build the call graph statically. But in the case of dynamic linking, i.e.
 * <code>invokedynamic</code> instructions, this relationship might be unknown
 * until the code is actually executed. Indeed, bootstrap methods are used to
 * dynamically link the code at first call. One can read details about the
 * <a href=
 * "https://docs.oracle.com/javase/8/docs/technotes/guides/vm/multiple-language-support.html#invokedynamic"><code>invokedynamic</code>
 * instruction</a> to know more about this mechanism.
 * <p>
 * Nested lambdas are particularly subject to such absence of concrete caller,
 * which lead us to produce method names like <code>lambda$null$0</code>, which
 * breaks the call graph. This information can however be retrieved statically
 * through the code of the bootstrap method called.
 * <p>
 * In [[# retrieveCalls ( Method, JavaClass)]], we retrieve the (called,
 * caller) relationships by analyzing the code of the caller [[Method]].
 * This information is then used in [[# linkCalls ( Method )]] to rename the
 * called [[Method]] properly.
 *
 * @author Matthieu Vergne <matthieu.vergne@gmail.com>
 *
 * Rewritten to Scala by Szigyi
 */
class DynamicCallManager {
  private val BOOTSTRAP_CALL_PATTERN = Pattern.compile("invokedynamic\t(\\d+):\\S+ \\S+ \\(\\d+\\)")
  private val CALL_HANDLE_INDEX_ARGUMENT = 1
  private val dynamicCallers = mutable.HashMap.empty[String, String]

  def retrieveCalls(method: Method, clazz: JavaClass): Unit = {
    if (!method.isAbstract && !method.isNative) {
      val constantPool = method.getConstantPool
      val boots = getBootstrapMethods(clazz)
      val code = method.getCode.toString
      val matcher = BOOTSTRAP_CALL_PATTERN.matcher(code)
      while (matcher.find) {
        val bootIndex = matcher.group(1).toInt
        val bootMethod = boots(bootIndex)
        if (bootMethod.getBootstrapArguments.length > 1) {
          val calledIndex = bootMethod.getBootstrapArguments()(CALL_HANDLE_INDEX_ARGUMENT)
          val calledName = getMethodNameFromHandleIndex(constantPool, calledIndex)
          val callerName = method.getName
          dynamicCallers.put(calledName, callerName)
        }
      }
    }
  }

  def linkCalls(method: Method): Unit = {
    val nameIndex = method.getNameIndex
    val constantPool = method.getConstantPool
    val methodName = constantPool.getConstant(nameIndex).asInstanceOf[ConstantUtf8].getBytes
    var linkedName = methodName
    var callerName = methodName

    while (linkedName.matches("(lambda\\$)+null(\\$\\d+)+")) {
      callerName = dynamicCallers(callerName)
      linkedName = linkedName.replace("null", callerName)
    }
    constantPool.setConstant(nameIndex, new ConstantUtf8(linkedName))
  }

  private def getBootstrapMethods(jc: JavaClass): Seq[BootstrapMethod] =
    jc.getAttributes.flatMap { attribute =>
      attribute match {
        case methods: BootstrapMethods =>
          methods.getBootstrapMethods.toSeq
        case _ => Seq.empty
      }
    }.toSeq

  private def getMethodNameFromHandleIndex(constantPool: ConstantPool, callIndex: Int): String = {
    val handle = constantPool.getConstant(callIndex).asInstanceOf[ConstantMethodHandle]
    val ref = constantPool.getConstant(handle.getReferenceIndex).asInstanceOf[ConstantCP]
    val nameAndType = constantPool.getConstant(ref.getNameAndTypeIndex).asInstanceOf[ConstantNameAndType]
    nameAndType.getName(constantPool)
  }
}
