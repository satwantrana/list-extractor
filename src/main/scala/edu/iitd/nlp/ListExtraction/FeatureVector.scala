package edu.iitd.nlp.ListExtraction

import scala.collection.mutable

case class FeatureVector(vec: mutable.ArrayBuffer[Double] = FeatureVector.Default.vec) {
  def +(other: FeatureVector): FeatureVector = {
    FeatureVector(vec.zip(other.vec).map { case (a, b) => a + b })
  }

  def -(other: FeatureVector): FeatureVector = {
    FeatureVector(vec.zip(other.vec).map { case (a, b) => a - b })
  }

  def *(other: FeatureVector): Double = {
    vec.zip(other.vec).map { case (a, b) => a * b }.sum
  }

  def *(mult: Double): FeatureVector = {
    FeatureVector(vec.map(_ * mult))
  }

  def /(div: Double): FeatureVector = {
    FeatureVector(vec.map(_ / div))
  }

  def ==(other: FeatureVector): Boolean = {
    this.vec == other.vec || (this - other).vec.map(Math.abs).max < FeatureVector.eps
  }

  def normalised: FeatureVector = {
    if (this.vec.sum == 0) this
    else this / this.vec.sum
  }
}

object FeatureVector {
  val eps = 1e-6
  val Default = FeatureVector(mutable.ArrayBuffer(0.0, 1.0, 0.0, 0.0))
  val Zeros = FeatureVector(mutable.ArrayBuffer.fill(4)(0.0))
  val NegativeInfinities = FeatureVector(mutable.ArrayBuffer.fill(4)(Double.NegativeInfinity))
}
