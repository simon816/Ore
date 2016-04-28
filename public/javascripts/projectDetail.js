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

function initFlagList() {
    var flagList = $('.list-flags');
    if (!flagList.length) return;
    flagList.find('li').click(function() {
        flagList.find(':checked').removeAttr('checked');
        $(this).find('input').prop('checked', true);
    });
}

function initBtnEdit() {
    var btnEdit = $('.btn-edit');
    if (!btnEdit.length) return;

    btnEdit.mouseenter(function() {
        $('.page-content').css('background-color', '#e6e6e6').css('border-color', '#adadad');
    }).mouseleave(function() {
        $('.page-content').css('background-color', 'white').css('border-color', '#ccc');
    });

    var origTop = btnEdit.position().top;
    $(window).scroll(function() {
        var navHeight = $('.navbar-main').height();
        var top = $(this).scrollTop();
        console.log(top);
        if (top > btnEdit.offset().top - navHeight - 30) {
            btnEdit.css('position', 'fixed').css('top', navHeight + 20);
        } else if (top - navHeight - 85 < origTop) {
            btnEdit.css('position', 'absolute').css('top', origTop);
        }
    });
}

$(function() {
    initFlagList();
    initBtnEdit();

    var flagMsg = $('.flag-msg');
    if (flagMsg.length) {
        flagMsg.hide().fadeIn(1000).delay(2000).fadeOut(1000);
    }

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
                    switchTabTo(getActiveTab().prev(), navBar.find('#discussion'));
                    break;
                default:
                    break;
            }
        }
    });
});
