---
section: Docs
subsection: Development
chapter: API
topic: Layer
permalink: docs/development/api/layer/
layout: chapter
---

Layer Service
=============

## Configuration ##

All server configurations adhere to the following general format:

```json
[
    {
        id: "...",
        public: {
        }
        private :{
        }
    }
]
```

Data under the **public** node is accessible from the client, while data under the **private** note is not.

The minimal required fields are:

```json
[ 
    {
        id: "...",
        public: {
            pyramid: {
                type: "...."
            }
        }
        private :{
            data : {
                id: "...",
                pyramidio : {
                    ...
                }
            }
        }   
    }
]
```

## Request All Layers ##

Return an object containing an array of:

- All available layers
- A version number

Layer information returned is the **public** node of the server-side configuration JSON. The layer IDs and metadata are appended to the returned JSON under the **id** and **meta** attributes, respectively.

<div class="props">
    <h3 class="sectionTitle">Method Summary</h3>
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
                    <div class="description">/{version}/layers
                        <br>/layers</div>
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
                    <div class="description">None</div>
                </td>
            </tr>
        </tbody>
    </table>
</div>

Example request:

```http
GET http://localhost:8080/instagram/rest/v1.0/layers/
```

Example response:

```json
{
    layers: [
        {
            id: "instagram-heatmap",
            pyramid: {
                type: "WebMercator"
            }
            meta: { … },
            renderer : {
                ramp: "spectral",
                coarseness: 2
            },
            valueTransform: {
                type: "log10"
            }
        },
        {
            id: "instagram-keyword-heatmap",
            pyramid: {
                type: "WebMercator"
            },
            meta: { … }
        }
    ],
    version: "v1.0"
}
```

## Request Specific Layer ##

Returns the requested layer information. Layer information returned is the **public** node of the server-side configuration JSON. The layer IDs and metadata are appended to the returned JSON under **id** and **meta** attributes, respectively.

<div class="props">
    <h3 class="sectionTitle">Method Summary</h3>
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
                    <div class="description">/{version}/layers/{layerId}
                        <br>/layers/{layerId}</div>
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
                    <div class="description">None</div>
                </td>
            </tr>
        </tbody>
    </table>
</div>

Example request:

```http
GET http://localhost:8080/instagram/rest/v1.0/layers/instagram-heatmap
```

Example response:

```json
{
    layer: {
        id: "instagram-heatmap",
        pyramid: {
            type: "WebMercator"
         }
        meta: { … },
        renderer : {
            ramp: "spectral",
            coarseness: 2
        },
        valueTransform: {
            type: "log10"
        }
    },
    version: "v1.0"
}
```

## Store a Configuration State ##

Store a configuration state on the server that can be accessed at a later time. Returns the SHA-256 hash of the state. This SHA-256 hash can be used later as a 'state' query parameter. Hashes are deterministic and cachable.

<div class="props">
    <h3 class="sectionTitle">Method Summary</h3>
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
                    <div class="description">/{version}/layers/{layerId}/states
                        <br>layers/{layerId}/states</div>
                </td>
            </tr>

            <tr class='item1'>
                <td class="attributes">Method</td>
                <td class="nameDescription">
                    <div class="description">`POST`</div>
                </td>
            </tr>

            <tr class='item2'>
                <td class="attributes">Params</td>
                <td class="nameDescription">
                    <div class="description">None</div>
                </td>
            </tr>
        </tbody>
    </table>
</div>

Example request:

```http
POST http://localhost:8080/instagram/rest/v1.0//layer/instagram-heatmap/states
```

```json
{
    tileTransform: {
        data: {
            vars: [ … ]
        }
    }
}
```

Example response:

```json
{
    version: "v1.0",
    state: "e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855"
}
```

## Get Configuration State ##

Returns a specified configured state stored on the server for a particular layer.

