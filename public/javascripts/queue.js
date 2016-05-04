$(function() {
    
    $('.btn-approve').click(function() {
        var listItem = $(this).closest('.list-group-item');
        var versionPath = listItem.data('version');
        var icon = $(this).find('i').removeClass('fa-thumbs-up').addClass('fa-spinner fa-spin');
        $.ajax({
            url: '/' + versionPath + '/approve',
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
