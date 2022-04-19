package hu.szigyi.scala.graph.visitor

import hu.szigyi.scala.graph.Model.*
import org.apache.bcel.classfile.JavaClass
import org.apache.bcel.generic.*

import scala.collection.mutable

class MethodVisitor(methodGen: MethodGen, visitedClass: JavaClass) extends EmptyVisitor {
  private val constantPool = methodGen.getConstantPool
  private val caller = ClassMethod(visitedClass.getClassName, methodGen.getName)
  private val collectedMethodCalls = mutable.ListBuffer.empty[Invokation]

  def start(): Seq[Invokation] =
    if (methodGen.isAbstract || methodGen.isNative) Seq.empty
    else {
      var ih: InstructionHandle = methodGen.getInstructionList.getStart
      while (ih != null) {
        val i = ih.getInstruction
        if (!visitInstruction(i))
          i.accept(this)
        ih = ih.getNext
      }
      collectedMethodCalls.toSeq
    }

  def visitInstruction(i: Instruction): Boolean = {
    val opCode = i.getOpcode
    (InstructionConst.getInstruction(opCode) != null) && !i.isInstanceOf[ConstantPushInstruction] && !i.isInstanceOf[ReturnInstruction]
  }

  override def visitINVOKEVIRTUAL(i: INVOKEVIRTUAL): Unit =
    collectedMethodCalls += VirtualInvokation(caller, ClassMethod(i.getReferenceType(constantPool).toString, i.getMethodName(constantPool)))

  override def visitINVOKEINTERFACE(i: INVOKEINTERFACE): Unit =
    collectedMethodCalls += InterfaceInvokation(caller, ClassMethod(i.getReferenceType(constantPool).toString, i.getMethodName(constantPool)))

  override def visitINVOKESPECIAL(i: INVOKESPECIAL): Unit =
    collectedMethodCalls += SpecialInvokation(caller, ClassMethod(i.getReferenceType(constantPool).toString, i.getMethodName(constantPool)))

  override def visitINVOKESTATIC(i: INVOKESTATIC): Unit =
    collectedMethodCalls += StaticInvokation(caller, ClassMethod(i.getReferenceType(constantPool).toString, i.getMethodName(constantPool)))

  override def visitINVOKEDYNAMIC(i: INVOKEDYNAMIC): Unit =
    collectedMethodCalls += DynamicInvokation(caller, ClassMethod(i.getType(constantPool).toString, i.getMethodName(constantPool)))
}
