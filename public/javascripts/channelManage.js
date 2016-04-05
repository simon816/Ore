function rgbToHex(rgb) {
    var parts = rgb.match(/^rgb\((\d+),\s*(\d+),\s*(\d+)\)$/);
    delete(parts[0]);
    for (var i = 1; i <= 3; ++i) {
        parts[i] = parseInt(parts[i]).toString(16);
        if (parts[i].length == 1) {
            parts[i] = '0' + parts[i];
        }
    }
    return '#' + parts.join('');
}

var onCustomSubmit = function(toggle, channelName, channelHex, title, submit) {
    $('#channel-name').text(channelName).css('background-color', channelHex);
    $('#chan-input').val(channelName);
    $('#chan-color-input').val(channelHex);
    $('#channel-manage').modal('hide');
    initChannelManager(toggle, channelName, channelHex, title, null, null, submit);
};

function initChannelManager(toggle, channelName, channelHex, title, call, method, submit) {
    // Set attributes for current channel that is being managed
    $(toggle).click(function() {
        var preview = $('#preview');
        var submitInput = $('#channel-submit');

        // Update modal attributes
        $('#color-picker').css('color', channelHex);
        $('#manage-label').text(title);
        $("#channel-color-input").val(channelHex);
        $("#channel-input").val(channelName);
        preview.css('background-color', channelHex).text(channelName);

        // Only show preview when there is input
        if (channelName.length > 0) {
            preview.show();
        } else {
            preview.hide();
        }

        submitInput.val(submit);
        if (call == null && method == null) {
            // Redirect form submit to client
            submitInput.click(function(event) {
                event.preventDefault();
                submitInput.submit();
            });
            submitInput.submit(function(event) {
                event.preventDefault();
                onCustomSubmit(toggle, $('#channel-input').val(), $('#channel-color-input').val(), title, submit);
            });
        } else {
            // Set form action
            $('#channel-manage-form').attr('action', call).attr('method', method);
        }
    });
}

$(function() {
    // Initialize popover to stay opened when hovered over
    $("#color-picker").popover({
        html: true,
        trigger: 'manual',
        container: $(this).attr('id'),
        placement: 'right',
        content: function() {
            return $("#popover-color-picker").html();
        }
    }).on('mouseenter', function () {
        var _this = this;
        $(this).popover('show');
        $(this).siblings(".popover").on('mouseleave', function () {
            $(_this).popover('hide');
        });
    }).on('mouseleave', function () {
        var _this = this;
        setTimeout(function () {
            if (!$('.popover:hover').length) {
                $(_this).popover('hide')
            }
        }, 100);
    });

    // Update the preview within the popover when the name is updated
    $('#channel-input').on('input', function() {
        var val = $(this).val();
        if (val.length == 0) {
            $('#preview').hide();
        } else {
            $('#preview').show().text(val);
        }
    });

    // Update colors when new color is selected
    $(document).on('click', '.channel-id', function() {
        var color = $(this).css("color");
        $('#channel-color-input').val(rgbToHex(color));
        $('#color-picker').css('color', color);
        $('#preview').css('background-color', color);
    });
});
