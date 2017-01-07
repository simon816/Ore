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
 * Hides projects in Ore via AJAX
 *
 * ==================================================
 */

var ICON_VIS = 'fa-eye-slash';
var ICON_INVIS = 'fa-eye';

/*
 * ==================================================
 * =                   Doc ready                    =
 * ==================================================
 */

$(function() {
    $('.btn-hide').click(function () {
        var lang = $(this).find('span');
        var icon = $(this).find('i');
        var visible = icon.hasClass(ICON_VIS);
        var iconClass = visible ? ICON_VIS : ICON_INVIS;
        var project = $(this).data('project');
        var spinner = icon.removeClass(iconClass).addClass('fa-spinner fa-spin');
        $.ajax({
            type: 'post',
            url: '/' + project + '/visible/' + !visible,
            data: { csrfToken: csrf },
            fail: function () {
                spinner.addClass(iconClass).removeClass('fa-spinner fa-spin');
            },
            success: function () {
                spinner.addClass(visible ? ICON_INVIS : ICON_VIS).removeClass('fa-spinner fa-spin');
                lang.text(visible ? 'Unhide' : 'Hide');
            }
        });
    });
});
