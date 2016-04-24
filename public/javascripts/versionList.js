var PLUGIN_ID = null;
var CHANNEL_STRING = null;
var VERSIONS_PER_PAGE = 10;
var PROJECT_OWNER = null;
var PROJECT_SLUG = null;

var page = 1;

function loadVersions(increment) {
    var versionPanel = $('.version-panel');

    // Calculate offset
    var currOffset = (page - 1) * VERSIONS_PER_PAGE;
    var step = VERSIONS_PER_PAGE * (increment / Math.abs(increment));
    var offset = Math.max(0, currOffset + step);

    // Build url
    var url = '/api/projects/' + PLUGIN_ID + '/versions?offset=' + offset;
    if (CHANNEL_STRING) url += '&channels=' + CHANNEL_STRING;

    // Request more versions
    var spinner = versionPanel.find('.fa-spinner').show();
    $.ajax({
        url: url,
        dataType: 'json',
        complete: function() { spinner.hide() },
        success: function(versions) {
            var content = '';
            var count = 0;
            for (var i in versions) {
                if (!versions.hasOwnProperty(i)) continue;
                var version = versions[i];
                var channel = version.channel;
                var slug = channel.name + '/' + version.name;

                // Build result row
                var row = $('#row-version').clone().removeAttr('id');
                row.find('.channel-id').css('color', channel.color);
                row.find('.version-str').text(version.name);
                row.find('.created').text(version.createdAt);
                row.find('.size').text(filesize(version.fileSize));
                row.find('.info').attr('href', window.location + '/versions/' + slug);
                row.find('.dl').attr('href', window.location + '/versions/download/' + slug);

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

$(function() {
    var versionPanel = $('.version-panel');
    versionPanel.find('.next').click(function() { loadVersions(1) });
    versionPanel.find('.prev').click(function() { loadVersions(-1) });

    // Setup channel list
    $('.list-channel').find('li').find('a').click(function() {
        var channelString = '';
        var channelName = $(this).closest('.list-group-item').find('.channel').text();
        if ($(this).hasClass('visible')) {
            var self = $(this);
            var visible = $('.list-channel').find('li').find('a.visible');
            // Find visible channels
            visible.each(function(i) {
                if ($(this).is(self)) return; // Skip this channel
                channelString += $(this).closest('.list-group-item').find('.channel').text();
                if (i < visible.length - 1) channelString += ',';
            });
        } else if (CHANNEL_STRING) {
            channelString += CHANNEL_STRING + ',' + channelName;
        } else {
            channelString += channelName;
        }

        // Build url
        var url = '/' + PROJECT_OWNER + '/' + PROJECT_SLUG + '/versions';
        if (channelString.length > 0) url += '?channels=' + channelString;

        // Booyah!
        window.location = url;
    });
});
