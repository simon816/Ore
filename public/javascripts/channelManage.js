function rgbToHex(colorval) {
    var parts = colorval.match(/^rgb\((\d+),\s*(\d+),\s*(\d+)\)$/);
    delete(parts[0]);
    for (var i = 1; i <= 3; ++i) {
        parts[i] = parseInt(parts[i]).toString(16);
        if (parts[i].length == 1) parts[i] = '0' + parts[i];
    }
    return '#' + parts.join('');
}

function channelManage(toggle, channelName, channelHex, title, call, method, submit) {
    // Set attributes for current channel that is being managed
    $(toggle).click(function() {
        var preview = $("#preview");
        if (channelName.length > 0) {
            preview.show();
        } else {
            preview.hide();
        }
        $("#channel-input").val(channelName);
        $("#channel-color-input").val(channelHex);
        $("#color-picker").css("color", channelHex);
        preview.css("background-color", channelHex).text(channelName);
        $("#manage-label").text(title);
        $("#channel-manage-form").attr("action", call).attr("method", method);
        $("#channel-submit").val(submit);
    });
}

$(function() {

    // initialize popover to stay opened when hovered over
    $("#color-picker").popover({
        html: true,
        trigger: 'manual',
        container: $(this).attr('id'),
        placement: 'right',
        content: function() {
            return $("#popover-color-picker").html();
        }
    }).on("mouseenter", function () {
        var _this = this;
        $(this).popover("show");
        $(this).siblings(".popover").on("mouseleave", function () {
            $(_this).popover('hide');
        });
    }).on("mouseleave", function () {
        var _this = this;
        setTimeout(function () {
            if (!$(".popover:hover").length) {
                $(_this).popover("hide")
            }
        }, 100);
    });

    // update the preview within the popover when the name is updated
    $("#channel-input").on('input', function() {
        var val = $(this).val();
        if (val.length == 0) {
            $("#preview").hide();
        } else {
            $("#preview").show().text(val);
        }
    });

    // update colors when new color is selected
    $(document).on("click", ".channel-id", function() {
        var color = $(this).css("color");
        $("#channel-color-input").val(rgbToHex(color));
        $("#color-picker").css("color", color);
        $("#preview").css("background-color", color);
    });

});
