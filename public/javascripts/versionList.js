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
 * Handles async loading and display of the version list.
 *
 * ==================================================
 */

/*
 * ==================================================
 * =               External constants               =
 * ==================================================
 */

var PLUGIN_ID = null;
var CHANNEL_STRING = '';
var VERSIONS_PER_PAGE = 10;
var PROJECT_OWNER = null;
var PROJECT_SLUG = null;

var page = 1;

/*
 * ==================================================
 * =                Helper functions                =
 * ==================================================
 */

function loadVersions(increment) {
    var versionPanel = $('.version-panel');

    var offset = (page + increment - 1) * VERSIONS_PER_PAGE;
    var url = '/api/projects/' + PLUGIN_ID + '/versions?offset=' + offset;
    if (CHANNEL_STRING) url += '&channels=' + CHANNEL_STRING;

    var spinner = versionPanel.find('.fa-spinner').show();
    $.ajax({
        url: url,
        dataType: 'json',
        complete: function() { spinner.hide(); },
        success: function(versions) {
            var content = '';
            var count = 0;
            for (var i in versions) {
                if (!versions.hasOwnProperty(i)) continue;
                var version = versions[i];
                var channel = version.channel;
                var slug = 'versions/' + version.name;

                // Build result row
                var row = $('#row-version').clone().removeAttr('id');
                row.find('.channel-id').css('color', channel.color);
                row.find('.version-str').html('<strong>' + version.name + '</strong>').attr('href', slug);
                row.find('.created').text(version.createdAt);
                row.find('.size').text(filesize(version.fileSize));
                row.find('.info').attr('href', window.location + '/versions/' + decodeHtml(slug));
                row.find('.dl').attr('href', window.location + '/versions/download/' + decodeHtml(slug));

                // Append to content string
                content += $('<div>').append(row).html();
                count++;
            }

            // Fill table
            if (count > 0) {
                versionPanel.find('tbody').html(content);
                page += increment;

                // Check visibility of nav
                var next = versionPanel.find('.next');
                var prev = versionPanel.find('.prev');
                if (count < VERSIONS_PER_PAGE) next.hide(); else next.show();
                if (page === 1) prev.hide(); else prev.show();

                versionPanel.find('.offset').text((page - 1) * VERSIONS_PER_PAGE + count);
            }
        }
    });
}

/*
 * ==================================================
 * =                   Doc ready                    =
 * ==================================================
 */

$(function() {
    var versionPanel = $('.version-panel');
    versionPanel.find('.next').click(function() { loadVersions(1) });
    versionPanel.find('.prev').click(function() { loadVersions(-1) });

    // Setup channel list

    $('.list-channel').find('li').find('input').on('change', function() {
        var channelString = CHANNEL_STRING;
        var channelName = $(this).closest('.list-group-item').find('.channel').text();
        if ($(this).is(":checked")) {
            if (channelString.length) channelString += ',';
            channelString += channelName;
        } else {
            channelString = '';
            var checked = $('.list-channel').find('li').find('input:checked');
            checked.each(function(i) {
                channelString += $(this).closest('.list-group-item').find('.channel').text();
                if (i < checked.length - 1) channelString += ',';
            });
        }

        var url = '/' + PROJECT_OWNER + '/' + PROJECT_SLUG + '/versions';
        if (channelString.length) url += '?channels=' + channelString;

        go(url);
    });

    $('.channels-all').on('change', function() {
        var channelString = '';
        if (!$(this).is(":checked")) {
            var checked = $('.list-channel').find('li').find('input');
            checked.each(function(i) {
                channelString += $(this).closest('.list-group-item').find('.channel').text();
                if (i < checked.length - 1) channelString += ',';
            });
        }

        var url = '/' + PROJECT_OWNER + '/' + PROJECT_SLUG + '/versions';
        if (channelString.length) url += '?channels=' + channelString;

        go(url);
    });

    versionPanel.find('tr').click(function(e) {
        if (e.target == this)
            window.location.href = $(this).find('td:first-child').find('a').prop('href');
    });
});
