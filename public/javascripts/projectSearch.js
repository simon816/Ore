var KEY_ENTER = 13;

$(function() {
    var searchBar = $('.search-bar');
    searchBar.find('input').on('keypress', function(event) {
        if (event.keyCode === KEY_ENTER) {
            event.preventDefault();
            $(this).next().find('.btn').click();
        }
    });

    searchBar.find('.btn').click(function(event) {
        var query = $(this).closest('.input-group').find('input').val();
        window.location = '/search?q=' + query;
    });
});
