var projectOwner = null;
var projectSlug = null;
var alreadyStarred = false;

var KEY_TAB = 9;

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

    $('body').keydown(function(event) {
        var target = $(event.target);
        if (target.is('body')) {
            switch (event.keyCode) {
                case KEY_TAB:
                    event.preventDefault();
                    var navBar = $('.project-navbar');
                    var activeTab = navBar.find('li.active');
                    var nextTab = activeTab.next();
                    var nextId = nextTab.attr('id');
                    if (nextTab.is('li') && nextId !== 'issues' && nextId !== 'source') {
                        window.location = nextTab.find('a').attr('href');
                    } else {
                        window.location = navBar.find('li:first').find('a').attr('href');
                    }
                    break;
                default:
                    break;
            }
        }
    });
});
