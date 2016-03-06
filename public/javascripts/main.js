$(document).ready(function() {
    var increment = 1;
    $(".btn-star").click(function() {
        // TODO: Ajax
        var starred = $(this).find(".starred");
        starred.text(parseInt(starred.text()) + increment);
        increment *= -1;
    });
});
