package xyz.hyperreal.btree

import org.scalatest._
import prop.PropertyChecks
import org.scalatest.prop.TableDrivenPropertyChecks._


class Tests extends FreeSpec with PropertyChecks with Matchers {
	
	val treeImplementations =
		Table(
			("object generator", 													"storage", 		"order"),
			//----------------                               -------	       -----
			(() => new MemoryBPlusTree[String, Any]( 3 ),    "in memory",  3),
			(() => new MemoryBPlusTree[String, Any]( 4 ),    "in memory", 	4),
			(() => new MemoryBPlusTree[String, Any]( 5 ),    "in memory", 	5),
			(() => new MemoryBPlusTree[String, Any]( 6 ),    "in memory", 	6),
			(() => new FileBPlusTree( "btree", 3, true ),    "on disk", 		3),
			(() => new FileBPlusTree( "btree", 4, true ),    "on disk", 		4),
			(() => new FileBPlusTree( "btree", 5, true ),    "on disk", 		5),
			(() => new FileBPlusTree( "btree", 6, true ),    "on disk", 		6)
 			)
	
	forAll (treeImplementations) { (gen, storage, order) =>
		val t = gen()
		
		("iterator: " + storage + ", order " + order) in {
		
			t.iterator.isEmpty shouldBe true
			t.boundedIterator( ('>=, "a") ).isEmpty shouldBe true
			t.boundedIterator( ('>=, "a"), ('<=, "z") ).isEmpty shouldBe true
			t.boundedIterator( ('<=, "z") ).isEmpty shouldBe true
			t.insertKeys( "v", "t", "u", "j", "g", "w", "y", "c", "n", "a", "r", "b", "s", "e", "f", "i", "z", "d", "p", "x", "m", "k", "o", "q" )
			t.wellConstructed shouldBe "true"
			t.iteratorOverKeys.mkString shouldBe "abcdefgijkmnopqrstuvwxyz"
			t.boundedIteratorOverKeys( ('>=, "a") ).mkString shouldBe "abcdefgijkmnopqrstuvwxyz"
			t.boundedIteratorOverKeys( ('>, "a") ).mkString shouldBe "bcdefgijkmnopqrstuvwxyz"
			t.boundedIteratorOverKeys( ('>=, "A") ).mkString shouldBe "abcdefgijkmnopqrstuvwxyz"
			t.boundedIteratorOverKeys( ('>, "A") ).mkString shouldBe "abcdefgijkmnopqrstuvwxyz"
			t.boundedIteratorOverKeys( ('<=, "z") ).mkString shouldBe "abcdefgijkmnopqrstuvwxyz"
			t.boundedIteratorOverKeys( ('<, "z") ).mkString shouldBe "abcdefgijkmnopqrstuvwxy"
			t.boundedIteratorOverKeys( ('<=, "{") ).mkString shouldBe "abcdefgijkmnopqrstuvwxyz"
			t.boundedIteratorOverKeys( ('<, "{") ).mkString shouldBe "abcdefgijkmnopqrstuvwxyz"
			t.boundedIteratorOverKeys( ('>=, "a"), ('<=, "d") ).mkString shouldBe "abcd"
			t.boundedIteratorOverKeys( ('>, "a"), ('<=, "d") ).mkString shouldBe "bcd"
			t.boundedIteratorOverKeys( ('>=, "a"), ('<, "d") ).mkString shouldBe "abc"
			t.boundedIteratorOverKeys( ('>, "a"), ('<, "d") ).mkString shouldBe "bc"
			t.boundedIteratorOverKeys( ('<, "d"), ('>, "a") ).mkString shouldBe "bc"
			t.boundedIteratorOverKeys( ('>, "a"), ('<, "a") ).mkString shouldBe ""
			t.boundedIteratorOverKeys( ('>=, "a"), ('<, "a") ).mkString shouldBe ""
			t.boundedIteratorOverKeys( ('>, "a"), ('<=, "a") ).mkString shouldBe ""
			t.boundedIteratorOverKeys( ('>=, "a"), ('<=, "a") ).mkString shouldBe "a"
			t.boundedIteratorOverKeys( ('>=, "c"), ('<=, "a") ).mkString shouldBe ""
			t.boundedIteratorOverKeys( ('>, "c"), ('<=, "a") ).mkString shouldBe ""
			t.boundedIteratorOverKeys( ('>=, "c"), ('<, "a") ).mkString shouldBe ""
			t.boundedIteratorOverKeys( ('>, "c"), ('<, "a") ).mkString shouldBe ""
			t.boundedIteratorOverKeys( ('>, "h"), ('<, "h") ).mkString shouldBe ""
			t.boundedIteratorOverKeys( ('>=, "h"), ('<, "h") ).mkString shouldBe ""
			t.boundedIteratorOverKeys( ('>, "h"), ('<=, "h") ).mkString shouldBe ""
			t.boundedIteratorOverKeys( ('>=, "h"), ('<=, "h") ).mkString shouldBe ""
			t.boundedIteratorOverKeys( ('>=, "h"), ('<=, "a") ).mkString shouldBe ""
			t.boundedIteratorOverKeys( ('>, "h"), ('<=, "a") ).mkString shouldBe ""
			t.boundedIteratorOverKeys( ('>=, "h"), ('<, "a") ).mkString shouldBe ""
			t.boundedIteratorOverKeys( ('>, "h"), ('<, "a") ).mkString shouldBe ""
			t.boundedIteratorOverKeys( ('>=, "k"), ('<=, "h") ).mkString shouldBe ""
			t.boundedIteratorOverKeys( ('>, "k"), ('<=, "h") ).mkString shouldBe ""
			t.boundedIteratorOverKeys( ('>=, "k"), ('<, "h") ).mkString shouldBe ""
			t.boundedIteratorOverKeys( ('>, "k"), ('<, "h") ).mkString shouldBe ""
			t.boundedIteratorOverKeys( ('>=, "h"), ('<=, "l") ).mkString shouldBe "ijk"
			t.boundedIteratorOverKeys( ('>, "h"), ('<=, "l") ).mkString shouldBe "ijk"
			t.boundedIteratorOverKeys( ('>=, "h"), ('<, "l") ).mkString shouldBe "ijk"
			t.boundedIteratorOverKeys( ('>, "h"), ('<, "l") ).mkString shouldBe "ijk"
		}
	}
	
	forAll (treeImplementations) { (gen, storage, order) =>
		val t = gen()
		
		("bulk loading: " + storage + ", order " + order) in {
			t.load( ("h",8), ("i",9), ("d",4), ("b",2), ("j",10), ("f",6), ("g",7), ("a",1), ("c",3), ("e",5) )
			t.wellConstructed shouldBe "true"
			t.iterator mkString ", " shouldBe "(a,1), (b,2), (c,3), (d,4), (e,5), (f,6), (g,7), (h,8), (i,9), (j,10)"
			t.load( ("p",16), ("r",18), ("l",12), ("k",11), ("o",15), ("s",19), ("q",17), ("t",20), ("m",13), ("n",14) )
			t.wellConstructed shouldBe "true"
			t.iterator mkString ", " shouldBe "(a,1), (b,2), (c,3), (d,4), (e,5), (f,6), (g,7), (h,8), (i,9), (j,10), (k,11), (l,12), (m,13), (n,14), (o,15), (p,16), (q,17), (r,18), (s,19), (t,20)"
			a [RuntimeException] should be thrownBy {t.load( ("A", 0) )}
		}
	}
}