package xyz.hyperreal.btree

import xyz.hyperreal.ramfile.RamFile

import io.Codec
import collection.mutable.ArrayBuffer
import collection.AbstractSeq

import java.io.{File, RandomAccessFile}


class FileBPlusTree( filename: String, order: Int, newfile: Boolean = false ) extends AbstractBPlusTree[String, Any]( order ) {
	protected type N = Long
	
	protected val NULL = 0
	
	protected val LEAF_NODE = 0
	protected val INTERNAL_NODE = 1
	
	protected val TYPE_BOOLEAN = 0x10
		protected val TYPE_BOOLEAN_FALSE = 0x10
		protected val TYPE_BOOLEAN_TRUE = 0x18
	protected val TYPE_INT = 0x11
	protected val TYPE_LONG = 0x12
	protected val TYPE_DOUBLE = 0x13
	protected val TYPE_STRING = 0x14
	protected val TYPE_NULL = 0x15
	
	protected val DATUM_SIZE = 1 + 8		// type + datum
	protected val POINTER_SIZE = 8
	protected val DATA_ARRAY_SIZE = (order - 1)*DATUM_SIZE
	
	protected val FILE_HEADER = 0
	protected val FILE_ORDER = FILE_HEADER + 12
	protected val FILE_FREE_PTR = FILE_ORDER + 2
	protected val FILE_ROOT_PTR = FILE_FREE_PTR + POINTER_SIZE
	protected val FILE_FIRST_PTR = FILE_ROOT_PTR + POINTER_SIZE
	protected val FILE_LAST_PTR = FILE_FIRST_PTR + POINTER_SIZE
	protected val FILE_BLOCKS = FILE_LAST_PTR + POINTER_SIZE
	
	protected val NODE_TYPE = 0
	protected val NODE_PARENT_PTR = NODE_TYPE + 1
	protected val NODE_LENGTH = NODE_PARENT_PTR + POINTER_SIZE
	protected val NODE_KEYS = NODE_LENGTH + 2
	
	protected val LEAF_PREV_PTR = NODE_KEYS + DATA_ARRAY_SIZE	
	protected val LEAF_NEXT_PTR = LEAF_PREV_PTR + POINTER_SIZE
	protected val LEAF_VALUES = LEAF_NEXT_PTR + POINTER_SIZE
	
	protected val INTERNAL_BRANCHES = NODE_KEYS + DATA_ARRAY_SIZE
	
	protected val BLOCK_SIZE = LEAF_VALUES + DATA_ARRAY_SIZE
	
	private var savedNode: Long = NULL
	private var savedKeys = new ArrayBuffer[String]
	private var savedValues = new ArrayBuffer[Any]
	private var savedBranches = new ArrayBuffer[Long]
	
 	if (newfile)
// 		RamFile.delete( filename )
		new File( filename ).delete
		
// 	protected val file = new RamFile( filename )
 	protected val file = new RandomAccessFile( filename, "rw" )		
	protected var root: Long = _
	protected var first: Long = _
	protected var last: Long = _
	protected var lastlen: Int = _
		
	if (file.length == 0) {
		file writeBytes "B+ Tree v0.1"
		file writeShort order
		file writeLong nul
		file writeLong FILE_BLOCKS
		file writeLong FILE_BLOCKS
		file writeLong FILE_BLOCKS
		root = newLeaf( nul )
		first = FILE_BLOCKS
		last = FILE_BLOCKS
		lastlen = 0
	} else {
		file seek FILE_ORDER
		
		if (file.readShort != order)
			sys.error( "order not the same as on disk" )
			
		file seek FILE_ROOT_PTR
		root = file.readLong
		first = file.readLong
		last = file.readLong
		lastlen = nodeLength( last )
	}
	
	protected def nodeLength( node: Long, len: Int ) {
		file seek (node + NODE_LENGTH)
		file writeShort len
	}
	
	protected def getBranch( node: Long, index: Int ) = {
		file seek (node + INTERNAL_BRANCHES + index*POINTER_SIZE)
		file readLong
	}
	
	protected def getBranches( node: Long ): Seq[Long] =
		new AbstractSeq[Long] with IndexedSeq[Long] {
			def apply( idx: Int ) = getBranch( node, idx )
			
			def length = nodeLength( node ) + 1
		}
	
