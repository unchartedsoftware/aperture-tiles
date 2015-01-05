---
section: Docs
subtitle: API
chapter: Tile
permalink: docs/api/tile/index.html
layout: submenu
---

# Tile Service #

## <a name="get-tile"></a> Get Tile ##

Returns a tile for a given layer at the specified level and index.

<div class="props">
	<h3 class="sectionTitle">Method Summary</h3>
		<nav>
			<table class="summaryTable">
				<thead>
					<tr>
						<th scope="col">Attributes</th>
						<th scope="col">Description</th>
					</tr>
				</thead>
				
				<tbody>
					<tr class='item0'>
						<td class="attributes">URL</td>
						<td class="nameDescription">
							<div class="description">/{version}/tile/{layer}/{level}/{x}/{y}.{ext}
								<br>/tile/{layer}/{level}/{x}/{y}.{ext}</div>
						</td>
					</tr>
					
					<tr class='item1'>
						<td class="attributes">Method</td>
						<td class="nameDescription">
							<div class="description">`GET`</div>
						</td>
					</tr>
					
					<tr class='item2'>
						<td class="attributes">Params</td>
						<td class="nameDescription">
							<div class="description">Attributes to override in the "public" node of the server configuration JSON.</div>
						</td>
					</tr>
				</tbody>
			</table>
		</nav>
</div>

Example request:

```http
GET http://localhost:8080/instagram/rest/v1.0/tiles/twitter-heatmap4/2/3.png?renderer.ramp=spectral&renderer.coarseness=2
```

Example response:

```
3.png
```