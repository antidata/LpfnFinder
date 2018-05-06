package com.leapfin.lpfnfinder

import akka.event.slf4j.Logger
import com.typesafe.config.Config

import scala.annotation.tailrec
import scala.util.{Success, Try}

object Util {
  case class LpfnFinderParams(workers: Int, searchString: String, timeout: Long)

  trait AppParamNames {
    type ParamName = String

    lazy val workersName: ParamName = "workers"
    lazy val searchStringName: ParamName = "searchString"
    lazy val timeoutName: ParamName = "timeout"
    lazy val paramStarter: ParamName = "--"
    lazy val paramSign: ParamName = "="

    def name2format(paramName: ParamName): String = s"$paramStarter$paramName$paramSign"
  }

  trait AppLogging {
    lazy val logger = Logger("LpfnFinder")
  }

  trait AppParamsGet {
    apg: AppParamNames with AppLogging =>

    protected def getAppParams(params: List[String], config: Config): LpfnFinderParams = {
      @tailrec
      def getParams(pendingParams: List[String], finderParams: LpfnFinderParams): LpfnFinderParams = {
        pendingParams match {
          case currentParam :: otherParams if currentParam.contains(name2format(workersName)) =>
            Try(currentParam.split(paramSign)(1).toInt) match {
              case Success(extraction) => getParams(otherParams, finderParams.copy(workers = extraction))
              case _ =>
                logger.error(s"Cannot retrieve int parameter value from $currentParam, using default")
                getParams(otherParams, finderParams)
            }

          case currentParam :: otherParams if currentParam.contains(name2format(searchStringName)) =>
            Try(currentParam.split(paramSign)(1)) match {
              case Success(extraction) => getParams(otherParams, finderParams.copy(searchString = extraction))
              case _ =>
                logger.error(s"Cannot retrieve string parameter value from $currentParam, using default")
                getParams(otherParams, finderParams)
            }

          case currentParam :: otherParams if currentParam.contains(name2format(timeoutName)) =>
            Try(currentParam.split(paramSign)(1).toLong) match {
              case Success(extraction) => getParams(otherParams, finderParams.copy(timeout = extraction))
              case _ =>
                logger.error(s"Cannot retrieve Long parameter value from $currentParam, using default")
                getParams(otherParams, finderParams)
            }

          case currentParam :: otherParams =>
            logger.info(s"Parameter value not used: $currentParam")
            getParams(otherParams, finderParams)

          case Nil =>
            finderParams
        }
      }
      getParams(params, LpfnFinderParams(config.getInt(workersName), config.getString(searchStringName), config.getLong(timeoutName)))
    }
  }
}