	protected def getKey( node: Long, index: Int ) =
		if (savedNode == node)
			savedKeys(index)
		else
			readString( node + NODE_KEYS + index*DATUM_SIZE )
	
	protected def getKeys( node: Long ): Seq[String] =
		new AbstractSeq[String] with IndexedSeq[String] {
			def apply( idx: Int ) = getKey( node, idx )
				
			def length = nodeLength( node )
		}
	
	protected def getNext( node: Long ): Long = {
		file seek (node + LEAF_NEXT_PTR)
		file readLong
	}
	
	protected def getParent( node: Long ): Long = {
		file seek (node + NODE_PARENT_PTR)
		file readLong
	}
	
	protected def getPrev( node: Long ): Long = {
		file seek (node + LEAF_PREV_PTR)
		file readLong
	}
	
	protected def getValue( node: Long, index: Int ) = readDatum( node + LEAF_VALUES + index*DATUM_SIZE )
	
	protected def getValues( node: Long ) =
		new AbstractSeq[Any] with IndexedSeq[Any] {
			def apply( idx: Int ) = getValue( node, idx )
				
			def length = nodeLength( node )
		}
	
	protected def insertInternal( node: Long, index: Int, key: String, branch: Long ) {
		val len = nodeLength( node )
		
		if (len < order - 1) {
			nodeLength( node, len + 1 )
			
			if (index < len) {
				copyKeys( node, index, len, node, index + 1 )
			
				val data = new Array[Byte]( (len - index + 1)*POINTER_SIZE )
			
				file seek (node + INTERNAL_BRANCHES + (index + 1)*POINTER_SIZE)
				file readFully data
				file seek (node + INTERNAL_BRANCHES + (index + 2)*POINTER_SIZE)
				file write data
			}
			
			writeDatum( node + NODE_KEYS + index*DATUM_SIZE, key )
			file seek (node + INTERNAL_BRANCHES + (index + 1)*POINTER_SIZE)
			file writeLong branch
		} else {
			if (savedNode != NULL)
				sys.error( "a node is already being saved" )
				
			savedKeys.clear
			savedBranches.clear
			savedKeys ++= getKeys( node )
			savedBranches ++= getBranches( node )
			savedKeys.insert( index, key )
			savedBranches.insert( index + 1, branch )
			savedNode = node
		}
	}
	
	protected def insertLeaf( node: Long, index: Int, key: String, value: Any ) {
		val len = nodeLength( node )
		
		if (len < order - 1) {
			nodeLength( node, len + 1 )
			
			if (index < len)
				copy( node, index, len, node, index + 1 )
			
			writeDatum( node + NODE_KEYS + index*DATUM_SIZE, key )
			setValue( node, index, value )
		} else {
			if (savedNode != NULL)
				sys.error( "a node is already being saved" )
				
			savedKeys.clear
			savedValues.clear
			savedKeys ++= getKeys( node )
			savedValues ++= getValues( node )
			savedKeys.insert( index, key )
			savedValues.insert( index, value )
			savedNode = node
		}
	}
	
	protected def isLeaf( node: Long ): Boolean = {
		file seek node
		file.read == LEAF_NODE
	}
		
	protected def moveInternal( src: Long, begin: Int, end: Int, dst: Long ) {
		if (savedNode == NULL) {
			copyKeys( src, begin, end, dst, 0 )
			nodeLength( src, nodeLength(src) - (end - begin) - 1 )
			
			val data = new Array[Byte]( (end - begin + 1)*POINTER_SIZE )
			
			file seek (src + INTERNAL_BRANCHES + begin*POINTER_SIZE)
			file readFully data
			file seek (dst + INTERNAL_BRANCHES)
			file write data
			nodeLength( dst, end - begin )
		} else {
			val dstKeys = new ArrayBuffer[String]
			val dstBranches = new ArrayBuffer[Long]
			
			savedKeys.view( begin, end ) copyToBuffer dstKeys
			savedKeys.remove( begin - 1, end - begin + 1 )
			savedBranches.view( begin, end + 1 ) copyToBuffer dstBranches
			savedBranches.remove( begin, end - begin + 1 )
			
			for ((k, i) <- savedKeys zipWithIndex)
				setKey( src, i, k )
				
			for ((k, i) <- dstKeys zipWithIndex)
				setKey( dst, i, k )

			for ((b, i) <- savedBranches zipWithIndex)
				setBranch( src, i, b )

			for ((b, i) <- dstBranches zipWithIndex)
				setBranch( dst, i, b )
				
			nodeLength( src, savedKeys.length )
			nodeLength( dst, end - begin )
			savedNode = NULL
		}
	}
	
