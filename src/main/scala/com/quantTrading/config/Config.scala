package com.quantTrading.config

import software.amazon.awssdk.regions.Region


trait Config {

  def awsRegion: Region

  def awsS3Bucket: String

  def awsSecretName: String

  def awsSecrets: Map[String, String]
}

