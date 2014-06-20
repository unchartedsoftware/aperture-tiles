/*
 * Copyright (c) 2014 Oculus Info Inc.
 * http://www.oculusinfo.com/
 *
 * Released under the MIT License.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of
 * this software and associated documentation files (the "Software"), to deal in
 * the Software without restriction, including without limitation the rights to
 * use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies
 * of the Software, and to permit persons to whom the Software is furnished to do
 * so, subject to the following conditions:

 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.

 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */


define(function (require) {
    "use strict";



    var ANNOTATION_CLASS = "annotation",
        ANNOTATION_ATTRIBUTE_CLASS = ANNOTATION_CLASS + " annotation-attribute",
        ANNOTATION_INPUT_CLASS = ANNOTATION_CLASS + " annotation-input",
        ANNOTATION_LABEL_CLASS = ANNOTATION_CLASS + " annotation-label",
        ANNOTATION_TEXTAREA_CLASS = ANNOTATION_CLASS + " annotation-textarea",
        ANNOTATION_TEXT_DIV_CLASS = ANNOTATION_CLASS + " annotation-text-div",
        ANNOTATION_CONTENT_CLASS = ANNOTATION_CLASS + " annotation-content-container",
        ANNOTATION_BUTTON_CLASS = ANNOTATION_CLASS + " annotation-button",
        ANNOTATION_LEFT_BUTTON_CLASS = ANNOTATION_BUTTON_CLASS + " annotation-button-left",
        ANNOTATION_RIGHT_BUTTON_CLASS = ANNOTATION_BUTTON_CLASS + " annotation-button-right",
        ANNOTATION_POPUP_ID = "annotation-popup-id",
        ANNOTATION_CANCEL_BUTTON_ID = "annotation-popup-cancel",
        ANNOTATION_SAVE_BUTTON_ID = "annotation-popup-save",
        ANNOTATION_EDIT_BUTTON_ID = "annotation-popup-edit",
        ANNOTATION_REMOVE_BUTTON_ID = "annotation-popup-remove",
        ANNOTATION_POPUP_TITLE_ID = "annotation-popup-title-id",
        ANNOTATION_POPUP_GROUP_ID = "annotation-popup-group-id",
        ANNOTATION_POPUP_DESCRIPTION_ID = "annotation-popup-description-id",
        ANNOTATION_ACCORDION_ID = "annotation-accordion-id";

    return {

        createEditablePopup: function( groups, closeFunc, saveFunc, removeFunc ) {

            // edit popup is only possible on single features, so take only data entry
            var that = this,
                hasRemoveFunc = ( removeFunc !== undefined ),
                html = this.getEditablePopupHTML( this.getDataArray()[0], groups, hasRemoveFunc );

            // create popup with close callback
            this.createPopup( html, function() {
                closeFunc( that );
                return false;   // stop event propagation
            });

            // set save callback
            $( "#"+ANNOTATION_SAVE_BUTTON_ID ).click( function() {
                saveFunc( that );
                return false;   // stop event propagation
            });

            // set cancel callback
            $( "#"+ANNOTATION_CANCEL_BUTTON_ID ).click( function() {
                closeFunc( that );
                return false;   // stop event propagation
            });

            if ( hasRemoveFunc ) {
                // set remove callback
                $( "#"+ANNOTATION_REMOVE_BUTTON_ID ).click( function() {
                    removeFunc( that );
                    return false;   // stop event propagation
                });
            }
        },


        createDisplayPopup: function( bin, closeFunc, editFunc ) {

            var that = this,
                hasEditFunc = editFunc !== null,
                html;

            if ( bin.length > 1 ) {
                // aggregate
                html = this.getAggregateDisplayPopupHTML();

            } else {
                // single
                html = this.getSingleDisplayPopupHTML( this.getDataArray()[0], hasEditFunc );

            }

            // create popup with close callback
            this.createPopup( html, function() {
                //closeFunc( that );
                return false;   // stop event propagation
            });


            if ( bin.length > 1 ) {

                // set accordion
                $( "#"+ANNOTATION_ACCORDION_ID ).accordion({
                    active: false,
                    collapsible: true,
                    heightStyle: "content"
                });

            } else {

                if ( hasEditFunc ) {
                    // set save callback
                    $( "#"+ANNOTATION_EDIT_BUTTON_ID ).click( function() {
                        editFunc( that );
                        return false;   // stop event propagation
                    });
                }

            }

            // set cancel callback
            $( "#"+ANNOTATION_CANCEL_BUTTON_ID ).click( function() {
                closeFunc( that );
                return false;   // stop event propagation
            });

        },


        checkInputData: function() {

            var $title = $('#'+ANNOTATION_POPUP_TITLE_ID),
                $group = $('#'+ANNOTATION_POPUP_GROUP_ID),
                $description = $('#'+ANNOTATION_POPUP_DESCRIPTION_ID),
                INVALID_COLOR = {color: '#7e0004'},
                INVALID_COLOR_EFFECT_LENGTH_MS = 3000;

            // if entry is invalid, flash
            if ( $title.val() === "" ) {
                $title.effect("highlight", INVALID_COLOR, INVALID_COLOR_EFFECT_LENGTH_MS);
            }
            if ( $group.val() === "" ) {
                $group.effect("highlight", INVALID_COLOR, INVALID_COLOR_EFFECT_LENGTH_MS);
            }
            if ( $description.val() === "" ) {
                $description.effect("highlight", INVALID_COLOR, INVALID_COLOR_EFFECT_LENGTH_MS);
            }

            // check input values
            return ( $title.val() !== "" &&
                     $group.val() !== "" &&
                     $description.val() !== "" );
        },


        setDataFromPopup: function( annotation ) {

            var $title = $('#'+ANNOTATION_POPUP_TITLE_ID),
                $group = $('#'+ANNOTATION_POPUP_GROUP_ID),
                $description = $('#'+ANNOTATION_POPUP_DESCRIPTION_ID);

            annotation.group = $group.val();
            annotation.data.title = $title.val();
            annotation.data.comment = $description.val();
        },


        checkAndSetData: function() {

            var annotation = this.getDataArray()[0];

            if ( this.checkInputData() ) {
                this.setDataFromPopup( annotation );
                return true;
            }
            return false;
        },


        getSingleDisplayPopupHTML: function( annotation, isEditable ) {

            var html = "<div style='overflow:hidden'>";

            if ( isEditable ) {
                html += "<input id='"+ ANNOTATION_EDIT_BUTTON_ID + "' type='image' src='./images/edit-icon.png' width='17' height='17'>";
            }

            html += "<div id='" +ANNOTATION_POPUP_ID+ "' class='ui-widget-content'>"+
                        "<div class='"+ANNOTATION_CONTENT_CLASS+"'>"+
                            "<div class='"+ANNOTATION_ATTRIBUTE_CLASS+"'>" +
                                annotation.data.title +
                            "</div>"+
                            "<div class='"+ANNOTATION_ATTRIBUTE_CLASS+"'>" +
                                "Priority: "+ annotation.group +
                            "</div>"+
                            "<div class='"+ANNOTATION_TEXT_DIV_CLASS+"'>"+
                                annotation.data.comment +
                            "</div>"+
                        "</div>"+
                    "</div>"+
                "</div>";

            return html;
        },


        getAggregateDisplayPopupHTML: function( bin ) {

            var annotations = this.getDataArray(),
                i,
                html = "<div style='overflow:hidden'>"+
                            "<div id='" +ANNOTATION_POPUP_ID+ "' class='ui-widget-content'>"+
                                "<div class='"+ANNOTATION_CONTENT_CLASS+"'>"+
                                    "<div id='"+ANNOTATION_ACCORDION_ID+"'>";

            for (i=0; i<bin.length; i++) {
                html += "<h3>" + bin.title + "</h3>"+
                            "<div>"+
                                "<div class='"+ANNOTATION_ATTRIBUTE_CLASS+"'>" +
                                    "Priority: "+ annotations[i].group +
                                "</div>"+
                                "<div class='"+ANNOTATION_TEXT_DIV_CLASS+"'>"+
                                    annotations[i].data.comment +
                                "</div>"+
                            "</div>";
            }

            return html + "</div></div></div></div>";
        },


        getEditablePopupHTML: function( annotation, groups, includeRemoveButton ) {

            var TITLE_PLACEHOLDER = ' Enter title',
                PRIORITY_PLACEHOLDER = 'Enter group',
                DESCRIPTION_PLACEHOLDER = ' Enter description',
                titleVal= annotation.data.title || "",
                descriptionVal = annotation.data.comment || "",
                removeButtonHTML = includeRemoveButton ? "<button class='"+ANNOTATION_LEFT_BUTTON_CLASS+"' id='" + ANNOTATION_REMOVE_BUTTON_ID + "'>Remove</button>" : "";

                function getSelectHTML() {

                    var html = "<select class='"+ANNOTATION_INPUT_CLASS+"' id='"+ ANNOTATION_POPUP_GROUP_ID+"'>",
                        i;

                    for (i=0; i<groups.length; i++) {
                        if ( groups[i] === annotation.group ) {
                            html += "<option selected='true' value='" +groups[i] +"'>"+groups[i]+"</option>";
                        } else {
                            html += "<option value='" +groups[i] +"'>"+groups[i]+"</option>";
                        }
                    }

                    if ( annotation.group === undefined ) {
                        html += "<option selected='true' style='display:none;' value=''>"+PRIORITY_PLACEHOLDER+"</option>";
                    }

                    return html + "</select>";
                }

                return  "<div style='overflow:hidden'>"+
                            "<div id='" +ANNOTATION_POPUP_ID+ "' class='ui-widget-content'>"+
                            "<div class='"+ANNOTATION_CONTENT_CLASS+"'>"+
                                "<div class='"+ANNOTATION_ATTRIBUTE_CLASS+"'>" +

                                    "<label class='"+ANNOTATION_LABEL_CLASS+"'>Title: </label>" +
                                    "<input class='"+ANNOTATION_INPUT_CLASS+"' id='"+ ANNOTATION_POPUP_TITLE_ID +"' type='text' placeholder='"+TITLE_PLACEHOLDER+"' value='"+ titleVal +"'>" +

                                "</div>"+
                                "<div class='"+ANNOTATION_ATTRIBUTE_CLASS+"'>" +

                                    "<label class='"+ANNOTATION_LABEL_CLASS+"'>Priority: </label>" +
                                    getSelectHTML()+

                                "</div>"+
                                "<div class='"+ANNOTATION_ATTRIBUTE_CLASS+"'> Description: </div>" +
                                "<textarea class='"+ANNOTATION_TEXTAREA_CLASS+"' id='"+ ANNOTATION_POPUP_DESCRIPTION_ID +"' placeholder='"+DESCRIPTION_PLACEHOLDER+"'>"+ descriptionVal +"</textarea> "+
                            "</div>"+

                        "</div>"+
                        removeButtonHTML +
                        "<button class='"+ANNOTATION_RIGHT_BUTTON_CLASS+"' id='" + ANNOTATION_CANCEL_BUTTON_ID + "'>Cancel</button>"+
                        "<button class='"+ANNOTATION_RIGHT_BUTTON_CLASS+"' id='" + ANNOTATION_SAVE_BUTTON_ID + "'>Save</button>"+
                    "</div>";
        }

    };


});