	protected def moveLeaf( src: Long, begin: Int, end: Int, dst: Long, index: Int ) {
		val dstlen = nodeLength( dst )
			
		if (savedNode == NULL) {
			copy( dst, index, dstlen, dst, index + end - begin )
			copy( src, begin, end, dst, index )
			nodeLength( src, nodeLength(src) - (end - begin) )
			nodeLength( dst, dstlen + end - begin )
		} else {
			val dstKeys = new ArrayBuffer[String]
			val dstValues = new ArrayBuffer[Any]
			val len = end - begin
			
			savedKeys.view( begin, end ) copyToBuffer dstKeys
			savedKeys.remove( begin, len )
			savedValues.view( begin, end ) copyToBuffer dstValues
			savedValues.remove( begin, len )
			
			for (((k, v), i) <- savedKeys zip savedValues zipWithIndex) {
				setKey( src, i, k )
				setValue( src, i, v )
			}
			
			for (((k, v), i) <- dstKeys zip dstValues zipWithIndex) {
				setKey( dst, i, k )
				setValue( dst, i, v )
			}
				
			nodeLength( src, savedKeys.length )
			nodeLength( dst, len )
			savedNode = NULL
		}
	}
	
	protected def newInternal( parent: Long ): Long = {
		val node = alloc( BLOCK_SIZE )
		
		file write INTERNAL_NODE
		file writeLong parent
		file writeShort 0
		node
	}
	
	protected def newLeaf( parent: Long ): Long = {
		val node = alloc( BLOCK_SIZE )
		
		file write LEAF_NODE
		file writeLong parent
		file writeShort 0
		node
	}
	
	protected def newRoot( branch: Long ): Long = {
		val node = newInternal( nul )
		
		file seek (node + INTERNAL_BRANCHES)
		file writeLong branch
		file seek FILE_ROOT_PTR
		file writeLong node
		node
	}
	
	protected def nodeLength( node: Long ) =
		if (savedNode == NULL) {
			file seek (node + NODE_LENGTH)
			file.readShort
		} else
			savedKeys.length
	
	protected def nul = 0

	protected def removeLeaf( node: Long, index: Int ) = {
		val len = nodeLength( node )
		
		nodeLength( node, len - 1 )
		
		if (index < len - 1)
			copy( node, index + 1, len, node, index - 1 )
			
		len
	}
	
	protected def setBranch( node: Long, index: Int, branch: Long ) {
		file seek (node + INTERNAL_BRANCHES + index*POINTER_SIZE)
		file writeLong branch
	}
	
	protected def setFirst( leaf: Long ) {
		file seek FILE_FIRST_PTR
		file writeLong leaf
	}

	protected def setKey( node: Long, index: Int, key: String ) = writeDatum( node + NODE_KEYS + index*DATUM_SIZE, key )

	protected def setLast( leaf: Long ) {
		file seek FILE_LAST_PTR
		file writeLong leaf
	}
	
	protected def setNext( node: Long, p: Long ) {
		file seek (node + LEAF_NEXT_PTR)
		file writeLong p
	}
	
	protected def setParent( node: Long, p: Long ) {
		file seek (node + NODE_PARENT_PTR)
		file writeLong p
	}
	
	protected def setPrev( node: Long, p: Long ) {
		file seek (node + LEAF_PREV_PTR)
		file writeLong p
	}
	
	protected def setValue( node: Long, index: Int, v: Any ) = writeDatum( node + LEAF_VALUES + index*DATUM_SIZE, v )
	
		
	def close = file.close
	
	
	protected def copyKeys( src: Long, begin: Int, end: Int, dst: Long, index: Int ) = {
		val data = new Array[Byte]( (end - begin)*DATUM_SIZE )
		
		file seek (src + NODE_KEYS + begin*DATUM_SIZE)
		file readFully data
		file seek (dst + NODE_KEYS + index*DATUM_SIZE)
		file write data
		data
	}
	
