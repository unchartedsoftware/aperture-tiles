/**
 * Created with JetBrains WebStorm.
 * User: dgray
 * Date: 30/07/13
 * Time: 1:57 PM
 * To change this template use File | Settings | File Templates.
 */

/**
 * PlotLink allows a for a box on one plot to track the bounds
 * of another. It takes two plots (e.g. density strips) and "links"
 * them together so that one is treated as a overview or thumbnail
 * map of the other.
 *
  * @param options
 *          options.tracker : an XDataMap that displays position of the trackee.
 *          options.trackee : an XDataMap that is tracked (the one users are zooming and panning.)
 *
 * @constructor
 */
var PlotLink = function (options){

    var onRelocate = function(source, event){
        var trackerMap = options.tracker;
        var trackeeMap = options.trackee;

        var trackerDivId = trackerMap.getParentDivId();
        var trackerDiv = $("#"+trackerDivId);
        var trackerPixelWidth = trackerDiv.width();
        var trackerPixelHeight = trackerDiv.height();

        // get the dataBounds of the tracker and trackee
        var trackeeBounds = trackeeMap.getViewBounds();
        var trackerBounds = trackerMap.getViewBounds();

        if(trackeeBounds === null || trackerBounds === null)
            return;

        var loupeDivId = trackerDivId + '-loupe'
        var loupe = $("#"+loupeDivId);
        if(loupe.length === 0){    // Not yet created - init

            loupe = $('<div></div>');
            loupe.addClass('plotlink-loupe');
            loupe.attr('id', loupeDivId);
            trackerDiv.append(loupe);
        }
        // May not need it to be shown at all if zoomed all the way out.
        if(trackeeBounds.left <= trackerBounds.left && trackeeBounds.right >= trackerBounds.right){
            loupe.css("border-style", "none");
        } else {
            loupe.css("border-style", "solid");
        }

        // calculate ratio of tracker to trackee.. apply % to loupe size.
        var trackeeWidth = trackeeBounds.right - trackeeBounds.left;

        var trackeeOffsetFromStart = trackerBounds.left - trackeeBounds.left;
        if(trackeeBounds.left < trackerBounds.left){ // exceeding range of actual data.
            trackeeOffsetFromStart = 0;
        }
        var trackeeEndOffsetFromStart = trackerBounds.left - trackeeBounds.right;
        var trackerWidth = trackerBounds.right - trackerBounds.left;
        // for now I'll assume the trackER is the full data width (i.e. viewbounds == totalbounds).
        // If that isn't the case then will have to grab full data widths.
        var startPos = Math.abs((trackeeOffsetFromStart/trackerWidth)*trackerPixelWidth);
        startPos = Math.max(startPos, 0);
        startPos = Math.min(startPos, 255);            //TODO: un-hard-code #
        //console.log("start: " + startPos);
        var endPos = Math.abs((trackeeEndOffsetFromStart/trackerWidth)*trackerPixelWidth);
        //console.log("end  :" + endPos);
        endPos = Math.min(endPos, 255);      //TODO: un-hard-code #
        var cssWidth = Math.max(endPos-startPos, 0);

        //console.log("width: " + cssWidth);
        loupe.css({
            "top"           : "-3px",
            "left"          : (startPos + 28) + "px",             //TODO: remove need for 28 by fixing layout of mini strip wrt. expansion button.
            "height"        : (trackerPixelHeight + 6) + "px",
            "width"         : cssWidth+"px"
        });
    };

    this.start = function(trackeeApertureMap){

        var trackeeMap = trackeeApertureMap;//options.trackee.getApertureMap();

        trackeeMap.on('zoom', onRelocate);
        trackeeMap.on('move', onRelocate);
        trackeeMap.on('panend', onRelocate);
        //onRelocate(null, null);
    };
}
