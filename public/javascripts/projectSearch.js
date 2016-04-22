var KEY_ENTER = 13;
var CATEGORY_STRING = null;

$(function() {

    $('.icon-project-search').click(function() {
        var searchBar = $('.project-search');
        var input = searchBar.find('.input-group');
        if (input.is(':visible')) {
            searchBar.animate({width: '0px'}, 100);
            input.fadeOut(100);
        } else {
            input.fadeIn(100);
            searchBar.animate({width: '790px'}, 100);
            input.find('input').focus();
        }
    });

    var searchBar = $('.project-search');
    searchBar.find('input').on('keypress', function(event) {
        if (event.keyCode === KEY_ENTER) {
            event.preventDefault();
            $(this).next().find('.btn').click();
        }
    });

    searchBar.find('.btn').click(function() {
        var query = $(this).closest('.input-group').find('input').val();
        var url = '/?q=' + query;
        if (CATEGORY_STRING) url += '&categories=' + CATEGORY_STRING;
        window.location = url;
    });
});
