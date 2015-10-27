package edu.iitd.nlp.ListExtraction

import org.allenai.common.LoggingWithUncaughtExceptions
import org.scalatest._

import scala.collection.mutable

class TestFeatureVector extends FlatSpec with LoggingWithUncaughtExceptions {
  "FeatureVector" should "add correctly" in {
    val a = FeatureVector(mutable.ArrayBuffer(1.0, 2.0, 3.0, 4.0))
    val b = FeatureVector(mutable.ArrayBuffer(1.0, 2.0, 3.0, 4.0))
    val c = FeatureVector(mutable.ArrayBuffer(2.0, 4.0, 6.0, 8.0))
    assert(a + b == c)
  }

  it should "subtract correctly" in {
    val a = FeatureVector(mutable.ArrayBuffer(1.0, 2.0, 3.0, 4.0))
    val b = FeatureVector(mutable.ArrayBuffer(1.0, 2.0, 3.0, 4.0))
    assert(a - b == FeatureVector.Zeros)
  }

  it should "dot product correctly" in {
    val a = FeatureVector(mutable.ArrayBuffer(1.0, 2.0, 3.0, 4.0))
    val b = FeatureVector(mutable.ArrayBuffer(1.0, 2.0, 3.0, 4.0))
    assert(a * b == 30.0)
  }

  it should "multiply correctly" in {
    val a = FeatureVector(mutable.ArrayBuffer(1.0, 2.0, 3.0, 4.0))
    val b = FeatureVector(mutable.ArrayBuffer(0.1, 0.2, 0.3, 0.4))
    assert(a * 0.1 == b)
  }

  it should "divide correctly" in {
    val a = FeatureVector(mutable.ArrayBuffer(1.0, 2.0, 3.0, 4.0))
    val b = FeatureVector(mutable.ArrayBuffer(0.1, 0.2, 0.3, 0.4))
    assert(a / 10 == b)
  }
}
