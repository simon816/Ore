var NAME_REGEX = null;
var BASE_URL = null;
var PLUGIN_ID = null;

var originalColor = null;
var originalName = null;

var colorFailed = false;
var nameFailed = false;

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

function removeSpinner() {
    return $('.status').removeClass('fa-spinner fa-spin');
}

function spinner() {
    return $('.status').removeClass('fa-check-circle fa-times-circle').addClass('fa-spinner fa-spin');
}

function success() {
    $('#channel-submit').prop('disabled', false);
    removeSpinner().addClass('fa-check-circle').tooltip('destroy');
}

function failure(message) {
    $('#channel-submit').prop('disabled', true);
    removeSpinner().addClass('fa-times-circle').tooltip({
        placement: 'right',
        title: message
    });
}

function getNameInput() {
    return $('#channel-input').val();
}

function getColorInput() {
    return $('#channel-color-input').val().toLowerCase();
}

function getChannels(onSuccess) {
    $.ajax({
        url: BASE_URL + '/api/project?pluginId=' + PLUGIN_ID,
        dataType: "json",
        success: function(data) {
            onSuccess(data.channels);
        }
    });
}

function checkName() {
    // Check for illegal characters
    spinner().show();
    var input = getNameInput();
    if (!input.match(NAME_REGEX)) {
        nameFailed = true;
        updateStatus();
        return;
    }

    // Check for existing channel
    getChannels(function(channels) {
        var existingChannel = channels.find(function(channel) {
            var name = channel.name.toLowerCase();
            return name === input.toLowerCase() && name !== originalName.toLowerCase();
        });
        nameFailed = existingChannel != null;
        updateStatus();
    });
}

function checkColor() {
    spinner().show();
    getChannels(function(channels) {
        var existingChannel = channels.find(function(channel) {
            var color = channel.color.toLowerCase();
            return color === getColorInput() && color !== originalColor;
        });
        colorFailed = existingChannel != null;
        updateStatus();
    });
}

function updateStatus() {
    if (nameFailed) {
        failure('This name is already taken.');
    } else if (colorFailed) {
        failure('This color is already taken.');
    } else if (getNameInput() === originalName && getColorInput() == originalColor) {
        spinner().hide();
    } else {
        success();
    }
}

function initChannelManager(toggle, channelName, channelHex, title, call, method, submit) {
    // Set attributes for current channel that is being managed
    $(toggle).click(function() {
        var preview = $('#preview');
        var submitInput = $('#channel-submit');
        var channelInput = $("#channel-input");
        var colorInput = $("#channel-color-input");

        originalColor = channelHex.toLowerCase();
        originalName = channelName;

        // Update modal attributes
        $('#color-picker').css('color', channelHex);
        $('#manage-label').text(title);
        colorInput.val(channelHex);
        preview.css('background-color', channelHex).text(channelName);
        submitInput.val(submit);
        spinner().hide();

        // Only show preview when there is input
        if (channelName.length > 0) {
            preview.show();
        } else {
            preview.hide();
        }

        // Validate channel name on input change
        channelInput.val(channelName);
        channelInput.on('input', function() {
            var val = $(this).val();
            if (val !== channelName) {
                checkName();
            } else {
                updateStatus();
            }
        });

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
        var hex = rgbToHex(color);
        $('#channel-color-input').val(hex);
        $('#color-picker').css('color', color);
        $('#preview').css('background-color', color);

        if (hex.toLowerCase() !== originalColor) {
            // Validate new color
            checkColor();
        } else {
            updateStatus();
        }
    });
});
