package xyz.hyperreal.btree

import xyz.hyperreal.ramfile.RamFile

import scala.io.Codec


class FileBPlusTree( order: Int ) extends AbstractBPlusTree[String, Any, Long]( order ) {
	val LEAF_NODE = 0
	val INTERNAL_NODE = 1
	
	val TYPE_STRING = 0x10
	val TYPE_INT = 0x11
	val TYPE_LONG = 0x12
	val TYPE_DOUBLE = 0x13
	val TYPE_BOOLEAN = 0x14
	val TYPE_NULL = 0x15
	
	val FILE_HEADER = 0
	val FILE_ORDER = FILE_HEADER + 12
	val FILE_FREE_PTR = FILE_ORDER + 2
	val FILE_ROOT_PTR = FILE_FREE_PTR + 8
	val FILE_BLOCKS = FILE_ROOT_PTR + 8
	
	val NODE_TYPE = 0
	val NODE_PARENT_PTR = NODE_TYPE + 4
	val NODE_LENGTH = NODE_PARENT_PTR + 1
	val NODE_KEYS = NODE_LENGTH + 8
	
	val DATUM_SIZE = 1 + 8		// type + datum
	
	val DATA_ARRAY_SIZE = (order - 1)*DATUM_SIZE
	
	val LEAF_PREV_PTR = NODE_KEYS + DATA_ARRAY_SIZE
	val LEAF_NEXT_PTR = LEAF_PREV_PTR + 8
	val LEAF_VALUES = LEAF_PREV_PTR + 8
	
	val BLOCK_SIZE = LEAF_VALUES + DATA_ARRAY_SIZE
	
	val NUL = 0
	
	val file = new RamFile( "btree" )
	
	var root =
		if (file.length == 0) {
			file writeBytes "B+ Tree v0.1"
			file writeLong NUL
			file writeLong FILE_BLOCKS
			newLeaf( NUL )
		} else {
			file seek FILE_ROOT_PTR
			file readLong
		}
	
	def branch( node: Long, index: Int ): Long = {
		ni
	}
	
	def branches( node: Long ): Seq[Long] = {
		ni
	}
	
	def getKey( node: Long, index: Int ) = readString( node + NODE_KEYS + index*DATUM_SIZE )
	
	def getValue( node: Long, index: Int ) = readDatum( node + NODE_KEYS + nodeLength( node )*DATUM_SIZE + LEAF_VALUES )
	
	def insertBranch( node: Long, index: Int, key: String, branch: Long ) {
		ni
	}
	
	def insertValue( node: Long, index: Int, key: String, value: Any ) {
		val len = nodeLength( node )
		
		file seek NODE_LENGTH
		file writeLong (len + 1)
		
		if (index < len) {
			val keydata = new Array[Byte]( (len - index)*DATUM_SIZE )
			
			file seek (NODE_KEYS + index*DATUM_SIZE)
			file readFully keydata
			file seek (NODE_KEYS + (index + 1)*DATUM_SIZE)
			file write keydata
		}
		
		file seek (NODE_KEYS + index*DATUM_SIZE)
		
	}
	
	def isLeaf( node: Long ): Boolean = {
		file seek node
		file.read == LEAF_NODE
	}
	
	private def readDatum( addr: Long ) = {
		def readUTF8( len: Int ) = {
			val a = new Array[Byte]( len )
			
			file readFully a
			new String( Codec fromUTF8 a )
		}
		
		file seek addr
		
		file read match {
			case len if len <= 0x08 => readUTF8( len )
			case TYPE_STRING => 
				file seek file.readLong
				readUTF8( file readInt )
			case TYPE_INT => file readInt
		}
	}
	
	private def readString( addr: Long ) = readDatum( addr ).asInstanceOf[String]
	
	private def writeDatum( addr: Long, datum: Any ) = {
		
	}
	
	def keys( node: Long ): Seq[String] =
		new Seq[String] {
			def apply( idx: Int ) = getKey( node, idx )
			
			def iterator =
				new Iterator[String] {
					var len = nodeLength( node )
					var index = 0
					
					def hasNext = index < len
					
					def next = {
						if (!hasNext) throw new NoSuchElementException( "no more keys" )
							
						val res = getKey( node, index )
						
						index += 1
						res
					}
				}
				
			def length = {
				file seek (node + NODE_LENGTH)
				file.readInt
			}
		}
	
	def nodeLength( node: Long ) = {
		file seek (node + NODE_LENGTH)
		file.readShort.toInt
	}
	
	def moveInternal( src: Long, begin: Int, end: Int, dst: Long ) {
		ni
	}
	
	def moveLeaf( src: Long, begin: Int, end: Int, dst: Long ) {
		ni
	}
	
	def newInternal( parent: Long ): Long = {
		ni
	}
	
	private def alloc = {
		val addr = file.length
		
		file seek addr
		
		for (_ <- 1 to BLOCK_SIZE)
			file write 0
		
		file seek addr
		addr
	}
	
	def newLeaf( parent: Long ): Long = {
		val node = alloc
		
		file write LEAF_NODE
		file writeLong parent
		node
	}
	
	def newRoot( branch: Long ): Long = {
		ni
	}
	
	def next( node: Long ): Long = {
		file seek LEAF_NEXT_PTR
		file readLong
	}
	
	def next( node: Long, p: Long ) {
		ni
	}
	
	def nul: Long = 0
	
	def parent( node: Long ): Long = {
		file seek NODE_PARENT_PTR
		file readLong
	}
	
	def parent( node: Long, p: Long ) {
		ni
	}
	
	def prev( node: Long ): Long = {
		file seek LEAF_PREV_PTR
		file readLong
	}
	
	def prev( node: Long, p: Long ) {
		ni
	}
	
	def setValue( node: Long, index: Int, v: Any ) {
		ni
	}
	
	def values( node: Long ): Seq[Any] = {
		ni
	}

	private def ni = sys.error( "not implemented" )

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