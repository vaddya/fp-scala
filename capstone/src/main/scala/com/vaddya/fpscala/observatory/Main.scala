package com.vaddya.fpscala.observatory

import java.io.File

object Main extends App {
  val temperatureFile = "/2015.csv"
  val stationsFile = "/stations.csv"
  val records = Extraction.locateTemperatures(2015, stationsFile, temperatureFile)
  val temperatures = Extraction.locationYearlyAverageRecords(records)
  
  val start = System.currentTimeMillis()
  val img = Interaction.tile(temperatures, TemperatureColors, Tile(60, 30, 2))
  println(s"Took ${System.currentTimeMillis() - start} ms")
  val file = new File("tile.png")
  println(file.getAbsolutePath)
  img.output(file)
}
