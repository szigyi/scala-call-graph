package hu.szigyi.scala.graph.visitor

import hu.szigyi.scala.graph.Model.*
import hu.szigyi.scala.graph.service.DynamicCallManager
import org.apache.bcel.classfile.{ConstantPool, EmptyVisitor, JavaClass, Method}
import org.apache.bcel.generic.{ConstantPoolGen, MethodGen}

import scala.collection.mutable

class ClassVisitor(clazz: JavaClass) extends EmptyVisitor {
  private val collectedMethodCalls = mutable.ListBuffer.empty[Invokation]
  private val constants = new ConstantPoolGen(clazz.getConstantPool)
  private val dcManager = new DynamicCallManager()

  def methodCalls: Seq[Invokation] = {
    visitJavaClass(clazz)
    collectedMethodCalls.toSeq
  }

  override def visitJavaClass(jc: JavaClass): Unit = {
    jc.getConstantPool.accept(this)
    jc.getMethods.foreach { method =>
      dcManager.retrieveCalls(method, clazz)
      dcManager.linkCalls(method)
      method.accept(this)
    }
  }

  override def visitConstantPool(constantPool: ConstantPool): Unit = {
    for (i <- 0 until constantPool.getLength) {
      val constant = constantPool.getConstant(i)
      if (null != constant && constant.getTag == 7) {
//        println(String.format(classReferenceFormat, constantPool.constantToString(constant)))
      }
    }
  }

  override def visitMethod(method: Method): Unit = {
    val mg = new MethodGen(method, clazz.getClassName, constants)
    val visitor = new MethodVisitor(mg, clazz)
    collectedMethodCalls.addAll(visitor.start())
  }
}