	protected def copy( src: Long, begin: Int, end: Int, dst: Long, index: Int ) {
		val data = copyKeys( src, begin, end, dst, index )
		
		file seek (src + LEAF_VALUES + begin*DATUM_SIZE)
		file readFully data
		file seek (dst + LEAF_VALUES + index*DATUM_SIZE)
		file write data
	}
	
	protected def freeDatum( addr: Long ) = {
		file seek addr
		
		file read match {
			case TYPE_STRING => 
				file seek file.readLong
				
				val start = file.getFilePointer
				val len = file.readUnsignedShort + 2
				
				free( start, len )
			case _ =>
		}
	}
	
	protected def readDatum( addr: Long ) = {
		def readUTF8( len: Int ) = {
			val a = new Array[Byte]( len )
			
			file readFully a
			new String( Codec fromUTF8 a )
		}
		
		file seek addr
		
		file read match {
			case len if len <= 0x08 => readUTF8( len )
			case TYPE_BOOLEAN_FALSE => false
			case TYPE_BOOLEAN_TRUE => true
			case TYPE_INT => file readInt
			case TYPE_LONG => file readLong
			case TYPE_DOUBLE => file readDouble
			case TYPE_STRING => 
				file seek file.readLong
				readUTF8( file readInt )
			case TYPE_NULL => null
		}
	}
	
	protected def readString( addr: Long ) = readDatum( addr ).asInstanceOf[String]
	
	protected def writeDatum( addr: Long, datum: Any ) = {
		file seek addr
		
		datum match {
			case null => file write TYPE_NULL
			case false => file write TYPE_BOOLEAN_FALSE
			case true => file write TYPE_BOOLEAN_TRUE
			case d: Long =>
				file write TYPE_LONG
				file writeLong d
			case d: Int =>
				file write TYPE_INT
				file writeInt d
			case d: Double =>
				file write TYPE_DOUBLE
				file writeDouble d
			case d: String =>
				val utf = Codec.toUTF8( d )
				
				if (utf.length > 8) {
					file write TYPE_STRING
					
					val addr = alloc( utf.length + 4 )
					
					file writeLong addr
					file seek addr
					file writeInt utf.length
				} else
					file write utf.length
				
				file write utf
				
			case _ => sys.error( "type not supported: " + datum )
		}
	}
	
	protected def alloc( size: Int ) = {
		file seek FILE_FREE_PTR
		
		val ptr = file.readLong
		
		if (ptr == NULL || size > BLOCK_SIZE) {
			val addr = file.length
			val blocks = size/BLOCK_SIZE + (if (size%BLOCK_SIZE == 0) 0 else 1)
			
			file.setLength( addr + blocks*BLOCK_SIZE )
			file seek addr
			addr
		} else {
			file seek ptr
			
			val n = file readLong
			
			file seek FILE_FREE_PTR
			file writeLong n
			file seek ptr
			ptr
		}
	}
	
	protected def free( start: Long, size: Int ) {
		val blocks = size/BLOCK_SIZE + (if (size%BLOCK_SIZE == 0) 0 else 1)
		
		for (i <- 0 until blocks) {
			val block = start + i*BLOCK_SIZE
			
			file seek FILE_FREE_PTR
			
			val next = file readLong
			
			file seek block
			file writeLong next
			file seek FILE_FREE_PTR
			file writeLong block
		}
	}
	
	private def hex( n: Long* ) = println( n map (a => "%h" format a) mkString ("(", ", ", ")") )

}

/* Example B+ Tree File Format
 * ***************************

fixed length ASCII text			"B+ Tree v0.1" (12)
branching factor						short (2)
free block pointer					long (8)
root node pointer						long (8)
====================================
Leaf Node
------------------------------------
type												0 (1)
parent pointer							long (8)
length											short (2)
key/value array
	key type									byte (1)
	key data									(8)
	. . .
prev pointer								long (8)
next pointer								long (8)
value array
	value type								byte (1)
	value data								(8)
	. . .

*/