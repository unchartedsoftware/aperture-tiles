/*
 * Copyright (c) 2014 Oculus Info Inc.
 * http://www.oculusinfo.com/
 *
 * Released under the MIT License.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of
 * this software and associated documentation files (the "Software"), to deal in
 * the Software without restriction, including without limitation the rights to
 * use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies
 * of the Software, and to permit persons to whom the Software is furnished to do
 * so, subject to the following conditions:

 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.

 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package com.oculusinfo.tilegen.graph.util

import org.apache.spark.SparkContext
import org.apache.spark.SparkContext._
import org.apache.spark.rdd.RDD
import org.apache.spark.graphx._
import scala.reflect.ClassTag
import org.apache.spark.HashPartitioner
import scala.collection.mutable.ArrayBuffer
import scala.util.Random

/**
 *  A Force-Directed graph layout algorithm
 *  
 *  Converted from the 'FFDLayouter' code in ApertureJS, which is
 *  based on the FADE algorithm by Quiqley and Edges "FADE: Graph drawing, clustering and visual
 *  abstraction", 2000 as well as Section 2 of the Kobourov paper "Spring Embedders and Force 
 *  Directed Graph Drawing Algorithms", 2012
 *  
 */ 
class ForceDirectedLayout extends Serializable {
	
	def determineLayout[VD: ClassTag](sc: SparkContext, graph: Graph[VD,Long], maxIterations: Int): VertexRDD[(Double, Double)] = {
		
		graph.cache		
		
		//TODO is this helpful?? (node: if going to use groupEdges, then need to do partitionBy first)
		//var graph2 = (Graph(graph.vertices, tempPartitionBy(graph.edges, PartitionStrategy.EdgePartition2D)))
    	//				.groupEdges(_+_)
		
		//----- perform connected component analysis to see if graph is, in fact, separated into multiple sub-graphs
// TODO -- add this in later!
//		val ccGraph = graph.connectedComponents()//.connectedComponents().vertices
//		
//		val cc2 = ccGraph.vertices.map(vPairs => vPairs._2)
//		var maxCC = cc2.max().toInt
//		var minCC = cc2.min().toInt
//
//		//var subGraphList: ArrayBuffer[(Long, RDD[Long])] = ArrayBuffer[(Long, RDD[Long])]()
//
//		val subGraphList = Range(minCC, maxCC+1).flatMap(i => {
//			val subg = ccGraph.subgraph(vpred = (vid, attr) => attr == i)	
//			if (subg.numVertices > 0) {
//				List[RDD[Long]](subg.vertices.map(vd => vd._1))
//			} else {
//				List[RDD[Long]]()
//			}
//		})
		
		//val allSubGraphs = subGraphs.map(p => p._2.map(node => (p._1, node))).reduce(_ union _)

//		for (i <- minCC to maxCC) {
//			// create subgraph of all nodes having same 'connected component nodeID attribute'
//			val subg = ccGraph.subgraph(vpred = (vid, attr) => attr == i)	
//			if (subg.numVertices > 0) {
//				subGraphList += (i, subg.vertices.map(vd => vd._1))  // save this subgraph result to array
//			}
//		}
//		println("Found " + subGraphList.size + " connected components")
			
		// Initially assign random co-ordinates for all nodes 
		// (TODO -- or use pagerank or node degree to sort nodes and make starting positions ...
		// see graphX overview webpage for examples on how to do this)
		val randSeed = 911						
		val nodeCoords = graph.vertices.mapPartitionsWithIndex((indx, iter) => {			
			//TODO -- if want to use the connected component stuff, then append the CC "ID", which should start at 0 and increment by 1, 
			// as a value onto the original set of graph nodes, and then adjust the offx and offy below as needed for each node location
			// below
		
			// need a separate instance of random number generator on each partition
			val random = new Random(indx+randSeed)
			
			iter.map(node => {
				val newX = random.nextDouble //+ offx.toDouble
				val newY = random.nextDouble //+ offy.toDouble
				
				(node._1, (newX, newY))
			})
		})
		var maxX = nodeCoords.map(_._2._1).reduce(_ max _)
		var minX = nodeCoords.map(_._2._1).reduce(_ min _)
		var maxY = nodeCoords.map(_._2._2).reduce(_ max _)
		var minY = nodeCoords.map(_._2._2).reduce(_ min _)			
						
		val graphEdges = graph.edges
		graphEdges.cache
		var graphXY = Graph(nodeCoords, graphEdges)	//create a new graph object that includes XY coords as node attributes
		nodeCoords.unpersist(false)	
		graph.unpersistVertices(false)	
				
		val width = maxX - minX
		val height = maxY - minY
		val area = width * height
		val k = Math.sqrt(area/(graphXY.vertices.count)) // used to scale the vectors
		var temperature = 0.5*Math.min(width, height) 	 // initially you can't take steps larger than 15% of the total view
		val stepLimit = Math.min(width, height)/1000.0
		
		var step = Double.MaxValue
		val theta = 1.0 //Double.parseDouble(System.getProperty("dashboard.layouts.FFD.theta", "1.0")); // theta parameter for the Quigley-Eades algorithm, used to choose node/pseudo-node comparison
		var iteration = 0

		do {
			val results = doForceLayoutStep(sc, graphXY, k, theta, temperature)
			graphXY.unpersistVertices(false)
			
			iteration += 1
			temperature *= (1.0 - iteration/maxIterations) // RHS approaches 1 as you iterate making the scale 0
			step = results._1
			graphXY = Graph(results._2, graphEdges)
			println("Finished iteration " + iteration + " with largest step " + step)	
			
		} while ((step > stepLimit) && (temperature > 0.0))
		
		// Do final scaling of XY co-ordinates from 0 to 100
		maxX = graphXY.vertices.map(_._2._1).reduce(_ max _)
		minX = graphXY.vertices.map(_._2._1).reduce(_ min _)
		maxY = graphXY.vertices.map(_._2._2).reduce(_ max _)
		minY = graphXY.vertices.map(_._2._2).reduce(_ min _)	
					
		val sx = 100.0/(maxX-minX);
		val sy = 100.0/(maxY-minY);
		
		val finalNodePositions = graphXY.vertices.map(n => {
			(n._1, ((n._2._1 - minX)*sx, (n._2._2 - minY)*sx))
		})
	
		println("Force-directed layout completed with " + iteration + " iterations.")	
	
		VertexRDD(finalNodePositions)
	}
	
