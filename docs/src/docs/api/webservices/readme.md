---
section: Docs
subtitle: API
chapter: Web Services
permalink: docs/api/webservices/index.html
layout: submenu
---

# Web Services #

## <a name="read-annotation-tile"></a>Read Annotation Tile ##

Returns an Annotation Tile for a specified filter configuration UUID, annotation layer, level and tile x and y.

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
							<div class="description">/{version}/annotation/{layerId}/{level}/{x}/{y}/{z}.{ext}</div>
						</td>
					</tr>
					
					<tr class='item1'>
						<td class="attributes">Method</td>
						<td class="nameDescription">
							<div class="description">`GET`</div>
						</td>
					</tr>
					
					<tr class='item2'>
						<td class="attributes">Output MIME Type</td>
						<td class="nameDescription">
							<div class="description">application/json</div>
						</td>
					</tr>
					
					<tr class='item3'>
						<td class="attributes">Output</td>
						<td class="nameDescription">
							<div class="description">Annotation Tile</div>
						</td>
					</tr>
					
				</tbody>
			</table>
		</nav>
</div>

<div class="details props">
	<div class="innerProps">
		<h3 class="sectionTitle">
			Method Detail
		</h3>
		
		<ul class="methodDetail" id="MethodDetail">
			<dl class="detailList params">
				<dt class="heading">URL Parameters:</dt>
				
				<dt>
					<b>layer</b>
				</dt>
				<dd>Annotation layer ID.</dd>
				
				<dt>
					<b>uuid</b>
				</dt>
				<dd>Filter configuration UUID. Submit <em>default</em> to use the default server filter configuration.</dd>
				
				<dt>
					<b>level</b>
				</dt>
				<dd>Zoom level, where 0 is highest level of aggregation.</dd>
				
				<dt>
					<b>x</b>
				</dt>
				<dd>Horizontal tile index, where 0 represents the leftmost column of tiles.</dd>
						
				<dt>
					<b>y</b>
				</dt>
				<dd>Vertical tile index, where 0 represents the bottommost row of tiles.</dd>
						
				<dt>
					<b>.ext</b>
				</dt>
				<dd>The extension of the annotation format. Only JSON is currently supported.</dd>
			</dl>
		</ul>
	</div>
</div>

Example request:

```http
GET http://localhost:8080/instagram/rest/v1.0/annotation/parlor-annotations/4/1/2/3.json
```

Example response:

```json
{
	“tile” : {
		“level”: 7,
		“xIndex”: 8,
		“yIndex”: 4
	},
	“annotations” : {
		“[0,0]”: [{
			“level”: 7,
			“x”: 73.35,
			“y”: -125.6,
			“range”: {
					“min”: 0,
					“max”: 9
			}
		]
	}
}
```

## <a name="write-annotation"></a>Write Annotation ##

Submits a new annotation to be written to the server. Upon success, a certificate containing the UUID and timestamp is returned to the client.

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
							<div class="description">/{version}/annotation/{layerId}/</div>
						</td>
					</tr>
					
					<tr class='item1'>
						<td class="attributes">Method</td>
						<td class="nameDescription">
							<div class="description">`POST`</div>
						</td>
					</tr>
					
					<tr class='item2'>
						<td class="attributes">Output MIME Type</td>
						<td class="nameDescription">
							<div class="description">application/json</div>
						</td>
					</tr>
					
					<tr class='item3'>
						<td class="attributes">Output</td>
						<td class="nameDescription">
							<div class="description">AnnotationCertificate</div>
						</td>
					</tr>
					
				</tbody>
			</table>
		</nav>
</div>

