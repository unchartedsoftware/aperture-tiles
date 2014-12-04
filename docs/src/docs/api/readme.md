---
section: Docs
subtitle: API
chapter: Overview
permalink: docs/api/index.html
layout: submenu
---

# Overview

The Aperture Tiles Annotation Service REST API is based on the Aperture Tiles Binning API. Annotations are submitted into tiles and aggregated through a given range of levels based on bin-index. Annotations can be written, modified and removed in real time.

## Web Service Paths

Web service paths are relative to a common base URL.  For example, assuming the hostname *localhost* and port *8080*, the URLs of the various services would be:

- `https://localhost:8080/{version}/annotation/{layer}/{uuid}/{level}/{x}/{y}.{ext}`
- `https://localhost:8080/annotation/{layer}/{level}/{x}/{y}.{ext}`
- `https://localhost:8080/{version}/annotation`
- `https://localhost:8080/annotation`
- `https://localhost:8080/{version}/layer/{layer}`
- `https://localhost:8080/layer/{layer}`
- `https://localhost:8080/{version}/layer`
- `https://localhost:8080/layer`


## Getting Started

To begin, review the available [Data Types](datatypes/).