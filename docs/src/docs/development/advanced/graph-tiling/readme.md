---
section: Docs
subsection: Development
chapter: Advanced
topic: Graph Tiling Jobs
permalink: docs/development/advanced/graph-tiling/
layout: chapter
---

Graph Tiling Jobs
=================

In addition to [heatmap layers](../../how-to/standard-tiling/) of individual data points, Aperture Tiles supports visualizations of graph datasets that contain edge and node information. Graph visualizations illustrate the relationships between nodes and communities across multiple zoom levels. The process of generating the [tile pyramid](../../getting-started/tile-pyramid/) that represents this type of layer is a graph tiling job.

Graph tiling jobs comprise several configuration and generation phases as described in the [Graph Tiling Process](#graph-tiling-process) section.

**NOTE**: The graph tiling capabilities of Aperture Tiles are considered experimental.

## Graph Tiling Process ##

Aperture Tiles requires graph data to be in a comma- or tab-delimited format (CSV). If your data is in a GraphML format, the first step in the graph tiling process is to [convert the GraphML data to a CSV format](#converting-graphml-data-to-csv-data).

Once your source data is in a valid delimited format, you can choose to apply either of the following optional layout styles before proceeding with the tile generation:

1. [Hierarchical Clustering](#hierarchical-clustering): Hierarchically group nodes into communities using Louvain detection based on modularity-maximization.
2. [Graph Layout](#graph-layout): Compute node locations using a force-directed algorithm.

Once your data is prepared, you can execute your tile generation job using [standard](#tile-generation) or [customized](#graph-analytics) methods.

## Converting GraphML Data to CSV Data ##

Aperture Tiles requires graph data to be in comma- or tab-delimited format (CSV). GraphML data can be converted to CSV using the following tools in [com.oculusinfo.tilegen.graph.util](https://github.com/unchartedsoftware/aperture-tiles/tree/master/tile-generation/src/main/java/com/oculusinfo/tilegen/graph/util):

- The [GraphmlParser](https://github.com/unchartedsoftware/aperture-tiles/blob/master/tile-generation/src/main/java/com/oculusinfo/tilegen/graph/util/GraphmlParser.java) class.
- The [GraphParseApp](https://github.com/unchartedsoftware/aperture-tiles/blob/master/tile-generation/src/main/java/com/oculusinfo/tilegen/graph/util/GraphParseApp.java) Java application.

### GraphParseApp ###

[GraphParseApp](https://github.com/unchartedsoftware/aperture-tiles/blob/master/tile-generation/src/main/java/com/oculusinfo/tilegen/graph/util/GraphParseApp.java) is an example Java application for converting GraphML data to CSV. 

<h6 class="procedure">To execute the GraphParseApp and convert your GraphML data to a CSV file</h6>

- Use the following command line syntax:

    ```bash
    java GraphParseApp –in source.graphml -out output.csv -longIDs true 
    –nAttr nAttr1, nAttr2 -eAttr eAttr1, eAttr2 -nCoordAttr NO
    ```

    Where:

    <table class="summaryTable" style="width:100%;">
        <thead>
            <tr>
                <th scope="col" style="width:20%;">Argument</th>
                <th scope="col" style="width:10%;">Required?</th>
                <th scope="col" style="width:70%;">Description</th>
            </tr>
        </thead>
        <tbody>
            <tr>
                <td class="property">-in</td>
                <td class="description">Yes</td>
                <td class="description">Path and filename of GraphML input file.</td>
            </tr>
            <tr>
                <td class="property">-out</td>
                <td class="description">Yes</td>
                <td class="description">Path and filename of tab-delimited output file.</td>
            </tr>
            <tr>
                <td class="property">-longIDs</td>
                <td class="description">No</td>
                <td class="description">Indicates whether nodes should be assigned a unique long ID (<em>true</em>) regardless of the ID format in the original file. This ID convention is needed for data processing with Spark's GraphX library. Defaults to <em>false</em>.</td>
            </tr>
            <tr>
                <td class="property">-nAttr</td>
                <td class="description">No</td>
                <td class="description">List of node attributes to parse. Enter as a list of attribute ID tags separated by commas. Defaults to <em>all</em> node attributes.</td>
            </tr>
            <tr>
                <td class="property">-eAttr</td>
                <td class="description">No</td>
                <td class="description">List of edge attributes to parse. Enter as a list of attribute ID tags separated by commas. Defaults to <em>all</em> edge attributes.</td>
            </tr>
            <tr>
                <td class="property">-nCoordAttr</td>
                <td class="description">No</td>
                <td class="description">Node attributes to use for node co-ordinates. Enter as a list of attribute ID tags separated by commas. Defaults to <em>NO</em>, which indicates no co-ordinate data should be associated with nodes.</td>
            </tr>
        </tbody>
    </table>

#### Output ####

The GraphParseApp outputs a file that contains tab-delimited data. The first column denotes whether each record is a *node* or an *edge* object.

<table class="summaryTable" style="width:100%;">
    <thead>
        <tr>
            <th scope="col" style="width:50%;">Node object columns</th>
            <th scope="col" style="width:50%;">Edge object columns</th>
        </tr>
    </thead>
    <tbody>
        <tr>
            <td class="description">
                <ul>
                    <li>Long ID identifier</li>
                    <li>Original string name</li>
                    <li>Any additional node attributes in the source file or just those passed in with the <strong>-nAttr</strong> argument</li>
                </ul>
            </td>
            <td class="description">
                <ul>
                    <li>ID of the source node</li>
                    <li>ID of the destination node</li>
                    <li>Any additional edge attributes in the source file or just those passed in with the <strong>-eAttr</strong> argument</li>
                </ul>
            </td>
        </tr>
    </tbody>
</table>

GraphParseApp also creates a README file containing column labels for all node and edge records in the CSV file.

#### Workflow Example ####

Consider a brain [connectomics](http://en.wikipedia.org/wiki/Connectomics) GraphML dataset with the following node and edge formats:

```xml
<node id="n0">
    <data key="v_region">rh-rostralmiddlefrontal</data>
    <data key="v_centroid">[59, 47, 91]</data>
    <data key="v_id">n0</data>
    <data key="v_latent_pos">NA</data>
    <data key="v_scan1">40</data>
    <data key="v_tri">30</data>
    <data key="v_clustcoeff">0.666667</data>
    <data key="v_degree">10</data>
</node>
...
<edge source="n361054" target="n364322">
    <data key="e_weight">19</data>
</edge>
...
```

To visualize this data, we are only interested in the **v\_region** and **v\_degree** node attributes and the **e\_weight** edge attribute (i.e., *all* edge attributes). Therefore, the proper GraphParseApp command line syntax is:

```bash
java GraphParseApp –in <graphML file> -out <output CSV file> -longIDs true 
–nAttr v_region, v_degree
```

GraphParseApp then outputs a file containing records for all of your nodes with the following format:

<table class="summaryTable" style="width:100%;">
    <thead>
        <tr>
            <th scope="col" colspan="6">Node columns</th>
        </tr>
    </thead>
    <tbody>
        <tr>
            <td class="description">node</td>
            <td class="description">0</td>
            <td class="description">n0</td>
            <td class="description">rh</td>
            <td class="description">rostralmiddlefrontal</td>
            <td class="description">10</td>
        </tr>
    </tbody>
</table>

<table class="summaryTable" style="width:100%;">
    <thead>
        <tr>
            <th scope="col" colspan="4">Edge columns</th>
        </tr>
    </thead>
    <tbody>
        <tr>
            <td class="description">edge</td>
            <td class="description">361054</td>
            <td class="description">364322</td>
            <td class="description">19</td>
        </tr>
    </tbody>
</table>

## Hierarchical Clustering ##

CSV datasets can be hierarchically clustered using the [GraphClusterApp](https://github.com/unchartedsoftware/aperture-tiles/blob/master/tile-generation/src/main/scala/com/oculusinfo/tilegen/graph/cluster/GraphClusterApp.scala) Scala application in [com.oculusinfo.tilegen.graph.cluster](https://github.com/unchartedsoftware/aperture-tiles/tree/master/tile-generation/src/main/scala/com/oculusinfo/tilegen/graph/cluster). GraphClusterApp groups nodes into communities using Louvain community detection based on modularity-maximization.

The number of hierarchical levels (*X*) is dependent on your data, the modularity of the graph, how sparse or dense the dataset is, etc. Hierarchy level\_0 corresponds to the raw unclustered data, while level\_X is the most clustered (top-level communities).

IDs and labels for parent communities are automatically chosen as the underlying child node with highest weighted degree (i.e., sum of weights of incident edges for a give node). The metadata for a parent community is also chosen in the same manner.

### GraphClusterApp ###

GraphClusterApp's implementation of the Louvain clustering algorithm uses Sotera's [distributed Louvain modularity algorithm](https://github.com/Sotera/distributed-louvain-modularity) in conjunction with Spark's [GraphX graph processing library](https://github.com/Sotera/spark-distributed-louvain-modularity).

<h6 class="procedure">To execute the GraphClusterApp and hierarchically cluster your data</h6>

-   Use the following command line syntax:

    ```bash
    spark-submit --class com.oculusinfo.tilegen.graph.cluster.GraphClusterApp 
    <tile-generation-assembly.jar> -source <hdfs source location> -output 
    <hdfs output location> -onlyEdges false -nID 1 -nAttr 2 -eSrcID 1 -eDstID 2 
    -eWeight 3 -spark local -sparkhome /opt/spark -user <username> 
    ```

    Where: 

    <table class="summaryTable" style="width:100%;">
        <thead>
            <tr>
                <th scope="col" style="width:20%;">Argument</th>
                <th scope="col" style="width:14%;">Required?</th>
                <th scope="col" style="width:66%;">Description</th>
            </tr>
        </thead>
        <tbody>
            <tr>
                <td class="property">-source</td>
                <td class="description">Yes</td>
                <td class="description">HDFS location of the input data.</td>
            </tr>
            <tr>
                <td class="property">-output</td>
                <td class="description">Yes</td>
                <td class="description">HDFS location to which to save clustered results.</td>
            </tr>
            <tr>
                <td class="property">-onlyEdges</td>
                <td class="description">No</td>
                <td class="description">Indicates whether the source data contains only edges (*true*). Defaults to *false*.</td>
            </tr>
            <tr>
                <td class="property">-parts</td>
                <td class="description">No</td>
                <td class="description">Number of partitions into which to break up the source dataset. Defaults to value chosen automatically by Spark.</td>
            </tr>
            <tr>
                <td class="property">-p</td>
                <td class="description">No</td>
                <td class="description">Amount of parallelism for Spark-based data processing of source data. Defaults to value chosen automatically by Spark.</td>
            </tr>
            <tr>
                <td class="property">-d</td>
                <td class="description">No</td>
                <td class="description">Source dataset delimiter. Defaults to tab-delimited.</td>
            </tr>
            <tr>
                <td class="property">-progMin</td>
                <td class="description">No</td>
                <td class="description">Percent of nodes that must change communities for the algorithm to consider progress relative to total vertices in a level. Defaults to <em>0.15</em>.</td>
            </tr>
            <tr>
                <td class="property">-progCount</td>
                <td class="description">No</td>
                <td class="description">Number of times the algorithm can fail to make progress before exiting. Defaults to <em>1</em>.</td>
            </tr>
            <tr>
                <td class="property">-nID</td>
                <td class="description">When <strong>&#8209;onlyEdges</strong> = <em>false</em></td>
                <td class="description">Number of the column in the raw data that contains the node IDs. Note that IDs must be of type <em>long</em>.</td>
            </tr>
            <tr>
                <td class="property">-nAttr</td>
                <td class="description">No</td>
                <td class="description">Column numbers in the raw data that contain additional node metadata that should be parsed and saved with cluster results. Individual attribute tags should be separated by commas.</td>
            </tr>
            <tr>
                <td class="property">-eSrcID</td>
                <td class="description">Yes</td>
                <td class="description">Number of the column in the raw data that contains the edge source IDs. Note that IDs must be of type <em>long</em>.</td>
            </tr>
            <tr>
                <td class="property">-eDstID</td>
                <td class="description">Yes</td>
                <td class="description">Number of the column in the raw data that contains the edge destination IDs. Note that IDs must be of type <em>long</em>.</td>
            </tr>
            <tr>
                <td class="property">-eWeight</td>
                <td class="description">No</td>
                <td class="description">Number of the column in the raw data that contains the edge weights. Defaults to <em>-1</em>, meaning that no edge weighting is used.</td>
            </tr>
            <tr>
                <td class="property">-spark</td>
                <td class="description">Yes</td>
                <td class="description">Spark master location.</td>
            </tr>
            <tr>
                <td class="property">-sparkhome</td>
                <td class="description">Yes</td>
                <td class="description">Spark HOME location.</td>
            </tr>
            <tr>
                <td class="property">-user</td>
                <td class="description">No</td>
                <td class="description">Spark/Hadoop username associated with the Spark job.</td>
            </tr>
        </tbody>
    </table>

#### Input ####

GraphClusterApp accepts two types of graph data formats:

-   **Node and edge tab-delimited data**, where the first column contains the keyword *node* or *edge*
-   **Edge-only tab-delimited data** with different columns for source ID, destination ID and edge weight (*optional*). 

    <p class="list-paragraph">In this case, all nodes will be inferred internally from the parsed edges, but no node attributes or metadata will be associated with the clustered nodes or communities.</p>

**NOTE**: GraphClusterApp requires that node IDs and edge weights are of type *long*.

#### Output ####

Clustered results are stored sub-directories in the **-output** HDFS location. Each hierarchical level from *0* to *X* has its own separate sub-directory.

Within each hierarchical level, clustered data is stored in the following tab-delimited format for nodes/communities and edges:

<table class="summaryTable" style="width:100%;">
    <thead>
        <tr>
            <th scope="col" colspan="6">Node columns</th>
        </tr>
    </thead>
    <tbody>
        <tr>
            <td class="description">node</td>
            <td class="description">ID</td>
            <td class="description">parent ID</td>
            <td class="description">number of internal nodes</td>
            <td class="description">node degree</td>
            <td class="description">metadata (<em>optional</em>)</td>
        </tr>
    </tbody>
</table>

<table class="summaryTable" style="width:100%;">
    <thead>
        <tr>
            <th scope="col" colspan="4">Edge columns</th>
        </tr>
    </thead>
    <tbody>
        <tr>
            <td class="description">edge</td>
            <td class="description">srcID</td>
            <td class="description">dstID</td>
            <td class="description">edge weight</td>
        </tr>
    </tbody>
</table>

The modularity (*q-value*) is also saved for each hierarchical level in a *_qvalues* sub-directory.

### Workflow Example ###

The following example illustrates command line syntax for GraphClusterApp with Spark in local mode:

```bash
spark-submit --class com.oculusinfo.tilegen.graph.cluster.GraphClusterApp 
<tile-generation-assembly.jar> -source <hdfs source location> -output 
<hdfs output location> -onlyEdges false -nID 1 -nAttr 2 -eSrcID 1 -eDstID 2 
-eWeight 3 -spark local -sparkhome /opt/spark -user <username>
```

In this case, the source dataset contains both nodes and edges (**-onlyEdges** *false*) with the following columns:

-   For nodes:
    -   Column 1 contains the node IDs (**-nID** *1*)
    -   Column 2 contains node metadata (**-nAttr** *2*)
-   For edges
    -   Column 1 contains source IDs (**-eSrcID** *1*)
    -   Column 2 contains destination IDs (**-eDstID** *2*)
    -   Column 3 contains the edge weights (**-eWeight** *3*)

Using the brain connectomics dataset as an example:

- Graph community *X* at hierarchy level 2 contains 100 communities (from hierarchy 1)
- If the child community with the highest weighted degree has ID *0* and metadata *rh-rostralmiddlefrontal*, community *X* at hierarchy 2 will be labeled with the same ID and metadata.

## Graph Layout ##

Node positions can be computed using a hierarchical force-directed algorithm with the following tools in [com.oculusinfo.tilegen.graph.util](https://github.com/unchartedsoftware/aperture-tiles/tree/master/tile-generation/src/main/scala/com/oculusinfo/tilegen/graph/util):

- The [HierarchicFDLayout](https://github.com/unchartedsoftware/aperture-tiles/blob/master/tile-generation/src/main/scala/com/oculusinfo/tilegen/graph/util/HierarchicFDLayout.scala) class.
- The [ClusteredGraphLayoutApp](https://github.com/unchartedsoftware/aperture-tiles/blob/master/tile-generation/src/main/scala/com/oculusinfo/tilegen/graph/util/ClusteredGraphLayoutApp.scala) Scala application.

The hierarchic force-directed algorithm runs in a distributed manner using Spark's GraphX library. The layout of each hierarchy level is determined in sequence starting with the highest hierarchical level. Graph communities are defined as circles, where size is based on the number of internal raw nodes in the community.

### ClusterGraphLayoutApp ###

[ClusteredGraphLayoutApp](https://github.com/unchartedsoftware/aperture-tiles/blob/master/tile-generation/src/main/scala/com/oculusinfo/tilegen/graph/util/ClusteredGraphLayoutApp.scala)

<h6 class="procedure">To execute the ClusterGraphLayoutApp and position your node/community data</h6>

-   User the following command line syntax:

    ```bash
    spark-submit --class com.oculusinfo.tilegen.graph.util.ClusteredGraphLayoutApp 
    <tile-generation-assembly.jar> -source <hdfs source location> -output 
    <hdfs output location> -i 1000 -maxLevel 4 -layoutLength 256 -nArea 45 
    -border 2 -eWeight true -g 0 -spark local -sparkhome /opt/spark
    -user <username>
    ```

    Where:

    <table class="summaryTable" style="width:100%;">
        <thead>
            <tr>
                <th scope="col" style="width:21%;">Argument</th>
                <th scope="col" style="width:10%;">Required?</th>
                <th scope="col" style="width:69%;">Description</th>
            </tr>
        </thead>
        <tbody>
            <tr>
                <td class="property">-source</td>
                <td class="description">Yes</td>
                <td class="description">HDFS location of the clustered input graph data.</td>
            </tr>
            <tr>
                <td class="property">-output</td>
                <td class="description">Yes</td>
                <td class="description">HDFS location to which to save graph layout results.</td>
            </tr>
            <tr>
                <td class="property">-parts</td>
                <td class="description">No</td>
                <td class="description">Number of partitions into which to break up the source dataset. Defaults to value chosen automatically by Spark.</td>
            </tr>
            <tr>
                <td class="property">-p</td>
                <td class="description">No</td>
                <td class="description">Amount of parallelism for Spark-based data processing of source data. Defaults to value chosen automatically by Spark.</td>
            </tr>
            <tr>
                <td class="property">-d</td>
                <td class="description">No</td>
                <td class="description">Source dataset delimiter. Defaults to tab-delimited.</td>
            </tr>
            <tr>
                <td class="property">-i</td>
                <td class="description">No</td>
                <td class="description">Maximum number of iterations for the force-directed algorithm. Defaults to <em>500</em>.</td>
            </tr>
            <tr>
                <td class="property">-maxLevel</td>
                <td class="description">No</td>
                <td class="description">Highest cluster hierarchic level to use for determining the graph layout. Defaults to <em>0</em>.</td>
            </tr>
            <tr>
                <td class="property">-border</td>
                <td class="description">No</td>
                <td class="description">Percent of the parent bounding box to leave as whitespace between neighbouring communities during initial layout. Defaults to <em>2</em> percent.</td>
            </tr>
            <tr>
                <td class="property">-layoutLength</td>
                <td class="description">No</td>
                <td class="description">Desired width/height of the total graph layout region. Defaults to <em>256.0</em>.</td>
            </tr>
            <tr>
                <td class="property">-nArea</td>
                <td class="description">No</td>
                <td class="description">Area of all node circles with a given parent community. Controls the amount of whitespace in the graph layout. Defaults to <em>30</em> percent.</td>
            </tr>
            <tr>
                <td class="property">-eWeight</td>
                <td class="description">No</td>
                <td class="description">Indicates whether to use edge weights to scale force-directed attraction forces (<em>true</em>). Defaults to <em>false</em>.</td>
            </tr>
            <tr>
                <td class="property">-g</td>
                <td class="description">No</td>
                <td class="description">Amount of gravitational force to use for force-directed layout to prevent outer nodes from spreading out too far. Defaults to <em>0</em> (no gravity).</td>
            </tr>
            <tr>
                <td class="property">-spark</td>
                <td class="description">Yes</td>
                <td class="description">Spark master location.</td>
            </tr>
            <tr>
                <td class="property">-sparkhome</td>
                <td class="description">Yes</td>
                <td class="description">Spark HOME location.</td>
            </tr>
            <tr>
                <td class="property">-user</td>
                <td class="description">No</td>
                <td class="description">Spark/Hadoop username associated with the Spark job.</td>
            </tr>
        </tbody>
    </table>

#### Input ####

The *source* location should correspond to the root directory of hierarchically clustered data. The force-directed layout algorithm works in conjunction with hierarchically clustered data; it expects all hierarchical levels to be in separate sub-directories labeled *level_##*.

**NOTE**: Not all hierarchy levels are required to generate a layout.

ClusterGraphLayoutApp requires the source clustered graph data to be a tab-delimited format analogous to that used by the Louvain Clustering application:

<table class="summaryTable" style="width:100%;">
    <thead>
        <tr>
            <th scope="col" colspan="6">Node columns</th>
        </tr>
    </thead>
    <tbody>
        <tr>
            <td class="description">node</td>
            <td class="description">ID</td>
            <td class="description">parent ID</td>
            <td class="description">number of internal nodes</td>
            <td class="description">node degree</td>
            <td class="description">metadata (<em>optional</em>)</td>
        </tr>
    </tbody>
</table>

<table class="summaryTable" style="width:100%;">
    <thead>
        <tr>
            <th scope="col" colspan="4">Edge columns</th>
        </tr>
    </thead>
    <tbody>
        <tr>
            <td class="description">edge</td>
            <td class="description">srcID</td>
            <td class="description">dstID</td>
            <td class="description">edge weight</td>
        </tr>
    </tbody>
</table>

#### Output ####

Graph layout results are stored separately for each hierarchical level. Each level has the following tab-delimited format:

<table class="summaryTable" style="width:100%;">
    <thead>
        <tr>
            <th scope="col" colspan="10">Node columns</th>
        </tr>
    </thead>
    <tbody>
        <tr>
            <td class="description">node</td>
            <td class="description">ID</td>
            <td class="description">XY coords</td>
            <td class="description">radius</td>
            <td class="description">parent ID</td>
            <td class="description">parent XY coords</td>
            <td class="description">parent XY radius</td>
            <td class="description">num internal nodes</td>
            <td class="description">degree</td>
            <td class="description">metadata (<em>optional</em>)</td>
        </tr>
    </tbody>
</table>

<table class="summaryTable" style="width:100%;">
    <thead>
        <tr>
            <th scope="col" colspan="7">Edge columns</th>
        </tr>
    </thead>
    <tbody>
        <tr>
            <td class="description">edge</td>
            <td class="description">srcID</td>
            <td class="description">src XY coords</td>
            <td class="description">dstID</td>
            <td class="description">dest XY coords</td>
            <td class="description">edge weight</td>
            <td class="description">isInter<wbr>Community<wbr>Edge</td>
        </tr>
    </tbody>
</table>

**NOTE**: The final edge column will be *0* for intra-community edges (both endpoints have the same parent community) or *1* for inter-community edges.

ClusterGraphLayoutApp also saves a separate *stats* sub-directory that contains general information about the layout of each hierarchical level including:

- Number of nodes and edges
- Min and max radii for the graph communities

**NOTE**: Radii information for each hierarchical level is used to provide a minimum recommended zoom level for tile generation. It is advantageous from a graph visualization standpoint to have a parent community radius to correspond to approximately one tile length at a given zoom level.

#### Example Workflow ####

Consider a graph dataset clustered up to hierarchy 4. The expected directory structure for the graph layout application is:

```text
../<hdfs source location>
                        /level_4/
                        /level_3/
                        /level_2/
                        /level_1/
                        /level_0/
```

The following command line syntax defines a layout for the clustered graph dataset with:

- 4 hierarchical levels (to only use a subset of hierarchy levels, set **-maxlevel** to exclude hierarchies from the layout process)
- Force-directed algorithm set to 1000 iterations
- 45% non-whitespace
- Edge weights
- No gravity force

```bash
spark-submit --class com.oculusinfo.tilegen.graph.util.ClusteredGraphLayoutApp 
<tile-generation-assembly.jar> -source <hdfs source location> -output 
<hdfs output location> -i 1000 -maxLevel 4 -layoutLength 256 -nArea 45 -border 2 
-eWeight true -g 0 -spark local -sparkhome /opt/spark -user <username>
```

## Tile Generation ##

Once a graph dataset has been converted to CSV format and the nodes have been positioned (using raw positions for geo-located data or using the hierarchical force-directed algorithm), tile generation can be performed. Standard heatmaps of nodes and edges can be generated using the [CSVGraphBinner](https://github.com/unchartedsoftware/aperture-tiles/blob/master/tile-generation/src/main/scala/com/oculusinfo/tilegen/examples/apps/CSVGraphBinner.scala) application in [com.oculusinfo.tilegen.examples.apps](https://github.com/unchartedsoftware/aperture-tiles/tree/master/tile-generation/src/main/scala/com/oculusinfo/tilegen/examples/apps)

**NOTE**: For information on performing custom tile generation jobs, see [Graph Analytics](#graph-analytics).

### CSVGraphBinner ###

In general, the CSVGraphBinner application works similarly to the standard Aperture Tiles [CSVBinner](../../how-to/standard-tiling/#csvbinner). It ingests a properties file (\*.bd) and create a collection of Avro tile data files.

**NOTE**: Heatmaps of graph nodes and edges must be generated separately.

#### Nodes ####

The following BD file parameters are used to configure CSVGraphBinner to generate tiles of graph nodes. For additional information on BD file parameters, see the [CSVBinner](../../how-to/standard-tiling/#csvbinner) section of the [Standard Tiling Jobs](../standard-tiling/) topic.

```properties
# Indicate that you want to create a tile set of node elements
oculus.binning.graph.data=nodes

# Specify the delimiter character. Defaults to tab (\t)
oculus.binning.parsing.separator=\t

# Specify the column numbers of your x/y coordinates
oculus.binning.parsing.x.index=2
oculus.binning.parsing.y.index=3

# Map the x/y coordinates to the cartesian index scheme used by the binner
oculus.binning.index.field.0=x
oculus.binning.index.field.1=y

# Define the projection (x/y cross-plot) over which to draw the nodes and 
# manually specify the min/max bounds
oculus.binning.projection.type=areaofinterest
oculus.binning.projection.autobounds=false
oculus.binning.projection.minX=0.0
oculus.binning.projection.maxX=256.0
oculus.binning.projection.minY=0.0
oculus.binning.projection.maxY=256.0
```

Given these parameters, the application parses columns 2 and 3 of each node object and uses them as x/y coordinates during tile generation. By default, the count of nodes is used as the binning value.

#### Edges ####

The following BD file parameters are used to configure CSVGraphBinner to generate tiles of graph edges. For additional information on BD file parameters, see the [CSVBinner](../../how-to/standard-tiling/#csvbinner) section of the [Standard Tiling Jobs](../standard-tiling/) topic.

```properties
# Indicate that you want to create a tile set of edge elements
oculus.binning.graph.data=edges

# Specify the delimiter character. Defaults to tab (\t)
oculus.binning.parsing.separator=\t

# Specify the edge types you want to include: inter, intra or all
oculus.binning.graph.edges.type=all

# Column number that indicates whether an edge is inter-community (1)
# or intra-community (0)
oculus.binning.graph.edges.type.index=0

# Indicate whether to draw edges as straight lines (false) or clockwise 
# arcs (true)
oculus.binning.line.style.arcs=true

# Specify the column numbers of your source (x/y) and destination (x2/y2)
# coordinates
oculus.binning.parsing.x.index=2
oculus.binning.parsing.y.index=3
oculus.binning.parsing.x2.index=5
oculus.binning.parsing.y2.index=6

# Specify the column number of your edge weights and indicate how to aggregate
# them
oculus.binning.parsing.v.index=7
oculus.binning.parsing.v.fieldAggregation=add

# Use the line segment index scheme
oculus.binning.index.type=segment

# Map the x/y coordinates to the line segment index scheme used by the binner
oculus.binning.xField=x
oculus.binning.yField=y
oculus.binning.xField2=x2
oculus.binning.yField2=y2

# Set the edge weights as the binning value
oculus.binning.value.field=v

# Define the projection (x/y cross-plot) over which to draw the nodes and 
# manually specify the min/max bounds
oculus.binning.projection.type=areaofinterest
oculus.binning.projection.autobounds=false
oculus.binning.projection.minX=0.0
oculus.binning.projection.maxX=256.0
oculus.binning.projection.minY=0.0
oculus.binning.projection.maxY=256.0
```

Given these parameters, the application parses two endpoints representing the source and destination coordinates of each edge object, and draws a line using the edge weight parsed from column 7.

#### Edge Length Considerations ####

On a zoom level, any edges longer than 1024 bins (4 tiles) or shorter than 2 bins are excluded. This is done for visualization purposes (it is not possible to discern two discrete endpoints on the screen for very short or long lines), as well as to optimize processing time for high zoom levels. These thresholds can be modified using the following BD file parameters:

```properties
oculus.binning.line.max.bins=1024
oculus.binning.line.min.bins=2 
```

Alternatively, long line segments can be included in the tiling job by setting:

```properties
oculus.binning.line.drawends=true
```

In this case, long line segments are rendered within one tile length of each endpoint (since both endpoints will not be on the screen simultaneously), and the line intensity will be faded-out to 0 as it gets farther away from an endpoint.

#### Hierarchical Tile Generation ####

By default, tiles for a nodes and edges are generated using one hierarchy level for all zoom levels. 

For example, even though hierarchical info of a graph's nodes may be available if the data has been Louvain clustered, it may be preferable to only use the raw nodes (hierarchy level 0) for tile generation. However, for a dense graph with many edges, it may be worthwhile to assign different hierarchy levels to different zooms. This can be accomplished using the following parameters:

```properties
oculus.binning.hierarchical.clusters=true
oculus.binning.source.levels.0=hdfs://hadoop/graphdata /level_3
oculus.binning.source.levels.1=hdfs://hadoop/graphdata /level_2
oculus.binning.source.levels.2=hdfs://hadoop/graphdata /level_1
oculus.binning.source.levels.3=hdfs://hadoop/graphdata /level_0

oculus.binning.levels.0=0-3
oculus.binning.levels.1=4-6
oculus.binning.levels.2=7,8
oculus.binning.levels.3=9-11
```

These settings instruct tile generation to use:

- Hierarchy level 3 for zoom levels 0-3
- Hierarchy level 2 for zoom levels 4-6
- Hierarchy level 1 for zoom levels 7-8
- Hierarchy level 0 for zoom levels 9-11

#### Hierarchical Tile Generation of Graph Edges ####

For clustered graph data, it can helpful to visualize intra and inter-community edges on separate layers (using the **oculus.binning.graph.edges.type** switch).

For example, if intra-community edges are tiled for hierarchy L for a given zoom, then it may be desirable to show all inter-community edges going between the parent communities of that hierarchy. To accomplish this, tile all edges for hierarchy L+1.

Sample configuration for intra-community edges for hierarchy level 2 at zoom levels 4-6:

```properties
oculus.binning.hierarchical.clusters=true
oculus.binning.source.levels.0=hdfs://hadoop/graphdata /level_2
oculus.binning.levels.1=4-6
oculus.binning.graph.data=edges
oculus.binning.graph.edges.type=intra
```

Sample configuration for inter-community edges between parent communities (i.e., tiling all edges for hierarchy level 3 (L+1) at zoom levels 4-6):

```properties
oculus.binning.hierarchical.clusters=true
oculus.binning.source.levels.0=hdfs://hadoop/graphdata /level_3
oculus.binning.levels.1=4-6
oculus.binning.graph.data=edges
oculus.binning.graph.edges.type=all
```

## Graph Analytics ##

You can also create custom analytics of key communities at each hierarchy for client-side rendering to display labels, metadata and add interactive features as desired. Custom analytics can be created using the [GraphAnalyticsBinner](https://github.com/unchartedsoftware/aperture-tiles/blob/master/tile-generation/src/main/scala/com/oculusinfo/tilegen/graph/analytics/GraphAnalyticsBinner.scala) in [com.oculusinfo.tilegen.graph.analytics](https://github.com/unchartedsoftware/aperture-tiles/tree/master/tile-generation/src/main/scala/com/oculusinfo/tilegen/graph/analytics).

This application uses many of the same BD file parameters for hierarchical tile generation as described in the previous section. Instead of generating tiles of 256x256 bins, each tile contains one GraphAnalyticsRecord object. 

By default, each GraphAnalyticsRecord contains stats about the 25 largest graph communities in a given tile. These stats include:

- Community ID
- x/y coordinates and radius
- Number of internal nodes
- Degree
- Metadata
- Parent community coordinates and radius

A list of BD file parameters for parsing each of these stats is given below.

The following parameters specify the column number of key graph community/node attributes:

<table class="summaryTable" style="width:100%;">
    <thead>
        <tr>
            <th scope="col" style="width:20%;">Argument</th>
            <th scope="col" style="width:80%;">Description</th>
        </tr>
    </thead>
    <tbody>
        <tr>
            <td class="property">oculus.binning.graph.x.index</td>
            <td class="description">x-axis coordinate</td>
        </tr>
        <tr>
            <td class="property">oculus.binning.graph.y.index</td>
            <td class="description">y-axis coordinate</td>
        </tr>
        <tr>
            <td class="property">oculus.binning.graph.id.index</td>
            <td class="description">Long ID</td>
        </tr>
        <tr>
            <td class="property">oculus.binning.graph.r.index</td>
            <td class="description">Radius</td>
        </tr>
        <tr>
            <td class="property">oculus.binning.graph.numnodes.index</td>
            <td class="description">Number of nodes</td>
        </tr>
        <tr>
            <td class="property">oculus.binning.graph.degree.index</td>
            <td class="description">Degree</td>
        </tr>
        <tr>
            <td class="property">oculus.binning.graph.metadata.index</td>
            <td class="description">Metadata</td>
        </tr>
        <tr>
            <td class="property">oculus.binning.graph.parentID.index</td>
            <td class="description">Long ID of the parent community</td>
        </tr>
    </tbody>
</table>

The following parameters specify the column number of key parent community attributes:

<table class="summaryTable" style="width:100%;">
    <thead>
        <tr>
            <th scope="col" style="width:20%;">Argument</th>
            <th scope="col" style="width:80%;">Description</th>
        </tr>
    </thead>
    <tbody>
        <tr>
            <td class="property">oculus.binning.graph.parentID.index</td>
            <td class="description">Long ID</td>
        </tr>
        <tr>
            <td class="property">oculus.binning.graph.parentR.index</td>
            <td class="description">Radius</td>
        </tr>
        <tr>
            <td class="property">oculus.binning.graph.parentX.index</td>
            <td class="description">x-axis coordinate</td>
        </tr>
        <tr>
            <td class="property">oculus.binning.graph.parentY.index</td>
            <td class="description">y-axis coordinate</td>
        </tr>
    </tbody>
</table>

It is possible to save stats on the 10 highest weighted edges incident on a given community using the following parameters. **NOTE**: If these parameters are excluded, no edge analytics information will be saved for each GraphAnalyticsRecord.

<table class="summaryTable" style="width:100%;">
    <thead>
        <tr>
            <th scope="col" style="width:20%;">Argument</th>
            <th scope="col" style="width:80%;">Description</th>
        </tr>
    </thead>
    <tbody>
        <tr>
            <td class="property">oculus.binning.graph.edge.srcID.index</td>
            <td class="description">Source ID of each graph edge</td>
        </tr>
        <tr>
            <td class="property">oculus.binning.graph.edges.dstID.index</td>
            <td class="description">Destination ID of each graph edge</td>
        </tr>
        <tr>
            <td class="property">oculus.binning.graph.edges.weight.index</td>
            <td class="description">Weight of each graph edge. Defaults to <em>1</em> (unweighted).</td>
        </tr>
    </tbody>
</table>

The number of communities to store per record can be tuned using the **oculus.<wbr>binning.<wbr>graph.<wbr>maxcommunities** parameter (set to 25 by default).

Similarly, the number of edges to store per graph community can be tuned using the **oculus.<wbr>binning.<wbr>graph.<wbr>maxedges** parameter (set to 10 by default).