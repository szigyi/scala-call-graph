package hu.szigyi.scala.graph

object Model {

  case class ClassMethod(className: String, method: String)

  sealed trait Invokation {
    val caller: ClassMethod
    val called: ClassMethod
  }
  case class VirtualInvokation(caller: ClassMethod, called: ClassMethod) extends Invokation
  case class InterfaceInvokation(caller: ClassMethod, called: ClassMethod) extends Invokation
  case class SpecialInvokation(caller: ClassMethod, called: ClassMethod) extends Invokation
  case class StaticInvokation(caller: ClassMethod, called: ClassMethod) extends Invokation
  case class DynamicInvokation(caller: ClassMethod, called: ClassMethod) extends Invokation


  /**
   * references => referencedClass
   * references called referencedClass in the sourcecode
   *
   * ClassA invokes ClassB (aka: ClassA => ClassB) then you will see
   * ClassLevel(ClassB, Set(ClassA))
   *
   * @param referencedClass the class that was invoked, referenced
   * @param references the classes that used, invoked the referenced class
   */
  case class ClassLevel(referencedClass: String, references: Set[Reference])

  sealed trait Reference {
    val className: String
    val count: Int
  }
  case class Virtual(className: String, count: Int) extends Reference
  case class InterfaceRef(className: String, count: Int) extends Reference
  case class Special(className: String, count: Int) extends Reference
  case class Static(className: String, count: Int) extends Reference
  case class Dynamic(className: String, count: Int) extends Reference
}