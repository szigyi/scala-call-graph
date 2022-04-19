package hu.szigyi.scala.graph.e2e

import hu.szigyi.scala.graph.*
import org.scalatest.Sequential

class CallGraphSuite extends Sequential(new CallGraphSpec, new NativeCallGraphSpec, new SelfCallGraphSpec, new StaticCallGraphSpec, new FilteredCallGraphSpec)
