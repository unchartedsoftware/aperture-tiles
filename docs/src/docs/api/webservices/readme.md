---
section: Docs
subtitle: API
chapter: Web Services
permalink: docs/api/webservices/index.html
layout: submenu
---

# Web Services

## <a name="annotations"></a>Annotations

### <a name="read-annotation-tile"></a>Read Annotation Tile

Returns an Annotation Tile for a specified filter configuration UUID, annotation layer, level and tile x and y.

<div class="props">
	<h2 class="sectionTitle">Method Summary</h2>
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
						<td class="attributes">Service URL</td>
						<td class="nameDescription">
							<div class="description">/annotation/{layer}/{uuid}/{level}/{x}/{y}.{ext}</div>
						</td>
					</tr>
					
					<tr class='item1'>
						<td class="attributes">HTTP Method</td>
						<td class="nameDescription">
							<div class="description">GET</div>
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
		<h2 class="sectionTitle">
			Method Detail
		</h2>
		
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
localhost:8080/annotation/test-layer/f47ac10b-58cc-4372-a567-0e02b2c3d479/7/8/14.json
```

Example output:

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
            “group”: "Urgent",   
            “data”: {
                “comment”: … ,
                “author”: … ,
                “date”: … 
            },
            “certificate”: { … }
        }]
    }
}
```

### <a name="write-annotation"></a>Write Annotation

Submits a new annotation to be written to the server. Upon success, a certificate is returned to the client. 

<div class="props">
	<h2 class="sectionTitle">Method Summary</h2>
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
						<td class="attributes">Service URL</td>
						<td class="nameDescription">
							<div class="description">/annotation</div>
						</td>
					</tr>
					
					<tr class='item1'>
						<td class="attributes">HTTP Method</td>
						<td class="nameDescription">
							<div class="description">POST</div>
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
	<h2 class="sectionTitle">Method Summary</h2>
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

```json
{
    “type”: “write”
    “layer”: , “annotation-test-layer-id”,
    “annotation”: {
        “level”: 6,
        “x”: 73.35,
        “y”: -125.6,
        “range”: {
            “min”: 0,
            “max”: 6
        }
        “group”: "Urgent",   
        “data”: {
            “comment”: … ,
            “author”: … ,
            “date”: … 
        }
    }
}
```

Example output:

```json
{
    “uuid”: “f47ac10b-58cc-4372-a567-0e02b2c3d479”,
    “timestamp”: “1401830862“
}
```

### <a name="modify-annotation"></a>Modify Annotation

Submits a modify request to the server. If the certificate in the annotation state is valid, the operation is processed and a new certificate is returned.

<div class="props">
	<h2 class="sectionTitle">Method Summary</h2>
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
						<td class="attributes">Service URL</td>
						<td class="nameDescription">
							<div class="description">/annotation</div>
						</td>
					</tr>
					
					<tr class='item1'>
						<td class="attributes">HTTP Method</td>
						<td class="nameDescription">
							<div class="description">POST</div>
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
	<h2 class="sectionTitle">Method Summary</h2>
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

```json
{
    “type”: “write”
    “layer”: , “annotation-test-layer-id”,
    “annotation” : {
        “level”: 6,
        “x”: 73.35,
        “y”: -125.6,
        “range”: {
            “min”: 0,
            “max”: 6
        }
        “group”: "Urgent",   
        “data”: {
            “comment”: … ,
            “author”: … ,
            “date”: … 
        },
        “certificate”: {
             “uuid”: “f47ac10b-58cc-4372-a567-0e02b2c3d479”,
            “timestamp”: “1401830862“
        }      
    }
}
```

Example output:

```json
{
    “uuid”: “f47ac10b-58cc-4372-a567-0e02b2c3d479”,
    “timestamp”: “1401830862“
}
```

### <a name="remove-annotation"></a>Remove Annotation

Submits a removal request to the server. If the certificate is valid, the operation will be processed and a success status will be returned.

<div class="props">
	<h2 class="sectionTitle">Method Summary</h2>
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
						<td class="attributes">Service URL</td>
						<td class="nameDescription">
							<div class="description">/annotation</div>
						</td>
					</tr>
					
					<tr class='item1'>
						<td class="attributes">HTTP Method</td>
						<td class="nameDescription">
							<div class="description">POST</div>
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
	<h2 class="sectionTitle">Method Summary</h2>
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

