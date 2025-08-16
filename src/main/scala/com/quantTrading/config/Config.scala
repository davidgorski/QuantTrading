package com.quantTrading.config

import com.quantTrading.aws.Secrets
import com.quantTrading.dateUtils.MarketCalendar
import com.quantTrading.symbols.QtSymbol
import org.scalactic.anyvals.PosZInt
import software.amazon.awssdk.regions.Region

import java.time.{Clock, ZoneId}


trait Config {

  def awsRegion: Region

  def awsS3Bucket: String

  def awsSecretName: String

  def awsSecrets: Secrets

  def zoneId: ZoneId

  def clock: Clock

  def symbols: Set[QtSymbol]

  def nApiRetries: PosZInt

  def marketCalendar: MarketCalendar
}

