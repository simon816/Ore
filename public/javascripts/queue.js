/*
 * ==================================================
 *  _____             _
 * |     |___ ___    |_|___
 * |  |  |  _| -_|_  | |_ -|
 * |_____|_| |___|_|_| |___|
 *                 |___|
 *
 * By Walker Crouse (windy) and contributors
 * (C) SpongePowered 2016-2017 MIT License
 * https://github.com/SpongePowered/Ore
 *
 * Powers the admin approval queue.
 *
 * ==================================================
 */

/*
 * ==================================================
 * =                   Doc ready                    =
 * ==================================================
 */

$(function() {
    $('.btn-approve').click(function() {
        var listItem = $(this).closest('.list-group-item');
        var versionPath = listItem.data('version');
        var icon = $(this).find('i').removeClass('fa-thumbs-up').addClass('fa-spinner fa-spin');
        $.ajax({
            type: 'post',
            url: '/' + versionPath + '/approve',
            data: { csrfToken: csrf },
            complete: function() { icon.removeClass('fa-spinner fa-spin').addClass('fa-thumbs-up'); },
            success: function() {
                $.when(listItem.fadeOut('slow')).done(function() { 
                    listItem.remove();
                    if (!$('.list-versions').find('li').length) {
                        $('.no-versions').fadeIn();
                        clearUnread($('a[href="/admin/queue"]'));
                    }
                });
            }
        });
    });
});
