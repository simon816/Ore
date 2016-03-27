function rgbToHex(colorval) {
    var parts = colorval.match(/^rgb\((\d+),\s*(\d+),\s*(\d+)\)$/);
    delete(parts[0]);
    for (var i = 1; i <= 3; ++i) {
        parts[i] = parseInt(parts[i]).toString(16);
        if (parts[i].length == 1) parts[i] = '0' + parts[i];
    }
    return '#' + parts.join('');
}

$(function() {
    var increment = 1;
    $(".btn-star").click(function() {
        // TODO: Increment / Decrement 'starred' accordingly via Ajax
        var starred = $(this).find(".starred");
        starred.text(parseInt(starred.text()) + increment);
        increment *= -1;
    });

    $('[data-toggle="tooltip"]').tooltip();

    $('[data-toggle="popover"]').popover({
        html: true,
        trigger: 'manual',
        container: $(this).attr('id'),
        placement: 'right',
        content: function() {
            return $("#popover-table").html();
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

    $(document).on("click", ".channel-id", function() {
        var color = $(this).css("color");
        $("#channel-color-input").val(rgbToHex(color));
        $("#color-picker").css("color", color);
        $("#preview").css("background-color", color);
    });

    $('#channel-input').on('input', function() {
        var val = $(this).val();
        if (val.length == 0) {
            $('#preview').hide()
        } else {
            $("#preview").show().text(val);
        }
    });

});
