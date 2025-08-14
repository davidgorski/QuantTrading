package com.quantTrading.config

import com.quantTrading.aws.Secrets
import software.amazon.awssdk.regions.Region

import java.time.{Clock, ZoneId}


trait Config {

  def awsRegion: Region

  def awsS3Bucket: String

  def awsSecretName: String

  def awsSecrets: Secrets

  def zoneId: ZoneId

  def clock: Clock
}

