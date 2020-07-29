package com.vaddya.fpscala.observatory

object Main extends App {
  val temperatureFile = "/2015.csv"
  val stationsFile = "/stations.csv"
  val records = Extraction.locateTemperaturesDf(2015, stationsFile, temperatureFile)
  records.show(100)
  records.printSchema()
  println(records.count)
  val avg = Extraction.locationYearlyAverageRecordsDf(records)
  avg.show(100)
  println(avg.count)
  avg.printSchema()
}
