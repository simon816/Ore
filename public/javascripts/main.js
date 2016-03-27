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
        $("#color-picker").css("color", $(this).css("color"));
    });

});
