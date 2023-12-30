/*
 * Scala (https://www.scala-lang.org)
 *
 * Copyright EPFL and Lightbend, Inc.
 *
 * Licensed under Apache License 2.0
 * (http://www.apache.org/licenses/LICENSE-2.0).
 *
 * See the NOTICE file distributed with this work for
 * additional information regarding copyright ownership.
 */

package scala.collection

import scala.{collection => sc}
import scala.collection.{mutable => scm, immutable => sci, parallel => scp, concurrent => scc}
import scala.collection.parallel.{mutable => scpm, immutable => scpi}

import org.junit.runner.RunWith
import org.junit.runners.JUnit4
import org.junit.Test
import scala.reflect.ClassTag
import org.junit.Assert._

/* Tests various maps by making sure they all agree on the same answers. */
@RunWith(classOf[JUnit4])
class NewBuilderTest {

  @Test
  def mapPreservesCollectionType(): Unit = {
    def test[T: ClassTag](mapped: Any): Unit = {
      val expected = reflect.classTag[T].runtimeClass
      val isInstance = reflect.classTag[T].runtimeClass.isInstance(mapped)
      assertTrue(s"$mapped (of class ${mapped.getClass} is not a in instance of ${expected}", isInstance)
    }

    test[sc.Iterable[?]         ]((sc.Iterable(1):          sc.Iterable[Int]).map(x => x))
    test[sc.Seq[?]              ]((sc.Seq(1):               sc.Iterable[Int]).map(x => x))
    test[sc.LinearSeq[?]        ]((sc.LinearSeq(1):         sc.Iterable[Int]).map(x => x))
    test[sc.LinearSeq[?]        ]((sc.LinearSeq(1):         sc.Seq[Int]     ).map(x => x))
    test[sc.IndexedSeq[?]       ]((sc.IndexedSeq(1):        sc.Iterable[Int]).map(x => x))
    test[sc.IndexedSeq[?]       ]((sc.IndexedSeq(1):        sc.Seq[Int]     ).map(x => x))
    test[sc.Set[?]              ]((sc.Set(1):               sc.Iterable[Int]).map(x => x))
    test[sc.Map[?, ?]           ]((sc.Map(1 -> 1):          sc.Map[Int, Int]).map(x => x))

    test[scm.Iterable[?]        ]((scm.Iterable(1):         sc.Iterable[Int]).map(x => x))
    test[scm.Queue[?]           ]((scm.Queue(1):            sc.Iterable[Int]).map(x => x))
    test[scm.Queue[?]           ]((scm.Queue(1):            sc.Seq[Int]     ).map(x => x))
    test[scm.ArraySeq[?]        ]((scm.ArraySeq(1):         sc.Iterable[Int]).map(x => x))
    test[scm.ArraySeq[?]        ]((scm.ArraySeq(1):         sc.Seq[Int]     ).map(x => x))

    test[scm.Buffer[?]          ]((scm.Buffer(1):           sc.Iterable[Int]).map(x => x))
    test[scm.Buffer[?]          ]((scm.Buffer(1):           sc.Seq[Int]     ).map(x => x))
    test[scm.IndexedSeq[?]      ]((scm.IndexedSeq(1):       sc.Iterable[Int]).map(x => x))
    test[scm.IndexedSeq[?]      ]((scm.IndexedSeq(1):       sc.Seq[Int]     ).map(x => x))
    test[scm.ArrayBuffer[?]     ]((scm.ArrayBuffer(1):      sc.Iterable[Int]).map(x => x))
    test[scm.ArrayBuffer[?]     ]((scm.ArrayBuffer(1):      sc.Seq[Int]     ).map(x => x))
    test[scm.ListBuffer[?]      ]((scm.ListBuffer(1):       sc.Iterable[Int]).map(x => x))
    test[scm.ListBuffer[?]      ]((scm.ListBuffer(1):       sc.Seq[Int]     ).map(x => x))
    test[scm.Seq[?]             ]((scm.Seq(1):              sc.Iterable[Int]).map(x => x))
    test[scm.Seq[?]             ]((scm.Seq(1):              sc.Seq[Int]     ).map(x => x))
    test[scm.Set[?]             ]((scm.Set(1):              sc.Iterable[Int]).map(x => x))
    test[scm.Set[?]             ]((scm.Set(1):              sc.Set[Int]     ).map(x => x))
    test[scm.HashSet[?]         ]((scm.HashSet(1):          sc.Iterable[Int]).map(x => x))
    test[scm.HashSet[?]         ]((scm.HashSet(1):          sc.Set[Int]     ).map(x => x))
    test[scm.LinkedHashSet[?]   ]((scm.LinkedHashSet(1):    sc.Iterable[Int]).map(x => x))
    test[scm.LinkedHashSet[?]   ]((scm.LinkedHashSet(1):    sc.Set[Int]     ).map(x => x))

    test[sci.Iterable[?]        ]((sci.Iterable(1):         sc.Iterable[Int]).map(x => x))
    test[sci.LinearSeq[?]       ]((sci.LinearSeq(1):        sc.Iterable[Int]).map(x => x))
    test[sci.LinearSeq[?]       ]((sci.LinearSeq(1):        sc.Seq[Int]     ).map(x => x))
    test[sci.List[?]            ]((sci.List(1):             sc.Iterable[Int]).map(x => x))
    test[sci.List[?]            ]((sci.List(1):             sc.Seq[Int]     ).map(x => x))
    test[sci.LazyList[?]        ]((sci.LazyList(1):         sc.Iterable[Int]).map(x => x))
    test[sci.LazyList[?]        ]((sci.LazyList(1):         sc.Seq[Int]     ).map(x => x))
    test[sci.Queue[?]           ]((sci.Queue(1):            sc.Iterable[Int]).map(x => x))
    test[sci.Queue[?]           ]((sci.Queue(1):            sc.Seq[Int]     ).map(x => x))
    test[sci.IndexedSeq[?]      ]((sci.IndexedSeq(1):       sc.Iterable[Int]).map(x => x))
    test[sci.IndexedSeq[?]      ]((sci.IndexedSeq(1):       sc.Seq[Int]     ).map(x => x))
    test[sci.Vector[?]          ]((sci.Vector(1):           sc.Iterable[Int]).map(x => x))
    test[sci.Vector[?]          ]((sci.Vector(1):           sc.Seq[Int]     ).map(x => x))
    test[sci.Seq[?]             ]((sci.Seq(1):              sc.Iterable[Int]).map(x => x))
    test[sci.Seq[?]             ]((sci.Seq(1):              sc.Seq[Int]     ).map(x => x))
    test[sci.Set[?]             ]((sci.Set(1):              sc.Iterable[Int]).map(x => x))
    test[sci.Set[?]             ]((sci.Set(1):              sc.Set[Int]     ).map(x => x))
    test[sci.ListSet[?]         ]((sci.ListSet(1):          sc.Iterable[Int]).map(x => x))
    test[sci.ListSet[?]         ]((sci.ListSet(1):          sc.Set[Int]     ).map(x => x))
    test[sci.HashSet[?]         ]((sci.HashSet(1):          sc.Iterable[Int]).map(x => x))
    test[sci.HashSet[?]         ]((sci.HashSet(1):          sc.Set[Int]     ).map(x => x))

    test[scp.ParIterable[?]     ]((scp.ParIterable(1):      scp.ParIterable[Int]).map(x => x))
    test[scp.ParSeq[?]          ]((scp.ParSeq(1):           scp.ParIterable[Int]).map(x => x))
    test[scp.ParSeq[?]          ]((scp.ParSeq(1):           scp.ParSeq[Int]     ).map(x => x))
    test[scp.ParSet[?]          ]((scp.ParSet(1):           scp.ParIterable[Int]).map(x => x))
    test[scp.ParSet[?]          ]((scp.ParSet(1):           scp.ParSet[Int]     ).map(x => x))

    test[scpm.ParIterable[?]    ]((scpm.ParIterable(1):     scp.ParIterable[Int]).map(x => x))
    test[scpm.ParSeq[?]         ]((scpm.ParSeq(1):          scp.ParIterable[Int]).map(x => x))
    test[scpm.ParSeq[?]         ]((scpm.ParSeq(1):          scp.ParSeq[Int]     ).map(x => x))
    test[scpm.ParArray[?]       ]((scpm.ParArray(1):        scp.ParIterable[Int]).map(x => x))
    test[scpm.ParArray[?]       ]((scpm.ParArray(1):        scp.ParSeq[Int]     ).map(x => x))
    test[scpm.ParSet[?]         ]((scpm.ParSet(1):          scp.ParIterable[Int]).map(x => x))
    test[scpm.ParSet[?]         ]((scpm.ParSet(1):          scp.ParSet[Int]     ).map(x => x))
    test[scpm.ParHashSet[?]     ]((scpm.ParHashSet(1):      scp.ParIterable[Int]).map(x => x))
    test[scpm.ParHashSet[?]     ]((scpm.ParHashSet(1):      scp.ParSet[Int]     ).map(x => x))

    test[scpi.ParIterable[?]    ]((scpi.ParIterable(1):     scp.ParIterable[Int]).map(x => x))
    test[scpi.ParSeq[?]         ]((scpi.ParSeq(1):          scp.ParIterable[Int]).map(x => x))
    test[scpi.ParSeq[?]         ]((scpi.ParSeq(1):          scp.ParSeq[Int]     ).map(x => x))
    test[scpi.ParVector[?]      ]((scpi.ParVector(1):       scp.ParIterable[Int]).map(x => x))
    test[scpi.ParVector[?]      ]((scpi.ParVector(1):       scp.ParSeq[Int]     ).map(x => x))
    test[scpi.ParSet[?]         ]((scpi.ParSet(1):          scp.ParIterable[Int]).map(x => x))
    test[scpi.ParSet[?]         ]((scpi.ParSet(1):          scp.ParSet[Int]     ).map(x => x))
    test[scpi.ParHashSet[?]     ]((scpi.ParHashSet(1):      scp.ParIterable[Int]).map(x => x))
    test[scpi.ParHashSet[?]     ]((scpi.ParHashSet(1):      scp.ParSet[Int]     ).map(x => x))

    // These go through `GenMap.canBuildFrom`. There is no simple fix for Map like there is for Set.
    // A Map does not provide access to its companion object at runtime. (The `companion` field
    // points to an inherited `GenericCompanion`, not the actual companion object). Therefore, the
    // `MapCanBuildFrom` has no way to get the correct builder for the source type at runtime.
    //test[scm.Map[_, _]          ]((scm.Map(1 -> 1):           sc.GenMap[Int, Int]).map(x => x)
    //test[scm.OpenHashMap[_, _]  ]((scm.OpenHashMap(1 -> 1):   sc.GenMap[Int, Int]).map(x => x))
    //test[scm.LongMap[_]         ]((scm.LongMap(1L -> 1):      sc.GenMap[Long, Int]).map(x => x))
    //test[scm.ListMap[_, _]      ]((scm.ListMap(1 -> 1):       sc.GenMap[Int, Int]).map(x => x))
    //test[scm.LinkedHashMap[_, _]]((scm.LinkedHashMap(1 -> 1): sc.GenMap[Int, Int]).map(x => x))
    //test[scm.HashMap[_, _]      ]((scm.HashMap(1 -> 1):       sc.GenMap[Int, Int]).map(x => x))
    //test[sci.Map[_, _]          ]((sci.Map(1 -> 1):           sc.GenMap[Int, Int]).map(x => x))
    //test[sci.ListMap[_, _]      ]((sci.ListMap(1 -> 1):       sc.GenMap[Int, Int]).map(x => x))
    //test[sci.IntMap[_]          ]((sci.IntMap(1 -> 1):        sc.GenMap[Int, Int]).map(x => x))
    //test[sci.LongMap[_]         ]((sci.LongMap(1L -> 1):      sc.GenMap[Long, Int]).map(x => x))
    //test[sci.HashMap[_, _]      ]((sci.HashMap(1 -> 1):       sc.GenMap[Int, Int]).map(x => x))
    //test[sci.SortedMap[_, _]    ]((sci.SortedMap(1 -> 1):     sc.GenMap[Int, Int]).map(x => x))
    //test[sci.TreeMap[_, _]      ]((sci.TreeMap(1 -> 1):       sc.GenMap[Int, Int]).map(x => x))
    //test[scc.TrieMap[_, _]      ]((scc.TrieMap(1 -> 1):       sc.GenMap[Int, Int]).map(x => x))
    //test[scp.ParMap[_, _]       ]((scp.ParMap(1 -> 1):        sc.GenMap[Int, Int]).map(x => x))
    //test[scpm.ParMap[_, _]      ]((scpm.ParMap(1 -> 1):       sc.GenMap[Int, Int]).map(x => x))
    //test[scpm.ParHashMap[_, _]  ]((scpm.ParHashMap(1 -> 1):   sc.GenMap[Int, Int]).map(x => x))
    //test[scpm.ParTrieMap[_, _]  ]((scpm.ParTrieMap(1 -> 1):   sc.GenMap[Int, Int]).map(x => x))
    //test[scpi.ParMap[_, _]      ]((scpi.ParMap(1 -> 1):       sc.GenMap[Int, Int]).map(x => x))
    //test[scpi.ParHashMap[_, _]  ]((scpi.ParHashMap(1 -> 1):   sc.GenMap[Int, Int]).map(x => x))

    // These cannot be expected to work. The static type information is lost, and `map` does not capture
    // a `ClassTag` of the result type, so there is no way for a `CanBuildFrom` to decide to build another
    // `BitSet` instead of a generic `Set` implementation:
    //test[scm.BitSet          ]((scm.BitSet(1):        sc.GenTraversable[Int]).map(x => x))
    //test[scm.BitSet          ]((scm.BitSet(1):        sc.Set[Int]).map(x => x))

    // These also require a `ClassTag`:
    //test[scm.UnrolledBuffer[_]]((scm.UnrolledBuffer(1): sc.GenTraversable[Int]).map(x => x))
    //test[scm.UnrolledBuffer[_]]((scm.UnrolledBuffer(1): sc.Seq[Int]).map(x => x))

    // The situation is similar for sorted collection. They require an implicit `Ordering` which cannot
    // be captured at runtime by a `CanBuildFrom` when the static type has been lost:
    //test[sc.SortedMap[_, _]  ]((sc.SortedMap(1 -> 1):  sc.GenTraversable[(Int, Int)]).map(x => x))
    //test[sc.SortedMap[_, _]  ]((sc.SortedMap(1 -> 1):  sc.GenMap[Int, Int]).map(x => x))
    //test[sc.SortedSet[_]     ]((sc.SortedSet(1):       sc.GenTraversable[Int]).map(x => x))
    //test[sc.SortedSet[_]     ]((sc.SortedSet(1):       sc.Set[Int]).map(x => x))
    //test[scm.SortedSet[_]    ]((scm.SortedSet(1):      sc.GenTraversable[Int]).map(x => x))
    //test[scm.SortedSet[_]    ]((scm.SortedSet(1):      sc.Set[Int]).map(x => x))
    //test[scm.TreeSet[_]      ]((scm.TreeSet(1):        sc.GenTraversable[Int]).map(x => x))
    //test[scm.TreeSet[_]      ]((scm.TreeSet(1):        sc.Set[Int]).map(x => x))
    //test[scm.TreeMap[_, _]   ]((scm.TreeMap(1 -> 1):   sc.GenTraversable[(Int, Int)]).map(x => x))
    //test[scm.TreeMap[_, _]   ]((scm.TreeMap(1 -> 1):   sc.GenMap[Int, Int]).map(x => x))
    //test[scm.SortedMap[_, _] ]((scm.SortedMap(1 -> 1): sc.GenTraversable[(Int, Int)]).map(x => x))
    //test[scm.SortedMap[_, _] ]((scm.SortedMap(1 -> 1): sc.GenMap[Int, Int]).map(x => x))

    // Maps do not map to maps when seen as GenTraversable. This would require knowledge that `map`
    // returns a `Tuple2`, which is not available dynamically:
    //test[sc.GenMap[_, _]     ]((sc.GenMap(1 -> 1):    sc.GenTraversable[(Int, Int)]).map(x => x))
    //test[sc.Map[_, _]        ]((sc.Map(1 -> 1):       sc.GenTraversable[(Int, Int)]).map(x => x))
   }
}
