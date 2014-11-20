
var Map = require('./map/Map'),
    Axis = require('./map/Axis'),
    MapOverlay = require('./map/MapOverlay'),
    BaseLayer = require('./layer/base/BaseLayer'),
    ClientRenderedLayer = require('./layer/client/WordCloudRenderer'),
    RendererTheme = require('./layer/client/renderer/RendererTheme'),
    SummaryRenderer = require('./layer/client/renderer/SummaryRenderer'),
    TranslationTransformer = require('./layer/client/renderer/TranslationTransformer'),
    ServerRenderedLayer = require('./layer/server/ServerRenderedLayer'),
    LayerControl = require('./layer/control/LayerControl'),
    VisibilityCheckbox = require('./layer/control/VisibilityCheckbox'),
    OpacitySlider = require('./layer/control/OpacitySlider'),
    FilterSlider = require('./layer/control/FilterSlider'),
    SettingsButton = require('./layer/control/SettingsButton');

var map = new Map({
    pyramid : {
        type : "WebMercator"
    },
    options : {
        numZoomLevels : 17,
        projection : "EPSG:900913",
        displayProjection : "EPSG:4326",
        units : "m",
        maxExtent : [
            -20037508.342789244,
            -20037508.342789244,
            20037508.342789244,
            20037508.342789244
        ]
    }
});

map.add( new BaseLayer({
    type: Google,
    theme : dark,
    options : {
        name : Dark,
        type : styled,
        style : [
            { stylers : [ { invert_lightness : true },
                            { saturation : -100 },
                            { visibility : "simplified" } ] },
            { featureType : "landscape.natural.landcover",
              stylers : [ { visibility : "off" } ] },
            { featureType : "road",
              stylers : [ { visibility : "on" } ] },
            { featureType : "landscape.man_made",
              stylers : [ { visibility : "off" } ] },
            { featureType : "landscape",
              stylers : [ { lightness : -100 } ] },
            { featureType : "poi",
              stylers : [ { visibility : "off" } ] },
            { featureType : "administrative.country",
              elementType : "geometry",
              stylers : [ { visibility : "on" },
                            { lightness : -56 } ] },
            { elementType : "labels",
              stylers : [ { lightness : -46 },
                            { visibility : "on" } ] }
        ]
    }
}));

map.add( new BaseLayer({
    type: Google,
    theme : light,
    options : {
        name : Light,
        type : styled,
        style : [
            { stylers : [ { saturation : -100 },
                            { visibility : "simplified" } ] },
            { featureType : "landscape.natural.landcover",
              stylers : [ { visibility : off } ] },
            { featureType : "road",
              stylers : [ { visibility : on },
                            { gamma : 3.2} ] },
            { featureType : "landscape.man_made",
              stylers : [ { visibility : off } ] },
            { featureType : "landscape",
              stylers : [ { lightness : 100 } ] },
            { featureType : "poi",
              stylers : [ { visibility : off } ] },
            { featureType : "administrative.country",
              elementType : "geometry",
              stylers : [ { visibility : on },
                            { lightness : 35 } ] },
            { elementType : "labels",
              stylers : [ { lightness : 25 },
                            { visibility : on } ] }
        ]
    }
}));

map.add( new Axis({
    title : "Longitude",
    position : "bottom",
    repeat : true,
    isOpen : false,
    intervals : {
        type : "value",
        increment : 120,
        pivot : 0,
        allowScaleByZoom : true
    },
    units : {
        type : "degrees",
        decimals : 2,
        allowStepDown : false
    }
}));

map.add( new Axis({
    title : "Latitude",
    position : "left",
    repeat : false,
    isOpen : false,
    intervals : {
        type : "value",
        increment : 60,
        pivot : 0,
        allowScaleByZoom : true
    },
    units : {
        type : "degrees",
        decimals : 2,
        allowStepDown : false
    }
}));

map.zoomTo( 4, -15, -60 );

var wordCloudRenderer = new WordCloudRenderer({
    idKey: "topic",
    title: "Terms of Interest",
    text: {
        textKey: "topic",
        countKey : "countMonthly"
    },
}));
wordCloudRenderer.add( new RendererTheme({
    id: "dark-theme",
    color: "#FFFFFF",
    hoverColor: "#09CFFF",
    outline: "#000"
}));
wordCloudRenderer.add( new RendererTheme({
    id: "light-theme",
    color: "#000",
    hoverColor: "#09CFFF",
    outline: "#fff"
}));

var hoverSummaries = new SummaryRenderer({
    countKey: "countMonthly"
}));
hoverSummaries.add( new RendererTheme({
    id: "dark-theme"
    color: "#09CFFF",
    outline: "#000"
}));
hoverSummaries.add( new RendererTheme({
    id: "light-theme"
    color: "#09CFFF",
    outline: "#fff"
}));

var translationTransformer = new TranslationTransformer({
    languages: [{
            key: "topic",
            language: "spanish"
        },
        {
            key: "topicEnglish",
            language: "english"
        }]
}));

var instagramUsersLayer = new ClientRenderedLayer({
    views: [{
            source: "instragram-users",
            components: [
                wordCloudRenderer,
                hoverSummaries,
                translationTransformer
            ]
        }]
    ]
});

var instagramHeatMapLayer = new ServerRenderedLayer({
    source: "instragram-users",
    renderer: {
        ramp: "spectral"
    }
    valueTransform: {
        type: "log10"
    }
});

map.add( instagramUsersLayer );
map.add( instagramHeatMapLayer );

var description = new MapOverlay({
    id: "description",
    header: "Description",
    content: descriptionHtml,
    position: "top-right"
});

var layerControls = new MapOverlay({
    id: "layer-controls",
    header: "Controls",
    position: "bottom-right"
});

var instagramUsersControl = new LayerControl( instagramUsersLayer );
instagramUsersControl.add( new VisibilityCheckbox() );
instagramUsersControl.add( new OpacitySlider() );

var instagramHeatMapControl = new LayerControl( instagramHeatMapLayer );
instagramHeatMapControl.add( new VisibilityCheckbox() );
instagramHeatMapControl.add( new OpacitySlider() );
instagramHeatMapControl.add( new FilterSlider() );
instagramHeatMapControl.add( new SettingsButton()
    .add( new ColorRampSettings({
        spectral: "Spectral",
        hot: "Hot",
        cold: "Cold"
    }))
    .add( new ValueTransFormSettings({
        linear: "Linear",
        log10: "Logarithmic"
    }))
    .add( new CoursenessSettings({
        "1": "1x1 Pixels",
        "2": "2x2 Pixels",
        "3": "4x4 Pixels",
        "4": "8x8 Pixels"
    }))
);

layerControls.getContentElement()
    .append( instragramUsersControl );
    .append( instagramHeatMapControl );