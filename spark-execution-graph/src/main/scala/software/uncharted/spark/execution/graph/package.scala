package software.uncharted.spark.execution

/**
 * This package contains an exploratory implementation of a full graph execution package.
 *
 * The heart of the implementation is the data class passed between nodes of the graph.  For linear execution
 * pipelines, or even execution trees, straightforward, simple generic parameterization of data types would be
 * sufficient, as the input to any given node would only be from one other node.
 *
 * With graphs, however, the input to a node can come from an arbitrary number of other nodes.  Because of this,
 * we need both a way to aggregate data from multiple nodes, and a way to represent this aggregate data.
 *
 * The representation thereof is ExecutionGraphData.
 *
 * Graph nodes take in ExecutionGraphData objects, and spit out the same; by the magic of this type, these objects
 * and their types can be joined into conglomerate objects and conglomerate types, still retaining proper compiler
 * level checking of all types along the way.
 *
 * Most of the ideas for how this can work come from "Type-Level Programming in Scala"
 * (https://apocalisp.wordpress.com/2010/06/08/type-level-programming-in-scala/).  The implementation of
 * ExecutionGraphData is just a much-simplified version of Harrah's HList, and the implementation of
 * ExecutionGraphNodeInputContainer is an even further simplified version, modified to allow another level of
 * indirection - to allow conglomerations of stages with proper typing of the conglomerations of their outputs.
 * Furthermore, Harrah's HList contains examples of numerous further functionality that could be added to this data
 * class.  I don't think the rest of this functionality is needed, but I can see potential uses for some of it
 * (for instance, the indexing, which would allow one to pull out a specific element from the conglomerated data,
 * or even sub-conglomerates).
 */
package object graph {

}