<div class="props">
	<h3 class="sectionTitle">Method Detail</h3>
		<nav>
			<table class="summaryTable">
				<thead>
					<tr>
						<th scope="col">Request Parameter</th>
						<th scope="col">Description</th>
						<th scope="col">Data Type</th>
						<th scope="col">Single/Multiple</th>
						<th scope="col">Required?</th>
					</tr>
				</thead>
				
				<tbody>
					<tr class='item0'>
						<td class="attributes">type</td>
						<td class="nameDescription">
							<div class="description">"write"</div>
						</td>
						<td class="nameDescription">
							<div class="description">String</div>
						</td>
						<td class="nameDescription">
							<div class="description">Single</div>
						</td>
						<td class="nameDescription">
							<div class="description">Yes</div>
						</td>
					</tr>
					
					<tr class='item1'>
						<td class="attributes">layer</td>
						<td class="nameDescription">
							<div class="description">annotation layer id</div>
						</td>
						<td class="nameDescription">
							<div class="description">String</div>
						</td>
						<td class="nameDescription">
							<div class="description">Single</div>
						</td>
						<td class="nameDescription">
							<div class="description">Yes</div>
						</td>
					</tr>
					
					<tr class='item2'>
						<td class="attributes">annotation</td>
						<td class="nameDescription">
							<div class="description">annotation</div>
						</td>
						<td class="nameDescription">
							<div class="description">JSON</div>
						</td>
						<td class="nameDescription">
							<div class="description">Single</div>
						</td>
						<td class="nameDescription">
							<div class="description">Yes</div>
						</td>
					</tr>					
				</tbody>
			</table>
		</nav>
</div>

Example request:

```html
POST http://localhost:8080/instagram/rest/v1.0/annotation/parlor-annotations/
```

```json
{
    "type": "write"
    "layer": , "annotation-test-layer-id",
    "annotation": {
        "level": 6,
        "x": 73.35,
        "y": -125.6,
        "range": {
            "min": 0,
            "max": 6
        }
        "group": "Urgent",   
        "data": {
            "comment": … ,
            "author": … ,
            "date": … 
        }
    }
}
```

Example response:

```json
{
    "uuid": "f47ac10b-58cc-4372-a567-0e02b2c3d479",
    "timestamp": "1401830862"
}
```

## <a name="modify-annotation"></a>Modify Annotation ##

Submits a modify request to the server. If the certificate in the annotation state is valid, the operation is processed and a new certificate is returned.

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
							<div class="description">/{version}/annotation/{layerId}/</div>
						</td>
					</tr>
					
					<tr class='item1'>
						<td class="attributes">Method</td>
						<td class="nameDescription">
							<div class="description">`POST`</div>
						</td>
					</tr>
					
					<tr class='item2'>
						<td class="attributes">Output MIME Type</td>
						<td class="nameDescription">
							<div class="description">application/json</div>
						</td>
					</tr>
					
					<tr class='item3'>
						<td class="attributes">Output</td>
						<td class="nameDescription">
							<div class="description">AnnotationCertificate</div>
						</td>
					</tr>
					
				</tbody>
			</table>
		</nav>
</div>

<div class="props">
	<h3 class="sectionTitle">Method Detail</h3>
		<nav>
			<table class="summaryTable">
				<thead>
					<tr>
						<th scope="col">Request Parameter</th>
						<th scope="col">Description</th>
						<th scope="col">Data Type</th>
						<th scope="col">Single/Multiple</th>
						<th scope="col">Required?</th>
					</tr>
				</thead>
				
				<tbody>
					<tr class='item0'>
						<td class="attributes">type</td>
						<td class="nameDescription">
							<div class="description">"modify"</div>
						</td>
						<td class="nameDescription">
							<div class="description">String</div>
						</td>
						<td class="nameDescription">
							<div class="description">Single</div>
						</td>
						<td class="nameDescription">
							<div class="description">Yes</div>
						</td>
					</tr>
					
					<tr class='item1'>
						<td class="attributes">layer</td>
						<td class="nameDescription">
							<div class="description">annotation layer id</div>
						</td>
						<td class="nameDescription">
							<div class="description">String</div>
						</td>
						<td class="nameDescription">
							<div class="description">Single</div>
						</td>
						<td class="nameDescription">
							<div class="description">Yes</div>
						</td>
					</tr>
					
					<tr class='item2'>
						<td class="attributes">current</td>
						<td class="nameDescription">
							<div class="description">The annotation after modification. Must contain a valid certificate key.</div>
						</td>
						<td class="nameDescription">
							<div class="description">JSON</div>
						</td>
						<td class="nameDescription">
							<div class="description">Single</div>
						</td>
						<td class="nameDescription">
							<div class="description">Yes</div>
						</td>
					</tr>					
				</tbody>
			</table>
		</nav>
</div>

Example request:

```html
POST http://localhost:8080/instagram/rest/v1.0/annotation/parlor-annotations/
```

