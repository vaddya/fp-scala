package com.vaddya.fpscala.observatory

import java.sql.Date
import java.time.LocalDate

import org.apache.log4j.{Level, Logger}
import org.apache.spark.sql.Row
import org.apache.spark.sql.functions._
import org.apache.spark.sql.types._

/**
  * 1st milestone: data extraction
  */
object Extraction extends ExtractionInterface with ExtractionLogic {
  Logger.getLogger("org.apache.spark").setLevel(Level.WARN)

  import spark.implicits._

  def locateTemperatures(year: Year, stationsFile: String, temperaturesFile: String): Iterable[(LocalDate, Location, Temperature)] = {
    locateTemperaturesDf(year, stationsFile, temperaturesFile)
      .collect
      .map(row => (
        row.getAs[Date]("date").toLocalDate,
        rowToLocation(row.getAs[Row]("location")),
        row.getAs[Double]("temp")
      ))
  }

  /**
    * @param records A sequence containing triplets (date, location, temperature)
    * @return A sequence containing, for each location, the average temperature over the year.
    */
  def locationYearlyAverageRecords(records: Iterable[(LocalDate, Location, Temperature)]): Iterable[(Location, Temperature)] = {
    val fixedRecords = records.map { case (date, loc, temp) =>
      Row(Date.valueOf(date), loc.lat, loc.lon, temp)
    }
    val rdd = spark.sparkContext.parallelize(fixedRecords.toSeq)
    val schema = StructType(
      StructField("date", DateType) ::
        StructField("lat", DoubleType) ::
        StructField("lon", DoubleType) ::
        StructField("temp", DoubleType) ::
        Nil)
    val recordsDf = spark.createDataFrame(rdd, schema)
      .withColumn("location", struct($"lat", $"lon"))
      .select("date", "location", "temp")

    locationYearlyAverageRecordsDf(recordsDf)
      .collect
      .map(row => (
        rowToLocation(row.getAs[Row]("location")),
        row.getAs[Double]("avg_temp")
      ))
  }

  private def rowToLocation(row: Row): Location = Location(
    row.getAs[Double]("lat"),
    row.getAs[Double]("lon")
  )
}
