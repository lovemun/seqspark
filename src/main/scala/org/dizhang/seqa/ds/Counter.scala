package org.dizhang.seqa.ds

import breeze.linalg.DenseVector
import it.unimi.dsi.fastutil.ints.Int2IntOpenHashMap
import org.dizhang.seqa.ds.Counter.CounterElementSemiGroup
//import collection.mutable.{IndexedSeq, Map}
//import scala.collection.mutable

/**
 * This class borrows a lot from the AlgeBird AdaptiveVector
 */

object Counter {
  object CounterElementSemiGroup {
    type IMap = Int2IntOpenHashMap
    type PairInt = (Int, Int)
    type TripleInt = (Int, Int, Int)
    type PairDouble = (Double, Double)
    type TripleDouble = (Double, Double, Double)
    type MapCounter = Map[PairInt, Int]
    implicit object AtomInt extends CounterElementSemiGroup[Int] {
      def zero = 0
      def op (x: Int, y: Int) = x + y
      def pow (x: Int, i: Int) = x * i
    }
    implicit object PairInt extends CounterElementSemiGroup[PairInt] {
      def zero = (0, 0)
      def op (x: PairInt, y: PairInt) = (x._1 + y._1, x._2 + y._2)
      def pow(x: PairInt, i: Int) = (x._1 * i, x._2 * i)
    }
    implicit object TripleInt extends CounterElementSemiGroup[TripleInt] {
      def zero = (0, 0, 0)
      def op (x: TripleInt, y: TripleInt) =
        (x._1 + y._1, x._2 + y._2, x._3 + y._3)
      def pow (x: TripleInt, i:Int) =
        (x._1 * i, x._2 * i, x._3 * i)
    }
    implicit object AtomDouble extends CounterElementSemiGroup[Double] {
      def zero = 0
      def op (x: Double, y: Double) = x + y
      def pow(x: Double, i: Int) = x * i
    }
    implicit object PairDouble extends CounterElementSemiGroup[PairDouble] {
      def zero = (0, 0)
      def op (x: PairDouble, y: PairDouble) = (x._1 + y._1, x._2 + y._2)
      def pow (x: PairDouble, i: Int) = (x._1 * i, x._2 * i)
    }
    implicit object TripleDouble extends CounterElementSemiGroup[TripleDouble] {
      def zero = (0, 0, 0)
      def op (x: TripleDouble, y: TripleDouble) =
        (x._1 + y._1, x._2 + y._2, x._3 + y._3)
      def pow (x: TripleDouble, i:Int) =
        (x._1 * i, x._2 * i, x._3 * i)
    }

    implicit object MapCounter extends CounterElementSemiGroup[MapCounter] {
      def zero = Map[PairInt, Int]()
      def op (x: MapCounter, y: MapCounter) =
        x ++ (for ((k, v) <- y) yield k -> (v + x.getOrElse(k, 0)))
      def pow (x: MapCounter, i: Int) =
        for ((k,v) <- x) yield k -> (v * i)
    }

    implicit object MapI2I extends CounterElementSemiGroup[IMap] {
      def zero = new Int2IntOpenHashMap()
      def op (x: IMap, y: IMap): IMap = {
        val res = new Int2IntOpenHashMap()
        res.putAll(x)
        val yi = y.keySet.iterator
        while (yi.hasNext) {
          val key = yi.next
          res.addTo(key, y.get(key))
        }
        res
      }
      def pow (x: IMap, i: Int) = {
        val res = new CounterElementSemiGroup.IMap()
        val xi = x.keySet.iterator
        while (xi.hasNext) {
          val key = xi.next
          res.addTo(key, x.get(key) * i)
        }
        res
      }
    }
    /**
      implicit object MapAtom extends CounterElementSemiGroup[Map[String, Double]] {
        def op (x: Map[String, Double], y: Map[String, Double]) =
          x ++ (for ((k, v) <- y) yield k -> (v + x.getOrElse(k, 0.0)))
      }
      implicit object MapPair extends CounterElementSemiGroup[Map[String, (Double, Double)]] {
        def op (x: Map[String, (Double, Double)], y: Map[String, (Double, Double)]) =
          x ++ (
            for { (k, yv) <- y; xv = x.getOrElse(k, (0.0, 0.0)) }
            yield k -> ((yv._1 + xv._1, yv._2 + xv._2))
          )
      }
      */


  }

  @SerialVersionUID(3L)
  trait CounterElementSemiGroup[A] extends Serializable {
    /** def zero here to help sparse operation
      * Note that zero is NOT necessarily the default of a sparse counter*/
    def zero: A
    /** define an associative operation */
    def op (x: A, y: A): A
    /** I known I should have defined a Monoid here,
      * but I'm too lazy to define all the zeros.
      * The pow operation is valid as long as it only takes positive i */
    def pow (x: A, i: Int): A
  }

  /** Start define some functions for Counter */

  val THRESHOLD = 0.25
  val MINIMIUM = 10000
  def fill[A](size: Int)(sparseValue: A): Counter[A] = SparseCounter[A](Map.empty[Int, A], sparseValue, size)
  def fromIndexedSeq[A](iseq: IndexedSeq[A], default: A): Counter[A] = {
    if (iseq.isEmpty) {
      fill[A](0)(default)
    } else {
      val denseSize = iseq.count( _ != default)
      if (iseq.size >= MINIMIUM && denseSize < iseq.size * THRESHOLD)
        SparseCounter(toMap(iseq, default), default, iseq.size)
      else
        DenseCounter(iseq, default)
    }
  }

