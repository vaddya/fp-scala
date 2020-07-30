package com.vaddya.fpscala.observatory

import com.sksamuel.scrimage.{Image, Pixel}

/**
  * 2nd milestone: basic visualization
  */
object Visualization extends VisualizationInterface {

  val EarthRadius = 6371
  val DoubleCompareEps = 1e-5
  val DistanceEps = 1
  val ImageWidth = 360
  val ImageHeight = 180
  val Alpha = 255
  val Colors = Seq(
    (60d,  Color(255, 255,	255)),
    (32d,  Color(255, 0,	  0)),
    (12d,  Color(255, 255,	0)),
    (0d,   Color(0,   255,	255)),
    (-15d, Color(0,   0,	  255)),
    (-27d, Color(255, 0,	  255)),
    (-50d, Color(33,  0,	  107)),
    (-60d, Color(0,   0,	  0))
  )

  /**
    * @param temperatures Known temperatures: pairs containing a location and the temperature at this location
    * @param location     Location where to predict the temperature
    * @return The predicted temperature at `location`
    */
  def predictTemperature(temperatures: Iterable[(Location, Temperature)], location: Location): Temperature = {
    val distanced = temperatures
      .map { case (loc, temp) => (distance(loc, location), temp) }
    val near = distanced
      .filter { case (dist, _) => dist < DistanceEps }
    if (near.nonEmpty) near.head._2
    else {
      val (weightSum, weightedTempSum) = distanced
        .map { case (dist, temp) => (distanceWeight(dist), temp) }
        .foldLeft((0.0, 0.0)) { case ((weightSum, weightedTempSum), (weight, temp)) =>
          (weightSum + weight, weightedTempSum + weight * temp)
        }
      weightedTempSum / weightSum
    }
  }

  /**
    * @param points Pairs containing a value and its associated color
    * @param value  The value to interpolate
    * @return The color that corresponds to `value`, according to the color scale defined by `points`
    */
  def interpolateColor(points: Iterable[(Temperature, Color)], value: Temperature): Color = {
    val sorted = points.toArray.sortBy { case (temp, _) => temp }
    val aboveIdx = sorted.indexWhere { case (temp, _) => temp > value }
    if (aboveIdx == -1) sorted.last._2
    else if (aboveIdx == 0) sorted.head._2
    else {
      val (belowT, belowC) = sorted(aboveIdx - 1)
      val (aboveT, aboveC) = sorted(aboveIdx)
      Color(
        interpolate(belowT, belowC.red,   aboveT, aboveC.red,   value),
        interpolate(belowT, belowC.green, aboveT, aboveC.green, value),
        interpolate(belowT, belowC.blue,  aboveT, aboveC.blue,  value)
      )
    }
  }

  /**
    * @param temperatures Known temperatures
    * @param colors       Color scale
    * @return A 360×180 image where each pixel shows the predicted temperature at its location
    */
  def visualize(temperatures: Iterable[(Location, Temperature)], colors: Iterable[(Temperature, Color)]): Image = {
    val coords = for {
      y <- 0 until ImageHeight
      x <- 0 until ImageWidth
    } yield (x, y)
    val pixels = new Array[Pixel](ImageWidth * ImageHeight)
    coords.par.foreach { case (x, y) =>
      val location = Location(90 - y, x - 180)
      val temp = predictTemperature(temperatures, location)
      val color = interpolateColor(colors, temp)
      pixels(x + y * ImageWidth) = Pixel(color.red, color.green, color.blue, Alpha)
    }
    Image(ImageWidth, ImageHeight, pixels)
  }

  /**
    * @see https://en.wikipedia.org/wiki/Linear_interpolation
    */
  def interpolate(x0: Temperature, y0: Int, x1: Temperature, y1: Int, x: Temperature): Int =
    ((y0 * (x1 - x) + y1 * (x - x0)) / (x1 - x0)).round.toInt

  /**
    * @see https://en.wikipedia.org/wiki/Inverse_distance_weighting
    */
  def distanceWeight(dist: Double): Double = 1 / math.pow(dist, 6)

  /**
    * @see https://en.wikipedia.org/wiki/Great-circle_distance
    */
  def distance(a: Location, b: Location): Double = {
    import math._
    val ar = Location(a.lat * Pi / 180, a.lon * Pi / 180)
    val br = Location(b.lat * Pi / 180, b.lon * Pi / 180)
    val angle =
      if (isSame(ar, br)) 0
      else if (isAntipodes(ar, br)) Pi
      else acos(
        sin(ar.lat) * sin(br.lat) + cos(ar.lat) * cos(br.lat) * cos(abs(ar.lon - br.lon))
      )
    angle * EarthRadius
  }

  private def isSame(a: Location, b: Location): Boolean = ~=(a.lat, b.lat) && ~=(a.lon, b.lon)

  private def isAntipodes(a: Location, b: Location): Boolean =
    ~=(a.lat, b.lat) && (~=(180 - b.lon, a.lon) || ~=(180 - a.lon, b.lon))

  private def ~=(x: Double, y: Double): Boolean = math.abs(x - y) < DoubleCompareEps
}