```json
{
    “type”: “write”
    “layer”: , “annotation-test-layer-id”,
    “certificate”: {
        “uuid”: “f47ac10b-58cc-4372-a567-0e02b2c3d479”,
        “timestamp”: “1401830862“
    }
}
```

Example output:

```json
{
}
```

## <a name="layers"></a>Layers

### <a name="configure-layer"></a>Configure Layer

Submits a new filter configuration. Upon success, returns a JSON object with the new filter UUID under the key “uuid”.

<div class="props">
	<h2 class="sectionTitle">Method Summary</h2>
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
						<td class="attributes">Service URL</td>
						<td class="nameDescription">
							<div class="description">/annotation</div>
						</td>
					</tr>
					
					<tr class='item1'>
						<td class="attributes">HTTP Method</td>
						<td class="nameDescription">
							<div class="description">POST</div>
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
	<h2 class="sectionTitle">Method Summary</h2>
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
							<div class="description">"configure"</div>
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
						<td class="attributes">configuration</td>
						<td class="nameDescription">
							<div class="description">JSON object holding the new layer specification overrides.</div>
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

```json
{
    “type”: “configure”
    “layer”: “annotation-test-layer-id”,
    “configuration”: {
        “filter”: {
            “type”: “n-most-recent-by-group”,
            “countsByGroup”: {
                “urgent”:10
            }
        }
    }
}
```

Example output:

```json
{
    “uuid”: “f47ac10b-58cc-4372-a567-0e02b2c3d479”,
}
```

### <a name="unconfigure-layer"></a>Unconfigure Layer

Requests the server to release a filter configuration from its memory.

<div class="props">
	<h2 class="sectionTitle">Method Summary</h2>
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
						<td class="attributes">Service URL</td>
						<td class="nameDescription">
							<div class="description">/annotation</div>
						</td>
					</tr>
					
					<tr class='item1'>
						<td class="attributes">HTTP Method</td>
						<td class="nameDescription">
							<div class="description">POST</div>
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
	<h2 class="sectionTitle">Method Summary</h2>
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
							<div class="description">"unconfigure"</div>
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
						<td class="attributes">uuid</td>
						<td class="nameDescription">
							<div class="description">uuid for the filter to be unconfigured.</div>
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
				</tbody>
			</table>
		</nav>
</div>

Example request:

```json
{
    “type”: “unconfigure”
    “layer”: “annotation-test-layer-id”,
    “uuid” : “f47ac10b-58cc-4372-a567-0e02b2c3d479”
}
```

Example output:

```json
{
}
```

### <a name="request-available-annotation-layers"></a>Request Available Annotation Layers

Requests all available annotation layer specifications held by the server.

<div class="props">
	<h2 class="sectionTitle">Method Summary</h2>
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
						<td class="attributes">Service URL</td>
						<td class="nameDescription">
							<div class="description">/annotation</div>
						</td>
					</tr>
					
					<tr class='item1'>
						<td class="attributes">HTTP Method</td>
						<td class="nameDescription">
							<div class="description">POST</div>
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
							<div class="description">Array of all available annotation layers</div>
						</td>
					</tr>
					
				</tbody>
			</table>
		</nav>
</div>

<div class="props">
	<h2 class="sectionTitle">Method Summary</h2>
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
							<div class="description">"list"</div>
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
				</tbody>
			</table>
		</nav>
</div>

Example request:

```rest
{
    “type”: “list”
}
```

Example output:

```json
[ 
    { 
        "id" : "bitcoin.source.amount.annotations",
        "name" : "Source vs Amount Annotation Service",
        "description": "Source vs Amount bitcoina annotations",
        "pyramid" : {
            "type" : "AreaOfInterest",
            "minX" : 1.0,
            "maxX" : 6336769,
            "minY" : 0,
            "maxY" : 500000
         },
         "data": {
            "pyramidio": {
                "type": "hbase",
                "hbase.zookeeper.quorum": "hadoop-s1.oculus.local",
                "hbase.zookeeper.port": "2181",
                "hbase.master": "hadoop-s1.oculus.local:60000"
            },
            "serializer": {
                "type": "string->[(string, long)]-j"
            }
        },
        "groups": [ "Urgent", "High", "Medium", "Low" ],
        "filter" : {
            “type”: “n-most-recent-by-group”,
            “countsByGroup”: {
                “urgent”:10
            }
        }  
    }
]
```

