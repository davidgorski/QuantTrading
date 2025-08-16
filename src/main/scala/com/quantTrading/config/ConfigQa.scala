package com.quantTrading.config

import com.quantTrading.aws.Secrets
import com.quantTrading.dateUtils.{DateUtils, MarketCalendar}
import com.quantTrading.symbols.QtSymbol
import org.scalactic.anyvals.PosZInt
import software.amazon.awssdk.regions.Region

import java.time.{Clock, ZoneId}


case class ConfigQa() extends Config {

  override val awsRegion: Region = Region.US_EAST_1

  override val awsS3Bucket: String = "quant-trader-bucket-qa"

  override val awsSecretName: String = "qa/quant_trader"

  override val awsSecrets: Secrets = Secrets.loadSecrets(awsRegion, awsSecretName)

  override val zoneId: ZoneId = ZoneId.of("America/New_York")

  override val clock: Clock =  Clock.system(zoneId)

  override val symbols: Set[QtSymbol] = QtSymbol.symbols

  override val nApiRetries: PosZInt = PosZInt(3)
  
  override val marketCalendar: MarketCalendar = DateUtils.loadNyseCalendar()
}
