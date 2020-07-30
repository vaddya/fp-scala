package com.vaddya.fpscala.observatory

import java.io.File

object Main extends App {
  val temperatureFile = "/2015.csv"
  val stationsFile = "/stations.csv"
  val records = Extraction.locateTemperatures(2015, stationsFile, temperatureFile)
  val avg = Extraction.locationYearlyAverageRecords(records)
  val start = System.currentTimeMillis()
  val img = Visualization.visualize(avg, Visualization.Colors)
  println(s"Took ${System.currentTimeMillis() - start} ms")
  val file = new File("2015.png")
  println(file.getAbsolutePath)
  img.output(file)
}
