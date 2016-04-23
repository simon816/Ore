var KEY_S = 83;
var KEY_H = 72;
var KEY_C = 67;
var KEY_ESC = 27;
var KEY_ENTER = 13;
var CATEGORY_STRING = null;
var SORT_STRING = null;

function uid() {
    // TODO: Move server-side
    return 'xxxxxxxx-xxxx-4xxx-yxxx-xxxxxxxxxxxx'.replace(/[xy]/g, function(c) {
        var r = Math.random()*16|0, v = c == 'x' ? r : (r&0x3|0x8);
        return v.toString(16);
    });
}

$(function() {
    if (Cookies.get('uid') == null) {
        // TODO: move this server-side
        Cookies.set('uid', uid());
    }

    $('.alert-fade').fadeIn('slow');
    $('[data-toggle="tooltip"]').tooltip();

    $('.icon-project-search').click(function() {
        var searchBar = $('.project-search');
        var input = searchBar.find('.input-group');
        if (input.is(':visible')) {
            searchBar.animate({width: '0px'}, 100);
            input.fadeOut(100);
        } else {
            var width = $('.user-avatar').length ? '870px' : '790px';
            input.fadeIn(100);
            searchBar.animate({width: width}, 100);
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
        if (SORT_STRING) url += '&sort=' + SORT_STRING;
        window.location = url;
    });

    /*
     * Ore hotkeys:
     *
     * s    Open search
     * esc  Close search
     * h    Go home
     * c    Create new project
     */
    $('body').keydown(function(event) {
        var target = $(event.target);
        var searchIcon = $('.icon-project-search');
        if (target.is('body')) {
            switch (event.keyCode) {
                case KEY_S:
                    event.preventDefault();
                    searchIcon.click();
                    break;
                case KEY_H:
                    event.preventDefault();
                    window.location = '/';
                    break;
                case KEY_C:
                    event.preventDefault();
                    window.location = '/new';
                    break;
                default:
                    break;
            }
        } else if (target.is('.project-search input')) {
            switch (event.keyCode) {
                case KEY_ESC:
                    event.preventDefault();
                    searchIcon.click();
                    break;
                default:
                    break;
            }
        }
    });
});