  def fromMap[A](m: Map[Int, A], default: A, size: Int): Counter[A] = {
    if (m.isEmpty)
      fill[A](size)(default)
    else {
      val maxIdx = m.keys.max
      require(maxIdx < size)
      val denseSize = m.count(_._2 != default)
      if (size >= MINIMIUM && denseSize < size * THRESHOLD)
        SparseCounter(m, default, size)
      else
        DenseCounter(toIndexedSeq(m, default, size), default)
    }
  }

  def toMap[A](iseq: IndexedSeq[A], default: A): Map[Int, A] =
    iseq.view.zipWithIndex.filter(_._1 != default).map(_.swap).toMap

  def toIndexedSeq[A](m: Map[Int, A], default: A, size: Int): IndexedSeq[A] = {
    import scala.collection.mutable
    val buf = mutable.Buffer.fill[A](size)(default)
    m.foreach { case (idx, v) => buf(idx) = v }
    Vector(buf: _*)
  }


  def addByKey[A](x: Map[Int, A], y: Map[Int, A])
                (implicit sg: CounterElementSemiGroup[A]): Map[Int, A] = {
    x ++ (for ((k, v) <- y) yield k -> sg.op(x.getOrElse(k, sg.zero), v))
  }

  /**
  def addGeno[A: ClassTag](g1: Array[A], g2: Array[A])(implicit sg: CounterElementSemiGroup[A]): Array[A] = {
    for (i <- (0 until g1.length).toArray) yield sg.op(g1(i), g2(i))
  }
  */
}

@SerialVersionUID(2L)
sealed trait Counter[A] extends Serializable {

  def default: A
  def size: Int
  //def denseSize: Int
  def length = size
  def apply(i: Int): A
  def reduce(implicit sg: CounterElementSemiGroup[A]): A
  def reduceByKey[B](keyFunc: Int => B)(implicit sg: CounterElementSemiGroup[A]): Map[B, A]

  def ++(that: Counter[A])(implicit sg: CounterElementSemiGroup[A]): Counter[A] = {
    require(this.length == that.length)
    val newElems: IndexedSeq[A] = for (i <- IndexedSeq( 0 until length : _*)) yield sg.op(this(i), that(i))
    Counter.fromIndexedSeq(newElems, sg.op(this.default, that.default))
  }

  def toDenseVector(make: A => Double): DenseVector[Double] = {
    DenseVector((0 until size).map(i => make(this(i))).toArray)
  }

  def toIndexedSeq = this match {
    case DenseCounter(e, _) => e
    case SparseCounter(e, d, s) => Counter.toIndexedSeq(e, d, s)
  }

  def toArray = this.toIndexedSeq.toArray

  def toMap: Map[Int, A]

  /**
  def collapse(implicit sg: Counter.CounterElementSemiGroup[A]): A

  = {
    //cnt.compact()
    cnt.reduce((a, b) => sg.op(a, b))
  }
  def collapseByBatch(batch: Array[Int])
                     (implicit cm:ClassTag[A], sg: Counter.CounterElementSemiGroup[A]): Map[Int, A] = {
    //cnt.compact
    cnt.zipWithIndex groupBy { case (c, i) => batch(i) } mapValues (
      c => c map (x => x._1) reduce ((a, b) => sg.op(a, b))
    ) map identity
  }
    */
}

case class DenseCounter[A](elems: IndexedSeq[A], default: A)
  extends Counter[A] {
  def apply(i: Int) = elems(i)
  def size = elems.length
  def reduce(implicit sg: CounterElementSemiGroup[A]): A =
    elems.reduce((a, b) => sg.op(a, b))
  def reduceByKey[B](keyFunc: Int => B)(implicit sg: CounterElementSemiGroup[A]): Map[B, A] = {
    elems.view.zipWithIndex.groupBy(x => keyFunc(x._2)).mapValues(
      c => c.map(x => x._1).reduce((a, b) => sg.op(a, b))
    ).map(identity)
  }

  def toMap = elems.view.zipWithIndex.filter( _._1 != default ).map( _.swap ).toMap

}

case class SparseCounter[A](elems: Map[Int, A], default: A, size: Int)
  extends Counter[A] {
  def apply(i: Int) = {
    require(i < size)
    elems.getOrElse(i, default)
  }
  def reduce(implicit sg: CounterElementSemiGroup[A]): A = {
    val dense = elems.values.reduce((a, b) => sg.op(a, b))
    val sparse = sg.pow(default, size - elems.size)
    sg.op(dense, sparse)
  }
  def reduceByKey[B](keyFunc: Int => B)(implicit sg: CounterElementSemiGroup[A]): Map[B, A] = {
    val dense: Map[B, A] = elems.groupBy(x => keyFunc(x._1)).mapValues(
      c => c.values.reduce((a, b) => sg.op(a, b))
    ).map(identity)
    val denseSizes: Map[B, Int] = elems.groupBy(x => keyFunc(x._1)).mapValues(_.size).map(identity)
    val sizes: Map[B, Int] = (0 until size).groupBy(keyFunc(_)).mapValues(_.size).map(identity)
    val sparseSizes: Map[B, Int] =
      for ((k, s) <- sizes) yield k -> (s - denseSizes.getOrElse(k, 0))
    val sparse = sparseSizes.map(x => x._1 -> sg.pow(default, x._2))
    dense ++ (for ((k, v) <- sparse) yield k -> sg.op(v, dense.getOrElse(k, sg.zero)))
  }

  def toMap = elems

}


















