package com.puppetlabs.gatling.config

import scala.util.parsing.json.JSON
import com.puppetlabs.json.{JsonInt, JsonList, JsonString, JsonMap}
import com.puppetlabs.gatling.runner.SimulationWithScenario

class PuppetGatlingConfig(configFilePath: String) {

  private val Some(JsonMap(config)) = JSON.parseFull(io.Source.fromFile(configFilePath).mkString)

  private val JsonList(jsonNodes) = config("nodes")

  val simulationId = PuppetGatlingConfig.getEnvVar("PUPPET_GATLING_SIMULATION_ID")
  val baseUrl = PuppetGatlingConfig.getEnvVar("PUPPET_GATLING_MASTER_BASE_URL")
  val JsonString(runDescription) = config("run_description")

  val nodes = jsonNodes.map((n) => {
    val JsonMap(node) = n
    val JsonString(simClass) = node("simulation_class")
    val JsonInt(numInstances) = node("num_instances")
    val JsonInt(numRepetitions) = node("num_repetitions")
    val JsonInt(rampUpDuration) = node("ramp_up_duration_seconds")
    Node(Class.forName(simClass).asInstanceOf[Class[SimulationWithScenario]], numRepetitions, numInstances, rampUpDuration)
  })
}


object PuppetGatlingConfig {
  def apply(configFilePath: String) = new PuppetGatlingConfig(configFilePath)

  def getEnvVar(varName: String): String = {
    sys.env.getOrElse(varName, {
      throw new IllegalStateException("You must specify the environment variable '" + varName + "'!")
    })
  }

  // this is basically a process-wide singleton that we'll use to hold the config,
  //  which is kind of crappy, but will get the job done for now.
  private var instance: Option[PuppetGatlingConfig] = None

  val configPath = PuppetGatlingConfig.getEnvVar("PUPPET_GATLING_SIMULATION_CONFIG")

  /**
   * This method should be called once at the beginning of a run to cause the config
   * file to be parsed and instantiate our config singleton
   */
  def initialize(configFilePath: String = configPath): PuppetGatlingConfig = {
    instance = Some(PuppetGatlingConfig(configFilePath))
    instance.get
  }

  /**
   * This is the main accessor that should be used to read the configuration.
   */
  def configuration: PuppetGatlingConfig = {
    instance match {
      case None => throw new IllegalStateException("Configuration not yet initialized; please call #initialize method!")
      case _ => instance.get
    }
  }

}