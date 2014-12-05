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
							<div class="description">/{version}/tile/{layer}/{level}/{x}/{y}.{ext}</div>
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
							<div class="description">Encoded json representing overrides to the “public” node of the server configuration JSON.
<pre>{ 
	renderer: {
		type: …,
		coarseness: …,
		ramp: …,
		rangeMin: …,
		rangeMax: ...
	},
	valueTransform: {
		type: ...
	},
	tileTransform: {
		type …,
		data: {
			…
		}
	}
}</pre>
							
							</div>
						</td>
					</tr>
				</tbody>
			</table>
		</nav>
</div>

Example request:

```http
GET http://localhost:8080/instagram/rest/v1.0/tiles/twitter-heatmap4/2/3.png?%7Brenderer%3A%7Bramp%3Aspectral%2Ccoarseness%3A2%7D%7D%0A
```

Example response:

```
3.png
```