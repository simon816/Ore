var projectOwner = null;
var projectSlug = null;
var alreadyStarred = false;

var KEY_PLUS = 61;
var KEY_MINUS = 173;

function getActiveTab() {
    return $('.project-navbar').find('li.active');
}

function switchTabTo(tab, def) {
    var id = tab.attr('id');
    if (tab.is('li') && id !== 'issues' && id !== 'source') {
        window.location = tab.find('a').attr('href');
    } else {
        window.location = def.find('a').attr('href');
    }
}

$(function() {
    // setup star button
    var increment = alreadyStarred ? -1 : 1;
    $('.btn-star').click(function() {
        var starred = $(this).find('.starred');
        starred.html(' ' + (parseInt(starred.text()) + increment).toString());
        $.ajax('/' + projectOwner + '/' + projectSlug + '/star/' + (increment > 0));

        var icon = $('#icon-star');
        if (increment > 0) {
            icon.removeClass('fa-star-o').addClass('fa-star');
        } else {
            icon.removeClass('fa-star').addClass('fa-star-o');
        }

        increment *= -1;
    });

    var body = $('body');
    body.keydown(function(event) {
        var target = $(event.target);
        if (target.is('body') && shouldExecuteHotkey(event)) {
            var navBar = $('.project-navbar');
            switch (event.keyCode) {
                case KEY_PLUS:
                    event.preventDefault();
                    switchTabTo(getActiveTab().next(), navBar.find('li:first'));
                    break;
                case KEY_MINUS:
                    event.preventDefault();
                    switchTabTo(getActiveTab().prev(), navBar.find('li:last'));
                    break;
                default:
                    break;
            }
        }
    });
});
