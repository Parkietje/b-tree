package xyz.hyperreal

/**
 * Provides an abstract class for building and using B+ Trees.
 * 
 * ==Overview==
 * The class to extend is [[AbstractBPlusTree]].  It is designed to be both generic (type parameters for keys and values, and an abstract type for node references) and general (doesn't care how the tree is stored). An extending class needs to implement a number of simple methods and the node type that 	provide storage abstraction.
 */
package object btree {}