package com.vaddya.fpscala.observatory

import org.junit.Assert._
import org.junit.Test

trait VisualizationTest extends MilestoneSuite {
  private val milestoneTest = namedMilestoneTest("raw data display", 2) _
  private val eps = 1e-5
  
  @Test
  def testPredict(): Unit = milestoneTest {
    val temperatures = Seq(
      (Location(2, 2), 10.0),
      (Location(-2, -2), 20.0),
    )
    val predict = Visualization.predictTemperature(temperatures, Location(0, 0))
    assertEquals(15.0, predict, eps)
  }

  @Test
  def testPredictNear(): Unit = milestoneTest {
    val temperatures = Seq(
      (Location(2, 2), 10.0),
      (Location(-2, -2), 20.0),
      (Location(0.2, 0.2), 17.0)
    )
    val predict = Visualization.predictTemperature(temperatures, Location(0, 0))
    assertEquals(17.0, predict, eps)
  }
  
  @Test
  def testInterpolate(): Unit = milestoneTest {    
    assertEquals(Color(255, 127, 127), Visualization.interpolateColor(Visualization.Colors, 46))
    assertEquals(Color(0, 255, 255), Visualization.interpolateColor(Visualization.Colors, 0))
  }
  
  @Test
  def testDistance(): Unit = milestoneTest {
    // https://www.movable-type.co.uk/scripts/latlong.html
    assertEquals(0, distance(50, 5, 50, 5), 1)
    assertEquals(7, distance(50, 5, 50, 5.1), 1)
    assertEquals(899, distance(50, 5, 58, 3), 1)
    assertEquals(11120, distance(-50, 5, 50, 5), 1)
  }

  private def distance(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double = {
    Visualization.distance(Location(lat1, lon1), Location(lat2, lon2))
  }
}