	/**	doForceLayoutStep
	 * Using the nodes and edges/perform one step of a force-directed layout
	 */
	private def doForceLayoutStep(sc: SparkContext,
			graph: Graph[(Double, Double), Long],
			k: Double, theta: Double, temperature: Double): (Double, VertexRDD[(Double, Double)]) = {
		
		graph.cache
								
		//---- STEP 1: Calculate displacements due to attractive forces
		// For each edge, create a spring attraction
		val displacementsAttr = graph.triplets.flatMap(edge => {	// iterate over edge triplets
			val srcPt = edge.srcAttr	// get current XY coords for src and dst nodes
			val dstPt = edge.dstAttr		
			// calc node displacements due to attractive force for this edge
			val vectSrcToDest = calcAttractionVector(srcPt, dstPt, k)	
			val vectDestToSrc = (-vectSrcToDest._1, -vectSrcToDest._2)
			Seq(edge.srcId -> vectSrcToDest, edge.dstId -> vectDestToSrc)	// save displacement result (nodeId, (x,y))
			
		}).reduceByKey((xy1, xy2) => (xy1._1 + xy2._1, xy1._2 + xy2._2))	// aggregate attractive displacements by nodeId
		displacementsAttr.cache	
		//val displ2 = graph.vertices.aggregateUsingIndex(displacementsAttr, accumXY)	//TODO could do this instead of the reduce by key func above (more efficient)
				
//		//---- STEP 2a: Create Quadtree Decomposition
//		// get a bounding box for the points
//		val maxX = graph.vertices.map(_._2._1).reduce(_ max _)
//		val minX = graph.vertices.map(_._2._1).reduce(_ min _)
//		val maxY = graph.vertices.map(_._2._2).reduce(_ max _)
//		val minY = graph.vertices.map(_._2._2).reduce(_ min _)
//		
//		// create quadtree decomposition
//		val boundingBoxBuffer = 0.05 * Math.max(maxX - minX, maxY - minY);
//		val qt = new QuadTree(minX-boundingBoxBuffer, minY-boundingBoxBuffer, 
//							  maxX - minX + 2.0*boundingBoxBuffer, 
//							  maxY - minY + 2.0*boundingBoxBuffer);
		
//		for (Node nm: nodeList) {
//			Point2D xy = origNPS.get(nm.getId());
//			qt.insert(xy.getX(), xy.getY(), nm);
//		}
		
		
		//---- STEP 2b: Calculate repulsive forces

		// Method I -- Using cartesian product operation ( more parallelized, but requires lots of memory!
		//	 cartesian product list will have size approx (num of nodes)^2 )
		// TODO ...
		// - Should use collesce after cartesian operation, as well as pass in a numPartitions value into
		// 	the reduceByKey func as well.
		// - If memory is an issue then could try the cartesian operation separately on each data partition
		val nodes = graph.vertices	// get RDD of graph nodes
		val displacementsRepulse = (nodes cartesian nodes)
									.filter(n => n._1._1 != n._2._1)
									.map(n => {
										val repulsion = calcRepulsionVector(n._1._2, n._2._2, k)
										(n._1._1, repulsion)
									})
									.reduceByKey((xy1, xy2) => (xy1._1 + xy2._1, xy1._2 + xy2._2))
		displacementsRepulse.cache							
		nodes.unpersist(false)
		
		// Method II -- Using nested loops ( less parallelized so will be slower, but requires less memory )
//		nodes.cache
//		val repulseSumX = sc.accumulator(0.0)
//		val repulseSumY = sc.accumulator(0.0)		
//		val parts = graph.vertices.partitions
//		val displacementsRepulse = sc.parallelize(parts.flatMap(p => {
//			val idx = p.index
//			val partRdd = graph.vertices.mapPartitionsWithIndex((i, iter) => if (i == idx) iter else Iterator(), true)
//			//(the second argument is true to avoid rdd reshuffling)
//			val localNodes = partRdd.collect //localNodes contains all values from a single partition of an RDD array
//			//TODO could broadcast localNodes to all workers??
//
//			val respulseSumNodes = localNodes.map(n => {
//				repulseSumX.setValue(0.0)	//reset accumulators
//				repulseSumY.setValue(0.0)
//
//				// nested for loop to calc repulsion force on each node due to all other nodes
//				nodes.foreach(node2 => {		
//					if (n._1 != node2._1) {
//						val repulsion = calcRepulsionVector(n._2, node2._2, k)
//						repulseSumX += repulsion._1
//						repulseSumY += repulsion._2
//	                }
//				})
//				(n._1 -> (repulseSumX.value, repulseSumY.value))	//save x,y repulseSum results for this node 'n'
//			})
//			respulseSumNodes	//and now concatenate and save x,y repulsion results for all nodes in all partitions 
//		}))
//		nodes.unpersist(blocking=false)
					
		// STEP 3: Apply displacements, limiting step size to temperature
		// track the biggest change we make		
		
		// cal final displacement amounts for each node by aggregating attraction and repulsion displacements together
		val tempSq = temperature*temperature	//temperature squared
		val displacements = VertexRDD(displacementsAttr)
			.innerJoin(displacementsRepulse)((id, xyA, xyR) => (xyA._1 + xyR._1, xyA._2 + xyR._2))
			.map(displ => {
				var displXY = displ._2
				var normSq = calcNormSq(displXY)
				if (normSq > tempSq) {
					// too big, scale the displacement to temp
					val scale = Math.sqrt(tempSq/normSq)
					displXY = (displXY._1*scale, displXY._2*scale) 
					normSq = tempSq
				}
				(displ._1, displXY, normSq)
			})	
			
		displacementsAttr.unpersist(false)
		displacementsRepulse.unpersist(false)

		val largestStep = Math.sqrt(displacements.map(_._3).reduce(_ max _))			//find max of squared norm results for all nodes
		
		val newNodePositions = (graph.vertices).innerJoin(								//innerJoin adds displacements to current node positions
								displacements.map(data => (data._1, data._2)))(			//converts RDD data format to (nodeID, (x,y))		
								(id, xyA, xyR) => (xyA._1 + xyR._1, xyA._2 + xyR._2))	//innerJoin aggregate func
		
		graph.unpersistVertices(false)
		(largestStep, newNodePositions)	// final results for new node positions
	}
	
	
//	private def accumXY(xy1: (Double, Double), xy2: (Double, Double)): (Double, Double) = {
//		(xy1._1 + xy2._1, xy1._2 + xy2._2)
//	}
	