```json
{
    "type": "write"
    "layer": , "annotation-test-layer-id",
    "annotation" : {
        "level": 6,
        "x": 73.35,
        "y": -125.6,
        "range": {
            "min": 0,
            "max": 6
        }
        "group": "Urgent",   
        "data": {
            "comment": … ,
            "author": … ,
            "date": … 
        },
        "certificate": {
             "uuid": "f47ac10b-58cc-4372-a567-0e02b2c3d479",
            "timestamp": "1401830862"
        }      
    }
}
```

Example response:

```json
{
    "uuid": "f47ac10b-58cc-4372-a567-0e02b2c3d479",
    "timestamp": "1401830862"
}
```

## <a name="remove-annotation"></a>Remove Annotation ##

Submits a removal request to the server. If the certificate is valid, the operation will be processed and a success status will be returned.

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
							<div class="description">/{version}/annotation/{layerId}/</div>
						</td>
					</tr>
					
					<tr class='item1'>
						<td class="attributes">HTTP Method</td>
						<td class="nameDescription">
							<div class="description">`POST`</div>
						</td>
					</tr>
					
					<tr class='item2'>
						<td class="attributes">Output MIME Type</td>
						<td class="nameDescription">
							<div class="description">application/json</div>
						</td>
					</tr>
					
					<tr class='item3'>
						<td class="attributes">Output</td>
						<td class="nameDescription">
							<div class="description">JSON</div>
						</td>
					</tr>
					
				</tbody>
			</table>
		</nav>
</div>

<div class="props">
	<h3 class="sectionTitle">Method Detail</h3>
		<nav>
			<table class="summaryTable">
				<thead>
					<tr>
						<th scope="col">Request Parameter</th>
						<th scope="col">Description</th>
						<th scope="col">Data Type</th>
						<th scope="col">Single/Multiple</th>
						<th scope="col">Required?</th>
					</tr>
				</thead>
				
				<tbody>
					<tr class='item0'>
						<td class="attributes">type</td>
						<td class="nameDescription">
							<div class="description">"write"</div>
						</td>
						<td class="nameDescription">
							<div class="description">String</div>
						</td>
						<td class="nameDescription">
							<div class="description">Single</div>
						</td>
						<td class="nameDescription">
							<div class="description">Yes</div>
						</td>
					</tr>
					
					<tr class='item1'>
						<td class="attributes">layer</td>
						<td class="nameDescription">
							<div class="description">annotation layer id</div>
						</td>
						<td class="nameDescription">
							<div class="description">String</div>
						</td>
						<td class="nameDescription">
							<div class="description">Single</div>
						</td>
						<td class="nameDescription">
							<div class="description">Yes</div>
						</td>
					</tr>
					
					<tr class='item2'>
						<td class="attributes">certificate</td>
						<td class="nameDescription">
							<div class="description">The certificate of the annotation to be removed.</div>
						</td>
						<td class="nameDescription">
							<div class="description">JSON</div>
						</td>
						<td class="nameDescription">
							<div class="description">Single</div>
						</td>
						<td class="nameDescription">
							<div class="description">Yes</div>
						</td>
					</tr>					
				</tbody>
			</table>
		</nav>
</div>

Example request:

```html
POST http://localhost:8080/instagram/rest/v1.0/annotation/parlor-annotations/
```

```json
{
    "type": "write"
    "layer": , "annotation-test-layer-id",
    "certificate": {
        "uuid": "f47ac10b-58cc-4372-a567-0e02b2c3d479",
        "timestamp": "1401830862"
    }
}
```

Example response:

```json
{
}
```

<div class="git">
	<h3>Interested in Learning More?</h3>

	<ul>
		<li><a href="../../../tour/overview/">Tour</a>: Take our tour to learn more about Aperture Tiles.
		<li><a href="../../development/quickstart/">Quick Start</a>: Our Julia data set provides an example of the process for generating tiles and visualizing them using Aperture Tiles.
		<li><a href="../../../demos/">Live Examples</a>: See our demos page to see live examples of the capabilities of Aperture Tiles.
		<li><a href="../../../download/">Download</a>: For details on downloading pre-packaged versions or acquiring the Aperture Tiles source code visit our download page.
	</ul>
</div>