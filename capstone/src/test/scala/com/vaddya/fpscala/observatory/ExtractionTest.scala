package com.vaddya.fpscala.observatory

import java.time.LocalDate

import org.junit.Assert._
import org.junit.Test

trait ExtractionTest extends MilestoneSuite {
  private val milestoneTest = namedMilestoneTest("data extraction", 1) _

  @Test
  def testRead(): Unit = milestoneTest {
    val records = Extraction.locateTemperatures(2015, "/stations.csv", "/2015.csv").toSeq
    assertEquals(3, records.size)
    assertEquals((LocalDate.of(2015, 8, 11), Location(37.35, -78.433), 27.3), records(2))
  }

  @Test
  def testAverage(): Unit = milestoneTest {
    val records = Seq(
      (LocalDate.of(2015, 8, 11), Location(37.35, -78.433), 27.3),
      (LocalDate.of(2015, 12, 6), Location(37.358, -78.438), 0.0),
      (LocalDate.of(2015, 1, 29), Location(37.358, -78.438), 2.0)
    )
    val averageRecords = Extraction.locationYearlyAverageRecords(records).toSeq
    assertEquals(2, averageRecords.size)
    assertEquals((Location(37.358, -78.438), 1.0), averageRecords(1))
  }
}
