/***********************************************************************
 * Copyright (c) 2013-2017 Commonwealth Computer Research, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Apache License, Version 2.0
 * which accompanies this distribution and is available at
 * http://www.opensource.org/licenses/apache2.0.php.
 ***********************************************************************/

package org.locationtech.geomesa.filter

import org.locationtech.geomesa.filter.Bounds.Bound

/**
  * Single typed bound. If filter is unbounded on one or both sides, the associated bound will be None.
  *
  * For example, bounds for 'foo < 5' would be (None, Some(5))
  * Special case for 'foo NOT NULL' will have both bounds be None
  *
  * @param lower lower bound, if any
  * @param upper upper bound, if any
  * @tparam T binding of the attribute type
  */
case class Bounds[T](lower: Bound[T], upper: Bound[T]) {

  def bounds: (Option[T], Option[T]) = (lower.value, upper.value)

  /**
    * Bounded on at least one side
    *
    * @return
    */
  def isBounded: Boolean = lower.value.nonEmpty || upper.value.nonEmpty

  /**
    * Bounded on both sides
    *
    * @return
    */
  def isBoundedBothSides: Boolean = lower.value.nonEmpty && upper.value.nonEmpty

  /**
    * Only covers a single exact value
    *
    * @return
    */
  def isExact: Boolean = lower.value.isDefined && lower.value == upper.value

  override def toString: String = {
    (if (lower.inclusive) { "[" } else { "(" }) + lower.value.getOrElse("-\u221E") + "," +
      upper.value.getOrElse("+\u221E") + (if (upper.inclusive) { "]" } else { ")" })
  }
}

object Bounds {

  /**
    * Single bound (lower or upper).
    *
    * Bound may be unbounded, in which case value is None. Note by convention unbounded bounds are exclusive
    *
    * @param value value of this bound, if bounded
    * @param inclusive whether the bound is inclusive or exclusive.
    *                  for example, 'foo < 5' is exclusive, 'foo <= 5' is inclusive
    */
  case class Bound[T](value: Option[T], inclusive: Boolean) {
    def exclusive: Boolean = !inclusive
  }

  object Bound {
    private val unboundedBound = Bound[Any](None, inclusive = false)
    def unbounded[T]: Bound[T] = unboundedBound.asInstanceOf[Bound[T]]
  }

  private val allValues = Bounds(Bound.unbounded, Bound.unbounded)

  def everything[T]: Bounds[T] = allValues.asInstanceOf[Bounds[T]]

  /**
    * Gets the smaller value between two lower bounds, taking into account exclusivity.
    * If the bounds are equal, the first bound will always be returned
    *
    * @param bound1 first bound
    * @param bound2 second bound
    * @return smaller bound
    */
  def smallerLowerBound[T](bound1: Bound[T], bound2: Bound[T]): Bound[T] = {
    if (bound1.value.isEmpty) {
      bound1
    } else if (bound2.value.isEmpty) {
      bound2
    } else {
      val c = bound1.value.get.asInstanceOf[Comparable[Any]].compareTo(bound2.value.get)
      if (c < 0 || (c == 0 && (bound1.inclusive || bound2.exclusive))) { bound1 } else { bound2 }
    }
  }

  /**
    * Gets the larger value between two upper bounds, taking into account exclusivity.
    * If the bounds are equal, the first bound will always be returned
    *
    * @param bound1 first bound
    * @param bound2 second bound
    * @return larger bound
    */
  def largerUpperBound[T](bound1: Bound[T], bound2: Bound[T]): Bound[T] = {
    if (bound1.value.isEmpty) {
      bound1
    } else if (bound2.value.isEmpty) {
      bound2
    } else {
      val c = bound1.value.get.asInstanceOf[Comparable[Any]].compareTo(bound2.value.get)
      if (c > 0 || (c == 0 && (bound1.inclusive || bound2.exclusive))) { bound1 } else { bound2 }
    }
  }

  /**
    * Gets the smaller value between two upper bounds, taking into account exclusivity.
    * If the bounds are equal, the first bound will always be returned
    *
    * @param bound1 first bound
    * @param bound2 second bound
    * @return smaller bound
    */
  def smallerUpperBound[T](bound1: Bound[T], bound2: Bound[T]): Bound[T] = {
    if (bound2.value.isEmpty) {
      bound1
    } else if (bound1.value.isEmpty) {
      bound2
    } else {
      val c = bound1.value.get.asInstanceOf[Comparable[Any]].compareTo(bound2.value.get)
      if (c < 0 || (c == 0 && (bound2.inclusive || bound1.exclusive))) { bound1 } else { bound2 }
    }
  }

  /**
    * Gets the larger value between two upper bounds, taking into account exclusivity.
    * If the bounds are equal, the first bound will always be returned
    *
    * @param bound1 first bound
    * @param bound2 second bound
    * @return larger bound
    */
  def largerLowerBound[T](bound1: Bound[T], bound2: Bound[T]): Bound[T] = {
    if (bound2.value.isEmpty) {
      bound1
    } else if (bound1.value.isEmpty) {
      bound2
    } else {
      val c = bound1.value.get.asInstanceOf[Comparable[Any]].compareTo(bound2.value.get)
      if (c > 0 || (c == 0 && (bound2.inclusive || bound1.exclusive))) { bound1 } else { bound2 }
    }
  }

  /**
    * Takes the intersection of two bounds. If they are disjoint, will return None.
    *
    * @param left first bounds
    * @param right second bounds
    * @tparam T type parameter
    * @return intersection
    */
  def intersection[T](left: Bounds[T], right: Bounds[T]): Option[Bounds[T]] = {
    val lower = largerLowerBound(left.lower, right.lower)
    val upper = smallerUpperBound(right.upper, left.upper)
    (lower.value, upper.value) match {
      case (Some(lo), Some(up)) if lo.asInstanceOf[Comparable[Any]].compareTo(up) > 0 => None
      case _ => Some(Bounds(lower, upper))
    }
  }

  /**
    * Takes the union of two bound sequences. Naive implementation that just concatenates
    *
    * @param left first bounds
    * @param right second bounds
    * @tparam T type parameter
    * @return union
    */
  def union[T](left: Seq[Bounds[T]], right: Seq[Bounds[T]]): Seq[Bounds[T]] = left ++ right
}