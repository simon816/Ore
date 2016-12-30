/*
 * ==================================================
 *  _____             _
 * |     |___ ___    |_|___
 * |  |  |  _| -_|_  | |_ -|
 * |_____|_| |___|_|_| |___|
 *                 |___|
 *
 * By Walker Crouse (windy) and contributors
 * (C) SpongePowered 2016-2017 MIT License
 * https://github.com/SpongePowered/Ore
 *
 * Handles new page creation.
 *
 * ==================================================
 */

/*
 * ==================================================
 * =               External constants               =
 * ==================================================
 */

var KEY_ENTER = 13;
var PROJECT_OWNER = null;
var PROJECT_SLUG = null;

/*
 * ==================================================
 * =                   Doc ready                    =
 * ==================================================
 */

$(function() {
    var modal = $('#new-page');
    modal.on('shown.bs.modal', function() { $(this).find('input').focus(); });

    modal.find('input').keydown(function(event) {
        if (event.keyCode === KEY_ENTER) {
            event.preventDefault();
            $('#continue-page').click();
        }
    });

    $('#continue-page').click(function() {
        var url = '/' + PROJECT_OWNER + '/' + PROJECT_SLUG + '/pages/' + $('#page-name').val() + '/edit';
        go(url);
    })
});
