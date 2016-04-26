function addSpinner(e) {
    return e.addClass('fa-spinner fa-spin');
}

function removeSpinner(e) {
    return e.removeClass('fa-spinner fa-spin');
}

var ICON_VIS = 'fa-eye-slash';
var ICON_INVIS = 'fa-eye';

$(function() {
    $('.btn-hide').click(function() {
        var lang = $(this).find('span');
        var icon = $(this).find('i');
        var visible = icon.hasClass(ICON_VIS);
        var iconClass = visible ? ICON_VIS : ICON_INVIS;
        var project = $(this).closest('li').data('project');
        var spinner = addSpinner(icon.removeClass(iconClass));
        $.ajax({
            url: '/' + project + '/visible/' + !visible,
            fail: function() { removeSpinner(spinner.addClass(iconClass)); },
            success: function() {
                removeSpinner(spinner.addClass(visible ? ICON_INVIS : ICON_VIS));
                lang.text(visible ? 'Unhide' : 'Hide');
            }
        });
    });

    $('.btn-resolve').click(function() {
        var listItem = $(this).closest('li');
        var flagId = listItem.data('flag-id');
        var spinner = addSpinner($(this).find('i').removeClass('fa-check'));
        $.ajax({
            url: '/admin/flags/' + flagId + '/resolve/true',
            complete: function() { removeSpinner(spinner.addClass('fa-check')); },
            success: function() {
                $.when(listItem.fadeOut('slow')).done(function() {
                    listItem.remove();
                    if (!$('.list-flags-admin').find('li').length) {
                        $('.unread').remove();
                        $('.no-flags').fadeIn();
                    }
                });
            }
        });
    });
});
