var KEY_S = 83;
var KEY_H = 72;
var KEY_C = 67;
var KEY_ESC = 27;

function uid() {
    return 'xxxxxxxx-xxxx-4xxx-yxxx-xxxxxxxxxxxx'.replace(/[xy]/g, function(c) {
        var r = Math.random()*16|0, v = c == 'x' ? r : (r&0x3|0x8);
        return v.toString(16);
    });
}

$(function() {
    $('[data-toggle="tooltip"]').tooltip();

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

    if (Cookies.get('uid') == null) {
        // TODO: move this server-side
        Cookies.set('uid', uid());
    }
});
