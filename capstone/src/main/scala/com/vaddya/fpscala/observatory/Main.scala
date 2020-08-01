package com.vaddya.fpscala.observatory

import java.io.File

import com.sksamuel.scrimage.Image

object Main extends App {
  val StationsFile = "/stations.csv"

  var temperaturess: Seq[(Year, Iterable[(Location, Temperature)])] = Nil
  measure("Find average temperature for each location") {
    temperaturess = TemperatureYearsRange
      .map(year => measure(s"Find average temperature in $year") {
        val records = Extraction.locateTemperatures(year, StationsFile, s"/$year.csv")
        val temperatures = Extraction.locationYearlyAverageRecords(records)
        year -> temperatures
      })
  }

  measure("Generate all temperature tiles") {
    Interaction.generateTiles[Iterable[(Location, Temperature)]](
      temperaturess,
      (year, tile, data) => measure(s"Generate temperature tile for $tile") {
        val img = Interaction.tile(data, TemperatureColors, tile)
        val file = createFile("temperatures", tile, year)
        saveImage(img, file)
      }
    )
  }

  measure("Generate all deviation tiles") {
    val (left, right) = temperaturess
      .partition { case (year, _) => DeviationNormalYearsRange contains year }
    val normals = left.map { case (_, data) => data }
    val average = Manipulation.average(normals)
    val deviations = right
      .par
      .map { case (year, data) => (year, Manipulation.deviation(data, average)) }
      .seq
    Interaction.generateTiles[GridLocation => Temperature](
      deviations,
      (year, tile, data) => measure(s"Generate deviation tile for $tile") {
        val img = Visualization2.visualizeGrid(data, DeviationColors, tile)
        val file = createFile("deviations", tile, year)
        saveImage(img, file)
      }
    )
  }

  def measure[T](name: String)(block: => T): T = {
    val start = System.currentTimeMillis()
    val res = block
    val seconds = (System.currentTimeMillis() - start) / 1000
    println(s"$name completed in $seconds s")
    res
  }

  def createFile(stage: String, tile: Tile, year: Year): File = {
    val (x, y, z) = (tile.x, tile.y, tile.zoom)
    val file = new File(s"capstone/target/$stage/$year/$z/$x-$y.png")
    file.getParentFile.mkdirs()
    file
  }

  def saveImage(img: Image, file: File): Unit = {
    println(file.getAbsolutePath)
    img.output(file)
  }
}
