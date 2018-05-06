package com.leapfin.lpfnfinder

import akka.actor.{Actor, ActorLogging, ActorRef, Props, Terminated}
import com.leapfin.lpfnfinder.Util._
import com.leapfin.lpfnfinder.LpfnFinderWorker._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._

class LpfnFinderMaster(params: LpfnFinderParams) extends Actor with ActorLogging {
  s: InfiniteStreamProvider =>

  log.info(s"Initializing with Params: $params")

  var workers: List[ActorRef] = Nil
  var completedOrTimeout = 0
  var results: List[Status] = Nil

  context.system.scheduler.scheduleOnce(1.millisecond, self, StartNow) // Starts after class has been initialized

  override def receive: Receive = {
    case StartNow =>
      workers = (1 to params.workers).toList.map { workerId =>
        context.watch(
          context.system.actorOf(Props(new LpfnFinderWorker(params.searchString, params.timeout, stream, self)), s"worker-$workerId-${randomAlphaGen.next()}"))
      }
      workers.foreach(_ ! StartNow)

    case e:Status =>
      results = e :: results

    case Terminated(_) =>
      completedOrTimeout = completedOrTimeout + 1
      if(completedOrTimeout == params.workers) {
        println(s"${results.sortWith(_.elapsed > _.elapsed).map(r => s"[${r.elapsed}] [${r.byteCnt}] [${r.status}]").mkString("\n")}")
        println(s"Average: ${
          val success = results.filter(_.status == Success)
          success.map(_.byteCnt).sum.toDouble / success.map(_.elapsed).sum
        } bytes/milliseconds")

        context.system.terminate()
      }
  }
}
