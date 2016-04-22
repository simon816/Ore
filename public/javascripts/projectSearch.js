var KEY_ENTER = 13;
var CATEGORY_STRING = null;

$(function() {

    $('.icon-project-search').click(function() {
        var searchBar = $('.project-search').find('.input-group');
        if (searchBar.is(':visible')) {
            searchBar.animate({width: '0px'}, 100).fadeOut(100);
        } else {
            searchBar.fadeIn(100).animate({width: '790px'}, 100);
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
