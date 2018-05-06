package com.leapfin.lpfnfinder.test

import akka.actor.{ActorSystem, Props}
import akka.testkit.{TestActorRef, TestKit, TestProbe}
import com.leapfin.lpfnfinder.{AlphabeticGenerator, LpfnFinderMaster, LpfnFinderWorker}
import com.leapfin.lpfnfinder.LpfnFinderWorker._
import com.leapfin.lpfnfinder.Util.LpfnFinderParams
import org.scalatest.{BeforeAndAfterAll, FlatSpecLike, Matchers}

import scala.concurrent.duration._

class LpfnFinderWorkerTest(_system: ActorSystem)
  extends TestKit(_system)
  with AlphabeticGenerator
  with Matchers
  with FlatSpecLike
  with BeforeAndAfterAll {

  def this() = this(ActorSystem("LpfnFinderWorkerTest"))

  override def afterAll: Unit = {
    shutdown(system)
  }

  "Worker" should "report timeout after time is over (also meaning that variable startTime is being set inside the actor)" in {
    val testProbe = TestProbe()
    val worker = TestActorRef(new LpfnFinderWorker("YouAreNotGoingToFindThis1", 1000, randomAlphaGen.toStream, testProbe.ref))
    worker ! StartNow
    testProbe.expectMsgPF(2000.milliseconds)({case Status(_,_,Timeout) => true})
  }

  "Worker" should "report Success after finding the string" in {
    val testProbe = TestProbe()
    val worker = TestActorRef(new LpfnFinderWorker("a", 5000, randomAlphaGen.toStream, testProbe.ref))
    worker ! StartNow
    testProbe.expectMsgPF(5000.milliseconds)({case Status(_,_, Success) => true})
  }

  "Worker" should "report Failure after Stream is over" in {
    val testProbe = TestProbe()
    val worker = TestActorRef(new LpfnFinderWorker("z", 5000, Stream('a', 'b', 'c'), testProbe.ref))
    worker ! StartNow
    testProbe.expectMsgPF(5000.milliseconds)({case Status(_,_, Failure) => true})
  }

  "Master" should "start workers" in {
    val testProbe = TestProbe()
    def newMaster = new LpfnFinderMaster(LpfnFinderParams(10, "1", 10000)) with AlphabeticGenerator
    val master = TestActorRef(Props(newMaster), testProbe.ref)
    awaitAssert(master.underlyingActor.asInstanceOf[LpfnFinderMaster].workers.nonEmpty, 10000.milliseconds)
  }

  "Master" should "get reports" in {
    val actorSystem = ActorSystem("WaitReport")
    val testProbe = TestProbe()(actorSystem)
    def newMaster = new LpfnFinderMaster(LpfnFinderParams(1, "a", 10000)) with AlphabeticGenerator
    val master = TestActorRef(Props(newMaster), testProbe.ref)(actorSystem)
    awaitAssert(master.underlyingActor.asInstanceOf[LpfnFinderMaster].results.nonEmpty, 5.seconds)
  }
}
