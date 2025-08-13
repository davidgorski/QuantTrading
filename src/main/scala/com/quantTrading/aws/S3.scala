package com.quantTrading.aws

import software.amazon.awssdk.core.ResponseBytes
import software.amazon.awssdk.core.sync.RequestBody
import software.amazon.awssdk.services.s3.S3Client
import software.amazon.awssdk.regions.Region
import software.amazon.awssdk.services.s3.model.{GetObjectRequest, GetObjectResponse, HeadObjectRequest, HeadObjectResponse, ListObjectsV2Request, PutObjectRequest, PutObjectResponse, S3Exception}
import scala.collection.JavaConverters._
import java.nio.charset.StandardCharsets


class S3(val region: Region) {

  private val s3Client: S3Client =
    S3Client.builder()
      .region(region)
      .build()

  /**
   * Write json to a bucket/key
   *
   * @param bucket
   * @param key
   * @param jsonString
   * @return
   */
  def writeJson(bucket: String, key: String, jsonString: String): scalaz.Validation[String, Unit] = {
    try {
      val putObjectRequest =
        PutObjectRequest.builder()
          .bucket(bucket)
          .key(key)
          .contentType("application/json")
          .build()

      val putObjectResponse: PutObjectResponse =
        s3Client.putObject(putObjectRequest, RequestBody.fromString(jsonString, StandardCharsets.UTF_8))

      if (putObjectResponse.eTag() != null && putObjectResponse.eTag().nonEmpty)
        scalaz.Success(())
      else
        scalaz.Failure(s"Failed to write $key to bucket $bucket")

    } catch {

      case ex: S3Exception => scalaz.Failure(s"S3Exception: ${ex.awsErrorDetails().errorMessage()}")
      case ex: Throwable => scalaz.Failure(s"Unexpected error ${ex.getMessage}")
    }
  }

  /**
   * Get keys that are not in the bucket
   *
   * @param bucket
   * @param keys
   * @return
   */
  def getMissingKeys(bucket: String, keys: Set[String]): Set[String] = {
    val keysInBucket: Set[String] = getAllKeysInBucket(bucket)
    val missingKeys = keys.diff(keysInBucket)
    missingKeys
  }

  /**
   * Get all keys in the bucket
   * @param bucket
   * @return
   */
  def getAllKeysInBucket(bucket: String): Set[String] = {
    var keys = List.empty[String]
    var continuationToken: String = null
    var isTruncated = true

    while (isTruncated) {
      val listObjectsRequestBuilder: ListObjectsV2Request.Builder =
        ListObjectsV2Request.builder()
          .bucket(bucket)
          .maxKeys(1000)

      if (continuationToken != null)
        listObjectsRequestBuilder.continuationToken(continuationToken)

      val result = s3Client.listObjectsV2(listObjectsRequestBuilder.build())

      keys ++= result.contents().asScala.map(_.key())
      isTruncated = result.isTruncated
      continuationToken = result.nextContinuationToken()
    }

    keys.toSet
  }

  /**
   * Check if a particular key is in the bucket
   *
   * @param bucket
   * @param key
   * @return
   */
  def isKeyInBucket(bucket: String, key: String): Boolean = {
    try {

      val headObjectRequest =
        HeadObjectRequest.builder()
          .bucket(bucket)
          .key(key)
          .build()

      // throws if bucket doesn't exist or key doesn't exist
      val _: HeadObjectResponse =
        s3Client.headObject(headObjectRequest)

      true

    } catch {

      case _: Throwable =>
        false
    }
  }

  /**
   * Read json
   *
   * @param bucket
   * @param key
   * @return
   */
  def readJson(bucket: String, key: String): scalaz.Validation[String, String] = {

    if (isKeyInBucket(bucket, key)) {
      val getObjectRequest =
        GetObjectRequest.builder()
          .bucket(bucket)
          .key(key)
          .build()

      val responseBytes: ResponseBytes[GetObjectResponse] = s3Client.getObjectAsBytes(getObjectRequest)
      val responseString = responseBytes.asString(StandardCharsets.UTF_8)
      scalaz.Success(responseString)

    } else {

      scalaz.Failure(s"Key=$key Bucket=$bucket not found")
    }
  }
}
