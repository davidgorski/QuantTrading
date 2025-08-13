package com.quantTrading.aws

import com.typesafe.scalalogging.StrictLogging
import software.amazon.awssdk.regions.Region
import software.amazon.awssdk.services.secretsmanager.SecretsManagerClient
import software.amazon.awssdk.services.secretsmanager.model.{GetSecretValueRequest, GetSecretValueResponse}
import spray.json._
import DefaultJsonProtocol._

object Secrets extends StrictLogging {

  def loadSecrets(awsRegion: Region, awsSecretsName: String): Map[String, String] = {
    // Create a Secrets Manager client
    val client = SecretsManagerClient.builder().region(awsRegion).build()

    val getSecretValueRequest = GetSecretValueRequest.builder().secretId(awsSecretsName).build()

    val getSecretValueResponse: GetSecretValueResponse = client.getSecretValue(getSecretValueRequest)

    val secretsJsonString: String = getSecretValueResponse.secretString() // eg {"secretKey1": "secretValue1", "secretKey2": "secretValue2"}

    val secrets: Map[String, String] = secretsJsonString.parseJson.convertTo[Map[String, String]]

    secrets
  }
}