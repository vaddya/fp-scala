package com.vaddya.fpscala.observatory

import java.sql.Date
import java.time.LocalDate

import org.apache.spark.sql.functions.{avg, struct, udf}
import org.apache.spark.sql.types._
import org.apache.spark.sql.{DataFrame, SparkSession}

import scala.io.Source

trait ExtractionLogic {
  val spark: SparkSession =
    SparkSession
      .builder()
      .appName("Observatory")
      .master("local[*]")
      .getOrCreate()

  import spark.implicits._

  val temperaturesFileSchema: StructType = StructType(
    StructField("stn", StringType) ::
      StructField("wban", StringType) ::
      StructField("month", IntegerType) ::
      StructField("day", IntegerType) ::
      StructField("temp", DoubleType) ::
      Nil)

  val stationsFileSchema: StructType = StructType(
    StructField("stn", StringType) ::
      StructField("wban", StringType) ::
      StructField("lat", DoubleType) ::
      StructField("lon", DoubleType) ::
      Nil)

  def locateTemperaturesDf(year: Year, stationsFile: String, temperaturesFile: String): DataFrame = {
    val createDate = udf { (month: Int, day: Int) =>
      Date.valueOf(LocalDate.of(year, month, day))
    }
    val records = readDf(temperaturesFile, temperaturesFileSchema)
      .withColumn("date", createDate($"month", $"day"))
      .withColumn("temp", ($"temp" - 32) / 1.8)
      .select("stn", "wban", "date", "temp")

    val stations = readDf(stationsFile, stationsFileSchema)
      .filter($"lat".isNotNull and $"lon".isNotNull)
      .withColumn("location", struct($"lat", $"lon"))
      .select("stn", "wban", "location")

    locateTemperaturesDf(records, stations)
  }

  /**
    * Accepts DataFrames with columns:
    * - records: snt, wban, date, temp
    * - stations: stn, wban, location
    * Returns DataFrame with columns: stn, wban, date, temp and location
    */
  def locateTemperaturesDf(records: DataFrame, stations: DataFrame): DataFrame = {
    // avoid ambiguity 
    val fixedStations = stations
      .withColumnRenamed("stn", "stn_s")
      .withColumnRenamed("wban", "wban_s")
    records
      .join(
        fixedStations,
        $"stn" <=> $"stn_s" && $"wban" <=> $"wban_s",
        "left"
      )
      .filter($"location".isNotNull)
      .select("stn", "wban", "date", "temp", "location")
  }

  /**
    * Receives DataFrame with columns: date, location and temp.
    * Returns DataFrame with columns: location, avg_temp
    */
  def locationYearlyAverageRecordsDf(records: DataFrame): DataFrame = {
    records
      .groupBy("location")
      .agg(
        avg($"temp").as("avg_temp")
      )
      .select("location", "avg_temp")
  }

  private def readDf(path: String, schema: StructType): DataFrame = {
    val reader = spark.read
      .option("header", "false")
      .schema(schema)
    if (!path.startsWith("/")) reader.csv(path)
    else {
      // file is likely to be in resources, so we have to read it :(
      val content = Source.fromInputStream(getClass.getResourceAsStream(path), "utf-8")
      reader.csv(content.getLines().toList.toDS())
    }
  }
}
