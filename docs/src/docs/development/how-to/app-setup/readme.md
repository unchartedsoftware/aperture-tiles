---
section: Docs
subsection: Development
chapter: How-To
topic: Set Up the Application
permalink: docs/development/how-to/app-setup/
layout: chapter
---

Set Up the Application
======================

Once you have generated a tile set from your source data, you should setup your Aperture Tiles application. Each Aperture Tiles application has two components:

- A Tile Server, which sends tile data to the client
- A Tile Client, which displays tile data received from the client

The fastest way to create these components is to modify an existing example application. Example applications are available in the [tile-examples/](https://github.com/unchartedsoftware/aperture-tiles/tree/master/tile-examples) directory in the source code.

<h6 class="procedure">To begin configuring your Aperture Tiles application</h6>

1.  Choose the one of the following demos to use as a template:

    <table class="summaryTable" style="width:100%;">
        <thead>
            <tr>
                <th scope="col" style="width:20%;">Demo</th>
                <th scope="col" style="width:35%;">Location</th>
                <th scope="col" style="width:45%;">Description</th>
            </tr>
        </thead>
        <tbody>
            <tr>
                <td class="description">Julia Set</td>
                <td class="description"><a href="https://github.com/unchartedsoftware/aperture-tiles/tree/master/tile-examples/julia-demo">tile-examples/julia-demo/</a></td>
                <td class="description">Server-rendered heatmap on a blank cross-plot baselayer</td>
            </tr>
            <tr>
                <td class="description">Twitter Topics</td>
                <td class="description"><a href="https://github.com/unchartedsoftware/aperture-tiles/tree/master/tile-examples/twitter-topics">tile-examples/twitter-topics/</a></td>
                <td class="description">Server-rendered heatmap with client-rendered word clouds on a geographic Google Maps baselayer</td>
            </tr>
        </tbody>
    </table>

2.  Copy the folder of the demo you want and give it a unique name (e.g., *new&#8209;project*).
3.  Update the Gradle build file (*new-project/build.gradle*) to add or change the following fields:

    <table class="summaryTable" style="width:100%;">
        <thead>
            <tr>
                <th scope="col" style="width:20%;">Field</th>
                <th scope="col" style="width:80%;">Description</th>
            </tr>
        </thead>
        <tbody>
            <tr>
                <td class="property">description</td>
                <td class="description">Project description</td>
            </tr>
            <tr>
                <td class="property">group</td>
                <td class="description">Group ID</td>
            </tr>
            <tr>
                <td class="property">version</td>
                <td class="description">Project version number</td>
            </tr>
        </tbody>
    </table>

## Next Steps ##

For details on configuring a Tile Server to pass tile-based data to the Tile Client, see the [Configure the Tile Server](../tile-server/) topic.