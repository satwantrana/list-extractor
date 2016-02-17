package edu.iitd.nlp.ListExtraction

import scala.collection.mutable

case class FeatureVector(vec: mutable.ArrayBuffer[Double] = FeatureVector.Default().vec) {
  def +(other: FeatureVector): FeatureVector = {
    require(this.vec.length == other.vec.length)
    FeatureVector(vec.zip(other.vec).map { case (a, b) => a + b })
  }

  def -(other: FeatureVector): FeatureVector = {
    require(this.vec.length == other.vec.length)
    FeatureVector(vec.zip(other.vec).map { case (a, b) => a - b })
  }

  def *(other: FeatureVector): Double = {
    require(this.vec.length == other.vec.length)
    vec.zip(other.vec).map { case (a, b) => a * b }.sum
  }

  def *(mult: Double): FeatureVector = {
    FeatureVector(vec.map(_ * mult))
  }

  def /(div: Double): FeatureVector = {
    FeatureVector(vec.map(_ / div))
  }

  def ==(other: FeatureVector): Boolean = {
    require(this.vec.length == other.vec.length)
    this.vec == other.vec || (this - other).vec.map(Math.abs).max < FeatureVector.eps
  }

  def normalised: FeatureVector = {
    if (this.vec.sum == 0) this
    else this / this.vec.sum
  }
}

object FeatureVector {
  val eps = 1e-6
  val defaultNumFeatures = 6
  def Default(n: Int = defaultNumFeatures) = {
    val res = Zeros(n)
    res.vec(1) = 1.0
    res
  }
  def Zeros(n: Int = defaultNumFeatures) = FeatureVector(mutable.ArrayBuffer.fill(n)(0.0))
  def NegativeInfinities(n: Int = defaultNumFeatures) = FeatureVector(mutable.ArrayBuffer.fill(n)(Double.NegativeInfinity))
  def baseLine(n: Int = defaultNumFeatures) = {
    require(n == defaultNumFeatures)
    val res = Zeros(n)
    res.vec(4) = -1.0
    res.vec(5) = -1.0
    res
  }
  def syntacticSimilarity(n: Int = defaultNumFeatures) = {
    val res = Zeros(n)
    //    res.vec(2) = 1.0
    res.vec(3) = 1.0
    res
  }
  def bagOfWordsSimilarity(n: Int = defaultNumFeatures) = {
    val res = Zeros(n)
    res.vec(0) = 1.0
    res
  }
}

case class Params(leftDis: Int = 0, rightDis: Int = 0)