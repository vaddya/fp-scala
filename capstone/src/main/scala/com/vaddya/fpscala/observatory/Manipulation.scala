package com.vaddya.fpscala.observatory

/**
  * 4th milestone: value-added information
  */
object Manipulation extends ManipulationInterface {

  /**
    * @param temperatures Known temperatures
    * @return A function that, given a latitude in [-89, 90] and a longitude in [-180, 179],
    *         returns the predicted temperature at this location
    */
  def makeGrid(temperatures: Iterable[(Location, Temperature)]): GridLocation => Temperature = {
    val tuples = for {
      lat <- -89 to 90
      loc <- -180 to 179
      location = Location(lat, loc)
      gridLocation = GridLocation(lat, loc)
    } yield {
      (gridLocation, Visualization.predictTemperature(temperatures, location))
    }
    tuples.toMap
  }

  /**
    * @param temperaturess Sequence of known temperatures over the years (each element of the collection
    *                      is a collection of pairs of location and temperature)
    * @return A function that, given a latitude and a longitude, returns the average temperature at this location
    */
  def average(temperaturess: Iterable[Iterable[(Location, Temperature)]]): GridLocation => Temperature = {
    val temperatures = for {
      yearTemperatures <- temperaturess
    } yield {
      makeGrid(yearTemperatures)
    }
    val tuples = for {
      lat <- -89 to 90
      loc <- -180 to 179
      gridLocation = GridLocation(lat, loc)
    } yield {
      val values = temperatures.map(_.apply(gridLocation))
      (gridLocation, values.sum / values.size)
    }
    tuples.toMap
  }

  /**
    * @param temperatures Known temperatures
    * @param normals A grid containing the “normal” temperatures
    * @return A grid containing the deviations compared to the normal temperatures
    */
  def deviation(temperatures: Iterable[(Location, Temperature)], normals: GridLocation => Temperature): GridLocation => Temperature = {
    val current = makeGrid(temperatures)
    location => current(location) - normals(location)
  }
}