<div class="props">
    <h3 class="sectionTitle">Method Summary</h3>
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
                    <div class="description">/{version}/layers/{layerId}/states/{stateId}
                        <br>layers/{layerId}/states/{stateId}</div>
                </td>
            </tr>

            <tr class='item1'>
                <td class="attributes">HTTP Method</td>
                <td class="nameDescription">
                    <div class="description">`GET`</div>
                </td>
            </tr>

            <tr class='item2'>
                <td class="attributes">Params</td>
                <td class="nameDescription">
                    <div class="description">None</div>
                </td>
            </tr>
        </tbody>
    </table>
</div>

Example request:

```http
GET http://localhost:8080/instagram/rest/v1.0//layer/instagram-heatmap/states/e5f8e8aa55e008aeb9a3bbf40d93da4a5630112cf280e2f7e12245e219044031
```

Example response:

```json
{  
   state:{
      tileTransform:{
         type:"identity"
      },
      pyramid:{
         maxY:0,
         maxX:0,
         type:"WebMercator",
         minX:0,
         minY:0
      },
      valueTransform:{
         min:5e-324,
         max:1.7976931348623157e+308,
         layerMax:0,
         layerMin:0,
         type:"linear"
      },
      renderer:{
         to:"0x000000",
         halign:0.5,
         from-alpha:-1,
         theme:"dark",
         to-alpha:-1,
         from:"0xffffff",
         type:"heatmap",
         ramp:"hot",
         coarseness:1,
         rangeMin:0,
         opacity:1,
         rangeMax:100,
         components:[

         ],
         valign:0.5,
         gradients:[

         ]
      }
   },
   version:"v1.0"
}
```

## Get Configuration States ##

Returns all configured states stored on the server for a particular layer, including the default state.

<div class="props">
    <h3 class="sectionTitle">Method Summary</h3>
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
                    <div class="description">/{version}/layers/{layerId}/states
                        <br>layers/{layerId}/states</div>
                </td>
            </tr>

            <tr class='item1'>
                <td class="attributes">HTTP Method</td>
                <td class="nameDescription">
                    <div class="description">`GET`</div>
                </td>
            </tr>

            <tr class='item2'>
                <td class="attributes">Params</td>
                <td class="nameDescription">
                    <div class="description">None</div>
                </td>
            </tr>
        </tbody>
    </table>
</div>

Example request:

```http
GET http://localhost:8080/instagram/rest/v1.0//layer/instagram-heatmap/states
```

Example response:

```json
{  
    states:{
       default:{
          tileTransform:{
             type:"identity"
          },
          pyramid:{
             maxY:0,
             maxX:0,
             type:"WebMercator",
             minX:0,
             minY:0
          },
          valueTransform:{
             min:5e-324,
             max:1.7976931348623157e+308,
             layerMax:0,
             layerMin:0,
             type:"linear"
          },
          renderer:{
             to:"0x000000",
             halign:0.5,
             from-alpha:-1,
             theme:"dark",
             to-alpha:-1,
             from:"0xffffff",
             type:"heatmap",
             ramp:"spectral",
             coarseness:1,
             rangeMin:0,
             opacity:1,
             rangeMax:100,
             components:[

             ],
             valign:0.5,
             gradients:[

             ]
          }
       },
       e5f8e8aa55e008aeb9a3bbf40d93da4a5630112cf280e2f7e12245e219044031:{  
          tileTransform:{
             type:"identity"
          },
          pyramid:{
             maxY:0,
             maxX:0,
             type:"WebMercator",
             minX:0,
             minY:0
          },
          valueTransform:{
             min:5e-324,
             max:1.7976931348623157e+308,
             layerMax:0,
             layerMin:0,
             type:"linear"
          },
          renderer:{
             to:"0x000000",
             halign:0.5,
             from-alpha:-1,
             theme:"dark",
             to-alpha:-1,
             from:"0xffffff",
             type:"heatmap",
             ramp:"hot",
             coarseness:1,
             rangeMin:0,
             opacity:1,
             rangeMax:100,
             components:[

             ],
             valign:0.5,
             gradients:[

             ]
          }
       }
   },
   version:"v1.0"
}
```