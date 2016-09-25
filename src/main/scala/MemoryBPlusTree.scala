package xyz.hyperreal.btree

import collection.mutable.ArrayBuffer


/**
 * An in-memory B+ Tree implementation.
 * 
 * @constructor creates an in-memory B+ Tree with a branching factor of `order`.
 * @param order the branching factor (maximum number of branches in an internal node) of the tree
 * @tparam K the type of the keys contained in this map.
 * @tparam V the type of the values associated with the keys.
 */
class MemoryBPlusTree[K <% Ordered[K], V]( order: Int ) extends AbstractBPlusTree[K, V]( order ) {
	protected type N = Node[K, V]
	
	protected var first: Node[K, V] = new LeafNode[K, V]( null )
	protected var last: Node[K, V] = first
	protected var root: Node[K, V] = first
	protected var lastlen = 0
	
  protected def addBranch( node: Node[K, V], branch: Node[K,V] ) = node.asInternal.branches += branch
		
	protected def addKey( node: Node[K, V], key: K ) = node.keys += key
	
	protected def addValue[V1 >: V]( node: Node[K, V], value: V1 ) = node.asInstanceOf[Node[K, V1]].asLeaf.values += value

	protected def freeNode( node: Node[K, V] ) {}

	protected def getBranch( n: Node[K, V], index: Int ) = n.asInternal.branches( index )
	
	protected def getBranches( n: Node[K, V] ) = n.asInternal.branches
	
	protected def getKey( n: Node[K, V], index: Int ) = n.keys( index )
	
	protected def getKeys( node: Node[K, V] ) = node.keys
	
	protected def getNext( node: Node[K, V] ) = node.next
	
	protected def getParent( node: Node[K, V] ) = node.parent
	
	protected def getPrev( node: Node[K, V] ) = node.prev
	
	protected def getValue( n: Node[K, V], index: Int ) = n.asLeaf.values( index )
		
	protected def getValues( node: Node[K, V] ) = node.asLeaf.values
	
	protected def insertInternal( n: Node[K, V], keyIndex: Int, key: K, branchIndex: Int, branch: Node[K, V] ) {
		n.keys.insert( keyIndex, key )
		n.asInternal.branches.insert( branchIndex, branch )
	}
	
	protected def insertLeaf[V1 >: V]( n: Node[K, V], index: Int, key: K, value: V1 ) {
		n.keys.insert( index, key )
		n.asInstanceOf[Node[K, V1]].asLeaf.values.insert( index, value )
	}
	
	protected def isLeaf( node: Node[K, V] ) = node.isLeaf
	
	protected def nodeLength( node: Node[K, V] ) = node.keys.length
	
	protected def moveInternalDelete( src: N, begin: Int, end: Int, dst: N, index: Int ) {
		dst.keys.insertAll( index, src.keys.view(begin, end) ) 
		src.keys.remove( begin, end - begin )
		dst.asInternal.branches.insertAll( index + 1, src.asInternal.branches.view(begin + 1, end + 1) )
		src.asInternal.branches.remove( begin + 1, end - begin )
	}
	
	protected def moveInternal( src: Node[K, V], begin: Int, end: Int, dst: Node[K, V] ) {
		src.keys.view( begin, end ) copyToBuffer dst.keys
		src.keys.remove( begin - 1, end - begin + 1 )
		src.asInternal.branches.view( begin, end + 1 ) copyToBuffer dst.asInternal.branches
		src.asInternal.branches.remove( begin, end - begin + 1 )
	}
	
	protected def moveLeaf( src: Node[K, V], begin: Int, end: Int, dst: Node[K, V], index: Int ) {
		dst.keys.insertAll( index, src.keys.view(begin, end) ) 
		src.keys.remove( begin, end - begin )
		dst.asLeaf.values.insertAll( index, src.asLeaf.values.view(begin, end) )
		src.asLeaf.values.remove( begin, end - begin )
	}
	
	protected def newInternal( parent: Node[K, V] ) = new InternalNode( parent.asInstanceOf[InternalNode[K, V]] )
	
	protected def newLeaf( parent: Node[K, V] ) = new LeafNode( parent.asInstanceOf[InternalNode[K, V]] )
	
	protected def newRoot( branch: Node[K, V] ) = {
		val res = new InternalNode[K, V]( null )
		
		res.branches += branch
		res
	}
	
	protected def nul = null

	protected def removeInternal( node: Node[K, V], keyIndex: Int, branchIndex: Int ) = {
		node.keys.remove( keyIndex, 1 )
		node.asInternal.branches.remove( branchIndex, 1 )
		node.length
	}

	protected def removeLeaf( node: Node[K, V], index: Int ) = {
		node.keys.remove( index, 1 )
		node.asLeaf.values.remove( index, 1 )
		node.length
	}
	
	protected def setFirst( leaf: Node[K, V] ) {}
	
	protected def setKey( node: Node[K, V], index: Int, key: K ) = node.keys( index ) = key

	protected def setLast( leaf: Node[K, V] ) {}
	
	protected def setNext( node: Node[K, V], p: Node[K, V] ) = node.next = p
	
	protected def setParent( node: Node[K, V], p: Node[K, V] ) = node.parent = p.asInstanceOf[InternalNode[K, V]]
	
	protected def setPrev( node: Node[K, V], p: Node[K, V] ) = node.prev = p
		
	protected def setRoot( node: Node[K, V] ) {}
	
	protected def setValue[V1 >: V]( node: Node[K, V], index: Int, v: V1 ) = node.asInstanceOf[Node[K, V1]].asLeaf.values(index) = v

	protected abstract class Node[K, V] {
		var parent: InternalNode[K, V]
		
		def isLeaf: Boolean
		
		var prev: Node[K, V] = null
		var next: Node[K, V] = null
		val keys = new ArrayBuffer[K]
		
		def length = keys.size
		
		def asInternal = asInstanceOf[InternalNode[K, V]]
		
		def asLeaf = asInstanceOf[LeafNode[K, V]]
	}

	protected class InternalNode[K, V]( var parent: InternalNode[K, V] ) extends Node[K, V] {
		val isLeaf = false
		val branches = new ArrayBuffer[Node[K, V]]
		
		override def toString = keys.mkString( "internal[", ", ", "]" )
	}

	protected class LeafNode[K, V]( var parent: InternalNode[K, V] ) extends Node[K, V] {
		val isLeaf = true
		val values = new ArrayBuffer[V]
		
		override def toString = keys.mkString( "leaf[", ", ", "]" )
	}
}