import xyz.hyperreal.btree.{MemoryBPlusTree, FileBPlusTree}


object Main extends App {
	val memoryTree = new MemoryBPlusTree[String, Any]( 5 )
	
	memoryTree.insertKeys( "k", "z", "p", "d", "b", "v", "h", "x", "o", "y", "c", "t", "j", "n", "f", "l", "s", "q", "i", "m", "e", "u", "w", "a", "g", "r" )
	memoryTree.diagram( "memoryTree" )
	
	val fileTree = new FileBPlusTree( "btree", 5, true )
	
	fileTree.insertKeys( "k", "z", "p", "d", "b", "v", "h", "x", "o", "y", "c", "t", "j", "n", "f", "l", "s", "q", "i", "m", "e", "u", "w", "a", "g", "r" )
	fileTree.diagram( "fileTree" )
}