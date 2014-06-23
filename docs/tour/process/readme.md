---
section: Tour
subtitle: Process
permalink: tour/process/index.html
layout: default
---

Aperture Tiles Process
=======================

Aperture Tiles uses a multi-stage process to develop tile-based visual analytics. 

1. The aggregation stage projects and bins data into a predefined grid, such as the Tiling Map Service standard.  This uses a (z,x,y) coordinate system, where z identifies the zoom level, and x,y identifies a specific tile on the plot for the given zoom level. 

2. The summarization stage applies one or more summary statistics or other analytics to the data in each tile region, storing the result in a data store. 

3. The rendering stage maps the summary to a visual representation, and renders it to an image tile or html at request time. This stage is capable of rendering on-demand support interactions such as filtering, or rich dynamic interactions such as brushing and drill-downs, if using client-side rendering with html and JavaScript.

![Aperture Tiles Architectural Overview](../../img/arch-overview.png)