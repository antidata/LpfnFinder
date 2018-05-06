package com.leapfin.lpfnfinder

import java.util.Date
import akka.actor.{Actor, ActorLogging, ActorRef, PoisonPill}
import scala.util.{Try, Failure => TFailure}

class LpfnFinderWorker(searchString: String, timeout: Long, stream: Stream[Char], master: ActorRef) extends Actor with ActorLogging {
  import LpfnFinderWorker._

  var startTime: Option[Long] = None
  var status: LpfnFinderWorkerStatus = Working
  var processedBytes: Long = 0
  var currentFind = ""
  val iterator: Iterator[Char] = stream.iterator

  def receive: Receive = {
    case StartNow =>
      startTime = Some(getTime) // StartTime is meant only to be set here
      self ! Process // Since Process object is protected we are sure that we are the only one using it and means that startTime will be set

    case Process if (getTime - startTime.get) < timeout =>
      Try {
        val current = iterator.next()
        if(searchString(currentFind.length) == current) {
          currentFind = currentFind + current
          if(currentFind == searchString) {
            master ! Status(getTime - startTime.get, processedBytes + 1, Success)
            self ! PoisonPill // Shutdown worker
          } else {
            processedBytes = processedBytes + 1
            self ! Process
          }
        } else {
          currentFind = ""
          self ! Process
        }
      } match {
        case TFailure(exception) =>
          log.error(s"Worker failed with exception $exception")
          master ! Status(getTime - startTime.get, processedBytes, Failure)
          self ! PoisonPill
        case _ => // Nothing to report, everything good here
      }

    case Process => //Timeout case
      master ! Status(getTime - startTime.get, processedBytes, Timeout)
      self ! PoisonPill
  }
}

object LpfnFinderWorker {
  trait LpfnFinderWorkerStatus

  case object Working extends LpfnFinderWorkerStatus
  case object Success extends LpfnFinderWorkerStatus
  case object Timeout extends LpfnFinderWorkerStatus
  case object Failure extends LpfnFinderWorkerStatus

  trait LpfnFinderWorkerMsg

  case object StartNow extends LpfnFinderWorkerMsg
  protected case object Process extends LpfnFinderWorkerMsg
  case class Status(elapsed: Long, byteCnt: Long, status: LpfnFinderWorkerStatus) extends LpfnFinderWorkerMsg

  def getTime: Long = (new Date).getTime
}