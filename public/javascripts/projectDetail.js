var projectOwner = null;
var projectSlug = null;
var alreadyStarred = false;
var markdown = new showdown.Converter();

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

function animateEditBtn(e, marginLeft, andThen) {
    e.animate({ marginLeft: marginLeft }, 100, function() {
        if (andThen) andThen();
    });
}

function showEditBtn(e, andThen) {
    animateEditBtn(e, '-34px', function() {
        e.css('z-index', '1000');
        if (andThen) andThen();
    });
}

function hideEditBtn(e, andThen) {
    animateEditBtn(e, '0', andThen);
}

function initBtnEdit() {
    var btnEdit = $('.btn-edit');
    if (!btnEdit.length) return;

    var pageBtns = $('.btn-page');
    var otherBtns = $('.btn-edit-container');

    // highlight with textarea
    var editText = $('.page-edit').find('textarea');
    editText.focus(function() {
        btnEdit
            .css('border-color', '#66afe9')
            .css('border-right', '1px solid white')
            .css('box-shadow', 'inset 0 1px 1px rgba(0,0,0,.075), -3px 0 8px rgba(102, 175, 233, 0.6)');
        otherBtns.find('.btn').css('border-right-color', '#66afe9')
    }).blur(function() {
        $('.btn-page').css('border', '1px solid #ccc').css('box-shadow', 'none');
        $('button.open').css('border-right', 'white');
    });

    pageBtns.click(function() {
        if ($(this).hasClass('open')) return;

        // toggle button
        $('button.open').removeClass('open').css('border', '1px solid #ccc');
        $(this).addClass('open').css('border-right-color', 'white');

        var editor = $('.page-edit');

        if ($(this).hasClass('btn-edit')) {
            // open editor
            var content = $('.page-rendered');
            editor.find('textarea').css('height', content.css('height'));
            content.hide();
            editor.show();

            // show buttons
            showEditBtn($('.btn-preview-container'), function() {
                showEditBtn($('.btn-save-container'), function() {
                    showEditBtn($('.btn-cancel-container'), function() {
                        showEditBtn($('.btn-delete-container'));
                    });
                });
            });
        }

        else if ($(this).hasClass('btn-preview')) {
            // render markdown
            var preview = $('.page-preview');
            var raw = editor.find('textarea').val();
            editor.hide();
            preview.show();
            var icon = $(this).find('i').removeClass('fa-eye').addClass('fa-spinner fa-spin');
            console.log(raw);
            $.post({
                url: '/pages/preview',
                data: JSON.stringify({ raw: raw }),
                contentType: 'application/json',
                dataType: 'html',
                complete: function() { icon.removeClass('fa-spinner fa-spin').addClass('fa-eye'); },
                success: function(cooked) { preview.html(cooked); }
            });
        }

        else if ($(this).hasClass('btn-save')) {
            // add spinner
            $(this).find('i').removeClass('fa-save').addClass('fa-spinner fa-spin');
        }
    });

    $('.btn-cancel').click(function() {
        // hide editor; show content
        $('.page-edit').hide();
        $('.page-preview').hide();
        $('.page-content').show();

        // move buttons behind
        $('.btn-edit-container').css('z-index', '-1000');

        // hide buttons
        var fromSave = function() {
            hideEditBtn($('.btn-save-container'), function() {
                hideEditBtn($('.btn-preview-container'));
            });
        };

        var btnDelete = $('.btn-delete-container');
        var btnCancel = $('.btn-cancel-container');
        if (btnDelete.length) {
            hideEditBtn(btnDelete, function() { hideEditBtn(btnCancel, fromSave) });
        } else {
            hideEditBtn(btnCancel, fromSave);
        }
    });

    // move with scroll
    var origTop = btnEdit.position().top;
    var origOff = btnEdit.offset().top;
    $(window).scroll(function() {
        var navHeight = $('.navbar-main').height();
        var top = $(this).scrollTop();
        if (top > btnEdit.offset().top - navHeight - 40) {
            var editTop = navHeight + 20;
            btnEdit.css('position', 'fixed').css('top', editTop);
            otherBtns.each(function() {
                editTop += 0.5;
                $(this).css('position', 'fixed').css('top', editTop);
            });
        } else if (top < origOff - navHeight - 40) {
            btnEdit.css('position', 'absolute').css('top', origTop);
            otherBtns.each(function() {
                $(this).css('position', 'absolute').css('top', origTop);
            });
        }
    });
}

$(function() {
    initFlagList();
    initBtnEdit();

    // flag button alert
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

    // hotkeys
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
