package com.leapfin.lpfnfinder

import akka.actor.{ActorSystem, Props}
import com.typesafe.config.ConfigFactory
import Util._

object LpfnFinder extends App with AppParamNames with AppLogging with AppParamsGet {
  private val config = ConfigFactory.load()
  private val system = ActorSystem("LpfnFinder")

  val finderParams = getAppParams(args.toList, config) // Getting parameters from command line overriding ones from config

  private def newMaster = new LpfnFinderMaster(finderParams) with AlphabeticGenerator

  private def startWorkers(): Unit = system.actorOf(Props(newMaster), s"master")

  startWorkers()
}
