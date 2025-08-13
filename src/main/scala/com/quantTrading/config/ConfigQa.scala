package com.quantTrading.config

import com.quantTrading.aws.Secrets
import software.amazon.awssdk.regions.Region

case class ConfigQa() extends Config {

  override val awsRegion: Region = Region.US_EAST_1

  override val awsS3Bucket: String = "quant-trader-bucket-qa"

  override val awsSecretName: String = "qa/quant_trader"

  override val awsSecrets: Map[String, String] = Secrets.loadSecrets(awsRegion, awsSecretName)

}
