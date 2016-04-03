function uid() {
    return 'xxxxxxxx-xxxx-4xxx-yxxx-xxxxxxxxxxxx'.replace(/[xy]/g, function(c) {
        var r = Math.random()*16|0, v = c == 'x' ? r : (r&0x3|0x8);
        return v.toString(16);
    });
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

    if (Cookies.get('uid') == null) {
        Cookies.set('uid', uid());
    }

});