	private def calcAttractionVector(src: (Double, Double), dest: (Double, Double), k: Double): (Double, Double) = {
		val v = (dest._1 - src._1, dest._2 - src._2)
		val scaleFactor = calcNorm(v)/k;	
		(v._1*scaleFactor, v._2*scaleFactor)
	}
	
	private def calcNorm(pt: (Double, Double)): Double = {
		Math.sqrt(pt._1*pt._1 + pt._2*pt._2)
	}
	
	private def calcNormSq(pt: (Double, Double)): Double = {
		(pt._1*pt._1 + pt._2*pt._2)
	}
	
	/**
	 * Give the repulsion affect of 'repulsor' on 'target'
	 * i.e the vector should point FROM repulsor TO target
	 */
	private def calcRepulsionVector(target: (Double, Double), repulsor: (Double, Double), k: Double): (Double, Double) = {
		val v = (target._1 - repulsor._1, target._2 - repulsor._2)
		val scaleFactor = k*k / calcNormSq(v);
		(v._1*scaleFactor, v._2*scaleFactor)
	}
	
	
	/**
	 * Calculate the contribution from all the nodes in qn to the node with the id 'currentID'
	 */
//	private MyPoint calcRepulsionContribution(QuadNode qn, String currentID, Map<String, Point2D> nps, double k, double theta) {
//		Point2D currentPosition = nps.get(currentID);
//		
//		if (qn.getnChildren() == 0)  // nothing to compute
//			return null;
//		if (qn.getnChildren() == 1) { // leaf
//			Object uncastNode = qn.getData().getValue();
//			if (!(uncastNode instanceof Node)) {
//				System.err.println("Failed to find proper data in quadtree");
//			}
//			Node nm = (Node)uncastNode;
//			if (nm.getId().equals(currentID))
//				return null;
//			Point2D currentLeafPosition = nps.get(nm.getId());
//			MyPoint repulsion = getRepulsionVector(currentPosition, currentLeafPosition.getX(), currentLeafPosition.getY(), k);
////			if (nm instanceof IGroupNodeModel) {		//Scale repulsion for group nodes based on number of contained nodes
////				int containedNodes = ((IGroupNodeModel)nm).getChildren().size();
////				repulsion.setXY(repulsion.getX()*containedNodes, repulsion.getY()*containedNodes);
////			}
//			return repulsion;
//		}
//		if (shouldCompareAsPseudoNode(qn, nps.get(currentID), theta)) {
//			Point2D com = qn.getCenterOfMass();
//			MyPoint repulsion = getRepulsionVector(currentPosition, com.getX(), com.getY(), k);
//			repulsion.setXY(repulsion.getX() * qn.getnChildren(), repulsion.getY() * qn.getnChildren());
//			return repulsion;
//		}
//		
//		// failed to resolve a repulsion, try the children
//		MyPoint nwVector = getRepulsionContribution(qn.getNW(), currentID, nps, k, theta);
//		MyPoint neVector = getRepulsionContribution(qn.getNE(), currentID, nps, k, theta);
//		MyPoint swVector = getRepulsionContribution(qn.getSW(), currentID, nps, k, theta);
//		MyPoint seVector = getRepulsionContribution(qn.getSE(), currentID, nps, k, theta);
//		
//		MyPoint out = new MyPoint(0, 0);
//		if (nwVector != null) out.addVectorInPlace(nwVector);
//		if (neVector != null) out.addVectorInPlace(neVector);
//		if (swVector != null) out.addVectorInPlace(swVector);
//		if (seVector != null) out.addVectorInPlace(seVector);
//		
//		return out;
//	}
	

	// TODO -- temporary PartitionBy func to use as a work-around because the graphX version is currently broken in Spark 1.0
	// (see https://github.com/apache/spark/pull/908.patch )
	def tempPartitionBy[ED](edges: RDD[Edge[ED]], partitionStrategy: PartitionStrategy): RDD[Edge[ED]] = {
		val numPartitions = edges.partitions.size
		edges.map(e => (partitionStrategy.getPartition(e.srcId, e.dstId, numPartitions), e))
			.partitionBy(new HashPartitioner(numPartitions))
			.mapPartitions(_.map(_._2), preservesPartitioning = true)
	}
	

}