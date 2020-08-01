package com.vaddya.fpscala.observatory

import com.sksamuel.scrimage.{Image, Pixel}

import scala.annotation.tailrec

/**
  * 3rd milestone: interactive visualization
  */
object Interaction extends InteractionInterface {
  val ImageWidth = 256
  val ImageHeight = 256
  val Alpha = 127

  /**
    * @param tile Tile coordinates
    * @return The latitude and longitude of the top-left corner of the tile, as per http://wiki.openstreetmap.org/wiki/Slippy_map_tilenames
    */
  def tileLocation(tile: Tile): Location = Location(
    latitude(tile.y, tile.zoom),
    longitude(tile.x, tile.zoom)
  )

  /**
    * @param temperatures Known temperatures
    * @param colors       Color scale
    * @param tile         Tile coordinates
    * @return A 256×256 image showing the contents of the given tile
    */
  def tile(
    temperatures: Iterable[(Location, Temperature)],
    colors: Iterable[(Temperature, Color)],
    tile: Tile
  ): Image = {
    def pixel(tile: Tile): (Int, Int, Pixel) = {
      val location = tileLocation(tile)
      val temp = Visualization.predictTemperature(temperatures, location)
      val color = Visualization.interpolateColor(colors, temp)
      (tile.x, tile.y, Pixel(color.red, color.green, color.blue, Alpha))
    }

    @tailrec
    def generateSubtiles(tiles: Seq[Tile], depth: Int): Seq[Tile] =
      if (depth == 0) tiles
      else generateSubtiles(tiles.flatMap(getSubtiles), depth - 1)

    val pixels = generateSubtiles(Seq(tile), 8)
      .par
      .map(pixel)
      .seq
      .sortBy { case (x, y, _) => (y, x) }
      .map { case (_, _, pixel) => pixel }
      .toArray
    Image(ImageWidth, ImageHeight, pixels)
  }

  /**
    * Generates all the tiles for zoom levels 0 to 3 (included), for all the given years.
    *
    * @param yearlyData    Sequence of (year, data), where `data` is some data associated with
    *                      `year`. The type of `data` can be anything.
    * @param generateImage Function that generates an image given a year, a zoom level, the x and
    *                      y coordinates of the tile and the data to build the image from
    */
  def generateTiles[Data](
    yearlyData: Iterable[(Year, Data)],
    generateImage: (Year, Tile, Data) => Unit
  ): Unit = {
    val inputs = for {
      (year, data) <- yearlyData
      zoom <- ZoomRange
      tile <- generateTiles(zoom)
    } yield (year, tile, data)
    inputs.par.foreach(generateImage.tupled)
  }

  /**
    * Computes the longitude by the given X coordinate and zoom level
    */
  private def longitude(x: Double, zoom: Int): Double =
    x * 360.0 / tilesNum(zoom) - 180.0

  /**
    * Computes the latitude by the given Y coordinate and zoom level
    *
    * @see https://en.wikipedia.org/wiki/Web_Mercator_projection
    */
  private def latitude(y: Double, zoom: Int): Double =
    math.atan(math.sinh(math.Pi * (1 - 2 * y / tilesNum(zoom)))).toDegrees

  /**
    * Computes the number of tiles by the given zoom level
    *
    * @see https://en.wikipedia.org/wiki/Web_Mercator_projection
    **/
  private def tilesNum(zoom: Int): Int = math.pow(2, zoom).toInt

  /**
    * @see https://wiki.openstreetmap.org/wiki/Slippy_map_tilenames#Subtiles
    */
  def getSubtiles(tile: Tile): Seq[Tile] = Seq(
    Tile(tile.x * 2, tile.y * 2, tile.zoom + 1),
    Tile(tile.x * 2 + 1, tile.y * 2, tile.zoom + 1),
    Tile(tile.x * 2, tile.y * 2 + 1, tile.zoom + 1),
    Tile(tile.x * 2 + 1, tile.y * 2 + 1, tile.zoom + 1)
  )

  /**
    * Generates all tiles for a given zoom level
    */
  private def generateTiles(zoom: Int): Seq[Tile] = {
    val tiles = tilesNum(zoom)
    for {
      y <- 0 until tiles
      x <- 0 until tiles
    } yield Tile(x, y, zoom)
  }
}
