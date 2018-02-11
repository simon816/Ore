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

var ICON = 'fa-eye';

/*
 * ==================================================
 * =                   Doc ready                    =
 * ==================================================
 */

$(function() {
    $('.btn-hide').click(function () {
        var project = $(this).data('project');
        var visibilityLevel = $(this).data('level');
        var spinner = $('button[data-project="'  + project + '"]').find('i');
        spinner.removeClass(ICON).addClass('fa-spinner fa-spin');
        $.ajax({
            type: 'post',
            url: '/' + project + '/visible/' + visibilityLevel,
            data: { csrfToken: csrf },
            fail: function () {
                spinner.addClass(ICON).removeClass('fa-spinner fa-spin');
            },
            success: function () {
                spinner.addClass(ICON).removeClass('fa-spinner fa-spin');
                location.reload();
            }
        });
    });
});
