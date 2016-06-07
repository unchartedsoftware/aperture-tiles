---
section: Docs
subsection: Development
chapter: API
topic: Legend
permalink: docs/development/api/legend/
layout: chapter
---

Legend Service
==============

## Get Encoded Image Key ##

Returns an image describing the values that a tile can take, with a specified orientation, width, and height.

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
                    <div class="description">/{version}/legend/{layer}
                        <br>/legend/{layer}</div>
                </td>
            </tr>

            <tr class='item1'>
                <td class="attributes">Method</td>
                <td class="nameDescription">
                    <div class="description">`GET`</div>
                </td>
            </tr>
        </tbody>
    </table>
</div>

Example request:

```http
GET http://localhost:8080/instagram/rest/v1.0/legend/instagram-heatmap
```

Example response:

```http
data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAAAUAAAAFCAYAAACNbyblAAAAHElEQV
QI12P4//8/w38GIAXDIBKE0DHxgljNBAAO9TXL0Y4OHwAAAABJRU5ErkJggg==
```