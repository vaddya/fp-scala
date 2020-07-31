package com.vaddya.fpscala.observatory

import com.sksamuel.scrimage.{Image, Pixel}

import scala.annotation.tailrec

/**
  * 5th milestone: value-added information visualization
  */
object Visualization2 extends Visualization2Interface {

  val ImageWidth = 256
  val ImageHeight = 256
  val Alpha = 127
  val Colors = Seq(
    (7d,  Color(0,   0,   0)),
    (4d,  Color(255, 0,   0)),
    (2d,  Color(255, 255, 0)),
    (0d,  Color(255, 255, 255)),
    (-2d, Color(0,   255, 255)),
    (-7d, Color(0,   0,   255)),
  )

  /**
    * @param point (x, y) coordinates of a point in the grid cell
    * @param d00   Top-left value
    * @param d01   Bottom-left value
    * @param d10   Top-right value
    * @param d11   Bottom-right value
    * @return A guess of the value at (x, y) based on the four known values, using bilinear interpolation
    *         See https://en.wikipedia.org/wiki/Bilinear_interpolation#Unit_Square
    * @see https://en.wikipedia.org/wiki/Bilinear_interpolation
    */
  def bilinearInterpolation(
    point: CellPoint,
    d00: Temperature,
    d01: Temperature,
    d10: Temperature,
    d11: Temperature
  ): Temperature = {
    val (x, y) = (point.x, point.y)
    d00 * (1 - x) * (1 - y) +
      d10 * x * (1 - y) +
      d01 * (1 - x) * y +
      d11 * x * y
  }

  /**
    * @param grid   Grid to visualize
    * @param colors Color scale to use
    * @param tile   Tile coordinates to visualize
    * @return The image of the tile at (x, y, zoom) showing the grid using the given color scale
    */
  def visualizeGrid(
    grid: GridLocation => Temperature,
    colors: Iterable[(Temperature, Color)],
    tile: Tile
  ): Image = {
    def pixel(tile: Tile): Pixel = {
      val location = Interaction.tileLocation(tile)
      val cellPoint = CellPoint(
        location.lat - location.lat.floor,
        location.lon - location.lon.floor
      )
      val temp = bilinearInterpolation(
        cellPoint,
        grid(point(location, math.floor, math.floor)),
        grid(point(location, math.floor, math.ceil)),
        grid(point(location, math.ceil, math.floor)),
        grid(point(location, math.ceil, math.ceil)),
      )
      val color = Visualization.interpolateColor(colors, temp)
      Pixel(color.red, color.green, color.blue, Alpha)
    }

    @tailrec
    def generateSubtiles(tiles: Seq[Tile], depth: Int): Seq[Tile] =
      if (depth == 0) tiles
      else generateSubtiles(tiles.flatMap(Interaction.getSubtiles), depth - 1)

    val pixels = generateSubtiles(Seq(tile), 8)
      .sortBy { case Tile(x, y, _) => (y, x) }
      .map(pixel)
      .toArray
    Image(ImageWidth, ImageHeight, pixels)
  }

  private def point(
    location: Location,
    latFun: Double => Double,
    locFun: Double => Double
  ): GridLocation = {
    @tailrec
    def fix(gridLocation: GridLocation): GridLocation = {
      if (gridLocation.lat < -89)       fix(gridLocation.copy(lat = 90))
      else if (gridLocation.lat > 90)   fix(gridLocation.copy(lat = -89))
      else if (gridLocation.lon < -180) fix(gridLocation.copy(lon = 179))
      else if (gridLocation.lon > 179)  fix(gridLocation.copy(lon = -180))
      else gridLocation
    }

    fix(GridLocation(
      latFun(location.lat).toInt,
      locFun(location.lon).toInt
    ))
  }

}
